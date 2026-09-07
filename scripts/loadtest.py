#!/usr/bin/env python3
"""부하 시험과 SLO 판정 (기술 설계서 §12.1, §19.1 "SLO 부하 시험이 재현 가능하다").

제출을 실제로 밀어 넣고 §12.1 의 SLI 를 재서 목표와 대조한다. 목표값은 이 스크립트가
아니라 `deploy/observability/alerts.yml` 이 정한다 — 값을 두 곳에 적으면 경보와 부하
시험이 서로 다른 기준으로 통과·실패를 말하게 된다.

두 가지를 각각 잰다.

  - **클라이언트가 보는 것**: 제출 수락 응답 시간, 제출부터 판정까지의 전체 시간.
  - **서버가 아는 것**: 큐 대기·판정 전파 히스토그램. 이 둘은 프로세스 안쪽 구간이라
    바깥에서 잴 수 없다. /actuator/prometheus 의 버킷에서 분위수를 직접 계산한다.

용량은 Runner 수와 동시성이 정한다. 로컬은 Runner 하나에 동시성 1 이므로, 목표
처리율이 아니라 **이 구성이 실제로 내는 처리율**을 재는 것이 목적이다.

사용법:
    python3 scripts/loadtest.py                 # 30건, 동시 4
    python3 scripts/loadtest.py -n 100 -c 8
"""

from __future__ import annotations

import argparse
import json
import sys
import threading
import time
import urllib.error
import urllib.request
import uuid

import accounts
from collections import defaultdict
from concurrent.futures import ThreadPoolExecutor

BASE = "http://localhost:8080/api/v1"

# SLI 는 배포 단위마다 다른 프로세스에서 난다. 큐 대기는 오케스트레이터만 알고, 제출
# 수락은 제어 영역만 안다. 한 곳만 긁으면 절반은 "표본 없음"으로 보인다.
METRICS = [
    "http://localhost:8080/actuator/prometheus",
    "http://localhost:8081/actuator/prometheus",
]

# §12.1 의 목표. alerts.yml 의 임계값과 같아야 한다.
SLO = {
    "submit_accept": ("제출 수락 p95", 1.0, "s"),
    "queue_wait": ("큐 대기 p95", 2.0, "s"),
    "verdict_propagation": ("판정 전파 p95", 1.0, "s"),
    "platform_error_rate": ("플랫폼 오류율", 0.001, ""),
}

SOURCE = """
fun twoSum(nums: IntArray, target: Int): IntArray {
    val seen = HashMap<Int, Int>()
    for (i in nums.indices) {
        val j = seen[target - nums[i]]
        if (j != null) return intArrayOf(j, i)
        seen.putIfAbsent(nums[i], i)
    }
    error("정답은 항상 존재한다")
}
"""


# 부하 시험이 쓰는 계정. main() 이 새로 만든다.
USER: accounts.Account | None = None


def request(method: str, path: str, body: dict | None = None, headers: dict | None = None):
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(f"{BASE}{path}", data=data, method=method)
    req.add_header("Content-Type", "application/json")
    for key, value in {**(USER.headers if USER else {}), **(headers or {})}.items():
        req.add_header(key, value)
    with urllib.request.urlopen(req, timeout=30) as response:
        return json.loads(response.read())


def scrape() -> dict[str, list[tuple[dict[str, str], float]]]:
    """모든 배포 단위의 /actuator/prometheus 를 이름 → [(라벨, 값)] 으로 합쳐 읽는다."""
    lines: list[str] = []
    for url in METRICS:
        with urllib.request.urlopen(url, timeout=10) as response:
            lines += response.read().decode().splitlines()

    series: dict[str, list[tuple[dict[str, str], float]]] = defaultdict(list)
    for line in lines:
        if not line or line.startswith("#"):
            continue
        name_part, _, value = line.rpartition(" ")
        labels: dict[str, str] = {}
        if "{" in name_part:
            name, _, rest = name_part.partition("{")
            for pair in rest.rstrip("}").split(","):
                if "=" in pair:
                    key, _, val = pair.partition("=")
                    labels[key] = val.strip('"')
        else:
            name = name_part
        try:
            series[name].append((labels, float(value)))
        except ValueError:
            continue
    return series


def quantile_from_buckets(series, metric: str, q: float) -> float | None:
    """히스토그램 버킷에서 분위수를 낸다.

    Prometheus 의 histogram_quantile 과 같은 선형 보간을 쓴다. 대시보드가 보여 주는
    값과 이 스크립트가 말하는 값이 다르면 어느 쪽도 믿을 수 없게 되기 때문이다.
    """
    buckets: dict[float, float] = defaultdict(float)
    for labels, value in series.get(f"{metric}_bucket", []):
        le = labels.get("le")
        if le is None:
            continue
        buckets[float("inf") if le == "+Inf" else float(le)] += value

    if not buckets:
        return None
    ordered = sorted(buckets.items())
    total = ordered[-1][1]
    if total <= 0:
        return None

    target = q * total
    previous_bound, previous_count = 0.0, 0.0
    for bound, count in ordered:
        if count >= target:
            if bound == float("inf"):
                return previous_bound
            if count == previous_count:
                return bound
            ratio = (target - previous_count) / (count - previous_count)
            return previous_bound + (bound - previous_bound) * ratio
        previous_bound, previous_count = bound, count
    return ordered[-1][0]


def counter_total(series, metric: str, **match: str) -> float:
    return sum(
        value for labels, value in series.get(metric, [])
        if all(labels.get(k) == v for k, v in match.items())
    )


