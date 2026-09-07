"""그래프 (역량: 관계를 자료구조로 옮기고 탐색하기).

간선은 평탄화한 정수 배열로 준다. `[a1, b1, a2, b2, ...]` 이며, 정점 수는 따로 받는다.
그래프를 인접 리스트로 옮기는 것부터가 문제의 일부다.
"""

from author import Problem, standard_groups, perf_groups, randoms

PROBLEMS = []


def _adjacency(n, edges, directed=False):
    graph = [[] for _ in range(n)]
    for i in range(0, len(edges), 2):
        a, b = edges[i], edges[i + 1]
        graph[a].append(b)
        if not directed:
            graph[b].append(a)
    return graph


# --- 17. 최단 거리(간선 수) --------------------------------------------------

def _shortest_hops(n, edges, target):
    from collections import deque
    graph = _adjacency(n, edges)
    distance = [-1] * n
    distance[0] = 0
    queue = deque([0])
    while queue:
        node = queue.popleft()
        for nxt in graph[node]:
            if distance[nxt] == -1:
                distance[nxt] = distance[node] + 1
                queue.append(nxt)
    return distance[target]


PROBLEMS.append(Problem(
    id="shortest-hops",
    title="가장 적은 간선으로 가기",
    summary="""
정점이 `0` 부터 `n-1` 까지 있는 **무방향** 그래프가 주어진다. `edges` 는 간선을 평탄하게
이은 배열로, `[a1, b1, a2, b2, ...]` 형태다.

정점 `0` 에서 `target` 까지 가는 데 필요한 **최소 간선 수**를 반환한다. 갈 수 없으면
`-1` 이다.
""",
    notes="""
가중치가 없으므로 너비 우선 탐색이 곧 최단 거리다. 깊이 우선으로 먼저 닿은 경로를
답으로 삼으면 더 짧은 길을 놓친다.
""",
    drill_doc="""
Drill.node("v3")           // 정점을 방문했다
Drill.edge("v3", "v7")     // 간선을 따라갔다
Drill.enqueue(next)        // 큐에 넣었다
Drill.dequeue(node)        // 큐에서 꺼냈다
""",
    constraints="""
- `1 <= n <= 100_000`
- `edges.size` 는 짝수이며 `0 <= edges.size <= 400_000`
- `0 <= target < n`
""",
    signature=dict(
        name="shortestHops",
        parameters=[("n", "INT"), ("edges", "INT_ARRAY"), ("target", "INT")],
        returns="INT",
    ),
    groups=standard_groups(),
    reference=_shortest_hops,
    cases={
        "sample": [
            ("01", [4, [0, 1, 1, 2, 2, 3], 3]),
            ("02", [5, [0, 1, 0, 2, 1, 3, 2, 4], 4]),
        ],
        "boundary": [
            ("01-self", [3, [0, 1, 1, 2], 0]),
            ("02-unreachable", [4, [0, 1, 2, 3], 3]),
            ("03-single-node", [1, [], 0]),
            # 지름길이 있다. 깊이 우선이 먼 길로 먼저 닿으면 틀린다.
            ("04-shortcut", [5, [0, 1, 1, 2, 2, 3, 3, 4, 0, 4], 4]),
            # 짧은 길(0-1-2)과 긴 길(0-3-4-5-2)이 함께 있고, 간선 순서 때문에 깊이
            # 우선 탐색은 긴 쪽을 먼저 끝까지 따라간다. 먼저 닿은 것을 답으로 삼으면 4 다.
            ("05-dfs-takes-long-path", [6, [0, 1, 1, 2, 0, 3, 3, 4, 4, 5, 5, 2], 2]),
            # 같은 간선이 여러 번 나온다.
            ("06-duplicate-edges", [3, [0, 1, 0, 1, 1, 2], 2]),
            # 자기 자신으로 가는 간선.
            ("07-self-loop", [3, [0, 0, 0, 1, 1, 2], 2]),
        ],
        "hidden": [
            ("01-line", [30, [x for i in range(29) for x in (i, i + 1)], 29]),
            ("02-star", [20, [x for i in range(1, 20) for x in (0, i)], 19]),
            ("03-two-components", [10, [0, 1, 1, 2, 5, 6, 6, 7], 7]),
            ("04-cycle", [6, [0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 0], 4]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 너비 우선 탐색.
//
// 가중치가 없으면 BFS 가 곧 최단 거리다. 큐에서 꺼내는 순서가 거리 순서라,
// 처음 닿은 순간이 가장 짧은 길이다 — 깊이 우선은 이 성질이 없다.
fun shortestHops(n: Int, edges: IntArray, target: Int): Int {
    val degree = IntArray(n)
    var i = 0
    while (i < edges.size) {
        degree[edges[i]] += 1
        degree[edges[i + 1]] += 1
        i += 2
    }
    val start = IntArray(n + 1)
    for (v in 0 until n) start[v + 1] = start[v] + degree[v]
    val cursor = start.copyOf()
    val flat = IntArray(edges.size)

    i = 0
    while (i < edges.size) {
        val a = edges[i]
        val b = edges[i + 1]
        flat[cursor[a]++] = b
        flat[cursor[b]++] = a
        i += 2
    }

    val distance = IntArray(n) { -1 }
    distance[0] = 0
    val queue = ArrayDeque<Int>()
    queue.addLast(0)
    Drill.enqueue(0)

    while (queue.isNotEmpty()) {
        val node = queue.removeFirst()
        Drill.dequeue(node)
        Drill.node("v$node")

        for (index in start[node] until start[node + 1]) {
            val next = flat[index]
            if (distance[next] != -1) continue
            distance[next] = distance[node] + 1
            Drill.edge("v$node", "v$next")
            Drill.enqueue(next)
            queue.addLast(next)
        }
    }
    return distance[target]
}
""",
    mutants=[
        ("depth-first--not-shortest", "WRONG_BRANCH",
         "깊이 우선으로 먼저 닿은 경로를 답으로 삼는다. 지름길을 놓친다.",
         """
fun shortestHops(n: Int, edges: IntArray, target: Int): Int {
    val graph = Array(n) { mutableListOf<Int>() }
    var i = 0
    while (i < edges.size) {
        graph[edges[i]].add(edges[i + 1])
        graph[edges[i + 1]].add(edges[i])
        i += 2
    }
    val distance = IntArray(n) { -1 }
    val stack = ArrayDeque<Int>()
    distance[0] = 0
    stack.addLast(0)
    while (stack.isNotEmpty()) {
        val node = stack.removeLast()
        for (next in graph[node]) {
            if (distance[next] != -1) continue
            distance[next] = distance[node] + 1
            stack.addLast(next)
        }
    }
    return distance[target]
}
"""),
        ("directed-only--ignores-reverse", "MISSING_EDGE_CASE",
         "간선을 한 방향으로만 넣는다. 무방향인데 되돌아가지 못한다.",
         """
fun shortestHops(n: Int, edges: IntArray, target: Int): Int {
    val graph = Array(n) { mutableListOf<Int>() }
    var i = 0
    while (i < edges.size) {
        graph[edges[i]].add(edges[i + 1])
        i += 2
    }
    val distance = IntArray(n) { -1 }
    distance[0] = 0
    val queue = ArrayDeque<Int>()
    queue.addLast(0)
    while (queue.isNotEmpty()) {
        val node = queue.removeFirst()
        for (next in graph[node]) {
            if (distance[next] != -1) continue
            distance[next] = distance[node] + 1
            queue.addLast(next)
        }
    }
    return distance[target]
}
"""),
        ("counts-nodes--off-by-one", "OFF_BY_ONE",
         "간선 수가 아니라 지나온 정점 수를 센다.",
         """
fun shortestHops(n: Int, edges: IntArray, target: Int): Int {
    val graph = Array(n) { mutableListOf<Int>() }
    var i = 0
    while (i < edges.size) {
        graph[edges[i]].add(edges[i + 1])
        graph[edges[i + 1]].add(edges[i])
        i += 2
    }
    val distance = IntArray(n) { -1 }
    distance[0] = 1
    val queue = ArrayDeque<Int>()
    queue.addLast(0)
    while (queue.isNotEmpty()) {
        val node = queue.removeFirst()
        for (next in graph[node]) {
            if (distance[next] != -1) continue
            distance[next] = distance[node] + 1
            queue.addLast(next)
        }
    }
    return distance[target]
}
"""),
    ],
))


# --- 18. 연결 요소 개수 ------------------------------------------------------

def _components(n, edges):
    parent = list(range(n))

    def find(x):
        while parent[x] != x:
            parent[x] = parent[parent[x]]
            x = parent[x]
        return x

    count = n
    for i in range(0, len(edges), 2):
        a, b = find(edges[i]), find(edges[i + 1])
        if a != b:
            parent[a] = b
            count -= 1
    return count


PROBLEMS.append(Problem(
    id="connected-components",
    title="연결 요소의 개수",
    summary="""
정점 `0..n-1` 과 무방향 간선 배열 `edges`(`[a1, b1, a2, b2, ...]`)가 주어진다.
서로 이어진 정점들을 하나로 묶었을 때 **묶음의 개수**를 반환한다.
""",
    notes="""
간선이 하나도 없으면 정점 수가 곧 답이다. 간선이 반복되거나 자기 자신을 가리켜도
묶음 수는 달라지지 않는다.
""",
    drill_doc="""
Drill.node("v3")        // 정점을 봤다
Drill.edge("v3", "v7")  // 두 묶음을 합쳤다
Drill.write(0, count)   // 현재 묶음 수
""",
    constraints="""
- `1 <= n <= 200_000`
- `edges.size` 는 짝수이며 `0 <= edges.size <= 400_000`
""",
    signature=dict(name="countComponents", parameters=[("n", "INT"), ("edges", "INT_ARRAY")],
                   returns="INT"),
    groups=perf_groups(),
    reference=_components,
    cases={
        "sample": [
            ("01", [5, [0, 1, 1, 2, 3, 4]]),
            ("02", [5, [0, 1, 1, 2, 2, 3, 3, 4]]),
        ],
        "boundary": [
            ("01-no-edges", [4, []]),
            ("02-single-node", [1, []]),
            ("03-self-loop", [3, [0, 0, 1, 1, 2, 2]]),
            # 같은 간선이 여러 번. 셀 때마다 줄이면 음수가 된다.
            ("04-duplicate-edges", [3, [0, 1, 0, 1, 0, 1]]),
            ("05-all-connected", [4, [0, 1, 1, 2, 2, 3]]),
            ("06-cycle-one-component", [4, [0, 1, 1, 2, 2, 3, 3, 0]]),
        ],
        "hidden": [
            ("01-pairs", [20, [x for i in range(0, 20, 2) for x in (i, i + 1)]]),
            ("02-chain-and-isolated", [15, [x for i in range(9) for x in (i, i + 1)]]),
            ("03-star-plus-loose", [12, [x for i in range(1, 6) for x in (0, i)]]),
        ],
        # 경로 압축이 없으면 무너지는 입력.
        #
        # 별 모양을 한 정점 기준으로 주면, 합칠 때마다 뿌리가 한 칸씩 깊어져 find 가
        # 매번 전체를 걸어 올라간다. 어느 방향으로 붙이느냐에 따라 깊어지는 쪽이
        # 달라지므로 **두 순서를 모두** 넣는다 — 한쪽만 있으면 반대로 구현한 풀이가
        # 우연히 통과한다.
        "performance": [
            ("01-small", [5000, [x for i in range(4999) for x in (i, i + 1)]]),
            ("02-star-outward", [100000, [x for k in range(1, 100000) for x in (0, k)]]),
            ("03-star-inward", [100000, [x for k in range(1, 100000) for x in (k, 0)]]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 유니온 파인드 + 경로 압축.
//
// 묶음 수는 정점 수에서 시작해, **서로 다른 두 묶음을 실제로 합쳤을 때만** 줄인다.
// 간선마다 줄이면 같은 간선이 반복되거나 자기 자신을 가리킬 때 음수가 된다.
//
// 경로 압축이 없으면 한 줄로 긴 그래프에서 매번 뿌리까지 걸어 올라가 O(n^2) 가 된다.
fun countComponents(n: Int, edges: IntArray): Int {
    val parent = IntArray(n) { it }

    fun find(start: Int): Int {
        var node = start
        while (parent[node] != node) {
            parent[node] = parent[parent[node]]
            node = parent[node]
        }
        return node
    }

    var count = n
    var i = 0
    while (i < edges.size) {
        val a = find(edges[i])
        val b = find(edges[i + 1])
        Drill.node("v${edges[i]}")
        if (a != b) {
            parent[a] = b
            count -= 1
            Drill.edge("v${edges[i]}", "v${edges[i + 1]}")
            Drill.write(0, count)
        }
        i += 2
    }
    return count
}
""",
    mutants=[
        ("counts-every-edge--goes-negative", "WRONG_BRANCH",
         "간선마다 묶음 수를 줄인다. 같은 간선이 반복되면 틀린다.",
         """
fun countComponents(n: Int, edges: IntArray): Int {
    val parent = IntArray(n) { it }
    fun find(start: Int): Int {
        var node = start
        while (parent[node] != node) node = parent[node]
        return node
    }
    var count = n
    var i = 0
    while (i < edges.size) {
        parent[find(edges[i])] = find(edges[i + 1])
        count -= 1
        i += 2
    }
    return count
}
"""),
        ("ignores-isolated--counts-edges-only", "MISSING_EDGE_CASE",
         "간선에 나온 정점만 센다. 홀로 떨어진 정점을 빠뜨린다.",
         """
fun countComponents(n: Int, edges: IntArray): Int {
    if (edges.isEmpty()) return 0
    val parent = IntArray(n) { it }
    fun find(start: Int): Int {
        var node = start
        while (parent[node] != node) { parent[node] = parent[parent[node]]; node = parent[node] }
        return node
    }
    val seen = HashSet<Int>()
    var i = 0
    while (i < edges.size) {
        seen.add(edges[i]); seen.add(edges[i + 1])
        val a = find(edges[i]); val b = find(edges[i + 1])
        if (a != b) parent[a] = b
        i += 2
    }
    return seen.map { find(it) }.distinct().size
}
"""),
        ("no-path-compression--quadratic", "PERFORMANCE",
         "경로 압축이 없어 한 줄로 긴 그래프에서 매번 뿌리까지 걸어 올라간다.",
         """
fun countComponents(n: Int, edges: IntArray): Int {
    val parent = IntArray(n) { it }
    fun find(start: Int): Int {
        var node = start
        while (parent[node] != node) node = parent[node]
        return node
    }
    var count = n
    var i = 0
    while (i < edges.size) {
        val a = find(edges[i])
        val b = find(edges[i + 1])
        if (a != b) { parent[b] = a; count -= 1 }
        i += 2
    }
    return count
}
"""),
    ],
))


# --- 19. 방향 그래프의 사이클 ------------------------------------------------

def _has_cycle(n, edges):
    graph = _adjacency(n, edges, directed=True)
    indegree = [0] * n
    for node in range(n):
        for nxt in graph[node]:
            indegree[nxt] += 1
    from collections import deque
    queue = deque(v for v in range(n) if indegree[v] == 0)
    seen = 0
    while queue:
        node = queue.popleft()
        seen += 1
        for nxt in graph[node]:
            indegree[nxt] -= 1
            if indegree[nxt] == 0:
                queue.append(nxt)
    return 0 if seen == n else 1


PROBLEMS.append(Problem(
    id="detect-cycle-directed",
    title="방향 그래프에 순환이 있는가",
    summary="""
정점 `0..n-1` 과 **방향** 간선 배열 `edges`(`[from1, to1, from2, to2, ...]`)가 주어진다.
순환이 있으면 `1`, 없으면 `0` 을 반환한다.

작업 사이의 선후 관계에 모순이 있는지를 보는 것과 같은 문제다.
""",
    notes="""
무방향 그래프의 사이클 판정과 다르다. `0 → 1`, `0 → 2`, `1 → 3`, `2 → 3` 은 순환이
아니다 — 같은 정점에 두 번 닿았다고 순환인 것은 아니다.
""",
    drill_doc="""
Drill.node("v3")        // 정점을 확정했다
Drill.edge("v3", "v7")  // 간선을 지웠다
Drill.write(0, done)    // 지금까지 확정한 정점 수
""",
    constraints="""
- `1 <= n <= 100_000`
- `edges.size` 는 짝수이며 `0 <= edges.size <= 200_000`
""",
    signature=dict(name="hasCycle", parameters=[("n", "INT"), ("edges", "INT_ARRAY")],
                   returns="INT"),
    groups=standard_groups(),
    reference=_has_cycle,
    cases={
        "sample": [
            ("01", [3, [0, 1, 1, 2, 2, 0]]),
            ("02", [3, [0, 1, 1, 2]]),
        ],
        "boundary": [
            ("01-no-edges", [3, []]),
            ("02-self-loop", [2, [0, 0]]),
            ("03-two-cycle", [2, [0, 1, 1, 0]]),
            # 다이아몬드. 같은 정점에 두 번 닿지만 순환은 아니다.
            ("04-diamond-not-cycle", [4, [0, 1, 0, 2, 1, 3, 2, 3]]),
            # 순환이 도달할 수 없는 곳에 있다. 0 에서만 탐색하면 놓친다.
            ("05-unreachable-cycle", [5, [0, 1, 2, 3, 3, 4, 4, 2]]),
            ("06-parallel-edges", [3, [0, 1, 0, 1, 1, 2]]),
        ],
        "hidden": [
            ("01-long-chain", [40, [x for i in range(39) for x in (i, i + 1)]]),
            ("02-chain-with-back-edge", [40, [x for i in range(39) for x in (i, i + 1)] + [39, 0]]),
            ("03-two-components-one-cyclic", [8, [0, 1, 1, 2, 4, 5, 5, 6, 6, 4]]),
            ("04-tree", [7, [0, 1, 0, 2, 1, 3, 1, 4, 2, 5, 2, 6]]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 위상 정렬(칸 알고리즘).
//
// 들어오는 간선이 없는 정점부터 하나씩 확정해 나간다. 전부 확정하지 못하고 멈추면
// 남은 것들이 서로를 기다리고 있다는 뜻이고, 그것이 곧 순환이다.
//
// 모든 정점을 시작점으로 삼는 것이 중요하다. 0 에서만 탐색하면 거기서 닿을 수 없는
// 곳의 순환을 놓친다.
fun hasCycle(n: Int, edges: IntArray): Int {
    val outDegree = IntArray(n)
    val inDegree = IntArray(n)
    var i = 0
    while (i < edges.size) {
        outDegree[edges[i]] += 1
        inDegree[edges[i + 1]] += 1
        i += 2
    }

    val start = IntArray(n + 1)
    for (v in 0 until n) start[v + 1] = start[v] + outDegree[v]
    val cursor = start.copyOf()
    val flat = IntArray(edges.size / 2)
    i = 0
    while (i < edges.size) {
        flat[cursor[edges[i]]++] = edges[i + 1]
        i += 2
    }

    val queue = ArrayDeque<Int>()
    for (v in 0 until n) if (inDegree[v] == 0) queue.addLast(v)

    var done = 0
    while (queue.isNotEmpty()) {
        val node = queue.removeFirst()
        Drill.node("v$node")
        done += 1
        Drill.write(0, done)

        for (index in start[node] until start[node + 1]) {
            val next = flat[index]
            Drill.edge("v$node", "v$next")
            inDegree[next] -= 1
            if (inDegree[next] == 0) queue.addLast(next)
        }
    }
    return if (done == n) 0 else 1
}
""",
    mutants=[
        ("revisit-means-cycle--false-positive", "WRONG_BRANCH",
         "이미 본 정점에 다시 닿으면 순환이라고 본다. 다이아몬드에서 틀린다.",
         """
fun hasCycle(n: Int, edges: IntArray): Int {
    val graph = Array(n) { mutableListOf<Int>() }
    var i = 0
    while (i < edges.size) { graph[edges[i]].add(edges[i + 1]); i += 2 }
    val seen = BooleanArray(n)
    for (root in 0 until n) {
        if (seen[root]) continue
        val stack = ArrayDeque<Int>()
        stack.addLast(root)
        while (stack.isNotEmpty()) {
            val node = stack.removeLast()
            if (seen[node]) return 1
            seen[node] = true
            for (next in graph[node]) stack.addLast(next)
        }
    }
    return 0
}
"""),
        ("from-zero-only--misses-unreachable", "MISSING_EDGE_CASE",
         "0 에서만 탐색해 거기서 닿을 수 없는 순환을 놓친다.",
         """
fun hasCycle(n: Int, edges: IntArray): Int {
    val graph = Array(n) { mutableListOf<Int>() }
    var i = 0
    while (i < edges.size) { graph[edges[i]].add(edges[i + 1]); i += 2 }
    val state = IntArray(n)
    fun visit(node: Int): Boolean {
        if (state[node] == 1) return true
        if (state[node] == 2) return false
        state[node] = 1
        for (next in graph[node]) if (visit(next)) return true
        state[node] = 2
        return false
    }
    return if (visit(0)) 1 else 0
}
"""),
        ("undirected--adds-reverse-edge", "WRONG_BRANCH",
         "간선을 양방향으로 넣어 방향 하나짜리 간선도 순환으로 본다.",
         """
fun hasCycle(n: Int, edges: IntArray): Int {
    val inDegree = IntArray(n)
    val graph = Array(n) { mutableListOf<Int>() }
    var i = 0
    while (i < edges.size) {
        graph[edges[i]].add(edges[i + 1])
        graph[edges[i + 1]].add(edges[i])
        inDegree[edges[i + 1]] += 1
        inDegree[edges[i]] += 1
        i += 2
    }
    val queue = ArrayDeque<Int>()
    for (v in 0 until n) if (inDegree[v] == 0) queue.addLast(v)
    var done = 0
    while (queue.isNotEmpty()) {
        val node = queue.removeFirst()
        done += 1
        for (next in graph[node]) {
            inDegree[next] -= 1
            if (inDegree[next] == 0) queue.addLast(next)
        }
    }
    return if (done == n) 0 else 1
}
"""),
    ],
))


# --- 26. 이분 그래프 ---------------------------------------------------------

def _bipartite(n, edges):
    from collections import deque
    graph = _adjacency(n, edges)
    colour = [0] * n
    for root in range(n):
        if colour[root] != 0:
            continue
        colour[root] = 1
        queue = deque([root])
        while queue:
            node = queue.popleft()
            for nxt in graph[node]:
                if colour[nxt] == 0:
                    colour[nxt] = -colour[node]
                    queue.append(nxt)
                elif colour[nxt] == colour[node]:
                    return 0
    return 1


PROBLEMS.append(Problem(
    id="bipartite-check",
    title="두 편으로 나눌 수 있는가",
    summary="""
정점 `0..n-1` 과 무방향 간선 배열 `edges` 가 주어진다. 모든 간선이 **서로 다른 편**을
잇도록 정점을 두 편으로 나눌 수 있으면 `1`, 없으면 `0` 을 반환한다.

"사이가 나쁜 둘을 다른 조로 보낼 수 있는가"와 같은 문제다.
""",
    notes="""
연결되어 있지 않은 덩어리가 여럿일 수 있다. 정점 0 에서만 시작하면 다른 덩어리의
모순을 놓친다.

홀수 길이의 순환이 하나라도 있으면 나눌 수 없다.
""",
    drill_doc="""
Drill.node("v3")        // 정점에 편을 정했다
Drill.edge("v3", "v7")  // 간선을 따라갔다
Drill.write(node, side) // 그 정점의 편
""",
    constraints="""
- `1 <= n <= 100_000`
- `edges.size` 는 짝수이며 `0 <= edges.size <= 400_000`
""",
    signature=dict(name="isBipartite", parameters=[("n", "INT"), ("edges", "INT_ARRAY")],
                   returns="INT"),
    groups=standard_groups(),
    reference=_bipartite,
    cases={
        "sample": [
            ("01", [4, [0, 1, 1, 2, 2, 3, 3, 0]]),
            ("02", [3, [0, 1, 1, 2, 2, 0]]),
        ],
        "boundary": [
            ("01-no-edges", [3, []]),
            ("02-single-edge", [2, [0, 1]]),
            # 홀수 순환이 0 에서 닿을 수 없는 곳에 있다.
            ("03-unreachable-odd-cycle", [6, [0, 1, 2, 3, 3, 4, 4, 2]]),
            # 자기 자신으로 가는 간선은 절대 나눌 수 없다.
            ("04-self-loop", [2, [0, 0]]),
            ("05-even-cycle", [6, [0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 0]]),
            ("06-duplicate-edge", [2, [0, 1, 0, 1]]),
        ],
        "hidden": [
            ("01-tree", [7, [0, 1, 0, 2, 1, 3, 1, 4, 2, 5, 2, 6]]),
            ("02-two-components-ok", [8, [0, 1, 1, 2, 4, 5, 5, 6]]),
            ("03-long-odd-cycle", [9, [x for i in range(8) for x in (i, i + 1)] + [8, 0]]),
            ("04-long-even-cycle", [10, [x for i in range(9) for x in (i, i + 1)] + [9, 0]]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 두 색으로 칠하기.
//
// 이웃은 반대 편이어야 한다. 칠하다가 이미 같은 편으로 칠해진 이웃을 만나면 홀수
// 순환이 있다는 뜻이고, 그러면 나눌 수 없다.
//
// 모든 정점을 시작점으로 삼는다. 덩어리가 여럿이면 0 에서 닿을 수 없는 곳의 모순을
// 놓치기 때문이다.
fun isBipartite(n: Int, edges: IntArray): Int {
    val degree = IntArray(n)
    var i = 0
    while (i < edges.size) {
        degree[edges[i]] += 1
        degree[edges[i + 1]] += 1
        i += 2
    }
    val start = IntArray(n + 1)
    for (v in 0 until n) start[v + 1] = start[v] + degree[v]
    val cursor = start.copyOf()
    val flat = IntArray(edges.size)
    i = 0
    while (i < edges.size) {
        flat[cursor[edges[i]]++] = edges[i + 1]
        flat[cursor[edges[i + 1]]++] = edges[i]
        i += 2
    }

    val side = IntArray(n)
    val queue = ArrayDeque<Int>()

    for (root in 0 until n) {
        if (side[root] != 0) continue
        side[root] = 1
        queue.addLast(root)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            Drill.node("v$node")
            Drill.write(node, side[node])

            for (index in start[node] until start[node + 1]) {
                val next = flat[index]
                if (side[next] == 0) {
                    side[next] = -side[node]
                    Drill.edge("v$node", "v$next")
                    queue.addLast(next)
                } else if (side[next] == side[node]) {
                    return 0
                }
            }
        }
    }
    return 1
}
""",
    mutants=[
        ("from-zero-only--misses-component", "MISSING_EDGE_CASE",
         "0 에서만 칠해 다른 덩어리의 홀수 순환을 놓친다.",
         """
fun isBipartite(n: Int, edges: IntArray): Int {
    val graph = Array(n) { mutableListOf<Int>() }
    var i = 0
    while (i < edges.size) {
        graph[edges[i]].add(edges[i + 1])
        graph[edges[i + 1]].add(edges[i])
        i += 2
    }
    val side = IntArray(n)
    side[0] = 1
    val queue = ArrayDeque<Int>()
    queue.addLast(0)
    while (queue.isNotEmpty()) {
        val node = queue.removeFirst()
        for (next in graph[node]) {
            if (side[next] == 0) { side[next] = -side[node]; queue.addLast(next) }
            else if (side[next] == side[node]) return 0
        }
    }
    return 1
}
"""),
        ("counts-parity--ignores-conflict", "WRONG_BRANCH",
         "간선 수가 짝수인지만 본다.",
         """
fun isBipartite(n: Int, edges: IntArray): Int =
    if ((edges.size / 2) % 2 == 0) 1 else 0
"""),
        ("no-conflict-check--always-true", "MISSING_EDGE_CASE",
         "이미 칠해진 이웃과 편이 같은지 보지 않는다.",
         """
fun isBipartite(n: Int, edges: IntArray): Int {
    val graph = Array(n) { mutableListOf<Int>() }
    var i = 0
    while (i < edges.size) {
        graph[edges[i]].add(edges[i + 1])
        graph[edges[i + 1]].add(edges[i])
        i += 2
    }
    val side = IntArray(n)
    for (root in 0 until n) {
        if (side[root] != 0) continue
        side[root] = 1
        val queue = ArrayDeque<Int>()
        queue.addLast(root)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            for (next in graph[node]) {
                if (side[next] == 0) { side[next] = -side[node]; queue.addLast(next) }
            }
        }
    }
    return 1
}
"""),
    ],
))
