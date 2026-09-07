#!/usr/bin/env python3
"""장애 주입 훈련 (기술 설계서 §12.2 장애 격리, §19.1 결과 수렴 확인).

§19.1 은 "중복 전달, 워커 유실, 저장소 장애, broker 복구를 주입해 결과 수렴을
확인한다"를 완료 기준으로 둔다. 이 스크립트가 그 주입을 실제로 한다.

훈련은 **문서가 아니라 실행**이어야 한다. 런북에 적힌 복구 절차가 맞는지는 장애가
났을 때 알게 되는데, 그때 틀린 것을 발견하면 이미 늦다.

시나리오:
    broker       브로커를 멈추고 제출한 뒤 되살린다. 제출이 유실되지 않아야 한다
    duplicate    같은 멱등 키로 동시에 제출한다. 제출과 판정이 하나여야 한다
    worker-loss  채점 중 Runner 를 죽인다. 임대 회수로 판정이 끝나야 한다
    trace-loss   트레이스를 잃는다. 판정은 그대로 유효해야 한다

사용법:
    python3 scripts/drill.py all
    python3 scripts/drill.py worker-loss --lease-seconds 15

worker-loss 는 오케스트레이터의 임대 기간만큼 기다린다. 기본 120초를 그대로 두면
훈련이 2분 넘게 걸리므로, 아래처럼 짧게 띄운 오케스트레이터로 돌린다.

    ./gradlew :judge:orchestrator:bootRun --args='--codedrill.judge.lease-seconds=15'
"""

from __future__ import annotations

import argparse
import json
import subprocess
import sys
import time
import urllib.error
import urllib.request
import uuid

import accounts
from concurrent.futures import ThreadPoolExecutor

BASE = "http://localhost:8080/api/v1"
COMPOSE = ["docker", "compose", "-f", "deploy/docker-compose.yml"]

# installDist 로 만든 Runner 배포. worker-loss 가 이걸 죽였다 되살린다.
RUNNER_BIN = "judge/runner-agent/build/install/runner-agent/bin/runner-agent"
RUNNER_MAIN = "dev.codedrill.judge.runner.RunnerAgentApplicationKt"

ACCEPTED_SOURCE = """
fun twoSum(nums: IntArray, target: Int): IntArray {
    val seen = HashMap<Int, Int>()
    for (i in nums.indices) {
        Drill.visit(i, nums[i])
        val j = seen[target - nums[i]]
        if (j != null) return intArrayOf(j, i)
        seen.putIfAbsent(nums[i], i)
    }
    error("정답은 항상 존재한다")
}
"""


# --- 기본 도구 -------------------------------------------------------------

# 훈련이 쓰는 계정. main() 이 새로 만든다.
USER: accounts.Account | None = None


def request(method: str, path: str, body=None, headers=None):
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(f"{BASE}{path}", data=data, method=method)
    req.add_header("Content-Type", "application/json")
    for key, value in {**(USER.headers if USER else {}), **(headers or {})}.items():
        req.add_header(key, value)
    with urllib.request.urlopen(req, timeout=30) as response:
        payload = response.read()
        return json.loads(payload) if payload else None


def submit(source: str = ACCEPTED_SOURCE, key: str | None = None, trace: bool = False):
    return request(
        "POST", "/submissions",
        {
            "problemId": "two-sum", "problemVersion": 1, "language": "KOTLIN",
            "source": source, "requestTrace": trace,
        },
        {"Idempotency-Key": key or uuid.uuid4().hex},
    )


def status_of(submission_id: str) -> dict:
    return request("GET", f"/submissions/{submission_id}")


def await_status(submission_id: str, wanted: str, timeout: float) -> dict | None:
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        current = status_of(submission_id)
        if current["status"] == wanted:
            return current
        time.sleep(0.3)
    return None


def compose(*args: str) -> None:
    subprocess.run([*COMPOSE, *args], check=True, capture_output=True)


def runner_pids() -> list[int]:
    result = subprocess.run(["pgrep", "-f", RUNNER_MAIN], capture_output=True, text=True)
    return [int(line) for line in result.stdout.split() if line.isdigit()]


