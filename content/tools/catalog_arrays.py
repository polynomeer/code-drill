"""배열·투 포인터·슬라이딩 윈도우 (역량: 인덱스 다루기, 불변식 유지).

각 문제의 `mutants` 는 그 알고리즘에서 **실제로 자주 나는 실수**다. 아무 코드나 망가뜨려
놓으면 kill rate 는 쉽게 100% 가 되지만, 그 숫자는 테스트가 좋다는 뜻이 아니라 오답이
엉성하다는 뜻이다.
"""

from author import Problem, standard_groups, perf_groups, randoms, shuffled, flat

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


# --- 86. 최근 k개의 평균 (큐) -----------------------------------------------------------

def _moving_average(values, k):
    from collections import deque
    out = []
    window = deque()
    total = 0
    for v in values:
        window.append(v)
        total += v
        if len(window) > k:
            total -= window.popleft()
        out.append(total // len(window))
    return out


PROBLEMS.append(Problem(
    id="moving-average",
    title="최근 k개의 평균",
    summary="""
정수가 하나씩 도착한다. 값이 올 때마다 **최근 k개** (아직 k개가 안 됐으면 지금까지 전부)
의 평균을 내림해 기록한다. 기록한 평균들을 배열로 반환한다.

예: `values = [1, 10, 3, 5]`, `k = 3` 이면 `[1, 5, 4, 6]` 이다 — `1`, `(1+10)/2`, `(1+10+3)/3`,
`(10+3+5)/3`.
""",
    notes="""
매번 최근 k개를 다시 더하면 O(n·k) 다. 창에 들어오는 값을 더하고 나가는 값을 빼면 값마다
O(1) 이고, 나가는 값이 무엇인지는 **가장 먼저 들어온 것** — 큐가 기억한다.
""",
    drill_doc="""
Drill.enqueue(v)              // 창에 들어왔다
Drill.dequeue(v)              // 창에서 나갔다
Drill.write(i, avg)           // i 번째 평균
""",
    constraints="""
- `1 <= values.size <= 200_000`
- `1 <= k <= 100_000`
- `-10^4 <= values[i] <= 10^4` — 합은 `Int` 범위 안
- 평균은 **내림** (음수는 0 에서 멀어지는 쪽으로: `-7 / 2 = -4`)
""",
    signature=dict(name="movingAverage", parameters=[("values", "INT_ARRAY"), ("k", "INT")],
                   returns="INT_ARRAY"),
    groups=perf_groups(),
    reference=_moving_average,
    cases={
        "sample": [
            ("01", [[1, 10, 3, 5], 3]),
            ("02", [[4], 1]),
        ],
        "boundary": [
            ("01-k-one", [[3, -1, 4], 1]),
            # k 가 배열보다 크다. 끝까지 "지금까지 전부"다.
            ("02-k-larger", [[2, 4, 9], 10]),
            # 음수의 내림. -7 / 2 는 -4 다.
            ("03-negative-floor", [[-3, -4], 2]),
            ("04-mixed", [[5, -5, 5, -5], 2]),
            ("05-zeros", [[0, 0, 0], 2]),
        ],
        "hidden": [
            ("01-random", [randoms(50, -100, 100, salt=4201), 4]),
            ("02-random-big-k", [randoms(60, -1000, 1000, salt=4202), 7]),
            ("03-all-negative", [randoms(30, -50, -1, salt=4203), 3]),
        ],
        "performance": [
            ("01-small", [randoms(20000, -10000, 10000, salt=4204), 5000]),
            ("02-medium", [randoms(100000, -10000, 10000, salt=4205), 50000]),
            ("03-large", [randoms(200000, -10000, 10000, salt=4206), 100000]),
        ],
    },
    limits={"timeMillis": 2000, "memoryMb": 256, "outputBytes": 4000000},
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 창의 합을 유지하고 큐가 나갈 값을 기억한다.
fun movingAverage(values: IntArray, k: Int): IntArray {
    val out = IntArray(values.size)
    val window = ArrayDeque<Int>()
    var total = 0
    for ((i, v) in values.withIndex()) {
        window.addLast(v)
        Drill.enqueue(v)
        total += v
        if (window.size > k) {
            val gone = window.removeFirst()
            Drill.dequeue(gone)
            total -= gone
        }
        out[i] = Math.floorDiv(total, window.size)
        Drill.write(i, out[i])
    }
    return out
}
""",
    mutants=[
        ("truncates-toward-zero", "MISSING_EDGE_CASE",
         "정수 나눗셈을 그대로 쓴다. 음수 평균이 0 쪽으로 잘린다.",
         """
fun movingAverage(values: IntArray, k: Int): IntArray {
    val out = IntArray(values.size)
    val window = ArrayDeque<Int>()
    var total = 0
    for ((i, v) in values.withIndex()) {
        window.addLast(v); total += v
        if (window.size > k) total -= window.removeFirst()
        out[i] = total / window.size
    }
    return out
}
"""),
        ("divides-by-k--always", "WRONG_BRANCH",
         "창이 아직 k개가 안 찼는데도 k 로 나눈다.",
         """
fun movingAverage(values: IntArray, k: Int): IntArray {
    val out = IntArray(values.size)
    val window = ArrayDeque<Int>()
    var total = 0
    for ((i, v) in values.withIndex()) {
        window.addLast(v); total += v
        if (window.size > k) total -= window.removeFirst()
        out[i] = Math.floorDiv(total, k)
    }
    return out
}
"""),
        ("evicts-at-k", "OFF_BY_ONE",
         "창이 k 개 이상이면 내보낸다. 정확히 k 개일 때도 내보내 창이 k-1 개다.",
         """
fun movingAverage(values: IntArray, k: Int): IntArray {
    val out = IntArray(values.size)
    val window = ArrayDeque<Int>()
    var total = 0
    for ((i, v) in values.withIndex()) {
        window.addLast(v); total += v
        if (window.size >= k) total -= window.removeFirst()
        out[i] = Math.floorDiv(total, window.size)
    }
    return out
}
"""),
        ("resums-window--per-value", "PERFORMANCE",
         "값마다 최근 k개를 다시 더한다. O(n·k).",
         """
fun movingAverage(values: IntArray, k: Int): IntArray {
    val out = IntArray(values.size)
    for (i in values.indices) {
        val from = maxOf(0, i - k + 1)
        var total = 0
        for (j in from..i) { Drill.visit(j, values[j]); total += values[j] }
        out[i] = Math.floorDiv(total, i - from + 1)
    }
    return out
}
"""),
    ],
))


# --- 87. 합이 목표 이상인 가장 짧은 구간 (슬라이딩 윈도) ----------------------------------

def _min_subarray_len(nums, target):
    best = 0
    left = 0
    total = 0
    for right, v in enumerate(nums):
        total += v
        while total >= target:
            length = right - left + 1
            best = length if best == 0 or length < best else best
            total -= nums[left]
            left += 1
    return best


PROBLEMS.append(Problem(
    id="min-subarray-len",
    # v2: 안쪽 반복이 끊기지 않는 성능 케이스를 더했다. 목표에 금세 닿는 입력에서는 두 겹
    # 풀이가 CI 머신에서 한도의 2.5배에 그쳤다 (§12.1 재현성).
    version=2,
    title="합이 목표 이상인 가장 짧은 구간",
    summary="""
**양의 정수** 배열 `nums` 와 `target` 이 주어진다. 합이 `target` **이상**인 연속 구간 중
가장 짧은 것의 길이를 반환한다. 그런 구간이 없으면 `0` 이다.

예: `[2, 3, 1, 2, 4, 3]`, `target = 7` 이면 `[4, 3]` 으로 `2` 다.
""",
    notes="""
값이 전부 양수라 구간을 오른쪽으로 늘리면 합이 늘고 왼쪽을 줄이면 합이 준다. 그래서
창의 오른쪽 끝을 한 칸씩 밀면서, 합이 목표 이상인 동안 왼쪽을 최대한 줄이면 된다. 양쪽
끝이 각각 n 번만 움직인다 — O(n).
""",
    drill_doc="""
Drill.pointer("left", i)      // 창의 왼쪽
Drill.pointer("right", j)     // 창의 오른쪽
Drill.write(0, best)          // 지금까지의 최소 길이
""",
    constraints="""
- `1 <= nums.size <= 200_000`
- `1 <= nums[i] <= 10^4`, `1 <= target <= 10^9`
""",
    signature=dict(name="minSubarrayLen", parameters=[("nums", "INT_ARRAY"), ("target", "INT")],
                   returns="INT"),
    groups=perf_groups(time_multiplier=0.5),
    reference=_min_subarray_len,
    cases={
        "sample": [
            ("01", [[2, 3, 1, 2, 4, 3], 7]),
            ("02", [[1, 4, 4], 4]),
        ],
        "boundary": [
            ("01-none", [[1, 1, 1], 10]),
            ("02-single-enough", [[10], 7]),
            # 정확히 같은 합. "이상"이다.
            ("03-exact", [[1, 2, 3, 4], 10]),
            # 전체가 답이다.
            ("04-whole", [[1, 1, 1, 1], 4]),
            # 가장 짧은 구간이 맨 끝에 있다.
            ("05-at-end", [[1, 1, 1, 1, 9], 9]),
            ("06-at-start", [[9, 1, 1, 1, 1], 9]),
        ],
        "hidden": [
            ("01-random-small", [randoms(30, 1, 20, salt=4301), 50]),
            ("02-random-medium", [randoms(500, 1, 100, salt=4302), 900]),
            ("03-random-unreachable", [randoms(200, 1, 10, salt=4303), 1000000]),
            ("04-big-values", [randoms(100, 9000, 10000, salt=4304), 27000]),
        ],
        "performance": [
            ("01-small", [randoms(20000, 1, 100, salt=4305), 300000]),
            ("02-medium", [randoms(100000, 1, 100, salt=4306), 2000000]),
            ("03-large", [randoms(200000, 1, 10, salt=4307), 500000]),
            # 목표가 전체 합 바로 아래다 — 첫 자리 말고는 끝까지 더해도 닿지 못해,
            # 시작점마다 더하는 오답의 안쪽 반복이 한 번도 끊기지 않는다.
            ("04-never-reaches", [randoms(200000, 1, 10, salt=4309), sum(randoms(200000, 1, 10, salt=4309)) - 3]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 양쪽 끝이 각각 n 번만 움직이는 창.
fun minSubarrayLen(nums: IntArray, target: Int): Int {
    var best = 0
    var left = 0
    var total = 0L
    for (right in nums.indices) {
        total += nums[right]
        Drill.pointer("right", right)
        while (total >= target) {
            val length = right - left + 1
            if (best == 0 || length < best) { best = length; Drill.write(0, best) }
            total -= nums[left]
            left += 1
            Drill.pointer("left", left)
        }
    }
    return best
}
""",
    mutants=[
        ("strictly-greater", "OFF_BY_ONE",
         "합이 목표보다 커야 한다고 본다. 정확히 같은 합을 놓친다.",
         """
fun minSubarrayLen(nums: IntArray, target: Int): Int {
    var best = 0; var left = 0; var total = 0L
    for (right in nums.indices) {
        total += nums[right]
        while (total > target) {
            val length = right - left + 1
            if (best == 0 || length < best) best = length
            total -= nums[left]; left += 1
        }
    }
    return best
}
"""),
        ("shrinks-once--per-step", "WRONG_BRANCH",
         "합이 목표 이상이면 왼쪽을 한 칸만 줄인다. 더 줄일 수 있어도 멈춘다.",
         """
fun minSubarrayLen(nums: IntArray, target: Int): Int {
    var best = 0; var left = 0; var total = 0L
    for (right in nums.indices) {
        total += nums[right]
        if (total >= target) {
            val length = right - left + 1
            if (best == 0 || length < best) best = length
            total -= nums[left]; left += 1
        }
    }
    return best
}
"""),
        ("returns-max-length", "WRONG_ALGORITHM",
         "가장 긴 구간을 답한다.",
         """
fun minSubarrayLen(nums: IntArray, target: Int): Int {
    var best = 0; var left = 0; var total = 0L
    for (right in nums.indices) {
        total += nums[right]
        while (total >= target) {
            best = maxOf(best, right - left + 1)
            total -= nums[left]; left += 1
        }
    }
    return best
}
"""),
        ("all-starts--quadratic", "PERFORMANCE",
         "시작점마다 합이 목표에 닿을 때까지 더한다. 닿지 않으면 끝까지 — O(n²).",
         """
fun minSubarrayLen(nums: IntArray, target: Int): Int {
    var best = 0
    for (i in nums.indices) {
        var total = 0L
        for (j in i until nums.size) {
            total += nums[j]
            Drill.compare(i, j)
            if (total >= target) { if (best == 0 || j - i + 1 < best) best = j - i + 1; break }
        }
    }
    return best
}
"""),
    ],
))


# --- 88. 정렬된 배열에서 중복 없애기 (제자리) -------------------------------------------------

def _remove_duplicates_sorted(nums):
    out = []
    for v in nums:
        if not out or out[-1] != v:
            out.append(v)
    return out


PROBLEMS.append(Problem(
    id="remove-duplicates-sorted",
    title="정렬된 배열에서 중복 없애기",
    summary="""
오름차순으로 정렬된 정수 배열 `nums` 가 주어진다. 같은 값이 한 번씩만 남도록 중복을
지운 배열을 **순서를 유지해** 반환한다.

예: `[1, 1, 2, 2, 2, 3]` → `[1, 2, 3]`.
""",
    notes="""
정렬돼 있으므로 같은 값은 붙어 있다. 바로 앞에 남긴 값과 다를 때만 남기면 된다 — 쓰는
자리와 읽는 자리를 따로 들고 한 번 훑는 것이 제자리 풀이다. 집합에 넣으면 순서가 흔들릴
수 있고, 새 배열에 담으면 메모리를 두 배 쓴다.
""",
    drill_doc="""
Drill.pointer("read", i)      // 읽는 자리
Drill.pointer("write", j)     // 쓰는 자리
Drill.write(j, v)             // 남겼다
""",
    constraints="""
- `0 <= nums.size <= 200_000`
- `-10^9 <= nums[i] <= 10^9`, 오름차순
""",
    signature=dict(name="removeDuplicates", parameters=[("nums", "INT_ARRAY")], returns="INT_ARRAY"),
    groups=standard_groups(),
    reference=_remove_duplicates_sorted,
    cases={
        "sample": [
            ("01", [[1, 1, 2, 2, 2, 3]]),
            ("02", [[0, 0, 1, 1, 1, 2, 2, 3, 3, 4]]),
        ],
        "boundary": [
            ("01-empty", [[]]),
            ("02-single", [[7]]),
            ("03-all-same", [[5, 5, 5, 5]]),
            ("04-no-duplicates", [[1, 2, 3, 4]]),
            # 음수와 큰 값. 값의 크기는 상관없다.
            ("05-negatives", [[-1000000000, -1000000000, -1, 0, 0, 1000000000]]),
            # 중복이 맨 끝에만 있다.
            ("06-tail-run", [[1, 2, 3, 3, 3]]),
        ],
        "hidden": [
            ("01-random", [sorted(randoms(60, 0, 20, salt=4401))]),
            ("02-random-sparse", [sorted(randoms(80, -50, 50, salt=4402))]),
            ("03-large", [sorted(randoms(200000, -1000, 1000, salt=4403))]),
        ],
    },
    limits={"timeMillis": 2000, "memoryMb": 256, "outputBytes": 4000000},
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 읽는 자리와 쓰는 자리가 따로 간다.
fun removeDuplicates(nums: IntArray): IntArray {
    if (nums.isEmpty()) return nums
    var write = 1
    for (read in 1 until nums.size) {
        Drill.pointer("read", read)
        if (nums[read] != nums[write - 1]) {
            nums[write] = nums[read]
            Drill.write(write, nums[read])
            write += 1
            Drill.pointer("write", write)
        }
    }
    return nums.copyOf(write)
}
""",
    mutants=[
        ("skips-first", "OFF_BY_ONE",
         "1 번부터 앞 값과 비교해 남긴다. 첫 값은 비교 상대가 없어 빠진다.",
         """
fun removeDuplicates(nums: IntArray): IntArray {
    val out = ArrayList<Int>()
    for (i in 1 until nums.size) if (nums[i] != nums[i - 1]) out.add(nums[i])
    return out.toIntArray()
}
"""),
        ("adjacent-only--keeps-first-of-run", "OFF_BY_ONE",
         "다음 값과 다를 때만 남긴다. 마지막 원소가 빠진다.",
         """
fun removeDuplicates(nums: IntArray): IntArray {
    val out = ArrayList<Int>()
    for (i in 0 until nums.size - 1) if (nums[i] != nums[i + 1]) out.add(nums[i])
    return out.toIntArray()
}
"""),
        ("hash-set--loses-order", "WRONG_ALGORITHM",
         "해시 집합에 넣고 꺼낸다. 순서가 값의 해시 순서다.",
         """
fun removeDuplicates(nums: IntArray): IntArray = nums.toHashSet().toIntArray()
"""),
    ],
))


# --- 99. 가장 긴 연속 수열 (해시 집합) ---------------------------------------------------

def _longest_consecutive(nums):
    present = set(nums)
    best = 0
    for v in present:
        if v - 1 in present:
            continue
        length = 1
        while v + length in present:
            length += 1
        best = max(best, length)
    return best


def _runs(lengths, gap, salt):
    """서로 떨어진 연속 구간들을 섞어 놓는다."""
    out = []
    base = -500000
    for i, n in enumerate(lengths):
        out += list(range(base, base + n))
        base += n + gap + i
    return shuffled(out, salt=salt)


PROBLEMS.append(Problem(
    id="longest-consecutive",
    title="가장 긴 연속 수열",
    summary="""
정렬되지 않은 정수 배열 `nums` 가 주어진다. 값이 **1 씩 이어지는** 수들의 집합 중 가장 큰
것의 크기를 반환한다. 순서는 상관없고 같은 값은 한 번으로 센다. 빈 배열은 `0`.

예: `[100, 4, 200, 1, 3, 2]` 는 `1, 2, 3, 4` 로 `4` 다.
""",
    notes="""
정렬하면 O(n log n) 이다. O(n) 으로 하려면 집합에 넣고, **수열의 시작점** — `v-1` 이 없는
`v` — 에서만 위로 세면 된다. 시작점이 아닌 곳에서도 세면 긴 수열 하나가 길이의 제곱만큼
비용을 낸다.
""",
    drill_doc="""
Drill.visit(i, v)             // 시작점을 찾았다
Drill.compare(v, v + 1)       // 다음 수가 있는지 봤다
Drill.write(0, best)          // 지금까지의 최대
""",
    constraints="""
- `0 <= nums.size <= 200_000`
- `-10^9 <= nums[i] <= 10^9`
""",
    signature=dict(name="longestConsecutive", parameters=[("nums", "INT_ARRAY")], returns="INT"),
    groups=perf_groups(time_multiplier=0.5),
    reference=_longest_consecutive,
    cases={
        "sample": [
            ("01", [[100, 4, 200, 1, 3, 2]]),
            ("02", [[0, 3, 7, 2, 5, 8, 4, 6, 0, 1]]),
        ],
        "boundary": [
            ("01-empty", [[]]),
            ("02-single", [[5]]),
            # 같은 값은 한 번이다.
            ("03-duplicates", [[1, 2, 2, 3, 3, 3]]),
            # 음수를 지나는 수열.
            ("04-across-zero", [[-2, 0, -1, 1, 3]]),
            # 값이 Int 의 끝이다 — v+1 이 넘칠 수 있다.
            ("05-max-int", [[2147483647, 2147483646]]),
            ("06-min-int", [[-2147483648, -2147483647]]),
            ("07-no-runs", [[10, 20, 30]]),
            # Int 의 양 끝. v+1 이 넘치면 MAX 다음이 MIN 이 되어 이어지는 것처럼 보인다.
            ("08-wraparound", [[2147483647, -2147483648]]),
        ],
        "hidden": [
            ("01-random", [randoms(200, -50, 50, salt=6301)]),
            ("02-runs", [_runs([5, 12, 3, 9], 7, salt=6302)]),
            ("03-two-equal-runs", [_runs([10, 10], 100, salt=6303)]),
            ("04-sparse", [randoms(300, -1000000000, 1000000000, salt=6304)]),
        ],
        "performance": [
            # 긴 수열 하나. 시작점이 아닌 곳에서도 세면 길이의 제곱이다.
            ("01-small", [_runs([20000], 1, salt=6305)]),
            ("02-medium", [_runs([100000], 1, salt=6306)]),
            ("03-large", [_runs([200000], 1, salt=6307)]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 집합에 넣고 시작점에서만 위로 센다.
fun longestConsecutive(nums: IntArray): Int {
    val present = HashSet<Int>(nums.size * 2)
    for (v in nums) present.add(v)
    var best = 0
    for ((i, v) in nums.withIndex()) {
        if (present.contains(v - 1)) continue
        Drill.visit(i, v)
        var length = 1
        var next = v.toLong() + 1
        while (next <= Int.MAX_VALUE && present.contains(next.toInt())) { length += 1; next += 1 }
        if (length > best) { best = length; Drill.write(0, best) }
    }
    return best
}
""",
    mutants=[
        ("counts-duplicates", "MISSING_EDGE_CASE",
         "정렬해 이웃이 같거나 1 차이면 잇는다. 같은 값이 길이에 들어간다.",
         """
fun longestConsecutive(nums: IntArray): Int {
    if (nums.isEmpty()) return 0
    val sorted = nums.sorted()
    var best = 1; var length = 1
    for (i in 1 until sorted.size) {
        if (sorted[i] - sorted[i - 1] <= 1) length += 1 else length = 1
        best = maxOf(best, length)
    }
    return best
}
"""),
        ("start-check-inverted", "WRONG_BRANCH",
         "v+1 이 없는 값을 시작점으로 보고 위로 센다. 수열의 끝에서 위로 세니 항상 1 이다.",
         """
fun longestConsecutive(nums: IntArray): Int {
    val present = HashSet<Int>()
    for (v in nums) present.add(v)
    var best = 0
    for (v in nums) {
        if (present.contains(v + 1)) continue
        var length = 1
        while (present.contains(v + length)) length += 1
        best = maxOf(best, length)
    }
    return best
}
"""),
        ("int-overflow-at-max", "MISSING_EDGE_CASE",
         "v + 1 을 Int 로 계산한다. Int.MAX_VALUE 다음이 Int.MIN_VALUE 가 되어 이어진다.",
         """
fun longestConsecutive(nums: IntArray): Int {
    val present = HashSet<Int>()
    for (v in nums) present.add(v)
    var best = 0
    for (v in nums) {
        if (present.contains(v - 1)) continue
        var length = 1
        while (present.contains(v + length)) length += 1
        best = maxOf(best, length)
    }
    return best
}
"""),
        ("no-start-check", "PERFORMANCE",
         "모든 값에서 위로 센다. 긴 수열 하나가 길이의 제곱이다.",
         """
fun longestConsecutive(nums: IntArray): Int {
    val present = HashSet<Int>()
    for (v in nums) present.add(v)
    var best = 0
    for (v in nums) {
        var length = 1
        var next = v.toLong() + 1
        while (next <= Int.MAX_VALUE && present.contains(next.toInt())) { Drill.compare(v, next.toInt()); length += 1; next += 1 }
        best = maxOf(best, length)
    }
    return best
}
"""),
    ],
))


# --- 101. 둘째로 큰 값 (입문) ---------------------------------------------------------

def _second_largest(nums):
    distinct = sorted(set(nums))
    return distinct[-2] if len(distinct) >= 2 else -1


PROBLEMS.append(Problem(
    id="second-largest",
    title="둘째로 큰 값",
    summary="""
정수 배열 `nums` 에서 **서로 다른 값 중** 둘째로 큰 값을 반환한다. 서로 다른 값이 둘
미만이면 `-1` 이다.

예: `[3, 5, 5, 1]` → `3`. `[7, 7]` → `-1`.
""",
    notes="""
가장 큰 값과 그 다음을 한 번 훑으며 들고 간다. 가장 큰 값과 **같은** 값은 둘째가 아니다 —
그 한 줄이 이 문제의 전부다. 정렬하면 O(n log n) 이지만 틀리지는 않는다.
""",
    drill_doc="""
Drill.visit(i, v)             // 원소를 봤다
Drill.write(0, first)         // 지금까지의 최댓값
Drill.write(1, second)        // 지금까지의 둘째
""",
    constraints="""
- `1 <= nums.size <= 200_000`
- `-10^9 <= nums[i] <= 10^9`
""",
    signature=dict(name="secondLargest", parameters=[("nums", "INT_ARRAY")], returns="INT"),
    groups=standard_groups(),
    reference=_second_largest,
    cases={
        "sample": [("01", [[3, 5, 5, 1]]), ("02", [[7, 7]])],
        "boundary": [
            ("01-single", [[4]]),
            ("02-two-distinct", [[1, 2]]),
            # 최댓값이 여럿. 같은 값은 둘째가 아니다.
            ("03-max-repeated", [[9, 9, 9, 2]]),
            # 음수만. "둘째"의 초기값을 0 으로 두면 틀린다.
            ("04-all-negative", [[-3, -1, -2]]),
            # 둘째가 Int 의 끝.
            ("05-extremes", [[-1000000000, 1000000000]]),
            ("06-descending", [[5, 4, 3, 2, 1]]),
            ("07-ascending", [[1, 2, 3, 4, 5]]),
            # 최댓값이 맨 앞이고 나머지가 음수. 둘째의 초기값이 0 이면 아무것도 둘째가 못 된다.
            ("08-max-first-then-negative", [[-1, -3, -2]]),
        ],
        "hidden": [
            ("01-random", [randoms(100, -50, 50, salt=7101)]),
            ("02-random-big", [randoms(200000, -1000000000, 1000000000, salt=7102)]),
            ("03-mostly-same", [[5] * 50 + [4]]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 최댓값과 둘째를 한 번에 들고 간다.
fun secondLargest(nums: IntArray): Int {
    var first: Long = Long.MIN_VALUE
    var second: Long = Long.MIN_VALUE
    for ((i, v) in nums.withIndex()) {
        Drill.visit(i, v)
        if (v > first) { second = first; first = v.toLong(); Drill.write(0, v) }
        else if (v < first && v > second) { second = v.toLong(); Drill.write(1, v) }
    }
    return if (second == Long.MIN_VALUE) -1 else second.toInt()
}
""",
    mutants=[
        ("counts-duplicate-max", "MISSING_EDGE_CASE", "최댓값과 같은 값을 둘째로 센다.", """
fun secondLargest(nums: IntArray): Int {
    var first = Long.MIN_VALUE; var second = Long.MIN_VALUE
    for (v in nums) {
        if (v > first) { second = first; first = v.toLong() }
        else if (v > second) second = v.toLong()
    }
    return if (second == Long.MIN_VALUE) -1 else second.toInt()
}
"""),
        ("zero-initial", "MISSING_EDGE_CASE", "둘째의 초기값을 0 으로 둔다. 최댓값이 맨 앞이고 나머지가 음수면 아무것도 둘째가 못 된다.", """
fun secondLargest(nums: IntArray): Int {
    var first = nums[0]; var second = 0
    for (v in nums) {
        if (v > first) { second = first; first = v }
        else if (v < first && v > second) second = v
    }
    return if (second != first && nums.contains(second)) second else -1
}
"""),
        ("sorted-second-index", "WRONG_ALGORITHM", "정렬해 끝에서 둘째를 답한다. 같은 값을 거르지 않는다.", """
fun secondLargest(nums: IntArray): Int {
    if (nums.size < 2) return -1
    val sorted = nums.sorted()
    return sorted[sorted.size - 2]
}
"""),
    ],
))


# --- 102. 하나 더하기 (입문, 자리 올림) --------------------------------------------------

def _plus_one(digits):
    out = list(digits)
    i = len(out) - 1
    while i >= 0:
        if out[i] < 9:
            out[i] += 1
            return out
        out[i] = 0
        i -= 1
    return [1] + out


PROBLEMS.append(Problem(
    id="plus-one",
    title="하나 더하기",
    summary="""
큰 정수의 십진 자릿수가 `digits` 에 앞자리부터 담겨 있다 (`[1, 2, 3]` 은 123). 이 수에
`1` 을 더한 자릿수 배열을 반환한다. 앞에 0 이 붙지 않는다.

예: `[1, 2, 9]` → `[1, 3, 0]`. `[9, 9]` → `[1, 0, 0]`.
""",
    notes="""
정수로 바꾸면 자릿수가 많을 때 넘친다. 끝자리부터 보며 9 는 0 으로 바꾸고 올림을 넘기고,
9 가 아닌 자리를 만나면 하나 더하고 끝낸다. 끝까지 올림이 남으면 앞에 1 을 붙인다.
""",
    drill_doc="""
Drill.visit(i, d)             // 자리를 봤다
Drill.write(i, v)             // 자리를 바꿨다
""",
    constraints="""
- `1 <= digits.size <= 100_000`, 각 자리는 `0..9`, 첫 자리는 `0` 이 아니다 (수가 0 이면 `[0]`)
""",
    signature=dict(name="plusOne", parameters=[("digits", "INT_ARRAY")], returns="INT_ARRAY"),
    groups=standard_groups(),
    reference=_plus_one,
    cases={
        "sample": [("01", [[1, 2, 9]]), ("02", [[9, 9]])],
        "boundary": [
            ("01-zero", [[0]]),
            ("02-nine", [[9]]),
            ("03-no-carry", [[1, 2, 3]]),
            # 가운데서 올림이 멈춘다.
            ("04-carry-stops", [[1, 9, 9]]),
            # 자릿수가 Long 을 넘는다. 정수로 바꾸면 넘친다.
            ("05-too-long-for-long", [[9] * 25]),
            ("06-long-no-carry", [[1] + [0] * 30]),
        ],
        "hidden": [
            ("01-random", [[1] + randoms(60, 0, 9, salt=7201)]),
            ("02-random-trailing-nines", [[1] + randoms(40, 0, 9, salt=7202) + [9] * 20]),
            ("03-all-nines-big", [[9] * 100000]),
        ],
    },
    limits={"timeMillis": 2000, "memoryMb": 256, "outputBytes": 2000000},
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 끝자리부터 올림을 넘긴다.
fun plusOne(digits: IntArray): IntArray {
    val out = digits.copyOf()
    for (i in out.indices.reversed()) {
        Drill.visit(i, out[i])
        if (out[i] < 9) { out[i] += 1; Drill.write(i, out[i]); return out }
        out[i] = 0
        Drill.write(i, 0)
    }
    val grown = IntArray(out.size + 1)
    grown[0] = 1
    return grown
}
""",
    mutants=[
        ("no-growth", "MISSING_EDGE_CASE", "전부 9 일 때 자릿수를 늘리지 않는다. 0 들만 남는다.", """
fun plusOne(digits: IntArray): IntArray {
    val out = digits.copyOf()
    for (i in out.indices.reversed()) {
        if (out[i] < 9) { out[i] += 1; return out }
        out[i] = 0
    }
    return out
}
"""),
        ("long-conversion--overflows", "MISSING_EDGE_CASE", "Long 으로 바꿔 더한다. 자릿수가 19 를 넘으면 넘친다.", """
fun plusOne(digits: IntArray): IntArray {
    var n = 0L
    for (d in digits) n = n * 10 + d
    n += 1
    val s = n.toString()
    return IntArray(s.length) { s[it] - '0' }
}
"""),
        ("carries-without-stopping", "WRONG_BRANCH", "올림이 끝난 뒤에도 앞자리에 계속 1 을 더한다.", """
fun plusOne(digits: IntArray): IntArray {
    val out = digits.copyOf()
    var carry = 1
    for (i in out.indices.reversed()) {
        out[i] += carry
        if (out[i] == 10) out[i] = 0 else carry = 1
    }
    return if (out[0] == 0 && carry == 1 && digits.all { it == 9 }) intArrayOf(1) + out else out
}
"""),
    ],
))


# --- 103. 누적 합 (입문) ------------------------------------------------------------

def _running_sum(nums):
    out = []
    total = 0
    for v in nums:
        total += v
        out.append(total)
    return out


PROBLEMS.append(Problem(
    id="running-sum",
    title="누적 합",
    summary="""
정수 배열 `nums` 에 대해 `out[i] = nums[0] + ... + nums[i]` 인 배열을 반환한다.

예: `[1, 2, 3, 4]` → `[1, 3, 6, 10]`.
""",
    notes="""
앞의 누적 합에 지금 값을 더하면 된다 — `out[i] = out[i-1] + nums[i]`. 매번 처음부터 다시
더하면 O(n²) 이고, 이 문제의 크기에서는 그것도 잡힌다.
""",
    drill_doc="""
Drill.visit(i, v)             // 원소를 봤다
Drill.write(i, total)         // 누적 합을 적었다
""",
    constraints="""
- `1 <= nums.size <= 200_000`
- `-10^4 <= nums[i] <= 10^4` — 합은 `Int` 범위 안
""",
    signature=dict(name="runningSum", parameters=[("nums", "INT_ARRAY")], returns="INT_ARRAY"),
    groups=perf_groups(),
    reference=_running_sum,
    cases={
        "sample": [("01", [[1, 2, 3, 4]]), ("02", [[3, -1, 4]])],
        "boundary": [
            ("01-single", [[7]]),
            ("02-negatives", [[-1, -2, -3]]),
            ("03-zeros", [[0, 0, 0]]),
            # 합이 0 을 지나 음수가 됐다 돌아온다.
            ("04-crosses-zero", [[5, -10, 5, 5]]),
        ],
        "hidden": [
            ("01-random", [randoms(100, -100, 100, salt=7301)]),
            ("02-random-big-values", [randoms(500, -10000, 10000, salt=7302)]),
        ],
        "performance": [
            ("01-small", [randoms(20000, -10000, 10000, salt=7303)]),
            ("02-medium", [randoms(100000, -10000, 10000, salt=7304)]),
            ("03-large", [randoms(200000, -10000, 10000, salt=7305)]),
        ],
    },
    limits={"timeMillis": 2000, "memoryMb": 256, "outputBytes": 4000000},
    kotlin="""
// 검증용 정답 (§6.1 solutions/).
fun runningSum(nums: IntArray): IntArray {
    val out = IntArray(nums.size)
    var total = 0
    for ((i, v) in nums.withIndex()) {
        Drill.visit(i, v)
        total += v
        out[i] = total
        Drill.write(i, total)
    }
    return out
}
""",
    mutants=[
        ("skips-first", "OFF_BY_ONE", "1 번부터 시작해 첫 원소를 누적에 넣지 않는다.", """
fun runningSum(nums: IntArray): IntArray {
    val out = IntArray(nums.size)
    for (i in 1 until nums.size) out[i] = out[i - 1] + nums[i]
    return out
}
"""),
        ("resums--quadratic", "PERFORMANCE", "원소마다 처음부터 다시 더한다. O(n²).", """
fun runningSum(nums: IntArray): IntArray {
    val out = IntArray(nums.size)
    for (i in nums.indices) {
        var total = 0
        for (j in 0..i) { Drill.visit(j, nums[j]); total += nums[j] }
        out[i] = total
    }
    return out
}
"""),
        ("prefix-excludes-self", "OFF_BY_ONE", "자기 앞까지의 합을 적는다. 한 칸 밀린다.", """
fun runningSum(nums: IntArray): IntArray {
    val out = IntArray(nums.size)
    var total = 0
    for (i in nums.indices) { out[i] = total; total += nums[i] }
    return out
}
"""),
    ],
))


# --- 148. 창의 중앙값 (힙 둘과 미룬 삭제) -------------------------------------------------------------

def _window_medians(nums, k):
    import heapq
    n = len(nums)
    low, high = [], []          # low: 최대 힙(음수), high: 최소 힙
    delayed = {}
    low_size = high_size = 0    # 지워지지 않은 원소 수

    def prune(heap):
        while heap:
            x = heap[0] if heap is high else -heap[0]
            if delayed.get(x, 0):
                delayed[x] -= 1
                heapq.heappop(heap)
            else:
                break

    def balance():
        nonlocal low_size, high_size
        if low_size > high_size + 1:
            heapq.heappush(high, -heapq.heappop(low)); low_size -= 1; high_size += 1
            prune(low)
        elif low_size < high_size:
            heapq.heappush(low, -heapq.heappop(high)); high_size -= 1; low_size += 1
            prune(high)

    def add(x):
        nonlocal low_size, high_size
        if not low or x <= -low[0]:
            heapq.heappush(low, -x); low_size += 1
        else:
            heapq.heappush(high, x); high_size += 1
        balance()

    def remove(x):
        nonlocal low_size, high_size
        delayed[x] = delayed.get(x, 0) + 1
        if low and x <= -low[0]:
            low_size -= 1
            if x == -low[0]:
                prune(low)
        else:
            high_size -= 1
            if high and x == high[0]:
                prune(high)
        balance()

    out = []
    for i in range(n):
        add(nums[i])
        if i >= k:
            remove(nums[i - k])
        if i >= k - 1:
            out.append(-low[0])
    return out


PROBLEMS.append(Problem(
    id="sliding-window-median",
    title="창의 중앙값",
    summary="""
정수 배열 `nums` 와 **홀수** `k` 가 주어진다. 길이 `k` 의 창을 왼쪽부터 오른쪽으로 한 칸씩 옮기며,
창마다의 **중앙값**(정렬했을 때 가운데 값)을 순서대로 담은 배열을 반환한다.
""",
    notes="""
창마다 정렬하면 n·k log k 다. 창을 **작은 절반(최대 힙)과 큰 절반(최소 힙)** 으로 나눠 들고
있으면 중앙값은 작은 절반의 꼭대기다. 창에서 나가는 원소는 힙 한가운데에 있을 수 있어 바로
못 지운다 — "지울 것" 으로 적어 두고 꼭대기에 올라왔을 때 걷어 내며, 두 힙의 **살아 있는**
원소 수로 균형을 잡는다. 정렬된 구조(트리)로 k 번째를 뽑는 것도 같은 O(n log k) 다.
""",
    drill_doc="""
Drill.compare(i, median)      // 창의 중앙값을 냈다
Drill.write(i, median)        // 답을 적었다
""",
    constraints="""
- `1 <= k <= nums.length <= 100_000`, `k` 는 홀수
- `-2^31 <= nums[i] <= 2^31 - 1`
""",
    signature=dict(name="slidingWindowMedian", parameters=[("nums", "INT_ARRAY"), ("k", "INT")], returns="INT_ARRAY"),
    groups=perf_groups(time_multiplier=0.5),
    reference=_window_medians,
    limits={"timeMillis": 2000, "memoryMb": 256, "outputBytes": 2000000},
    cases={
        "sample": [("01", [[1, 3, -1, -3, 5, 3, 6, 7], 3]), ("02", [[4, 2, 1], 1])],
        "boundary": [
            ("01-window-is-whole", [[5, 1, 9, 3, 7], 5]),
            # 같은 값이 여럿 — 미룬 삭제가 값이 아니라 개수여야 한다.
            ("02-duplicates", [[2, 2, 2, 1, 2, 2, 3], 3]),
            ("03-descending", [[9, 8, 7, 6, 5, 4], 3]),
            ("04-int-extremes", [[2147483647, -2147483648, 0, 2147483647, -2147483648], 3]),
            # 나가는 원소가 힙 한가운데에 있다.
            ("05-leaving-from-middle", [[1, 5, 3, 4, 2, 6, 0], 5]),
            ("06-single-element", [[-4], 1]),
        ],
        "hidden": [
            ("01-random-small", [randoms(12, -5, 5, salt=8801), 3]),
            ("02-random-medium", [randoms(500, -100, 100, salt=8802), 7]),
            ("03-random-wide", [randoms(3000, -2147483648, 2147483647, salt=8803), 101]),
            ("04-many-duplicates", [randoms(2000, 0, 2, salt=8804), 9]),
        ],
        "performance": [
            ("01-small", [randoms(20000, -1000000, 1000000, salt=8811), 999]),
            ("02-medium", [randoms(60000, -1000000, 1000000, salt=8812), 5001]),
            ("03-large", [randoms(100000, -1000000, 1000000, salt=8813), 20001]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 최대 힙 + 최소 힙, 미룬 삭제, 살아 있는 수로 균형.
fun slidingWindowMedian(nums: IntArray, k: Int): IntArray {
    val low = java.util.PriorityQueue<Int>(compareByDescending { it })
    val high = java.util.PriorityQueue<Int>()
    val delayed = HashMap<Int, Int>()
    var lowSize = 0; var highSize = 0
    fun prune(heap: java.util.PriorityQueue<Int>) {
        while (heap.isNotEmpty()) {
            val top = heap.peek()
            val pending = delayed[top] ?: 0
            if (pending == 0) break
            if (pending == 1) delayed.remove(top) else delayed[top] = pending - 1
            heap.poll()
        }
    }
    fun balance() {
        if (lowSize > highSize + 1) { high.add(low.poll()); lowSize -= 1; highSize += 1; prune(low) }
        else if (lowSize < highSize) { low.add(high.poll()); highSize -= 1; lowSize += 1; prune(high) }
    }
    fun add(x: Int) {
        if (low.isEmpty() || x <= low.peek()) { low.add(x); lowSize += 1 } else { high.add(x); highSize += 1 }
        balance()
    }
    fun remove(x: Int) {
        delayed[x] = (delayed[x] ?: 0) + 1
        if (low.isNotEmpty() && x <= low.peek()) { lowSize -= 1; if (x == low.peek()) prune(low) }
        else { highSize -= 1; if (high.isNotEmpty() && x == high.peek()) prune(high) }
        balance()
    }
    val out = IntArray(nums.size - k + 1)
    for (i in nums.indices) {
        add(nums[i])
        if (i >= k) remove(nums[i - k])
        if (i >= k - 1) { out[i - k + 1] = low.peek(); Drill.compare(i, out[i - k + 1]); Drill.write(i - k + 1, out[i - k + 1]) }
    }
    return out
}
""",
    mutants=[
        ("balances-by-heap-size--counts-dead", "WRONG_BRANCH",
         "균형을 살아 있는 수가 아니라 힙의 크기로 잡는다. 지울 것으로 적어 둔 원소가 절반을 부풀린다.",
         """
fun slidingWindowMedian(nums: IntArray, k: Int): IntArray {
    val low = java.util.PriorityQueue<Int>(compareByDescending { it })
    val high = java.util.PriorityQueue<Int>()
    val delayed = HashMap<Int, Int>()
    fun prune(heap: java.util.PriorityQueue<Int>) {
        while (heap.isNotEmpty()) { val top = heap.peek(); val pending = delayed[top] ?: 0; if (pending == 0) break; if (pending == 1) delayed.remove(top) else delayed[top] = pending - 1; heap.poll() }
    }
    fun balance() {
        if (low.size > high.size + 1) { high.add(low.poll()); prune(low) }
        else if (low.size < high.size) { low.add(high.poll()); prune(high) }
    }
    fun add(x: Int) { if (low.isEmpty() || x <= low.peek()) low.add(x) else high.add(x); balance() }
    fun remove(x: Int) {
        delayed[x] = (delayed[x] ?: 0) + 1
        if (low.isNotEmpty() && x <= low.peek()) { if (x == low.peek()) prune(low) } else { if (high.isNotEmpty() && x == high.peek()) prune(high) }
        balance()
    }
    val out = IntArray(nums.size - k + 1)
    for (i in nums.indices) { add(nums[i]); if (i >= k) remove(nums[i - k]); if (i >= k - 1) out[i - k + 1] = low.peek() }
    return out
}
"""),
        ("delayed-as-set--loses-duplicates", "MISSING_EDGE_CASE",
         "지울 것을 개수가 아니라 집합으로 둔다. 같은 값이 두 번 나가면 하나만 지워진다.",
         """
fun slidingWindowMedian(nums: IntArray, k: Int): IntArray {
    val low = java.util.PriorityQueue<Int>(compareByDescending { it })
    val high = java.util.PriorityQueue<Int>()
    val delayed = HashSet<Int>()
    var lowSize = 0; var highSize = 0
    fun prune(heap: java.util.PriorityQueue<Int>) {
        while (heap.isNotEmpty() && delayed.remove(heap.peek())) heap.poll()
    }
    fun balance() {
        if (lowSize > highSize + 1) { high.add(low.poll()); lowSize -= 1; highSize += 1; prune(low) }
        else if (lowSize < highSize) { low.add(high.poll()); highSize -= 1; lowSize += 1; prune(high) }
    }
    fun add(x: Int) { if (low.isEmpty() || x <= low.peek()) { low.add(x); lowSize += 1 } else { high.add(x); highSize += 1 }; balance() }
    fun remove(x: Int) {
        delayed.add(x)
        if (low.isNotEmpty() && x <= low.peek()) { lowSize -= 1; if (x == low.peek()) prune(low) } else { highSize -= 1; if (high.isNotEmpty() && x == high.peek()) prune(high) }
        balance()
    }
    val out = IntArray(nums.size - k + 1)
    for (i in nums.indices) { add(nums[i]); if (i >= k) remove(nums[i - k]); if (i >= k - 1) out[i - k + 1] = low.peek() }
    return out
}
"""),
        ("removes-without-pruning-top", "WRONG_BRANCH",
         "나가는 원소가 꼭대기여도 걷어 내지 않는다. 죽은 원소가 중앙값으로 나온다.",
         """
fun slidingWindowMedian(nums: IntArray, k: Int): IntArray {
    val low = java.util.PriorityQueue<Int>(compareByDescending { it })
    val high = java.util.PriorityQueue<Int>()
    val delayed = HashMap<Int, Int>()
    var lowSize = 0; var highSize = 0
    fun prune(heap: java.util.PriorityQueue<Int>) {
        while (heap.isNotEmpty()) { val top = heap.peek(); val pending = delayed[top] ?: 0; if (pending == 0) break; if (pending == 1) delayed.remove(top) else delayed[top] = pending - 1; heap.poll() }
    }
    fun balance() {
        if (lowSize > highSize + 1) { high.add(low.poll()); lowSize -= 1; highSize += 1; prune(low) }
        else if (lowSize < highSize) { low.add(high.poll()); highSize -= 1; lowSize += 1; prune(high) }
    }
    fun add(x: Int) { if (low.isEmpty() || x <= low.peek()) { low.add(x); lowSize += 1 } else { high.add(x); highSize += 1 }; balance() }
    fun remove(x: Int) {
        delayed[x] = (delayed[x] ?: 0) + 1
        if (low.isNotEmpty() && x <= low.peek()) lowSize -= 1 else highSize -= 1
        balance()
    }
    val out = IntArray(nums.size - k + 1)
    for (i in nums.indices) { add(nums[i]); if (i >= k) remove(nums[i - k]); if (i >= k - 1) out[i - k + 1] = low.peek() }
    return out
}
"""),
        ("sort-each-window", "PERFORMANCE",
         "창마다 복사해 정렬한다. O(n·k log k).",
         """
fun slidingWindowMedian(nums: IntArray, k: Int): IntArray {
    val out = IntArray(nums.size - k + 1)
    for (i in out.indices) {
        val window = nums.copyOfRange(i, i + k)
        window.sort()
        Drill.compare(i, window[k / 2])
        out[i] = window[k / 2]
    }
    return out
}
"""),
    ],
))


# --- 149. 다음 순열 (제자리) -----------------------------------------------------------------------------

def _next_permutation(nums):
    a = list(nums)
    i = len(a) - 2
    while i >= 0 and a[i] >= a[i + 1]:
        i -= 1
    if i < 0:
        a.reverse()
        return a
    j = len(a) - 1
    while a[j] <= a[i]:
        j -= 1
    a[i], a[j] = a[j], a[i]
    a[i + 1:] = reversed(a[i + 1:])
    return a


PROBLEMS.append(Problem(
    id="next-permutation",
    title="다음 순열",
    summary="""
정수 배열 `nums` 가 주어진다. 같은 원소들로 만들 수 있는 배열들을 사전순으로 늘어놓았을 때
`nums` **바로 다음** 배열을 반환한다. `nums` 가 마지막(내림차순)이면 처음(오름차순)을 반환한다.
원소는 중복될 수 있다.
""",
    notes="""
뒤에서부터 처음으로 `a[i] < a[i+1]` 인 자리 `i` 를 찾는다 — 그 뒤는 내림차순이라 더 키울 수 없다.
`i` 뒤에서 `a[i]` 보다 **큰 것 중 가장 작은**(뒤에서부터 처음으로 큰) 원소와 바꾸고, `i` 뒤를
뒤집어 오름차순으로 만든다. 중복이 있으면 등호의 방향이 답을 바꾼다 — `a[i] >= a[i+1]` 는
건너뛰고, 바꿀 상대는 `a[j] > a[i]` 여야 한다.
""",
    drill_doc="""
Drill.compare(i, j)           // 두 자리를 비교했다
Drill.swap(i, j)              // 두 자리를 바꿨다
""",
    constraints="""
- `1 <= nums.length <= 100_000`
- `0 <= nums[i] <= 100`
""",
    signature=dict(name="nextPermutation", parameters=[("nums", "INT_ARRAY")], returns="INT_ARRAY"),
    # 제자리 O(n) 이 유일하게 자연스러운 풀이라 자릿수로 지는 오답이 없다 — 성능 그룹을 두지 않는다.
    groups=standard_groups(),
    reference=_next_permutation,
    limits={"timeMillis": 2000, "memoryMb": 256, "outputBytes": 2000000},
    cases={
        "sample": [("01", [[1, 2, 3]]), ("02", [[3, 2, 1]])],
        "boundary": [
            ("01-single", [[5]]),
            ("02-two-ascending", [[1, 2]]),
            # 중복: 1,1,2 → 1,2,1 → 2,1,1 → 1,1,2.
            ("03-duplicates-middle", [[1, 2, 1]]),
            ("04-duplicates-last", [[2, 1, 1]]),
            # 바꿀 상대가 같은 값이면 안 된다.
            ("05-equal-neighbor", [[1, 3, 3, 2]]),
            ("06-all-equal", [[7, 7, 7]]),
            ("07-suffix-descending", [[1, 5, 4, 3, 2]]),
        ],
        "hidden": [
            ("01-random-small", [randoms(8, 0, 3, salt=8821)]),
            ("02-random-medium", [randoms(300, 0, 10, salt=8822)]),
            ("03-random-wide", [randoms(2000, 0, 100, salt=8823)]),
            ("04-long-descending-tail", [[1] + list(range(100, 0, -1)) * 20]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 꺾이는 자리, 뒤에서 처음 큰 것과 교환, 뒤집기.
fun nextPermutation(nums: IntArray): IntArray {
    val a = nums.copyOf()
    var i = a.size - 2
    while (i >= 0 && a[i] >= a[i + 1]) { Drill.compare(i, i + 1); i -= 1 }
    if (i >= 0) {
        var j = a.size - 1
        while (a[j] <= a[i]) { Drill.compare(i, j); j -= 1 }
        val t = a[i]; a[i] = a[j]; a[j] = t
        Drill.swap(i, j)
    }
    var lo = i + 1; var hi = a.size - 1
    while (lo < hi) { val t = a[lo]; a[lo] = a[hi]; a[hi] = t; Drill.swap(lo, hi); lo += 1; hi -= 1 }
    return a
}
""",
    mutants=[
        ("swaps-with-equal--strict-pivot", "OFF_BY_ONE",
         "꺾이는 자리를 a[i] > a[i+1] 로 찾고 바꿀 상대를 a[j] >= a[i] 로 찾는다. 중복에서 틀린다.",
         """
fun nextPermutation(nums: IntArray): IntArray {
    val a = nums.copyOf()
    var i = a.size - 2
    while (i >= 0 && a[i] > a[i + 1]) i -= 1
    if (i >= 0) {
        var j = a.size - 1
        while (a[j] < a[i]) j -= 1
        val t = a[i]; a[i] = a[j]; a[j] = t
    }
    var lo = i + 1; var hi = a.size - 1
    while (lo < hi) { val t = a[lo]; a[lo] = a[hi]; a[hi] = t; lo += 1; hi -= 1 }
    return a
}
"""),
        ("no-reverse-after-swap", "MISSING_EDGE_CASE",
         "바꾼 뒤 뒤를 뒤집지 않는다. 다음이 아니라 더 뒤의 순열이 된다.",
         """
fun nextPermutation(nums: IntArray): IntArray {
    val a = nums.copyOf()
    var i = a.size - 2
    while (i >= 0 && a[i] >= a[i + 1]) i -= 1
    if (i < 0) { a.reverse(); return a }
    var j = a.size - 1
    while (a[j] <= a[i]) j -= 1
    val t = a[i]; a[i] = a[j]; a[j] = t
    return a
}
"""),
        ("swaps-with-first-larger-from-left", "WRONG_BRANCH",
         "i 뒤에서 왼쪽부터 처음으로 큰 원소와 바꾼다. 가장 작은 큰 원소가 아니다.",
         """
fun nextPermutation(nums: IntArray): IntArray {
    val a = nums.copyOf()
    var i = a.size - 2
    while (i >= 0 && a[i] >= a[i + 1]) i -= 1
    if (i >= 0) {
        var j = i + 1
        while (a[j] <= a[i]) j += 1
        val t = a[i]; a[i] = a[j]; a[j] = t
    }
    var lo = i + 1; var hi = a.size - 1
    while (lo < hi) { val t = a[lo]; a[lo] = a[hi]; a[hi] = t; lo += 1; hi -= 1 }
    return a
}
"""),
        ("last-permutation-stays", "MISSING_EDGE_CASE",
         "내림차순(마지막)이면 그대로 돌려준다. 처음(오름차순)으로 돌아가야 한다.",
         """
fun nextPermutation(nums: IntArray): IntArray {
    val a = nums.copyOf()
    var i = a.size - 2
    while (i >= 0 && a[i] >= a[i + 1]) i -= 1
    if (i < 0) return a
    var j = a.size - 1
    while (a[j] <= a[i]) j -= 1
    val t = a[i]; a[i] = a[j]; a[j] = t
    var lo = i + 1; var hi = a.size - 1
    while (lo < hi) { val u = a[lo]; a[lo] = a[hi]; a[hi] = u; lo += 1; hi -= 1 }
    return a
}
"""),
    ],
))


# --- 153. 최댓값과 최솟값의 차가 한도 안인 가장 긴 구간 (단조 덱 둘) --------------------------------------

def _longest_within_limit(nums, limit):
    from collections import deque
    max_dq, min_dq = deque(), deque()
    left = 0
    best = 0
    for right, x in enumerate(nums):
        while max_dq and nums[max_dq[-1]] < x:
            max_dq.pop()
        max_dq.append(right)
        while min_dq and nums[min_dq[-1]] > x:
            min_dq.pop()
        min_dq.append(right)
        while nums[max_dq[0]] - nums[min_dq[0]] > limit:
            left += 1
            if max_dq[0] < left:
                max_dq.popleft()
            if min_dq[0] < left:
                min_dq.popleft()
        best = max(best, right - left + 1)
    return best


PROBLEMS.append(Problem(
    id="longest-subarray-with-limit",
    title="차가 한도 안인 가장 긴 구간",
    summary="""
정수 배열 `nums` 와 `limit` 가 주어진다. 구간 안의 **최댓값과 최솟값의 차**가 `limit` 이하인
연속 부분 배열 중 가장 긴 것의 길이를 반환한다. 길이 1 의 구간은 언제나 조건을 만족한다.
""",
    notes="""
구간마다 최댓값·최솟값을 다시 구하면 n² 이다. 창을 오른쪽으로 늘리며 **최댓값의 단조 감소 덱**과
**최솟값의 단조 증가 덱**을 유지하면 창의 최댓값·최솟값이 각 덱의 앞이고, 차가 한도를 넘는 동안
왼쪽을 줄이며 덱의 앞이 창 밖으로 나가면 뺀다. 창은 줄어들지 않으니 답은 창의 최대 길이다.
""",
    drill_doc="""
Drill.compare(left, right)    // 창을 봤다
Drill.write(0, best)          // 답을 늘렸다
""",
    constraints="""
- `1 <= nums.length <= 100_000`
- `-10^9 <= nums[i] <= 10^9`, `0 <= limit <= 2 · 10^9`
""",
    signature=dict(name="longestSubarrayWithLimit", parameters=[("nums", "INT_ARRAY"), ("limit", "INT")], returns="INT"),
    groups=perf_groups(time_multiplier=0.5),
    reference=_longest_within_limit,
    cases={
        "sample": [("01", [[8, 2, 4, 7], 4]), ("02", [[10, 1, 2, 4, 7, 2], 5])],
        "boundary": [
            ("01-single", [[5], 0]),
            ("02-limit-zero-with-run", [[3, 3, 3, 1, 1], 0]),
            ("03-whole-array", [[1, 2, 3, 4], 3]),
            # 차의 최댓값 2·10⁹ — Int 안이지만 끝이다.
            ("04-max-difference-allowed", [[1000000000, -1000000000, 1000000000], 2000000000]),
            ("05-max-difference-not-allowed", [[1000000000, -1000000000], 1999999999]),
            # 창을 줄일 때 최댓값 덱의 앞이 나간다.
            ("06-max-leaves-window", [[9, 1, 2, 3, 4], 3]),
            ("07-min-leaves-window", [[1, 9, 8, 7, 6], 3]),
            # 한 칸만 줄이면 되는데 창을 처음부터 다시 시작하면 놓친다.
            ("08-partial-shrink", [[1, 5, 3, 7, 4], 4]),
        ],
        "hidden": [
            ("01-random-small", [randoms(10, 0, 9, salt=8861), 3]),
            ("02-random-medium", [randoms(500, -100, 100, salt=8862), 20]),
            ("03-random-wide", [randoms(3000, -1000000000, 1000000000, salt=8863), 500000000]),
            ("04-sorted", [sorted(randoms(1000, 0, 10000, salt=8864)), 100]),
        ],
        "performance": [
            ("01-small", [randoms(20000, 0, 100000, salt=8871), 30000]),
            ("02-medium", [randoms(60000, 0, 100000, salt=8872), 50000]),
            ("03-large-nearly-sorted", [sorted(randoms(100000, 0, 1000000000, salt=8873)), 900000000]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 최댓값·최솟값 단조 덱과 줄어들지 않는 창.
fun longestSubarrayWithLimit(nums: IntArray, limit: Int): Int {
    val maxDq = ArrayDeque<Int>()
    val minDq = ArrayDeque<Int>()
    var left = 0
    var best = 0
    for (right in nums.indices) {
        val x = nums[right]
        while (maxDq.isNotEmpty() && nums[maxDq.last()] < x) maxDq.removeLast()
        maxDq.addLast(right)
        while (minDq.isNotEmpty() && nums[minDq.last()] > x) minDq.removeLast()
        minDq.addLast(right)
        while (nums[maxDq.first()].toLong() - nums[minDq.first()] > limit) {
            left += 1
            if (maxDq.first() < left) maxDq.removeFirst()
            if (minDq.first() < left) minDq.removeFirst()
        }
        Drill.compare(left, right)
        if (right - left + 1 > best) { best = right - left + 1; Drill.write(0, best) }
    }
    return best
}
""",
    mutants=[
        # 차는 최대 2·10⁹ 으로 Int 안이라 "Int 로 넘친다" 오답은 동치였고, 최댓값 덱에서 같은 값을 빼는
        # 것도 맞다(최근 자리가 남는다) — 둘 다 걷어 내고 실제로 틀리는 둘로.
        ("reset-window-on-violation", "WRONG_ALGORITHM",
         "한도를 넘으면 창을 지금 자리에서 새로 시작한다. 한 칸만 줄이면 되는 창을 잃는다.",
         """
fun longestSubarrayWithLimit(nums: IntArray, limit: Int): Int {
    val maxDq = ArrayDeque<Int>(); val minDq = ArrayDeque<Int>()
    var left = 0; var best = 0
    for (right in nums.indices) {
        val x = nums[right]
        while (maxDq.isNotEmpty() && nums[maxDq.last()] < x) maxDq.removeLast(); maxDq.addLast(right)
        while (minDq.isNotEmpty() && nums[minDq.last()] > x) minDq.removeLast(); minDq.addLast(right)
        if (nums[maxDq.first()].toLong() - nums[minDq.first()] > limit) { left = right; maxDq.clear(); minDq.clear(); maxDq.addLast(right); minDq.addLast(right) }
        if (right - left + 1 > best) best = right - left + 1
    }
    return best
}
"""),
        ("shrinks-before-adding", "OFF_BY_ONE",
         "새 원소를 넣기 전에 한도를 검사한다. 새 원소가 한도를 깨도 그 창을 센다.",
         """
fun longestSubarrayWithLimit(nums: IntArray, limit: Int): Int {
    val maxDq = ArrayDeque<Int>(); val minDq = ArrayDeque<Int>()
    var left = 0; var best = 0
    for (right in nums.indices) {
        while (maxDq.isNotEmpty() && minDq.isNotEmpty() && nums[maxDq.first()].toLong() - nums[minDq.first()] > limit) { left += 1; if (maxDq.first() < left) maxDq.removeFirst(); if (minDq.first() < left) minDq.removeFirst() }
        val x = nums[right]
        while (maxDq.isNotEmpty() && nums[maxDq.last()] < x) maxDq.removeLast(); maxDq.addLast(right)
        while (minDq.isNotEmpty() && nums[minDq.last()] > x) minDq.removeLast(); minDq.addLast(right)
        if (right - left + 1 > best) best = right - left + 1
    }
    return best
}
"""),
        ("never-evicts-front", "WRONG_ALGORITHM",
         "창을 줄여도 덱의 앞을 빼지 않는다. 나간 원소가 최댓값·최솟값으로 남는다.",
         """
fun longestSubarrayWithLimit(nums: IntArray, limit: Int): Int {
    val maxDq = ArrayDeque<Int>(); val minDq = ArrayDeque<Int>()
    var left = 0; var best = 0
    for (right in nums.indices) {
        val x = nums[right]
        while (maxDq.isNotEmpty() && nums[maxDq.last()] < x) maxDq.removeLast(); maxDq.addLast(right)
        while (minDq.isNotEmpty() && nums[minDq.last()] > x) minDq.removeLast(); minDq.addLast(right)
        while (nums[maxDq.first()].toLong() - nums[minDq.first()] > limit && left < right) left += 1
        if (right - left + 1 > best) best = right - left + 1
    }
    return best
}
"""),
        ("rescan-window--quadratic", "PERFORMANCE",
         "창을 늘릴 때마다 최댓값·최솟값을 다시 훑는다. O(n²).",
         """
fun longestSubarrayWithLimit(nums: IntArray, limit: Int): Int {
    var best = 0
    for (i in nums.indices) {
        var lo = nums[i]; var hi = nums[i]
        for (j in i until nums.size) {
            Drill.compare(i, j)
            if (nums[j] < lo) lo = nums[j]
            if (nums[j] > hi) hi = nums[j]
            if (hi.toLong() - lo > limit) break
            if (j - i + 1 > best) best = j - i + 1
        }
    }
    return best
}
"""),
    ],
))


# --- 163. 모든 목록을 덮는 가장 짧은 구간 (k 포인터 + 힙) ------------------------------------------------

def _smallest_range(values, sizes):
    import heapq
    lists = []
    start = 0
    for size in sizes:
        lists.append(values[start:start + size])
        start += size
    heap = [(lst[0], i, 0) for i, lst in enumerate(lists)]
    heapq.heapify(heap)
    current_max = max(lst[0] for lst in lists)
    best = (lists[0][0], lists[0][0] + 10 ** 12)
    while True:
        low, i, j = heap[0]
        if current_max - low < best[1] - best[0]:
            best = (low, current_max)
        if j + 1 == len(lists[i]):
            break
        heapq.heapreplace(heap, (lists[i][j + 1], i, j + 1))
        current_max = max(current_max, lists[i][j + 1])
    return [best[0], best[1]]


def _interleaved_lists(k, size):
    """목록 i 가 i, i+k, i+2k, … — 어느 목록도 먼저 끝나지 않아 포인터가 N 번 다 움직인다. 무작위 목록은
    값이 작은 목록이 금세 끝나 몇천 걸음에 멈춘다."""
    values = []
    for i in range(k):
        values += [i + k * j for j in range(size)]
    return values, [size] * k


def _sorted_lists(k, size, lo, hi, salt):
    values = []
    sizes = []
    for i in range(k):
        chunk = sorted(randoms(size, lo, hi, salt=salt + i))
        values += chunk
        sizes.append(size)
    return values, sizes


PROBLEMS.append(Problem(
    id="smallest-range-covering-lists",
    title="모든 목록을 덮는 가장 짧은 구간",
    summary="""
오름차순으로 정렬된 정수 목록 `k` 개가 하나의 배열 `values` 에 이어 붙어 있고, `sizes[i]` 는 `i`
번째 목록의 길이다. **모든 목록에서 하나 이상의 원소**를 담는 가장 짧은 구간 `[lo, hi]` 를 `[lo, hi]`
로 반환한다. 길이가 같으면 `lo` 가 작은 것을 고른다.
""",
    notes="""
목록마다 포인터를 하나씩 두고 **가장 작은 값을 가진 포인터를 앞으로 미는** 것이 답이다. 지금 포인터들이
가리키는 값들의 `[min, max]` 는 모든 목록을 덮는 구간이고, 그 구간을 줄이는 유일한 길은 `min` 을
키우는 것이다 — `max` 는 줄일 수 없다. `min` 은 최소 힙이 주고, `max` 는 지금까지 밀어 넣은 값의
최댓값이다. 어느 목록의 포인터가 끝에 닿으면 더 줄일 수 없다.
""",
    drill_doc="""
Drill.compare(lo, hi)         // 구간을 봤다
Drill.write(0, length)        // 답을 줄였다
""",
    constraints="""
- `1 <= k <= 10_000`, `1 <= sizes[i] <= 50`, `values.length <= 100_000`
- `-10^5 <= values[i] <= 10^5`, 목록마다 오름차순
""",
    signature=dict(name="smallestRangeCoveringLists", parameters=[("values", "INT_ARRAY"), ("sizes", "INT_ARRAY")], returns="INT_ARRAY"),
    # 매 걸음 k 개를 훑는 오답은 N·k 다. 무작위 목록은 값이 작은 목록이 금세 끝나 몇천 걸음에 멈추므로
    # 어느 목록도 먼저 끝나지 않는 엇갈린 목록(만 개 × 열 개 = 10⁹)이 있어야 진다.
    groups=perf_groups(time_multiplier=0.15),
    reference=_smallest_range,
    cases={
        "sample": [("01", [[4, 10, 15, 24, 26, 0, 9, 12, 20, 5, 18, 22, 30], [5, 4, 4]]), ("02", [[1, 2, 3, 1, 2, 3, 1, 2, 3], [3, 3, 3]])],
        "boundary": [
            ("01-single-list", [[7, 8, 9], [3]]),
            ("02-single-elements", [[5, 1, 9], [1, 1, 1]]),
            # 같은 길이면 lo 가 작은 것.
            ("03-tie-prefers-lower", [[1, 4, 2, 5], [2, 2]]),
            ("04-shared-value", [[3, 3, 3], [1, 1, 1]]),
            ("05-negatives", [[-5, -1, -3, 0, -4, 2], [2, 2, 2]]),
            # 답이 마지막 원소들에 있다 — 끝까지 밀어야 한다.
            ("06-answer-at-the-end", [[0, 100, 50, 101, 80, 102], [2, 2, 2]]),
            ("07-disjoint-lists", [[1, 2, 10, 11, 20, 21], [2, 2, 2]]),
        ],
        "hidden": [
            ("01-random-small", list(_sorted_lists(3, 4, 0, 20, salt=9041))),
            ("02-random-medium", list(_sorted_lists(20, 10, -1000, 1000, salt=9045))),
            ("03-random-wide", list(_sorted_lists(100, 30, -100000, 100000, salt=9071))),
            ("04-uneven-sizes", [[1, 5, 9, 13, 17, 21, 25, 29, 2, 30, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17], [8, 2, 15]]),
        ],
        "performance": [
            ("01-small", list(_sorted_lists(2000, 10, -100000, 100000, salt=9201))),
            ("02-medium-interleaved", list(_interleaved_lists(5000, 10))),
            ("03-large-interleaved", list(_interleaved_lists(10000, 10))),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 최소 힙이 min 을, 누적 최댓값이 max 를 준다.
fun smallestRangeCoveringLists(values: IntArray, sizes: IntArray): IntArray {
    val k = sizes.size
    val start = IntArray(k)
    for (i in 1 until k) start[i] = start[i - 1] + sizes[i - 1]
    val pointer = IntArray(k)
    val heap = java.util.PriorityQueue<Int>(compareBy { values[start[it] + pointer[it]] })
    var currentMax = Int.MIN_VALUE
    for (i in 0 until k) { heap.add(i); currentMax = maxOf(currentMax, values[start[i]]) }
    var bestLo = 0; var bestHi = 0; var bestLength = Long.MAX_VALUE
    while (true) {
        val i = heap.poll()
        val low = values[start[i] + pointer[i]]
        Drill.compare(low, currentMax)
        if (currentMax.toLong() - low < bestLength) { bestLength = currentMax.toLong() - low; bestLo = low; bestHi = currentMax; Drill.write(0, bestLength.toInt()) }
        if (pointer[i] + 1 == sizes[i]) break
        pointer[i] += 1
        currentMax = maxOf(currentMax, values[start[i] + pointer[i]])
        heap.add(i)
    }
    return intArrayOf(bestLo, bestHi)
}
""",
    mutants=[
        ("tie-takes-later", "OFF_BY_ONE",
         "같은 길이면 나중 것으로 바꾼다. lo 가 작은 것을 골라야 한다.",
         """
fun smallestRangeCoveringLists(values: IntArray, sizes: IntArray): IntArray {
    val k = sizes.size
    val start = IntArray(k)
    for (i in 1 until k) start[i] = start[i - 1] + sizes[i - 1]
    val pointer = IntArray(k)
    val heap = java.util.PriorityQueue<Int>(compareBy { values[start[it] + pointer[it]] })
    var currentMax = Int.MIN_VALUE
    for (i in 0 until k) { heap.add(i); currentMax = maxOf(currentMax, values[start[i]]) }
    var bestLo = 0; var bestHi = 0; var bestLength = Long.MAX_VALUE
    while (true) {
        val i = heap.poll()
        val low = values[start[i] + pointer[i]]
        if (currentMax.toLong() - low <= bestLength) { bestLength = currentMax.toLong() - low; bestLo = low; bestHi = currentMax }
        if (pointer[i] + 1 == sizes[i]) break
        pointer[i] += 1
        currentMax = maxOf(currentMax, values[start[i] + pointer[i]])
        heap.add(i)
    }
    return intArrayOf(bestLo, bestHi)
}
"""),
        ("stops-before-checking-last", "MISSING_EDGE_CASE",
         "포인터가 끝에 닿으면 그 상태의 구간을 보지 않고 멈춘다. 답이 끝에 있으면 놓친다.",
         """
fun smallestRangeCoveringLists(values: IntArray, sizes: IntArray): IntArray {
    val k = sizes.size
    val start = IntArray(k)
    for (i in 1 until k) start[i] = start[i - 1] + sizes[i - 1]
    val pointer = IntArray(k)
    val heap = java.util.PriorityQueue<Int>(compareBy { values[start[it] + pointer[it]] })
    var currentMax = Int.MIN_VALUE
    for (i in 0 until k) { heap.add(i); currentMax = maxOf(currentMax, values[start[i]]) }
    var bestLo = 0; var bestHi = 0; var bestLength = Long.MAX_VALUE
    while (true) {
        val i = heap.poll()
        val low = values[start[i] + pointer[i]]
        if (pointer[i] + 1 == sizes[i]) break
        if (currentMax.toLong() - low < bestLength) { bestLength = currentMax.toLong() - low; bestLo = low; bestHi = currentMax }
        pointer[i] += 1
        currentMax = maxOf(currentMax, values[start[i] + pointer[i]])
        heap.add(i)
    }
    return intArrayOf(bestLo, bestHi)
}
"""),
        ("max-not-tracked", "WRONG_BRANCH",
         "구간의 위 끝을 힙의 최댓값이 아니라 방금 밀어 넣은 값으로 둔다.",
         """
fun smallestRangeCoveringLists(values: IntArray, sizes: IntArray): IntArray {
    val k = sizes.size
    val start = IntArray(k)
    for (i in 1 until k) start[i] = start[i - 1] + sizes[i - 1]
    val pointer = IntArray(k)
    val heap = java.util.PriorityQueue<Int>(compareBy { values[start[it] + pointer[it]] })
    var currentMax = Int.MIN_VALUE
    for (i in 0 until k) { heap.add(i); currentMax = maxOf(currentMax, values[start[i]]) }
    var bestLo = 0; var bestHi = 0; var bestLength = Long.MAX_VALUE
    while (true) {
        val i = heap.poll()
        val low = values[start[i] + pointer[i]]
        if (currentMax.toLong() - low < bestLength) { bestLength = currentMax.toLong() - low; bestLo = low; bestHi = currentMax }
        if (pointer[i] + 1 == sizes[i]) break
        pointer[i] += 1
        currentMax = values[start[i] + pointer[i]]
        heap.add(i)
    }
    return intArrayOf(bestLo, bestHi)
}
"""),
        ("scan-min-each-step", "PERFORMANCE",
         "힙 대신 매 걸음 k 개 포인터를 훑어 최솟값을 찾는다. O(N·k).",
         """
fun smallestRangeCoveringLists(values: IntArray, sizes: IntArray): IntArray {
    val k = sizes.size
    val start = IntArray(k)
    for (i in 1 until k) start[i] = start[i - 1] + sizes[i - 1]
    val pointer = IntArray(k)
    var bestLo = 0; var bestHi = 0; var bestLength = Long.MAX_VALUE
    while (true) {
        var minI = 0; var low = Int.MAX_VALUE; var high = Int.MIN_VALUE
        for (i in 0 until k) {
            val v = values[start[i] + pointer[i]]
            Drill.compare(i, v)
            if (v < low) { low = v; minI = i }
            if (v > high) high = v
        }
        if (high.toLong() - low < bestLength) { bestLength = high.toLong() - low; bestLo = low; bestHi = high }
        if (pointer[minI] + 1 == sizes[minI]) break
        pointer[minI] += 1
    }
    return intArrayOf(bestLo, bestHi)
}
"""),
    ],
))


# --- 164. 제자리 런-길이 압축 ---------------------------------------------------------------------------

def _compress_in_place(chars):
    out = []
    i = 0
    while i < len(chars):
        j = i
        while j < len(chars) and chars[j] == chars[i]:
            j += 1
        out.append(chars[i])
        if j - i > 1:
            out += list(str(j - i))
        i = j
    return "".join(out)


PROBLEMS.append(Problem(
    id="run-length-compress",
    title="제자리 런-길이 압축",
    summary="""
영문 소문자 문자열 `text` 를 압축한다. 같은 글자가 연달아 `n` 번 나오면 그 글자 하나 뒤에 `n` 을
십진수로 붙이되, **한 번 나온 글자에는 숫자를 붙이지 않는다.** `"aabcccc"` → `"a2bc4"`. 압축한
문자열을 반환한다. 압축이 원문보다 길어져도 규칙대로 낸다.
""",
    notes="""
글자 배열 위에서 **읽는 자리와 쓰는 자리**를 따로 두면 추가 배열 없이 된다 — 쓰는 자리는 읽는
자리를 앞지르지 못한다(글자 하나에 최소 한 칸을 쓰고, 숫자는 글자 수보다 짧다). 연속한 구간의 길이를
세고, 글자를 쓰고, 길이가 2 이상이면 그 십진 표기를 자리마다 쓴다. 10 이상은 두 자리 이상이라
숫자를 문자열로 바꿔 한 글자씩 쓴다.
""",
    drill_doc="""
Drill.compare(read, write)    // 읽는 자리와 쓰는 자리를 봤다
Drill.write(write, code)      // 한 글자를 썼다
""",
    constraints="""
- `1 <= text.length <= 200_000`, 소문자
""",
    signature=dict(name="runLengthCompress", parameters=[("text", "STRING")], returns="STRING"),
    groups=standard_groups(),
    reference=_compress_in_place,
    limits={"timeMillis": 2000, "memoryMb": 256, "outputBytes": 400000},
    cases={
        "sample": [("01", ["aabcccc"]), ("02", ["abc"])],
        "boundary": [
            ("01-single", ["a"]),
            ("02-two-same", ["aa"]),
            # 길이 10 이상은 두 자리.
            ("03-ten", ["aaaaaaaaaa"]),
            ("04-twelve-then-one", ["aaaaaaaaaaaab"]),
            ("05-alternating", ["ababab"]),
            ("06-hundred", ["a" * 100]),
            ("07-long-then-short-runs", ["a" * 11 + "bb" + "c"]),
        ],
        "hidden": [
            ("01-random-small", ["".join("ab"[v] for v in randoms(12, 0, 1, salt=9101))]),
            ("02-random-runs", ["".join("abc"[v % 3] * (v % 7 + 1) for v in randoms(60, 0, 20, salt=9102))]),
            ("03-random-letters", ["".join(chr(97 + v) for v in randoms(500, 0, 25, salt=9103))]),
            ("04-long-runs", ["".join(chr(97 + v % 26) * (v * 7 % 1000 + 1) for v in randoms(200, 0, 25, salt=9104))]),
            ("05-max-single-run", ["z" * 200000]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 읽는 자리와 쓰는 자리 둘로 제자리.
fun runLengthCompress(text: String): String {
    val chars = text.toCharArray()
    var read = 0
    var write = 0
    while (read < chars.size) {
        val ch = chars[read]
        var end = read
        while (end < chars.size && chars[end] == ch) end += 1
        Drill.compare(read, write)
        chars[write++] = ch
        Drill.write(write - 1, ch.code)
        if (end - read > 1) for (d in (end - read).toString()) { chars[write++] = d; Drill.write(write - 1, d.code) }
        read = end
    }
    return String(chars, 0, write)
}
""",
    mutants=[
        ("writes-count-for-single", "OFF_BY_ONE",
         "한 번 나온 글자에도 1 을 붙인다.",
         """
fun runLengthCompress(text: String): String {
    val chars = text.toCharArray()
    var read = 0; var write = 0
    while (read < chars.size) {
        val ch = chars[read]; var end = read
        while (end < chars.size && chars[end] == ch) end += 1
        chars[write++] = ch
        for (d in (end - read).toString()) chars[write++] = d
        read = end
    }
    return String(chars, 0, write)
}
"""),
        ("single-digit-count", "MISSING_EDGE_CASE",
         "길이를 한 자리로만 쓴다. 10 이상이 깨진다.",
         """
fun runLengthCompress(text: String): String {
    val chars = text.toCharArray()
    var read = 0; var write = 0
    while (read < chars.size) {
        val ch = chars[read]; var end = read
        while (end < chars.size && chars[end] == ch) end += 1
        chars[write++] = ch
        if (end - read > 1) chars[write++] = ('0' + (end - read) % 10)
        read = end
    }
    return String(chars, 0, write)
}
"""),
        ("counts-total-not-run", "WRONG_ALGORITHM",
         "연속한 구간이 아니라 그 글자의 전체 개수를 붙인다.",
         """
fun runLengthCompress(text: String): String {
    val counts = IntArray(26)
    for (ch in text) counts[ch - 'a'] += 1
    val out = StringBuilder()
    var i = 0
    while (i < text.length) {
        val ch = text[i]
        var end = i
        while (end < text.length && text[end] == ch) end += 1
        out.append(ch)
        if (counts[ch - 'a'] > 1) out.append(counts[ch - 'a'])
        i = end
    }
    return out.toString()
}
"""),
        ("last-run-dropped", "OFF_BY_ONE",
         "마지막 구간을 쓰지 않는다 — 구간의 끝을 다음 글자가 바뀔 때만 감지한다.",
         """
fun runLengthCompress(text: String): String {
    val out = StringBuilder()
    var run = 1
    for (i in 1 until text.length) {
        if (text[i] == text[i - 1]) { run += 1; continue }
        out.append(text[i - 1]); if (run > 1) out.append(run)
        run = 1
    }
    return out.toString()
}
"""),
    ],
))


# --- 165. 정렬된 배열에서 두 번까지만 남기기 (제자리) ---------------------------------------------------

def _keep_at_most_twice(nums):
    out = []
    for x in nums:
        if len(out) < 2 or out[-2] != x:
            out.append(x)
    return out


PROBLEMS.append(Problem(
    id="remove-duplicates-at-most-twice",
    title="정렬된 배열에서 두 번까지만 남기기",
    summary="""
오름차순으로 정렬된 정수 배열 `nums` 에서 같은 값이 **최대 두 번**만 남도록 지운 배열을 순서
그대로 반환한다. 추가 배열 없이 제자리에서 앞으로 당겨 쓰는 것이 이 문제의 뜻이다.
""",
    notes="""
쓰는 자리 `write` 를 두고 원소를 하나씩 보며, `write < 2` 이거나 `nums[write − 2] != x` 이면 쓴다.
정렬돼 있으니 두 칸 앞의 값과 다르다는 것이 곧 "이 값이 아직 두 번 미만 나왔다"는 것이다. 바로 앞과
비교하면 한 번만 남기는 문제가 되고, 세 칸 앞과 비교하면 세 번이 남는다.
""",
    drill_doc="""
Drill.compare(read, write)    // 읽는 자리와 쓰는 자리를 봤다
Drill.write(write, value)     // 값을 당겨 썼다
""",
    constraints="""
- `1 <= nums.length <= 100_000`, 오름차순
- `-10^9 <= nums[i] <= 10^9`
""",
    signature=dict(name="removeDuplicatesAtMostTwice", parameters=[("nums", "INT_ARRAY")], returns="INT_ARRAY"),
    groups=standard_groups(),
    reference=_keep_at_most_twice,
    limits={"timeMillis": 2000, "memoryMb": 256, "outputBytes": 2000000},
    cases={
        "sample": [("01", [[1, 1, 1, 2, 2, 3]]), ("02", [[0, 0, 1, 1, 1, 1, 2, 3, 3]])],
        "boundary": [
            ("01-single", [[7]]),
            ("02-two-same", [[4, 4]]),
            ("03-three-same", [[4, 4, 4]]),
            ("04-all-distinct", [[1, 2, 3, 4]]),
            ("05-all-same-long", [[9] * 10]),
            ("06-negative-and-int-edges", [[-2147483648, -2147483648, -2147483648, 2147483647, 2147483647, 2147483647]]),
            # 두 칸 앞과 비교해야 한다 — 바로 앞과 비교하면 한 번만 남는다.
            ("07-pairs", [[1, 1, 2, 2, 3, 3]]),
        ],
        "hidden": [
            ("01-random-small", [sorted(randoms(10, 0, 3, salt=9111))]),
            ("02-random-medium", [sorted(randoms(500, 0, 50, salt=9112))]),
            ("03-random-wide", [sorted(randoms(3000, -1000000000, 1000000000, salt=9113))]),
            ("04-long-runs", [sorted(randoms(100000, 0, 20, salt=9114))]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 두 칸 앞과 비교하며 당겨 쓴다.
fun removeDuplicatesAtMostTwice(nums: IntArray): IntArray {
    val a = nums.copyOf()
    var write = 0
    for (read in a.indices) {
        Drill.compare(read, write)
        if (write < 2 || a[write - 2] != a[read]) { a[write] = a[read]; Drill.write(write, a[read]); write += 1 }
    }
    return a.copyOf(write)
}
""",
    mutants=[
        ("compares-previous--keeps-once", "OFF_BY_ONE",
         "바로 앞과 비교한다. 한 번만 남는다.",
         """
fun removeDuplicatesAtMostTwice(nums: IntArray): IntArray {
    val a = nums.copyOf()
    var write = 0
    for (read in a.indices) if (write < 1 || a[write - 1] != a[read]) { a[write] = a[read]; write += 1 }
    return a.copyOf(write)
}
"""),
        ("compares-three-back--keeps-thrice", "OFF_BY_ONE",
         "세 칸 앞과 비교한다. 세 번이 남는다.",
         """
fun removeDuplicatesAtMostTwice(nums: IntArray): IntArray {
    val a = nums.copyOf()
    var write = 0
    for (read in a.indices) if (write < 3 || a[write - 3] != a[read]) { a[write] = a[read]; write += 1 }
    return a.copyOf(write)
}
"""),
        # "원본의 두 칸 앞과 비교"는 동치였다 — 정렬된 입력에서는 원본의 두 칸 앞이 같으면 쓴 자리의 두 칸 앞도 같다.
        ("count-never-resets", "WRONG_BRANCH",
         "값이 바뀌어도 개수를 0 으로 돌리지 않는다. 처음 둘만 남는다.",
         """
fun removeDuplicatesAtMostTwice(nums: IntArray): IntArray {
    val a = nums.copyOf()
    var write = 0
    var count = 0
    for (read in a.indices) {
        count += 1
        if (count <= 2) { a[write] = a[read]; write += 1 }
    }
    return a.copyOf(write)
}
"""),
        ("counts-run-then-drops-all-extra", "WRONG_ALGORITHM",
         "세 번 이상 나온 값은 아예 지운다.",
         """
fun removeDuplicatesAtMostTwice(nums: IntArray): IntArray {
    val out = ArrayList<Int>()
    var i = 0
    while (i < nums.size) {
        var j = i
        while (j < nums.size && nums[j] == nums[i]) j += 1
        if (j - i <= 2) for (t in i until j) out.add(nums[t])
        i = j
    }
    return out.toIntArray()
}
"""),
    ],
))


# --- 167. 서로 다른 값이 정확히 k 개인 구간의 수 (창 둘의 차) --------------------------------------------

def _exactly_k_distinct(nums, k):
    def at_most(limit):
        counts = {}
        left = 0
        total = 0
        for right, x in enumerate(nums):
            counts[x] = counts.get(x, 0) + 1
            while len(counts) > limit:
                y = nums[left]
                counts[y] -= 1
                if counts[y] == 0:
                    del counts[y]
                left += 1
            total += right - left + 1
        return total
    return at_most(k) - at_most(k - 1)


PROBLEMS.append(Problem(
    id="subarrays-with-k-distinct",
    title="서로 다른 값이 정확히 k 개인 구간의 수",
    summary="""
정수 배열 `nums` 와 `k` 가 주어진다. 서로 다른 값이 **정확히** `k` 개인 연속 부분 배열의 수를 반환한다.
""",
    notes="""
"정확히 k" 는 창으로 바로 세기 어렵다 — 오른쪽을 늘리면 구간이 조건을 벗어났다가 다시 들어오기 때문이다.
"**k 개 이하**" 는 창으로 센다: 오른쪽마다 종류가 `k` 를 넘지 않을 때까지 왼쪽을 줄이면 그 창의 모든
끝이 답이라 `right − left + 1` 을 더한다. 정확히 `k` 는 `이하 k − 이하 k−1` 이다. 답은 `Int` 를 넘지
않지만 n(n+1)/2 는 조심해서 본다 — n ≤ 2·10⁴ 면 2·10⁸ 이다.
""",
    drill_doc="""
Drill.compare(left, right)    // 창을 봤다
Drill.write(0, total)         // 세었다
""",
    constraints="""
- `1 <= nums.length <= 60_000` (답은 `Int` 안이다)
- `1 <= nums[i] <= nums.length`, `1 <= k <= nums.length`
""",
    signature=dict(name="subarraysWithKDistinct", parameters=[("nums", "INT_ARRAY"), ("k", "INT")], returns="INT"),
    # n² 오답이 6 만에서 2.1배였다 — 답이 Int 안이어야 해서 n 을 못 키우니 한도를 조인다.
    groups=perf_groups(time_multiplier=0.25),
    reference=_exactly_k_distinct,
    cases={
        "sample": [("01", [[1, 2, 1, 2, 3], 2]), ("02", [[1, 2, 1, 3, 4], 3])],
        "boundary": [
            ("01-single", [[1], 1]),
            ("02-all-same-k1", [[2, 2, 2, 2], 1]),
            ("03-all-distinct-k1", [[1, 2, 3, 4], 1]),
            ("04-k-larger-than-kinds", [[1, 2, 1], 3]),
            # 창이 늘었다 줄었다 하며 정확히 k 를 오간다.
            ("05-oscillating", [[1, 2, 1, 2, 1, 2], 2]),
            ("06-k-equals-length", [[1, 2, 3], 3]),
        ],
        "hidden": [
            ("01-random-small", [randoms(12, 1, 4, salt=9301), 2]),
            ("02-random-medium", [randoms(500, 1, 10, salt=9302), 4]),
            ("03-random-wide", [randoms(3000, 1, 3000, salt=9303), 50]),
            ("04-few-kinds", [randoms(2000, 1, 3, salt=9304), 3]),
        ],
        "performance": [
            # 종류가 k 를 넘지 않는 입력이어야 n² 오답의 안쪽 반복이 끊기지 않는다.
            ("01-small", [randoms(10000, 1, 10, salt=9311), 10]),
            ("02-medium", [randoms(30000, 1, 10, salt=9312), 10]),
            ("03-large", [randoms(60000, 1, 10, salt=9313), 10]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 이하 k − 이하 k−1.
fun subarraysWithKDistinct(nums: IntArray, k: Int): Int {
    fun atMost(limit: Int): Long {
        val counts = IntArray(nums.size + 1)
        var kinds = 0; var left = 0; var total = 0L
        for (right in nums.indices) {
            if (counts[nums[right]]++ == 0) kinds += 1
            while (kinds > limit) { if (--counts[nums[left]] == 0) kinds -= 1; left += 1 }
            Drill.compare(left, right)
            total += right - left + 1
        }
        return total
    }
    val answer = atMost(k) - atMost(k - 1)
    Drill.write(0, answer.toInt())
    return answer.toInt()
}
""",
    mutants=[
        ("counts-at-most-k", "WRONG_ALGORITHM",
         "k 개 이하인 구간을 센다. 정확히 k 가 아니다.",
         """
fun subarraysWithKDistinct(nums: IntArray, k: Int): Int {
    val counts = IntArray(nums.size + 1)
    var kinds = 0; var left = 0; var total = 0L
    for (right in nums.indices) {
        if (counts[nums[right]]++ == 0) kinds += 1
        while (kinds > k) { if (--counts[nums[left]] == 0) kinds -= 1; left += 1 }
        total += right - left + 1
    }
    return total.toInt()
}
"""),
        ("adds-one-per-window", "OFF_BY_ONE",
         "창마다 하나만 센다. 창 안의 모든 끝이 답이다.",
         """
fun subarraysWithKDistinct(nums: IntArray, k: Int): Int {
    fun atMost(limit: Int): Long {
        val counts = IntArray(nums.size + 1)
        var kinds = 0; var left = 0; var total = 0L
        for (right in nums.indices) {
            if (counts[nums[right]]++ == 0) kinds += 1
            while (kinds > limit) { if (--counts[nums[left]] == 0) kinds -= 1; left += 1 }
            if (right >= left) total += 1
        }
        return total
    }
    return (atMost(k) - atMost(k - 1)).toInt()
}
"""),
        ("kinds-not-decremented", "WRONG_BRANCH",
         "왼쪽을 줄일 때 종류 수를 줄이지 않는다. 창이 다시 늘지 못한다.",
         """
fun subarraysWithKDistinct(nums: IntArray, k: Int): Int {
    fun atMost(limit: Int): Long {
        val counts = IntArray(nums.size + 1)
        var kinds = 0; var left = 0; var total = 0L
        for (right in nums.indices) {
            if (counts[nums[right]]++ == 0) kinds += 1
            while (kinds > limit && left <= right) { counts[nums[left]] -= 1; left += 1 }
            total += right - left + 1
        }
        return total
    }
    return (atMost(k) - atMost(k - 1)).toInt()
}
"""),
        ("all-subarrays--quadratic", "PERFORMANCE",
         "모든 구간의 종류를 센다. O(n²).",
         """
fun subarraysWithKDistinct(nums: IntArray, k: Int): Int {
    var total = 0
    val counts = IntArray(nums.size + 1)
    for (i in nums.indices) {
        counts.fill(0)
        var kinds = 0
        for (j in i until nums.size) {
            Drill.compare(i, j)
            if (counts[nums[j]]++ == 0) kinds += 1
            if (kinds > k) break
            if (kinds == k) total += 1
        }
    }
    return total
}
"""),
    ],
))


# --- 168. 곱이 k 미만인 구간의 수 (창, Long) ----------------------------------------------------------------

def _product_less_than_k(nums, k):
    if k <= 1:
        return 0
    product = 1
    left = 0
    total = 0
    for right, x in enumerate(nums):
        product *= x
        while product >= k:
            product //= nums[left]
            left += 1
        total += right - left + 1
    return total


PROBLEMS.append(Problem(
    id="subarray-product-less-than-k",
    title="곱이 k 미만인 구간의 수",
    summary="""
양의 정수 배열 `nums` 와 `k` 가 주어진다. 원소의 곱이 `k` **미만**인 연속 부분 배열의 수를 반환한다.
""",
    notes="""
값이 전부 양수라 곱은 오른쪽을 늘리면 커지고 왼쪽을 줄이면 작아진다 — 창이 된다. 오른쪽마다 곱이
`k` 이상인 동안 왼쪽을 줄이고 `right − left + 1` 을 더한다. 곱은 창 안에서만 유지되어 `k · 최댓값`
을 넘지 않지만 그것이 이미 `Int` 를 넘는다 — `Long` 이다. `k ≤ 1` 이면 답은 0 이다(곱은 1 이상).
""",
    drill_doc="""
Drill.compare(left, right)    // 창을 봤다
Drill.write(0, total)         // 세었다
""",
    constraints="""
- `1 <= nums.length <= 60_000`, `1 <= nums[i] <= 1000` (답은 `Int` 안이다: n(n+1)/2 < 2³¹)
- `0 <= k <= 10^9`
""",
    signature=dict(name="subarrayProductLessThanK", parameters=[("nums", "INT_ARRAY"), ("k", "INT")], returns="INT"),
    # n² 오답이 6 만에서 1.8배였다 — 답이 Int 안이어야 해서 n 을 못 키우니 한도를 조인다.
    groups=perf_groups(time_multiplier=0.25),
    reference=_product_less_than_k,
    cases={
        "sample": [("01", [[10, 5, 2, 6], 100]), ("02", [[1, 2, 3], 0])],
        "boundary": [
            ("01-k-one", [[1, 1, 1], 1]),
            ("02-k-two-all-ones", [[1, 1, 1], 2]),
            ("03-single-too-big", [[1000], 1000]),
            # 곱이 Int 를 넘는 순간이 창 안에서 온다: 1000 × 1000 × 1000 × 1000.
            ("04-product-exceeds-int", [[1000, 1000, 1000, 1000], 1000000000]),
            ("05-exact-boundary", [[2, 5], 10]),
            ("06-ones-everywhere", [[1, 1, 5, 1, 1], 5]),
        ],
        "hidden": [
            ("01-random-small", [randoms(10, 1, 5, salt=9321), 20]),
            ("02-random-medium", [randoms(500, 1, 10, salt=9322), 1000]),
            ("03-random-wide", [randoms(3000, 1, 1000, salt=9323), 1000000000]),
            ("04-many-ones", [randoms(2000, 1, 2, salt=9324), 8]),
        ],
        "performance": [
            ("01-small", [randoms(20000, 1, 3, salt=9331), 1000000000]),
            ("02-medium", [randoms(40000, 1, 2, salt=9332), 1000000000]),
            ("03-large", [[1] * 60000, 1000000000]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 곱을 Long 으로 유지하는 창.
fun subarrayProductLessThanK(nums: IntArray, k: Int): Int {
    if (k <= 1) return 0
    var product = 1L; var left = 0; var total = 0L
    for (right in nums.indices) {
        product *= nums[right]
        while (product >= k) { product /= nums[left]; left += 1 }
        Drill.compare(left, right)
        total += right - left + 1
        Drill.write(0, total.toInt())
    }
    return total.toInt()
}
""",
    mutants=[
        ("int-product--overflows", "WRONG_BRANCH",
         "곱을 Int 로 둔다. 1000⁴ 에서 넘친다.",
         """
fun subarrayProductLessThanK(nums: IntArray, k: Int): Int {
    if (k <= 1) return 0
    var product = 1; var left = 0; var total = 0L
    for (right in nums.indices) {
        product *= nums[right]
        while (product >= k) { product /= nums[left]; left += 1 }
        total += right - left + 1
    }
    return total.toInt()
}
"""),
        ("less-or-equal", "OFF_BY_ONE",
         "곱이 k 이하인 구간을 센다. 미만이어야 한다.",
         """
fun subarrayProductLessThanK(nums: IntArray, k: Int): Int {
    if (k < 1) return 0
    var product = 1L; var left = 0; var total = 0L
    for (right in nums.indices) {
        product *= nums[right]
        while (product > k) { product /= nums[left]; left += 1 }
        total += right - left + 1
    }
    return total.toInt()
}
"""),
        # "k ≤ 1 을 따로 보지 않는다"는 왼쪽이 오른쪽을 넘지 않게만 막으면 0 이 나와 동치였다.
        ("window-length-off-by-one", "OFF_BY_ONE",
         "창의 길이를 right − left 로 센다. 원소 하나짜리 구간이 빠진다.",
         """
fun subarrayProductLessThanK(nums: IntArray, k: Int): Int {
    if (k <= 1) return 0
    var product = 1L; var left = 0; var total = 0L
    for (right in nums.indices) {
        product *= nums[right]
        while (product >= k) { product /= nums[left]; left += 1 }
        total += right - left
    }
    return total.toInt()
}
"""),
        ("all-subarrays--quadratic", "PERFORMANCE",
         "모든 구간의 곱을 본다. O(n²) — 1 이 많으면 끊기지도 않는다.",
         """
fun subarrayProductLessThanK(nums: IntArray, k: Int): Int {
    var total = 0
    for (i in nums.indices) {
        var product = 1L
        for (j in i until nums.size) {
            Drill.compare(i, j)
            product *= nums[j]
            if (product >= k) break
            total += 1
        }
    }
    return total
}
"""),
    ],
))


# --- 174. 균형점 (왼쪽 합 = 오른쪽 합) ---------------------------------------------------------------------

def _pivot_index(nums):
    total = sum(nums)
    left = 0
    for i, x in enumerate(nums):
        if left == total - left - x:
            return i
        left += x
    return -1


PROBLEMS.append(Problem(
    id="pivot-index",
    title="균형점",
    summary="""
정수 배열 `nums` 에서 **왼쪽 원소들의 합과 오른쪽 원소들의 합이 같은** 가장 왼쪽 자리를 반환한다.
자기 자신은 어느 쪽에도 들지 않는다. 맨 왼쪽 자리의 왼쪽 합과 맨 오른쪽 자리의 오른쪽 합은 `0` 이다.
없으면 `-1`.
""",
    notes="""
자리마다 양쪽을 더하면 n² 이다. 전체 합을 한 번 구해 두면 자리 `i` 의 오른쪽 합은 `전체 − 왼쪽 − nums[i]`
라 왼쪽 합만 누적하며 한 번에 본다. 합은 `Int` 안이지만(원소 1000 × 10 만) 음수가 섞여 있으니 "합이 0
이면 없다" 같은 지름길은 없다.
""",
    drill_doc="""
Drill.compare(i, left)        // 자리의 왼쪽 합을 봤다
Drill.write(0, index)         // 답을 정했다
""",
    constraints="""
- `1 <= nums.length <= 100_000`
- `-1000 <= nums[i] <= 1000`
""",
    signature=dict(name="pivotIndex", parameters=[("nums", "INT_ARRAY")], returns="INT"),
    groups=standard_groups(),
    reference=_pivot_index,
    cases={
        "sample": [("01", [[1, 7, 3, 6, 5, 6]]), ("02", [[1, 2, 3]])],
        "boundary": [
            ("01-single", [[5]]),
            # 맨 왼쪽: 왼쪽 합 0, 오른쪽 합 0.
            ("02-leftmost", [[2, -1, 1]]),
            ("03-rightmost", [[1, -1, 2]]),
            ("04-all-zero", [[0, 0, 0]]),
            ("05-negatives", [[-1, -1, -1, -1, -1, 0]]),
            # 둘 이상이면 가장 왼쪽.
            ("06-multiple", [[0, 0, 0, 0]]),
            ("07-none", [[1, 1]]),
        ],
        "hidden": [
            ("01-random-small", [randoms(10, -5, 5, salt=9421)]),
            ("02-random-medium", [randoms(300, -50, 50, salt=9422)]),
            ("03-random-wide", [randoms(5000, -1000, 1000, salt=9423)]),
            ("04-pivot-at-end", [randoms(999, -1000, 1000, salt=9424) + [10**9]]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 전체 합에서 왼쪽 합을 빼며 한 번.
fun pivotIndex(nums: IntArray): Int {
    var total = 0L
    for (x in nums) total += x
    var left = 0L
    for (i in nums.indices) {
        Drill.compare(i, left.toInt())
        if (left == total - left - nums[i]) { Drill.write(0, i); return i }
        left += nums[i]
    }
    return -1
}
""",
    mutants=[
        ("includes-self-on-left", "OFF_BY_ONE",
         "자기 자신을 왼쪽 합에 넣고 비교한다.",
         """
fun pivotIndex(nums: IntArray): Int {
    var total = 0L
    for (x in nums) total += x
    var left = 0L
    for (i in nums.indices) {
        left += nums[i]
        if (left == total - left) return i
    }
    return -1
}
"""),
        ("skips-first-index", "MISSING_EDGE_CASE",
         "맨 왼쪽 자리는 보지 않는다. 왼쪽 합 0 도 합이다.",
         """
fun pivotIndex(nums: IntArray): Int {
    var total = 0L
    for (x in nums) total += x
    var left = nums[0].toLong()
    for (i in 1 until nums.size) {
        if (left == total - left - nums[i]) return i
        left += nums[i]
    }
    return -1
}
"""),
        ("returns-last-match", "WRONG_BRANCH",
         "가장 오른쪽 균형점을 돌려준다.",
         """
fun pivotIndex(nums: IntArray): Int {
    var total = 0L
    for (x in nums) total += x
    var left = 0L
    var found = -1
    for (i in nums.indices) {
        if (left == total - left - nums[i]) found = i
        left += nums[i]
    }
    return found
}
"""),
        ("zero-total-shortcut", "WRONG_ALGORITHM",
         "전체 합이 0 이면 -1 이라고 지름길을 둔다. 음수가 있으면 틀린다.",
         """
fun pivotIndex(nums: IntArray): Int {
    var total = 0L
    for (x in nums) total += x
    if (total == 0L && nums.size > 1) return -1
    var left = 0L
    for (i in nums.indices) {
        if (left == total - left - nums[i]) return i
        left += nums[i]
    }
    return -1
}
"""),
    ],
))


# --- 175. 구간 더하기 뒤의 배열 (차분 배열) ------------------------------------------------------------------

def _apply_range_updates(n, updates):
    diff = [0] * (n + 1)
    for i in range(0, len(updates), 3):
        l, r, v = updates[i], updates[i + 1], updates[i + 2]
        diff[l] += v
        diff[r + 1] -= v
    out = []
    running = 0
    for i in range(n):
        running += diff[i]
        out.append(running)
    return out


def _full_span_updates(n, m, salt):
    """전 구간을 덮는 갱신 — 갱신마다 구간을 훑는 오답이 n × 갱신 수를 그대로 다 돈다.
    무작위 구간은 평균 길이가 n/3 이라 CI 머신에서 한도의 2.3배밖에 못 넘겼다."""
    v = randoms(m, -1000, 1000, salt=salt)
    return flat([0, n - 1, v[i]] for i in range(m))


def _updates(n, m, salt):
    a = randoms(m, 0, n - 1, salt=salt)
    b = randoms(m, 0, n - 1, salt=salt + 1)
    v = randoms(m, -1000, 1000, salt=salt + 2)
    return flat([min(a[i], b[i]), max(a[i], b[i]), v[i]] for i in range(m))


PROBLEMS.append(Problem(
    id="apply-range-updates",
    # v2: 전 구간을 덮는 성능 케이스를 더했다. 무작위 구간은 평균 길이가 n/3 이라 갱신마다
    # 훑는 오답이 CI 머신에서 한도의 2.3배에 그쳤다 (§12.1 재현성).
    version=2,
    title="구간 더하기 뒤의 배열",
    summary="""
길이 `n` 의 0 배열에 갱신 `updates = [l1, r1, v1, l2, r2, v2, ...]` 를 차례로 적용한다 — 각각 `l..r`
(양 끝 포함)의 모든 원소에 `v` 를 더한다. 끝난 뒤의 배열을 반환한다.
""",
    notes="""
갱신마다 구간을 훑으면 `n × 갱신 수` 다. **차분 배열**은 갱신 하나를 두 칸으로 바꾼다 — `diff[l] += v`,
`diff[r+1] −= v`. 끝나면 차분의 누적합이 배열이다. `r + 1` 이 배열 끝을 넘을 수 있으니 차분은 한 칸
길다. 합은 `Int` 안이다(1000 × 10 만).
""",
    drill_doc="""
Drill.write(l, v)             // 차분에 적었다
Drill.compare(i, running)     // 누적합을 냈다
""",
    constraints="""
- `1 <= n <= 100_000`, 갱신 `0..100_000` 개
- `0 <= l <= r < n`, `-1000 <= v <= 1000`
""",
    signature=dict(name="applyRangeUpdates", parameters=[("n", "INT"), ("updates", "INT_ARRAY")], returns="INT_ARRAY"),
    groups=perf_groups(time_multiplier=0.5),
    reference=_apply_range_updates,
    # 출력 한도는 그룹의 케이스들이 나눠 쓴다 — 십만 칸짜리 성능 케이스가 넷이면 2MB 로는 모자란다.
    limits={"timeMillis": 2000, "memoryMb": 256, "outputBytes": 8000000},
    cases={
        "sample": [("01", [5, [1, 3, 2, 2, 4, 3, 0, 2, -2]]), ("02", [3, []])],
        "boundary": [
            ("01-single-cell", [1, [0, 0, 7]]),
            # r 이 마지막 자리 — 차분의 r+1 이 배열 밖이다.
            ("02-to-the-end", [4, [2, 3, 5]]),
            ("03-whole-range", [4, [0, 3, 1, 0, 3, 1]]),
            ("04-overlapping", [6, [0, 2, 1, 1, 4, 10, 2, 5, 100]]),
            ("05-negative-cancels", [3, [0, 2, 5, 0, 2, -5]]),
            ("06-point-updates", [5, [1, 1, 1, 3, 3, 3]]),
        ],
        "hidden": [
            ("01-random-small", [8, _updates(8, 5, salt=9431)]),
            ("02-random-medium", [300, _updates(300, 200, salt=9434)]),
            ("03-random-wide", [5000, _updates(5000, 3000, salt=9437)]),
            ("04-many-on-few", [10, _updates(10, 500, salt=9440)]),
        ],
        "performance": [
            ("01-small", [20000, _updates(20000, 20000, salt=9451)]),
            ("02-medium", [60000, _updates(60000, 60000, salt=9454)]),
            ("03-large", [100000, _updates(100000, 100000, salt=9457)]),
            ("04-full-span", [100000, _full_span_updates(100000, 100000, salt=9460)]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 차분 두 칸, 누적합 한 번.
fun applyRangeUpdates(n: Int, updates: IntArray): IntArray {
    val diff = IntArray(n + 1)
    for (i in updates.indices step 3) {
        val l = updates[i]; val r = updates[i + 1]; val v = updates[i + 2]
        diff[l] += v; diff[r + 1] -= v
        Drill.write(l, v)
    }
    val out = IntArray(n)
    var running = 0
    for (i in 0 until n) { running += diff[i]; out[i] = running; Drill.compare(i, running) }
    return out
}
""",
    mutants=[
        ("subtracts-at-r-not-r-plus-one", "OFF_BY_ONE",
         "차분을 r 에서 뺀다. 구간의 마지막 원소가 빠진다.",
         """
fun applyRangeUpdates(n: Int, updates: IntArray): IntArray {
    val diff = IntArray(n + 1)
    for (i in updates.indices step 3) { diff[updates[i]] += updates[i + 2]; diff[updates[i + 1]] -= updates[i + 2] }
    val out = IntArray(n); var running = 0
    for (i in 0 until n) { running += diff[i]; out[i] = running }
    return out
}
"""),
        ("diff-array-too-short", "MISSING_EDGE_CASE",
         "차분 배열을 n 칸으로 둔다. r 이 마지막이면 배열 밖을 쓴다.",
         """
fun applyRangeUpdates(n: Int, updates: IntArray): IntArray {
    val diff = IntArray(n)
    for (i in updates.indices step 3) { diff[updates[i]] += updates[i + 2]; diff[updates[i + 1] + 1] -= updates[i + 2] }
    val out = IntArray(n); var running = 0
    for (i in 0 until n) { running += diff[i]; out[i] = running }
    return out
}
"""),
        ("forgets-prefix-sum", "WRONG_ALGORITHM",
         "차분을 그대로 돌려준다. 누적합을 안 한다.",
         """
fun applyRangeUpdates(n: Int, updates: IntArray): IntArray {
    val diff = IntArray(n + 1)
    for (i in updates.indices step 3) { diff[updates[i]] += updates[i + 2]; diff[updates[i + 1] + 1] -= updates[i + 2] }
    return diff.copyOf(n)
}
"""),
        ("loops-each-range", "PERFORMANCE",
         "갱신마다 구간을 훑는다. O(n × 갱신 수).",
         """
fun applyRangeUpdates(n: Int, updates: IntArray): IntArray {
    val out = IntArray(n)
    for (i in updates.indices step 3) {
        for (j in updates[i]..updates[i + 1]) { Drill.compare(j, i); out[j] += updates[i + 2] }
    }
    return out
}
"""),
    ],
))


# --- 186. 빠진 첫 양의 정수 (제자리 자리 맞추기) ---------------------------------------------------

def _first_missing_positive(nums):
    seen = set(nums)
    n = len(nums)
    for candidate in range(1, n + 2):
        if candidate not in seen:
            return candidate
    return n + 1


PROBLEMS.append(Problem(
    id="first-missing-positive",
    title="빠진 첫 양의 정수",
    summary="""
정수 배열 `nums` 에서 **나타나지 않는 가장 작은 양의 정수**를 반환한다. 음수와 0 과 중복이 섞여 있을 수
있고, 정렬돼 있지 않다.
""",
    notes="""
길이가 `n` 이면 답은 반드시 `1..n+1` 안에 있다 — 그 밖의 값은 답을 밀어낼 수 없다. 그래서 **범위 밖의 값은
볼 필요가 없고**, 볼 값은 `n` 개뿐이다.

배열 자신을 표로 쓴다. 값 `v` 가 `1..n` 이면 `v` 를 `v-1` 번 자리로 보내는 맞바꾸기를 자리마다 반복한다.
보낼 자리에 이미 같은 값이 있으면 멈춘다 — 안 그러면 중복에서 영원히 맞바꾼다. 다 맞춘 뒤 `i+1` 이 아닌
첫 자리가 답이고, 없으면 `n+1` 이다.
""",
    drill_doc="""
Drill.swap(i, j)              // 값을 제자리로 보냈다
Drill.visit(i, value)         // 자리와 값이 맞는지 확인했다
""",
    constraints="""
- `1 <= nums.size <= 200_000`, `-1_000_000_000 <= nums[i] <= 1_000_000_000`
""",
    signature=dict(name="firstMissingPositive", parameters=[("nums", "INT_ARRAY")], returns="INT"),
    # 후보마다 배열을 훑는 오답의 안쪽 반복은 정수 비교뿐이라 JIT 가 벡터화한다 — 이십만 개
    # 순열에서도 한도의 1.8배에 그쳤다. 정답이 한도의 2% 를 쓰므로 성능 그룹의 시계를 조인다.
    groups=perf_groups(time_multiplier=0.25),
    reference=_first_missing_positive,
    limits={"timeMillis": 2000, "memoryMb": 256, "outputBytes": 65536},
    cases={
        "sample": [("01", [[3, 4, -1, 1]]), ("02", [[1, 2, 0]])],
        "boundary": [
            ("01-single-one", [[1]]),
            ("02-single-other", [[2]]),
            # 1..n 이 꽉 차 있으면 답은 n+1 이다.
            ("03-full", [[1, 2, 3, 4]]),
            ("04-duplicates", [[1, 1, 2, 2]]),
            ("05-all-negative", [[-3, -1, -7]]),
            ("06-out-of-range-values", [[1000000000, -1000000000, 2]]),
            ("07-reverse-sorted", [[5, 4, 3, 2, 1]]),
            ("08-zeros", [[0, 0, 0]]),
        ],
        "hidden": [
            ("01-random-small", [randoms(20, -5, 25, salt=9761)]),
            ("02-random-medium", [randoms(2000, -1000, 3000, salt=9763)]),
            ("03-dense-permutation", [shuffled(range(1, 3001), salt=9765)]),
            ("04-permutation-with-hole", [shuffled([v for v in range(1, 3001) if v != 1500], salt=9767) + [7000]]),
            ("05-wide-values", [randoms(3000, -1000000000, 1000000000, salt=9769)]),
        ],
        "performance": [
            ("01-permutation", [shuffled(range(1, 100001), salt=9771)]),
            # 1..n 이 꽉 차 있어 답이 마지막에야 나온다 — 후보마다 배열을 훑는 풀이가 안 끊긴다.
            ("02-full-large", [shuffled(range(1, 200001), salt=9773)]),
            ("03-random-large", [randoms(200000, -1000000000, 1000000000, salt=9775)]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 값 v 를 v-1 번 자리로 보내고, 자리와 값이 어긋난 첫 곳을 찾는다.
fun firstMissingPositive(nums: IntArray): Int {
    val n = nums.size
    for (i in 0 until n) {
        while (nums[i] in 1..n && nums[nums[i] - 1] != nums[i]) {
            val target = nums[i] - 1
            val temp = nums[target]
            nums[target] = nums[i]
            nums[i] = temp
            Drill.swap(i, target)
        }
    }
    for (i in 0 until n) {
        Drill.visit(i, nums[i])
        if (nums[i] != i + 1) return i + 1
    }
    return n + 1
}
""",
    mutants=[
        ("no-duplicate-guard", "MISSING_EDGE_CASE",
         "보낼 자리에 같은 값이 이미 있는지 보지 않는다. 중복이 있으면 두 자리를 영원히 맞바꾼다.",
         """
fun firstMissingPositive(nums: IntArray): Int {
    val n = nums.size
    for (i in 0 until n) {
        while (nums[i] in 1..n && nums[i] != i + 1) {
            val target = nums[i] - 1
            val temp = nums[target]
            nums[target] = nums[i]
            nums[i] = temp
        }
    }
    for (i in 0 until n) if (nums[i] != i + 1) return i + 1
    return n + 1
}
"""),
        ("starts-at-zero", "OFF_BY_ONE",
         "0 부터 세어 자리와 값을 맞춘다. 양의 정수만 세는 문제다.",
         """
fun firstMissingPositive(nums: IntArray): Int {
    val n = nums.size
    for (i in 0 until n) {
        while (nums[i] in 0 until n && nums[nums[i]] != nums[i]) {
            val target = nums[i]
            val temp = nums[target]
            nums[target] = nums[i]
            nums[i] = temp
        }
    }
    for (i in 0 until n) if (nums[i] != i) return i
    return n
}
"""),
        ("no-full-array-answer", "WRONG_BRANCH",
         "자리가 다 맞았을 때 n+1 대신 n 을 돌려준다. 1..n 이 꽉 찬 입력에서 어긋난다.",
         """
fun firstMissingPositive(nums: IntArray): Int {
    val n = nums.size
    for (i in 0 until n) {
        while (nums[i] in 1..n && nums[nums[i] - 1] != nums[i]) {
            val target = nums[i] - 1
            val temp = nums[target]
            nums[target] = nums[i]
            nums[i] = temp
        }
    }
    for (i in 0 until n) if (nums[i] != i + 1) return i + 1
    return n
}
"""),
        ("candidate-scans-array", "PERFORMANCE",
         "1 부터 후보를 올리며 후보마다 배열 전체를 훑는다. O(n^2).",
         """
fun firstMissingPositive(nums: IntArray): Int {
    var candidate = 1
    while (true) {
        var found = false
        for (v in nums) { Drill.compare(candidate, v); if (v == candidate) { found = true; break } }
        if (!found) return candidate
        candidate += 1
    }
}
"""),
    ],
))


# --- 192. 연속한 1 의 최대 길이 -------------------------------------------------------------------

def _max_consecutive_ones(nums):
    best = 0
    run = 0
    for value in nums:
        if value == 1:
            run += 1
            if run > best:
                best = run
        else:
            run = 0
    return best


PROBLEMS.append(Problem(
    id="max-consecutive-ones",
    title="연속한 1 의 최대 길이",
    summary="""
`0` 과 `1` 로만 된 배열 `nums` 에서 **연속한 `1` 이 가장 길게 이어지는 길이**를 반환한다. `1` 이 하나도
없으면 `0` 이다.
""",
    notes="""
한 번 훑으며 "지금 이어지는 길이"와 "지금까지의 최댓값" 둘만 들고 간다. `1` 을 만나면 이어지는 길이를 올리고
최댓값을 갱신하고, `0` 을 만나면 이어지는 길이를 `0` 으로 되돌린다.

**끝에서 한 번 더 갱신해야 하는가**를 확인한다 — 갱신을 `0` 을 만났을 때만 하면 배열이 `1` 로 끝날 때 마지막
구간이 빠진다.
""",
    drill_doc="""
Drill.visit(index, value)     // 칸을 봤다
Drill.write(0, best)          // 최댓값을 갱신했다
""",
    constraints="""
- `1 <= nums.size <= 200_000`, `nums[i]` 는 `0` 또는 `1`
""",
    signature=dict(name="maxConsecutiveOnes", parameters=[("nums", "INT_ARRAY")], returns="INT"),
    groups=standard_groups(),
    reference=_max_consecutive_ones,
    limits={"timeMillis": 2000, "memoryMb": 256, "outputBytes": 65536},
    cases={
        "sample": [("01", [[1, 1, 0, 1, 1, 1]]), ("02", [[1, 0, 1, 1, 0, 1]])],
        "boundary": [
            ("01-single-one", [[1]]),
            ("02-single-zero", [[0]]),
            ("03-all-ones", [[1, 1, 1, 1]]),
            ("04-all-zeros", [[0, 0, 0]]),
            # 가장 긴 구간이 맨 끝에서 끝난다.
            ("05-longest-at-end", [[1, 0, 1, 1, 1]]),
            ("06-longest-at-start", [[1, 1, 1, 0, 1]]),
            ("07-alternating", [[1, 0, 1, 0, 1]]),
        ],
        "hidden": [
            ("01-random-small", [randoms(20, 0, 1, salt=9911)]),
            ("02-random-medium", [randoms(500, 0, 1, salt=9913)]),
            ("03-mostly-ones", [[1 if v > 0 else 0 for v in randoms(500, 0, 9, salt=9915)]]),
            ("04-mostly-zeros", [[1 if v == 0 else 0 for v in randoms(500, 0, 9, salt=9917)]]),
            ("05-long-run-at-end", [[0] * 300 + [1] * 200]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 이어지는 길이와 최댓값 둘만 들고 한 번 훑는다.
fun maxConsecutiveOnes(nums: IntArray): Int {
    var best = 0
    var run = 0
    for (i in nums.indices) {
        Drill.visit(i, nums[i])
        if (nums[i] == 1) {
            run += 1
            if (run > best) { best = run; Drill.write(0, best) }
        } else {
            run = 0
        }
    }
    return best
}
""",
    mutants=[
        ("updates-only-on-zero", "MISSING_EDGE_CASE",
         "0 을 만났을 때만 최댓값을 갱신한다. 1 로 끝나는 배열의 마지막 구간이 빠진다.",
         """
fun maxConsecutiveOnes(nums: IntArray): Int {
    var best = 0
    var run = 0
    for (value in nums) {
        if (value == 1) run += 1
        else { if (run > best) best = run; run = 0 }
    }
    return best
}
"""),
        ("never-resets", "WRONG_ALGORITHM",
         "0 을 만나도 이어지는 길이를 되돌리지 않는다. 1 의 전체 개수를 센다.",
         """
fun maxConsecutiveOnes(nums: IntArray): Int {
    var best = 0
    var run = 0
    for (value in nums) {
        if (value == 1) { run += 1; if (run > best) best = run }
    }
    return best
}
"""),
        ("counts-zeros-too", "WRONG_BRANCH",
         "값을 보지 않고 자리마다 길이를 올린다. 0 도 이어진 것으로 센다.",
         """
fun maxConsecutiveOnes(nums: IntArray): Int {
    var best = 0
    var run = 0
    for (value in nums) {
        run += 1
        if (run > best) best = run
        if (value == 0) { }
    }
    return best
}
"""),
        ("off-by-one-run", "OFF_BY_ONE",
         "이어지는 길이를 0 이 아니라 1 에서 다시 시작한다. 0 뒤의 구간이 하나씩 길어진다.",
         """
fun maxConsecutiveOnes(nums: IntArray): Int {
    var best = 0
    var run = 0
    for (value in nums) {
        if (value == 1) { run += 1; if (run > best) best = run } else run = 1
    }
    return best
}
"""),
    ],
))


# --- 193. 산 모양 배열인가 ------------------------------------------------------------------------

def _is_mountain(nums):
    n = len(nums)
    i = 0
    while i + 1 < n and nums[i] < nums[i + 1]:
        i += 1
    if i == 0 or i == n - 1:
        return 0
    while i + 1 < n and nums[i] > nums[i + 1]:
        i += 1
    return 1 if i == n - 1 else 0


PROBLEMS.append(Problem(
    id="valid-mountain-array",
    title="산 모양 배열인가",
    summary="""
배열 `nums` 가 **산 모양**이면 `1`, 아니면 `0` 을 반환한다. 산 모양이란 어떤 꼭대기까지 **엄격히 늘다가**
그 뒤로 **엄격히 주는** 것이다. 오르막과 내리막이 각각 한 칸 이상 있어야 하므로 길이가 3 보다 짧으면 산이
아니고, 평평한 구간이 있어도 산이 아니다.
""",
    notes="""
앞에서부터 오를 수 있는 데까지 오르고, 거기서부터 내릴 수 있는 데까지 내린다. 끝에 닿았으면 산이다.

걸리는 곳은 **꼭대기의 자리**다 — 한 번도 오르지 못했거나(첫 칸이 꼭대기) 끝까지 올랐으면(마지막 칸이
꼭대기) 한쪽이 비었으니 산이 아니다.
""",
    drill_doc="""
Drill.pointer("peak", index)  // 꼭대기로 정한 자리
Drill.compare(left, right)    // 이웃한 두 칸을 견줬다
""",
    constraints="""
- `1 <= nums.size <= 200_000`, `-1_000_000_000 <= nums[i] <= 1_000_000_000`
""",
    signature=dict(name="isMountain", parameters=[("nums", "INT_ARRAY")], returns="INT"),
    groups=standard_groups(),
    reference=_is_mountain,
    limits={"timeMillis": 2000, "memoryMb": 256, "outputBytes": 65536},
    cases={
        "sample": [("01", [[0, 3, 2, 1]]), ("02", [[3, 5, 5]])],
        "boundary": [
            ("01-too-short", [[1, 2]]),
            ("02-single", [[7]]),
            # 꼭대기가 맨 끝 — 내리막이 없다.
            ("03-only-up", [[1, 2, 3]]),
            ("04-only-down", [[3, 2, 1]]),
            ("05-flat-top", [[1, 3, 3, 1]]),
            ("06-flat-start", [[2, 2, 3, 1]]),
            ("07-minimal-mountain", [[1, 2, 1]]),
            ("08-all-equal", [[4, 4, 4]]),
            ("09-negatives", [[-5, -1, -3]]),
        ],
        "hidden": [
            ("01-long-mountain", [list(range(0, 300)) + list(range(299, -1, -1))]),
            ("02-up-then-flat", [list(range(0, 100)) + [99] + list(range(98, -1, -1))]),
            ("03-random", [randoms(50, 0, 20, salt=9921)]),
            ("04-peak-near-start", [[0, 9] + list(range(8, -1, -1))]),
            ("05-peak-near-end", [list(range(0, 300)) + [1]]),
            ("06-dip-then-rise", [[5, 3, 4]]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 오를 수 있는 데까지 오르고, 거기서 끝까지 내려가는지 본다.
fun isMountain(nums: IntArray): Int {
    val n = nums.size
    var i = 0
    while (i + 1 < n && nums[i] < nums[i + 1]) { Drill.compare(i, i + 1); i += 1 }
    Drill.pointer("peak", i)
    if (i == 0 || i == n - 1) return 0
    while (i + 1 < n && nums[i] > nums[i + 1]) { Drill.compare(i, i + 1); i += 1 }
    return if (i == n - 1) 1 else 0
}
""",
    mutants=[
        ("allows-flat", "WRONG_BRANCH",
         "같은 값이 이어져도 오르막·내리막으로 친다. 산은 엄격히 늘고 엄격히 줄어야 한다.",
         """
fun isMountain(nums: IntArray): Int {
    val n = nums.size
    var i = 0
    while (i + 1 < n && nums[i] <= nums[i + 1]) i += 1
    if (i == 0 || i == n - 1) return 0
    while (i + 1 < n && nums[i] >= nums[i + 1]) i += 1
    return if (i == n - 1) 1 else 0
}
"""),
        ("peak-at-edge-allowed", "MISSING_EDGE_CASE",
         "꼭대기가 첫 칸이나 마지막 칸이어도 산으로 친다. 양쪽이 모두 있어야 한다.",
         """
fun isMountain(nums: IntArray): Int {
    val n = nums.size
    var i = 0
    while (i + 1 < n && nums[i] < nums[i + 1]) i += 1
    while (i + 1 < n && nums[i] > nums[i + 1]) i += 1
    return if (i == n - 1) 1 else 0
}
"""),
        ("checks-only-the-climb", "WRONG_ALGORITHM",
         "오르막만 확인하고 내리막이 끝까지 가는지 보지 않는다.",
         """
fun isMountain(nums: IntArray): Int {
    val n = nums.size
    var i = 0
    while (i + 1 < n && nums[i] < nums[i + 1]) i += 1
    return if (i > 0 && i < n - 1) 1 else 0
}
"""),
        ("length-two-passes", "OFF_BY_ONE",
         "길이가 둘인 배열을 거른 것으로 친다. 꼭대기 자리 검사가 그 자리를 못 막는다.",
         """
fun isMountain(nums: IntArray): Int {
    val n = nums.size
    if (n < 2) return 0
    var i = 0
    while (i + 1 < n && nums[i] < nums[i + 1]) i += 1
    if (i == 0) return 0
    while (i + 1 < n && nums[i] > nums[i + 1]) i += 1
    return if (i == n - 1) 1 else 0
}
"""),
    ],
))


# --- 194. 합이 0 인 가장 긴 구간 --------------------------------------------------------------------

def _longest_zero_sum(nums):
    first = {0: -1}
    total = 0
    best = 0
    for i, value in enumerate(nums):
        total += value
        if total in first:
            if i - first[total] > best:
                best = i - first[total]
        else:
            first[total] = i
    return best


PROBLEMS.append(Problem(
    id="longest-zero-sum-subarray",
    title="합이 0 인 가장 긴 구간",
    summary="""
정수 배열 `nums` 에서 **합이 `0` 인 가장 긴 연속 구간**의 길이를 반환한다. 그런 구간이 없으면 `0` 이다.
""",
    notes="""
구간의 합은 접두사 합의 차다 — `nums[l..r]` 의 합이 `0` 이라는 것은 `합[r] == 합[l-1]` 이라는 뜻이다.
그러니 **같은 접두사 합이 두 번 나오는 자리**를 찾으면 된다.

같은 값이 여러 번 나오면 **가장 먼저 나온 자리**만 기억한다 — 나중 것으로 덮어쓰면 구간이 짧아진다.
빈 접두사(합 `0`, 자리 `-1`)를 미리 넣어 두면 맨 앞에서 시작하는 구간이 저절로 잡힌다.
""",
    drill_doc="""
Drill.write(index, prefix)    // 그 자리까지의 접두사 합
Drill.match(left, right)      // 같은 접두사 합을 만났다
""",
    constraints="""
- `1 <= nums.size <= 200_000`, `-1_000_000 <= nums[i] <= 1_000_000`
""",
    signature=dict(name="longestZeroSum", parameters=[("nums", "INT_ARRAY")], returns="INT"),
    groups=perf_groups(),
    reference=_longest_zero_sum,
    limits={"timeMillis": 2000, "memoryMb": 256, "outputBytes": 65536},
    cases={
        "sample": [("01", [[1, 2, -3, 3]]), ("02", [[1, 2, 3]])],
        "boundary": [
            ("01-single-zero", [[0]]),
            ("02-single-nonzero", [[5]]),
            ("03-whole-array", [[2, -2, 3, -3]]),
            # 같은 접두사 합이 세 번 — 가장 먼 짝이 답이다.
            ("04-repeated-prefix", [[1, -1, 1, -1, 1, -1]]),
            ("05-starts-at-front", [[3, -3, 5]]),
            ("06-none", [[1, 2, 3, 4]]),
            ("07-all-zeros", [[0, 0, 0]]),
            ("08-negatives-first", [[-4, 4, -1, 1]]),
        ],
        "hidden": [
            ("01-random-small", [randoms(20, -3, 3, salt=9931)]),
            ("02-random-medium", [randoms(500, -5, 5, salt=9933)]),
            ("03-random-wide", [randoms(1000, -1000000, 1000000, salt=9935)]),
            ("04-long-balanced", [[1] * 500 + [-1] * 500]),
            ("05-no-zero-sum", [[7] * 500]),
        ],
        "performance": [
            ("01-medium", [randoms(50000, -2, 2, salt=9941)]),
            # 합이 0 인 구간이 없다 — 두 겹 풀이가 한 번도 일찍 끝나지 못한다.
            ("02-no-answer-large", [[1] * 200000]),
            ("03-random-large", [randoms(200000, -1000000, 1000000, salt=9943)]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 접두사 합이 처음 나온 자리를 기억하고, 다시 만나면 그 거리가 후보다.
fun longestZeroSum(nums: IntArray): Int {
    val first = HashMap<Long, Int>()
    first[0L] = -1
    var total = 0L
    var best = 0
    for (i in nums.indices) {
        total += nums[i]
        Drill.write(i, total.toInt())
        val seen = first[total]
        if (seen != null) {
            if (i - seen > best) { best = i - seen; Drill.match(seen, i) }
        } else {
            first[total] = i
        }
    }
    return best
}
""",
    mutants=[
        ("keeps-latest-index", "WRONG_ALGORITHM",
         "접두사 합의 자리를 볼 때마다 덮어쓴다. 가장 먼 짝 대신 가까운 짝을 잰다.",
         """
fun longestZeroSum(nums: IntArray): Int {
    val seen = HashMap<Long, Int>()
    seen[0L] = -1
    var total = 0L
    var best = 0
    for (i in nums.indices) {
        total += nums[i]
        val at = seen[total]
        if (at != null && i - at > best) best = i - at
        seen[total] = i
    }
    return best
}
"""),
        ("no-empty-prefix", "MISSING_EDGE_CASE",
         "빈 접두사를 넣지 않는다. 맨 앞에서 시작하는 구간을 놓친다.",
         """
fun longestZeroSum(nums: IntArray): Int {
    val first = HashMap<Long, Int>()
    var total = 0L
    var best = 0
    for (i in nums.indices) {
        total += nums[i]
        val at = first[total]
        if (at != null) { if (i - at > best) best = i - at } else first[total] = i
    }
    return best
}
"""),
        ("length-off-by-one", "OFF_BY_ONE",
         "구간의 길이를 하나 짧게 센다. 두 자리 사이의 칸 수가 곧 길이다.",
         """
fun longestZeroSum(nums: IntArray): Int {
    val first = HashMap<Long, Int>()
    first[0L] = -1
    var total = 0L
    var best = 0
    for (i in nums.indices) {
        total += nums[i]
        val at = first[total]
        if (at != null) { if (i - at - 1 > best) best = i - at - 1 } else first[total] = i
    }
    return best
}
"""),
        ("all-pairs", "PERFORMANCE",
         "모든 시작점에서 합을 다시 더해 본다. O(n^2).",
         """
fun longestZeroSum(nums: IntArray): Int {
    var best = 0
    for (l in nums.indices) {
        var total = 0L
        for (r in l until nums.size) {
            total += nums[r]
            Drill.compare(l, r)
            if (total == 0L && r - l + 1 > best) best = r - l + 1
        }
    }
    return best
}
"""),
    ],
))