def run_one(problem: str) -> tuple[float, float, str]:
    """한 건을 제출하고 판정까지 기다린다. (수락 시간, 전체 시간, 판정)."""
    started = time.monotonic()
    submission = request(
        "POST", "/submissions",
        {
            "problemId": problem,
            "problemVersion": 1,
            "language": "KOTLIN",
            "source": SOURCE,
            # 트레이스는 판정 뒤에 실행을 한 번 더 돌린다. 부하 시험의 대상은 판정
            # 경로이므로 끈다 — 켜면 Runner 용량의 절반이 트레이스로 간다.
            "requestTrace": False,
        },
        {"Idempotency-Key": uuid.uuid4().hex},
    )
    accepted = time.monotonic() - started

    deadline = time.monotonic() + 120
    while time.monotonic() < deadline:
        current = request("GET", f"/submissions/{submission['id']}")
        if current["status"] == "COMPLETED":
            return accepted, time.monotonic() - started, current["verdict"]
        time.sleep(0.2)
    return accepted, time.monotonic() - started, "TIMEOUT"


def percentile(values: list[float], q: float) -> float:
    if not values:
        return float("nan")
    ordered = sorted(values)
    index = min(int(q * len(ordered)), len(ordered) - 1)
    return ordered[index]


def main() -> int:
    parser = argparse.ArgumentParser(description="부하 시험과 SLO 판정 (§12.1)")
    parser.add_argument("-n", "--count", type=int, default=30, help="제출 건수")
    parser.add_argument("-c", "--concurrency", type=int, default=4, help="동시 사용자 수")
    parser.add_argument("--problem", default="two-sum")
    args = parser.parse_args()

    try:
        before = scrape()
    except urllib.error.URLError as error:
        print(f"메트릭을 읽지 못했다: {error}")
        print("제어 영역과 오케스트레이터가 둘 다 떠 있어야 한다 (docs/running-locally.md).")
        return 2

    global USER
    USER = accounts.create("load")

    print(f"제출 {args.count}건, 동시 {args.concurrency}명, 문제 {args.problem}")
    print()

    accepts: list[float] = []
    totals: list[float] = []
    verdicts: dict[str, int] = defaultdict(int)
    lock = threading.Lock()

    wall_start = time.monotonic()
    with ThreadPoolExecutor(max_workers=args.concurrency) as pool:
        for accept, total, verdict in pool.map(lambda _: run_one(args.problem), range(args.count)):
            with lock:
                accepts.append(accept)
                totals.append(total)
                verdicts[verdict] += 1
    wall = time.monotonic() - wall_start

    after = scrape()

    # --- 클라이언트가 본 것 ---
    print("클라이언트 관점")
    print(f"  처리율            {args.count / wall:.2f} 건/s ({wall:.1f}초 동안 {args.count}건)")
    print(f"  제출 수락  p50/p95 {percentile(accepts, 0.5):.3f}s / {percentile(accepts, 0.95):.3f}s")
    print(f"  제출→판정  p50/p95 {percentile(totals, 0.5):.2f}s / {percentile(totals, 0.95):.2f}s")
    print(f"  최댓값             {max(totals):.2f}s")
    print(f"  판정 분포          {dict(verdicts)}")
    print()

    # --- 서버가 아는 것 ---
    #
    # 부하 시험 구간만 보려면 시작 전 값을 빼야 하지만, 히스토그램 버킷의 차분은
    # 분위수를 정확히 주지 않는다. 대신 시험 전 표본이 적을 때만 의미가 있으므로
    # 시험 전 건수를 함께 찍어 판단할 수 있게 한다.
    submitted_before = counter_total(before, "codedrill_verdict_total")
    print("서버 관점 (히스토그램 누적, 시험 전 판정 %d건 포함)" % submitted_before)

    measured = {
        "submit_accept": quantile_from_buckets(after, "codedrill_submission_accept_seconds", 0.95),
        "queue_wait": quantile_from_buckets(after, "codedrill_queue_wait_seconds", 0.95),
        "verdict_propagation": quantile_from_buckets(
            after, "codedrill_verdict_propagation_seconds", 0.95,
        ),
    }

    errors = counter_total(after, "codedrill_system_error_total") - counter_total(
        before, "codedrill_system_error_total",
    )
    judged = counter_total(after, "codedrill_verdict_total") - submitted_before
    measured["platform_error_rate"] = (errors / judged) if judged else 0.0

    failures = 0
    for key, value in measured.items():
        label, target, unit = SLO[key]
        if value is None:
            print(f"  {label:<16} 표본 없음")
            continue
        ok = value <= target
        failures += 0 if ok else 1
        mark = "OK  " if ok else "FAIL"
        print(f"  {mark} {label:<16} {value:.3f}{unit} (목표 {target}{unit})")

    print()
    if failures:
        print(f"{failures}개 SLO 위반. 이 구성으로는 이 부하를 감당하지 못한다.")
        print("Runner 를 늘리거나 부하를 낮춘 뒤 다시 잰다 (docs/runbook.md#capacity).")
    else:
        print(f"SLO 전부 충족. 이 구성의 확인된 처리율은 {args.count / wall:.2f} 건/s 다.")

    # 큐 대기가 목표를 넘는 것은 대개 용량 문제다. 판정을 내리기 전에 원인을 말해 준다.
    if measured.get("queue_wait") and measured["queue_wait"] > SLO["queue_wait"][1]:
        print()
        print("큐 대기가 길다 — Runner 동시성은 1 이다 (§5.2 측정 오염 방지).")
        print("동시 제출이 Runner 수를 넘으면 나머지는 줄을 선다. 용량 산정은 §17.1 이다.")

    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
