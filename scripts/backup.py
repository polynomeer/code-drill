#!/usr/bin/env python3
"""백업과 복원 리허설 (기술 설계서 §12.3).

**복원해 본 적 없는 백업은 백업이 아니다.** 그래서 이 스크립트의 중심은 `create` 가
아니라 `verify` 다 — 받아 둔 덤프를 임시 DB 에 실제로 복원하고, 데이터가 맞아떨어지는지
확인한 뒤 지운다. 운영 DB 를 건드리지 않으므로 아무 때나 돌릴 수 있다.

    python3 scripts/backup.py create            # 덤프를 받는다
    python3 scripts/backup.py verify            # 최신 덤프를 임시 DB 에 복원해 본다
    python3 scripts/backup.py restore <파일>    # 운영 DB 를 덮어쓴다 (--yes 필요)
    python3 scripts/backup.py list

## 무엇을 받고 무엇을 받지 않나

받는 것은 **Postgres 뿐**이다. 나머지는 받을 필요가 없거나, 받아도 소용이 없다.

- **브로커**: 진행 중인 작업만 들어 있다. 제출은 아웃박스와 함께 커밋되므로 복원 후
  다시 발행된다 (§3.2). 큐를 복원하면 오히려 같은 작업을 두 번 돌린다.
- **문제 패키지**: git 과 이미지에 있다. 공개 포인터만 DB 에 있고, 그건 덤프에 들어간다.
- **오브젝트 스토어**: 아직 실제 데이터가 없다. §8.3 이 붙어 소스와 트레이스가 그쪽으로
  옮겨가면 **이 스크립트도 함께 늘어야 한다.**
"""

from __future__ import annotations

import argparse
import json
import pathlib
import subprocess
import sys
import time

ROOT = pathlib.Path(__file__).resolve().parents[1]
BACKUPS = ROOT / "build" / "backups"
CONTAINER = "code-drill-postgres-1"
DB = "codedrill"
USER = "codedrill"

# 복원이 끝났다는 것은 프로세스가 뜬 것이 아니라 **데이터가 맞아떨어진다**는 뜻이다.
# 여기 있는 것은 §12.4 일관성 점검 중 복원 직후에 의미가 있는 것들이다.
INVARIANTS = [
    ("공개 포인터가 가리키는 버전이 있다",
     """SELECT count(*) FROM problem p
         WHERE p.published_version_id IS NOT NULL
           AND NOT EXISTS (SELECT 1 FROM problem_version v WHERE v.id = p.published_version_id)"""),
    ("판정 이력의 제출이 모두 있다",
     """SELECT count(*) FROM submission_judgement j
         WHERE NOT EXISTS (SELECT 1 FROM submission s WHERE s.id = j.submission_id)"""),
    ("관리자 역할의 계정이 모두 있다",
     """SELECT count(*) FROM admin_role r
         WHERE NOT EXISTS (SELECT 1 FROM app_user u WHERE u.id = r.user_id)"""),
    ("끝난 제출에는 판정이 있다",
     "SELECT count(*) FROM submission WHERE status = 'COMPLETED' AND verdict IS NULL"),
]

# 덤프가 비어 있지 않은지 보는 기준. 행 수가 아니라 **표가 있는지**를 본다 — 행 수는
# 백업 시점에 따라 달라지지만, 표가 없으면 그건 복원이 아니라 빈 DB 다.
REQUIRED_TABLES = ["submission", "problem", "problem_version", "app_user", "admin_role"]


def run(args: list[str], **kwargs) -> subprocess.CompletedProcess:
    return subprocess.run(args, capture_output=True, text=True, **kwargs)


def psql(sql: str, database: str = DB) -> str:
    result = run(["docker", "exec", CONTAINER, "psql", "-U", USER, "-d", database, "-tAc", sql])
    if result.returncode != 0:
        raise SystemExit(f"psql 실패: {result.stderr.strip()[:300]}")
    return result.stdout.strip()


def ensure_container() -> None:
    if run(["docker", "inspect", CONTAINER]).returncode != 0:
        raise SystemExit(f"{CONTAINER} 가 없다. python3 scripts/up.py 로 띄운다.")


def create(args) -> int:
    ensure_container()
    BACKUPS.mkdir(parents=True, exist_ok=True)
    stamp = time.strftime("%Y%m%dT%H%M%S")
    target = BACKUPS / f"{stamp}.dump"

    # -Fc 는 custom 포맷이다. pg_restore 로 표를 골라 복원할 수 있어, 사고 때 필요한
    # 부분만 되살리는 선택지가 남는다. 바이너리라 텍스트로 받지 않는다.
    dumped = subprocess.run(
        ["docker", "exec", CONTAINER, "pg_dump", "-U", USER, "-d", DB, "-Fc"],
        capture_output=True,
    )
    if dumped.returncode != 0:
        raise SystemExit(f"pg_dump 실패: {dumped.stderr.decode(errors='replace')[:300]}")
    target.write_bytes(dumped.stdout)

    manifest = {
        "createdAt": stamp,
        "gitSha": run(["git", "rev-parse", "HEAD"], cwd=ROOT).stdout.strip(),
        # version 은 텍스트라 max() 가 사전순으로 센다 — '9' > '12' 가 된다.
        # 적용 순서로 마지막 것을 본다.
        "schemaVersion": psql(
            "SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank DESC LIMIT 1"),
        "rows": {t: int(psql(f"SELECT count(*) FROM {t}")) for t in REQUIRED_TABLES},
    }
    target.with_suffix(".json").write_text(json.dumps(manifest, indent=2) + "\n")

    print(f"덤프: {target}  ({target.stat().st_size // 1024}KB)")
    print(f"  스키마 {manifest['schemaVersion']}, 커밋 {manifest['gitSha'][:12]}")
    for table, count in manifest["rows"].items():
        print(f"  {table:<18} {count}행")
    return 0


