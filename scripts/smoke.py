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
import pathlib
import os
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid

import accounts
import operators

# 제어 영역 주소. scripts/up.py 가 포트를 비켜 갔으면 그 값을 넘겨 준다.
BASE = os.environ.get("CODEDRILL_BASE", "http://localhost:8080").rstrip("/") + "/api/v1"

# 이 스모크가 쓰는 계정. main() 이 새로 만든다. 제출·초안·기록은 전부 인증을 요구하므로,
# Authorization 을 따로 주지 않은 요청에는 이 계정의 토큰이 붙는다.
USER: accounts.Account | None = None

# 이 스모크가 실제로 제출하는 문제들.
REQUIRED_PROBLEMS = ["two-sum", "max-subarray", "island-count", "is-palindrome", "rotate-grid"]

# 판정 하나를 기다리는 상한. 지연 목표가 아니다 — 그것은 loadtest.py 가 잰다.
#
# Runner 는 하나이고 큐는 직렬이라, 쿼터 절에서 걸어 둔 끝나지 않는 풀이 다섯이 뒤의 제출
# 앞에 선다. 각각이 컴파일 한 번에 그룹 수만큼의 시간 초과다. 컴파일이 샌드박스 안으로
# 들어가며(§5.5) 그 값이 커졌고, 60초로는 그 뒤의 첫 제출이 간발로 넘겼다.
TIMEOUT = 120

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

PALINDROME_KOTLIN = """
fun isPalindrome(text: String): Int {
    val kept = text.filter { it.isLetterOrDigit() }.lowercase()
    return if (kept == kept.reversed()) 1 else 0
}
"""

PALINDROME_JAVA = """
class Solution {
    public int isPalindrome(String text) {
        StringBuilder kept = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (Character.isLetterOrDigit(c)) kept.append(Character.toLowerCase(c));
        }
        String forward = kept.toString();
        return forward.equals(kept.reverse().toString()) ? 1 : 0;
    }
}
"""

PALINDROME_PYTHON = """
def isPalindrome(text):
    kept = [c.lower() for c in text if c.isalnum()]
    return 1 if kept == kept[::-1] else 0
"""

REVERSE_WORDS_KOTLIN = """
fun reverseWords(words: Array<String>): Array<String> =
    Array(words.size) { words[words.size - 1 - it].reversed() }
"""

# 격자 왕복. 세 언어가 `<행>,<열>,<행 우선 원소>` 를 같게 읽고 쓰는지 본다.
# 직사각형 케이스가 들어 있어 행과 열을 맞바꾸면 통과하지 못한다.
ROTATE_KOTLIN = """
fun rotate(grid: Array<IntArray>): Array<IntArray> {
    val rows = grid.size
    val cols = if (rows == 0) 0 else grid[0].size
    return Array(cols) { c -> IntArray(rows) { r -> grid[rows - 1 - r][c] } }
}
"""

ROTATE_JAVA = """
class Solution {
    public int[][] rotate(int[][] grid) {
        int rows = grid.length;
        int cols = rows == 0 ? 0 : grid[0].length;
        int[][] out = new int[cols][rows];
        for (int c = 0; c < cols; c++) {
            for (int r = 0; r < rows; r++) out[c][r] = grid[rows - 1 - r][c];
        }
        return out;
    }
}
"""

ROTATE_PYTHON = """
def rotate(grid):
    return [list(row) for row in zip(*grid[::-1])]
"""

