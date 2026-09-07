#!/usr/bin/env python3
"""첫 vertical slice E2E 스모크 (기술 설계서 §16.2).

문제 열기 → 제출 → SSE → 판정까지 실제 서비스를 통해 확인한다. 세 앱과 인프라가 이미
떠 있어야 한다. 사용법은 docs/running-locally.md 에 있다.

검증하는 것:
  - 판정 정확성: AC / WA / CE / RE / TLE
  - 트레이스: 목차·청크·요약 예산, 다섯 자료구조 종류
  - 다국어: Kotlin / Java / Python 이 같은 문제에서 같은 판정을 받는다
  - 부분 점수: SUM 그룹이 통과 비율만큼 점수를 준다
  - 멱등성: 같은 Idempotency-Key 로 두 번 제출하면 제출이 하나만 생긴다
  - SSE: 상태 변화가 스트림으로 흘러나온다
  - 숨은 테스트 비노출: 응답에 숨은 그룹의 케이스 내역이 없다
"""

from __future__ import annotations

import json
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid

import operators

BASE = "http://localhost:8080/api/v1"

# 이 스모크가 실제로 제출하는 문제들.
REQUIRED_PROBLEMS = ["two-sum", "max-subarray", "island-count"]
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

JAVA_ACCEPTED = """
import java.util.HashMap;
import java.util.Map;

class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer, Integer> seen = new HashMap<>();
        for (int i = 0; i < nums.length; i++) {
            Integer j = seen.get(target - nums[i]);
            if (j != null) return new int[] { j, i };
            seen.putIfAbsent(nums[i], i);
        }
        throw new IllegalStateException("no answer");
    }
}
"""

PYTHON_ACCEPTED = """
def twoSum(nums, target):
    seen = {}
    for i, value in enumerate(nums):
        j = seen.get(target - value)
        if j is not None:
            return [j, i]
        seen.setdefault(value, i)
    raise AssertionError("no answer")
"""

# 부분 점수 확인용. Kadane 는 전부 통과하고, O(n^2) 는 큰 입력에서 시간 초과가 난다.
KADANE = """
fun maxSubarray(nums: IntArray): Int {
    var current = nums[0]
    var best = nums[0]
    for (i in 1 until nums.size) {
        current = maxOf(nums[i], current + nums[i])
        if (current > best) best = current
    }
    return best
}
"""

QUADRATIC = """
fun maxSubarray(nums: IntArray): Int {
    var best = nums[0]
    for (i in nums.indices) {
        var sum = 0
        for (j in i until nums.size) {
            sum += nums[j]
            if (sum > best) best = sum
        }
    }
    return best
}
"""


def raw_request(method: str, path: str, body: dict | None = None, headers: dict | None = None):
    """상태 코드까지 봐야 하는 경로용. 409 는 오류가 아니라 결과의 한 종류다."""
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(f"{BASE}{path}", data=data, method=method)
    req.add_header("Content-Type", "application/json")
    for key, value in (headers or {}).items():
        req.add_header(key, value)
    try:
        with urllib.request.urlopen(req, timeout=10) as response:
            payload = response.read()
            return response.status, (json.loads(payload) if payload else None)
    except urllib.error.HTTPError as error:
        payload = error.read()
        return error.code, (json.loads(payload) if payload else None)


def request(method: str, path: str, body: dict | None = None, headers: dict | None = None) -> dict:
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(f"{BASE}{path}", data=data, method=method)
    req.add_header("Content-Type", "application/json")
    for key, value in (headers or {}).items():
        req.add_header(key, value)
    with urllib.request.urlopen(req, timeout=10) as response:
        return json.loads(response.read())