def latest() -> pathlib.Path:
    dumps = sorted(BACKUPS.glob("*.dump"))
    if not dumps:
        raise SystemExit("덤프가 없다. python3 scripts/backup.py create 를 먼저 돌린다.")
    return dumps[-1]


def verify(args) -> int:
    """임시 DB 에 복원해 보고 지운다. 운영 DB 를 건드리지 않는다."""
    ensure_container()
    dump = pathlib.Path(args.file) if args.file else latest()
    scratch = f"codedrill_restore_{int(time.time())}"
    print(f"복원 리허설: {dump.name} → {scratch}")

    run(["docker", "exec", CONTAINER, "createdb", "-U", USER, scratch])
    try:
        restored = subprocess.run(
            ["docker", "exec", "-i", CONTAINER, "pg_restore", "-U", USER, "-d", scratch, "--no-owner"],
            input=dump.read_bytes(), capture_output=True,
        )
        # pg_restore 는 소유자·권한 경고로도 0 이 아닌 값을 낸다. 실패 여부는 데이터로 본다.
        if restored.returncode != 0:
            print(f"  (pg_restore 경고: {restored.stderr.decode()[:120].strip()})")

        failures = []
        for table in REQUIRED_TABLES:
            exists = psql(f"SELECT to_regclass('public.{table}') IS NOT NULL", scratch)
            if exists != "t":
                failures.append(f"표가 없다: {table}")
        if not failures:
            for label, sql in INVARIANTS:
                broken = int(psql(sql, scratch))
                mark = "·" if broken == 0 else "✗"
                print(f"  {mark} {label}: {broken}건")
                if broken:
                    failures.append(f"{label} — {broken}건")

        # 표가 없으면 행 수는 물을 것도 없다. 물으면 psql 이 예외를 던져, 진단이 아니라
        # 스택 트레이스가 나온다 — 리허설이 실패를 **보고**하지 못하면 리허설이 아니다.
        manifest = dump.with_suffix(".json")
        if manifest.exists() and not failures:
            expected = json.loads(manifest.read_text())["rows"]
            for table, count in expected.items():
                actual = int(psql(f"SELECT count(*) FROM {table}", scratch))
                if actual != count:
                    failures.append(f"{table} 행 수가 다르다: 덤프 {count} → 복원 {actual}")

        if failures:
            print("\n복원 리허설 실패")
            for line in failures:
                print(f"  ✗ {line}")
            return 1

        print("\n복원 리허설 통과 — 이 덤프로 되살릴 수 있다")
        return 0
    finally:
        run(["docker", "exec", CONTAINER, "dropdb", "-U", USER, "--force", scratch])


def restore(args) -> int:
    """운영 DB 를 덮어쓴다. 되돌릴 수 없다."""
    ensure_container()
    dump = pathlib.Path(args.file)
    if not dump.exists():
        raise SystemExit(f"그런 덤프가 없다: {dump}")
    if not args.yes:
        print(f"{DB} 의 현재 내용을 {dump.name} 으로 덮어쓴다. 되돌릴 수 없다.")
        print("정말 하려면 --yes 를 붙인다. 그 전에 verify 로 이 덤프가 쓸 만한지 본다.")
        return 2

    print("앱을 먼저 내린다 (python3 scripts/up.py --down)")
    result = subprocess.run(
        ["docker", "exec", "-i", CONTAINER, "pg_restore", "-U", USER, "-d", DB,
         "--clean", "--if-exists", "--no-owner"],
        input=dump.read_bytes(), capture_output=True,
    )
    print(result.stderr.decode()[:400] if result.returncode != 0 else "복원 완료")
    print("복원이 끝났다는 것은 프로세스가 뜬 것이 아니라 데이터가 맞아떨어진다는 뜻이다.")
    print("앱을 띄운 뒤 GET /api/v1/admin/consistency 가 깨끗한지 본다 (§12.4).")
    return 0


def listing(args) -> int:
    dumps = sorted(BACKUPS.glob("*.dump"))
    if not dumps:
        print("덤프가 없다.")
        return 0
    for dump in dumps:
        manifest = dump.with_suffix(".json")
        note = ""
        if manifest.exists():
            data = json.loads(manifest.read_text())
            note = f"  스키마 {data['schemaVersion']}, 커밋 {data['gitSha'][:12]}"
        print(f"{dump.name}  {dump.stat().st_size // 1024}KB{note}")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="command", required=True)
    sub.add_parser("create", help="덤프를 받는다").set_defaults(run=create)
    verify_cmd = sub.add_parser("verify", help="임시 DB 에 복원해 본다")
    verify_cmd.add_argument("file", nargs="?", help="생략하면 가장 최근 덤프")
    verify_cmd.set_defaults(run=verify)
    restore_cmd = sub.add_parser("restore", help="운영 DB 를 덮어쓴다")
    restore_cmd.add_argument("file")
    restore_cmd.add_argument("--yes", action="store_true", help="정말 덮어쓴다")
    restore_cmd.set_defaults(run=restore)
    sub.add_parser("list", help="받아 둔 덤프").set_defaults(run=listing)

    args = parser.parse_args()
    return args.run(args)


if __name__ == "__main__":
    sys.exit(main())
