"""탐욕·구간·힙 (역량: 알고리즘 선택, 정확성 — "왜 이 선택이 최적을 놓치지 않는가").

탐욕 문제의 오답은 대개 **거의 맞는 탐욕**이다. 정렬 기준을 하나 잘못 잡거나, 같을 때의
처리를 빠뜨리거나. 그래서 오답도 그런 모양으로 만든다.
"""

from author import Problem, standard_groups, perf_groups, randoms, shuffled, flat

PROBLEMS = []


# --- 1. 겹치는 구간 합치기 -----------------------------------------------------

def _merge_count(intervals):
    pairs = sorted((intervals[i], intervals[i + 1]) for i in range(0, len(intervals), 2))
    count = 0
    end = None
    for start, finish in pairs:
        if end is None or start > end:
            count += 1
            end = finish
        elif finish > end:
            end = finish
    return count


PROBLEMS.append(Problem(
    id="merge-intervals-count",
    title="겹치는 구간 합치기",
    summary="""
닫힌 구간들이 `[s1, e1, s2, e2, ...]` 로 평탄하게 주어진다. **겹치거나 맞닿은** 구간을
전부 합쳤을 때 남는 구간의 개수를 반환한다.

- `[1, 3]` 과 `[3, 5]` 는 맞닿았으므로 하나다.
- `[1, 2]` 와 `[3, 4]` 는 떨어져 있으므로 둘이다.
""",
    notes="""
시작점으로 정렬하고 나면 "지금 구간이 직전 덩어리와 겹치는가"만 보면 된다. 끝점은
**최댓값으로** 늘려야 한다 — 긴 구간 안에 짧은 구간이 통째로 들어 있는 경우가 있다.
""",
    drill_doc="""
Drill.visit(i, start)          // i 번째 구간을 봤다
Drill.compare(i, end)          // 직전 덩어리의 끝과 견줬다
Drill.match(i, count)          // 새 덩어리를 열었다
""",
    constraints="""
- `2 <= intervals.size <= 200_000`, 짝수
- `0 <= s <= e <= 10^9`
""",
    signature=dict(name="mergeCount", parameters=[("intervals", "INT_ARRAY")], returns="INT"),
    groups=perf_groups(),
    reference=_merge_count,
    cases={
        "sample": [
            ("01", [[1, 3, 2, 6, 8, 10, 15, 18]]),
            ("02", [[1, 4, 4, 5]]),
        ],
        "boundary": [
            # 구간 하나.
            ("01-single", [[5, 5]]),
            # 큰 구간이 작은 것을 통째로 품는다. 끝점을 "마지막 것"으로 두면 틀린다.
            ("02-nested", [[1, 10, 2, 3, 4, 5, 11, 12]]),
            # 정렬돼 있지 않다.
            ("03-unsorted", [[8, 10, 1, 3, 2, 6]]),
            # 전부 떨어져 있다.
            ("04-disjoint", [[1, 1, 3, 3, 5, 5]]),
            # 맞닿음은 겹침이다.
            ("05-touching", [[1, 2, 2, 3, 3, 4]]),
        ],
        "hidden": [
            ("01-all-same", [[7, 9] * 50]),
            ("02-chain", [flat([i, i + 1] for i in range(0, 100))]),
            ("03-large-values", [[0, 1000000000, 5, 6]]),
        ],
        "performance": [
            ("01-small", [flat([v, v + 3] for v in randoms(2000, 0, 100000, salt=901))]),
            ("02-medium", [flat([v, v + 3] for v in randoms(20000, 0, 1000000, salt=902))]),
            ("03-large", [flat([v, v + 3] for v in randoms(100000, 0, 1000000000, salt=903))]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 시작점 정렬 + 한 번 훑기.
fun mergeCount(intervals: IntArray): Int {
    val n = intervals.size / 2
    val order = (0 until n).sortedBy { intervals[2 * it] }
    var count = 0
    var end = Long.MIN_VALUE
    for (i in order) {
        val start = intervals[2 * i]
        val finish = intervals[2 * i + 1]
        Drill.visit(i, start)
        Drill.compare(i, if (end == Long.MIN_VALUE) -1 else end.toInt())
        if (start > end) {
            count += 1
            Drill.match(i, count)
            end = finish.toLong()
        } else if (finish > end) {
            end = finish.toLong()
        }
    }
    return count
}
""",
    mutants=[
        ("last-end--not-max", "WRONG_BRANCH",
         "덩어리의 끝을 마지막 구간의 끝으로 둔다. 큰 구간이 작은 것을 품으면 끝이 뒤로 물러난다.",
         """
fun mergeCount(intervals: IntArray): Int {
    val n = intervals.size / 2
    val order = (0 until n).sortedBy { intervals[2 * it] }
    var count = 0
    var end = Long.MIN_VALUE
    for (i in order) {
        val start = intervals[2 * i]
        val finish = intervals[2 * i + 1]
        if (start > end) count += 1
        end = finish.toLong()
    }
    return count
}
"""),
        ("strict-overlap--touching-splits", "OFF_BY_ONE",
         "맞닿은 구간을 떨어진 것으로 센다. 부등호 하나 차이다.",
         """
fun mergeCount(intervals: IntArray): Int {
    val n = intervals.size / 2
    val order = (0 until n).sortedBy { intervals[2 * it] }
    var count = 0
    var end = Long.MIN_VALUE
    for (i in order) {
        val start = intervals[2 * i]
        val finish = intervals[2 * i + 1]
        if (start >= end) { count += 1; end = finish.toLong() }
        else if (finish > end) end = finish.toLong()
    }
    return count
}
"""),
        ("no-sort--assumes-order", "MISSING_EDGE_CASE",
         "정렬하지 않는다. 입력이 시작점 순이면 맞고 아니면 틀린다.",
         """
fun mergeCount(intervals: IntArray): Int {
    var count = 0
    var end = Long.MIN_VALUE
    for (i in 0 until intervals.size / 2) {
        val start = intervals[2 * i]
        val finish = intervals[2 * i + 1]
        if (start > end) { count += 1; end = finish.toLong() }
        else if (finish > end) end = finish.toLong()
    }
    return count
}
"""),
        ("quadratic--merges-pairwise", "PERFORMANCE",
         "구간마다 다른 모든 구간과 겹치는지 본다. O(n²).",
         """
fun mergeCount(intervals: IntArray): Int {
    val n = intervals.size / 2
    val parent = IntArray(n) { it }
    fun find(x: Int): Int { var v = x; while (parent[v] != v) v = parent[v]; return v }
    for (i in 0 until n) for (j in i + 1 until n) {
        Drill.compare(i, j)
        val overlap = intervals[2 * i] <= intervals[2 * j + 1] && intervals[2 * j] <= intervals[2 * i + 1]
        if (overlap) { val a = find(i); val b = find(j); if (a != b) parent[b] = a }
    }
    return (0 until n).count { find(it) == it }
}
"""),
    ],
))


# --- 2. 회의실 수 ---------------------------------------------------------------

def _rooms(intervals):
    import heapq
    pairs = sorted((intervals[i], intervals[i + 1]) for i in range(0, len(intervals), 2))
    heap = []
    best = 0
    for start, finish in pairs:
        while heap and heap[0] <= start:
            heapq.heappop(heap)
        heapq.heappush(heap, finish)
        best = max(best, len(heap))
    return best


PROBLEMS.append(Problem(
    id="meeting-rooms",
    title="필요한 회의실 수",
    summary="""
회의 시간이 `[s1, e1, s2, e2, ...]` 로 평탄하게 주어진다. 회의는 `s` 에 시작해 `e` 에
끝나며, **끝나는 순간 그 방을 다음 회의가 쓸 수 있다.** 모든 회의를 치르는 데 필요한
회의실의 최소 개수를 반환한다.
""",
    notes="""
답은 "어느 한 순간에 동시에 열려 있는 회의의 최대 수"다. 시작 순으로 보면서, 지금 시작
전에 끝난 회의의 방을 돌려받으면 된다. 가장 먼저 끝나는 회의를 빨리 찾는 구조가 힙이다.
""",
    drill_doc="""
Drill.push(finish)             // 방을 하나 차지했다 (끝나는 시각)
Drill.pop(finish)              // 끝난 회의의 방을 돌려받았다
Drill.match(i, rooms)          // 동시에 열린 수가 늘었다
""",
    constraints="""
- `2 <= intervals.size <= 200_000`, 짝수
- `0 <= s < e <= 10^9`
""",
    signature=dict(name="minRooms", parameters=[("intervals", "INT_ARRAY")], returns="INT"),
    groups=perf_groups(),
    reference=_rooms,
    cases={
        "sample": [
            ("01", [[0, 30, 5, 10, 15, 20]]),
            ("02", [[7, 10, 2, 4]]),
        ],
        "boundary": [
            ("01-single", [[1, 2]]),
            # 끝나는 순간 시작하는 회의는 같은 방을 쓴다.
            ("02-back-to-back", [[1, 3, 3, 5, 5, 7]]),
            # 전부 겹친다.
            ("03-all-overlap", [[1, 10, 2, 9, 3, 8, 4, 7]]),
            # 긴 회의 하나 위에 짧은 것들이 차례로.
            ("04-long-plus-short", [[0, 100, 1, 2, 3, 4, 5, 6]]),
            ("05-unsorted", [[10, 20, 0, 5, 3, 12]]),
        ],
        "hidden": [
            ("01-staircase", [flat([i, i + 10] for i in range(0, 50, 2))]),
            ("02-large-values", [[0, 1000000000, 999999999, 1000000000]]),
            ("03-many-same", [[5, 6] * 40]),
        ],
        "performance": [
            ("01-small", [flat([v, v + 50] for v in randoms(2000, 0, 100000, salt=911))]),
            ("02-medium", [flat([v, v + 500] for v in randoms(20000, 0, 1000000, salt=912))]),
            ("03-large", [flat([v, v + 5000] for v in randoms(100000, 0, 100000000, salt=913))]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 시작 순 정렬 + 끝나는 시각의 최소 힙.
import java.util.PriorityQueue

fun minRooms(intervals: IntArray): Int {
    val n = intervals.size / 2
    val order = (0 until n).sortedBy { intervals[2 * it] }
    val ends = PriorityQueue<Int>()
    var best = 0
    for (i in order) {
        val start = intervals[2 * i]
        val finish = intervals[2 * i + 1]
        while (ends.isNotEmpty() && ends.peek() <= start) {
            Drill.pop(ends.poll())
        }
        ends.add(finish)
        Drill.push(finish)
        if (ends.size > best) {
            best = ends.size
            Drill.match(i, best)
        }
    }
    return best
}
""",
    mutants=[
        ("strict-release--same-time-conflicts", "OFF_BY_ONE",
         "끝나는 순간 시작하는 회의를 겹친 것으로 센다. 부등호 하나 차이다.",
         """
import java.util.PriorityQueue

fun minRooms(intervals: IntArray): Int {
    val n = intervals.size / 2
    val order = (0 until n).sortedBy { intervals[2 * it] }
    val ends = PriorityQueue<Int>()
    var best = 0
    for (i in order) {
        while (ends.isNotEmpty() && ends.peek() < intervals[2 * i]) ends.poll()
        ends.add(intervals[2 * i + 1])
        best = maxOf(best, ends.size)
    }
    return best
}
"""),
        ("no-sort--assumes-order", "MISSING_EDGE_CASE",
         "시작 순으로 정렬하지 않는다. 입력이 시간 순이면 맞고 아니면 방을 돌려받을 타이밍이 어긋난다.",
         """
import java.util.PriorityQueue

fun minRooms(intervals: IntArray): Int {
    val ends = PriorityQueue<Int>()
    var best = 0
    for (i in 0 until intervals.size / 2) {
        while (ends.isNotEmpty() && ends.peek() <= intervals[2 * i]) ends.poll()
        ends.add(intervals[2 * i + 1])
        best = maxOf(best, ends.size)
    }
    return best
}
"""),
        ("max-end--wrong-priority", "WRONG_ALGORITHM",
         "가장 늦게 끝나는 회의를 먼저 본다. 방을 돌려받을 기회를 놓친다.",
         """
import java.util.PriorityQueue

fun minRooms(intervals: IntArray): Int {
    val n = intervals.size / 2
    val order = (0 until n).sortedBy { intervals[2 * it] }
    val ends = PriorityQueue<Int>(compareByDescending { it })
    var best = 0
    for (i in order) {
        while (ends.isNotEmpty() && ends.peek() <= intervals[2 * i]) ends.poll()
        ends.add(intervals[2 * i + 1])
        best = maxOf(best, ends.size)
    }
    return best
}
"""),
        ("quadratic--counts-overlaps-per-meeting", "PERFORMANCE",
         "회의마다 자기 시작 시각에 열려 있는 회의를 전부 센다. O(n²).",
         """
fun minRooms(intervals: IntArray): Int {
    val n = intervals.size / 2
    var best = 0
    for (i in 0 until n) {
        val start = intervals[2 * i]
        var open = 0
        for (j in 0 until n) {
            Drill.compare(i, j)
            if (intervals[2 * j] <= start && start < intervals[2 * j + 1]) open += 1
        }
        if (open > best) best = open
    }
    return best
}
"""),
    ],
))


# --- 3. 점프 게임 ---------------------------------------------------------------

def _can_jump(steps):
    reach = 0
    for i, s in enumerate(steps):
        if i > reach:
            return 0
        reach = max(reach, i + s)
    return 1


PROBLEMS.append(Problem(
    id="jump-game",
    title="끝까지 갈 수 있는가",
    summary="""
각 칸에서 최대 몇 칸까지 앞으로 뛸 수 있는지를 담은 배열 `steps` 가 주어진다. 첫 칸에서
시작해 **마지막 칸에 닿을 수 있으면** `1`, 없으면 `0` 을 반환한다.
""",
    notes="""
"지금까지 닿을 수 있는 가장 먼 칸"을 하나 들고 훑는다. 어떤 칸이 그보다 멀면 거기 갈 수
없고, 그러면 그 뒤도 갈 수 없다.
""",
    drill_doc="""
Drill.visit(i, steps[i])       // i 번째 칸을 봤다
Drill.pointer("reach", reach)  // 닿을 수 있는 가장 먼 칸이 늘었다
""",
    constraints="""
- `1 <= steps.size <= 100_000`
- `0 <= steps[i] <= 100_000`
""",
    signature=dict(name="canJump", parameters=[("steps", "INT_ARRAY")], returns="INT"),
    groups=standard_groups(),
    reference=_can_jump,
    cases={
        "sample": [
            ("01", [[2, 3, 1, 1, 4]]),
            ("02", [[3, 2, 1, 0, 4]]),
        ],
        "boundary": [
            # 칸 하나면 이미 도착이다.
            ("01-single", [[0]]),
            # 0 이 마지막 칸이면 갇힌 것이 아니다.
            ("02-zero-at-end", [[1, 0]]),
            # 정확히 닿는다.
            ("03-exact", [[1, 1, 1, 1]]),
            # 큰 점프 하나가 0 들을 넘는다.
            ("04-long-jump", [[5, 0, 0, 0, 0, 1]]),
            ("05-stuck-early", [[0, 5]]),
            # 최대로 뛰면 0 위에 떨어진다. 한 칸만 뛰어야 산다.
            ("06-max-jump-traps", [[2, 3, 0, 0, 1]]),
        ],
        "hidden": [
            ("01-long-ones", [[1] * 1000]),
            ("02-trap-middle", [[3, 2, 1, 0, 0, 0, 1]]),
            ("03-big-first", [[100000] + [0] * 500]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 닿을 수 있는 가장 먼 칸 하나를 들고 훑는다.
fun canJump(steps: IntArray): Int {
    var reach = 0
    for (i in steps.indices) {
        Drill.visit(i, steps[i])
        if (i > reach) return 0
        if (i + steps[i] > reach) {
            reach = i + steps[i]
            Drill.pointer("reach", reach)
        }
    }
    return 1
}
""",
    mutants=[
        ("zero-means-stuck--ignores-last", "MISSING_EDGE_CASE",
         "0 을 만나면 무조건 갇혔다고 본다. 마지막 칸의 0 은 도착이다.",
         """
fun canJump(steps: IntArray): Int {
    var reach = 0
    for (i in steps.indices) {
        if (i > reach) return 0
        if (steps[i] == 0) return 0
        reach = maxOf(reach, i + steps[i])
    }
    return 1
}
"""),
        ("greedy-max-jump--always-farthest", "WRONG_ALGORITHM",
         "매번 최대로 뛴다. 최대로 뛰면 0 위에 떨어지는 경우를 놓친다.",
         """
fun canJump(steps: IntArray): Int {
    var i = 0
    while (i < steps.size - 1) {
        if (steps[i] == 0) return 0
        i += steps[i]
    }
    return 1
}
"""),
        ("off-by-one--needs-past-end", "OFF_BY_ONE",
         "마지막 칸을 넘어야 도착으로 본다. 정확히 닿는 경우를 실패로 낸다.",
         """
fun canJump(steps: IntArray): Int {
    var reach = 0
    for (i in steps.indices) {
        if (i > reach) return 0
        reach = maxOf(reach, i + steps[i])
    }
    return if (reach > steps.size - 1) 1 else 0
}
"""),
    ],
))


# --- 4. 상위 k 빈도 -------------------------------------------------------------

def _top_k(nums, k):
    from collections import Counter
    counts = Counter(nums)
    ordered = sorted(counts.items(), key=lambda kv: (-kv[1], kv[0]))
    return [value for value, _ in ordered[:k]]


PROBLEMS.append(Problem(
    id="top-k-frequent",
    title="가장 자주 나온 k 개",
    summary="""
정수 배열 `nums` 와 정수 `k` 가 주어진다. 가장 자주 나온 값 `k` 개를 **빈도 내림차순**으로,
빈도가 같으면 **값 오름차순**으로 담아 반환한다.
""",
    notes="""
세는 것과 고르는 것이 따로다. 세는 것은 해시맵이고, 고르는 것은 정렬이거나 크기 k 의
힙이다. 순서 규칙이 둘이라 비교 기준을 정확히 적어야 한다.
""",
    drill_doc="""
Drill.visit(i, value)          // i 번째 값을 셌다
Drill.push(value)              // 후보에 넣었다
Drill.pop(value)               // 후보에서 밀려났다
""",
    constraints="""
- `1 <= nums.size <= 100_000`
- `-10^6 <= nums[i] <= 10^6`
- `1 <= k <= 서로 다른 값의 수`
""",
    signature=dict(name="topK", parameters=[("nums", "INT_ARRAY"), ("k", "INT")], returns="INT_ARRAY"),
    groups=standard_groups(),
    reference=_top_k,
    cases={
        "sample": [
            ("01", [[1, 1, 1, 2, 2, 3], 2]),
            ("02", [[4, 4, 5, 5, 6], 3]),
        ],
        "boundary": [
            ("01-single", [[7], 1]),
            # 빈도가 전부 같다. 값 오름차순만이 순서를 정한다.
            # 해시맵 순서가 오름차순과 다르도록 값을 고른다. 작은 양수만 쓰면
            # 해시맵이 우연히 오름차순이라 tie-break 누락이 잡히지 않는다.
            ("02-all-tied", [[100, 3, 17], 3]),
            # 음수와 0.
            ("03-negative", [[-1, -1, 0, 0, 0, 5], 2]),
            # k 가 종류 수와 같다.
            ("04-k-equals-distinct", [[2, 2, 9, 9, 9, 1], 3]),
        ],
        "hidden": [
            ("01-mixed", [shuffled([1] * 10 + [2] * 20 + [3] * 5 + [4] * 20, salt=921), 2]),
            ("02-large", [[v % 100 for v in randoms(50000, 0, 999, salt=922)], 5]),
            ("03-ties-many", [shuffled(list(range(50)) * 3, salt=923), 10]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 해시맵으로 세고, (빈도 내림, 값 오름)으로 정렬해 k 개.
fun topK(nums: IntArray, k: Int): IntArray {
    val counts = HashMap<Int, Int>()
    for (i in nums.indices) {
        Drill.visit(i, nums[i])
        counts[nums[i]] = (counts[nums[i]] ?: 0) + 1
    }
    val ordered = counts.entries.sortedWith(compareByDescending<Map.Entry<Int, Int>> { it.value }.thenBy { it.key })
    val out = IntArray(k)
    for (i in 0 until k) {
        out[i] = ordered[i].key
        Drill.push(out[i])
    }
    return out
}
""",
    mutants=[
        ("tie-descending--wrong-secondary", "WRONG_BRANCH",
         "빈도가 같을 때 값을 내림차순으로 둔다. 둘째 정렬 기준을 반대로 읽었다.",
         """
fun topK(nums: IntArray, k: Int): IntArray {
    val counts = HashMap<Int, Int>()
    for (v in nums) counts[v] = (counts[v] ?: 0) + 1
    val ordered = counts.entries.sortedWith(compareByDescending<Map.Entry<Int, Int>> { it.value }.thenByDescending { it.key })
    return IntArray(k) { ordered[it].key }
}
"""),
        ("ascending-frequency--bottom-k", "WRONG_BRANCH",
         "빈도 오름차순으로 정렬한다. 가장 드문 값을 낸다.",
         """
fun topK(nums: IntArray, k: Int): IntArray {
    val counts = HashMap<Int, Int>()
    for (v in nums) counts[v] = (counts[v] ?: 0) + 1
    val ordered = counts.entries.sortedWith(compareBy<Map.Entry<Int, Int>> { it.value }.thenBy { it.key })
    return IntArray(k) { ordered[it].key }
}
"""),
        ("no-tiebreak--map-order", "MISSING_EDGE_CASE",
         "빈도만으로 정렬한다. 빈도가 같으면 해시맵 순서가 답이 된다.",
         """
fun topK(nums: IntArray, k: Int): IntArray {
    val counts = HashMap<Int, Int>()
    for (v in nums) counts[v] = (counts[v] ?: 0) + 1
    val ordered = counts.entries.sortedByDescending { it.value }
    return IntArray(k) { ordered[it].key }
}
"""),
    ],
))


# --- 5. 밧줄 잇기 비용 ------------------------------------------------------------

def _connect_cost(lengths):
    import heapq
    heap = list(lengths)
    heapq.heapify(heap)
    total = 0
    while len(heap) > 1:
        a = heapq.heappop(heap)
        b = heapq.heappop(heap)
        total += a + b
        heapq.heappush(heap, a + b)
    return total


PROBLEMS.append(Problem(
    id="connect-ropes",
    title="밧줄을 하나로 잇는 최소 비용",
    summary="""
밧줄의 길이가 배열 `lengths` 로 주어진다. 두 밧줄을 하나로 이으면 **두 길이의 합**만큼
비용이 들고, 이은 밧줄의 길이는 그 합이다. 모든 밧줄을 하나로 이을 때까지의 **최소 총
비용**을 반환한다. 밧줄이 하나뿐이면 `0` 이다.
""",
    notes="""
먼저 이은 밧줄은 뒤의 잇기마다 비용에 다시 들어간다. 그러니 긴 것은 늦게, 짧은 것은
일찍 이어야 한다 — 매번 **지금 가장 짧은 둘**을 잇는다. 이은 결과가 다시 후보에 들어가므로
정렬 한 번으로는 안 되고, 가장 작은 둘을 계속 꺼낼 수 있는 구조가 필요하다.
""",
    drill_doc="""
Drill.compare(a, b)           // 가장 짧은 둘을 골랐다
Drill.push(a + b)             // 이은 밧줄을 다시 넣었다
Drill.write(0, total)         // 지금까지의 비용
""",
    constraints="""
- `1 <= lengths.size <= 100_000`
- `1 <= lengths[i] <= 1000`, 총 비용은 `Int` 범위 안
""",
    signature=dict(name="connectCost", parameters=[("lengths", "INT_ARRAY")], returns="INT"),
    groups=perf_groups(),
    reference=_connect_cost,
    cases={
        "sample": [
            ("01", [[4, 3, 2, 6]]),
            ("02", [[1, 8, 3, 5]]),
        ],
        "boundary": [
            # 하나뿐. 이을 것이 없다.
            ("01-single", [[7]]),
            ("02-two", [[3, 9]]),
            # 전부 같다. 어느 순서든 같다 — 여기서는 갈리지 않아야 한다.
            ("03-all-equal", [[5, 5, 5, 5]]),
            # 정렬한 뒤 앞에서부터 차례로 이으면 틀린다: 이은 결과가 남은 것보다 클 수 있다.
            ("04-merged-exceeds", [[1, 2, 3, 4, 5]]),
            # 짝지어 이으면 틀린다.
            ("05-pairwise-wrong", [[1, 1, 1, 100]]),
        ],
        "hidden": [
            ("01-random-small", [randoms(20, 1, 100, salt=1701)]),
            ("02-random-medium", [randoms(500, 1, 1000, salt=1702)]),
            ("03-descending", [list(range(200, 0, -1))]),
            ("04-ones-and-big", [[1] * 50 + [1000] * 5]),
        ],
        "performance": [
            ("01-small", [randoms(3000, 1, 1000, salt=1703)]),
            ("02-medium", [randoms(20000, 1, 1000, salt=1704)]),
            ("03-large", [randoms(100000, 1, 1000, salt=1705)]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 최소 힙에서 가장 짧은 둘을 꺼내 잇고 다시 넣는다.
fun connectCost(lengths: IntArray): Int {
    val heap = java.util.PriorityQueue<Int>()
    for (v in lengths) heap.add(v)
    var total = 0
    while (heap.size > 1) {
        val a = heap.poll()
        val b = heap.poll()
        Drill.compare(a, b)
        total += a + b
        Drill.push(a + b)
        heap.add(a + b)
        Drill.write(0, total)
    }
    return total
}
""",
    mutants=[
        ("sort-once--merge-in-order", "WRONG_ALGORITHM",
         "정렬한 뒤 앞에서부터 차례로 잇는다. 이은 결과가 남은 것보다 길어지는 순간을 놓친다.",
         """
fun connectCost(lengths: IntArray): Int {
    val sorted = lengths.sorted()
    var total = 0
    var current = sorted[0]
    for (i in 1 until sorted.size) { current += sorted[i]; total += current }
    return total
}
"""),
        ("pairwise--merges-neighbors", "WRONG_ALGORITHM",
         "정렬한 뒤 이웃끼리 짝지어 잇는다. 짧은 것 셋을 먼저 잇는 편이 싼 경우를 놓친다.",
         """
fun connectCost(lengths: IntArray): Int {
    var current = lengths.sorted()
    var total = 0
    while (current.size > 1) {
        val next = mutableListOf<Int>()
        var i = 0
        while (i + 1 < current.size) { total += current[i] + current[i + 1]; next.add(current[i] + current[i + 1]); i += 2 }
        if (i < current.size) next.add(current[i])
        current = next.sorted()
    }
    return total
}
"""),
        ("counts-final-length--not-cost", "WRONG_BRANCH",
         "마지막 밧줄의 길이를 답한다. 비용은 잇는 횟수마다 쌓인다.",
         """
fun connectCost(lengths: IntArray): Int {
    val heap = java.util.PriorityQueue<Int>()
    for (v in lengths) heap.add(v)
    while (heap.size > 1) heap.add(heap.poll() + heap.poll())
    return if (lengths.size == 1) 0 else heap.poll()
}
"""),
        ("scan-min--quadratic", "PERFORMANCE",
         "매번 목록을 훑어 가장 짧은 둘을 찾는다. O(n²).",
         """
fun connectCost(lengths: IntArray): Int {
    val list = lengths.toMutableList()
    var total = 0
    while (list.size > 1) {
        var a = 0
        for (i in 1 until list.size) { Drill.compare(i, a); if (list[i] < list[a]) a = i }
        val first = list.removeAt(a)
        var b = 0
        for (i in 1 until list.size) if (list[i] < list[b]) b = i
        val second = list.removeAt(b)
        total += first + second
        list.add(first + second)
    }
    return total
}
"""),
    ],
))


# --- 124. 냉각 시간이 있는 작업 (탐욕 셈) ---------------------------------------------------------

def _least_interval(tasks, n):
    from collections import Counter
    counts = Counter(tasks)
    most = max(counts.values())
    ties = sum(1 for v in counts.values() if v == most)
    return max(len(tasks), (most - 1) * (n + 1) + ties)


def _tasks(length, kinds, salt):
    return "".join(chr(ord("A") + d) for d in randoms(length, 0, kinds - 1, salt=salt))


PROBLEMS.append(Problem(
    id="cooldown-schedule",
    title="냉각 시간이 있는 작업",
    summary="""
CPU 가 할 작업들이 대문자 한 글자씩 `tasks` 로 주어진다. 같은 글자는 같은 종류의 작업이다.
한 칸의 시간에 작업 하나를 하거나 쉰다. **같은 종류의 작업 사이에는 적어도 `n` 칸**이
있어야 한다 — 그 사이에 다른 작업을 하거나 쉬어야 한다.

모든 작업을 마치는 데 필요한 **최소 칸 수**를 반환한다.
""",
    notes="""
가장 많은 종류의 작업이 뼈대를 만든다. 그 작업이 `m` 번이면 `m-1` 개의 틈이 있고 틈마다
`n` 칸이다 — `(m-1)(n+1)` 칸 뒤에 마지막 것을 놓는다. 가장 많은 종류가 여럿이면 마지막 줄에
그 수만큼 나란히 선다. 작업이 그 뼈대보다 많으면 쉴 필요가 없어 답은 작업 수다.
""",
    drill_doc="""
Drill.compare(count, most)    // 종류별 횟수와 최댓값을 비교했다
Drill.write(kind, count)      // 종류별 횟수를 셌다
""",
    constraints="""
- `1 <= tasks.length <= 100_000`
- `tasks` 는 `A`~`Z` 로만 되어 있다
- `0 <= n <= 100`
""",
    signature=dict(
        name="leastInterval",
        parameters=[("tasks", "STRING"), ("n", "INT")],
        returns="INT",
    ),
    groups=standard_groups(),
    reference=_least_interval,
    cases={
        "sample": [
            ("01", ["AAABBB", 2]),
            ("02", ["AAABBB", 0]),
        ],
        "boundary": [
            ("01-single", ["A", 5]),
            # 뼈대보다 작업이 많으면 쉴 틈이 없다.
            ("02-more-tasks-than-frame", ["AAABBBCCCDDE", 2]),
            # 가장 많은 종류가 셋 — 마지막 줄에 셋이 선다.
            ("03-three-way-tie", ["AABBCC", 3]),
            ("04-no-cooldown", ["ABCABC", 0]),
            # 한 종류뿐이면 틈마다 n 칸을 전부 쉰다.
            ("05-one-kind", ["AAAA", 3]),
            ("06-tie-and-extra", ["AAABBBC", 2]),
        ],
        "hidden": [
            ("01-random-small", [_tasks(12, 3, salt=8331), 2]),
            ("02-random-medium", [_tasks(200, 5, salt=8332), 7]),
            ("03-random-large", [_tasks(100000, 26, salt=8333), 100]),
            ("04-one-kind-large", ["Z" * 1000, 100]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 가장 많은 작업이 뼈대, 나머지는 틈에 들어간다.
fun leastInterval(tasks: String, n: Int): Int {
    val counts = IntArray(26)
    for (ch in tasks) counts[ch - 'A'] += 1
    var most = 0
    var ties = 0
    for (kind in 0 until 26) {
        Drill.write(kind, counts[kind])
        Drill.compare(counts[kind], most)
        if (counts[kind] > most) { most = counts[kind]; ties = 1 } else if (counts[kind] == most && most > 0) ties += 1
    }
    return maxOf(tasks.length, (most - 1) * (n + 1) + ties)
}
""",
    mutants=[
        ("no-length-clamp", "MISSING_EDGE_CASE",
         "작업이 뼈대보다 많을 때도 뼈대 길이를 답한다. 작업 수보다 작은 답이 나온다.",
         """
fun leastInterval(tasks: String, n: Int): Int {
    val counts = IntArray(26)
    for (ch in tasks) counts[ch - 'A'] += 1
    val most = counts.max()
    val ties = counts.count { it == most }
    return (most - 1) * (n + 1) + ties
}
"""),
        ("ignores-ties", "OFF_BY_ONE",
         "가장 많은 종류가 여럿이어도 마지막 줄에 하나만 센다.",
         """
fun leastInterval(tasks: String, n: Int): Int {
    val counts = IntArray(26)
    for (ch in tasks) counts[ch - 'A'] += 1
    val most = counts.max()
    return maxOf(tasks.length, (most - 1) * (n + 1) + 1)
}
"""),
        ("gap-is-n--not-n-plus-one", "OFF_BY_ONE",
         "틈 하나를 n 칸으로 센다. 같은 작업 사이는 n 칸이지만 작업 자체까지 n+1 칸이 한 주기다.",
         """
fun leastInterval(tasks: String, n: Int): Int {
    val counts = IntArray(26)
    for (ch in tasks) counts[ch - 'A'] += 1
    val most = counts.max()
    val ties = counts.count { it == most }
    return maxOf(tasks.length, (most - 1) * n + ties)
}
"""),
    ],
))


# --- 126. 마감 전에 끝낼 수 있는 작업의 최대 수 (정렬 + 힙) -------------------------------------------

def _max_tasks(durations, deadlines):
    import heapq
    tasks = sorted(zip(deadlines, durations))
    heap = []
    elapsed = 0
    for deadline, duration in tasks:
        elapsed += duration
        heapq.heappush(heap, -duration)
        if elapsed > deadline:
            elapsed += heapq.heappop(heap)  # 가장 긴 것을 뺀다 (음수라 더한다)
    return len(heap)


def _tasks_random(n, salt):
    # 마감의 간격을 평균 걸리는 시간의 절반(250/500)으로 둔다. 남는 작업의 비율 f 가 250/500 이고
    # 빼는 횟수는 (1-f)·n 이라, 힙 없이 훑는 풀이의 일은 n²·f(1-f)/2 — f = 1/2 에서 가장 크다.
    # 걸리는 시간을 고르게 둔다(450~550). 편차가 크면 긴 것 하나를 빼는 것으로 한참을 버텨 뺄 일이 드물다.
    durations = randoms(n, 450, 550, salt=salt)
    order = shuffled(range(n), salt=salt + 1)
    deadlines = [0] * n
    for rank, i in enumerate(order):
        deadlines[i] = 250 * (rank + 1)
    return durations, deadlines


PROBLEMS.append(Problem(
    id="max-tasks-before-deadlines",
    title="마감 전에 끝낼 수 있는 작업의 최대 수",
    summary="""
작업이 `n` 개 있다. `durations[i]` 는 `i` 번째 작업에 걸리는 시간, `deadlines[i]` 는 그 작업을
**끝내야 하는** 시각이다. 시각 `0` 부터 작업을 하나씩 골라 쉬지 않고 이어서 한다 — 한 번에
하나, 중간에 끊지 않는다. 작업은 끝나는 시각이 마감 **이하**여야 한다.

끝낼 수 있는 작업 수의 최댓값을 반환한다. 어떤 작업을 골라 어떤 순서로 할지는 자유다.
""",
    notes="""
마감이 이른 순서로 보면서 일단 넣고, 넣은 것들의 합이 지금 작업의 마감을 넘기면 **지금까지
넣은 것 중 가장 긴 하나**를 뺀다. 뺄 것을 빨리 찾는 구조가 최대 힙이다. 남은 작업 수가
답이다 — 무엇을 남겼는지는 안 물어본다.
""",
    drill_doc="""
Drill.push(duration)          // 작업을 힙에 넣었다
Drill.pop(duration)           // 가장 긴 작업을 뺐다
Drill.compare(elapsed, deadline)
""",
    constraints="""
- `1 <= n <= 100_000`
- `1 <= durations[i] <= 1000`, `1 <= deadlines[i] <= 10^8`
- `durations.size == deadlines.size`
""",
    signature=dict(
        name="maxTasksBeforeDeadlines",
        parameters=[("durations", "INT_ARRAY"), ("deadlines", "INT_ARRAY")],
        returns="INT",
    ),
    # 힙 없이 훑는 오답의 일이 n²/8 이라 10 만에서 한도를 배로밖에 못 넘긴다. 입력은 상한이라 한도를 조인다.
    groups=perf_groups(time_multiplier=0.25),
    reference=_max_tasks,
    cases={
        "sample": [
            ("01", [[3, 2, 1], [3, 5, 5]]),
            ("02", [[5, 5], [4, 4]]),
        ],
        "boundary": [
            ("01-single-fits", [[3], [3]]),
            ("02-single-late", [[4], [3]]),
            # 마감이 이른 것을 먼저 해야 둘 다 된다.
            ("03-order-matters", [[2, 1], [3, 1]]),
            # 짧은 것을 남기고 긴 것을 버려야 셋이 된다.
            ("04-drop-longest", [[5, 1, 1, 1], [5, 6, 7, 8]]),
            # 넣은 것을 나중에 빼야 하는 경우 — 처음 넣을 때는 맞았다.
            ("05-evict-earlier-task", [[4, 1, 1], [4, 5, 5]]),
            ("06-all-same-deadline", [[3, 3, 3, 3], [6, 6, 6, 6]]),
            ("07-none-fit", [[9, 9], [1, 2]]),
        ],
        "hidden": [
            ("01-random-small", [randoms(8, 1, 10, salt=8401), randoms(8, 1, 30, salt=8402)]),
            ("02-random-medium", [randoms(60, 1, 50, salt=8403), randoms(60, 1, 400, salt=8404)]),
            ("03-tight", [[1, 2, 3, 4, 5], [1, 3, 6, 10, 15]]),
            ("04-random-large-values", [randoms(200, 1, 1000, salt=8405), randoms(200, 1, 20000, salt=8406)]),
        ],
        "performance": [
            ("01-small", list(_tasks_random(3000, salt=8411))),
            ("02-medium", list(_tasks_random(30000, salt=8413))),
            ("03-large", list(_tasks_random(100000, salt=8415))),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 마감순으로 넣고 넘치면 가장 긴 것을 뺀다.
fun maxTasksBeforeDeadlines(durations: IntArray, deadlines: IntArray): Int {
    val order = durations.indices.sortedBy { deadlines[it] }
    val heap = java.util.PriorityQueue<Int>(compareByDescending { it })
    var elapsed = 0L
    for (i in order) {
        elapsed += durations[i]
        heap.add(durations[i])
        Drill.push(durations[i])
        Drill.compare(elapsed.toInt(), deadlines[i])
        if (elapsed > deadlines[i]) {
            val longest = heap.poll()
            Drill.pop(longest)
            elapsed -= longest
        }
    }
    return heap.size
}
""",
    mutants=[
        ("no-eviction--skip-when-late", "WRONG_ALGORITHM",
         "마감을 넘기는 작업을 건너뛰기만 한다. 이미 넣은 긴 작업을 빼고 지금 것을 넣는 편이 나을 때를 놓친다.",
         """
fun maxTasksBeforeDeadlines(durations: IntArray, deadlines: IntArray): Int {
    val order = durations.indices.sortedBy { deadlines[it] }
    var elapsed = 0L
    var count = 0
    for (i in order) {
        if (elapsed + durations[i] <= deadlines[i]) { elapsed += durations[i]; count += 1 }
    }
    return count
}
"""),
        ("sorted-by-duration--not-deadline", "WRONG_BRANCH",
         "마감이 아니라 걸리는 시간 순으로 본다. 마감이 이른 것을 뒤로 미룬다.",
         """
fun maxTasksBeforeDeadlines(durations: IntArray, deadlines: IntArray): Int {
    val order = durations.indices.sortedBy { durations[it] }
    val heap = java.util.PriorityQueue<Int>(compareByDescending { it })
    var elapsed = 0L
    for (i in order) {
        elapsed += durations[i]; heap.add(durations[i])
        if (elapsed > deadlines[i]) elapsed -= heap.poll()
    }
    return heap.size
}
"""),
        ("evicts-shortest", "WRONG_BRANCH",
         "넘치면 가장 짧은 작업을 뺀다. 시간을 가장 적게 되찾는다.",
         """
fun maxTasksBeforeDeadlines(durations: IntArray, deadlines: IntArray): Int {
    val order = durations.indices.sortedBy { deadlines[it] }
    val heap = java.util.PriorityQueue<Int>()
    var elapsed = 0L
    for (i in order) {
        elapsed += durations[i]; heap.add(durations[i])
        if (elapsed > deadlines[i]) elapsed -= heap.poll()
    }
    return heap.size
}
"""),
        ("scan-for-longest--quadratic", "PERFORMANCE",
         "힙 없이 넣은 작업들을 목록에 두고 넘칠 때마다 훑어 가장 긴 것을 찾는다. O(n²).",
         """
fun maxTasksBeforeDeadlines(durations: IntArray, deadlines: IntArray): Int {
    val order = durations.indices.sortedBy { deadlines[it] }
    val chosen = ArrayList<Int>()
    var elapsed = 0L
    for (i in order) {
        elapsed += durations[i]; chosen.add(durations[i])
        if (elapsed > deadlines[i]) {
            var longest = 0
            for (k in chosen.indices) { Drill.compare(chosen[k], chosen[longest]); if (chosen[k] > chosen[longest]) longest = k }
            elapsed -= chosen[longest]
            chosen.removeAt(longest)
        }
    }
    return chosen.size
}
"""),
    ],
))