class Drill:
    """한 훈련의 결과. 실패해도 다음 훈련은 돈다 — 하나가 막혀 나머지를 못 보면 안 된다."""

    def __init__(self, name: str) -> None:
        self.name = name
        self.steps: list[tuple[str, bool, str]] = []

    def check(self, label: str, ok: bool, detail: str = "") -> bool:
        self.steps.append((label, bool(ok), detail))
        mark = "OK  " if ok else "FAIL"
        print(f"  {mark} {label}" + (f" — {detail}" if detail else ""))
        return bool(ok)

    @property
    def passed(self) -> bool:
        return all(ok for _, ok, _ in self.steps)


# --- 시나리오 --------------------------------------------------------------

def drill_broker(args) -> Drill:
    """브로커 장애와 복구 (§12.2 Broker: outbox 축적 후 복구 발행)."""
    drill = Drill("broker")
    print("\n[broker] 브로커를 멈추고 제출한 뒤 되살린다")

    compose("stop", "rabbitmq")
    print("  rabbitmq 정지")

    try:
        # 브로커가 없어도 제출은 받아야 한다. 아웃박스는 DB 에 커밋되기 때문이다 (§3.2).
        submissions = [submit() for _ in range(3)]
        drill.check("브로커가 없어도 제출을 받는다", len(submissions) == 3)

        time.sleep(3)
        stuck = [status_of(s["id"])["status"] for s in submissions]
        drill.check(
            "판정이 진행되지 않는다", all(s in ("QUEUED", "CREATED") for s in stuck), str(set(stuck)),
        )
    finally:
        compose("start", "rabbitmq")
        print("  rabbitmq 재기동 — 재연결을 기다린다")

    # Spring AMQP 의 재연결과 아웃박스 재발행을 기다린다.
    converged = [await_status(s["id"], "COMPLETED", timeout=args.timeout) for s in submissions]
    drill.check(
        "복구 후 모든 제출이 판정된다",
        all(c is not None for c in converged),
        f"{sum(c is not None for c in converged)}/{len(submissions)}",
    )
    drill.check(
        "판정이 정상이다",
        all(c and c["verdict"] == "ACCEPTED" for c in converged),
    )
    return drill


def drill_duplicate(args) -> Drill:
    """중복 전달 (§4.3 제출 버튼 중복)."""
    drill = Drill("duplicate")
    print("\n[duplicate] 같은 멱등 키로 동시에 제출한다")

    key = uuid.uuid4().hex
    with ThreadPoolExecutor(max_workers=5) as pool:
        created = list(pool.map(lambda _: submit(key=key), range(5)))

    ids = {s["id"] for s in created}
    drill.check("제출이 하나만 만들어진다", len(ids) == 1, f"{len(ids)}개")

    final = await_status(next(iter(ids)), "COMPLETED", timeout=args.timeout)
    drill.check("판정이 온다", final is not None)

    history = request("GET", "/submissions?problemId=two-sum&limit=50")
    same_key = [item for item in history["items"] if item["id"] in ids]
    drill.check("기록에도 하나만 남는다", len(same_key) == 1, f"{len(same_key)}건")
    return drill


def drill_worker_loss(args) -> Drill:
    """워커 유실 (§4.3, §12.2 Runner zone)."""
    drill = Drill("worker-loss")
    print("\n[worker-loss] 채점 중 Runner 를 죽인다")

    pids = runner_pids()
    if not drill.check("Runner 가 떠 있다", bool(pids), f"pid {pids}"):
        return drill

    submission = submit()
    # 실행이 시작된 뒤에 죽여야 "실행 중 유실"이다. 큐에 있는 동안 죽이면 그냥 대기다.
    if not await_status(submission["id"], "LEASED", timeout=15):
        drill.check("실행이 시작됐다", False, "LEASED 로 넘어가지 않았다")
        return drill

    for pid in pids:
        subprocess.run(["kill", "-9", str(pid)], check=False)
    print(f"  Runner 강제 종료 (pid {pids})")

    time.sleep(1)
    drill.check("Runner 가 죽었다", not runner_pids())

    process = subprocess.Popen(
        [RUNNER_BIN], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
    )
    print(f"  Runner 재기동 (pid {process.pid})")

    # 임대가 만료돼야 회수가 일어난다. 만료 + 회수 주기 + 실행 시간만큼 기다린다.
    wait = args.lease_seconds + 30
    final = await_status(submission["id"], "COMPLETED", timeout=wait)
    drill.check(
        "임대 회수로 판정이 끝난다", final is not None,
        f"{wait}초 안에 수렴" if final else f"{wait}초 안에 수렴하지 않았다",
    )
    if final:
        drill.check("판정이 정상이다", final["verdict"] == "ACCEPTED", final["verdict"])
        drill.check("점수도 정상이다", final["score"] == 100, str(final["score"]))
    return drill


