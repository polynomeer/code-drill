"""배포 이미지 취약점 게이트 (기술 설계서 §11.4 이미지 스캔, §5.5 런타임 이미지).

§11.4 의 네 게이트 중 마지막이다. 앞의 셋 — API authz, sandbox 회귀, 콘텐츠 검증 — 은
우리가 쓴 코드를 본다. 이것은 **우리가 쓰지 않은 코드**를 본다. 이미지의 대부분은 우리
것이 아니고, 그 대부분이 사용자 코드와 같은 커널 위에서 돈다.

    python3 scripts/scan-images.py                 # 우리 이미지 넷 + 샌드박스 런타임 셋
    python3 scripts/scan-images.py --tag <SHA>     # 특정 태그
    python3 scripts/scan-images.py <이미지> ...     # 지정한 것만
    python3 scripts/scan-images.py --report r.json <이름>   # 저장해 둔 보고서로 다시 판단

## 무엇으로 막고 무엇으로 막지 않는가

**고칠 수 있는 CRITICAL 에서만 막는다.**

고칠 수 없는 것으로 막으면 게이트가 꺼진다. 상류에 패치가 없는 취약점 앞에서 빌드를
세우면 할 수 있는 일이 "게이트를 끄는 것" 하나뿐이고, 그러면 고칠 수 있는 것까지 함께
통과하게 된다. **막을 수 없는 것으로 막지 않는 편이 더 안전하다** — 대신 전부 세어
보고하므로, 수가 늘고 있다는 사실은 숨지 않는다.

HIGH 로 막지 않는 것도 같은 이유다. Runner 이미지는 JDK·python3·컨테이너 CLI 를 얹은
1.35GB 짜리라 HIGH 가 상시 존재한다. 상시 빨간 게이트는 게이트가 아니다.

## 면제

`deploy/scan-allowlist.json` 에 적는다. **모든 면제에는 만료일이 있다.** 날짜가 지난
면제는 통과가 아니라 실패다 — 영구 면제는 결국 아무도 기억하지 못하는 예외가 된다.
"""

from __future__ import annotations

import argparse
import datetime
import json
import pathlib
import shutil
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
ALLOWLIST = ROOT / "deploy" / "scan-allowlist.json"

# 스캐너 DB 는 크고 자주 바뀐다. 다시 내려받지 않도록 한자리에 둔다.
CACHE = ROOT / "build" / "trivy-cache"
SCANNER = "aquasec/trivy:0.74.0"

# 우리가 만드는 것.
OURS = ["control-plane", "orchestrator", "runner-agent", "web"]

# 사용자 코드가 실제로 도는 곳 (§5.5). 우리가 만들지 않았지만 **우리가 고른 것**이고,
# 신뢰 경계 바깥에서 가장 노출된 자리라 우리 이미지보다 덜 중요하지 않다.
# judge/runner-agent/src/main/resources/application.yml 의 기본값과 같아야 한다.
SANDBOX = ["eclipse-temurin:21-jre", "python:3.12-alpine"]


def scanner_command() -> list[str]:
    """스캐너를 부르는 방법.

    설치돼 있으면 그것을 쓰고, 없으면 컨테이너로 돈다. 둘을 다 두는 이유는 **아무것도
    설치하지 않은 사람도 같은 게이트를 돌려 볼 수 있어야** 하기 때문이다. 게이트를
    로컬에서 돌릴 수 없으면 막혔을 때 손에 쥔 것이 CI 로그뿐이다.
    """
    if shutil.which("trivy"):
        return ["trivy", "--cache-dir", str(CACHE)]
    return [
        "docker", "run", "--rm",
        "-v", "/var/run/docker.sock:/var/run/docker.sock",
        "-v", f"{CACHE}:/cache",
        SCANNER, "--cache-dir", "/cache",
    ]


def scan(image: str) -> dict:
    CACHE.mkdir(parents=True, exist_ok=True)
    result = subprocess.run(
        scanner_command() + [
            "image", "--quiet", "--scanners", "vuln",
            "--severity", "CRITICAL,HIGH", "--format", "json",
            image,
        ],
        capture_output=True, text=True,
    )
    if result.returncode != 0:
        print(f"  스캔 실패: {result.stderr.strip().splitlines()[-1:] or result.stderr}")
        return {}
    return json.loads(result.stdout or "{}")


