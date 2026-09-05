#!/usr/bin/env python3
"""첫 vertical slice E2E 스모크 (기술 설계서 §16.2).

문제 열기 → 제출 → SSE → 판정까지 실제 서비스를 통해 확인한다. 세 앱과 인프라가 이미
떠 있어야 한다. 사용법은 docs/running-locally.md 에 있다.

검증하는 것:
  - 판정 정확성: AC / WA / CE / RE / TLE
  - 멱등성: 같은 Idempotency-Key 로 두 번 제출하면 제출이 하나만 생긴다
  - SSE: 상태 변화가 스트림으로 흘러나온다
  - 숨은 테스트 비노출: 응답에 숨은 그룹의 케이스 내역이 없다
"""

from __future__ import annotations

import json
import sys
import time
import urllib.error
import urllib.request
import uuid

BASE = "http://localhost:8080/api/v1"
TIMEOUT = 60

ACCEPTED_SOURCE = """
fun twoSum(nums: IntArray, target: Int): IntArray {
    val seen = HashMap<Int, Int>()
    for (i in nums.indices) {
        Drill.visit(i, nums[i])
        val j = seen[target - nums[i]]
        if (j != null) {
            Drill.match(j, i)
            return intArrayOf(j, i)
        }
        Drill.compare(i, target - nums[i])
        seen.putIfAbsent(nums[i], i)
    }
    error("정답은 항상 존재한다")
}
"""

WRONG_SOURCE = "fun twoSum(nums: IntArray, target: Int): IntArray = intArrayOf(0, 0)"

COMPILE_ERROR_SOURCE = "fun twoSum(nums: IntArray, target: Int): IntArray { 이건 코드가 아니다 }"

RUNTIME_ERROR_SOURCE = """
fun twoSum(nums: IntArray, target: Int): IntArray {
    throw IllegalStateException("의도적 실패")
}
"""

TIME_LIMIT_SOURCE = """
fun twoSum(nums: IntArray, target: Int): IntArray {
    while (true) { }
}
"""


def request(method: str, path: str, body: dict | None = None, headers: dict | None = None) -> dict:
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(f"{BASE}{path}", data=data, method=method)
    req.add_header("Content-Type", "application/json")
    for key, value in (headers or {}).items():
        req.add_header(key, value)
    with urllib.request.urlopen(req, timeout=10) as response:
        return json.loads(response.read())


def submit(source: str, key: str | None = None) -> dict:
    return request(
        "POST",
        "/submissions",
        {"problemId": "two-sum", "problemVersion": 1, "language": "KOTLIN", "source": source},
        {"Idempotency-Key": key or str(uuid.uuid4())},
    )


def await_verdict(submission_id: str) -> dict:
    deadline = time.time() + TIMEOUT
    last = {}
    while time.time() < deadline:
        last = request("GET", f"/submissions/{submission_id}")
        if last["status"] == "COMPLETED":
            return last
        time.sleep(0.5)
    raise TimeoutError(f"{TIMEOUT}초 안에 판정이 끝나지 않았다: {last.get('status')}")


def read_events(submission_id: str, limit: int = 4) -> list[str]:
    """SSE 스트림에서 이벤트 이름을 모은다. 종료 이벤트를 만나면 멈춘다."""
    seen: list[str] = []
    req = urllib.request.Request(f"{BASE}/submissions/{submission_id}/events")
    try:
        with urllib.request.urlopen(req, timeout=TIMEOUT) as stream:
            for raw in stream:
                line = raw.decode().strip()
                if line.startswith("event:"):
                    seen.append(line.split(":", 1)[1].strip())
                    if seen[-1] == "completed" or len(seen) >= limit:
                        break
    except (urllib.error.URLError, TimeoutError, OSError):
        pass
    return seen


def await_trace(submission_id: str) -> dict | None:
    """트레이스는 판정 뒤 별도 작업으로 온다. 없으면 204 라서 None 이 돌아온다."""
    deadline = time.time() + 20
    while time.time() < deadline:
        req = urllib.request.Request(f"{BASE}/submissions/{submission_id}/trace")
        with urllib.request.urlopen(req, timeout=10) as response:
            if response.status == 200:
                return json.loads(response.read())
        time.sleep(0.5)
    return None


def check(label: str, actual, expected) -> bool:
    ok = actual == expected
    print(f"  {'PASS' if ok else 'FAIL'}  {label}: {actual}" + ("" if ok else f" (기대: {expected})"))
    return ok


def main() -> int:
    results: list[bool] = []

    print("문제 조회")
    problems = request("GET", "/problems")
    results.append(check("문제 수", len(problems), 1))
    detail = request("GET", "/problems/two-sum")
    results.append(check("공개 샘플 수", len(detail["samples"]), 2))
    results.append(check("시그니처", detail["signature"], "fun twoSum(nums: IntArray, target: Int): IntArray"))

    print("\n판정 정확성 (§14.2)")
    for label, source, expected in [
        ("정답", ACCEPTED_SOURCE, "ACCEPTED"),
        ("오답", WRONG_SOURCE, "WRONG_ANSWER"),
        ("컴파일 실패", COMPILE_ERROR_SOURCE, "COMPILE_ERROR"),
        ("런타임 예외", RUNTIME_ERROR_SOURCE, "RUNTIME_ERROR"),
        ("무한 루프", TIME_LIMIT_SOURCE, "TIME_LIMIT"),
    ]:
        final = await_verdict(submit(source)["id"])
        results.append(check(label, final["verdict"], expected))
        if label == "정답":
            results.append(check("  만점", final["score"], 100))
            hidden = [g for g in final["groups"] if g["groupId"] != "sample"]
            leaked = [g for g in hidden if g["cases"]]
            results.append(check("  숨은 그룹 케이스 비노출", leaked, []))

    print("\n실행 트레이스 (§7)")
    traced = submit(ACCEPTED_SOURCE)
    await_verdict(traced["id"])
    capture = await_trace(traced["id"])
    results.append(check("트레이스 도착", capture is not None, True))
    if capture:
        results.append(check("이벤트 있음", len(capture["events"]) > 0, True))
        results.append(check("잘리지 않음", capture["truncated"], False))
        results.append(check("공개 케이스만", capture["caseId"].startswith("sample/"), True))

    print("\n멱등성 (§4.3)")
    key = str(uuid.uuid4())
    first = submit(ACCEPTED_SOURCE, key)
    second = submit(ACCEPTED_SOURCE, key)
    results.append(check("같은 키는 같은 제출", second["id"], first["id"]))

    print("\nSSE (§9.1)")
    pending = submit(ACCEPTED_SOURCE)
    events = read_events(pending["id"])
    results.append(check("이벤트 수신", len(events) > 0, True))
    await_verdict(pending["id"])

    passed, total = sum(results), len(results)
    print(f"\n{passed}/{total} 통과")
    return 0 if passed == total else 1


if __name__ == "__main__":
    sys.exit(main())
