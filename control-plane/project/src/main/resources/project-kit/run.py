"""공개 테스트를 로컬에서 돌린다 — 채점기와 같은 하네스다.

    python3 .codedrill/run.py

채점은 서버가 한다. 여기서 도는 것은 공개 테스트와 내가 더 쓴 테스트뿐이고, 숨은 테스트는 키트에 없다.
결과가 채점과 같으려면 채점기와 같은 Python(3.10 이상)이면 좋다.
"""

import json
import pathlib
import secrets
import subprocess
import sys
import tempfile

ROOT = pathlib.Path(__file__).resolve().parent.parent
KIT = ROOT / ".codedrill"


def syntax_errors() -> list[str]:
    """채점기의 빌드 단계와 같다 — 모든 .py 를 컴파일해 본다. 키트 파일은 뺀다."""
    errors = []
    for path in sorted(ROOT.rglob("*.py")):
        if KIT in path.parents:
            continue
        try:
            compile(path.read_text(encoding="utf-8"), str(path), "exec")
        except SyntaxError as error:
            errors.append(f"{path.relative_to(ROOT)}:{error.lineno}: {error.msg}")
    return errors


def main() -> int:
    meta = json.loads((KIT / "project.json").read_text(encoding="utf-8"))
    print(f"{meta['title']} · v{meta['version']} · Python")

    errors = syntax_errors()
    if errors:
        print("빌드 ✕ 문법 오류")
        for line in errors:
            print(f"  {line}")
        return 1
    print("빌드 ✓")

    with tempfile.TemporaryDirectory() as tmp:
        nonce = pathlib.Path(tmp, "nonce")
        nonce.write_text(secrets.token_hex(16), encoding="utf-8")
        report_path = pathlib.Path(tmp, "report.json")
        subprocess.run(
            [sys.executable, "-B", str(KIT / "harness" / "python_harness.py"), str(ROOT), str(report_path),
             str(meta["limits"]["memoryMb"]), str(nonce)],
            cwd=ROOT,
            check=False,
        )
        if not report_path.exists():
            print("테스트를 돌리지 못했습니다 — 하네스가 리포트를 남기지 않았습니다")
            return 1
        report = json.loads(report_path.read_text(encoding="utf-8"))
    return print_report(report, set(meta["publicTests"]))


def print_report(report: dict, public: set[str]) -> int:
    if report.get("loadError"):
        print("테스트를 불러오지 못했습니다")
        print(report["loadError"])
        return 1
    if report.get("tampered"):
        print(f"테스트 기반이 바뀌었습니다 — 채점에서는 전부 실패입니다: {report['tampered']}")
        return 1
    tests = report.get("tests", [])
    passed = sum(1 for test in tests if test["passed"])
    print("공개 테스트")
    for test in tests:
        mark = "✓" if test["passed"] else "✕"
        mine = "" if test["module"] in public else "  (내가 쓴 테스트)"
        print(f"  {mark} {test['module']}.{test['name']}{mine}")
        if not test["passed"] and test.get("message"):
            print(f"      {test['message']}")
    print(f"{passed} / {len(tests)} 통과 — 숨은 테스트는 제출하면 서버에서 돕니다")
    return 0 if tests and passed == len(tests) else 1


if __name__ == "__main__":
    sys.exit(main())
