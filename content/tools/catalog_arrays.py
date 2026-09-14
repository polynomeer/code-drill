"""배열·투 포인터·슬라이딩 윈도우 (역량: 인덱스 다루기, 불변식 유지).

각 문제의 `mutants` 는 그 알고리즘에서 **실제로 자주 나는 실수**다. 아무 코드나 망가뜨려
놓으면 kill rate 는 쉽게 100% 가 되지만, 그 숫자는 테스트가 좋다는 뜻이 아니라 오답이
엉성하다는 뜻이다.
"""

from author import Problem, standard_groups, perf_groups, randoms, shuffled

PROBLEMS = []


# --- 1. 정렬 배열의 두 수 합 -------------------------------------------------

def _pair_sum(nums, target):
    left, right = 0, len(nums) - 1
    while left < right:
        total = nums[left] + nums[right]
        if total == target:
            return [left, right]
        if total < target:
            left += 1
        else:
            right -= 1
    return [-1, -1]


PROBLEMS.append(Problem(
    id="pair-sum-sorted",
    title="정렬된 두 수의 합",
    summary="""
**오름차순으로 정렬된** 정수 배열 `nums` 와 정수 `target` 이 주어진다. 더해서 `target`
이 되는 두 원소의 인덱스를 오름차순으로 담아 반환한다.

- 정답은 항상 정확히 하나 존재한다.
- 같은 원소를 두 번 쓸 수 없다.
""",
    notes="""
정렬되어 있다는 사실이 핵심이다. 해시맵으로도 풀리지만, 그러면 정렬이라는 조건을 쓰지
않은 것이다.
""",
    drill_doc="""
Drill.pointer("left", left)    // 왼쪽 포인터를 옮겼다
Drill.pointer("right", right)  // 오른쪽 포인터를 옮겼다
Drill.compare(left, right)     // 두 끝의 합을 봤다
Drill.match(left, right)       // 답을 찾았다
""",
    constraints="""
- `2 <= nums.size <= 100_000`
- `-10^9 <= nums[i] <= 10^9`, 오름차순 정렬
- 두 원소의 합은 `Int` 범위를 넘지 않는다
""",
    signature=dict(name="pairSum", parameters=[("nums", "INT_ARRAY"), ("target", "INT")],
                   returns="INT_ARRAY"),
    groups=standard_groups(),
    reference=_pair_sum,
    cases={
        "sample": [
            ("01", [[2, 7, 11, 15], 9]),
            ("02", [[1, 3, 4, 6, 8], 10]),
        ],
        "boundary": [
            # 길이가 최소인 경우. 포인터가 한 번도 움직이지 않는다.
            ("01-min-size", [[3, 5], 8]),
            # 음수가 섞이면 "합이 작으면 왼쪽을 민다"는 불변식만이 답을 준다.
            ("02-negative", [[-8, -3, 0, 2, 9], -6]),
            # 답이 양 끝이다. 한쪽 포인터만 움직이는 구현이 걸린다.
            ("03-both-ends", [[1, 2, 3, 4, 100], 101]),
            # 답이 가운데 인접한 두 원소다.
            ("04-adjacent-middle", [[1, 2, 40, 41, 90], 81]),
        ],
        "hidden": [
            ("01-long-tail", [list(range(0, 200, 2)) + [999], 999 + 198]),
            ("02-duplicates", [[1, 1, 1, 1, 5, 5, 9], 14]),
            ("03-large-values", [[-1000000000, -5, 0, 7, 1000000000], 0]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 투 포인터.
//
// 합이 작으면 왼쪽을 오른쪽으로, 크면 오른쪽을 왼쪽으로 민다. 정렬되어 있으므로
// 이 한 번의 이동이 "그 포인터가 만들 수 있는 다른 모든 짝"을 함께 버린다.
fun pairSum(nums: IntArray, target: Int): IntArray {
    var left = 0
    var right = nums.size - 1

    while (left < right) {
        Drill.pointer("left", left)
        Drill.pointer("right", right)
        Drill.compare(left, right)

        val total = nums[left] + nums[right]
        when {
            total == target -> {
                Drill.match(left, right)
                return intArrayOf(left, right)
            }
            total < target -> left += 1
            else -> right -= 1
        }
    }
    error("정답은 항상 존재한다")
}
""",
    mutants=[
        ("wrong-direction--moves-wrong-pointer", "WRONG_BRANCH",
         "합이 작을 때 오른쪽을 당긴다. 답이 양 끝에 있는 경우에만 우연히 맞는다.",
         """
fun pairSum(nums: IntArray, target: Int): IntArray {
    var left = 0
    var right = nums.size - 1
    while (left < right) {
        val total = nums[left] + nums[right]
        if (total == target) return intArrayOf(left, right)
        if (total < target) right -= 1 else left += 1
    }
    error("정답은 항상 존재한다")
}
"""),
        ("one-indexed--returns-positions", "OFF_BY_ONE",
         "0 부터가 아니라 1 부터 세어 돌려준다. 같은 문제의 다른 판본을 기억해 두면 나는 실수다.",
         """
fun pairSum(nums: IntArray, target: Int): IntArray {
    var left = 0
    var right = nums.size - 1
    while (left < right) {
        val total = nums[left] + nums[right]
        if (total == target) return intArrayOf(left + 1, right + 1)
        if (total < target) left += 1 else right -= 1
    }
    error("정답은 항상 존재한다")
}
"""),
        ("reversed-order--descending-indices", "WRONG_BRANCH",
         "인덱스를 내림차순으로 돌려준다.",
         """
fun pairSum(nums: IntArray, target: Int): IntArray {
    var left = 0
    var right = nums.size - 1
    while (left < right) {
        val total = nums[left] + nums[right]
        if (total == target) return intArrayOf(right, left)
        if (total < target) left += 1 else right -= 1
    }
    error("정답은 항상 존재한다")
}
"""),
    ],
))


# --- 2. 0 옮기기 -------------------------------------------------------------

def _move_zeros(nums):
    kept = [v for v in nums if v != 0]
    return kept + [0] * (len(nums) - len(kept))


PROBLEMS.append(Problem(
    id="move-zeros",
    title="0을 뒤로 밀기",
    summary="""
정수 배열 `nums` 에서 모든 `0` 을 배열의 끝으로 옮긴 결과를 반환한다.

- **0 이 아닌 원소의 상대 순서는 그대로여야 한다.**
- 배열의 길이는 바뀌지 않는다.
""",
    notes="""
"0 을 지우고 뒤에 붙인다"가 아니라 "쓰기 위치를 따로 들고 훑는다"로 풀면 추가 배열
없이 한 번에 끝난다.
""",
    drill_doc="""
Drill.visit(index, value)   // 원소를 읽었다
Drill.write(index, value)   // 쓰기 위치에 값을 넣었다
Drill.pointer("write", i)   // 쓰기 위치를 옮겼다
""",
    constraints="""
- `1 <= nums.size <= 100_000`
- `-10^9 <= nums[i] <= 10^9`
""",
    signature=dict(name="moveZeros", parameters=[("nums", "INT_ARRAY")], returns="INT_ARRAY"),
    groups=standard_groups(),
    reference=_move_zeros,
    cases={
        "sample": [
            ("01", [[0, 1, 0, 3, 12]]),
            ("02", [[1, 2, 3]]),
        ],
        "boundary": [
            ("01-single-zero", [[0]]),
            ("02-all-zeros", [[0, 0, 0, 0]]),
            ("03-no-zeros", [[5, -4, 3]]),
            # 0 이 앞에만 몰려 있으면 순서를 뒤집는 구현도 우연히 맞는다. 섞어 둔다.
            ("04-interleaved", [[0, 5, 0, -3, 0, 7, 0]]),
        ],
        "hidden": [
            ("01-zeros-at-tail", [[4, 5, 6, 0, 0]]),
            ("02-long-mixed", [[(0 if i % 3 == 0 else i) for i in range(1, 201)]]),
            ("03-order-matters", [[3, 0, 1, 0, 4, 0, 1, 5]]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 쓰기 위치를 따로 든 한 번 훑기.
//
// 읽는 위치와 쓰는 위치를 나누면 추가 배열이 필요 없다. 쓰기 위치는 읽기 위치를
// 앞지르지 않으므로 아직 읽지 않은 값을 덮어쓸 일도 없다.
fun moveZeros(nums: IntArray): IntArray {
    val out = IntArray(nums.size)
    var write = 0

    for (index in nums.indices) {
        Drill.visit(index, nums[index])
        if (nums[index] != 0) {
            out[write] = nums[index]
            Drill.write(write, nums[index])
            write += 1
            Drill.pointer("write", write)
        }
    }
    return out
}
""",
    mutants=[
        ("unstable--reverses-order", "WRONG_BRANCH",
         "뒤에서부터 채워 0 이 아닌 원소의 순서가 뒤집힌다.",
         """
fun moveZeros(nums: IntArray): IntArray {
    val out = IntArray(nums.size)
    var write = 0
    for (index in nums.indices.reversed()) {
        if (nums[index] != 0) {
            out[write] = nums[index]
            write += 1
        }
    }
    return out
}
"""),
        ("drops-length--returns-shorter", "MISSING_EDGE_CASE",
         "0 을 빼고 남은 것만 돌려줘 길이가 줄어든다.",
         """
fun moveZeros(nums: IntArray): IntArray = nums.filter { it != 0 }.toIntArray()
"""),
        ("off-by-one--skips-last", "OFF_BY_ONE",
         "마지막 원소를 보지 않는다.",
         """
fun moveZeros(nums: IntArray): IntArray {
    val out = IntArray(nums.size)
    var write = 0
    for (index in 0 until nums.size - 1) {
        if (nums[index] != 0) {
            out[write] = nums[index]
            write += 1
        }
    }
    return out
}
"""),
    ],
))


# --- 3. 크기 k 창의 최대 합 --------------------------------------------------

def _max_window(nums, k):
    total = sum(nums[:k])
    best = total
    for i in range(k, len(nums)):
        total += nums[i] - nums[i - k]
        best = max(best, total)
    return best


PROBLEMS.append(Problem(
    id="max-window-sum",
    # v2: 성능 케이스를 키웠다. 두 겹 풀이가 한도를 배로만 넘겨, 한가한 머신에서는
    # 통과하고 바쁜 머신에서만 잡혔다 (§12.1 재현성).
    version=2,
    title="크기 k 창의 최대 합",
    summary="""
정수 배열 `nums` 와 정수 `k` 가 주어진다. 연속한 `k` 개 원소의 합 중 **최댓값**을
반환한다.
""",
    notes="""
창을 한 칸 옮길 때 다시 더하지 않는다. 들어온 값을 더하고 나간 값을 빼면 한 번의
덧셈과 뺄셈으로 끝난다.
""",
    drill_doc="""
Drill.pointer("right", i)   // 창의 오른쪽 끝을 옮겼다
Drill.visit(i, value)       // 창에 들어온 값
Drill.write(0, sum)         // 현재 창의 합
""",
    constraints="""
- `1 <= k <= nums.size <= 200_000`
- `-10^4 <= nums[i] <= 10^4`
""",
    signature=dict(name="maxWindowSum", parameters=[("nums", "INT_ARRAY"), ("k", "INT")],
                   returns="INT"),
    groups=perf_groups(),
    reference=_max_window,
    cases={
        "sample": [
            ("01", [[1, 4, 2, 10, 2, 3, 1, 0, 20], 4]),
            ("02", [[2, 3], 1]),
        ],
        "boundary": [
            # 창이 배열 전체다. 한 번도 옮기지 않는다.
            ("01-k-equals-n", [[5, -2, 7], 3]),
            ("02-k-one", [[-4, -1, -9], 1]),
            # 전부 음수면 "0 으로 초기화" 하는 구현이 걸린다.
            ("03-all-negative", [[-3, -8, -2, -5], 2]),
            # 최댓값이 맨 앞에 있다. 첫 창을 후보로 넣지 않으면 놓친다.
            ("04-best-at-head", [[9, 9, 1, 1, 1], 2]),
            # 최댓값이 맨 뒤에 있다.
            ("05-best-at-tail", [[1, 1, 1, 9, 9], 2]),
        ],
        "hidden": [
            ("01-plateau", [[3] * 50, 7]),
            ("02-alternating", [[(10 if i % 2 == 0 else -10) for i in range(60)], 3]),
            ("03-mixed", [[7, -3, 12, -8, 4, 15, -20, 6, 9, -1, 3], 5]),
        ],
        # k 를 크게 잡는다. O(n*k) 풀이가 무너지는 지점은 n 이 아니라 n*k 이고,
        # k 가 작으면 200_000 짜리 입력에서도 다시 더하는 풀이가 통과한다.
        "performance": [
            ("01-small", [randoms(2000, -10000, 10000, salt=1), 100]),
            ("02-medium", [randoms(20000, -10000, 10000, salt=2), 10000]),
            ("03-large", [randoms(400000, -10000, 10000, salt=3), 200000]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 슬라이딩 윈도우.
//
// 창을 옮길 때 전체를 다시 더하지 않는다. 들어온 값을 더하고 나간 값을 빼면
// O(n) 이며, 다시 더하는 구현은 O(n*k) 라 성능 그룹에서 갈린다.
fun maxWindowSum(nums: IntArray, k: Int): Int {
    var total = 0
    for (i in 0 until k) {
        Drill.visit(i, nums[i])
        total += nums[i]
    }

    var best = total
    Drill.write(0, total)

    for (i in k until nums.size) {
        total += nums[i] - nums[i - k]
        Drill.pointer("right", i)
        Drill.write(0, total)
        if (total > best) best = total
    }
    return best
}
""",
    mutants=[
        ("zero-init--breaks-on-negative", "MISSING_EDGE_CASE",
         "최댓값을 0 으로 초기화해 전부 음수인 입력에서 0 을 돌려준다.",
         """
fun maxWindowSum(nums: IntArray, k: Int): Int {
    var total = 0
    for (i in 0 until k) total += nums[i]
    var best = 0
    for (i in k until nums.size) {
        total += nums[i] - nums[i - k]
        if (total > best) best = total
    }
    return best
}
"""),
        ("skips-first-window--ignores-head", "OFF_BY_ONE",
         "첫 창을 후보에 넣지 않아 최댓값이 맨 앞에 있으면 놓친다.",
         """
fun maxWindowSum(nums: IntArray, k: Int): Int {
    var total = 0
    for (i in 0 until k) total += nums[i]
    var best = Int.MIN_VALUE
    for (i in k until nums.size) {
        total += nums[i] - nums[i - k]
        if (total > best) best = total
    }
    return if (best == Int.MIN_VALUE) total else best
}
"""),
        ("quadratic--recomputes-window", "PERFORMANCE",
         "창마다 k 개를 다시 더해 O(n*k) 다. 작은 입력은 통과한다.",
         """
fun maxWindowSum(nums: IntArray, k: Int): Int {
    var best = Int.MIN_VALUE
    for (start in 0..nums.size - k) {
        var total = 0
        for (i in start until start + k) { Drill.visit(i, nums[i]); total += nums[i] }
        if (total > best) best = total
    }
    return best
}
"""),
    ],
))


# --- 4. 가장 많은 물을 담는 그릇 ---------------------------------------------

def _container(heights):
    left, right, best = 0, len(heights) - 1, 0
    while left < right:
        best = max(best, (right - left) * min(heights[left], heights[right]))
        if heights[left] < heights[right]:
            left += 1
        else:
            right -= 1
    return best


PROBLEMS.append(Problem(
    id="container-water",
    title="가장 많은 물을 담는 그릇",
    summary="""
높이 배열 `heights` 가 주어진다. 두 선분을 골라 만든 그릇에 담을 수 있는 물의 최대
넓이를 반환한다. 넓이는 `(오른쪽 인덱스 - 왼쪽 인덱스) x min(두 높이)` 다.
""",
    notes="""
양 끝에서 좁혀 온다. **낮은 쪽을 옮기는 것**이 핵심이다 — 낮은 쪽을 그대로 두면 폭만
줄어드니, 그 선분이 만들 수 있는 더 넓은 그릇은 이미 다 봤다.
""",
    drill_doc="""
Drill.pointer("left", left)    // 왼쪽 벽
Drill.pointer("right", right)  // 오른쪽 벽
Drill.write(0, area)           // 지금까지의 최대 넓이
""",
    constraints="""
- `2 <= heights.size <= 100_000`
- `0 <= heights[i] <= 10_000`
""",
    signature=dict(name="maxWater", parameters=[("heights", "INT_ARRAY")], returns="INT"),
    groups=standard_groups(),
    reference=_container,
    cases={
        "sample": [
            ("01", [[1, 8, 6, 2, 5, 4, 8, 3, 7]]),
            ("02", [[1, 1]]),
        ],
        "boundary": [
            ("01-two-walls", [[4, 9]]),
            # 0 이 섞이면 넓이가 0 인 후보가 생긴다.
            ("02-zeros", [[0, 5, 0, 5, 0]]),
            # 단조 증가. 항상 오른쪽 벽이 낮지 않아 왼쪽만 움직인다.
            ("03-increasing", [[1, 2, 3, 4, 5, 6]]),
            ("04-decreasing", [[6, 5, 4, 3, 2, 1]]),
            # 가장 높은 두 벽이 붙어 있어 답은 그 둘이 아니다.
            ("05-tall-pair-adjacent", [[10000, 10000, 1, 1, 1, 1, 1, 1, 1, 1]]),
        ],
        "hidden": [
            ("01-plateau", [[7] * 40]),
            ("02-valley", [[9, 1, 1, 1, 1, 1, 1, 9]]),
            ("03-random", [randoms(300, 0, 10000, salt=11)]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 투 포인터.
//
// 낮은 쪽 벽을 옮긴다. 낮은 벽은 자기가 낀 어떤 그릇에서도 높이를 결정하므로,
// 폭이 줄어드는 쪽으로는 더 나은 답이 나올 수 없다.
fun maxWater(heights: IntArray): Int {
    var left = 0
    var right = heights.size - 1
    var best = 0

    while (left < right) {
        Drill.pointer("left", left)
        Drill.pointer("right", right)

        val area = (right - left) * minOf(heights[left], heights[right])
        if (area > best) {
            best = area
            Drill.write(0, best)
        }
        if (heights[left] < heights[right]) left += 1 else right -= 1
    }
    return best
}
""",
    mutants=[
        ("moves-taller--wrong-pointer", "WRONG_BRANCH",
         "낮은 쪽이 아니라 높은 쪽을 옮겨, 더 나은 후보를 지나쳐 버린다.",
         """
fun maxWater(heights: IntArray): Int {
    var left = 0
    var right = heights.size - 1
    var best = 0
    while (left < right) {
        val area = (right - left) * minOf(heights[left], heights[right])
        if (area > best) best = area
        if (heights[left] > heights[right]) left += 1 else right -= 1
    }
    return best
}
"""),
        ("uses-max-height--wrong-formula", "WRONG_BRANCH",
         "두 높이 중 큰 값을 쓴다. 물은 낮은 벽까지만 찬다.",
         """
fun maxWater(heights: IntArray): Int {
    var left = 0
    var right = heights.size - 1
    var best = 0
    while (left < right) {
        val area = (right - left) * maxOf(heights[left], heights[right])
        if (area > best) best = area
        if (heights[left] < heights[right]) left += 1 else right -= 1
    }
    return best
}
"""),
        ("adjacent-only--ignores-width", "MISSING_EDGE_CASE",
         "붙어 있는 두 벽만 본다. 폭이 넓은 그릇을 놓친다.",
         """
fun maxWater(heights: IntArray): Int {
    var best = 0
    for (i in 0 until heights.size - 1) {
        val area = minOf(heights[i], heights[i + 1])
        if (area > best) best = area
    }
    return best
}
"""),
    ],
))


# --- 5. 배열 회전 ------------------------------------------------------------

def _rotate(nums, k):
    n = len(nums)
    shift = k % n
    return nums[n - shift:] + nums[:n - shift]


PROBLEMS.append(Problem(
    id="rotate-array",
    title="배열 오른쪽으로 회전",
    summary="""
정수 배열 `nums` 를 오른쪽으로 `k` 칸 회전한 결과를 반환한다. 끝을 넘어간 원소는 앞으로
돌아온다.
""",
    notes="""
`k` 가 배열 길이보다 클 수 있다. `k % n` 으로 줄이지 않으면 인덱스가 범위를 벗어난다.
""",
    drill_doc="""
Drill.visit(index, value)  // 원본에서 읽었다
Drill.write(target, value) // 회전한 자리에 썼다
""",
    constraints="""
- `1 <= nums.size <= 200_000`
- `0 <= k <= 10^9`
- `-10^9 <= nums[i] <= 10^9`
""",
    signature=dict(name="rotate", parameters=[("nums", "INT_ARRAY"), ("k", "INT")],
                   returns="INT_ARRAY"),
    groups=standard_groups(),
    reference=_rotate,
    cases={
        "sample": [
            ("01", [[1, 2, 3, 4, 5, 6, 7], 3]),
            ("02", [[-1, -100, 3, 99], 2]),
        ],
        "boundary": [
            ("01-k-zero", [[1, 2, 3], 0]),
            # k 가 길이와 같다. 제자리로 돌아온다.
            ("02-k-equals-n", [[1, 2, 3, 4], 4]),
            # k 가 길이보다 크다. 나머지 연산이 없으면 여기서 터진다.
            ("03-k-larger-than-n", [[1, 2, 3], 10]),
            ("04-single", [[42], 1000000000]),
            ("05-k-huge", [[1, 2, 3, 4, 5], 1000000000]),
        ],
        "hidden": [
            ("01-long", [list(range(1, 121)), 47]),
            ("02-k-one", [list(range(10, 20)), 1]),
            ("03-k-n-minus-one", [list(range(10, 20)), 9]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/).
//
// k 를 먼저 길이로 나눈 나머지로 줄인다. 줄이지 않으면 k 가 클 때 인덱스가 배열을
// 벗어나고, 그 실패는 "가끔 터지는 풀이"로 나타나 원인을 찾기 어렵다.
fun rotate(nums: IntArray, k: Int): IntArray {
    val n = nums.size
    val shift = k % n
    val out = IntArray(n)

    for (index in nums.indices) {
        Drill.visit(index, nums[index])
        val target = (index + shift) % n
        out[target] = nums[index]
        Drill.write(target, nums[index])
    }
    return out
}
""",
    mutants=[
        ("no-modulo--breaks-on-large-k", "MISSING_EDGE_CASE",
         "k 를 길이로 줄이지 않아 k 가 길이보다 크면 자리를 잘못 잡는다.",
         """
fun rotate(nums: IntArray, k: Int): IntArray {
    val n = nums.size
    val out = IntArray(n)
    for (index in nums.indices) out[(index + k) % n] = nums[index]
    return if (k < n) out else nums.copyOf()
}
"""),
        ("rotates-left--wrong-direction", "WRONG_BRANCH",
         "왼쪽으로 회전한다.",
         """
fun rotate(nums: IntArray, k: Int): IntArray {
    val n = nums.size
    val shift = k % n
    val out = IntArray(n)
    for (index in nums.indices) out[(index - shift + n) % n] = nums[index]
    return out
}
"""),
        ("off-by-one--shifts-one-extra", "OFF_BY_ONE",
         "한 칸 더 민다.",
         """
fun rotate(nums: IntArray, k: Int): IntArray {
    val n = nums.size
    val shift = (k + 1) % n
    val out = IntArray(n)
    for (index in nums.indices) out[(index + shift) % n] = nums[index]
    return out
}
"""),
    ],
))


# --- 6. 합이 k 인 부분배열 개수 ----------------------------------------------

def _subarray_count(nums, k):
    seen = {0: 1}
    total = 0
    count = 0
    for value in nums:
        total += value
        count += seen.get(total - k, 0)
        seen[total] = seen.get(total, 0) + 1
    return count


PROBLEMS.append(Problem(
    id="subarray-sum-count",
    title="합이 k 인 부분배열의 개수",
    summary="""
정수 배열 `nums` 와 정수 `k` 가 주어진다. 합이 정확히 `k` 인 **연속 부분배열**의 개수를
반환한다.
""",
    notes="""
음수가 섞여 있어 슬라이딩 윈도우로는 풀리지 않는다. 접두사 합을 세어 두고, 지금까지의
합에서 `k` 를 뺀 값이 몇 번 나왔는지 보면 한 번 훑기로 끝난다.

빈 접두사(합 0)를 미리 한 번 세어 둬야 배열 맨 앞에서 시작하는 부분배열이 잡힌다.
""",
    drill_doc="""
Drill.visit(index, value)  // 원소를 더했다
Drill.write(index, sum)    // 접두사 합
Drill.match(start, index)  // 조건을 만족하는 구간을 찾았다
""",
    constraints="""
- `1 <= nums.size <= 200_000`
- `-1_000 <= nums[i] <= 1_000`
- `-10^7 <= k <= 10^7`
- 정답은 `Int` 범위를 넘지 않는다
""",
    signature=dict(name="countSubarrays", parameters=[("nums", "INT_ARRAY"), ("k", "INT")],
                   returns="INT"),
    groups=perf_groups(),
    reference=_subarray_count,
    cases={
        "sample": [
            ("01", [[1, 1, 1], 2]),
            ("02", [[1, 2, 3], 3]),
        ],
        "boundary": [
            # 배열 맨 앞에서 시작하는 구간. 빈 접두사를 세지 않으면 놓친다.
            ("01-prefix-from-head", [[3, 4, 7], 3]),
            ("02-single-hit", [[5], 5]),
            ("03-single-miss", [[5], 4]),
            # 0 이 이어지면 같은 합이 여러 번 나온다. 개수를 세지 않고 존재만 보면 틀린다.
            ("04-zeros", [[0, 0, 0], 0]),
            ("05-negative", [[-1, 2, -1, 2, -1], 1]),
            ("06-no-answer", [[1, 2, 3], 100]),
        ],
        "hidden": [
            ("01-all-same", [[2] * 40, 8]),
            ("02-mixed-signs", [[3, -1, -1, 3, -2, 2, 1, -3, 4], 3]),
            ("03-whole-array", [list(range(1, 21)), 210]),
        ],
        # 해시맵 없이 두 겹으로 돌면 O(n^2) 다. 큰 입력에서만 갈린다.
        "performance": [
            ("01-small", [randoms(2000, -1000, 1000, salt=21), 0]),
            ("02-medium", [randoms(30000, -1000, 1000, salt=22), 0]),
            ("03-large", [randoms(200000, -1000, 1000, salt=23), 0]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 접두사 합 + 빈도 맵.
//
// 지금까지의 합이 total 일 때, 합이 k 인 구간은 "이전에 total - k 였던 지점"의 수만큼
// 있다. 그 수를 세어 두면 한 번 훑기로 끝난다.
//
// 빈 접두사를 1 로 시작하는 것이 중요하다. 배열 맨 앞에서 시작하는 구간은 이전 지점이
// 없으므로, 그 자리를 미리 만들어 두지 않으면 세어지지 않는다.
fun countSubarrays(nums: IntArray, k: Int): Int {
    val seen = HashMap<Int, Int>()
    seen[0] = 1

    var total = 0
    var count = 0

    for (index in nums.indices) {
        Drill.visit(index, nums[index])
        total += nums[index]
        Drill.write(index, total)

        val hits = seen[total - k] ?: 0
        if (hits > 0) {
            count += hits
            Drill.match(index, index)
        }
        seen[total] = (seen[total] ?: 0) + 1
    }
    return count
}
""",
    mutants=[
        ("no-empty-prefix--misses-head", "MISSING_EDGE_CASE",
         "빈 접두사를 세지 않아 배열 맨 앞에서 시작하는 구간을 놓친다.",
         """
fun countSubarrays(nums: IntArray, k: Int): Int {
    val seen = HashMap<Int, Int>()
    var total = 0
    var count = 0
    for (value in nums) {
        total += value
        count += seen[total - k] ?: 0
        seen[total] = (seen[total] ?: 0) + 1
    }
    return count
}
"""),
        ("counts-presence--not-frequency", "WRONG_BRANCH",
         "같은 합이 몇 번 나왔는지 세지 않고 있었는지만 본다.",
         """
fun countSubarrays(nums: IntArray, k: Int): Int {
    val seen = HashSet<Int>()
    seen.add(0)
    var total = 0
    var count = 0
    for (value in nums) {
        total += value
        if (seen.contains(total - k)) count += 1
        seen.add(total)
    }
    return count
}
"""),
        ("quadratic--sums-every-pair", "PERFORMANCE",
         "모든 구간의 합을 다시 더해 O(n^2) 다. 작은 입력은 통과한다.",
         """
fun countSubarrays(nums: IntArray, k: Int): Int {
    var count = 0
    for (start in nums.indices) {
        var total = 0
        for (end in start until nums.size) {
            Drill.visit(end, nums[end])
            total += nums[end]
            if (total == k) count += 1
        }
    }
    return count
}
"""),
    ],
))


# --- 30. 0 을 k 개까지 뒤집어 만든 최장 1 구간 -------------------------------

def _longest_ones(bits, k):
    left = 0
    zeros = 0
    best = 0
    for right, bit in enumerate(bits):
        if bit == 0:
            zeros += 1
        while zeros > k:
            if bits[left] == 0:
                zeros -= 1
            left += 1
        best = max(best, right - left + 1)
    return best


PROBLEMS.append(Problem(
    id="longest-ones-after-flip",
    title="0을 k개까지 뒤집어 만든 최장 1 구간",
    summary="""
`0` 과 `1` 로만 이루어진 배열 `bits` 와 정수 `k` 가 주어진다. `0` 을 **최대 `k` 개까지**
`1` 로 바꿀 수 있을 때, 만들 수 있는 가장 긴 연속 `1` 구간의 길이를 반환한다.
""",
    notes="""
어떤 0 을 뒤집을지 고르는 문제가 아니라, **0 을 k 개 이하로 품는 가장 긴 창**을 찾는
문제다. 창의 왼쪽 끝은 되돌아가지 않으므로 한 번 훑기로 끝난다.
""",
    drill_doc="""
Drill.pointer("left", left)   // 창의 왼쪽
Drill.pointer("right", right) // 창의 오른쪽
Drill.write(0, best)          // 지금까지의 최장 길이
""",
    constraints="""
- `1 <= bits.size <= 200_000`
- `bits[i]` 는 `0` 또는 `1`
- `0 <= k <= bits.size`
""",
    signature=dict(name="longestOnes", parameters=[("bits", "INT_ARRAY"), ("k", "INT")],
                   returns="INT"),
    groups=perf_groups(),
    reference=_longest_ones,
    cases={
        "sample": [
            ("01", [[1, 1, 1, 0, 0, 0, 1, 1, 1, 1, 0], 2]),
            ("02", [[0, 0, 1, 1, 0, 0, 1, 1, 1, 0, 1, 1, 0, 0, 0, 1, 1, 1, 1], 3]),
        ],
        "boundary": [
            # 뒤집을 수 없다. 원래 있는 1 구간이 답이다.
            ("01-k-zero", [[1, 1, 0, 1, 1, 1], 0]),
            # 전부 뒤집을 수 있다. 배열 전체가 답이다.
            ("02-k-covers-all", [[0, 0, 0], 3]),
            ("03-all-ones", [[1, 1, 1], 1]),
            ("04-all-zeros-limited", [[0, 0, 0, 0], 2]),
            ("05-single-zero", [[0], 0]),
            ("06-single-one", [[1], 0]),
            # 답이 맨 끝에서 끝난다. 창을 끝까지 재지 않으면 놓친다.
            ("07-best-at-tail", [[0, 0, 1, 1, 1, 1], 1]),
        ],
        "hidden": [
            ("01-alternating", [[i % 2 for i in range(40)], 5]),
            ("02-sparse-zeros", [[0 if i % 17 == 0 else 1 for i in range(200)], 3]),
            ("03-blocks", [[1] * 10 + [0] * 5 + [1] * 12 + [0] * 2 + [1] * 8, 4]),
        ],
        # 창의 왼쪽을 매번 처음부터 다시 미는 풀이는 O(n*k) 가 된다.
        "performance": [
            ("01-small", [[v % 2 for v in randoms(3000, 0, 9, salt=111)], 50]),
            ("02-medium", [[1 if v > 2 else 0 for v in randoms(50000, 0, 9, salt=112)], 500]),
            ("03-large", [[1 if v > 0 else 0 for v in randoms(200000, 0, 9, salt=113)], 5000]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 슬라이딩 윈도우.
//
// 창이 품은 0 의 개수가 k 를 넘으면 왼쪽을 민다. 왼쪽 끝이 되돌아가지 않으므로 두
// 포인터를 합쳐도 배열을 한 번 훑는 것과 같다.
//
// 답을 매번 갱신하는 것이 아니라 "지금 창의 길이"로 재는 것이 요령이다. 창은 절대
// 줄어들지 않으므로 마지막 길이가 곧 최댓값이 되는 구현도 가능하지만, 여기서는
// 읽기 쉬운 쪽을 골랐다.
fun longestOnes(bits: IntArray, k: Int): Int {
    var left = 0
    var zeros = 0
    var best = 0

    for (right in bits.indices) {
        if (bits[right] == 0) zeros += 1
        Drill.pointer("right", right)

        while (zeros > k) {
            if (bits[left] == 0) zeros -= 1
            left += 1
            Drill.pointer("left", left)
        }

        val length = right - left + 1
        if (length > best) {
            best = length
            Drill.write(0, best)
        }
    }
    return best
}
""",
    mutants=[
        ("counts-ones-only--ignores-flips", "MISSING_EDGE_CASE",
         "이미 1 인 구간만 재고 뒤집기를 쓰지 않는다.",
         """
fun longestOnes(bits: IntArray, k: Int): Int {
    var best = 0
    var run = 0
    for (bit in bits) {
        run = if (bit == 1) run + 1 else 0
        if (run > best) best = run
    }
    return best
}
"""),
        ("off-by-one--window-too-short", "OFF_BY_ONE",
         "창의 길이를 한 칸 짧게 센다.",
         """
fun longestOnes(bits: IntArray, k: Int): Int {
    var left = 0
    var zeros = 0
    var best = 0
    for (right in bits.indices) {
        if (bits[right] == 0) zeros += 1
        while (zeros > k) {
            if (bits[left] == 0) zeros -= 1
            left += 1
        }
        val length = right - left
        if (length > best) best = length
    }
    return best
}
"""),
        ("restart-left--quadratic", "PERFORMANCE",
         "창이 넘칠 때마다 왼쪽을 처음부터 다시 민다. 작은 입력은 통과한다.",
         """
fun longestOnes(bits: IntArray, k: Int): Int {
    var best = 0
    for (start in bits.indices) {
        var zeros = 0
        for (end in start until bits.size) {
            Drill.visit(end, bits[end])
            if (bits[end] == 0) zeros += 1
            if (zeros > k) break
            val length = end - start + 1
            if (length > best) best = length
        }
    }
    return best
}
"""),
    ],
))


# --- 58. 가장 자주 나온 값 (값의 범위가 작다) ----------------------------------------------

def _most_frequent(nums):
    counts = [0] * 101
    for v in nums:
        counts[v] += 1
    best = 0
    for v in range(1, 101):
        if counts[v] > counts[best]:
            best = v
    return best


PROBLEMS.append(Problem(
    id="most-frequent-small-values",
    title="가장 자주 나온 값",
    summary="""
정수 배열 `nums` 가 주어진다. **가장 자주 나온 값**을 반환한다. 가장 많이 나온 값이 여럿이면
그중 **가장 작은 값**이다.
""",
    notes="""
값의 범위를 보라. `0` 이상 `100` 이하라면 값마다 칸을 하나씩 둔 배열이 곧 빈도표이고,
해시도 정렬도 필요 없다. 그리고 그 배열을 작은 값부터 훑으면 동률 규칙이 저절로 지켜진다.
""",
    drill_doc="""
Drill.write(value, count)     // 값의 개수를 올렸다
Drill.compare(value, best)    // 지금까지의 최빈값과 견줬다
""",
    constraints="""
- `1 <= nums.size <= 200_000`
- `0 <= nums[i] <= 100`
""",
    signature=dict(name="mostFrequent", parameters=[("nums", "INT_ARRAY")], returns="INT"),
    groups=perf_groups(),
    reference=_most_frequent,
    cases={
        "sample": [
            ("01", [[3, 1, 3, 2, 1, 3]]),
            ("02", [[5, 7, 5, 7]]),
        ],
        "boundary": [
            ("01-single", [[42]]),
            # 전부 한 번씩. 가장 작은 값.
            ("02-all-once", [[9, 4, 7, 1]]),
            # 0 도 값이다.
            ("03-zero-wins", [[0, 0, 1]]),
            ("04-max-value", [[100, 100, 99]]),
            # 동률인데 해시 순서가 오름차순이 아닌 값들.
            ("05-tie-hash-order", [[100, 3, 17, 100, 3, 17]]),
            # 큰 값이 먼저 나오지만 작은 값과 동률.
            ("06-tie-big-first", [[50, 50, 2, 2]]),
        ],
        "hidden": [
            ("01-random-small", [randoms(30, 0, 100, salt=1801)]),
            ("02-random-medium", [randoms(3000, 0, 100, salt=1802)]),
            ("03-two-values", [[7, 8] * 100 + [8]]),
            ("04-all-same", [[13] * 500]),
        ],
        "performance": [
            ("01-small", [randoms(5000, 0, 100, salt=1803)]),
            ("02-medium", [randoms(50000, 0, 100, salt=1804)]),
            ("03-large", [randoms(200000, 0, 100, salt=1805)]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 값의 범위가 작으니 배열이 빈도표다.
fun mostFrequent(nums: IntArray): Int {
    val counts = IntArray(101)
    for (v in nums) {
        counts[v] += 1
        Drill.write(v, counts[v])
    }
    var best = 0
    for (v in 1..100) {
        Drill.compare(v, best)
        if (counts[v] > counts[best]) best = v
    }
    return best
}
""",
    mutants=[
        ("tie-takes-later", "WRONG_BRANCH",
         "동률이면 나중에 본 값으로 바꾼다. 가장 작은 값이어야 한다.",
         """
fun mostFrequent(nums: IntArray): Int {
    val counts = IntArray(101)
    for (v in nums) counts[v] += 1
    var best = 0
    for (v in 1..100) if (counts[v] >= counts[best]) best = v
    return best
}
"""),
        ("hash-order--tie-by-iteration", "MISSING_EDGE_CASE",
         "해시맵을 만난 순서로 훑어 동률을 정한다. 순서가 값 순이라는 보장이 없다.",
         """
fun mostFrequent(nums: IntArray): Int {
    val counts = HashMap<Int, Int>()
    for (v in nums) counts[v] = (counts[v] ?: 0) + 1
    var best = -1
    var bestCount = -1
    for ((v, c) in counts) if (c > bestCount) { best = v; bestCount = c }
    return best
}
"""),
        ("skips-zero--starts-at-one", "OFF_BY_ONE",
         "값 0 을 세지 않는다. 범위는 0 부터다.",
         """
fun mostFrequent(nums: IntArray): Int {
    val counts = IntArray(101)
    for (v in nums) if (v >= 1) counts[v] += 1
    var best = 1
    for (v in 2..100) if (counts[v] > counts[best]) best = v
    return best
}
"""),
        ("count-per-element--quadratic", "PERFORMANCE",
         "원소마다 배열 전체를 훑어 자기 개수를 센다. O(n²).",
         """
fun mostFrequent(nums: IntArray): Int {
    var best = -1
    var bestCount = 0
    for (i in nums.indices) {
        var count = 0
        for (j in nums.indices) { Drill.compare(i, j); if (nums[j] == nums[i]) count += 1 }
        if (count > bestCount || (count == bestCount && nums[i] < best)) { best = nums[i]; bestCount = count }
    }
    return best
}
"""),
    ],
))


# --- 60. 합이 k 로 나누어떨어지는 부분배열 수 ----------------------------------------------

def _divisible_subarrays(nums, k):
    counts = {0: 1}
    prefix = 0
    total = 0
    for v in nums:
        prefix = (prefix + v) % k
        total += counts.get(prefix, 0)
        counts[prefix] = counts.get(prefix, 0) + 1
    return total


PROBLEMS.append(Problem(
    id="subarrays-divisible-by-k",
    title="합이 k 의 배수인 부분배열의 수",
    summary="""
정수 배열 `nums` 와 양의 정수 `k` 가 주어진다. 원소의 합이 `k` 로 **나누어떨어지는**
연속 부분배열(길이 1 이상)의 개수를 반환한다. 음수도 있다.
""",
    notes="""
구간 합은 누적 합 두 개의 차다. 두 누적 합의 차가 k 의 배수라는 것은 **k 로 나눈 나머지가
같다**는 것이고, 그러면 나머지마다 몇 번 나왔는지만 세면 된다. 음수의 나머지는 언어마다
다르게 나온다 — 0 이상으로 맞춰 둔다.
""",
    drill_doc="""
Drill.visit(i, remainder)     // i 까지의 누적 합의 나머지
Drill.write(remainder, count) // 그 나머지가 몇 번 나왔나
""",
    constraints="""
- `1 <= nums.size <= 200_000`
- `-10^4 <= nums[i] <= 10^4`
- `1 <= k <= 10_000`
- 답은 `Int` 범위 안
""",
    signature=dict(name="divisibleSubarrays", parameters=[("nums", "INT_ARRAY"), ("k", "INT")],
                   returns="INT"),
    groups=perf_groups(),
    reference=_divisible_subarrays,
    cases={
        "sample": [
            ("01", [[4, 5, 0, -2, -3, 1], 5]),
            ("02", [[5], 9]),
        ],
        "boundary": [
            ("01-single-divisible", [[6], 3]),
            # k = 1 이면 모든 부분배열이다.
            ("02-k-one", [[1, 2, 3], 1]),
            # 음수의 나머지. 언어의 % 를 그대로 쓰면 -2 와 3 이 다른 칸에 간다.
            ("03-negative-remainder", [[-2, 3], 5]),
            ("04-all-zero", [[0, 0, 0], 7]),
            # 누적 합 자체가 k 의 배수인 접두사. 빈 접두사(나머지 0)를 하나로 세어야 한다.
            ("05-prefix-itself", [[2, 3, 5], 5]),
            ("06-none", [[1, 1, 1], 5]),
        ],
        "hidden": [
            ("01-random-small", [randoms(30, -50, 50, salt=2001), 7]),
            ("02-random-medium", [randoms(2000, -10000, 10000, salt=2002), 97]),
            ("03-big-k", [randoms(500, -10000, 10000, salt=2003), 10000]),
            ("04-negatives-only", [[-v for v in randoms(300, 1, 1000, salt=2004)], 13]),
        ],
        "performance": [
            ("01-small", [randoms(5000, -10000, 10000, salt=2005), 101]),
            ("02-medium", [randoms(50000, -10000, 10000, salt=2006), 1009]),
            ("03-large", [randoms(200000, -10000, 10000, salt=2007), 9973]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 나머지별 누적 합 개수.
fun divisibleSubarrays(nums: IntArray, k: Int): Int {
    val counts = IntArray(k)
    counts[0] = 1
    var prefix = 0
    var total = 0
    for ((i, v) in nums.withIndex()) {
        prefix = ((prefix + v) % k + k) % k
        Drill.visit(i, prefix)
        total += counts[prefix]
        counts[prefix] += 1
        Drill.write(prefix, counts[prefix])
    }
    return total
}
""",
    mutants=[
        ("negative-remainder--unnormalized", "MISSING_EDGE_CASE",
         "음수 누적 합의 나머지를 0 이상으로 맞추지 않는다. -2 와 3 을 다른 나머지로 센다.",
         """
fun divisibleSubarrays(nums: IntArray, k: Int): Int {
    val counts = HashMap<Int, Int>()
    counts[0] = 1
    var prefix = 0
    var total = 0
    for (v in nums) {
        prefix = (prefix + v) % k
        total += counts[prefix] ?: 0
        counts[prefix] = (counts[prefix] ?: 0) + 1
    }
    return total
}
"""),
        ("empty-prefix-missing", "OFF_BY_ONE",
         "빈 접두사(나머지 0 하나)를 세어 두지 않는다. 처음부터 시작하는 부분배열을 놓친다.",
         """
fun divisibleSubarrays(nums: IntArray, k: Int): Int {
    val counts = IntArray(k)
    var prefix = 0
    var total = 0
    for (v in nums) {
        prefix = ((prefix + v) % k + k) % k
        total += counts[prefix]
        counts[prefix] += 1
    }
    return total
}
"""),
        ("counts-after-increment", "WRONG_BRANCH",
         "개수를 올린 뒤에 더한다. 자기 자신과의 쌍을 센다.",
         """
fun divisibleSubarrays(nums: IntArray, k: Int): Int {
    val counts = IntArray(k)
    counts[0] = 1
    var prefix = 0
    var total = 0
    for (v in nums) {
        prefix = ((prefix + v) % k + k) % k
        counts[prefix] += 1
        total += counts[prefix]
    }
    return total
}
"""),
        ("all-pairs--quadratic", "PERFORMANCE",
         "시작점마다 끝점을 늘려 가며 합을 센다. O(n²).",
         """
fun divisibleSubarrays(nums: IntArray, k: Int): Int {
    var total = 0
    for (i in nums.indices) {
        var sum = 0L
        for (j in i until nums.size) {
            Drill.compare(i, j)
            sum += nums[j]
            if (sum % k == 0L) total += 1
        }
    }
    return total
}
"""),
    ],
))


# --- 61. 세 가지 색 정렬 (값의 범위가 셋뿐이다) ----------------------------------------------

def _sort_colors(nums):
    return sorted(nums)


PROBLEMS.append(Problem(
    id="sort-colors",
    title="세 가지 색 정렬",
    summary="""
`0`, `1`, `2` 만 들어 있는 배열 `nums` 가 주어진다. 오름차순으로 정렬한 배열을 반환한다.

값이 **세 가지뿐**이다. 일반 정렬로도 답은 나오지만, 이 문제가 묻는 것은 그 사실을 읽고
한 번 훑는 것으로 끝내는 것이다.
""",
    notes="""
값이 셋뿐이면 개수를 세어 다시 쓰면 되고(두 번 훑기), 한 번에 하려면 왼쪽 끝에 0 을, 오른쪽
끝에 2 를 보내면서 가운데를 지나간다. 오른쪽으로 보낸 뒤에는 **그 자리에 온 값을 아직 보지
않았으므로** 포인터를 옮기면 안 된다.
""",
    drill_doc="""
Drill.swap(i, j)              // 두 자리를 바꿨다
Drill.pointer("low", low)     // 0 의 경계
Drill.pointer("high", high)   // 2 의 경계
""",
    constraints="""
- `0 <= nums.size <= 200_000`
- `nums[i]` 는 `0`, `1`, `2` 중 하나
""",
    signature=dict(name="sortColors", parameters=[("nums", "INT_ARRAY")], returns="INT_ARRAY"),
    groups=standard_groups(),
    reference=_sort_colors,
    limits={"timeMillis": 2000, "memoryMb": 256, "outputBytes": 2000000},
    cases={
        "sample": [
            ("01", [[2, 0, 2, 1, 1, 0]]),
            ("02", [[2, 0, 1]]),
        ],
        "boundary": [
            ("01-empty", [[]]),
            ("02-single", [[1]]),
            ("03-already-sorted", [[0, 0, 1, 1, 2, 2]]),
            ("04-reversed", [[2, 2, 1, 1, 0, 0]]),
            # 2 를 오른쪽으로 보낸 자리에 0 이 온다. 포인터를 옮기면 그 0 을 놓친다.
            ("05-two-then-zero", [[2, 0]]),
            ("06-all-twos", [[2, 2, 2]]),
            ("07-no-ones", [[2, 0, 2, 0]]),
            ("08-all-same-ones", [[1, 1, 1, 1]]),
        ],
        "hidden": [
            ("01-random-small", [randoms(30, 0, 2, salt=2101)]),
            ("02-random-medium", [randoms(3000, 0, 2, salt=2102)]),
            ("03-large", [randoms(200000, 0, 2, salt=2103)]),
            ("04-zeros-late", [[2] * 100 + [1] * 100 + [0] * 100]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 네덜란드 국기 — 한 번 훑는다.
fun sortColors(nums: IntArray): IntArray {
    val out = nums.copyOf()
    var low = 0
    var mid = 0
    var high = out.size - 1
    while (mid <= high) {
        when (out[mid]) {
            0 -> { val t = out[low]; out[low] = out[mid]; out[mid] = t; Drill.swap(low, mid); low += 1; mid += 1 }
            2 -> { val t = out[high]; out[high] = out[mid]; out[mid] = t; Drill.swap(mid, high); high -= 1 }
            else -> mid += 1
        }
        Drill.pointer("low", low)
        Drill.pointer("high", high)
    }
    return out
}
""",
    mutants=[
        ("advances-after-high-swap", "WRONG_BRANCH",
         "2 를 오른쪽으로 보낸 뒤 가운데 포인터도 옮긴다. 오른쪽에서 온 값을 보지 않고 지나친다.",
         """
fun sortColors(nums: IntArray): IntArray {
    val out = nums.copyOf()
    var low = 0; var mid = 0; var high = out.size - 1
    while (mid <= high) {
        when (out[mid]) {
            0 -> { val t = out[low]; out[low] = out[mid]; out[mid] = t; low += 1; mid += 1 }
            2 -> { val t = out[high]; out[high] = out[mid]; out[mid] = t; high -= 1; mid += 1 }
            else -> mid += 1
        }
    }
    return out
}
"""),
        ("stops-before-high", "OFF_BY_ONE",
         "가운데 포인터가 오른쪽 경계와 같아지면 멈춘다. 마지막 칸을 보지 않는다.",
         """
fun sortColors(nums: IntArray): IntArray {
    val out = nums.copyOf()
    var low = 0; var mid = 0; var high = out.size - 1
    while (mid < high) {
        when (out[mid]) {
            0 -> { val t = out[low]; out[low] = out[mid]; out[mid] = t; low += 1; mid += 1 }
            2 -> { val t = out[high]; out[high] = out[mid]; out[mid] = t; high -= 1 }
            else -> mid += 1
        }
    }
    return out
}
"""),
        ("counts-two-values", "MISSING_EDGE_CASE",
         "0 과 1 만 세고 나머지를 2 로 채운다고 믿는다 — 세는 순서가 어긋나 1 의 자리가 밀린다.",
         """
fun sortColors(nums: IntArray): IntArray {
    var zeros = 0; var ones = 0
    for (v in nums) if (v == 0) zeros += 1 else if (v == 1) ones += 1
    val out = IntArray(nums.size) { 2 }
    for (i in 0 until zeros) out[i] = 0
    for (i in zeros until minOf(nums.size, zeros + ones + 1)) out[i] = 1
    return out
}
"""),
    ],
))


# --- 62. 과반수 원소 (제약이 곧 힌트다) --------------------------------------------------

def _majority(nums):
    candidate = None
    count = 0
    for v in nums:
        if count == 0:
            candidate = v
            count = 1
        elif v == candidate:
            count += 1
        else:
            count -= 1
    return candidate


PROBLEMS.append(Problem(
    id="majority-element",
    title="과반수 원소",
    summary="""
정수 배열 `nums` 가 주어진다. 배열 길이의 **절반보다 많이** 나오는 원소를 반환한다.
그런 원소는 **항상 정확히 하나 존재한다** — 이 약속이 문제의 절반이다.
""",
    notes="""
과반수가 있다고 약속했으므로 다른 값 하나와 짝지어 지워 나가도 그 값은 살아남는다.
후보 하나와 개수 하나면 된다. 약속이 없다면 두 번째로 훑어 확인해야 하지만, 여기서는
그 확인이 필요 없다.
""",
    drill_doc="""
Drill.visit(i, count)         // i 번째를 보고 난 뒤의 개수
Drill.write(0, candidate)     // 후보가 바뀌었다
""",
    constraints="""
- `1 <= nums.size <= 200_000`, 홀수
- `-10^9 <= nums[i] <= 10^9`
- 과반수 원소가 정확히 하나 존재한다
""",
    signature=dict(name="majority", parameters=[("nums", "INT_ARRAY")], returns="INT"),
    groups=standard_groups(),
    reference=_majority,
    cases={
        "sample": [
            ("01", [[3, 2, 3]]),
            ("02", [[2, 2, 1, 1, 1, 2, 2]]),
        ],
        "boundary": [
            ("01-single", [[7]]),
            # 과반수가 앞에 몰려 있다.
            ("02-front", [[5, 5, 5, 1, 2]]),
            # 과반수가 뒤에 몰려 있다. 앞에서 후보가 여러 번 바뀐다.
            ("03-back", [[1, 2, 5, 5, 5]]),
            ("04-alternating", [[4, 9, 4, 9, 4]]),
            ("05-negative", [[-1, -1, 3]]),
            ("06-large-values", [[1000000000, -1000000000, 1000000000]]),
        ],
        "hidden": [
            ("01-random-small", [[7] * 16 + randoms(15, 0, 3, salt=2201)]),
            ("02-shuffled-medium", [shuffled([42] * 1001 + randoms(1000, 0, 100, salt=2202), salt=2203)]),
            ("03-shuffled-large", [shuffled([-5] * 100001 + randoms(99999, -1000, 1000, salt=2204), salt=2205)]),
            # 과반수가 딱 절반 + 1.
            ("04-bare-majority", [shuffled([1] * 501 + [2] * 500, salt=2206)]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). Boyer–Moore 투표.
fun majority(nums: IntArray): Int {
    var candidate = nums[0]
    var count = 0
    for ((i, v) in nums.withIndex()) {
        if (count == 0) { candidate = v; Drill.write(0, candidate) }
        count += if (v == candidate) 1 else -1
        Drill.visit(i, count)
    }
    return candidate
}
""",
    mutants=[
        ("first-half-only", "WRONG_ALGORITHM",
         "앞 절반만 보고 가장 많이 나온 값을 답한다. 과반수가 뒤에 몰려 있으면 틀린다.",
         """
fun majority(nums: IntArray): Int {
    val counts = HashMap<Int, Int>()
    for (i in 0..nums.size / 2) counts[nums[i]] = (counts[nums[i]] ?: 0) + 1
    return counts.maxByOrNull { it.value }!!.key
}
"""),
        ("never-resets", "WRONG_BRANCH",
         "개수가 0 이 돼도 후보를 바꾸지 않는다. 첫 원소가 곧 답이 된다.",
         """
fun majority(nums: IntArray): Int {
    val candidate = nums[0]
    var count = 0
    for (v in nums) count += if (v == candidate) 1 else -1
    return candidate
}
"""),
        ("longest-run--not-count", "WRONG_ALGORITHM",
         "가장 길게 연속으로 이어진 값을 답한다. 과반수는 흩어져 있어도 과반수다.",
         """
fun majority(nums: IntArray): Int {
    var best = nums[0]; var bestRun = 0
    var run = 0
    for (i in nums.indices) {
        run = if (i > 0 && nums[i] == nums[i - 1]) run + 1 else 1
        if (run > bestRun) { bestRun = run; best = nums[i] }
    }
    return best
}
"""),
    ],
))


# --- 63. 빗물 가두기 ---------------------------------------------------------------

def _trapped_water(heights):
    left, right = 0, len(heights) - 1
    left_max = right_max = 0
    total = 0
    while left < right:
        if heights[left] < heights[right]:
            left_max = max(left_max, heights[left])
            total += left_max - heights[left]
            left += 1
        else:
            right_max = max(right_max, heights[right])
            total += right_max - heights[right]
            right -= 1
    return total


PROBLEMS.append(Problem(
    id="trapping-rain-water",
    title="빗물 가두기",
    summary="""
막대의 높이가 배열 `heights` 로 주어진다. 막대의 너비는 모두 1 이다. 비가 온 뒤 막대
사이에 **고이는 물의 총량**을 반환한다.

예: `[0, 1, 0, 2, 1, 0, 1, 3, 2, 1, 2, 1]` 이면 `6` 이다.
""",
    notes="""
한 칸에 고이는 물은 "왼쪽에서 가장 높은 것과 오른쪽에서 가장 높은 것 중 낮은 쪽" 에서 그
칸의 높이를 뺀 것이다. 양끝에서 포인터를 옮기되 **낮은 쪽을 옮긴다** — 낮은 쪽의 물은
반대편이 더 높다는 것만으로 이미 정해진다.
""",
    drill_doc="""
Drill.pointer("left", left)   // 왼쪽 포인터
Drill.pointer("right", right) // 오른쪽 포인터
Drill.write(i, water)         // 그 칸에 고인 물
""",
    constraints="""
- `0 <= heights.size <= 200_000`
- `0 <= heights[i] <= 10^4`, 답은 `Int` 범위 안
""",
    signature=dict(name="trappedWater", parameters=[("heights", "INT_ARRAY")], returns="INT"),
    groups=perf_groups(),
    reference=_trapped_water,
    cases={
        "sample": [
            ("01", [[0, 1, 0, 2, 1, 0, 1, 3, 2, 1, 2, 1]]),
            ("02", [[4, 2, 0, 3, 2, 5]]),
        ],
        "boundary": [
            ("01-empty", [[]]),
            ("02-single", [[5]]),
            ("03-two", [[3, 1]]),
            # 단조 증가·감소. 아무것도 안 고인다.
            ("04-increasing", [[1, 2, 3, 4]]),
            ("05-decreasing", [[4, 3, 2, 1]]),
            # 평평한 바닥. 0 이다.
            ("06-flat", [[2, 2, 2]]),
            # 한가운데 깊은 우물.
            ("07-well", [[5, 0, 0, 0, 5]]),
            # 오른쪽 벽이 더 낮다. 왼쪽 최대만 보면 넘친다.
            ("08-right-lower", [[5, 0, 3]]),
        ],
        "hidden": [
            ("01-random-small", [randoms(30, 0, 10, salt=2301)]),
            ("02-random-medium", [randoms(3000, 0, 10000, salt=2302)]),
            ("03-valleys", [[9, 1, 9, 1, 9, 1, 9]]),
            ("04-plateau-inside", [[6, 2, 2, 2, 4, 4, 6]]),
        ],
        "performance": [
            ("01-small", [randoms(5000, 0, 10000, salt=2303)]),
            ("02-medium", [randoms(50000, 0, 10000, salt=2304)]),
            ("03-large", [randoms(200000, 0, 10000, salt=2305)]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 양끝 포인터 — 낮은 쪽을 옮긴다.
fun trappedWater(heights: IntArray): Int {
    var left = 0
    var right = heights.size - 1
    var leftMax = 0
    var rightMax = 0
    var total = 0
    while (left < right) {
        if (heights[left] < heights[right]) {
            leftMax = maxOf(leftMax, heights[left])
            total += leftMax - heights[left]
            Drill.write(left, leftMax - heights[left])
            left += 1
            Drill.pointer("left", left)
        } else {
            rightMax = maxOf(rightMax, heights[right])
            total += rightMax - heights[right]
            Drill.write(right, rightMax - heights[right])
            right -= 1
            Drill.pointer("right", right)
        }
    }
    return total
}
""",
    mutants=[
        ("left-max-only", "WRONG_ALGORITHM",
         "왼쪽 최대 높이만 본다. 오른쪽 벽이 더 낮으면 넘치는 물까지 센다.",
         """
fun trappedWater(heights: IntArray): Int {
    var leftMax = 0
    var total = 0
    for (h in heights) {
        leftMax = maxOf(leftMax, h)
        total += leftMax - h
    }
    return total
}
"""),
        ("moves-higher-side", "WRONG_BRANCH",
         "높은 쪽 포인터를 옮긴다. 물의 높이가 아직 정해지지 않은 칸을 계산한다.",
         """
fun trappedWater(heights: IntArray): Int {
    var left = 0; var right = heights.size - 1
    var leftMax = 0; var rightMax = 0
    var total = 0
    while (left < right) {
        if (heights[left] >= heights[right]) {
            leftMax = maxOf(leftMax, heights[left]); total += leftMax - heights[left]; left += 1
        } else {
            rightMax = maxOf(rightMax, heights[right]); total += rightMax - heights[right]; right -= 1
        }
    }
    return total
}
"""),
        ("skips-last-column", "OFF_BY_ONE",
         "포인터가 만나는 마지막 칸을 계산하지 않는다 — 그 칸은 물이 0 이라 답은 같다. 대신 첫 칸을 건너뛴다.",
         """
fun trappedWater(heights: IntArray): Int {
    if (heights.size < 3) return 0
    var left = 1; var right = heights.size - 1
    var leftMax = heights[1]; var rightMax = 0
    var total = 0
    while (left < right) {
        if (heights[left] < heights[right]) {
            leftMax = maxOf(leftMax, heights[left]); total += leftMax - heights[left]; left += 1
        } else {
            rightMax = maxOf(rightMax, heights[right]); total += rightMax - heights[right]; right -= 1
        }
    }
    return total
}
"""),
        ("scan-both-sides--quadratic", "PERFORMANCE",
         "칸마다 왼쪽과 오른쪽 전체를 훑어 최대를 찾는다. O(n²).",
         """
fun trappedWater(heights: IntArray): Int {
    var total = 0
    for (i in heights.indices) {
        var leftMax = 0; var rightMax = 0
        for (j in 0..i) { Drill.compare(i, j); leftMax = maxOf(leftMax, heights[j]) }
        for (j in i until heights.size) rightMax = maxOf(rightMax, heights[j])
        total += minOf(leftMax, rightMax) - heights[i]
    }
    return total
}
"""),
    ],
))


# --- 64. 정렬된 두 배열 합치기 --------------------------------------------------------

def _merge_sorted(first, second):
    out = []
    i = j = 0
    while i < len(first) and j < len(second):
        if first[i] <= second[j]:
            out.append(first[i])
            i += 1
        else:
            out.append(second[j])
            j += 1
    out.extend(first[i:])
    out.extend(second[j:])
    return out


PROBLEMS.append(Problem(
    id="merge-sorted-arrays",
    title="정렬된 두 배열 합치기",
    summary="""
**오름차순으로 정렬된** 두 정수 배열 `first` 와 `second` 가 주어진다. 둘의 모든 원소를
담은 오름차순 배열을 반환한다. 같은 값은 그대로 여러 번 들어간다.
""",
    notes="""
둘 다 정렬돼 있으니 앞에서부터 작은 쪽을 하나씩 뽑으면 된다. 한쪽이 먼저 끝나면 남은 쪽을
그대로 붙인다 — 그 마무리를 빠뜨리는 것이 이 문제에서 가장 흔한 실수다.
""",
    drill_doc="""
Drill.compare(i, j)           // 두 배열의 앞 원소를 견줬다
Drill.write(k, value)         // 결과의 k 번째 칸을 채웠다
""",
    constraints="""
- `0 <= first.size, second.size <= 100_000`
- `-10^9 <= 원소 <= 10^9`, 각각 오름차순
""",
    signature=dict(name="mergeSorted", parameters=[("first", "INT_ARRAY"), ("second", "INT_ARRAY")],
                   returns="INT_ARRAY"),
    groups=standard_groups(),
    reference=_merge_sorted,
    limits={"timeMillis": 2000, "memoryMb": 256, "outputBytes": 4000000},
    cases={
        "sample": [
            ("01", [[1, 3, 5], [2, 4, 6]]),
            ("02", [[1, 2], [3]]),
        ],
        "boundary": [
            ("01-both-empty", [[], []]),
            ("02-first-empty", [[], [1, 2]]),
            ("03-second-empty", [[7, 9], []]),
            # 한쪽이 먼저 끝난다. 남은 쪽을 붙이지 않으면 짧아진다.
            ("04-first-ends-first", [[1, 2], [3, 4, 5, 6]]),
            ("05-second-ends-first", [[5, 6, 7, 8], [1]]),
            ("06-duplicates", [[1, 1, 2], [1, 2, 2]]),
            ("07-negative", [[-5, -1], [-3, 0]]),
        ],
        "hidden": [
            ("01-random-small", [sorted(randoms(20, -100, 100, salt=2401)), sorted(randoms(25, -100, 100, salt=2402))]),
            ("02-random-medium", [sorted(randoms(3000, -100000, 100000, salt=2403)), sorted(randoms(2000, -100000, 100000, salt=2404))]),
            ("03-large", [sorted(randoms(100000, -1000000000, 1000000000, salt=2405)), sorted(randoms(100000, -1000000000, 1000000000, salt=2406))]),
            ("04-interleaved", [list(range(0, 200, 2)), list(range(1, 200, 2))]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 두 포인터.
fun mergeSorted(first: IntArray, second: IntArray): IntArray {
    val out = IntArray(first.size + second.size)
    var i = 0
    var j = 0
    var k = 0
    while (i < first.size && j < second.size) {
        Drill.compare(i, j)
        out[k] = if (first[i] <= second[j]) first[i++] else second[j++]
        Drill.write(k, out[k])
        k += 1
    }
    while (i < first.size) { out[k] = first[i++]; Drill.write(k, out[k]); k += 1 }
    while (j < second.size) { out[k] = second[j++]; Drill.write(k, out[k]); k += 1 }
    return out
}
""",
    mutants=[
        ("drops-tail", "MISSING_EDGE_CASE",
         "한쪽이 끝나면 멈춘다. 남은 쪽을 붙이지 않는다.",
         """
fun mergeSorted(first: IntArray, second: IntArray): IntArray {
    val out = mutableListOf<Int>()
    var i = 0; var j = 0
    while (i < first.size && j < second.size) {
        out.add(if (first[i] <= second[j]) first[i++] else second[j++])
    }
    return out.toIntArray()
}
"""),
        ("drops-second-tail", "MISSING_EDGE_CASE",
         "first 의 나머지는 붙이는데 second 의 나머지는 잊는다.",
         """
fun mergeSorted(first: IntArray, second: IntArray): IntArray {
    val out = mutableListOf<Int>()
    var i = 0; var j = 0
    while (i < first.size && j < second.size) {
        out.add(if (first[i] <= second[j]) first[i++] else second[j++])
    }
    while (i < first.size) out.add(first[i++])
    return out.toIntArray()
}
"""),
        ("concat-only", "WRONG_ALGORITHM",
         "그냥 이어 붙인다. 정렬을 잊었다.",
         """
fun mergeSorted(first: IntArray, second: IntArray): IntArray = first + second
"""),
    ],
))