def drill_trace_loss(args) -> Drill:
    """트레이스 실패가 판정을 흔들지 않는다 (§7.1, §12.2 Trace Processor)."""
    drill = Drill("trace-loss")
    print("\n[trace-loss] 판정 직후 Runner 를 죽여 트레이스를 잃는다")

    pids = runner_pids()
    if not drill.check("Runner 가 떠 있다", bool(pids), f"pid {pids}"):
        return drill

    submission = submit(trace=True)
    final = await_status(submission["id"], "COMPLETED", timeout=args.timeout)
    if not drill.check("판정이 먼저 온다", final is not None):
        return drill

    # 판정은 끝났고 트레이스 작업만 남은 시점이다. 여기서 Runner 를 죽이면 트레이스는
    # 영영 오지 않는다.
    for pid in runner_pids():
        subprocess.run(["kill", "-9", str(pid)], check=False)
    print("  Runner 강제 종료")

    process = subprocess.Popen(
        [RUNNER_BIN], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
    )
    print(f"  Runner 재기동 (pid {process.pid})")

    still = status_of(submission["id"])
    drill.check("판정은 그대로다", still["verdict"] == "ACCEPTED", still["verdict"])
    drill.check("점수도 그대로다", still["score"] == 100, str(still["score"]))

    # 트레이스는 있어도 없어도 된다. 없다는 것이 오류로 나오지 않는 것이 요점이다.
    try:
        req = urllib.request.Request(f"{BASE}/submissions/{submission['id']}/trace")
        for key, value in (USER.headers if USER else {}).items():
            req.add_header(key, value)
        with urllib.request.urlopen(req, timeout=10) as response:
            code = response.status
    except urllib.error.HTTPError as error:
        code = error.code
    drill.check("트레이스 부재가 오류가 아니다", code in (200, 204), f"HTTP {code}")
    return drill


SCENARIOS = {
    "broker": drill_broker,
    "duplicate": drill_duplicate,
    "worker-loss": drill_worker_loss,
    "trace-loss": drill_trace_loss,
}


def main() -> int:
    parser = argparse.ArgumentParser(description="장애 주입 훈련 (§12.2, §19.1)")
    parser.add_argument("scenario", nargs="?", default="all", choices=[*SCENARIOS, "all"])
    parser.add_argument("--timeout", type=float, default=90, help="수렴을 기다리는 초")
    parser.add_argument(
        "--lease-seconds", type=int, default=15,
        help="오케스트레이터의 임대 기간. worker-loss 가 이만큼 기다린다",
    )
    args = parser.parse_args()

    global USER
    try:
        request("GET", "/problems?limit=1")
        USER = accounts.create("drill")
    except urllib.error.URLError as error:
        print(f"제어 영역에 닿지 못했다: {error}")
        print("세 앱이 떠 있어야 한다 (docs/running-locally.md).")
        return 2

    chosen = list(SCENARIOS) if args.scenario == "all" else [args.scenario]
    print(f"장애 주입 훈련: {', '.join(chosen)}")

    results = []
    for name in chosen:
        try:
            results.append(SCENARIOS[name](args))
        except Exception as error:  # noqa: BLE001 - 한 훈련의 실패가 나머지를 막으면 안 된다
            failed = Drill(name)
            failed.check("훈련이 예외로 끝났다", False, repr(error))
            results.append(failed)

    print()
    passed = sum(1 for drill in results if drill.passed)
    for drill in results:
        mark = "통과" if drill.passed else "실패"
        print(f"  {drill.name:<12} {mark}")
    print(f"\n{passed}/{len(results)} 훈련 통과")

    if passed != len(results):
        print("실패한 훈련의 복구 절차는 docs/runbook.md 에 있다.")
    return 0 if passed == len(results) else 1


if __name__ == "__main__":
    sys.exit(main())