PERIMETER_KOTLIN = """
fun perimeter(grid: Array<IntArray>): Int {
    val rows = grid.size
    val cols = if (rows == 0) 0 else grid[0].size
    var total = 0
    for (r in 0 until rows) {
        for (c in 0 until cols) {
            if (grid[r][c] != 1) continue
            total += 4
            if (r > 0 && grid[r - 1][c] == 1) total -= 1
            if (r + 1 < rows && grid[r + 1][c] == 1) total -= 1
            if (c > 0 && grid[r][c - 1] == 1) total -= 1
            if (c + 1 < cols && grid[r][c + 1] == 1) total -= 1
        }
    }
    return total
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

# 같은 답, 다른 구조. 유사도 신호가 "같은 소스"와 "다른 접근"을 가르는지 보는 데 쓴다 (§11.4).
ACCEPTED_QUADRATIC = """
fun twoSum(nums: IntArray, target: Int): IntArray {
    for (i in nums.indices) {
        for (j in i + 1 until nums.size) {
            Drill.compare(i, j)
            if (nums[i] + nums[j] == target) {
                Drill.match(i, j)
                return intArrayOf(i, j)
            }
        }
    }
    error("정답은 항상 존재한다")
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


def with_auth(headers: dict | None) -> dict:
    """호출부가 신원을 지정하지 않았으면 기본 계정으로 보낸다."""
    merged = dict(headers or {})
    if USER and not any(k.lower() == "authorization" for k in merged):
        merged.update(USER.headers)
    return merged


def raw_request(method: str, path: str, body: dict | None = None, headers: dict | None = None):
    """상태 코드까지 봐야 하는 경로용. 409 는 오류가 아니라 결과의 한 종류다."""
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(f"{BASE}{path}", data=data, method=method)
    req.add_header("Content-Type", "application/json")
    for key, value in with_auth(headers).items():
        # 빈 값은 "이 헤더 없이 보내라"는 뜻이다. 인증 없는 요청을 시험할 때 쓴다.
        if value != "":
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
    for key, value in with_auth(headers).items():
        # 빈 값은 "이 헤더 없이 보내라"는 뜻이다. 인증 없는 요청을 시험할 때 쓴다.
        if value != "":
            req.add_header(key, value)
    with urllib.request.urlopen(req, timeout=10) as response:
        return json.loads(response.read())


_VERSIONS: dict[str, int] = {}


def version_of(problem: str) -> int:
    """지금 공개돼 있는 버전.

    예전에는 1 로 고정돼 있었다. 문제를 v2 로 올리는 순간 제출이 **존재하지 않는 조합**을
    가리키고, 오케스트레이터가 그 메시지를 거부해 dead-letter 로 보낸다. 제품은 멀쩡한데
    스모크가 낡은 것인데, 화면에는 "판정이 안 온다"로만 보인다.
    """
    if problem not in _VERSIONS:
        _VERSIONS[problem] = request("GET", f"/problems/{problem}")["version"]
    return _VERSIONS[problem]


def submit(
    source: str,
    key: str | None = None,
    language: str = "KOTLIN",
    problem: str = "two-sum",
    headers: dict | None = None,
) -> dict:
    return request(
        "POST",
        "/submissions",
        {
            "problemId": problem,
            "problemVersion": version_of(problem),
            "language": language,
            "source": source,
        },
        {**(headers or {}), "Idempotency-Key": key or str(uuid.uuid4())},
    )


def all_problems() -> list[dict]:
    """공개된 문제 전부. 한 페이지가 100 이라 커서를 따라간다 (§9.1)."""
    items: list[dict] = []
    cursor = None
    while True:
        page = request("GET", "/problems?limit=100" + (f"&cursor={cursor}" if cursor else ""))
        items += page["items"]
        cursor = page.get("nextCursor")
        if not cursor:
            return items


def await_verdict(submission_id: str, headers: dict | None = None) -> dict:
    deadline = time.time() + TIMEOUT
    last = {}
    while time.time() < deadline:
        last = request("GET", f"/submissions/{submission_id}", None, headers)
        if last["status"] == "COMPLETED":
            return last
        time.sleep(0.5)
    raise TimeoutError(f"{TIMEOUT}초 안에 판정이 끝나지 않았다: {last.get('status')}")


def read_events(submission_id: str, limit: int = 4) -> list[str]:
    """SSE 스트림에서 이벤트 이름을 모은다. 종료 이벤트를 만나면 멈춘다."""
    seen: list[str] = []
    req = urllib.request.Request(f"{BASE}/submissions/{submission_id}/events")
    # 스트림도 소유자만 열 수 있다 (§11.4). 다른 요청과 같은 토큰을 실어 보낸다.
    for key, value in with_auth(None).items():
        req.add_header(key, value)
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


def await_rejudge(job_id: str, headers: dict, timeout: int = TIMEOUT) -> dict:
    """재채점 작업이 끝나기를 기다린다. 끝나지 않아도 마지막 보고서를 돌려준다."""
    deadline = time.time() + timeout
    report = request("GET", f"/admin/rejudges/{job_id}", None, headers)
    while time.time() < deadline:
        if report["job"]["status"] in ("COMPLETED", "REJECTED"):
            return report
        time.sleep(0.3)
        report = request("GET", f"/admin/rejudges/{job_id}", None, headers)
    return report


def check(label: str, actual, expected) -> bool:
    ok = actual == expected
    print(f"  {'PASS' if ok else 'FAIL'}  {label}: {actual}" + ("" if ok else f" (기대: {expected})"))
    return ok


def main() -> int:
    results: list[bool] = []

    global USER
    USER = accounts.create("smoke")
    print(f"계정 {USER.display_name} <{USER.email}> 으로 진행한다")

    print("\n인증과 소유권 (§11.2, §11.4)")
    status, _ = raw_request("GET", "/submissions", None, {"Authorization": ""})
    results.append(check("토큰 없는 제출 조회 거부", status, 401))
    status, denied = raw_request(
        "GET", "/submissions", None, {"Authorization": "Bearer not-a-real-token"},
    )
    results.append(check("모르는 토큰 거부", status, 401))
    results.append(check("  오류 코드", denied["errorCode"], "UNAUTHENTICATED"))

    me = request("GET", "/auth/me")
    results.append(check("내 정보 조회", me["id"], USER.user_id))

    # 문제 목록은 로그인 없이 열린다. 무엇을 풀 수 있는지 둘러보는 것은 공개 정보다.
    status, _ = raw_request("GET", "/problems", None, {"Authorization": ""})
    results.append(check("문제 목록은 공개", status, 200))

    print("문제 조회")
    # 한 페이지 크기보다 문제가 많다. 목록을 통째로 고정하면 문제를 추가할 때마다
    # 스모크가 깨지므로, 이 스모크가 실제로 쓰는 문제들이 있는지만 본다.
    # 한 페이지는 100 이고 문제는 그보다 많다 — 커서를 따라 끝까지 읽는다.
    problems = all_problems()
    slugs = sorted(p["id"] for p in problems)
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

    print("\n문자열 값 타입 (§6.1)")
    # 탭·쉼표·비ASCII 가 든 경계 케이스를 통과해야 인코딩이 산 것이다. 그 케이스는
    # 문제 패키지에 들어 있으므로, 전부 통과한다는 것이 곧 확인이다.
    for language, source in [
        ("KOTLIN", PALINDROME_KOTLIN),
        ("JAVA", PALINDROME_JAVA),
        ("PYTHON", PALINDROME_PYTHON),
    ]:
        final = await_verdict(submit(source, language=language, problem="is-palindrome")["id"])
        results.append(check(f"{language} 문자열 입력", final["verdict"], "ACCEPTED"))

    words = await_verdict(submit(REVERSE_WORDS_KOTLIN, problem="reverse-words")["id"])
    results.append(check("문자열 배열 입출력", words["verdict"], "ACCEPTED"))

    print("\n격자 값 타입 (§6.1)")
    detail = request("GET", "/problems/rotate-grid")
    results.append(
        check("격자 시그니처", detail["signature"], "fun rotate(grid: Array<IntArray>): Array<IntArray>")
    )
    # 격자를 받아 격자를 돌려준다. 빈 격자와 직사각형 케이스가 패키지에 들어 있으므로
    # 전부 통과한다는 것이 곧 크기가 제대로 실렸다는 확인이다.
    for language, source in [
        ("KOTLIN", ROTATE_KOTLIN),
        ("JAVA", ROTATE_JAVA),
        ("PYTHON", ROTATE_PYTHON),
    ]:
        final = await_verdict(submit(source, language=language, problem="rotate-grid")["id"])
        results.append(check(f"{language} 격자 입출력", final["verdict"], "ACCEPTED"))

    grid = await_verdict(submit(PERIMETER_KOTLIN, problem="island-perimeter")["id"])
    results.append(check("격자 입력, 정수 출력", grid["verdict"], "ACCEPTED"))

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

    print("\n개인 데이터 반출·삭제 (§11.3)")
    leaver = accounts.create("leaver")
    leaver_auth = leaver.headers
    raw_request("POST", "/submissions",
                {"problemId": "two-sum", "problemVersion": version_of("two-sum"), "language": "KOTLIN",
                 "source": ACCEPTED_SOURCE},
                {**leaver_auth, "Idempotency-Key": str(uuid.uuid4())})

    dump = request("GET", "/auth/me/export", None, leaver_auth)
    results.append(check("반출에 계정이 있다", "account" in dump, True))
    results.append(check("  제출도 함께", len(dump.get("submissions", [])) >= 1, True))
    results.append(check("  소스가 실린다", bool(dump["submissions"][0].get("source")), True))

    # 되돌릴 수 없는 요청이다. 자리를 비운 사이 남이 만졌을 때 막을 것이 세션 하나뿐이면
    # 안 된다 (§11.3).
    status, _ = raw_request("DELETE", "/auth/me", {"password": "definitely-not-the-password"}, leaver_auth)
    results.append(check("비밀번호 없이는 못 지운다", status, 401))

    status, erased = raw_request("DELETE", "/auth/me", {"password": leaver.password}, leaver_auth)
    results.append(check("계정 삭제", status, 200))
    results.append(check("  제출 소스를 지웠다", erased["erased"]["submissions.submissionSources"] >= 1, True))
    # 소스는 스토어에도 실행용 복제가 있다 (§8.3). 삭제는 둘을 함께 지운다.
    results.append(check("  스토어의 복제도 지웠다", erased["erased"]["submissions.storedSources"] >= 1, True))
    results.append(check("  세션을 끊었다", erased["erased"]["sessions"] >= 1, True))

    status, _ = raw_request("POST", "/auth/login", {"email": leaver.email, "password": leaver.password},
                            {"Authorization": ""})
    results.append(check("지운 계정으로 로그인 불가", status, 401))
    status, _ = raw_request("GET", "/auth/me/export", None, leaver_auth)
    results.append(check("옛 토큰도 안 통한다", status, 401))

    print("\n제출 쿼터 (§10.2 남용 방어)")
    # 한 사람이 판정 큐를 혼자 채우지 못해야 한다. 끝나지 않는 풀이로 동시 진행 수를
    # 채운 뒤, 그 다음 제출이 거절되는지 본다.
    quota_user = accounts.create("quota")
    quota_auth = quota_user.headers
    forever = "fun twoSum(nums: IntArray, target: Int): IntArray { while (true) {} }"
    body = {"problemId": "two-sum", "problemVersion": version_of("two-sum"), "language": "KOTLIN", "source": forever}

    key = str(uuid.uuid4())
    status, first = raw_request("POST", "/submissions", body,
                                {**quota_auth, "Idempotency-Key": key})
    results.append(check("첫 제출은 통과", status, 202))

    rejected = None
    for _ in range(12):
        status, payload = raw_request("POST", "/submissions", body,
                                      {**quota_auth, "Idempotency-Key": str(uuid.uuid4())})
        if status == 429:
            rejected = payload
            break
    results.append(check("쿼터가 막는다", rejected is not None, True))
    if rejected:
        results.append(check("  오류 코드", rejected["errorCode"], "QUOTA_EXCEEDED"))

    # 멱등 재시도는 새 제출이 아니다. 네트워크가 흔들려 다시 보낸 클라이언트를 쿼터로
    # 막으면 멱등성이 있으나 마나다 (§4.3).
    status, retry = raw_request("POST", "/submissions", body,
                                {**quota_auth, "Idempotency-Key": key})
    results.append(check("멱등 재시도는 막지 않는다", status, 202))
    results.append(check("  같은 제출을 돌려준다", retry["id"], first["id"]))

    print("\n초안 자동 저장 CAS (§9.2, §9.4)")
    # 초안은 사용자별로 키가 잡혀 있다. 신원은 토큰이 정한다.
    headers = None

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
    page = request("GET", f"/problems?query={needle}&limit=100")
    found = [p["id"] for p in page["items"]]
    # 결과 목록을 통째로 고정하지 않는다. 제목에 같은 말이 든 문제를 추가할 때마다
    # 깨지고, 그건 검색이 고장난 것이 아니다.
    results.append(check("제목 검색이 찾는다", "max-subarray" in found, True))
    results.append(
        check("  결과가 전부 질의를 담는다", all("부분" in p["title"] for p in page["items"]), True)
    )

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

    print("\n수평 권한 (§11.4 객체 소유권)")
    intruder = accounts.create("intruder")
    mine = submit(ACCEPTED_SOURCE)
    await_verdict(mine["id"])

    for label, path in [
        ("제출 조회", f"/submissions/{mine['id']}"),
        ("판정 이력", f"/submissions/{mine['id']}/judgements"),
        ("트레이스 목차", f"/submissions/{mine['id']}/trace"),
        ("상태 스트림", f"/submissions/{mine['id']}/events"),
    ]:
        status, _ = raw_request("GET", path, None, intruder.headers)
        # 403 이 아니라 404 다. "있는데 네 것이 아니다"를 알려 주면 ID 를 훑어
        # 누가 무엇을 제출했는지 셀 수 있다 (§11.3 비공개 기본).
        results.append(check(f"남의 {label} 는 없는 것처럼", status, 404))

    others = request("GET", "/submissions", None, intruder.headers)
    results.append(check("남의 기록은 안 보인다", others["items"], []))

    others_draft = raw_request("GET", "/workspaces/two-sum/KOTLIN", None, intruder.headers)
    results.append(check("남의 초안도 안 보인다", others_draft[0], 204))

    print("\n관리자 API 인증 (§11.2, §11.4)")
    # 등록과 공개를 모두 할 수 있는 계정. 권한이 과하게 열린 계정에서도 2인 승인이
    # 남아 있는지 보기 위해 일부러 이런 계정으로 등록한다.
    registrar_op = operators.with_roles("CONTENT_EDITOR", "PUBLISHER")
    registrar = registrar_op.headers
    publisher = operators.with_roles("PUBLISHER", other_than=registrar_op).headers
    editor_only = operators.with_roles("CONTENT_EDITOR", other_than=registrar_op).headers
    security_op = operators.with_roles("SECURITY_ADMIN")
    security = security_op.headers
    # 역할 부여에도 2인 승인이 걸려 있어 SECURITY_ADMIN 이 둘 필요하다 (§11.2).
    approver_op = operators.with_roles("SECURITY_ADMIN", other_than=security_op)
    approver = approver_op.headers

    # 토큰 없이는 아무것도 못 한다. 이전 슬라이스는 X-Actor 헤더를 자칭하면 통과했다.
    # 빈 값은 "이 헤더 없이 보내라"는 뜻이다 — 기본값은 스모크 사용자의 토큰이라,
    # 비우지 않으면 "로그인은 했지만 역할이 없다"(403)를 시험하게 된다.
    status, _ = raw_request("GET", "/admin/audit", None, {"Authorization": ""})
    results.append(check("토큰 없는 요청 거부", status, 401))
    # 로그인한 일반 사용자도 관리자 API 는 못 연다. 로그인했다는 사실만으로 열리면
    # 관리자 API 는 사실상 모두에게 열려 있는 셈이다 (§11.2).
    status, denied = raw_request("GET", "/admin/audit")
    results.append(check("역할 없는 계정 거부", status, 403))
    results.append(check("  사유가 역할을 지목", "역할이 없다" in denied["message"], True))
    status, _ = raw_request(
        "GET", "/admin/audit", None, {"Authorization": "Bearer unknown-token-that-is-long-enough"},
    )
    results.append(check("모르는 토큰 거부", status, 401))
    status, _ = raw_request(
        "POST", "/admin/problems/x/publish",
        {"version": 1, "reportDigest": "x", "validatorVersion": "1"}, editor_only,
    )
    results.append(check("역할 없는 호출 거부", status, 403))
    status, _ = raw_request("GET", "/admin/audit", None, registrar)
    results.append(check("감사 로그는 보안 역할만", status, 403))

    # 역할을 스스로 늘릴 수 있으면 2인 승인은 사람 수를 세지 못한다 (§11.2).
    status, refused = raw_request(
        "POST", f"/admin/operators/{security_op.user_id}/roles",
        {"role": "PUBLISHER", "reason": "스모크"}, security,
    )
    results.append(check("자기 자신에게 역할 부여 거부", status, 409))
    results.append(check("  사유가 자기 부여를 지목", "자기 자신" in refused["reason"], True))

    # 남에게 주는 것도 혼자서는 못 한다. 계정 등록은 누구에게나 열려 있어, 혼자
    # 두 번째 계정을 만들어 역할을 붙이면 등록자·승인자 비교는 그 둘을 두 사람으로
    # 센다 — 그래서 권한이 늘어나는 순간 자체를 두 사람이 밟는다.
    other = operators.with_roles("CONTENT_EDITOR")
    status, requested = raw_request(
        "POST", f"/admin/operators/{other.user_id}/roles",
        {"role": "REVIEWER", "reason": "스모크"}, security,
    )
    results.append(check("역할 부여는 요청으로 접수된다", status, 202))
    results.append(check("  아직 부여되지 않았다", requested["changed"], False))

    status, _ = raw_request(
        "POST", f"/admin/role-requests/{requested['requestId']}/approve", None, security,
    )
    results.append(check("요청자가 자기 요청 승인 거부", status, 409))

    status, _ = raw_request(
        "POST", f"/admin/role-requests/{requested['requestId']}/approve", None, approver,
    )
    results.append(check("다른 SECURITY_ADMIN 이 승인하면 부여된다", status, 200))

    # 권한을 줄이는 것은 승인을 기다리지 않는다. 사고가 났을 때 가장 급한 조치다.
    status, _ = raw_request(
        "DELETE", f"/admin/operators/{other.user_id}/roles/REVIEWER", None, security,
    )
    results.append(check("회수는 즉시다", status, 200))

    # 담합: SECURITY_ADMIN 둘이 서로에게 주면 각자 권한이 늘고, 그러면 권한을 키우는
    # 데 필요한 사람 수가 다시 둘로 돌아간다. 수혜자는 자기 승격에 표를 못 던진다.
    status, mutual = raw_request(
        "POST", f"/admin/operators/{approver_op.user_id}/roles",
        {"role": "PUBLISHER", "reason": "담합 시험"}, security,
    )
    results.append(check("서로 주기도 요청까지는 된다", status, 202))
    status, blocked = raw_request(
        "POST", f"/admin/role-requests/{mutual['requestId']}/approve", None, approver,
    )
    results.append(check("수혜자가 자기 승격 승인 거부", status, 409))
    results.append(check("  사유가 자기 승격을 지목", "자기 승격" in blocked["reason"], True))
    status, _ = raw_request(
        "POST", f"/admin/role-requests/{mutual['requestId']}/reject",
        {"reason": "스모크 정리"}, security,
    )
    results.append(check("반려는 요청자도 할 수 있다", status, 200))

    print("\n콘텐츠 공개와 2인 승인 (§3.2, §11.2, §13.3)")
    pid = "smoke-" + uuid.uuid4().hex[:8]
    digest = uuid.uuid4().hex
    report = uuid.uuid4().hex
    validator = "1"

    status, registered = raw_request(
        "POST", f"/admin/problems/{pid}/versions",
        {"version": 1, "packageDigest": digest, "reportDigest": report,
         "validatorVersion": validator}, registrar,
    )
    results.append(check("버전 등록", (status, registered["versionId"]), (200, f"{pid}@1")))

    status, denied = raw_request(
        "POST", f"/admin/problems/{pid}/publish",
        {"version": 1, "reportDigest": report, "validatorVersion": validator}, registrar,
    )
    results.append(check("등록자 본인 공개 거부", status, 409))
    results.append(check("  사유에 2인 승인 언급", "두 사람" in denied["reason"], True))

    status, stale = raw_request(
        "POST", f"/admin/problems/{pid}/publish",
        {"version": 1, "reportDigest": uuid.uuid4().hex, "validatorVersion": validator}, publisher,
    )
    results.append(check("보고서 digest 불일치 거부", status, 409))
    # digest 가 어긋나는 원인은 둘이다. 파이프라인이 같으면 패키지가 바뀐 것이고,
    # 그 사실을 사유가 말해야 무엇을 고칠지 알 수 있다 (§15.3).
    results.append(check("  사유가 패키지를 지목", "패키지가 바뀐 것" in stale["reason"], True))

    status, moved = raw_request(
        "POST", f"/admin/problems/{pid}/publish",
        {"version": 1, "reportDigest": report, "validatorVersion": "99"}, publisher,
    )
    results.append(check("파이프라인 버전 불일치 거부", status, 409))
    results.append(check("  사유가 파이프라인을 지목", "파이프라인이 바뀌었다" in moved["reason"], True))
    results.append(check("  양쪽 버전을 밝힌다", "등록은 1, 지금은 99" in moved["reason"], True))

    status, published = raw_request(
        "POST", f"/admin/problems/{pid}/publish",
        {"version": 1, "reportDigest": report, "validatorVersion": validator}, publisher,
    )
    results.append(check("다른 사람이 공개", (status, published["publishedVersionId"]), (200, f"{pid}@1")))

    state = request("GET", f"/admin/problems/{pid}", None, publisher)
    results.append(check("  공개 버전 반영", state["publishedVersionId"], f"{pid}@1"))

    # 파이프라인만 바뀐 재검증은 새 버전이 아니라 새 보고서다 (§15.3).
    revalidated = uuid.uuid4().hex
    status, again = raw_request(
        "POST", f"/admin/problems/{pid}/versions",
        {"version": 1, "packageDigest": digest, "reportDigest": revalidated,
         "validatorVersion": "2"}, registrar,
    )
    results.append(check("새 파이프라인으로 재검증 등록", (status, again["versionId"]), (200, f"{pid}@1")))
    status, changed = raw_request(
        "POST", f"/admin/problems/{pid}/versions",
        {"version": 1, "packageDigest": uuid.uuid4().hex, "reportDigest": revalidated,
         "validatorVersion": "2"}, registrar,
    )
    results.append(check("  패키지가 다르면 거부", status, 409))
    results.append(check("  사유가 새 버전을 요구", "새 버전이어야" in changed["reason"], True))

    # 패키지 digest 는 manifest 와 tests 만 덮는다. 참조 풀이나 오답을 고치면 패키지는
    # 그대로인 채 보고서만 달라지므로, 파이프라인이 같아도 다시 받아야 한다 (§6.1, §6.3).
    status, inputs = raw_request(
        "POST", f"/admin/problems/{pid}/versions",
        {"version": 1, "packageDigest": digest, "reportDigest": uuid.uuid4().hex,
         "validatorVersion": "2"}, registrar,
    )
    results.append(check("  검증 입력만 바뀐 재등록", (status, inputs["versionId"]), (200, f"{pid}@1")))

    trail = request("GET", f"/admin/audit?subject={pid}@1", None, security)
    actions = [entry["action"] for entry in trail]
    # 재검증이 두 번이다. 파이프라인이 바뀐 것과 검증 입력만 바뀐 것 — 둘 다 같은 행위로
    # 남고, 무엇이 옮겨갔는지는 detail 이 말한다.
    results.append(check(
        "감사 로그", sorted(actions),
        ["PROBLEM_PUBLISHED", "PROBLEM_VERSION_REGISTERED",
         "PROBLEM_VERSION_REVALIDATED", "PROBLEM_VERSION_REVALIDATED"],
    ))
    # detail 은 jsonb 를 문자열로 내려보낸다. 어느 파이프라인에서 어디로 옮겼는지가
    # 남아야 "언제 기준이 바뀌었나"를 로그만으로 되짚을 수 있다 (§13.3).
    moves = [e for e in trail if e["action"] == "PROBLEM_VERSION_REVALIDATED"]
    results.append(check(
        "  파이프라인 이동이 남는다",
        any('"validatorVersion": "1 -> 2"' in e["detail"] for e in moves), True,
    ))
    results.append(check(
        "  입력만 바뀐 재검증은 파이프라인을 그대로 적는다",
        any('"validatorVersion": "2"' in e["detail"] for e in moves), True,
    ))

    # 공개된 문제만 목록에 나온다. 디렉터리에 파일을 놓는 것만으로 공개되면 §6.3 검증과
    # §11.2 승인이 모두 우회된다.
    listed = [p["id"] for p in all_problems()]
    results.append(check("공개된 문제만 목록에", pid not in listed, True))
    results.append(check("  검증·공개된 문제는 보인다", "two-sum" in listed, True))

    print("\n재채점 승인 (§4.2, §11.2)")
    # 요청과 승인을 둘 다 할 수 있는 계정. 권한이 열려 있어도 요청자·승인자 분리가
    # 남아 있는지 보기 위해 이 계정으로 요청한다.
    operator_op = operators.with_roles("JUDGE_OPERATOR", "REVIEWER")
    operator = operator_op.headers
    approver = operators.with_roles("REVIEWER", other_than=operator_op).headers

    status, job = raw_request(
        "POST", "/admin/rejudges",
        {"scope": "problem:two-sum", "reason": "테스트 데이터 수정"}, operator,
    )
    results.append(check("재채점 요청", job["status"], "REQUESTED"))

    status, _ = raw_request("POST", f"/admin/rejudges/{job['id']}/approve", None, editor_only)
    results.append(check("승인 역할 없는 계정 거부", status, 403))

    status, self_approve = raw_request("POST", f"/admin/rejudges/{job['id']}/approve", None, operator)
    results.append(check("본인 승인 거부", status, 409))
    results.append(check("  사유에 2인 승인 언급", "두 사람" in self_approve["reason"], True))

    status, targets = raw_request("GET", f"/admin/rejudges/{job['id']}/targets", None, operator)
    results.append(check("승인 전 대상 없음", targets["count"], 0))

    status, approved = raw_request("POST", f"/admin/rejudges/{job['id']}/approve", None, approver)
    results.append(check("다른 사람이 승인", (status, approved["status"]), (200, "APPROVED")))

    status, targets = raw_request("GET", f"/admin/rejudges/{job['id']}/targets", None, operator)
    results.append(check("승인 후 대상 있음", targets["count"] > 0, True))

    print("\n재채점 실행 (§4.2 INV-02)")
    # 방금 판정된 제출 하나를 대상으로 삼는다. 범위가 좁아야 무엇이 바뀌었는지 명확하다.
    subject = submit(ACCEPTED_SOURCE)
    before = await_verdict(subject["id"])
    results.append(check("대상 최초 판정", (before["verdict"], before["revision"]), ("ACCEPTED", 1)))

    history = request("GET", f"/submissions/{subject['id']}/judgements")
    results.append(check("  최초 판정도 이력에 있다", len(history), 1))
    results.append(check("  재채점 표시 없음", history[0]["rejudgeJobId"], None))

    # dry-run: 판정을 바꾸지 않고 무엇이 바뀔지만 본다.
    _, dry = raw_request(
        "POST", "/admin/rejudges",
        {"scope": f"submission:{subject['id']}", "reason": "dry-run 확인", "dryRun": True},
        operator,
    )
    raw_request("POST", f"/admin/rejudges/{dry['id']}/approve", None, approver)
    status, dispatched = raw_request("POST", f"/admin/rejudges/{dry['id']}/dispatch", None, operator)
    results.append(check("dry-run 실행", (status, dispatched["targets"]), (202, 1)))

    await_rejudge(dry["id"], operator)
    after_dry = request("GET", f"/submissions/{subject['id']}")
    results.append(check("  현재 판정 그대로", after_dry["revision"], 1))
    dry_history = request("GET", f"/submissions/{subject['id']}/judgements")
    results.append(check("  이력에는 남는다", len(dry_history), 2))
    results.append(check("  반영되지 않음 표시", dry_history[1]["applied"], False))

    # 실제 재채점: revision 이 오르고 이력이 쌓인다.
    _, real = raw_request(
        "POST", "/admin/rejudges",
        {"scope": f"submission:{subject['id']}", "reason": "테스트 데이터 수정"}, operator,
    )
    raw_request("POST", f"/admin/rejudges/{real['id']}/approve", None, approver)
    status, _ = raw_request("POST", f"/admin/rejudges/{real['id']}/dispatch", None, operator)
    results.append(check("재채점 실행", status, 202))

    report = await_rejudge(real["id"], operator)
    results.append(check("  작업이 끝난다", report["job"]["status"], "COMPLETED"))
    after = request("GET", f"/submissions/{subject['id']}")
    results.append(check("  revision 이 오른다", after["revision"], 2))
    results.append(check("  판정은 같다", after["verdict"], "ACCEPTED"))
    results.append(check("  판정이 바뀌지 않았다고 보고", report["changes"], []))

    final_history = request("GET", f"/submissions/{subject['id']}/judgements")
    results.append(check("  이력 세 건", len(final_history), 3))
    results.append(check("  마지막은 반영됨", final_history[2]["applied"], True))
    results.append(check("  재채점 작업 표시", final_history[2]["rejudgeJobId"], real["id"]))

    status, again = raw_request("POST", f"/admin/rejudges/{real['id']}/dispatch", None, operator)
    results.append(check("끝난 작업은 다시 실행 못 함", status, 409))

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

    print("\n아레나에 남의 오답 세우기 (§8.3, §8.5 신고·검수)")
    # 틀린 사람이 내놓고, 검수자가 세우고, 맞힌 사람이 깨뜨리거나 신고한다. 세 사람이다.
    donor = accounts.create("donor")
    wrong = submit(WRONG_SOURCE, headers=donor.headers)
    await_verdict(wrong["id"], headers=donor.headers)
    status, refused = raw_request("POST", "/arena/two-sum/donations",
                                  {"submissionId": wrong["id"], "note": "짧다"}, donor.headers)
    results.append(check("사유가 짧으면 거절", status, 400))
    status, refused = raw_request("POST", "/arena/two-sum/donations",
                                  {"submissionId": wrong["id"], "note": "남이 낸 것을 내놓을 수는 없다"})
    results.append(check("남의 제출은 내놓지 못한다", status, 400))
    status, donation = raw_request("POST", "/arena/two-sum/donations",
                                   {"submissionId": wrong["id"], "note": "인덱스를 보지 않고 0, 0 을 돌려준다"}, donor.headers)
    results.append(check("내 오답을 내놓는다", status, 202))
    results.append(check("  소스는 응답에 없다", donation.get("source"), None))
    status, again = raw_request("POST", "/arena/two-sum/donations",
                                {"submissionId": wrong["id"], "note": "인덱스를 보지 않고 0, 0 을 돌려준다"}, donor.headers)
    results.append(check("  같은 제출은 한 번만", status, 409))

    # 맞힌 사람. 아직 과녁은 없다 — 검수 전이다.
    await_verdict(submit(ACCEPTED_SOURCE)["id"])
    board = request("GET", "/arena/two-sum")
    results.append(check("검수 전에는 과녁이 아니다", any(t["community"] for t in board["targets"]), False))

    reviewer = operators.with_roles("REVIEWER").headers
    status, _ = raw_request("GET", "/admin/arena/queue")
    results.append(check("검수 큐는 검수자만", status, 403))
    queue = request("GET", "/admin/arena/queue", None, reviewer)
    results.append(check("검수 큐에 올라 있다", any(d["id"] == donation["id"] for d in queue["pending"]), True))
    results.append(check("  검수자는 소스를 본다",
                         next(d for d in queue["pending"] if d["id"] == donation["id"])["source"], WRONG_SOURCE))
    status, bad_kind = raw_request("POST", f"/admin/arena/donations/{donation['id']}/approve",
                                   {"kind": "PERFORMANCE"}, reviewer)
    results.append(check("손으로 못 깨뜨리는 종류로는 못 세운다", status, 409))
    approved = request("POST", f"/admin/arena/donations/{donation['id']}/approve",
                       {"kind": "WRONG_ALGORITHM", "note": "무엇을 받든 0, 0 이다"}, reviewer)
    results.append(check("세웠다", approved["status"], "APPROVED"))
    target_name = approved["targetName"]

    board = request("GET", "/arena/two-sum")
    target = next((t for t in board["targets"] if t["name"] == target_name), None)
    results.append(check("과녁이 됐다", target is not None, True))
    if target:
        results.append(check("  누군가의 오답으로 표시", target["community"], True))
        results.append(check("  검수자의 설명이 과녁의 설명", target["note"], "무엇을 받든 0, 0 이다"))
        attempt = request("POST", "/arena/two-sum/attempts", {"args": [[2, 7, 11, 15], 9]})
        deadline = time.time() + TIMEOUT
        while attempt["status"] == "PENDING" and time.time() < deadline:
            time.sleep(1)
            attempt = request("GET", f"/arena/attempts/{attempt['id']}")
        broken = next((r for r in attempt["results"] if r["name"] == target_name), None)
        results.append(check("  깨뜨렸다", bool(broken and broken["broken"]), True))
        mine = request("GET", "/arena/two-sum/donations/mine", None, donor.headers)
        results.append(check("기부자에게 세워졌다고 보인다", mine[0]["status"], "APPROVED"))

    status, report = raw_request("POST", f"/arena/two-sum/targets/{target_name}/reports",
                                 {"reason": "이건 오답이 아니라 자리표다"}, donor.headers)
    results.append(check("못 맞힌 사람은 신고하지 못한다", status, 409))
    status, report = raw_request("POST", f"/arena/two-sum/targets/{target_name}/reports",
                                 {"reason": "이건 오답이 아니라 자리표다"})
    results.append(check("맞힌 사람은 신고한다", status, 202))
    status, _ = raw_request("POST", f"/arena/two-sum/targets/{target_name}/reports",
                            {"reason": "이건 오답이 아니라 자리표다"})
    results.append(check("  두 번째 신고는 조용히", status, 204))
    queue = request("GET", "/admin/arena/queue", None, reviewer)
    results.append(check("신고가 큐에 올라 있다", any(r["report"]["id"] == report["id"] for r in queue["reports"]), True))
    retired = request("POST", f"/admin/arena/reports/{report['id']}/resolve",
                      {"retire": True, "resolution": "확인. 내린다"}, reviewer)
    results.append(check("신고를 받아 내렸다", retired["status"], "RETIRED"))
    board = request("GET", "/arena/two-sum")
    results.append(check("  과녁에서 사라졌다", any(t["name"] == target_name for t in board["targets"]), False))
    mine = request("GET", "/arena/two-sum/donations/mine", None, donor.headers)
    results.append(check("  기부자에게 사유가 보인다", mine[0]["reason"], "확인. 내린다"))

    print("\n문제별 질문 게시판 (§8.5 질문·코드 구간 링크·리플레이 시점)")
    # 막힌 사람이 묻고, 맞힌 사람이 답한다. 기본 계정은 위에서 two-sum 을 맞혔고, asker 는 아직이다.
    asker = accounts.create("asker")
    asked_wrong = submit(WRONG_SOURCE, headers=asker.headers)
    await_verdict(asked_wrong["id"], headers=asker.headers)
    # 토큰 없는 요청이 401 인지 먼저 본다. 인터셉터 경로에 빠져 있으면 principal 이 없어
    # 모든 요청이 400 이고, 그러면 아래의 "거절" 검사들이 엉뚱한 이유로 초록이 된다.
    status, _ = raw_request("GET", "/discussions/two-sum", None, {"Authorization": ""})
    results.append(check("토큰 없는 요청 거부", status, 401))
    status, _ = raw_request("POST", "/discussions/two-sum", {"title": "왜", "body": "왜 0, 0 이 나오나요 이해가 안 됩니다"}, asker.headers)
    results.append(check("제목이 짧으면 거절", status, 400))
    status, _ = raw_request("POST", "/discussions/two-sum",
                            {"title": "왜 0, 0 이 나오나요", "body": "본문이 열 자 이상이어야 합니다",
                             "anchor": {"submissionId": wrong["id"], "lineFrom": 1, "lineTo": 1}}, asker.headers)
    results.append(check("남의 제출은 붙이지 못한다", status, 400))
    status, _ = raw_request("POST", "/discussions/two-sum",
                            {"title": "왜 0, 0 이 나오나요", "body": "본문이 열 자 이상이어야 합니다",
                             "anchor": {"submissionId": asked_wrong["id"], "lineFrom": 1, "lineTo": 999}}, asker.headers)
    results.append(check("줄 범위 밖은 거절", status, 400))
    status, question = raw_request("POST", "/discussions/two-sum",
                                   {"title": "왜 0, 0 이 나오나요", "body": "두 번째 줄에서 무엇을 돌려주는지 모르겠습니다",
                                    "anchor": {"submissionId": asked_wrong["id"], "lineFrom": 1, "lineTo": 1, "step": 0}},
                                   asker.headers)
    results.append(check("코드 구간을 붙여 묻는다", status, 201))
    results.append(check("  구간의 코드가 실린다", question["anchor"]["excerpt"], WRONG_SOURCE.strip()))
    results.append(check("  글쓴이의 id 는 나가지 않는다", "authorId" in question, False))
    listed = request("GET", "/discussions/two-sum")
    results.append(check("목록에 올랐다", any(q["id"] == question["id"] for q in listed), True))
    results.append(check("  남에게는 내 것이 아니다", next(q["mine"] for q in listed if q["id"] == question["id"]), False))

    # 맞힌 사람이 풀이를 드러내는 답을 단다. 붙인 리플레이 시점은 맞힌 사람에게만 열린다.
    accepted = submit(ACCEPTED_SOURCE)
    await_verdict(accepted["id"])
    status, answer = raw_request("POST", f"/discussions/threads/{question['id']}/answers",
                                 {"body": "해시맵에 넣기 전에 먼저 찾아야 합니다. 이 걸음을 보세요",
                                  "anchor": {"submissionId": accepted["id"], "step": 3}, "spoiler": True})
    results.append(check("풀이를 드러내는 답을 단다", status, 201))
    thread = request("GET", f"/discussions/threads/{question['id']}", None, asker.headers)
    locked = next(a for a in thread["answers"] if a["id"] == answer["id"])
    results.append(check("못 맞힌 사람에게는 잠긴다", locked["locked"], True))
    results.append(check("  본문이 비어 있다", locked["body"], ""))
    results.append(check("  리플레이 시점도 없다", locked["anchor"], None))
    status, _ = raw_request("GET", f"/submissions/{accepted['id']}", None, asker.headers)
    results.append(check("  잠긴 글의 제출은 열리지 않는다", status, 404))
    status, _ = raw_request("GET", f"/submissions/{asked_wrong['id']}/trace")
    results.append(check("질문에 붙은 제출의 리플레이는 남에게 열린다", status in (200, 204), True))
    status, _ = raw_request("GET", f"/submissions/{asked_wrong['id']}/source")
    results.append(check("  소스는 열리지 않는다", status, 404))
    asker_accepted = submit(ACCEPTED_SOURCE, headers=asker.headers)
    await_verdict(asker_accepted["id"], headers=asker.headers)
    thread = request("GET", f"/discussions/threads/{question['id']}", None, asker.headers)
    opened = next(a for a in thread["answers"] if a["id"] == answer["id"])
    results.append(check("맞히고 나면 열린다", opened["locked"], False))
    results.append(check("  리플레이 시점이 보인다", opened["anchor"]["step"], 3))
    status, _ = raw_request("GET", f"/submissions/{accepted['id']}/trace", None, asker.headers)
    results.append(check("  그 제출의 리플레이도 열린다", status in (200, 204), True))

    status, _ = raw_request("POST", f"/discussions/posts/{answer['id']}/reports", {"reason": "내 글은 내가 신고 못 한다"})
    results.append(check("자기 글은 신고하지 못한다", status, 400))
    status, post_report = raw_request("POST", f"/discussions/posts/{answer['id']}/reports",
                                      {"reason": "정답 코드를 통째로 붙였습니다"}, asker.headers)
    results.append(check("남의 글을 신고한다", status, 202))
    status, _ = raw_request("POST", f"/discussions/posts/{answer['id']}/reports",
                            {"reason": "정답 코드를 통째로 붙였습니다"}, asker.headers)
    results.append(check("  두 번째 신고는 조용히", status, 204))
    status, _ = raw_request("GET", "/admin/discussions/queue")
    results.append(check("검수 큐는 검수자만", status, 403))
    queue = request("GET", "/admin/discussions/queue", None, reviewer)
    reported = next((r for r in queue if r["report"]["id"] == post_report["id"]), None)
    results.append(check("신고가 큐에 올라 있다", reported is not None, True))
    if reported:
        results.append(check("  검수자는 글 전체를 본다", reported["post"]["body"].startswith("해시맵에"), True))
    hidden = request("POST", f"/admin/discussions/reports/{post_report['id']}/resolve",
                     {"hide": True, "resolution": "정답 노출. 내린다"}, reviewer)
    results.append(check("신고를 받아 내렸다", hidden["status"], "HIDDEN"))
    thread = request("GET", f"/discussions/threads/{question['id']}")
    results.append(check("  답이 사라졌다", any(a["id"] == answer["id"] for a in thread["answers"]), False))
    status, _ = raw_request("GET", f"/submissions/{accepted['id']}", None, asker.headers)
    results.append(check("  붙었던 제출도 닫혔다", status, 404))
    trail = request("GET", f"/admin/audit?subject={post_report['id']}", None, security)
    results.append(check("  감사 로그에 남는다", [e["action"] for e in trail], ["DISCUSSION_POST_HIDDEN"]))

    print("\n풀이 공유와 기여자 평판 (§8.5 정적 풀이·도움됐다)")
    # asker 는 위에서 two-sum 을 맞혔다. 오답은 풀이로 올릴 수 없고, 맞힌 제출은 소스 전체가 실린다.
    status, _ = raw_request("POST", "/discussions/two-sum/solutions",
                            {"submissionId": asked_wrong["id"], "title": "해시맵", "body": "값을 인덱스로 기억하고 짝을 먼저 찾는다. 넣기는 나중에"},
                            asker.headers)
    results.append(check("오답은 풀이로 못 올린다", status, 400))
    status, _ = raw_request("POST", "/discussions/two-sum/solutions",
                            {"submissionId": accepted["id"], "title": "해시맵", "body": "남의 제출을 내 풀이라고 올릴 수는 없다"},
                            asker.headers)
    results.append(check("남의 제출은 못 올린다", status, 400))
    status, _ = raw_request("POST", "/discussions/two-sum/solutions",
                            {"submissionId": accepted["id"], "title": "해시맵", "body": "코드만"})
    results.append(check("접근 설명이 짧으면 거절", status, 400))
    status, solution = raw_request("POST", "/discussions/two-sum/solutions",
                                   {"submissionId": accepted["id"], "title": "해시맵 한 번 훑기",
                                    "body": "값을 인덱스로 기억하고 짝을 먼저 찾는다. 넣기는 찾은 뒤에 — 같은 원소를 두 번 쓰지 않으려면"})
    results.append(check("맞힌 제출을 풀이로 올린다", status, 201))
    results.append(check("  종류가 풀이다", solution["kind"], "SOLUTION"))
    results.append(check("  소스 전체가 실린다", solution["anchor"]["excerpt"], ACCEPTED_SOURCE))
    results.append(check("  항상 풀이 노출", solution["spoiler"], True))
    status, _ = raw_request("POST", "/discussions/two-sum/solutions",
                            {"submissionId": accepted["id"], "title": "해시맵 한 번 훑기", "body": "같은 제출을 두 번 올릴 수는 없다 — 한 번만"})
    results.append(check("  같은 제출은 한 번만", status, 400))

    fresh = accounts.create("fresh")
    listed = request("GET", "/discussions/two-sum/solutions", None, fresh.headers)
    mine_view = next(sol for sol in listed if sol["id"] == solution["id"])
    results.append(check("못 맞힌 사람에게 제목은 보인다", mine_view["title"], "해시맵 한 번 훑기"))
    results.append(check("  코드는 잠긴다", (mine_view["locked"], mine_view["anchor"]), (True, None)))
    status, _ = raw_request("POST", f"/discussions/posts/{solution['id']}/helpful", None, fresh.headers)
    results.append(check("못 맞힌 사람은 도움됐다를 못 남긴다", status, 409))
    status, _ = raw_request("POST", f"/discussions/posts/{solution['id']}/helpful", None)
    results.append(check("자기 글에는 못 남긴다", status, 400))
    status, _ = raw_request("POST", f"/discussions/posts/{solution['id']}/helpful", None, asker.headers)
    results.append(check("맞힌 사람이 도움됐다를 남긴다", status, 202))
    status, _ = raw_request("POST", f"/discussions/posts/{solution['id']}/helpful", None, asker.headers)
    results.append(check("  두 번째는 조용히", status, 204))
    listed = request("GET", "/discussions/two-sum/solutions", None, asker.headers)
    opened = next(sol for sol in listed if sol["id"] == solution["id"])
    results.append(check("  수가 올랐고 내가 남긴 것으로 보인다", (opened["helpful"], opened["markedHelpful"]), (1, True)))
    results.append(check("  맞힌 사람에게는 코드가 보인다", opened["anchor"]["excerpt"], ACCEPTED_SOURCE))
    results.append(check("  글쓴이의 이름 대신 등급", opened["contributor"], "NEW"))
    contributions = request("GET", "/discussions/me/contributions")
    results.append(check("내 기여에 잡힌다", (contributions["helpfulReceived"], contributions["solutionsShared"], contributions["score"]),
                         (1, 1, 1)))
    # 위에서 기부자의 과녁은 신고로 내려졌다. 내려진 기부는 기여가 아니다.
    results.append(check("  내려진 기부는 세지 않는다", request("GET", "/discussions/me/contributions", None, donor.headers)["donationsApproved"], 0))

    print("\n실험실에 남의 풀이 세우기 (§8.5 실행 가능한 인터랙티브 해설)")
    # 위에서 기본 계정이 풀이를 올렸다. 맞힌 asker 에게는 실험실 후보로 보이고, 못 맞힌 fresh 에게는 해설을 열어도 안 보인다.
    shared_label = f"공유 풀이: 해시맵 한 번 훑기 ({solution['id'][:8]})"
    editorial = request("GET", "/labs/two-sum/editorial", None, asker.headers)
    results.append(check("맞힌 사람의 실험실에 공유 풀이가 선다", shared_label in editorial["approaches"], True))
    request("POST", "/labs/two-sum/editorial/unlock", None, fresh.headers)
    editorial = request("GET", "/labs/two-sum/editorial", None, fresh.headers)
    results.append(check("해설을 미리 연 사람에게는 참조 풀이까지만", ("참조 풀이" in editorial["approaches"], shared_label in editorial["approaches"]), (True, False)))
    status, run = raw_request("POST", "/labs/two-sum/runs", {"args": [[2, 7, 11, 15], 9], "labels": ["참조 풀이", shared_label]}, asker.headers)
    results.append(check("참조 풀이와 나란히 돌린다", status, 202))
    if status == 202:
        deadline = time.time() + TIMEOUT
        while run["status"] == "PENDING" and time.time() < deadline:
            time.sleep(1)
            run = request("GET", f"/labs/runs/{run['id']}", None, asker.headers)
        actuals = [r["actual"] for r in run["results"]]
        results.append(check("  두 풀이가 같은 답을 냈다", (len(actuals), len(set(actuals)), actuals[0] is not None), (2, 1, True)))
        results.append(check("  공유 풀이의 이벤트가 있다", next(r for r in run["results"] if r["label"] == shared_label)["events"] != [], True))

    print("\n제출 유사도 신호 (§11.4 부정행위 방어, §10.4 정책)")
    # 위에서 기본 계정과 asker 가 같은 소스로 맞혔다. 구조가 같으면 신호가 서고, 다른 접근이면 서지 않는다.
    honest = accounts.create("honest")
    distinct = submit(ACCEPTED_QUADRATIC, headers=honest.headers)
    final = await_verdict(distinct["id"], headers=honest.headers)
    results.append(check("다른 접근도 정답이다", final["verdict"], "ACCEPTED"))
    status, _ = raw_request("GET", "/admin/integrity/queue")
    results.append(check("신호 큐는 검수자만", status, 403))
    queue = request("GET", "/admin/integrity/queue", None, reviewer)
    pair = {accepted["id"], asker_accepted["id"]}
    flagged = next((f for f in queue if {f["flag"]["submissionId"], f["flag"]["otherSubmissionId"]} == pair), None)
    results.append(check("같은 소스 둘이 신호로 섰다", flagged is not None, True))
    if flagged:
        results.append(check("  점수는 1.0", flagged["flag"]["score"], 1.0))
        results.append(check("  검수자는 두 소스를 본다", (flagged["source"], flagged["otherSource"]), (ACCEPTED_SOURCE, ACCEPTED_SOURCE)))
    # 스모크를 여러 번 돌리면 이전 실행의 같은 소스와는 신호가 선다 — 그것은 맞는 신호다.
    # 여기서 보는 것은 해시맵 풀이와는 서지 않는다는 것이다.
    hashmap_ids = {accepted["id"], asker_accepted["id"]}
    results.append(check("다른 접근은 신호가 아니다",
                         any(distinct["id"] in (f["flag"]["submissionId"], f["flag"]["otherSubmissionId"])
                             and hashmap_ids & {f["flag"]["submissionId"], f["flag"]["otherSubmissionId"]} for f in queue), False))
    verdict_before = request("GET", f"/submissions/{accepted['id']}")["verdict"]
    if flagged:
        decided = request("POST", f"/admin/integrity/flags/{flagged['flag']['id']}/resolve",
                          {"confirmed": False, "note": "같은 교재의 풀이다"}, reviewer)
        results.append(check("검수자가 기각했다", decided["status"], "DISMISSED"))
        status, _ = raw_request("POST", f"/admin/integrity/flags/{flagged['flag']['id']}/resolve",
                                {"confirmed": True}, reviewer)
        results.append(check("  두 번째 결정은 없다", status, 409))
        trail = request("GET", f"/admin/audit?subject={flagged['flag']['id']}", None, security)
        results.append(check("  감사 로그에 남는다", [e["action"] for e in trail], ["SIMILARITY_DISMISSED"]))
    results.append(check("신호는 판정을 바꾸지 않는다", request("GET", f"/submissions/{accepted['id']}")["verdict"], verdict_before))

    print("\n제재와 이의 (§8.5 단계적 제재, §10.4 이의 절차)")
    # 위의 신고와 유사도 신호가 근거다. 보안 관리자가 걸고, 발부하지 않은 다른 보안 관리자가 이의를 본다.
    status, _ = raw_request("POST", "/admin/sanctions",
                            {"userId": asker.user_id, "kind": "MUTE", "reason": "정답 코드를 통째로 붙였다", "evidence": f"report:{post_report['id']}", "days": 7},
                            reviewer)
    results.append(check("제재는 보안 관리자만", status, 403))
    status, _ = raw_request("POST", "/admin/sanctions",
                            {"userId": asker.user_id, "kind": "MUTE", "reason": "정답 코드를 통째로 붙였다", "evidence": "느낌", "days": 7}, security)
    results.append(check("근거 없는 제재는 없다", status, 409))
    muted = request("POST", "/admin/sanctions",
                    {"userId": asker.user_id, "kind": "MUTE", "reason": "정답 코드를 통째로 붙였다", "evidence": f"report:{post_report['id']}", "days": 7},
                    security)
    results.append(check("글쓰기 정지를 걸었다", muted["kind"], "MUTE"))
    me = request("GET", "/auth/me", None, asker.headers)
    results.append(check("  본인에게 보인다", (me["sanction"] or {}).get("kind"), "MUTE"))
    status, denied = raw_request("POST", "/discussions/two-sum", {"title": "정지 중에 묻기", "body": "이 요청은 문 앞에서 막혀야 합니다"}, asker.headers)
    results.append(check("  글쓰기가 막힌다", (status, denied.get("errorCode")), (403, "ACCOUNT_SANCTIONED")))
    status, _ = raw_request("GET", "/discussions/two-sum", None, asker.headers)
    results.append(check("  읽기는 열려 있다", status, 200))
    status, _ = raw_request("POST", "/submissions", {"problemId": "two-sum", "problemVersion": version_of("two-sum"), "language": "KOTLIN", "source": ACCEPTED_SOURCE},
                            {**asker.headers, "Idempotency-Key": str(uuid.uuid4())})
    results.append(check("  제출은 막히지 않는다", status, 202))
    status, _ = raw_request("POST", f"/auth/me/sanction/{muted['id']}/appeal", {"text": "짧다"}, asker.headers)
    results.append(check("이의가 짧으면 거절", status, 400))
    status, appealed = raw_request("POST", f"/auth/me/sanction/{muted['id']}/appeal",
                                   {"text": "코드 구간이 아니라 접근을 설명하려던 것이고 풀이 노출로 표시했습니다"}, asker.headers)
    results.append(check("정지 중에도 이의는 낸다", (status, appealed.get("appealed")), (202, True)))
    status, _ = raw_request("POST", f"/auth/me/sanction/{muted['id']}/appeal", {"text": "두 번째 이의는 받지 않아야 합니다 — 한 번이다"}, asker.headers)
    results.append(check("  이의는 한 번", status, 409))
    appeals = request("GET", "/admin/sanctions/appeals", None, security)
    results.append(check("이의가 큐에 올라 있다", any(a["id"] == muted["id"] for a in appeals), True))
    status, _ = raw_request("POST", f"/admin/sanctions/{muted['id']}/appeal/resolve", {"uphold": False, "note": "받아들인다"}, security)
    results.append(check("발부한 사람은 이의를 판단하지 못한다", status, 409))
    second_security = operators.with_roles("SECURITY_ADMIN", other_than=security_op).headers
    lifted = request("POST", f"/admin/sanctions/{muted['id']}/appeal/resolve", {"uphold": False, "note": "풀이 노출로 표시돼 있었다"}, second_security)
    results.append(check("다른 보안 관리자가 받아들이면 풀린다", (lifted["appealResolution"], lifted["liftedAt"] is not None), ("LIFTED", True)))
    status, _ = raw_request("POST", "/discussions/two-sum", {"title": "풀린 뒤에 묻기", "body": "이 요청은 이제 통과해야 합니다"}, asker.headers)
    results.append(check("  글쓰기가 돌아왔다", status, 201))
    me = request("GET", "/auth/me", None, asker.headers)
    results.append(check("  본인에게 이의의 답이 보인다", ((me["sanction"] or {}).get("appealNote"), (me["sanction"] or {}).get("active")), ("풀이 노출로 표시돼 있었다", False)))

    evidence = f"similarity:{flagged['flag']['id']}" if flagged else f"report:{post_report['id']}"
    suspended = request("POST", "/admin/sanctions",
                        {"userId": asker.user_id, "kind": "SUSPEND", "reason": "같은 소스를 다른 계정으로 냈다", "evidence": evidence, "days": 1}, security)
    results.append(check("제출 정지를 걸었다", suspended["kind"], "SUSPEND"))
    status, denied = raw_request("POST", "/submissions", {"problemId": "two-sum", "problemVersion": version_of("two-sum"), "language": "KOTLIN", "source": ACCEPTED_SOURCE},
                                 {**asker.headers, "Idempotency-Key": str(uuid.uuid4())})
    results.append(check("  제출이 막힌다", (status, denied.get("errorCode")), (403, "ACCOUNT_SANCTIONED")))
    status, _ = raw_request("GET", "/submissions?problemId=two-sum", None, asker.headers)
    results.append(check("  내 기록은 본다", status, 200))
    released = request("POST", f"/admin/sanctions/{suspended['id']}/lift", None, security)
    results.append(check("풀었다", released["liftedAt"] is not None, True))
    status, _ = raw_request("POST", f"/admin/sanctions/{suspended['id']}/lift", None, security)
    results.append(check("  두 번 풀 수는 없다", status, 409))
    history = request("GET", f"/admin/sanctions/users/{asker.user_id}", None, security)
    results.append(check("이력 두 건", len(history), 2))
    trail = request("GET", f"/admin/audit?subject={asker.user_id}", None, security)
    results.append(check("  감사 로그에 발부가 남는다", [e["action"] for e in trail].count("SANCTION_ISSUED"), 2))

    print("\n가입·로그인 남용 방어 (§10.2, A6)")
    # 개발 스택은 프록시를 믿고 루프백을 세지 않는다 (scripts/up.py). 그래서 X-Forwarded-For 로 남의 출처를 흉내 내 한도를 시험한다.
    origin = f"203.0.113.{uuid.uuid4().int % 250 + 1}"
    other_origin = f"198.51.100.{uuid.uuid4().int % 250 + 1}"

    def signup_from(ip: str):
        return raw_request("POST", "/auth/register",
                           {"email": f"abuse-{uuid.uuid4().hex[:10]}@example.test", "displayName": "abuse", "password": uuid.uuid4().hex},
                           {"Authorization": "", "X-Forwarded-For": ip})

    statuses = [signup_from(origin)[0] for _ in range(5)]
    results.append(check("같은 곳에서 다섯 번까지 가입한다", statuses, [201] * 5))
    status, refused = signup_from(origin)
    results.append(check("여섯 번째는 막힌다", (status, refused.get("errorCode")), (429, "QUOTA_EXCEEDED")))
    results.append(check("  다른 곳은 막히지 않는다", signup_from(other_origin)[0], 201))
    results.append(check("  루프백(스모크 자신)은 세지 않는다", raw_request("POST", "/auth/register",
                         {"email": f"abuse-{uuid.uuid4().hex[:10]}@example.test", "displayName": "abuse", "password": uuid.uuid4().hex},
                         {"Authorization": ""})[0], 201))

    victim = accounts.create("victim")
    failures = [raw_request("POST", "/auth/login", {"email": victim.email, "password": "wrong-" + uuid.uuid4().hex},
                            {"Authorization": "", "X-Forwarded-For": other_origin})[0] for _ in range(10)]
    results.append(check("열 번 틀릴 수 있다", failures, [401] * 10))
    status, locked = raw_request("POST", "/auth/login", {"email": victim.email, "password": victim.password},
                                 {"Authorization": "", "X-Forwarded-For": other_origin})
    results.append(check("열한 번째는 맞는 비밀번호로도 잠긴다", (status, locked.get("errorCode")), (429, "QUOTA_EXCEEDED")))
    status, _ = raw_request("POST", "/auth/login", {"email": victim.email, "password": victim.password},
                            {"Authorization": "", "X-Forwarded-For": origin})
    results.append(check("  다른 곳에서도 잠겨 있다 — 계정 단위다", status, 429))

    print("\n대회와 미니 대결 (§8.4 비레이팅·미니 대결)")
    from datetime import datetime, timedelta, timezone
    now = datetime.now(timezone.utc)
    starts, ends = (now - timedelta(minutes=1)).isoformat(), (now + timedelta(minutes=30)).isoformat()
    status, _ = raw_request("POST", "/admin/contests", {"title": "스모크 대회", "problemIds": ["two-sum", "no-such-problem"], "startsAt": starts, "endsAt": ends}, registrar)
    results.append(check("공개되지 않은 문제로는 못 만든다", status, 409))
    contest = request("POST", "/admin/contests", {"title": "스모크 대회", "problemIds": ["two-sum"], "startsAt": starts, "endsAt": ends}, registrar)
    results.append(check("대회를 만들었다", contest["published"], False))
    status, _ = raw_request("GET", f"/contests/{contest['id']}")
    results.append(check("  공개 전에는 없는 것이다", status, 404))
    status, _ = raw_request("POST", f"/admin/contests/{contest['id']}/publish", None, editor_only)
    results.append(check("  여는 것은 PUBLISHER 다", status, 403))
    status, _ = raw_request("POST", f"/admin/contests/{contest['id']}/publish", None, registrar)
    results.append(check("  만든 사람은 못 연다 (2인 승인)", status, 409))
    opened = request("POST", f"/admin/contests/{contest['id']}/publish", None, publisher)
    results.append(check("PUBLISHER 가 열었다", opened["published"], True))
    listed = request("GET", "/contests")
    results.append(check("목록에 보인다", any(c["id"] == contest["id"] and c["status"] == "RUNNING" for c in listed), True))
    joined = request("POST", f"/contests/{contest['id']}/join")
    results.append(check("참가했다", (joined["joined"], joined["entrants"]), (True, 1)))
    status, _ = raw_request("POST", f"/contests/{contest['id']}/join")
    results.append(check("  두 번째 참가는 조용히", status, 204))
    view = request("GET", f"/contests/{contest['id']}")
    results.append(check("  순위표에 내가 0점으로 있다", [(r["mine"], r["total"]) for r in view["standings"]], [(True, 0)]))
    await_verdict(submit(ACCEPTED_SOURCE)["id"])
    await_verdict(submit(ACCEPTED_SOURCE, headers=honest.headers)["id"], headers=honest.headers)
    view = request("GET", f"/contests/{contest['id']}")
    me_row = next(r for r in view["standings"] if r["mine"])
    results.append(check("대회 중의 정답이 점수가 됐다", (me_row["rank"], me_row["total"], me_row["solved"]), (1, 100, 1)))
    results.append(check("  참가하지 않은 사람은 순위표에 없다", len(view["standings"]), 1))
    results.append(check("  순위표에 이름이 나간다", me_row["displayName"], USER.display_name))

    status, duel = raw_request("POST", "/contests/duels", {"problemId": "two-sum", "minutes": 10}, asker.headers)
    results.append(check("미니 대결을 열었다", (status, duel["contest"]["status"], len(duel["joinCode"])), (201, "WAITING", 6)))
    code = duel["joinCode"]
    status, _ = raw_request("GET", f"/contests/{duel['contest']['id']}", None, honest.headers)
    results.append(check("  코드 없는 사람에게는 없는 것이다", status, 404))
    status, joined_duel = raw_request("POST", "/contests/duels/join", {"code": code.lower()}, honest.headers)
    results.append(check("코드로 붙으면 시작한다", (status, joined_duel["contest"]["status"], joined_duel["contest"]["entrants"]), (201, "RUNNING", 2)))
    status, _ = raw_request("POST", "/contests/duels/join", {"code": code}, fresh.headers)
    results.append(check("  셋째는 없다", status, 400))
    await_verdict(submit(ACCEPTED_SOURCE, headers=honest.headers)["id"], headers=honest.headers)
    view = request("GET", f"/contests/{duel['contest']['id']}", None, asker.headers)
    results.append(check("상대의 정답이 순위표에 올랐다", [(r["rank"], r["total"], r["mine"]) for r in view["standings"]], [(1, 100, False), (2, 0, True)]))
    results.append(check("  코드는 만든 사람에게만", (view["joinCode"], request("GET", f"/contests/{duel['contest']['id']}", None, honest.headers)["joinCode"]), (code, None)))

    # 반례 대전: 맞힌 뒤 아레나의 과녁을 깨뜨린 수가 점수다. 기본 계정은 위에서 맞혔고 과녁을 깨뜨려 봤다.
    hack = request("POST", "/admin/contests", {"kind": "HACK", "title": "스모크 반례 대전", "problemIds": ["two-sum"], "startsAt": starts, "endsAt": ends}, registrar)
    request("POST", f"/admin/contests/{hack['id']}/publish", None, publisher)
    request("POST", f"/contests/{hack['id']}/join")
    await_verdict(submit(ACCEPTED_SOURCE)["id"])
    view = request("GET", f"/contests/{hack['id']}")
    results.append(check("반례 대전에서 정답은 점수가 아니다", next(r for r in view["standings"] if r["mine"])["total"], 0))
    attempt = request("POST", "/arena/two-sum/attempts", {"args": [[2, 7, 11, 15], 9]})
    deadline = time.time() + TIMEOUT
    while attempt["status"] == "PENDING" and time.time() < deadline:
        time.sleep(1)
        attempt = request("GET", f"/arena/attempts/{attempt['id']}")
    broken = sum(1 for r in attempt["results"] if r["broken"])
    view = request("GET", f"/contests/{hack['id']}")
    results.append(check("깨뜨린 과녁의 수가 점수다", (broken > 0, next(r for r in view["standings"] if r["mine"])["total"]), (True, broken)))
    attempt = request("POST", "/arena/two-sum/attempts", {"args": [[2, 7, 11, 15], 9]})
    deadline = time.time() + TIMEOUT
    while attempt["status"] == "PENDING" and time.time() < deadline:
        time.sleep(1)
        attempt = request("GET", f"/arena/attempts/{attempt['id']}")
    view = request("GET", f"/contests/{hack['id']}")
    results.append(check("  같은 과녁을 다시 깨뜨려도 한 번이다", next(r for r in view["standings"] if r["mine"])["total"], broken))

    # 가상 참가: 끝난 대회를 같은 시간 조건으로 혼자 다시. 짧은 대회를 열어 끝내고 돈다.
    now = datetime.now(timezone.utc)  # 위의 아레나 시도가 시간을 먹었다. 창을 여기서 다시 잰다.
    short = request("POST", "/admin/contests", {"title": "스모크 짧은 대회", "problemIds": ["two-sum"],
                                                "startsAt": (now - timedelta(minutes=2)).isoformat(), "endsAt": (now + timedelta(seconds=30)).isoformat()}, registrar)
    request("POST", f"/admin/contests/{short['id']}/publish", None, publisher)
    request("POST", f"/contests/{short['id']}/join")
    await_verdict(submit(ACCEPTED_SOURCE)["id"])
    status, _ = raw_request("POST", f"/contests/{short['id']}/virtual", None, honest.headers)
    results.append(check("돌고 있는 대회는 가상으로 못 돈다", status, 400))
    while datetime.now(timezone.utc) < now + timedelta(seconds=31):
        time.sleep(1)
    view = request("GET", f"/contests/{short['id']}")
    results.append(check("대회가 끝났다", (view["contest"]["status"], view["virtual"]), ("FINISHED", None)))
    status, virtual = raw_request("POST", f"/contests/{short['id']}/virtual", None, honest.headers)
    results.append(check("끝난 대회를 가상으로 돈다", (status, virtual["contest"]["kind"], virtual["contest"]["status"], virtual["contest"]["minutes"]),
                         (201, "VIRTUAL", "RUNNING", 2)))
    again = request("POST", f"/contests/{short['id']}/virtual", None, honest.headers)
    results.append(check("  돌고 있으면 새로 열지 않는다", again["contest"]["id"], virtual["contest"]["id"]))
    await_verdict(submit(ACCEPTED_SOURCE, headers=honest.headers)["id"], headers=honest.headers)
    vview = request("GET", f"/contests/{virtual['contest']['id']}", None, honest.headers)
    rows = [(r["virtual"], r["mine"], r["total"]) for r in vview["standings"]]
    results.append(check("원래 순위표 사이에 내 가상 줄이 있다", (sorted(rows), len(rows)), (sorted([(False, False, 100), (True, True, 100)]), 2)))
    status, _ = raw_request("GET", f"/contests/{virtual['contest']['id']}")
    results.append(check("  가상 참가는 남에게 없다", status, 404))
    results.append(check("  원래 순위표는 그대로다", len(request("GET", f"/contests/{short['id']}")["standings"]), 1))

    # 정기 레이팅: 레이팅 대회가 끝나면 순위표에서 Elo 를 한 번 적용한다. 둘 다 1500 에서 시작한다.
    now = datetime.now(timezone.utc)
    rated = request("POST", "/admin/contests", {"title": "스모크 레이팅 대회", "problemIds": ["two-sum"], "rated": True,
                                                "startsAt": (now - timedelta(minutes=1)).isoformat(), "endsAt": (now + timedelta(seconds=30)).isoformat()}, registrar)
    results.append(check("레이팅 대회를 만들었다", rated["rated"], True))
    request("POST", f"/admin/contests/{rated['id']}/publish", None, publisher)
    request("POST", f"/contests/{rated['id']}/join")
    request("POST", f"/contests/{rated['id']}/join", None, honest.headers)
    await_verdict(submit(ACCEPTED_SOURCE)["id"])
    before = request("GET", "/contests/me/rating")
    results.append(check("끝나기 전에는 레이팅이 안 움직인다", (before["rating"], before["contests"]), (1500, 0)))
    while datetime.now(timezone.utc) < now + timedelta(seconds=31):
        time.sleep(1)
    view = request("GET", f"/contests/{rated['id']}")
    results.append(check("끝나면 순위표에 변화가 실린다", sorted(r["ratingChange"] for r in view["standings"]), [-32, 32]))
    results.append(check("  적용 시각이 찍힌다", view["contest"]["ratedAt"] is not None, True))
    mine = request("GET", "/contests/me/rating")
    results.append(check("내 레이팅이 올랐다", (mine["rating"], mine["contests"], mine["history"][0]["rank"]), (1532, 1, 1)))
    results.append(check("  진 쪽은 내렸다", request("GET", "/contests/me/rating", None, honest.headers)["rating"], 1468))
    request("GET", f"/contests/{rated['id']}")
    results.append(check("  두 번 적용하지 않는다", request("GET", "/contests/me/rating")["rating"], 1532))

    print("\n프로젝트형 문제 (로드맵 11단계 — 두 번째 판정기)")
    project_root = pathlib.Path(__file__).resolve().parents[1] / "content" / "projects" / "inventory-ledger"

    def overlay(*dirs: pathlib.Path) -> dict[str, str]:
        files: dict[str, str] = {}
        for base in dirs:
            for path in sorted(base.rglob("*")):
                if path.is_file():
                    files[path.relative_to(base).as_posix()] = path.read_text()
        return files

    def await_project(submission_id: str, headers: dict | None = None) -> dict:
        deadline = time.time() + TIMEOUT * 3
        last: dict = {}
        while time.time() < deadline:
            last = request("GET", f"/projects/submissions/{submission_id}", None, headers)
            if last["status"] == "COMPLETED":
                return last
            time.sleep(1)
        raise TimeoutError(f"프로젝트 판정이 끝나지 않았다: {last.get('status')}")

    status, _ = raw_request("POST", "/projects/inventory-ledger/submissions", {"files": {"a.py": ""}}, {"Authorization": "", "Idempotency-Key": "p-noauth"})
    results.append(check("토큰 없는 프로젝트 제출 거부", status, 401))
    status, projects = raw_request("GET", "/projects", None, {"Authorization": ""})
    results.append(check("프로젝트 목록은 공개", (status, [p["id"] for p in projects]), (200, ["inventory-ledger"])))
    results.append(check("  역량 없이 난이도·태그·요약만", (projects[0]["difficulty"], projects[0]["tags"], projects[0]["solved"]), ("MEDIUM", ["queue", "simulation"], False)))
    view = request("GET", "/projects/inventory-ledger")
    results.append(check("상세는 시작 저장소를 준다", sorted(view["files"]), ["ledger/__init__.py", "ledger/inventory.py", "tests/__init__.py", "tests/test_public.py"]))
    results.append(check("  숨은 테스트는 어디에도 없다", any("hidden" in path or "test_hidden" in content for path, content in view["files"].items()) or "hidden" in view["statement"].lower(), False))
    results.append(check("  공개 테스트 모듈", view["publicTests"], ["tests.test_public"]))
    results.append(check("  한도는 분 단위", (view["limits"]["buildSeconds"], view["limits"]["testSeconds"]), (60, 120)))
    status, _ = raw_request("GET", "/projects/no-such-project")
    results.append(check("없는 프로젝트는 404", status, 404))

    status, rejected = raw_request("POST", "/projects/inventory-ledger/submissions", {"files": {"../escape.py": "x"}}, {"Idempotency-Key": f"p-escape-{uuid.uuid4()}"})
    results.append(check("밖을 가리키는 경로는 거절", (status, "상위" in rejected["message"]), (400, True)))
    status, rejected = raw_request("POST", "/projects/inventory-ledger/submissions", {"files": {"big.py": "x" * (300 * 1024)}}, {"Idempotency-Key": f"p-big-{uuid.uuid4()}"})
    results.append(check("너무 큰 파일은 거절", status, 400))

    starter = view["files"]
    reference = {**starter, **overlay(project_root / "reference")}
    key = f"p-ref-{uuid.uuid4()}"
    status, accepted = raw_request("POST", "/projects/inventory-ledger/submissions", {"files": reference}, {"Idempotency-Key": key})
    results.append(check("참조 구현을 제출했다", (status, accepted["status"]), (202, "QUEUED")))
    status, again = raw_request("POST", "/projects/inventory-ledger/submissions", {"files": reference}, {"Idempotency-Key": key})
    results.append(check("  같은 키는 같은 제출", again["id"], accepted["id"]))
    final = await_project(accepted["id"])
    results.append(check("참조 구현은 ACCEPTED", (final["verdict"], final["score"]), ("ACCEPTED", 100)))
    results.append(check("  공개 테스트 셋은 이름과 함께", sorted(t["name"] for t in final["tests"]), ["PublicTests.test_receive_then_on_hand", "PublicTests.test_rejects_non_positive_quantity", "PublicTests.test_ship_uses_oldest_lot_first"]))
    results.append(check("  숨은 테스트는 수로만", (final["hiddenPassed"], final["hiddenTotal"], any(t["module"] == "tests.test_hidden" for t in final["tests"])), (10, 10, False)))
    results.append(check("  제출한 파일이 함께 온다", sorted(final["files"]) == sorted(reference), True))
    results.append(check("목록에 완료 표시", request("GET", "/projects")[0]["solved"], True))

    mutant = {**starter, **overlay(project_root / "mutants" / "partial-lot--drops-remainder")}
    wrong = await_project(request("POST", "/projects/inventory-ledger/submissions", {"files": mutant}, {"Idempotency-Key": f"p-mut-{uuid.uuid4()}"})["id"])
    results.append(check("로트 나머지를 버리는 오답은 WRONG_ANSWER", (wrong["verdict"], wrong["hiddenTotal"] - wrong["hiddenPassed"] >= 1), ("WRONG_ANSWER", True)))
    failed_public = [t for t in wrong["tests"] if not t["passed"]]
    results.append(check("  공개 테스트의 실패 사유는 보인다", (len(failed_public), failed_public[0]["message"] is not None), (1, True)))
    # 공개 3 + 숨은 10 = 13 이 각각 한 표다. 공개·숨은 것을 가리지 않는다.
    passed_count = len(wrong["tests"]) - len(failed_public) + wrong["hiddenPassed"]
    results.append(check("  점수는 통과 비율", wrong["score"], passed_count * 100 // (len(wrong["tests"]) + wrong["hiddenTotal"])))

    hijack = {**reference, "tests/test_hidden.py": "import unittest\nclass Nothing(unittest.TestCase):\n    def test_pass(self):\n        pass\n", "ledger/inventory.py": starter["ledger/inventory.py"]}
    hijacked = await_project(request("POST", "/projects/inventory-ledger/submissions", {"files": hijack}, {"Idempotency-Key": f"p-hijack-{uuid.uuid4()}"})["id"])
    results.append(check("숨은 테스트 파일을 갈아 끼워도 숨은 것이 돈다", (hijacked["verdict"], hijacked["hiddenTotal"], hijacked["hiddenPassed"]), ("WRONG_ANSWER", 10, 0)))

    tamper = {**starter, **overlay(project_root / "mutants" / "tamper--patches-unittest")}
    tampered = await_project(request("POST", "/projects/inventory-ledger/submissions", {"files": tamper}, {"Idempotency-Key": f"p-tamper-{uuid.uuid4()}"})["id"])
    results.append(check("테스트 기반을 손대면 전부 실패", (tampered["verdict"], tampered["score"], "테스트 기반이 바뀌었다" in (tampered["log"] or "")), ("WRONG_ANSWER", 0, True)))

    broken = {**starter, "ledger/inventory.py": "def broken(:\n"}
    compile_error = await_project(request("POST", "/projects/inventory-ledger/submissions", {"files": broken}, {"Idempotency-Key": f"p-syntax-{uuid.uuid4()}"})["id"])
    results.append(check("문법 오류는 COMPILE_ERROR", (compile_error["verdict"], "inventory.py" in compile_error["log"]), ("COMPILE_ERROR", True)))

    history = request("GET", "/projects/submissions?projectId=inventory-ledger")
    results.append(check("내 프로젝트 제출 기록", len(history) >= 5 and all("files" not in h or h["files"] is None for h in history), True))
    status, _ = raw_request("GET", f"/projects/submissions/{accepted['id']}", None, honest.headers)
    results.append(check("남의 프로젝트 제출은 404", status, 404))

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
