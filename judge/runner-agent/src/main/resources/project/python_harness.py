"""프로젝트형 문제의 Python 테스트 하네스 (ProjectEngine 이 샌드박스에 넣는다).

    python3 -B python_harness.py <워크스페이스> <리포트 파일> <메모리 MB>

워크스페이스의 tests/ 아래 test_*.py 를 unittest 로 찾아 돌리고, 테스트마다 (모듈, 이름,
통과 여부, 사유) 를 리포트 파일에 JSON 으로 적는다. stdout 이 아니라 **파일**이다 — 사용자
코드가 stdout 에 무엇을 얼마나 찍든 리포트가 잘리면 안 된다.

**테스트 기반을 지킨다.** 테스트는 사용자 코드와 한 프로세스에서 돌고, 사용자 코드는
import 되는 순간 unittest 를 손댈 수 있다 — 단언을 전부 통과로 바꾸는 한 줄이면 된다.
막을 수는 없으니 **잡는다**:

1. 사용자 코드를 import 하기 전에 unittest.TestCase 의 메서드를 전부 기억해 두고, 로딩과
   실행이 끝난 뒤 하나라도 바뀌었는지 본다.
2. 반드시 실패해야 하는 카나리 테스트를 숨은 모듈마다 무작위 이름으로 끼워 넣는다. 그것이
   통과하면 단언이 죽은 것이다.

어느 쪽이든 걸리면 리포트에 `tampered` 를 적고, 채점기는 전부 실패로 판정한다. 이것은
증명이 아니라 가드다 — 규모에서의 방어는 검토와 유사도 신호다.
"""

import io
import json
import os
import sys
import traceback
import uuid

# 사용자 코드보다 먼저 들여온다. 이 아래의 참조가 "손대기 전"의 것이다.
import unittest
import unittest.case as _case

_ORIGINAL_TESTCASE = _case.TestCase
_ORIGINAL_METHODS = {
    name: getattr(_case.TestCase, name)
    for name in dir(_case.TestCase)
    if name.startswith("assert") or name in ("run", "fail", "__call__", "subTest")
}
_ORIGINAL_MODULE = sys.modules["unittest"]


def _tampering() -> str | None:
    if sys.modules.get("unittest") is not _ORIGINAL_MODULE or unittest.TestCase is not _ORIGINAL_TESTCASE:
        return "unittest 모듈 또는 TestCase 가 바뀌었다"
    for name, original in _ORIGINAL_METHODS.items():
        if getattr(_case.TestCase, name, None) is not original:
            return f"unittest.TestCase.{name} 이 바뀌었다"
    return None


def _limit_memory(megabytes: int) -> None:
    try:
        import resource

        limit = megabytes * 1024 * 1024
        resource.setrlimit(resource.RLIMIT_AS, (limit, limit))
    except Exception:  # macOS 는 주소 공간 상한을 낮추지 못한다. 컨테이너의 cgroup 이 잡는다.
        pass


class _Result(unittest.TestResult):
    """테스트마다 한 줄. 사유는 마지막 줄만 — 트레이스백은 사용자 화면에 쓸모가 없고 길다."""

    def __init__(self) -> None:
        super().__init__()
        self.rows: list[dict] = []

    def _row(self, test, passed: bool, message: str | None) -> None:
        if isinstance(test, unittest.loader._FailedTest):
            # 모듈을 import 하지 못했다. unittest 는 그것을 자기 모듈의 테스트로 꾸미는데,
            # 그러면 어느 모듈이 깨졌는지 — 숨은 것인지 — 를 채점기가 알 수 없다.
            self.rows.append({"module": test._testMethodName, "name": "<import>", "passed": False, "message": message})
            return
        self.rows.append({
            "module": test.__class__.__module__,
            "name": f"{test.__class__.__name__}.{test._testMethodName}",
            "passed": passed,
            "message": message,
        })

    def addSuccess(self, test):
        super().addSuccess(test)
        self._row(test, True, None)

    def addFailure(self, test, err):
        super().addFailure(test, err)
        self._row(test, False, _reason(err))

    def addError(self, test, err):
        super().addError(test, err)
        self._row(test, False, _reason(err))

    def addSkip(self, test, reason):
        super().addSkip(test, reason)
        self._row(test, False, f"건너뜀: {reason}")

    def addExpectedFailure(self, test, err):
        super().addExpectedFailure(test, err)
        self._row(test, True, None)

    def addUnexpectedSuccess(self, test):
        super().addUnexpectedSuccess(test)
        self._row(test, False, "실패해야 하는데 통과했다")


def _reason(err) -> str:
    kind, value, _ = err
    lines = traceback.format_exception_only(kind, value)
    text = "".join(lines).strip().splitlines()
    return (text[-1] if text else kind.__name__)[:500]


def _canary_name() -> str:
    return "Canary_" + uuid.uuid4().hex[:12]


def main() -> int:
    workspace, report_path, memory_mb = sys.argv[1], sys.argv[2], int(sys.argv[3])
    _limit_memory(memory_mb)
    os.environ.setdefault("PYTHONHASHSEED", "0")

    report: dict = {"tests": [], "tampered": None, "loadError": None}

    # 사용자 출력은 파일로 가지 않는다. 화면에 보일 것은 판정이지 print 가 아니다.
    sys.path.insert(0, workspace)
    loader = unittest.TestLoader()
    try:
        suite = loader.discover(start_dir=os.path.join(workspace, "tests"), pattern="test_*.py", top_level_dir=workspace)
    except Exception:
        report["loadError"] = traceback.format_exc()[-2000:]
        _write(report_path, report)
        return 0

    # 카나리: 각 테스트 모듈에 "반드시 실패하는" 테스트를 무작위 이름으로 끼운다.
    modules = {t.__class__.__module__ for t in _flatten(suite) if not isinstance(t, unittest.loader._FailedTest)}
    canaries: set[str] = set()
    for module_name in sorted(modules):
        name = _canary_name()
        canary = type(name, (_ORIGINAL_TESTCASE,), {"test_must_fail": lambda self: self.assertEqual(1, 2)})
        canary.__module__ = module_name
        canaries.add(f"{name}.test_must_fail")
        suite.addTest(canary("test_must_fail"))

    tampered = _tampering()
    result = _Result()
    stdout, stderr = sys.stdout, sys.stderr
    try:
        sys.stdout = io.StringIO()
        sys.stderr = io.StringIO()
        suite.run(result)
    finally:
        sys.stdout, sys.stderr = stdout, stderr

    tampered = tampered or _tampering()
    for row in result.rows:
        if row["name"] in canaries:
            if row["passed"]:
                tampered = tampered or "실패해야 하는 카나리 테스트가 통과했다"
            continue
        # 로딩에 실패한 모듈은 unittest 가 _FailedTest 로 바꿔 놓는다. 그것도 실패다.
        report["tests"].append(row)

    report["tampered"] = tampered
    _write(report_path, report)
    return 0


def _flatten(suite):
    for item in suite:
        if isinstance(item, unittest.TestSuite):
            yield from _flatten(item)
        else:
            yield item


def _write(path: str, report: dict) -> None:
    with open(path, "w", encoding="utf-8") as out:
        json.dump(report, out, ensure_ascii=False)


if __name__ == "__main__":
    sys.exit(main())