def findings(report: dict) -> list[dict]:
    out = []
    for target in report.get("Results") or []:
        for v in target.get("Vulnerabilities") or []:
            out.append({
                "id": v.get("VulnerabilityID", ""),
                "package": v.get("PkgName", ""),
                "installed": v.get("InstalledVersion", ""),
                # 고칠 수 있는가. 이 한 칸이 막을지 말지를 정한다.
                "fix": v.get("FixedVersion") or "",
                "severity": v.get("Severity", ""),
            })
    return out


def load_allowlist(today: datetime.date) -> tuple[dict[str, dict], list[str]]:
    """(쓸 수 있는 면제, 쓸 수 없는 면제 설명) 을 돌려준다."""
    if not ALLOWLIST.exists():
        return {}, []

    entries = json.loads(ALLOWLIST.read_text()).get("allow", [])
    live, rejected = {}, []
    for entry in entries:
        missing = [k for k in ("id", "reason", "until") if not entry.get(k)]
        if missing:
            rejected.append(f"{entry.get('id', '?')}: {', '.join(missing)} 가 없다")
            continue
        until = datetime.date.fromisoformat(entry["until"])
        if until < today:
            rejected.append(f"{entry['id']}: {entry['until']} 에 만료됐다 ({entry['reason']})")
        else:
            live[entry["id"]] = entry
    return live, rejected


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("images", nargs="*", help="스캔할 이미지. 비우면 전부.")
    parser.add_argument("--tag", default="dev", help="우리 이미지의 태그 (기본 dev)")
    parser.add_argument(
        "--report",
        help="스캔하는 대신 저장해 둔 trivy JSON 으로 판단한다. 정책만 따로 확인할 때 쓴다.",
    )
    parser.add_argument("--today", help="만료 판단 기준일 (YYYY-MM-DD). 면제 만료를 확인할 때 쓴다.")
    args = parser.parse_args()

    if args.report is None and shutil.which("trivy") is None and shutil.which("docker") is None:
        print("trivy 나 docker 중 하나가 필요하다.")
        return 2

    targets = args.images or [f"codedrill/{n}:{args.tag}" for n in OURS] + SANDBOX
    today = datetime.date.fromisoformat(args.today) if args.today else datetime.date.today()
    allowed, rejected = load_allowlist(today)

    if rejected:
        # 스캔하기 전에 끊는다. 면제 목록이 이 상태면 어떤 스캔 결과도 믿을 수 없다.
        print("쓸 수 없는 면제가 있다 (deploy/scan-allowlist.json):")
        for line in rejected:
            print(f"  {line}")
        print("\n다시 판단해서 날짜를 늘리거나 지운다. 만료를 무시하면 면제는 영구가 된다.")
        return 1

    blocking: list[tuple[str, dict]] = []
    if args.report:
        saved = json.loads(pathlib.Path(args.report).read_text())
        targets = targets[:1]
        print(f"저장된 보고서 {args.report}\n")
    else:
        saved = None
        print(f"이미지 {len(targets)}개, 스캐너 {SCANNER}\n")

    for image in targets:
        found = findings(saved if saved is not None else scan(image))
        critical = [f for f in found if f["severity"] == "CRITICAL"]
        fixable = [f for f in critical if f["fix"]]
        blocked = [f for f in fixable if f["id"] not in allowed]
        blocking += [(image, f) for f in blocked]

        high = len(found) - len(critical)
        exempt = len(fixable) - len(blocked)
        note = f", 면제 {exempt}" if exempt else ""
        mark = "막힘" if blocked else "통과"
        print(
            f"{mark}  {image}\n"
            f"      CRITICAL {len(critical)} (고칠 수 있는 것 {len(fixable)}{note}) · HIGH {high}"
        )

    if not blocking:
        print("\n막을 것이 없다. 올려도 된다.")
        return 0

    print(f"\n고칠 수 있는 CRITICAL {len(blocking)}건이 남아 있다.\n")
    for image, f in blocking:
        print(f"  {image}\n    {f['id']}  {f['package']} {f['installed']} → {f['fix']}")
    print(
        "\n기반 이미지를 올리면 대부분 사라진다. 지금 고칠 수 없다면 만료일과 이유를 붙여\n"
        "deploy/scan-allowlist.json 에 적는다 — 이유 없는 면제는 받지 않는다."
    )
    return 1


if __name__ == "__main__":
    sys.exit(main())