def submit(
    source: str,
    key: str | None = None,
    language: str = "KOTLIN",
    problem: str = "two-sum",
) -> dict:
    return request(
        "POST",
        "/submissions",
        {"problemId": problem, "problemVersion": 1, "language": language, "source": source},
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
    """트레이스 목차. 판정 뒤 별도 작업으로 오므로 없으면 204 다."""
    deadline = time.time() + 20
    while time.time() < deadline:
        status, manifest = raw_request("GET", f"/submissions/{submission_id}/trace")
        if status == 200:
            return manifest
        time.sleep(0.5)
    return None


def check(label: str, actual, expected) -> bool:
    ok = actual == expected
    print(f"  {'PASS' if ok else 'FAIL'}  {label}: {actual}" + ("" if ok else f" (기대: {expected})"))
    return ok


def main() -> int:
    results: list[bool] = []

    print("문제 조회")
    problems = request("GET", "/problems")["items"]
    slugs = sorted(p["id"] for p in problems)
    # 목록을 통째로 고정하면 문제를 추가할 때마다 스모크가 깨진다. 이 스모크가 실제로
    # 쓰는 문제들이 들어 있는지만 본다.
    results.append(check("문제 목록", set(REQUIRED_PROBLEMS) <= set(slugs), True))
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

    print("\n다국어 채점 (§1.1)")
    for language, source in [
        ("JAVA", JAVA_ACCEPTED),
        ("PYTHON", PYTHON_ACCEPTED),
    ]:
        final = await_verdict(submit(source, language=language)["id"])
        results.append(check(f"{language} 정답", final["verdict"], "ACCEPTED"))
        results.append(check(f"  {language} 만점", final["score"], 100))

    print("\n부분 점수 (§6.2 SUM)")
    detail = request("GET", "/problems/max-subarray")
    sum_groups = [g for g in detail["groups"] if g["aggregation"] == "SUM"]
    results.append(check("SUM 그룹 존재", len(sum_groups), 1))

    fast = await_verdict(submit(KADANE, problem="max-subarray")["id"])
    results.append(check("O(n) 풀이 만점", fast["score"], 100))

    slow = await_verdict(submit(QUADRATIC, problem="max-subarray")["id"])
    perf = next(g for g in slow["groups"] if g["groupId"] == "performance")
    results.append(check("O(n^2) 풀이 부분 점수", 0 < slow["score"] < 100, True))
    results.append(check("  performance 부분 득점", 0 < perf["score"] < perf["maxScore"], True))
    print(f"        (총점 {slow['score']}, performance {perf['score']}/{perf['maxScore']})")

    print("\n초안 자동 저장 CAS (§9.2, §9.4)")
    user = f"smoke-{uuid.uuid4().hex[:8]}"
    headers = {"X-User-Id": user}

    status, saved = raw_request(
        "PUT", "/workspaces/two-sum/KOTLIN", {"code": "fun a() {}", "version": None}, headers
    )
    results.append(check("첫 저장", (status, saved["version"]), (200, 1)))

    status, saved = raw_request(
        "PUT", "/workspaces/two-sum/KOTLIN", {"code": "fun b() {}", "version": 1}, headers
    )
    results.append(check("기대 버전으로 덮어쓰기", (status, saved["version"]), (200, 2)))

    # 다른 탭이 낡은 버전으로 저장하려는 상황. 조용히 이기면 안 된다.
    status, conflict = raw_request(
        "PUT", "/workspaces/two-sum/KOTLIN", {"code": "fun stale() {}", "version": 1}, headers
    )
    results.append(check("낡은 버전은 충돌", status, 409))
    results.append(check("  오류 코드", conflict["error"]["errorCode"], "DRAFT_VERSION_CONFLICT"))
    results.append(check("  현재 초안 동봉", conflict["current"]["code"], "fun b() {}"))

    status, draft = raw_request("GET", "/workspaces/two-sum/KOTLIN", None, headers)
    results.append(check("초안 조회", draft["code"], "fun b() {}"))
    results.append(check("  덮어쓰기 실패가 남긴 흔적 없음", draft["version"], 2))

    status, missing = raw_request("GET", "/workspaces/two-sum/PYTHON", None, headers)
    results.append(check("초안 없으면 204", status, 204))

    print("\n문제 목록·제출 기록 (§9.1 cursor)")
    needle = urllib.parse.quote("부분")
    page = request("GET", f"/problems?query={needle}")
    results.append(check("제목 검색", [p["id"] for p in page["items"]], ["max-subarray"]))

    first = request("GET", "/problems?limit=1")
    results.append(check("limit 반영", len(first["items"]), 1))
    results.append(check("  다음 커서 있음", first["nextCursor"] is not None, True))
    second = request("GET", f"/problems?limit=1&cursor={first['nextCursor']}")
    results.append(
        check("  커서로 이어보기", first["items"][0]["id"] != second["items"][0]["id"], True)
    )

    history = request("GET", "/submissions?problemId=two-sum&limit=3")
    results.append(check("제출 기록 조회", len(history["items"]) <= 3, True))
    verdicts = [item["verdict"] for item in history["items"]]
    results.append(check("  최신 제출이 먼저", verdicts[0] is not None, True))

    print("\n관리자 API 인증 (§11.2, §11.4)")
    # 등록과 공개를 모두 할 수 있는 계정. 권한이 과하게 열린 계정에서도 2인 승인이
    # 남아 있는지 보기 위해 일부러 이런 계정으로 등록한다.
    registrar_op = operators.with_roles("CONTENT_EDITOR", "PUBLISHER")
    registrar = registrar_op.headers
    publisher = operators.with_roles("PUBLISHER", other_than=registrar_op).headers
    editor_only = operators.with_roles("CONTENT_EDITOR", other_than=registrar_op).headers
    security = operators.with_roles("SECURITY_ADMIN").headers

    # 토큰 없이는 아무것도 못 한다. 이전 슬라이스는 X-Actor 헤더를 자칭하면 통과했다.
    status, _ = raw_request("GET", "/admin/audit")
    results.append(check("토큰 없는 요청 거부", status, 401))
    status, _ = raw_request(
        "GET", "/admin/audit", None, {"Authorization": "Bearer 모르는토큰모르는토큰모르는토큰"},
    )
    results.append(check("모르는 토큰 거부", status, 401))
    status, _ = raw_request(
        "POST", "/admin/problems/x/publish", {"version": 1, "reportDigest": "x"}, editor_only,
    )
    results.append(check("역할 없는 호출 거부", status, 403))
    status, _ = raw_request("GET", "/admin/audit", None, registrar)
    results.append(check("감사 로그는 보안 역할만", status, 403))

    print("\n콘텐츠 공개와 2인 승인 (§3.2, §11.2, §13.3)")
    pid = "smoke-" + uuid.uuid4().hex[:8]
    digest = uuid.uuid4().hex
    report = uuid.uuid4().hex

    status, registered = raw_request(
        "POST", f"/admin/problems/{pid}/versions",
        {"version": 1, "packageDigest": digest, "reportDigest": report}, registrar,
    )
    results.append(check("버전 등록", (status, registered["versionId"]), (200, f"{pid}@1")))

    status, denied = raw_request(
        "POST", f"/admin/problems/{pid}/publish", {"version": 1, "reportDigest": report}, registrar,
    )
    results.append(check("등록자 본인 공개 거부", status, 409))
    results.append(check("  사유에 2인 승인 언급", "두 사람" in denied["reason"], True))

    status, stale = raw_request(
        "POST", f"/admin/problems/{pid}/publish",
        {"version": 1, "reportDigest": uuid.uuid4().hex}, publisher,
    )
    results.append(check("보고서 digest 불일치 거부", status, 409))

    status, published = raw_request(
        "POST", f"/admin/problems/{pid}/publish", {"version": 1, "reportDigest": report}, publisher,
    )
    results.append(check("다른 사람이 공개", (status, published["publishedVersionId"]), (200, f"{pid}@1")))

    state = request("GET", f"/admin/problems/{pid}", None, publisher)
    results.append(check("  공개 버전 반영", state["publishedVersionId"], f"{pid}@1"))

    trail = request("GET", f"/admin/audit?subject={pid}@1", None, security)
    actions = [entry["action"] for entry in trail]
    results.append(check("감사 로그", sorted(actions), ["PROBLEM_PUBLISHED", "PROBLEM_VERSION_REGISTERED"]))

    # 공개된 문제만 목록에 나온다. 디렉터리에 파일을 놓는 것만으로 공개되면 §6.3 검증과
    # §11.2 승인이 모두 우회된다.
    listed = [p["id"] for p in request("GET", "/problems")["items"]]
    results.append(check("공개된 문제만 목록에", pid not in listed, True))
    results.append(check("  검증·공개된 문제는 보인다", "two-sum" in listed, True))

    print("\n재채점 승인 (§4.2, §11.2)")
    operator_op = operators.with_roles("JUDGE_OPERATOR")
    operator = operator_op.headers
    approver = operators.with_roles("REVIEWER", other_than=operator_op).headers

    status, job = raw_request(
        "POST", "/admin/rejudges",
        {"scope": "problem:two-sum", "reason": "테스트 데이터 수정"}, operator,
    )
    results.append(check("재채점 요청", job["status"], "REQUESTED"))

    status, self_approve = raw_request("POST", f"/admin/rejudges/{job['id']}/approve", None, operator)
    results.append(check("본인 승인 거부", status, 409))

    status, targets = raw_request("GET", f"/admin/rejudges/{job['id']}/targets", None, operator)
    results.append(check("승인 전 대상 없음", targets["count"], 0))

    status, approved = raw_request("POST", f"/admin/rejudges/{job['id']}/approve", None, approver)
    results.append(check("다른 사람이 승인", (status, approved["status"]), (200, "APPROVED")))

    status, targets = raw_request("GET", f"/admin/rejudges/{job['id']}/targets", None, operator)
    results.append(check("승인 후 대상 있음", targets["count"] > 0, True))

    print("\n실행 트레이스 (§7)")
    traced = submit(ACCEPTED_SOURCE)
    await_verdict(traced["id"])
    manifest = await_trace(traced["id"])
    results.append(check("트레이스 목차 도착", manifest is not None, True))
    if manifest:
        results.append(check("상태", manifest["status"], "READY"))
        results.append(check("스키마", manifest["schemaVersion"], "2.0"))
        results.append(check("이벤트 있음", manifest["eventCount"] > 0, True))
        results.append(check("공개 케이스만", manifest["caseId"].startswith("sample/"), True))
        results.append(check("요약 예산 준수", len(manifest["summary"]) <= 1000, True))

        # 목차의 청크를 실제로 받아 seq 범위와 맞는지 본다 (§7.5).
        chunks = manifest["chunks"]
        results.append(check("청크 목차 있음", len(chunks) > 0, True))
        status, chunk = raw_request("GET", f"/submissions/{traced['id']}/trace/chunks/0")
        results.append(check("청크 조회", status, 200))
        if chunk:
            events = chunk["events"]
            results.append(check("  목차와 개수 일치", len(events), chunks[0]["eventCount"]))
            results.append(check("  목차와 첫 seq 일치", events[0]["seq"], chunks[0]["firstSeq"]))
            kinds = sorted({e["targetKind"] for e in events})
            results.append(check("  자료구조 종류", kinds, ["ARRAY"]))

    # 다섯 렌더러를 모두 쓰는 문제로 종류가 전부 나오는지 본다 (§1.1, §7.5).
    islands = submit(
        open("content/problems/island-count/solutions/reference.kt").read(),
        problem="island-count",
    )
    final = await_verdict(islands["id"])
    results.append(check("다중 자료구조 문제 정답", final["verdict"], "ACCEPTED"))
    rich = await_trace(islands["id"])
    if rich:
        kinds = sorted({e["targetKind"] for e in rich["summary"]})
        results.append(
            check("  다섯 종류 모두", kinds, ["ARRAY", "CALL", "GRAPH", "QUEUE", "STACK"])
        )
        status, missing = raw_request("GET", f"/submissions/{traced['id']}/trace/chunks/999")
        results.append(check("  없는 청크는 404", status, 404))

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
