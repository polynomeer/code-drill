"""그래프 (역량: 관계를 자료구조로 옮기고 탐색하기).

간선은 평탄화한 정수 배열로 준다. `[a1, b1, a2, b2, ...]` 이며, 정점 수는 따로 받는다.
그래프를 인접 리스트로 옮기는 것부터가 문제의 일부다.
"""

from author import Problem, standard_groups, perf_groups, randoms, flat, shuffled

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
        while (parent[node] != node) {
            Drill.edge(node.toString(), parent[node].toString())
            node = parent[node]
        }
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


# --- 53. 최소 학기 수 (위상 정렬) ----------------------------------------------------

def _min_semesters(n, prereqs):
    from collections import deque
    graph = [[] for _ in range(n)]
    indegree = [0] * n
    for i in range(0, len(prereqs), 2):
        before, after = prereqs[i], prereqs[i + 1]
        graph[before].append(after)
        indegree[after] += 1
    queue = deque(v for v in range(n) if indegree[v] == 0)
    taken = 0
    semesters = 0
    while queue:
        semesters += 1
        for _ in range(len(queue)):
            node = queue.popleft()
            taken += 1
            for nxt in graph[node]:
                indegree[nxt] -= 1
                if indegree[nxt] == 0:
                    queue.append(nxt)
    return semesters if taken == n else -1


def _chain_prereqs(n):
    return flat([i, i + 1] for i in range(n - 1))


PROBLEMS.append(Problem(
    id="minimum-semesters",
    title="모든 과목을 듣는 최소 학기",
    summary="""
과목이 `0` 부터 `n-1` 까지 있다. `prereqs` 는 선수 관계를 평탄하게 이은 배열로,
`[a1, b1, a2, b2, ...]` 는 "`a` 를 들어야 `b` 를 들을 수 있다"는 뜻이다.

한 학기에는 **선수 과목을 전부 마친 과목을 몇 개든** 들을 수 있다. 모든 과목을 듣는 데
필요한 **최소 학기 수**를 반환한다. 선수 관계가 돌아서 다 들을 수 없으면 `-1` 이다.
""",
    notes="""
첫 학기에 들을 수 있는 것은 선수 과목이 없는 과목 전부다. 그것을 듣고 나면 선수 과목이
새로 다 채워진 과목이 다음 학기다. 층을 세다 보면 답이 나오고, 끝났는데 못 들은 과목이
있으면 순환이다.
""",
    drill_doc="""
Drill.node("c3")              // 과목을 들었다
Drill.edge("c3", "c5")        // 선수 관계를 따라 다음 과목의 남은 선수 수를 줄였다
Drill.enqueue(next)           // 들을 수 있게 된 과목을 다음 학기에 넣었다
Drill.write(semester, count)  // 한 학기를 마쳤다
""",
    constraints="""
- `1 <= n <= 100_000`
- `prereqs.size` 는 짝수이며 `0 <= prereqs.size <= 400_000`
- 같은 관계가 두 번 나오지 않는다
""",
    signature=dict(
        name="minSemesters",
        parameters=[("n", "INT"), ("prereqs", "INT_ARRAY")],
        returns="INT",
    ),
    groups=perf_groups(),
    reference=_min_semesters,
    cases={
        "sample": [
            ("01", [3, [0, 1, 0, 2]]),
            ("02", [3, [0, 1, 1, 2, 2, 0]]),
        ],
        "boundary": [
            ("01-single", [1, []]),
            # 선수 관계가 없다. 한 학기.
            ("02-no-prereqs", [5, []]),
            # 한 줄 사슬. 과목 수만큼 학기.
            ("03-chain", [4, _chain_prereqs(4)]),
            # 자기 자신이 선수 과목이다.
            ("04-self-loop", [2, [0, 0]]),
            # 순환이 일부에만 있다. 나머지는 들을 수 있어도 답은 -1.
            ("05-partial-cycle", [5, [0, 1, 1, 2, 2, 1, 3, 4]]),
            # 다이아몬드. 두 길 중 긴 쪽이 학기를 정한다.
            ("06-diamond", [5, [0, 1, 0, 2, 1, 3, 2, 3, 3, 4, 1, 4]]),
        ],
        "hidden": [
            ("01-two-chains", [6, [0, 1, 1, 2, 3, 4, 4, 5]]),
            ("02-wide", [8, [0, 1, 0, 2, 0, 3, 0, 4, 0, 5, 0, 6, 0, 7]]),
            ("03-late-cycle", [6, _chain_prereqs(6) + [5, 3]]),
            ("04-long-and-short", [7, [0, 1, 1, 2, 2, 3, 3, 4, 0, 5, 5, 4, 6, 4]]),
        ],
        "performance": [
            ("01-small", [3000, _chain_prereqs(3000)]),
            ("02-medium", [20000, _chain_prereqs(20000)]),
            ("03-large", [100000, _chain_prereqs(100000)]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). Kahn 알고리즘을 층 단위로.
fun minSemesters(n: Int, prereqs: IntArray): Int {
    val indegree = IntArray(n)
    val head = IntArray(n) { -1 }
    val next = IntArray(prereqs.size / 2)
    val to = IntArray(prereqs.size / 2)
    var i = 0
    var e = 0
    while (i < prereqs.size) {
        val before = prereqs[i]
        val after = prereqs[i + 1]
        to[e] = after
        next[e] = head[before]
        head[before] = e
        indegree[after] += 1
        i += 2
        e += 1
    }
    val queue = ArrayDeque<Int>()
    for (v in 0 until n) if (indegree[v] == 0) { queue.addLast(v); Drill.enqueue(v) }
    var taken = 0
    var semesters = 0
    while (queue.isNotEmpty()) {
        semesters += 1
        repeat(queue.size) {
            val node = queue.removeFirst()
            Drill.node("c$node")
            taken += 1
            var edge = head[node]
            while (edge != -1) {
                val after = to[edge]
                Drill.edge("c$node", "c$after")
                indegree[after] -= 1
                if (indegree[after] == 0) { queue.addLast(after); Drill.enqueue(after) }
                edge = next[edge]
            }
        }
        Drill.write(semesters, taken)
    }
    return if (taken == n) semesters else -1
}
""",
    mutants=[
        ("ignores-cycle--returns-semesters", "MISSING_EDGE_CASE",
         "못 들은 과목이 남아도 학기 수를 돌려준다. 순환을 알아채지 못한다.",
         """
fun minSemesters(n: Int, prereqs: IntArray): Int {
    val indegree = IntArray(n)
    val graph = Array(n) { mutableListOf<Int>() }
    var i = 0
    while (i < prereqs.size) { graph[prereqs[i]].add(prereqs[i + 1]); indegree[prereqs[i + 1]] += 1; i += 2 }
    val queue = ArrayDeque<Int>()
    for (v in 0 until n) if (indegree[v] == 0) queue.addLast(v)
    var semesters = 0
    while (queue.isNotEmpty()) {
        semesters += 1
        repeat(queue.size) {
            val node = queue.removeFirst()
            for (after in graph[node]) { indegree[after] -= 1; if (indegree[after] == 0) queue.addLast(after) }
        }
    }
    return semesters
}
"""),
        ("counts-nodes--not-layers", "WRONG_ALGORITHM",
         "과목을 하나씩 꺼내며 학기를 센다. 같은 학기에 들을 수 있는 과목을 따로 센다.",
         """
fun minSemesters(n: Int, prereqs: IntArray): Int {
    val indegree = IntArray(n)
    val graph = Array(n) { mutableListOf<Int>() }
    var i = 0
    while (i < prereqs.size) { graph[prereqs[i]].add(prereqs[i + 1]); indegree[prereqs[i + 1]] += 1; i += 2 }
    val queue = ArrayDeque<Int>()
    for (v in 0 until n) if (indegree[v] == 0) queue.addLast(v)
    var taken = 0
    while (queue.isNotEmpty()) {
        val node = queue.removeFirst()
        taken += 1
        for (after in graph[node]) { indegree[after] -= 1; if (indegree[after] == 0) queue.addLast(after) }
    }
    return if (taken == n) taken else -1
}
"""),
        ("last-layer-uncounted--off-by-one", "OFF_BY_ONE",
         "다음 학기에 들을 과목이 생길 때만 학기를 센다. 마지막 학기가 빠진다.",
         """
fun minSemesters(n: Int, prereqs: IntArray): Int {
    val indegree = IntArray(n)
    val graph = Array(n) { mutableListOf<Int>() }
    var i = 0
    while (i < prereqs.size) { graph[prereqs[i]].add(prereqs[i + 1]); indegree[prereqs[i + 1]] += 1; i += 2 }
    var layer = (0 until n).filter { indegree[it] == 0 }
    var taken = 0
    var semesters = 0
    while (layer.isNotEmpty()) {
        taken += layer.size
        val nextLayer = mutableListOf<Int>()
        for (node in layer) for (after in graph[node]) { indegree[after] -= 1; if (indegree[after] == 0) nextLayer.add(after) }
        if (nextLayer.isNotEmpty()) semesters += 1
        layer = nextLayer
    }
    return if (taken == n) semesters else -1
}
"""),
        ("rescans-indegrees--per-semester", "PERFORMANCE",
         "학기마다 모든 관계를 다시 훑어 들을 수 있는 과목을 찾는다. O(학기 × 관계).",
         """
fun minSemesters(n: Int, prereqs: IntArray): Int {
    val taken = BooleanArray(n)
    var count = 0
    var semesters = 0
    while (count < n) {
        val ready = mutableListOf<Int>()
        for (v in 0 until n) {
            if (taken[v]) continue
            var ok = true
            var i = 0
            while (i < prereqs.size) {
                Drill.compare(v, i / 2)
                if (prereqs[i + 1] == v && !taken[prereqs[i]]) { ok = false; break }
                i += 2
            }
            if (ok) ready.add(v)
        }
        if (ready.isEmpty()) return -1
        for (v in ready) taken[v] = true
        count += ready.size
        semesters += 1
    }
    return semesters
}
"""),
    ],
))


# --- 54. 가장 싼 길 (다익스트라) ----------------------------------------------------

def _cheapest_paths(n, edges, source):
    import heapq
    graph = [[] for _ in range(n)]
    for i in range(0, len(edges), 3):
        a, b, w = edges[i], edges[i + 1], edges[i + 2]
        graph[a].append((b, w))
        graph[b].append((a, w))
    dist = [-1] * n
    dist[source] = 0
    heap = [(0, source)]
    while heap:
        d, node = heapq.heappop(heap)
        if d > dist[node]:
            continue
        for nxt, w in graph[node]:
            nd = d + w
            if dist[nxt] == -1 or nd < dist[nxt]:
                dist[nxt] = nd
                heapq.heappush(heap, (nd, nxt))
    return dist


def _weighted_chain(n, salt):
    weights = randoms(n - 1, 1, 100, salt=salt)
    return flat([i, i + 1, weights[i]] for i in range(n - 1))


def _weighted_random(n, m, salt):
    a = randoms(m, 0, n - 1, salt=salt)
    b = randoms(m, 0, n - 1, salt=salt + 1)
    w = randoms(m, 1, 1000, salt=salt + 2)
    return flat([a[i], b[i], w[i]] for i in range(m))


PROBLEMS.append(Problem(
    id="cheapest-paths",
    title="가장 싼 길",
    summary="""
정점이 `0` 부터 `n-1` 까지 있는 **무방향 가중 그래프**가 주어진다. `edges` 는 간선을
평탄하게 이은 배열로 `[a1, b1, w1, a2, b2, w2, ...]` 이며, `w` 는 그 간선의 비용이다.
비용은 항상 `1` 이상이다.

`source` 에서 각 정점까지의 **최소 비용**을 담은 길이 `n` 의 배열을 반환한다. 갈 수
없는 정점은 `-1` 이다.
""",
    notes="""
비용이 다르면 간선 수가 적은 길이 싼 길이 아니다. "지금까지 찾은 가장 싼 정점"을 먼저
확정해 나가면 되고, 그 정점을 빨리 꺼내는 구조가 힙이다. 같은 정점이 힙에 여러 번 들어갈
수 있으니, 꺼낸 값이 이미 확정된 값보다 크면 지나간다.
""",
    drill_doc="""
Drill.node("v3")              // 정점의 비용을 확정했다
Drill.edge("v3", "v7")        // 간선으로 이웃의 비용을 낮췄다
Drill.write(v, cost)          // 정점의 비용을 적었다
""",
    constraints="""
- `1 <= n <= 100_000`
- `edges.size` 는 3 의 배수이며 `0 <= edges.size <= 600_000`
- `1 <= w <= 1000`, 모든 비용의 합은 `Int` 범위 안이다
- `0 <= source < n`
""",
    signature=dict(
        name="cheapestPaths",
        parameters=[("n", "INT"), ("edges", "INT_ARRAY"), ("source", "INT")],
        returns="INT_ARRAY",
    ),
    groups=perf_groups(),
    reference=_cheapest_paths,
    limits={"timeMillis": 2000, "memoryMb": 256, "outputBytes": 2000000},
    cases={
        "sample": [
            ("01", [4, [0, 1, 4, 0, 2, 1, 2, 1, 2, 1, 3, 5], 0]),
            ("02", [3, [0, 1, 7], 0]),
        ],
        "boundary": [
            ("01-single", [1, [], 0]),
            # 간선 수는 적지만 비싼 길과, 간선 수는 많지만 싼 길.
            ("02-hops-vs-cost", [4, [0, 3, 10, 0, 1, 1, 1, 2, 1, 2, 3, 1], 0]),
            ("03-unreachable", [4, [0, 1, 3, 2, 3, 3], 0]),
            # 같은 두 정점 사이에 간선이 둘. 싼 쪽이다.
            ("04-parallel-edges", [2, [0, 1, 9, 0, 1, 2], 0]),
            # 출발점이 0 이 아니다.
            ("05-source-not-zero", [3, [0, 1, 5, 1, 2, 5], 2]),
            # 더 싼 길이 나중에 발견된다. 처음 적은 값을 고치지 않으면 틀린다.
            ("06-relax-later", [4, [0, 1, 1, 0, 2, 5, 1, 2, 1, 2, 3, 1], 0]),
        ],
        "hidden": [
            ("01-random-small", [8, _weighted_random(8, 14, salt=1201), 0]),
            ("02-random-medium", [50, _weighted_random(50, 120, salt=1204), 7]),
            ("03-chain", [10, _weighted_chain(10, salt=1207), 9]),
            ("04-star", [6, [0, 1, 1, 0, 2, 2, 0, 3, 3, 0, 4, 4, 0, 5, 5], 3]),
        ],
        "performance": [
            ("01-small", [3000, _weighted_chain(3000, salt=1211), 0]),
            ("02-medium", [20000, _weighted_chain(20000, salt=1212), 0]),
            ("03-large", [100000, _weighted_chain(100000, salt=1213), 0]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 힙으로 가장 싼 정점부터 확정한다.
fun cheapestPaths(n: Int, edges: IntArray, source: Int): IntArray {
    val m = edges.size / 3
    val head = IntArray(n) { -1 }
    val next = IntArray(2 * m)
    val to = IntArray(2 * m)
    val cost = IntArray(2 * m)
    var e = 0
    var i = 0
    while (i < edges.size) {
        val a = edges[i]; val b = edges[i + 1]; val w = edges[i + 2]
        to[e] = b; cost[e] = w; next[e] = head[a]; head[a] = e; e += 1
        to[e] = a; cost[e] = w; next[e] = head[b]; head[b] = e; e += 1
        i += 3
    }
    val dist = IntArray(n) { -1 }
    dist[source] = 0
    val heap = java.util.PriorityQueue<LongArray>(compareBy { it[0] })
    heap.add(longArrayOf(0L, source.toLong()))
    while (heap.isNotEmpty()) {
        val top = heap.poll()
        val d = top[0].toInt()
        val node = top[1].toInt()
        if (d > dist[node]) continue
        Drill.node("v$node")
        var edge = head[node]
        while (edge != -1) {
            val nxt = to[edge]
            val nd = d + cost[edge]
            if (dist[nxt] == -1 || nd < dist[nxt]) {
                dist[nxt] = nd
                Drill.edge("v$node", "v$nxt")
                Drill.write(nxt, nd)
                heap.add(longArrayOf(nd.toLong(), nxt.toLong()))
            }
            edge = next[edge]
        }
    }
    return dist
}
""",
    mutants=[
        ("bfs-ignores-weights", "WRONG_ALGORITHM",
         "비용을 보지 않고 간선 수로 고른다. 간선은 적지만 비싼 길을 답으로 삼는다.",
         """
fun cheapestPaths(n: Int, edges: IntArray, source: Int): IntArray {
    val graph = Array(n) { mutableListOf<Pair<Int, Int>>() }
    var i = 0
    while (i < edges.size) {
        graph[edges[i]].add(edges[i + 1] to edges[i + 2]); graph[edges[i + 1]].add(edges[i] to edges[i + 2]); i += 3
    }
    val dist = IntArray(n) { -1 }
    dist[source] = 0
    val queue = ArrayDeque<Int>()
    queue.addLast(source)
    while (queue.isNotEmpty()) {
        val node = queue.removeFirst()
        for ((nxt, w) in graph[node]) {
            if (dist[nxt] != -1) continue
            dist[nxt] = dist[node] + w
            queue.addLast(nxt)
        }
    }
    return dist
}
"""),
        ("first-visit-final--no-relax", "WRONG_BRANCH",
         "처음 적은 비용을 다시 낮추지 않는다. 더 싼 길이 나중에 발견되면 틀린다.",
         """
fun cheapestPaths(n: Int, edges: IntArray, source: Int): IntArray {
    val graph = Array(n) { mutableListOf<Pair<Int, Int>>() }
    var i = 0
    while (i < edges.size) {
        graph[edges[i]].add(edges[i + 1] to edges[i + 2]); graph[edges[i + 1]].add(edges[i] to edges[i + 2]); i += 3
    }
    val dist = IntArray(n) { -1 }
    dist[source] = 0
    val heap = java.util.PriorityQueue<LongArray>(compareBy { it[0] })
    heap.add(longArrayOf(0L, source.toLong()))
    while (heap.isNotEmpty()) {
        val top = heap.poll()
        val node = top[1].toInt()
        for ((nxt, w) in graph[node]) {
            if (dist[nxt] != -1) continue
            dist[nxt] = dist[node] + w
            heap.add(longArrayOf(dist[nxt].toLong(), nxt.toLong()))
        }
    }
    return dist
}
"""),
        ("directed-only--one-way", "MISSING_EDGE_CASE",
         "간선을 한 방향으로만 넣는다. 무방향인데 되돌아가지 못한다.",
         """
fun cheapestPaths(n: Int, edges: IntArray, source: Int): IntArray {
    val graph = Array(n) { mutableListOf<Pair<Int, Int>>() }
    var i = 0
    while (i < edges.size) { graph[edges[i]].add(edges[i + 1] to edges[i + 2]); i += 3 }
    val dist = IntArray(n) { -1 }
    dist[source] = 0
    val heap = java.util.PriorityQueue<LongArray>(compareBy { it[0] })
    heap.add(longArrayOf(0L, source.toLong()))
    while (heap.isNotEmpty()) {
        val top = heap.poll()
        val d = top[0].toInt(); val node = top[1].toInt()
        if (d > dist[node]) continue
        for ((nxt, w) in graph[node]) {
            if (dist[nxt] == -1 || d + w < dist[nxt]) { dist[nxt] = d + w; heap.add(longArrayOf(dist[nxt].toLong(), nxt.toLong())) }
        }
    }
    return dist
}
"""),
        ("array-scan--quadratic", "PERFORMANCE",
         "힙 없이 매번 모든 정점을 훑어 가장 싼 미확정 정점을 고른다. O(n²).",
         """
fun cheapestPaths(n: Int, edges: IntArray, source: Int): IntArray {
    val graph = Array(n) { mutableListOf<Pair<Int, Int>>() }
    var i = 0
    while (i < edges.size) {
        graph[edges[i]].add(edges[i + 1] to edges[i + 2]); graph[edges[i + 1]].add(edges[i] to edges[i + 2]); i += 3
    }
    val dist = IntArray(n) { -1 }
    val done = BooleanArray(n)
    dist[source] = 0
    repeat(n) {
        var best = -1
        for (v in 0 until n) {
            Drill.compare(v, best)
            if (!done[v] && dist[v] != -1 && (best == -1 || dist[v] < dist[best])) best = v
        }
        if (best == -1) return dist
        done[best] = true
        for ((nxt, w) in graph[best]) {
            if (dist[nxt] == -1 || dist[best] + w < dist[nxt]) dist[nxt] = dist[best] + w
        }
    }
    return dist
}
"""),
    ],
))


# --- 76. 모두 잇는 최소 비용 (크루스칼) -------------------------------------------------

def _min_spanning_cost(n, edges):
    parent = list(range(n))

    def find(x):
        while parent[x] != x:
            parent[x] = parent[parent[x]]
            x = parent[x]
        return x

    order = sorted(range(len(edges) // 3), key=lambda i: edges[3 * i + 2])
    total = 0
    joined = 0
    for i in order:
        a, b, w = edges[3 * i], edges[3 * i + 1], edges[3 * i + 2]
        ra, rb = find(a), find(b)
        if ra != rb:
            parent[ra] = rb
            total += w
            joined += 1
            if joined == n - 1:
                break
    return total if joined == n - 1 else -1


def _spanning_edges(n, extra, salt):
    """연결된 무작위 그래프. 사슬로 먼저 잇고 간선을 더한다."""
    chain_w = randoms(n - 1, 1, 1000, salt=salt)
    a = randoms(extra, 0, n - 1, salt=salt + 1)
    b = randoms(extra, 0, n - 1, salt=salt + 2)
    w = randoms(extra, 1, 1000, salt=salt + 3)
    flat = []
    for i in range(n - 1):
        flat += [i, i + 1, chain_w[i]]
    for i in range(extra):
        flat += [a[i], b[i], w[i]]
    return flat


PROBLEMS.append(Problem(
    id="min-spanning-cost",
    title="모두 잇는 최소 비용",
    summary="""
정점이 `0` 부터 `n-1` 까지 있는 무방향 가중 그래프가 주어진다. `edges` 는
`[a1, b1, w1, a2, b2, w2, ...]` 이며 `w` 는 그 간선을 쓰는 비용이다.

**모든 정점이 서로 이어지도록** 간선을 고를 때의 최소 총비용을 반환한다. 이을 수 없으면
`-1` 이다. 정점이 하나면 `0` 이다.
""",
    notes="""
싼 간선부터 보되, **이미 이어진 두 정점을 또 잇는 간선은 버린다.** 그 판단을 빠르게 하는
구조가 유니온 파인드다. 간선 n-1 개를 골랐으면 끝이고, 다 보고도 모자라면 이을 수 없는
것이다.
""",
    drill_doc="""
Drill.edge("v3", "v7")        // 간선을 골랐다
Drill.match(a, b)             // 두 묶음을 합쳤다 (대표 정점)
Drill.write(0, total)         // 지금까지의 비용
""",
    constraints="""
- `1 <= n <= 100_000`
- `edges.size` 는 3 의 배수이며 `0 <= edges.size <= 600_000`
- `1 <= w <= 1000`, 총비용은 `Int` 범위 안
- 같은 두 정점 사이에 간선이 여럿일 수 있고, 자기 자신을 잇는 간선도 있을 수 있다
""",
    signature=dict(
        name="minSpanningCost",
        parameters=[("n", "INT"), ("edges", "INT_ARRAY")],
        returns="INT",
    ),
    groups=perf_groups(),
    reference=_min_spanning_cost,
    cases={
        "sample": [
            ("01", [4, [0, 1, 1, 1, 2, 2, 2, 3, 3, 0, 3, 10, 0, 2, 5]]),
            ("02", [3, [0, 1, 4]]),
        ],
        "boundary": [
            ("01-single", [1, []]),
            ("02-two-no-edge", [2, []]),
            # 같은 두 정점 사이의 간선이 여럿. 싼 것 하나만 쓴다.
            ("03-parallel", [2, [0, 1, 7, 0, 1, 3, 0, 1, 9]]),
            # 자기 자신을 잇는 간선. 아무것도 잇지 않는다.
            ("04-self-loop", [2, [0, 0, 1, 0, 1, 5]]),
            # 싼 간선이 순환을 만든다. 순환을 막지 않으면 비용이 는다.
            ("05-cheap-cycle", [3, [0, 1, 1, 1, 2, 1, 2, 0, 1, 0, 2, 100]]),
            # 두 덩어리가 이어지지 않는다.
            ("06-disconnected", [4, [0, 1, 1, 2, 3, 1]]),
            # 가장 싼 간선 n-1 개가 답이 아니다 — 그것들이 순환을 만든다.
            ("07-cheapest-cycle", [4, [0, 1, 1, 1, 2, 1, 2, 0, 1, 2, 3, 10]]),
        ],
        "hidden": [
            ("01-random-small", [10, _spanning_edges(10, 15, salt=3901)]),
            ("02-random-medium", [500, _spanning_edges(500, 2000, salt=3905)]),
            ("03-star", [6, [0, 1, 5, 0, 2, 4, 0, 3, 3, 0, 4, 2, 0, 5, 1]]),
            ("04-disconnected-big", [50, _spanning_edges(25, 30, salt=3909) + [30, 31, 1]]),
        ],
        "performance": [
            ("01-small", [5000, _spanning_edges(5000, 10000, salt=3913)]),
            ("02-medium", [30000, _spanning_edges(30000, 60000, salt=3917)]),
            ("03-large", [100000, _spanning_edges(100000, 100000, salt=3921)]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 크루스칼 — 싼 간선부터, 유니온 파인드로 순환을 막는다.
fun minSpanningCost(n: Int, edges: IntArray): Int {
    val m = edges.size / 3
    val order = (0 until m).sortedBy { edges[3 * it + 2] }
    val parent = IntArray(n) { it }
    fun find(x: Int): Int {
        var v = x
        while (parent[v] != v) { parent[v] = parent[parent[v]]; v = parent[v] }
        return v
    }
    var total = 0
    var joined = 0
    for (i in order) {
        if (joined == n - 1) break
        val a = edges[3 * i]
        val b = edges[3 * i + 1]
        val ra = find(a)
        val rb = find(b)
        if (ra == rb) continue
        parent[ra] = rb
        Drill.match(ra, rb)
        Drill.edge("v$a", "v$b")
        total += edges[3 * i + 2]
        joined += 1
        Drill.write(0, total)
    }
    return if (joined == n - 1) total else -1
}
""",
    mutants=[
        ("cheapest-n-minus-one", "WRONG_ALGORITHM",
         "가장 싼 간선 n-1 개의 비용을 더한다. 그것들이 순환을 만들면 정점을 잇지 못한다.",
         """
fun minSpanningCost(n: Int, edges: IntArray): Int {
    val m = edges.size / 3
    if (m < n - 1) return -1
    val weights = (0 until m).map { edges[3 * it + 2] }.sorted()
    return weights.take(n - 1).sum()
}
"""),
        ("ignores-disconnected", "MISSING_EDGE_CASE",
         "간선을 다 보고도 n-1 개를 못 골랐는데 비용을 돌려준다. 이을 수 없으면 -1 이다.",
         """
fun minSpanningCost(n: Int, edges: IntArray): Int {
    val m = edges.size / 3
    val order = (0 until m).sortedBy { edges[3 * it + 2] }
    val parent = IntArray(n) { it }
    fun find(x: Int): Int { var v = x; while (parent[v] != v) v = parent[v]; return v }
    var total = 0
    for (i in order) {
        val ra = find(edges[3 * i]); val rb = find(edges[3 * i + 1])
        if (ra == rb) continue
        parent[ra] = rb
        total += edges[3 * i + 2]
    }
    return total
}
"""),
        ("no-union--never-merges", "WRONG_BRANCH",
         "두 정점이 같은 묶음인지 보지만 합치지는 않는다. 자기 자신을 잇는 간선 말고는 전부 고른다.",
         """
fun minSpanningCost(n: Int, edges: IntArray): Int {
    val m = edges.size / 3
    val order = (0 until m).sortedBy { edges[3 * it + 2] }
    val parent = IntArray(n) { it }
    var total = 0
    var joined = 0
    for (i in order) {
        if (joined == n - 1) break
        if (parent[edges[3 * i]] == parent[edges[3 * i + 1]] && edges[3 * i] == edges[3 * i + 1]) continue
        total += edges[3 * i + 2]
        joined += 1
    }
    return if (joined == n - 1) total else -1
}
"""),
        ("dfs-cycle-check--per-edge", "PERFORMANCE",
         "간선을 고를 때마다 지금까지 고른 간선으로 DFS 해 이미 이어졌는지 본다. O(E · V).",
         """
fun minSpanningCost(n: Int, edges: IntArray): Int {
    val m = edges.size / 3
    val order = (0 until m).sortedBy { edges[3 * it + 2] }
    val adj = Array(n) { mutableListOf<Int>() }
    val seen = BooleanArray(n)
    fun connected(a: Int, b: Int): Boolean {
        java.util.Arrays.fill(seen, false)
        val stack = ArrayDeque<Int>(); stack.addLast(a); seen[a] = true
        while (stack.isNotEmpty()) {
            val v = stack.removeLast()
            Drill.visit(v, 0)
            if (v == b) return true
            for (u in adj[v]) if (!seen[u]) { seen[u] = true; stack.addLast(u) }
        }
        return false
    }
    var total = 0
    var joined = 0
    for (i in order) {
        if (joined == n - 1) break
        val a = edges[3 * i]; val b = edges[3 * i + 1]
        if (connected(a, b)) continue
        adj[a].add(b); adj[b].add(a)
        total += edges[3 * i + 2]
        joined += 1
    }
    return if (joined == n - 1) total else -1
}
"""),
    ],
))


# --- 92. 임계 경로 (가중 위상 정렬) -----------------------------------------------------

def _critical_path(durations, deps):
    n = len(durations)
    after = [[] for _ in range(n)]
    indeg = [0] * n
    for i in range(0, len(deps), 2):
        a, b = deps[i], deps[i + 1]
        after[a].append(b)
        indeg[b] += 1
    finish = [0] * n
    ready = [v for v in range(n) if indeg[v] == 0]
    seen = 0
    while ready:
        v = ready.pop()
        seen += 1
        finish[v] += durations[v]
        for u in after[v]:
            finish[u] = max(finish[u], finish[v])
            indeg[u] -= 1
            if indeg[u] == 0:
                ready.append(u)
    return max(finish) if seen == n else -1


def _dag_deps(n, extra, salt):
    """작은 번호에서 큰 번호로만 가는 간선. 순환이 없다."""
    a = randoms(extra, 0, n - 2, salt=salt)
    span = randoms(extra, 1, 5, salt=salt + 1)
    return flat([a[i], min(n - 1, a[i] + span[i])] for i in range(extra))


PROBLEMS.append(Problem(
    id="critical-path",
    title="모든 일을 마치는 최소 시간",
    summary="""
일이 `0` 부터 `n-1` 까지 있고 `durations[i]` 는 `i` 번 일에 걸리는 시간이다. `deps` 는
`[a1, b1, a2, b2, ...]` 로, "`a` 를 마쳐야 `b` 를 시작할 수 있다"는 뜻이다.

**동시에 몇 개든** 할 수 있을 때 모든 일을 마치는 데 걸리는 최소 시간을 반환한다.
순환이 있어 끝낼 수 없으면 `-1` 이다.

예: `durations = [3, 2, 4]`, `deps = [0, 2, 1, 2]` 면 0 과 1 을 동시에 시작해 3 에 끝나고,
2 는 3 에 시작해 7 에 끝난다 — `7`.
""",
    notes="""
일 하나의 끝나는 시각은 **선행 일들의 끝나는 시각 중 최댓값 + 자기 시간**이다. 선행 일이
먼저 계산돼 있어야 하므로 위상 순서로 돈다. 답은 모든 끝나는 시각의 최댓값 — 가장 긴
경로의 길이다. 학기 문제와 같은 순서, 다른 값이다.
""",
    drill_doc="""
Drill.node("t3")              // 일을 마쳤다
Drill.edge("t3", "t7")        // 선행 관계를 따라 끝나는 시각을 밀었다
Drill.write(i, finish)        // i 번 일이 끝나는 시각
""",
    constraints="""
- `1 <= n <= 100_000`
- `deps.size` 는 짝수, `0 <= deps.size <= 400_000`
- `1 <= durations[i] <= 1000`, 답은 `Int` 범위 안
- 같은 관계가 여러 번 나올 수 있다
""",
    signature=dict(name="criticalPath", parameters=[("durations", "INT_ARRAY"), ("deps", "INT_ARRAY")],
                   returns="INT"),
    groups=perf_groups(),
    reference=_critical_path,
    cases={
        "sample": [
            ("01", [[3, 2, 4], [0, 2, 1, 2]]),
            ("02", [[5], []]),
        ],
        "boundary": [
            # 관계가 없다. 전부 동시에 — 가장 긴 일 하나.
            ("01-no-deps", [[1, 9, 4], []]),
            ("02-chain", [[1, 2, 3, 4], [0, 1, 1, 2, 2, 3]]),
            ("03-cycle", [[1, 1], [0, 1, 1, 0]]),
            ("04-self-cycle", [[1, 1], [1, 1]]),
            # 짧은 선행이 여럿 — 최댓값이지 합이 아니다.
            ("05-fan-in", [[1, 5, 2, 1], [0, 3, 1, 3, 2, 3]]),
            # 같은 관계가 두 번. 진입 차수를 두 번 세면 시작하지 못한다.
            ("06-duplicate-dep", [[2, 3], [0, 1, 0, 1]]),
            # 뒤 번호가 앞 번호의 선행이다. 번호 순서를 믿으면 틀린다.
            ("07-reverse-numbering", [[2, 3, 4], [2, 1, 1, 0]]),
        ],
        "hidden": [
            ("01-random-small", [randoms(12, 1, 20, salt=4801), _dag_deps(12, 15, salt=4802)]),
            ("02-random-medium", [randoms(300, 1, 100, salt=4803), _dag_deps(300, 600, salt=4804)]),
            ("03-random-cycle", [randoms(50, 1, 10, salt=4805), _dag_deps(50, 60, salt=4806) + [49, 10]]),
            ("04-diamond", [[1, 10, 1, 1], [0, 1, 0, 2, 1, 3, 2, 3]]),
        ],
        "performance": [
            ("01-small", [randoms(5000, 1, 1000, salt=4807), _dag_deps(5000, 10000, salt=4808)]),
            ("02-medium", [randoms(30000, 1, 1000, salt=4809), _dag_deps(30000, 100000, salt=4810)]),
            ("03-large", [randoms(100000, 1, 1000, salt=4811), _dag_deps(100000, 200000, salt=4812)]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 칸 알고리즘으로 위상 순서를 돌며 끝나는 시각을 민다.
fun criticalPath(durations: IntArray, deps: IntArray): Int {
    val n = durations.size
    val after = Array(n) { ArrayList<Int>() }
    val indeg = IntArray(n)
    for (i in deps.indices step 2) { after[deps[i]].add(deps[i + 1]); indeg[deps[i + 1]] += 1 }
    val finish = IntArray(n)
    val ready = ArrayDeque<Int>()
    for (v in 0 until n) if (indeg[v] == 0) ready.addLast(v)
    var seen = 0
    var best = 0
    while (ready.isNotEmpty()) {
        val v = ready.removeLast()
        seen += 1
        finish[v] += durations[v]
        Drill.node("t$v")
        Drill.write(v, finish[v])
        best = maxOf(best, finish[v])
        for (u in after[v]) {
            Drill.edge("t$v", "t$u")
            finish[u] = maxOf(finish[u], finish[v])
            indeg[u] -= 1
            if (indeg[u] == 0) ready.addLast(u)
        }
    }
    return if (seen == n) best else -1
}
""",
    mutants=[
        ("sums-predecessors", "WRONG_ALGORITHM",
         "선행 일들의 끝나는 시각을 더한다. 동시에 하므로 최댓값이어야 한다.",
         """
fun criticalPath(durations: IntArray, deps: IntArray): Int {
    val n = durations.size
    val after = Array(n) { ArrayList<Int>() }
    val indeg = IntArray(n)
    for (i in deps.indices step 2) { after[deps[i]].add(deps[i + 1]); indeg[deps[i + 1]] += 1 }
    val finish = IntArray(n)
    val ready = ArrayDeque<Int>()
    for (v in 0 until n) if (indeg[v] == 0) ready.addLast(v)
    var seen = 0; var best = 0
    while (ready.isNotEmpty()) {
        val v = ready.removeLast(); seen += 1
        finish[v] += durations[v]; best = maxOf(best, finish[v])
        for (u in after[v]) { finish[u] += finish[v]; indeg[u] -= 1; if (indeg[u] == 0) ready.addLast(u) }
    }
    return if (seen == n) best else -1
}
"""),
        ("ignores-cycle", "MISSING_EDGE_CASE",
         "순환이 있어 시작하지 못한 일을 그냥 두고 지금까지의 최댓값을 답한다.",
         """
fun criticalPath(durations: IntArray, deps: IntArray): Int {
    val n = durations.size
    val after = Array(n) { ArrayList<Int>() }
    val indeg = IntArray(n)
    for (i in deps.indices step 2) { after[deps[i]].add(deps[i + 1]); indeg[deps[i + 1]] += 1 }
    val finish = IntArray(n)
    val ready = ArrayDeque<Int>()
    for (v in 0 until n) if (indeg[v] == 0) ready.addLast(v)
    var best = 0
    while (ready.isNotEmpty()) {
        val v = ready.removeLast()
        finish[v] += durations[v]; best = maxOf(best, finish[v])
        for (u in after[v]) { finish[u] = maxOf(finish[u], finish[v]); indeg[u] -= 1; if (indeg[u] == 0) ready.addLast(u) }
    }
    return best
}
"""),
        ("trusts-numbering", "WRONG_ALGORITHM",
         "번호 순서가 위상 순서라고 믿고 0 부터 차례로 민다.",
         """
fun criticalPath(durations: IntArray, deps: IntArray): Int {
    val n = durations.size
    val after = Array(n) { ArrayList<Int>() }
    for (i in deps.indices step 2) { if (deps[i] == deps[i + 1]) return -1; after[deps[i]].add(deps[i + 1]) }
    val finish = IntArray(n)
    var best = 0
    for (v in 0 until n) {
        finish[v] += durations[v]; best = maxOf(best, finish[v])
        for (u in after[v]) finish[u] = maxOf(finish[u], finish[v])
    }
    return best
}
"""),
        ("dfs-without-memo", "PERFORMANCE",
         "일마다 선행 일들을 재귀로 다시 계산한다. 마름모가 겹치면 지수적이다.",
         """
fun criticalPath(durations: IntArray, deps: IntArray): Int {
    val n = durations.size
    val before = Array(n) { ArrayList<Int>() }
    for (i in deps.indices step 2) before[deps[i + 1]].add(deps[i])
    val onPath = BooleanArray(n)
    var cyclic = false
    fun finish(v: Int): Int {
        if (onPath[v]) { cyclic = true; return 0 }
        onPath[v] = true
        var best = 0
        for (u in before[v]) { Drill.edge("t$u", "t$v"); best = maxOf(best, finish(u)) }
        onPath[v] = false
        return best + durations[v]
    }
    var best = 0
    for (v in 0 until n) { best = maxOf(best, finish(v)); if (cyclic) return -1 }
    return best
}
"""),
    ],
))


# --- 93. 경유 k번 이내의 가장 싼 항공편 (벨만-포드) ------------------------------------------

def _cheapest_with_stops(n, flights, src, dst, k):
    INF = float("inf")
    dist = [INF] * n
    dist[src] = 0
    for _ in range(k + 1):
        nxt = dist[:]
        for i in range(0, len(flights), 3):
            a, b, w = flights[i], flights[i + 1], flights[i + 2]
            if dist[a] != INF and dist[a] + w < nxt[b]:
                nxt[b] = dist[a] + w
        dist = nxt
    return -1 if dist[dst] == INF else dist[dst]


def _flights(n, m, salt):
    a = randoms(m, 0, n - 1, salt=salt)
    b = randoms(m, 0, n - 1, salt=salt + 1)
    w = randoms(m, 1, 1000, salt=salt + 2)
    return flat([a[i], b[i], w[i]] for i in range(m) if a[i] != b[i])


PROBLEMS.append(Problem(
    id="cheapest-with-stops",
    title="경유 k번 이내의 가장 싼 항공편",
    summary="""
도시가 `0` 부터 `n-1` 까지 있고 `flights` 는 `[a1, b1, w1, a2, b2, w2, ...]` — `a` 에서 `b` 로
가는 **편도** 항공편의 요금 `w` 다. `src` 에서 `dst` 까지 **경유를 `k` 번 이하**로 하는
가장 싼 요금을 반환한다. 없으면 `-1` 이다. 경유 `k` 번은 항공편 `k+1` 편이다.

예: `n = 4`, `flights = [0,1,100, 1,2,100, 2,0,100, 1,3,600, 2,3,200]`, `src = 0`,
`dst = 3`, `k = 1` 이면 `0→1→3` 이 `700` 이다. `0→1→2→3` 은 `400` 이지만 경유가 둘이다.
""",
    notes="""
가장 싼 길이 편수 제한을 넘을 수 있으므로 보통의 최단 경로로는 안 된다. "편 수 `i` 이하로
갈 때의 최소 요금"을 `i` 를 늘려 가며 갱신하면 `k+1` 번의 갱신으로 끝난다 — 벨만-포드의
반복 횟수를 제한한 것이다. **한 반복 안에서 방금 갱신한 값을 다시 쓰면** 편 수가 새므로,
반복마다 이전 표를 따로 둔다.
""",
    drill_doc="""
Drill.edge("c1", "c3")        // 항공편으로 갱신했다
Drill.write(city, cost)       // 그 도시까지의 요금
""",
    constraints="""
- `1 <= n <= 1_000`, `0 <= flights.size / 3 <= 20_000`
- `0 <= k < n`, `1 <= w <= 1000`, `src != dst`
- 같은 두 도시 사이에 항공편이 여럿일 수 있다
""",
    signature=dict(
        name="cheapestWithStops",
        parameters=[("n", "INT"), ("flights", "INT_ARRAY"), ("src", "INT"), ("dst", "INT"), ("k", "INT")],
        returns="INT",
    ),
    groups=standard_groups(),
    reference=_cheapest_with_stops,
    cases={
        "sample": [
            ("01", [4, [0, 1, 100, 1, 2, 100, 2, 0, 100, 1, 3, 600, 2, 3, 200], 0, 3, 1]),
            ("02", [3, [0, 1, 100, 1, 2, 100, 0, 2, 500], 0, 2, 0]),
        ],
        "boundary": [
            ("01-unreachable", [3, [0, 1, 5], 0, 2, 2]),
            # 직항이 있지만 경유가 싸다. k 가 허락하면 경유.
            ("02-direct-vs-via", [3, [0, 1, 100, 1, 2, 100, 0, 2, 500], 0, 2, 1]),
            # k = 0 이면 직항만.
            ("03-direct-only", [3, [0, 1, 1, 1, 2, 1, 0, 2, 10], 0, 2, 0]),
            # 한 반복 안에서 갱신을 이어 쓰면 편 수가 샌다: 0→1→2→3 을 k=1 로 잡는다.
            ("04-relaxation-leaks", [4, [0, 1, 1, 1, 2, 1, 2, 3, 1, 0, 3, 10], 0, 3, 1]),
            # 병렬 항공편. 싼 것을 쓴다.
            ("05-parallel", [2, [0, 1, 9, 0, 1, 3], 0, 1, 0]),
            # 싼 길이 편수 제한을 넘는다 — 비싼 짧은 길이 답이다.
            ("06-cheap-too-long", [5, [0, 1, 1, 1, 2, 1, 2, 3, 1, 3, 4, 1, 0, 4, 50], 0, 4, 2]),
            ("07-no-flights", [2, [], 0, 1, 1]),
        ],
        "hidden": [
            ("01-random-small", [8, _flights(8, 20, salt=4901), 0, 7, 2]),
            ("02-random-medium", [50, _flights(50, 300, salt=4904), 3, 47, 4]),
            ("03-random-large-k", [200, _flights(200, 2000, salt=4907), 0, 199, 199]),
            ("04-random-k-zero", [200, _flights(200, 2000, salt=4910), 5, 6, 0]),
            ("05-random-big", [1000, _flights(1000, 20000, salt=4913), 1, 999, 10]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 반복 횟수를 k+1 로 제한한 벨만-포드. 반복마다 이전 표를 따로 둔다.
fun cheapestWithStops(n: Int, flights: IntArray, src: Int, dst: Int, k: Int): Int {
    val inf = Int.MAX_VALUE / 2
    var dist = IntArray(n) { inf }
    dist[src] = 0
    repeat(k + 1) {
        val next = dist.copyOf()
        for (i in flights.indices step 3) {
            val a = flights[i]; val b = flights[i + 1]; val w = flights[i + 2]
            if (dist[a] != inf && dist[a] + w < next[b]) {
                next[b] = dist[a] + w
                Drill.edge("c$a", "c$b")
                Drill.write(b, next[b])
            }
        }
        dist = next
    }
    return if (dist[dst] == inf) -1 else dist[dst]
}
""",
    mutants=[
        ("relaxes-in-place", "WRONG_ALGORITHM",
         "한 반복 안에서 방금 갱신한 값을 이어 쓴다. 편 수가 샌다.",
         """
fun cheapestWithStops(n: Int, flights: IntArray, src: Int, dst: Int, k: Int): Int {
    val inf = Int.MAX_VALUE / 2
    val dist = IntArray(n) { inf }
    dist[src] = 0
    repeat(k + 1) {
        for (i in flights.indices step 3) {
            val a = flights[i]; val b = flights[i + 1]; val w = flights[i + 2]
            if (dist[a] != inf && dist[a] + w < dist[b]) dist[b] = dist[a] + w
        }
    }
    return if (dist[dst] == inf) -1 else dist[dst]
}
"""),
        ("k-iterations", "OFF_BY_ONE",
         "k 번만 반복한다. 경유 k 번은 항공편 k+1 편이다.",
         """
fun cheapestWithStops(n: Int, flights: IntArray, src: Int, dst: Int, k: Int): Int {
    val inf = Int.MAX_VALUE / 2
    var dist = IntArray(n) { inf }
    dist[src] = 0
    repeat(k) {
        val next = dist.copyOf()
        for (i in flights.indices step 3) {
            val a = flights[i]; val b = flights[i + 1]; val w = flights[i + 2]
            if (dist[a] != inf && dist[a] + w < next[b]) next[b] = dist[a] + w
        }
        dist = next
    }
    return if (dist[dst] == inf) -1 else dist[dst]
}
"""),
        ("plain-dijkstra--ignores-k", "WRONG_ALGORITHM",
         "편 수 제한을 무시하고 가장 싼 길을 답한다.",
         """
fun cheapestWithStops(n: Int, flights: IntArray, src: Int, dst: Int, k: Int): Int {
    val inf = Int.MAX_VALUE / 2
    val adj = Array(n) { ArrayList<IntArray>() }
    for (i in flights.indices step 3) adj[flights[i]].add(intArrayOf(flights[i + 1], flights[i + 2]))
    val dist = IntArray(n) { inf }
    dist[src] = 0
    val heap = java.util.PriorityQueue<IntArray>(compareBy { it[1] })
    heap.add(intArrayOf(src, 0))
    while (heap.isNotEmpty()) {
        val (v, d) = heap.poll()
        if (d > dist[v]) continue
        for (e in adj[v]) if (d + e[1] < dist[e[0]]) { dist[e[0]] = d + e[1]; heap.add(intArrayOf(e[0], dist[e[0]])) }
    }
    return if (dist[dst] == inf) -1 else dist[dst]
}
"""),
        ("stops-at-first-reach", "WRONG_BRANCH",
         "목적지에 처음 닿은 반복에서 멈춘다. 더 많은 편으로 더 싼 길이 있을 수 있다.",
         """
fun cheapestWithStops(n: Int, flights: IntArray, src: Int, dst: Int, k: Int): Int {
    val inf = Int.MAX_VALUE / 2
    var dist = IntArray(n) { inf }
    dist[src] = 0
    repeat(k + 1) {
        val next = dist.copyOf()
        for (i in flights.indices step 3) {
            val a = flights[i]; val b = flights[i + 1]; val w = flights[i + 2]
            if (dist[a] != inf && dist[a] + w < next[b]) next[b] = dist[a] + w
        }
        dist = next
        if (dist[dst] != inf) return dist[dst]
    }
    return if (dist[dst] == inf) -1 else dist[dst]
}
"""),
    ],
))


# --- 98. 등식과 부등식이 모순 없는가 (유니온 파인드) ---------------------------------------------

def _equations_possible(equations):
    parent = list(range(26))

    def find(x):
        while parent[x] != x:
            parent[x] = parent[parent[x]]
            x = parent[x]
        return x

    for eq in equations:
        if eq[1] == "=":
            parent[find(ord(eq[0]) - 97)] = find(ord(eq[3]) - 97)
    for eq in equations:
        if eq[1] == "!" and find(ord(eq[0]) - 97) == find(ord(eq[3]) - 97):
            return 0
    return 1


def _equation_set(count, salt, contradict):
    a = randoms(count, 0, 25, salt=salt)
    b = randoms(count, 0, 25, salt=salt + 1)
    out = [f"{chr(97 + a[i])}=={chr(97 + b[i])}" for i in range(count)]
    if contradict:
        out.append(f"{chr(97 + a[0])}!={chr(97 + b[0])}")
    else:
        # 등식으로 이어지지 않은 두 글자 — 마지막 두 글자를 서로 다른 것으로 잡는다.
        out.append("y!=z")
    return out


PROBLEMS.append(Problem(
    id="equations-possible",
    title="등식과 부등식이 모순 없는가",
    summary="""
`"a==b"` 또는 `"a!=b"` 꼴의 식 배열이 주어진다 (소문자 한 글자씩). 모든 식을 동시에
만족하는 값 배정이 있으면 `1`, 없으면 `0` 을 반환한다.

예: `["a==b", "b!=a"]` 는 `0`, `["b==a", "a==b"]` 는 `1`, `["a==b", "b==c", "a!=c"]` 는 `0`.
""",
    notes="""
등식은 "같은 묶음"이고 전이된다 — a==b, b==c 면 a 와 c 도 같다. 등식을 **먼저 전부** 유니온
파인드로 합친 뒤, 부등식마다 두 글자가 같은 묶음인지 보면 된다. 순서대로 처리하면 나중에
오는 등식이 앞의 부등식을 깨뜨리는 것을 놓친다.
""",
    drill_doc="""
Drill.match(a, b)             // 두 묶음을 합쳤다
Drill.compare(a, b)           // 부등식을 확인했다
""",
    constraints="""
- `1 <= equations.size <= 100_000`
- 각 식은 정확히 4 글자: `x==y` 또는 `x!=y`, `x`, `y` 는 소문자
""",
    signature=dict(name="equationsPossible", parameters=[("equations", "STRING_ARRAY")], returns="INT"),
    groups=standard_groups(),
    reference=_equations_possible,
    cases={
        "sample": [
            ("01", [["a==b", "b!=a"]]),
            ("02", [["b==a", "a==b"]]),
        ],
        "boundary": [
            # 자기 자신과 다르다 — 항상 모순.
            ("01-self-not-equal", [["a!=a"]]),
            ("02-self-equal", [["a==a"]]),
            # 부등식이 등식보다 먼저 온다. 순서대로 보면 놓친다.
            ("03-inequality-first", [["a!=c", "a==b", "b==c"]]),
            ("04-transitive", [["a==b", "b==c", "c==d", "d!=a"]]),
            ("05-only-inequalities", [["a!=b", "b!=c", "c!=a"]]),
            ("06-separate-groups", [["a==b", "c==d", "a!=c"]]),
            ("07-single-equal", [["x==y"]]),
        ],
        "hidden": [
            ("01-random-consistent", [_equation_set(40, salt=6201, contradict=False)]),
            ("02-random-contradiction", [_equation_set(40, salt=6203, contradict=True)]),
            ("03-all-letters-chain", [[f"{chr(97 + i)}=={chr(98 + i)}" for i in range(25)] + ["a!=z"]]),
            ("04-large-consistent", [_equation_set(50000, salt=6205, contradict=False)]),
            ("05-large-contradiction", [_equation_set(50000, salt=6207, contradict=True)]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 등식을 전부 합친 뒤 부등식을 본다.
fun equationsPossible(equations: Array<String>): Int {
    val parent = IntArray(26) { it }
    fun find(x: Int): Int { var v = x; while (parent[v] != v) { parent[v] = parent[parent[v]]; v = parent[v] }; return v }
    for (eq in equations) if (eq[1] == '=') {
        val a = find(eq[0] - 'a'); val b = find(eq[3] - 'a')
        if (a != b) { parent[a] = b; Drill.match(a, b) }
    }
    for (eq in equations) if (eq[1] == '!') {
        val a = eq[0] - 'a'; val b = eq[3] - 'a'
        Drill.compare(a, b)
        if (find(a) == find(b)) return 0
    }
    return 1
}
""",
    mutants=[
        ("in-order--misses-later-equality", "WRONG_ALGORITHM",
         "식을 순서대로 처리한다. 뒤에 오는 등식이 앞의 부등식을 깨뜨리는 것을 놓친다.",
         """
fun equationsPossible(equations: Array<String>): Int {
    val parent = IntArray(26) { it }
    fun find(x: Int): Int { var v = x; while (parent[v] != v) v = parent[v]; return v }
    for (eq in equations) {
        val a = find(eq[0] - 'a'); val b = find(eq[3] - 'a')
        if (eq[1] == '=') parent[a] = b else if (a == b) return 0
    }
    return 1
}
"""),
        ("no-transitivity", "WRONG_ALGORITHM",
         "직접 등식으로 이어진 쌍만 같다고 본다. a==b, b==c 에서 a 와 c 를 다르게 본다.",
         """
fun equationsPossible(equations: Array<String>): Int {
    val same = Array(26) { BooleanArray(26) }
    for (i in 0 until 26) same[i][i] = true
    for (eq in equations) if (eq[1] == '=') { same[eq[0] - 'a'][eq[3] - 'a'] = true; same[eq[3] - 'a'][eq[0] - 'a'] = true }
    for (eq in equations) if (eq[1] == '!' && same[eq[0] - 'a'][eq[3] - 'a']) return 0
    return 1
}
"""),
        ("ignores-self-inequality", "MISSING_EDGE_CASE",
         "같은 글자끼리의 부등식을 건너뛴다. a!=a 는 항상 모순이다.",
         """
fun equationsPossible(equations: Array<String>): Int {
    val parent = IntArray(26) { it }
    fun find(x: Int): Int { var v = x; while (parent[v] != v) v = parent[v]; return v }
    for (eq in equations) if (eq[1] == '=') parent[find(eq[0] - 'a')] = find(eq[3] - 'a')
    for (eq in equations) if (eq[1] == '!' && eq[0] != eq[3] && find(eq[0] - 'a') == find(eq[3] - 'a')) return 0
    return 1
}
"""),
    ],
))


# --- 109. 단어 사다리 (문자열 위의 BFS) --------------------------------------------------------

def _word_ladder(begin, end, words):
    from collections import deque
    pool = set(words)
    if end not in pool:
        return 0
    seen = {begin}
    queue = deque([(begin, 1)])
    while queue:
        word, steps = queue.popleft()
        if word == end:
            return steps
        for i in range(len(word)):
            for c in "abcdefghijklmnopqrstuvwxyz":
                if c == word[i]:
                    continue
                nxt = word[:i] + c + word[i + 1:]
                if nxt in pool and nxt not in seen:
                    seen.add(nxt)
                    queue.append((nxt, steps + 1))
    return 0


def _word_pool(count, length, salt):
    letters = "abcdefgh"
    picks = randoms(count * length, 0, len(letters) - 1, salt=salt)
    words = {"".join(letters[picks[i * length + j]] for j in range(length)) for i in range(count)}
    return sorted(words)


PROBLEMS.append(Problem(
    id="word-ladder",
    title="단어 사다리",
    summary="""
같은 길이의 소문자 단어 `begin`, `end` 와 단어 목록 `words` 가 주어진다. `begin` 에서
시작해 **한 번에 글자 하나만 바꿔** `end` 에 이르되, 중간의 단어는 전부 `words` 에 있어야
한다. 가장 짧은 변환의 **단어 수** (`begin` 과 `end` 포함) 를 반환한다. 없으면 `0`.
`end` 가 `words` 에 없으면 `0` 이다.

예: `hit → hot → dot → dog → cog` 이면 `5`.
""",
    notes="""
단어가 정점이고 "글자 하나 차이"가 간선인 그래프의 최단 경로 — BFS 다. 간선을 모든 쌍으로
만들면 O(n²·L) — 10 만 단어면 50 억 번 — 이고, 단어마다 자리 하나를 26 글자로 바꿔 목록에 있는지 보면 O(n·L·26) 이다.
방문 표시가 없으면 같은 단어를 몇 번이고 다시 넣어 끝나지 않는다.
""",
    drill_doc="""
Drill.enqueue(i)              // 단어를 큐에 넣었다 (목록의 자리)
Drill.dequeue(i)              // 꺼냈다
Drill.match(i, steps)         // end 에 닿았다
""",
    constraints="""
- `1 <= 단어 길이 <= 10`, `0 <= words.size <= 100_000`, 소문자만
- `begin != end`
""",
    signature=dict(name="wordLadder", parameters=[("begin", "STRING"), ("end", "STRING"), ("words", "STRING_ARRAY")], returns="INT"),
    # 빠른 CI 러너에서 모든 쌍을 견주는 오답이 한도의 2.9배에 그쳤다 — 러너의 CPU 는 실행마다 다르다. 정답이 한도의
    # 2 할 가까이를 써서 시계를 조일 수 없고(0.3 에서 정답이 넘쳤다), 단어만 늘리면 정답도 같이 무거워진다. 그래서 정답은
    # 일이 없고 오답만 모든 쌍을 견주는 입력 — 고립된 시작 단어와 10 만 단어 — 을 더한다(v4 — v2·v3 은 시계를 조이고
    # 단어를 늘린 시도였고 개발 스택에만 등록됐다).
    version=4,
    groups=perf_groups(time_multiplier=0.5),
    reference=_word_ladder,
    cases={
        "sample": [
            ("01", ["hit", "cog", ["hot", "dot", "dog", "lot", "log", "cog"]]),
            ("02", ["hit", "cog", ["hot", "dot", "dog", "lot", "log"]]),
        ],
        "boundary": [
            # 한 글자 차이라 바로 간다. begin 이 목록에 없어도 된다.
            ("01-direct", ["a", "b", ["b"]]),
            ("02-end-missing", ["a", "b", ["c"]]),
            ("03-empty-words", ["ab", "cd", []]),
            # 두 길. 짧은 쪽이 답이다.
            ("04-two-paths", ["aa", "bb", ["ab", "ba", "bb", "ac", "bc"]]),
            # 순환이 있다. 방문 표시가 없으면 끝나지 않는다.
            ("05-cycle", ["aa", "cc", ["ab", "ba", "bb", "aa", "cb", "cc"]]),
            ("06-begin-in-list", ["hit", "hot", ["hit", "hot"]]),
            ("07-unreachable-island", ["aaa", "zzz", ["aab", "abb", "bbb", "zzy", "zzz"]]),
        ],
        "hidden": [
            ("01-random-reachable", ["aaaa", "bbbb", sorted(set(_word_pool(300, 4, salt=7601)) | {"aaab", "aabb", "abbb", "bbbb"})]),
            ("02-random-maybe", ["abcd", "hgfe", _word_pool(800, 4, salt=7602)]),
            ("03-long-chain", ["a" * 8, "b" * 8, ["a" * (8 - k) + "b" * k for k in range(1, 9)]]),
        ],
        "performance": [
            ("01-small", ["aaaaa", "hhhhh", _word_pool(5000, 5, salt=7603)]),
            ("02-medium", ["aaaaaa", "hhhhhh", _word_pool(20000, 6, salt=7604)]),
            # 5 만 단어. 모든 쌍을 견주면 12 억 번이다.
            ("03-large", ["aaaaaa", "hhhhhh", sorted(set(_word_pool(50000, 6, salt=7605)) | {"hhhhhh"})]),
            # 시작 단어의 이웃이 없다 — 정답은 한 걸음에 끝나고, 간선부터 만드는 풀이는 10 만 단어의 모든 쌍(50 억)을 견준다.
            ("04-isolated-begin", ["zzzzzzz", "hhhhhhh", sorted(set(_word_pool(100000, 7, salt=10239)) | {"hhhhhhh"})]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 자리마다 26 글자를 바꿔 목록에 있는지 본다. BFS.
fun wordLadder(begin: String, end: String, words: Array<String>): Int {
    val pool = HashSet<String>()
    val index = HashMap<String, Int>()
    for ((i, w) in words.withIndex()) { pool.add(w); index[w] = i }
    if (end !in pool) return 0
    val seen = HashSet<String>().apply { add(begin) }
    val queue = ArrayDeque<Pair<String, Int>>()
    queue.addLast(begin to 1)
    while (queue.isNotEmpty()) {
        val (word, steps) = queue.removeFirst()
        Drill.dequeue(index[word] ?: -1)
        if (word == end) { Drill.match(index[word] ?: -1, steps); return steps }
        val chars = word.toCharArray()
        for (i in chars.indices) {
            val original = chars[i]
            for (c in 'a'..'z') {
                if (c == original) continue
                chars[i] = c
                val next = String(chars)
                if (next in pool && seen.add(next)) { queue.addLast(next to steps + 1); Drill.enqueue(index[next] ?: -1) }
            }
            chars[i] = original
        }
    }
    return 0
}
""",
    mutants=[
        ("counts-edges", "OFF_BY_ONE", "변환의 횟수를 답한다. 단어 수는 하나 더 많다.", """
fun wordLadder(begin: String, end: String, words: Array<String>): Int {
    val pool = words.toHashSet()
    if (end !in pool) return 0
    val seen = hashSetOf(begin)
    val queue = ArrayDeque(listOf(begin to 0))
    while (queue.isNotEmpty()) {
        val (word, steps) = queue.removeFirst()
        if (word == end) return steps
        val chars = word.toCharArray()
        for (i in chars.indices) { val o = chars[i]; for (c in 'a'..'z') { if (c == o) continue; chars[i] = c; val n = String(chars); if (n in pool && seen.add(n)) queue.addLast(n to steps + 1) }; chars[i] = o }
    }
    return 0
}
"""),
        ("ignores-end-missing", "MISSING_EDGE_CASE", "end 가 목록에 없어도 한 글자 차이면 간다.", """
fun wordLadder(begin: String, end: String, words: Array<String>): Int {
    val pool = words.toHashSet().apply { add(end) }
    val seen = hashSetOf(begin)
    val queue = ArrayDeque(listOf(begin to 1))
    while (queue.isNotEmpty()) {
        val (word, steps) = queue.removeFirst()
        if (word == end) return steps
        val chars = word.toCharArray()
        for (i in chars.indices) { val o = chars[i]; for (c in 'a'..'z') { if (c == o) continue; chars[i] = c; val n = String(chars); if (n in pool && seen.add(n)) queue.addLast(n to steps + 1) }; chars[i] = o }
    }
    return 0
}
"""),
        ("dfs-first-found", "WRONG_ALGORITHM", "DFS 로 처음 닿은 길의 길이를 답한다. 가장 짧다는 보장이 없다.", """
fun wordLadder(begin: String, end: String, words: Array<String>): Int {
    val pool = words.toHashSet()
    if (end !in pool) return 0
    val seen = hashSetOf(begin)
    fun go(word: String, steps: Int): Int {
        if (word == end) return steps
        val chars = word.toCharArray()
        for (i in chars.indices) { val o = chars[i]; for (c in 'a'..'z') { if (c == o) continue; chars[i] = c; val n = String(chars); if (n in pool && seen.add(n)) { val r = go(n, steps + 1); if (r > 0) return r } }; chars[i] = o }
        return 0
    }
    return go(begin, 1)
}
"""),
        ("pairwise-edges--quadratic", "PERFORMANCE", "모든 단어 쌍을 견줘 간선을 만든다. O(n²·L).", """
fun wordLadder(begin: String, end: String, words: Array<String>): Int {
    val all = (listOf(begin) + words).distinct()
    val idx = all.withIndex().associate { it.value to it.index }
    val target = idx[end] ?: return 0
    fun adjacent(a: String, b: String): Boolean { var d = 0; for (i in a.indices) { if (a[i] != b[i]) d += 1; if (d > 1) return false }; return d == 1 }
    val adj = Array(all.size) { ArrayList<Int>() }
    for (i in all.indices) for (j in i + 1 until all.size) { Drill.compare(i, j); if (adjacent(all[i], all[j])) { adj[i].add(j); adj[j].add(i) } }
    val dist = IntArray(all.size) { 0 }
    dist[0] = 1
    val queue = ArrayDeque(listOf(0))
    while (queue.isNotEmpty()) {
        val v = queue.removeFirst()
        if (v == target) return dist[v]
        for (u in adj[v]) if (dist[u] == 0) { dist[u] = dist[v] + 1; queue.addLast(u) }
    }
    return 0
}
"""),
    ],
))


# --- 116. 신호가 모두 닿는 시간 (방향 다익스트라) -------------------------------------------------

def _signal_delay(n, edges, source):
    import heapq
    graph = [[] for _ in range(n)]
    for i in range(0, len(edges), 3):
        graph[edges[i]].append((edges[i + 1], edges[i + 2]))
    dist = [-1] * n
    dist[source] = 0
    heap = [(0, source)]
    while heap:
        d, node = heapq.heappop(heap)
        if d > dist[node]:
            continue
        for nxt, w in graph[node]:
            nd = d + w
            if dist[nxt] == -1 or nd < dist[nxt]:
                dist[nxt] = nd
                heapq.heappush(heap, (nd, nxt))
    if any(d == -1 for d in dist):
        return -1
    return max(dist)


def _directed_chain(n, salt):
    weights = randoms(n - 1, 1, 100, salt=salt)
    return flat([i, i + 1, weights[i]] for i in range(n - 1))


def _directed_random(n, m, salt):
    # 사슬로 먼저 모두 닿게 하고, 그 위에 무작위 간선을 얹는다 — 답이 -1 이 아니어야 시간이 시험된다.
    a = randoms(m, 0, n - 1, salt=salt)
    b = randoms(m, 0, n - 1, salt=salt + 1)
    w = randoms(m, 1, 1000, salt=salt + 2)
    return _directed_chain(n, salt + 3) + flat([a[i], b[i], w[i]] for i in range(m))


PROBLEMS.append(Problem(
    id="signal-delay",
    title="신호가 모두 닿는 시간",
    summary="""
정점이 `0` 부터 `n-1` 까지 있는 **방향 가중 그래프**가 주어진다. `edges` 는
`[a1, b1, w1, a2, b2, w2, ...]` 이며 `a → b` 로 가는 데 `w` 만큼 걸린다.

`source` 에서 신호를 보낸다. 신호는 간선을 따라 동시에 퍼진다. **모든 정점이 신호를 받는
데 걸리는 시간**을 반환한다. 받지 못하는 정점이 하나라도 있으면 `-1` 이다.
""",
    notes="""
답은 "가장 늦게 받는 정점의 시간"이다 — 출발점에서 각 정점까지의 최단 시간 중 최댓값.
간선은 한 방향이라 `a → b` 를 `b → a` 로도 넣으면 갈 수 없는 곳이 갈 수 있게 된다.
""",
    drill_doc="""
Drill.node("v3")              // 정점의 시간을 확정했다
Drill.edge("v3", "v7")        // 간선으로 이웃의 시간을 낮췄다
Drill.write(v, time)          // 정점의 시간을 적었다
""",
    constraints="""
- `1 <= n <= 100_000`
- `edges.size` 는 3 의 배수이며 `0 <= edges.size <= 600_000`
- `1 <= w <= 1000`, 모든 시간의 합은 `Int` 범위 안이다
- `0 <= source < n`
""",
    signature=dict(
        name="signalDelay",
        parameters=[("n", "INT"), ("edges", "INT_ARRAY"), ("source", "INT")],
        returns="INT",
    ),
    groups=perf_groups(),
    reference=_signal_delay,
    cases={
        "sample": [
            ("01", [4, [1, 0, 1, 1, 2, 1, 2, 3, 1], 1]),
            ("02", [2, [0, 1, 1], 1]),
        ],
        "boundary": [
            ("01-single", [1, [], 0]),
            # 반대 방향 간선뿐이다. 무방향으로 읽으면 닿는다.
            ("02-one-way-only", [2, [1, 0, 5], 0]),
            # 가장 늦은 정점이 답이지 가장 가까운 정점이 아니다.
            ("03-max-not-min", [3, [0, 1, 1, 0, 2, 9], 0]),
            # 닿는 정점만 보면 답이 있는데, 못 닿는 정점이 하나 있다.
            ("04-one-unreachable", [4, [0, 1, 1, 1, 2, 1], 0]),
            # 더 빠른 길이 나중에 발견된다.
            ("05-relax-later", [4, [0, 1, 1, 0, 2, 5, 1, 2, 1, 2, 3, 1], 0]),
            ("06-parallel-edges", [2, [0, 1, 9, 0, 1, 2], 0]),
        ],
        "hidden": [
            ("01-random-small", [8, _directed_random(8, 14, salt=8101), 0]),
            ("02-random-medium", [50, _directed_random(50, 120, salt=8105), 0]),
            ("03-chain", [10, _directed_chain(10, salt=8109), 0]),
            # 출발점이 사슬의 중간이면 앞쪽은 못 닿는다.
            ("04-chain-mid-source", [10, _directed_chain(10, salt=8110), 4]),
        ],
        "performance": [
            ("01-small", [3000, _directed_random(3000, 9000, salt=8111), 0]),
            ("02-medium", [20000, _directed_random(20000, 60000, salt=8115), 0]),
            ("03-large", [100000, _directed_random(100000, 200000, salt=8119), 0]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 방향 간선으로 다익스트라, 답은 최댓값.
fun signalDelay(n: Int, edges: IntArray, source: Int): Int {
    val m = edges.size / 3
    val head = IntArray(n) { -1 }
    val next = IntArray(m)
    val to = IntArray(m)
    val cost = IntArray(m)
    var e = 0
    var i = 0
    while (i < edges.size) {
        to[e] = edges[i + 1]; cost[e] = edges[i + 2]; next[e] = head[edges[i]]; head[edges[i]] = e; e += 1
        i += 3
    }
    val dist = IntArray(n) { -1 }
    dist[source] = 0
    val heap = java.util.PriorityQueue<LongArray>(compareBy { it[0] })
    heap.add(longArrayOf(0L, source.toLong()))
    while (heap.isNotEmpty()) {
        val top = heap.poll()
        val d = top[0].toInt()
        val node = top[1].toInt()
        if (d > dist[node]) continue
        Drill.node("v$node")
        var edge = head[node]
        while (edge != -1) {
            val nxt = to[edge]
            val nd = d + cost[edge]
            if (dist[nxt] == -1 || nd < dist[nxt]) {
                dist[nxt] = nd
                Drill.edge("v$node", "v$nxt")
                Drill.write(nxt, nd)
                heap.add(longArrayOf(nd.toLong(), nxt.toLong()))
            }
            edge = next[edge]
        }
    }
    var answer = 0
    for (v in 0 until n) {
        if (dist[v] == -1) return -1
        if (dist[v] > answer) answer = dist[v]
    }
    return answer
}
""",
    mutants=[
        ("undirected--adds-reverse-edge", "MISSING_EDGE_CASE",
         "간선을 양방향으로 넣는다. 한 방향뿐인 그래프에서 못 닿는 곳이 닿는다.",
         """
fun signalDelay(n: Int, edges: IntArray, source: Int): Int {
    val graph = Array(n) { mutableListOf<Pair<Int, Int>>() }
    var i = 0
    while (i < edges.size) {
        graph[edges[i]].add(edges[i + 1] to edges[i + 2]); graph[edges[i + 1]].add(edges[i] to edges[i + 2]); i += 3
    }
    val dist = IntArray(n) { -1 }
    dist[source] = 0
    val heap = java.util.PriorityQueue<LongArray>(compareBy { it[0] })
    heap.add(longArrayOf(0L, source.toLong()))
    while (heap.isNotEmpty()) {
        val top = heap.poll()
        val d = top[0].toInt(); val node = top[1].toInt()
        if (d > dist[node]) continue
        for ((nxt, w) in graph[node]) {
            if (dist[nxt] == -1 || d + w < dist[nxt]) { dist[nxt] = d + w; heap.add(longArrayOf(dist[nxt].toLong(), nxt.toLong())) }
        }
    }
    if (dist.any { it == -1 }) return -1
    return dist.max()
}
"""),
        ("min-instead-of-max", "WRONG_BRANCH",
         "가장 늦게 받는 정점이 아니라 가장 먼저 받는 정점의 시간을 돌려준다.",
         """
fun signalDelay(n: Int, edges: IntArray, source: Int): Int {
    val graph = Array(n) { mutableListOf<Pair<Int, Int>>() }
    var i = 0
    while (i < edges.size) { graph[edges[i]].add(edges[i + 1] to edges[i + 2]); i += 3 }
    val dist = IntArray(n) { -1 }
    dist[source] = 0
    val heap = java.util.PriorityQueue<LongArray>(compareBy { it[0] })
    heap.add(longArrayOf(0L, source.toLong()))
    while (heap.isNotEmpty()) {
        val top = heap.poll()
        val d = top[0].toInt(); val node = top[1].toInt()
        if (d > dist[node]) continue
        for ((nxt, w) in graph[node]) {
            if (dist[nxt] == -1 || d + w < dist[nxt]) { dist[nxt] = d + w; heap.add(longArrayOf(dist[nxt].toLong(), nxt.toLong())) }
        }
    }
    if (dist.any { it == -1 }) return -1
    var best = Int.MAX_VALUE
    for (v in 0 until n) if (v != source && dist[v] < best) best = dist[v]
    return if (best == Int.MAX_VALUE) 0 else best
}
"""),
        ("ignores-unreachable", "MISSING_EDGE_CASE",
         "못 닿는 정점을 빼고 최댓값을 낸다. 모두 닿아야 답이 있다.",
         """
fun signalDelay(n: Int, edges: IntArray, source: Int): Int {
    val graph = Array(n) { mutableListOf<Pair<Int, Int>>() }
    var i = 0
    while (i < edges.size) { graph[edges[i]].add(edges[i + 1] to edges[i + 2]); i += 3 }
    val dist = IntArray(n) { -1 }
    dist[source] = 0
    val heap = java.util.PriorityQueue<LongArray>(compareBy { it[0] })
    heap.add(longArrayOf(0L, source.toLong()))
    while (heap.isNotEmpty()) {
        val top = heap.poll()
        val d = top[0].toInt(); val node = top[1].toInt()
        if (d > dist[node]) continue
        for ((nxt, w) in graph[node]) {
            if (dist[nxt] == -1 || d + w < dist[nxt]) { dist[nxt] = d + w; heap.add(longArrayOf(dist[nxt].toLong(), nxt.toLong())) }
        }
    }
    return dist.max()
}
"""),
        ("array-scan--quadratic", "PERFORMANCE",
         "힙 없이 매번 모든 정점을 훑어 가장 빠른 미확정 정점을 고른다. O(n²).",
         """
fun signalDelay(n: Int, edges: IntArray, source: Int): Int {
    val graph = Array(n) { mutableListOf<Pair<Int, Int>>() }
    var i = 0
    while (i < edges.size) { graph[edges[i]].add(edges[i + 1] to edges[i + 2]); i += 3 }
    val dist = IntArray(n) { -1 }
    val done = BooleanArray(n)
    dist[source] = 0
    repeat(n) {
        var best = -1
        for (v in 0 until n) {
            Drill.compare(v, best)
            if (!done[v] && dist[v] != -1 && (best == -1 || dist[v] < dist[best])) best = v
        }
        if (best == -1) return -1
        done[best] = true
        for ((nxt, w) in graph[best]) {
            if (dist[nxt] == -1 || dist[best] + w < dist[nxt]) dist[nxt] = dist[best] + w
        }
    }
    return dist.max()
}
"""),
    ],
))


# --- 117. 사전순 수강 순서 (힙으로 위상 정렬) -----------------------------------------------------

def _smallest_order(n, prereqs):
    import heapq
    graph = [[] for _ in range(n)]
    indegree = [0] * n
    for i in range(0, len(prereqs), 2):
        graph[prereqs[i]].append(prereqs[i + 1])
        indegree[prereqs[i + 1]] += 1
    heap = [v for v in range(n) if indegree[v] == 0]
    heapq.heapify(heap)
    order = []
    while heap:
        v = heapq.heappop(heap)
        order.append(v)
        for nxt in graph[v]:
            indegree[nxt] -= 1
            if indegree[nxt] == 0:
                heapq.heappush(heap, nxt)
    return order if len(order) == n else []


def _descending_chain(n):
    """n-1 → n-2 → … → 0 사슬. 들을 수 있는 과목이 늘 번호가 가장 큰 하나뿐이라, 매번 0 부터
    훑어 찾는 오답이 걸음마다 거의 n 칸을 걷는다 — 무작위 그래프에서는 금세 찾아 끊겼다."""
    return flat([i + 1, i] for i in range(n - 2, -1, -1))


def _dag_pairs(n, m, salt):
    # 큰 번호 → 작은 번호로만 간선을 두어 순환이 없게 한다. 사전순이 시험되도록 방향을 뒤집는다.
    a = randoms(m, 0, n - 1, salt=salt)
    b = randoms(m, 0, n - 1, salt=salt + 1)
    return flat([max(a[i], b[i]), min(a[i], b[i])] for i in range(m) if a[i] != b[i])


PROBLEMS.append(Problem(
    id="smallest-course-order",
    # v2: 내림차순으로 강제되는 사슬을 성능 케이스로 더했다. 무작위 그래프에서는 들을 수 있는
    # 과목을 금세 찾아, 매번 전부 훑는 오답이 CI 머신에서 한도의 2.4배에 그쳤다 (§12.1 재현성).
    version=2,
    title="사전순 수강 순서",
    summary="""
과목이 `0` 부터 `n-1` 까지 있고 `prereqs` 는 `[a1, b1, a2, b2, ...]` 로 "`a` 를 들은 뒤에야
`b` 를 들을 수 있다"는 조건들이다.

조건을 모두 지키는 수강 순서 중 **사전순으로 가장 앞선 것**을 반환한다 — 순서를 배열로
봤을 때 가장 작은 것. 조건이 서로 얽혀 모든 과목을 들을 수 없으면 빈 배열을 반환한다.
""",
    notes="""
들을 수 있는 과목(남은 선수가 없는 과목)이 여럿일 때 **번호가 가장 작은 것**을 먼저 들어야
사전순으로 가장 앞선다. "지금 들을 수 있는 것 중 가장 작은 것"을 매번 빨리 꺼내는 구조가
필요하다. 끝났는데 든 과목이 `n` 개가 안 되면 순환이다.
""",
    drill_doc="""
Drill.enqueue(v)              // 들을 수 있게 된 과목을 후보에 넣었다
Drill.dequeue(v)              // 가장 작은 후보를 들었다
Drill.edge("c1", "c4")        // 선수 하나를 지웠다
""",
    constraints="""
- `1 <= n <= 100_000`
- `prereqs.size` 는 짝수이며 `0 <= prereqs.size <= 400_000`
- 같은 조건이 여러 번 나올 수 있다
""",
    signature=dict(
        name="smallestOrder",
        parameters=[("n", "INT"), ("prereqs", "INT_ARRAY")],
        returns="INT_ARRAY",
    ),
    groups=perf_groups(time_multiplier=0.5),
    reference=_smallest_order,
    limits={"timeMillis": 2000, "memoryMb": 256, "outputBytes": 2000000},
    cases={
        "sample": [
            ("01", [4, [1, 0, 2, 0, 3, 1, 3, 2]]),
            ("02", [2, [0, 1, 1, 0]]),
        ],
        "boundary": [
            ("01-single", [1, []]),
            ("02-no-prereqs", [4, []]),
            # 선입선출로 처리하면 유효한 순서지만 사전순이 아니다.
            ("03-fifo-is-not-smallest", [4, [3, 0, 1, 2]]),
            # 순환이 있으면 일부만 들을 수 있어도 빈 배열이다.
            ("04-partial-cycle", [4, [0, 1, 1, 2, 2, 1, 2, 3]]),
            ("05-self-loop", [2, [0, 0]]),
            ("06-duplicate-condition", [3, [2, 0, 2, 0, 1, 0]]),
            # 방향을 뒤집어 읽으면 다른 순서가 나온다.
            ("07-direction-matters", [3, [2, 1, 1, 0]]),
        ],
        "hidden": [
            ("01-random-small", [8, _dag_pairs(8, 12, salt=8121)]),
            ("02-random-medium", [60, _dag_pairs(60, 150, salt=8125)]),
            ("03-reversed-chain", [10, flat([i + 1, i] for i in range(9))]),
            ("04-cycle-deep", [6, [0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 3]]),
        ],
        "performance": [
            ("01-small", [3000, _dag_pairs(3000, 9000, salt=8131)]),
            ("02-medium", [20000, _dag_pairs(20000, 60000, salt=8135)]),
            ("03-large", [100000, _dag_pairs(100000, 200000, salt=8139)]),
            ("04-descending-chain", [100000, _descending_chain(100000)]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 남은 선수가 없는 과목 중 가장 작은 것을 힙으로 꺼낸다.
fun smallestOrder(n: Int, prereqs: IntArray): IntArray {
    val m = prereqs.size / 2
    val head = IntArray(n) { -1 }
    val next = IntArray(m)
    val to = IntArray(m)
    val indegree = IntArray(n)
    var i = 0
    var e = 0
    while (i < prereqs.size) {
        to[e] = prereqs[i + 1]; next[e] = head[prereqs[i]]; head[prereqs[i]] = e; e += 1
        indegree[prereqs[i + 1]] += 1
        i += 2
    }
    val heap = java.util.PriorityQueue<Int>()
    for (v in 0 until n) if (indegree[v] == 0) { heap.add(v); Drill.enqueue(v) }
    val order = IntArray(n)
    var taken = 0
    while (heap.isNotEmpty()) {
        val v = heap.poll()
        Drill.dequeue(v)
        order[taken] = v
        taken += 1
        var edge = head[v]
        while (edge != -1) {
            val nxt = to[edge]
            Drill.edge("c$v", "c$nxt")
            indegree[nxt] -= 1
            if (indegree[nxt] == 0) { heap.add(nxt); Drill.enqueue(nxt) }
            edge = next[edge]
        }
    }
    return if (taken == n) order else IntArray(0)
}
""",
    mutants=[
        ("fifo-queue--valid-but-not-smallest", "WRONG_BRANCH",
         "들을 수 있는 과목을 선입선출로 든다. 유효한 순서이지만 사전순이 아니다.",
         """
fun smallestOrder(n: Int, prereqs: IntArray): IntArray {
    val graph = Array(n) { mutableListOf<Int>() }
    val indegree = IntArray(n)
    var i = 0
    while (i < prereqs.size) { graph[prereqs[i]].add(prereqs[i + 1]); indegree[prereqs[i + 1]] += 1; i += 2 }
    val queue = ArrayDeque<Int>()
    for (v in 0 until n) if (indegree[v] == 0) queue.addLast(v)
    val order = mutableListOf<Int>()
    while (queue.isNotEmpty()) {
        val v = queue.removeFirst()
        order.add(v)
        for (nxt in graph[v]) { indegree[nxt] -= 1; if (indegree[nxt] == 0) queue.addLast(nxt) }
    }
    return if (order.size == n) order.toIntArray() else IntArray(0)
}
"""),
        ("partial-on-cycle--returns-what-it-could", "MISSING_EDGE_CASE",
         "순환이 있어도 들을 수 있었던 과목까지만 돌려준다. 순환이면 빈 배열이어야 한다.",
         """
fun smallestOrder(n: Int, prereqs: IntArray): IntArray {
    val graph = Array(n) { mutableListOf<Int>() }
    val indegree = IntArray(n)
    var i = 0
    while (i < prereqs.size) { graph[prereqs[i]].add(prereqs[i + 1]); indegree[prereqs[i + 1]] += 1; i += 2 }
    val heap = java.util.PriorityQueue<Int>()
    for (v in 0 until n) if (indegree[v] == 0) heap.add(v)
    val order = mutableListOf<Int>()
    while (heap.isNotEmpty()) {
        val v = heap.poll()
        order.add(v)
        for (nxt in graph[v]) { indegree[nxt] -= 1; if (indegree[nxt] == 0) heap.add(nxt) }
    }
    return order.toIntArray()
}
"""),
        ("reversed-direction", "WRONG_ALGORITHM",
         "조건의 방향을 거꾸로 읽는다 — b 를 들은 뒤 a 를 듣는다.",
         """
fun smallestOrder(n: Int, prereqs: IntArray): IntArray {
    val graph = Array(n) { mutableListOf<Int>() }
    val indegree = IntArray(n)
    var i = 0
    while (i < prereqs.size) { graph[prereqs[i + 1]].add(prereqs[i]); indegree[prereqs[i]] += 1; i += 2 }
    val heap = java.util.PriorityQueue<Int>()
    for (v in 0 until n) if (indegree[v] == 0) heap.add(v)
    val order = mutableListOf<Int>()
    while (heap.isNotEmpty()) {
        val v = heap.poll()
        order.add(v)
        for (nxt in graph[v]) { indegree[nxt] -= 1; if (indegree[nxt] == 0) heap.add(nxt) }
    }
    return if (order.size == n) order.toIntArray() else IntArray(0)
}
"""),
        ("scan-all--quadratic", "PERFORMANCE",
         "매번 모든 과목을 훑어 들을 수 있는 가장 작은 것을 찾는다. O(n²).",
         """
fun smallestOrder(n: Int, prereqs: IntArray): IntArray {
    val graph = Array(n) { mutableListOf<Int>() }
    val indegree = IntArray(n)
    var i = 0
    while (i < prereqs.size) { graph[prereqs[i]].add(prereqs[i + 1]); indegree[prereqs[i + 1]] += 1; i += 2 }
    val taken = BooleanArray(n)
    val order = IntArray(n)
    for (step in 0 until n) {
        var chosen = -1
        for (v in 0 until n) {
            Drill.compare(v, chosen)
            if (!taken[v] && indegree[v] == 0) { chosen = v; break }
        }
        if (chosen == -1) return IntArray(0)
        taken[chosen] = true
        order[step] = chosen
        for (nxt in graph[chosen]) indegree[nxt] -= 1
    }
    return order
}
"""),
    ],
))


# --- 118. 결국 멈추는 정점 ---------------------------------------------------------------------

def _safe_nodes(n, edges):
    reverse = [[] for _ in range(n)]
    outdegree = [0] * n
    for i in range(0, len(edges), 2):
        a, b = edges[i], edges[i + 1]
        reverse[b].append(a)
        outdegree[a] += 1
    queue = [v for v in range(n) if outdegree[v] == 0]
    safe = [False] * n
    head = 0
    while head < len(queue):
        v = queue[head]
        head += 1
        safe[v] = True
        for prev in reverse[v]:
            outdegree[prev] -= 1
            if outdegree[prev] == 0:
                queue.append(prev)
    return [v for v in range(n) if safe[v]]


def _directed_pairs(n, m, salt):
    a = randoms(m, 0, n - 1, salt=salt)
    b = randoms(m, 0, n - 1, salt=salt + 1)
    return flat([a[i], b[i]] for i in range(m))


PROBLEMS.append(Problem(
    id="eventually-safe",
    title="결국 멈추는 정점",
    summary="""
정점이 `0` 부터 `n-1` 까지 있는 **방향 그래프**가 주어진다. `edges` 는 `[a1, b1, a2, b2, ...]`
로 `a → b` 간선들이다. 나가는 간선이 없는 정점을 **끝 정점**이라 한다.

어떤 정점에서 출발해 간선을 따라 어떻게 걸어도 **반드시 유한 걸음 안에 끝 정점에 닿으면**
그 정점은 안전하다. 안전한 정점을 오름차순으로 담은 배열을 반환한다.
""",
    notes="""
끝 정점은 안전하다. 나가는 간선이 **전부** 안전한 정점으로만 가는 정점도 안전하다. 그래서
끝 정점에서 거꾸로 — 간선을 뒤집어 — 퍼져 나가며 "남은 나가는 간선"을 세면 된다. 순환에
걸린 정점은 남은 간선이 0 이 되지 않아 영영 안전해지지 않는다.
""",
    drill_doc="""
Drill.enqueue(v)              // 안전해진 정점을 큐에 넣었다
Drill.dequeue(v)              // 안전한 정점을 꺼내 거꾸로 퍼진다
Drill.edge("v3", "v1")        // 뒤집은 간선을 따라 남은 간선 수를 줄였다
""",
    constraints="""
- `1 <= n <= 100_000`
- `edges.size` 는 짝수이며 `0 <= edges.size <= 400_000`
- 자기 자신으로 가는 간선이 있을 수 있다
""",
    signature=dict(
        name="safeNodes",
        parameters=[("n", "INT"), ("edges", "INT_ARRAY")],
        returns="INT_ARRAY",
    ),
    groups=perf_groups(),
    reference=_safe_nodes,
    limits={"timeMillis": 2000, "memoryMb": 256, "outputBytes": 2000000},
    cases={
        "sample": [
            ("01", [7, [0, 1, 0, 2, 1, 2, 1, 3, 2, 5, 3, 0, 4, 5]]),
            ("02", [3, [0, 1, 1, 2]]),
        ],
        "boundary": [
            ("01-single-terminal", [1, []]),
            # 자기 자신으로 가는 간선은 순환이다.
            ("02-self-loop", [2, [0, 0]]),
            # 순환에 닿을 수 있는 정점은 끝 정점으로도 갈 수 있어도 안전하지 않다.
            ("03-can-reach-cycle", [4, [0, 1, 0, 3, 1, 2, 2, 1]]),
            # 순환에서 빠져나가는 간선이 있어도 순환 안의 정점은 안전하지 않다.
            ("04-cycle-with-exit", [3, [0, 1, 1, 0, 1, 2]]),
            ("05-all-terminal", [3, []]),
            # 끝 정점만이 아니라 끝 정점으로만 가는 정점도 안전하다.
            ("06-chain-to-terminal", [4, [0, 1, 1, 2, 2, 3]]),
        ],
        "hidden": [
            ("01-random-small", [8, _directed_pairs(8, 10, salt=8141)]),
            ("02-random-medium", [60, _directed_pairs(60, 90, salt=8145)]),
            ("03-two-cycles", [7, [0, 1, 1, 0, 2, 3, 3, 4, 4, 2, 5, 6, 6, 0]]),
            ("04-long-chain", [12, flat([i, i + 1] for i in range(11))]),
        ],
        "performance": [
            ("01-chain-small", [5000, flat([i, i + 1] for i in range(4999))]),
            ("02-chain-medium", [30000, flat([i, i + 1] for i in range(29999))]),
            ("03-chain-large", [100000, flat([i, i + 1] for i in range(99999))]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 간선을 뒤집어 끝 정점에서부터 남은 나가는 간선을 센다.
fun safeNodes(n: Int, edges: IntArray): IntArray {
    val m = edges.size / 2
    val head = IntArray(n) { -1 }
    val next = IntArray(m)
    val to = IntArray(m)
    val outdegree = IntArray(n)
    var i = 0
    var e = 0
    while (i < edges.size) {
        val a = edges[i]; val b = edges[i + 1]
        to[e] = a; next[e] = head[b]; head[b] = e; e += 1
        outdegree[a] += 1
        i += 2
    }
    val queue = IntArray(n)
    var tail = 0
    for (v in 0 until n) if (outdegree[v] == 0) { queue[tail] = v; tail += 1; Drill.enqueue(v) }
    val safe = BooleanArray(n)
    var front = 0
    while (front < tail) {
        val v = queue[front]; front += 1
        Drill.dequeue(v)
        safe[v] = true
        var edge = head[v]
        while (edge != -1) {
            val prev = to[edge]
            Drill.edge("v$v", "v$prev")
            outdegree[prev] -= 1
            if (outdegree[prev] == 0) { queue[tail] = prev; tail += 1; Drill.enqueue(prev) }
            edge = next[edge]
        }
    }
    val out = IntArray(tail)
    var k = 0
    for (v in 0 until n) if (safe[v]) { out[k] = v; k += 1 }
    return out
}
""",
    mutants=[
        ("terminal-only", "WRONG_ALGORITHM",
         "나가는 간선이 없는 정점만 안전하다고 본다. 끝 정점으로만 가는 정점도 안전하다.",
         """
fun safeNodes(n: Int, edges: IntArray): IntArray {
    val outdegree = IntArray(n)
    var i = 0
    while (i < edges.size) { outdegree[edges[i]] += 1; i += 2 }
    return (0 until n).filter { outdegree[it] == 0 }.toIntArray()
}
"""),
        ("no-gray--memo-without-in-progress", "WRONG_BRANCH",
         "DFS 에서 방문 중인 정점을 표시하지 않아 순환에 걸린 정점을 안전하다고 적는다.",
         """
fun safeNodes(n: Int, edges: IntArray): IntArray {
    val graph = Array(n) { mutableListOf<Int>() }
    var i = 0
    while (i < edges.size) { graph[edges[i]].add(edges[i + 1]); i += 2 }
    val memo = IntArray(n) // 0 모름, 1 안전, 2 위험
    fun safe(v: Int): Boolean {
        if (memo[v] != 0) return memo[v] == 1
        memo[v] = 1
        for (nxt in graph[v]) if (!safe(nxt)) { memo[v] = 2; return false }
        return true
    }
    return (0 until n).filter { safe(it) }.toIntArray()
}
"""),
        ("ignores-self-loop", "MISSING_EDGE_CASE",
         "자기 자신으로 가는 간선을 빼고 센다. 자기 순환은 순환이다.",
         """
fun safeNodes(n: Int, edges: IntArray): IntArray {
    val reverse = Array(n) { mutableListOf<Int>() }
    val outdegree = IntArray(n)
    var i = 0
    while (i < edges.size) {
        val a = edges[i]; val b = edges[i + 1]
        if (a != b) { reverse[b].add(a); outdegree[a] += 1 }
        i += 2
    }
    val queue = ArrayDeque<Int>()
    for (v in 0 until n) if (outdegree[v] == 0) queue.addLast(v)
    val safe = BooleanArray(n)
    while (queue.isNotEmpty()) {
        val v = queue.removeFirst()
        safe[v] = true
        for (prev in reverse[v]) { outdegree[prev] -= 1; if (outdegree[prev] == 0) queue.addLast(prev) }
    }
    return (0 until n).filter { safe[it] }.toIntArray()
}
"""),
        ("dfs-per-node--no-memo", "PERFORMANCE",
         "정점마다 처음부터 DFS 를 돌려 순환에 닿는지 본다. 사슬에서 O(n²).",
         """
fun safeNodes(n: Int, edges: IntArray): IntArray {
    val graph = Array(n) { mutableListOf<Int>() }
    var i = 0
    while (i < edges.size) { graph[edges[i]].add(edges[i + 1]); i += 2 }
    fun reachesCycle(start: Int): Boolean {
        val state = IntArray(n)
        val stack = ArrayDeque<IntArray>()
        stack.addLast(intArrayOf(start, 0)); state[start] = 1
        while (stack.isNotEmpty()) {
            val top = stack.last()
            val v = top[0]
            if (top[1] < graph[v].size) {
                val nxt = graph[v][top[1]]; top[1] += 1
                Drill.compare(v, nxt)
                if (state[nxt] == 1) return true
                if (state[nxt] == 0) { state[nxt] = 1; stack.addLast(intArrayOf(nxt, 0)) }
            } else { state[v] = 2; stack.removeLast() }
        }
        return false
    }
    return (0 until n).filter { !reachesCycle(it) }.toIntArray()
}
"""),
    ],
))


# --- 119. 모두 이어지는 순간 (정렬 + 유니온파인드) --------------------------------------------------

def _earliest_all_connected(n, logs):
    if n == 1:
        return 0
    events = sorted((logs[i], logs[i + 1], logs[i + 2]) for i in range(0, len(logs), 3))
    parent = list(range(n))

    def find(x):
        while parent[x] != x:
            parent[x] = parent[parent[x]]
            x = parent[x]
        return x

    groups = n
    for t, a, b in events:
        ra, rb = find(a), find(b)
        if ra != rb:
            parent[ra] = rb
            groups -= 1
            if groups == 1:
                return t
    return -1


def _friend_logs(n, m, salt):
    # 사슬로 먼저 다 잇고 무작위를 얹은 뒤 시각을 뒤섞는다 — 입력 순서를 믿으면 틀리게.
    t = randoms(m, 1, 10 ** 9, salt=salt)
    a = randoms(m, 0, n - 1, salt=salt + 1)
    b = randoms(m, 0, n - 1, salt=salt + 2)
    chain_t = randoms(n - 1, 1, 10 ** 9, salt=salt + 3)
    rows = [[chain_t[i], i, i + 1] for i in range(n - 1)] + [[t[i], a[i], b[i]] for i in range(m)]
    return flat(shuffled(rows, salt=salt + 4))


PROBLEMS.append(Problem(
    id="earliest-all-connected",
    title="모두 친구가 되는 순간",
    summary="""
사람이 `0` 부터 `n-1` 까지 있다. `logs` 는 `[t1, a1, b1, t2, a2, b2, ...]` 로 "시각 `t` 에
`a` 와 `b` 가 친구가 됐다"는 기록들이며 **시각 순서대로 주어지지 않는다.** 친구 관계는
이어진다 — `a` 와 `b` 가 친구이고 `b` 와 `c` 가 친구면 `a` 와 `c` 는 서로 아는 사이다.

모든 사람이 서로 아는 사이가 되는 **가장 이른 시각**을 반환한다. 끝까지 그런 순간이 없으면
`-1` 이다. 사람이 한 명이면 `0` 이다.
""",
    notes="""
시각순으로 정렬한 뒤 기록을 하나씩 반영하며 "서로 아는 무리"의 수를 센다. 무리가 하나가
되는 순간의 시각이 답이다. 이미 같은 무리인 둘의 기록은 무리 수를 줄이지 않는다 — 기록 수를
세면 틀린다.
""",
    drill_doc="""
Drill.compare(a, b)           // 두 사람의 무리를 비교했다
Drill.write(root, newRoot)    // 무리를 합쳤다
""",
    constraints="""
- `1 <= n <= 100_000`
- `logs.size` 는 3 의 배수이며 `0 <= logs.size <= 600_000`
- `1 <= t <= 10^9`, `0 <= a, b < n`, `a != b`
- 시각은 겹칠 수 있다
""",
    signature=dict(
        name="earliestAllConnected",
        parameters=[("n", "INT"), ("logs", "INT_ARRAY")],
        returns="INT",
    ),
    groups=perf_groups(),
    reference=_earliest_all_connected,
    cases={
        "sample": [
            ("01", [4, [20, 0, 1, 5, 2, 3, 30, 1, 2, 40, 0, 3]]),
            ("02", [3, [10, 0, 1, 20, 0, 1]]),
        ],
        "boundary": [
            ("01-single-person", [1, []]),
            ("02-two-people", [2, [7, 0, 1]]),
            # 입력 순서를 믿으면 마지막 기록의 시각을 답한다.
            ("03-unsorted", [3, [50, 1, 2, 10, 0, 1]]),
            # 세 번째 기록은 이미 같은 무리라 아무것도 잇지 않는다. 기록 수로 세면 틀린다.
            ("04-redundant-log", [3, [1, 0, 1, 2, 0, 1, 3, 0, 1, 4, 1, 2]]),
            ("05-never", [3, [1, 0, 1, 2, 0, 1]]),
            # 같은 시각의 기록 둘이 함께 완성한다.
            ("06-tie-time", [3, [5, 0, 1, 5, 1, 2]]),
        ],
        "hidden": [
            ("01-random-small", [8, _friend_logs(8, 10, salt=8151)]),
            ("02-random-medium", [60, _friend_logs(60, 80, salt=8156)]),
            ("03-late-bridge", [6, [1, 0, 1, 2, 1, 2, 3, 3, 4, 4, 4, 5, 100, 2, 3, 50, 0, 2]]),
            ("04-never-large", [7, flat([i * 3 + 1, i, i + 1] for i in range(5))]),
        ],
        "performance": [
            ("01-small", [3000, _friend_logs(3000, 6000, salt=8161)]),
            ("02-medium", [20000, _friend_logs(20000, 40000, salt=8166)]),
            ("03-large", [100000, _friend_logs(100000, 100000, salt=8171)]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 시각순 정렬 뒤 유니온파인드로 무리 수를 센다.
fun earliestAllConnected(n: Int, logs: IntArray): Int {
    if (n == 1) return 0
    val m = logs.size / 3
    val order = (0 until m).sortedBy { logs[it * 3] }
    val parent = IntArray(n) { it }
    fun find(x: Int): Int {
        var v = x
        while (parent[v] != v) { parent[v] = parent[parent[v]]; v = parent[v] }
        return v
    }
    var groups = n
    for (k in order) {
        val t = logs[k * 3]; val a = logs[k * 3 + 1]; val b = logs[k * 3 + 2]
        val ra = find(a); val rb = find(b)
        Drill.compare(ra, rb)
        if (ra != rb) {
            parent[ra] = rb
            Drill.write(ra, rb)
            groups -= 1
            if (groups == 1) return t
        }
    }
    return -1
}
""",
    mutants=[
        ("input-order--no-sort", "WRONG_ALGORITHM",
         "기록을 주어진 순서대로 반영한다. 시각순이 아니면 답이 나중 기록의 시각이 된다.",
         """
fun earliestAllConnected(n: Int, logs: IntArray): Int {
    if (n == 1) return 0
    val parent = IntArray(n) { it }
    fun find(x: Int): Int { var v = x; while (parent[v] != v) v = parent[v]; return v }
    var groups = n
    var i = 0
    while (i < logs.size) {
        val ra = find(logs[i + 1]); val rb = find(logs[i + 2])
        if (ra != rb) { parent[ra] = rb; groups -= 1; if (groups == 1) return logs[i] }
        i += 3
    }
    return -1
}
"""),
        ("counts-logs--not-merges", "WRONG_BRANCH",
         "무리가 실제로 합쳐졌는지 보지 않고 기록 n-1 개째의 시각을 답한다.",
         """
fun earliestAllConnected(n: Int, logs: IntArray): Int {
    if (n == 1) return 0
    val m = logs.size / 3
    val order = (0 until m).sortedBy { logs[it * 3] }
    var merges = 0
    for (k in order) {
        merges += 1
        if (merges == n - 1) return logs[k * 3]
    }
    return -1
}
"""),
        ("single-person--returns-minus-one", "MISSING_EDGE_CASE",
         "사람이 한 명일 때 -1 을 돌려준다. 혼자면 이미 모두가 아는 사이다.",
         """
fun earliestAllConnected(n: Int, logs: IntArray): Int {
    val m = logs.size / 3
    val order = (0 until m).sortedBy { logs[it * 3] }
    val parent = IntArray(n) { it }
    fun find(x: Int): Int { var v = x; while (parent[v] != v) { parent[v] = parent[parent[v]]; v = parent[v] }; return v }
    var groups = n
    for (k in order) {
        val ra = find(logs[k * 3 + 1]); val rb = find(logs[k * 3 + 2])
        if (ra != rb) { parent[ra] = rb; groups -= 1; if (groups == 1) return logs[k * 3] }
    }
    return -1
}
"""),
        ("recount-by-bfs--each-log", "PERFORMANCE",
         "기록마다 그래프를 처음부터 훑어 무리 수를 다시 센다. O(m·(n+m)).",
         """
fun earliestAllConnected(n: Int, logs: IntArray): Int {
    if (n == 1) return 0
    val m = logs.size / 3
    val order = (0 until m).sortedBy { logs[it * 3] }
    val graph = Array(n) { mutableListOf<Int>() }
    for (k in order) {
        val a = logs[k * 3 + 1]; val b = logs[k * 3 + 2]
        graph[a].add(b); graph[b].add(a)
        val seen = BooleanArray(n)
        val queue = ArrayDeque<Int>()
        queue.addLast(0); seen[0] = true
        var count = 1
        while (queue.isNotEmpty()) {
            val v = queue.removeFirst()
            for (nxt in graph[v]) { Drill.compare(v, nxt); if (!seen[nxt]) { seen[nxt] = true; count += 1; queue.addLast(nxt) } }
        }
        if (count == n) return logs[k * 3]
    }
    return -1
}
"""),
    ],
))


# --- 145. 공통 소인수로 이어진 가장 큰 무리 (유니온파인드 + 소인수 체) ----------------------------------

def _largest_common_factor_component(nums):
    limit = max(nums)
    spf = list(range(limit + 1))
    for i in range(2, int(limit ** 0.5) + 1):
        if spf[i] == i:
            for k in range(i * i, limit + 1, i):
                if spf[k] == k:
                    spf[k] = i
    parent = list(range(limit + 1))

    def find(x):
        while parent[x] != x:
            parent[x] = parent[parent[x]]
            x = parent[x]
        return x

    def union(a, b):
        a, b = find(a), find(b)
        if a != b:
            parent[a] = b

    for x in nums:
        v = x
        while v > 1:
            p = spf[v]
            union(x, p)
            while v % p == 0:
                v //= p
    counts = {}
    best = 0
    for x in nums:
        r = find(x)
        counts[r] = counts.get(r, 0) + 1
        best = max(best, counts[r])
    return best


PROBLEMS.append(Problem(
    id="largest-common-factor-component",
    title="공통 소인수로 이어진 가장 큰 무리",
    summary="""
서로 다른 양의 정수 배열 `nums` 가 주어진다. 두 수가 `1` 보다 큰 공약수를 가지면 이어져 있다.
이어짐을 따라 만들어지는 무리(연결 요소) 중 가장 큰 것의 크기를 반환한다.
""",
    notes="""
쌍마다 gcd 를 보면 n² 이다. 두 수가 이어진다는 것은 **공통 소인수**가 있다는 것이니, 수를 그
소인수들과 묶으면 — 수 하나를 자기 소인수 각각과 union — 같은 소인수를 가진 수들은 저절로
한 무리다. 소인수는 최대값까지의 최소 소인수 체로 O(log) 에 나온다. 1 은 소인수가 없어 혼자다.
""",
    drill_doc="""
Drill.compare(x, p)           // 수를 소인수와 묶었다
Drill.write(root, size)       // 무리의 크기를 늘렸다
""",
    constraints="""
- `1 <= nums.length <= 20_000`, 원소는 서로 다르다
- `1 <= nums[i] <= 100_000`
""",
    signature=dict(name="largestCommonFactorComponent", parameters=[("nums", "INT_ARRAY")], returns="INT"),
    groups=perf_groups(time_multiplier=0.5),
    reference=_largest_common_factor_component,
    cases={
        "sample": [("01", [[4, 6, 15, 35]]), ("02", [[20, 50, 9, 63]])],
        "boundary": [
            ("01-single", [[7]]),
            # 1 은 아무와도 이어지지 않는다.
            ("02-one-alone", [[1, 2, 3]]),
            ("03-all-primes", [[2, 3, 5, 7, 11]]),
            # 같은 소수의 거듭제곱들은 한 무리다.
            ("04-prime-powers", [[2, 4, 8, 16, 3]]),
            # 사슬: 6-10-15 는 쌍마다 다른 소수로 이어진다.
            ("05-chain-through-different-primes", [[6, 10, 15, 77]]),
            ("06-large-primes-alone", [[99991, 99989, 2, 4]]),
        ],
        "hidden": [
            ("01-random-small", [shuffled(sorted(set(randoms(15, 1, 60, salt=8731))), salt=8732)]),
            ("02-random-medium", [shuffled(sorted(set(randoms(500, 1, 5000, salt=8733))), salt=8734)]),
            ("03-random-wide", [shuffled(sorted(set(randoms(2000, 1, 100000, salt=8735))), salt=8736)]),
            ("04-evens-and-odd-primes", [[2 * k for k in range(1, 200)] + [99991, 99989, 99971]]),
        ],
        "performance": [
            ("01-small", [shuffled(sorted(set(randoms(5000, 1, 100000, salt=8741))), salt=8742)]),
            ("02-medium", [shuffled(sorted(set(randoms(12000, 1, 100000, salt=8743))), salt=8744)]),
            ("03-large", [shuffled(list(range(80001, 100001)), salt=8745)]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 최소 소인수 체 + 수를 소인수와 union.
fun largestCommonFactorComponent(nums: IntArray): Int {
    val limit = nums.max()
    val spf = IntArray(limit + 1) { it }
    var i = 2
    while (i.toLong() * i <= limit) {
        if (spf[i] == i) { var k = i * i; while (k <= limit) { if (spf[k] == k) spf[k] = i; k += i } }
        i += 1
    }
    val parent = IntArray(limit + 1) { it }
    fun find(x: Int): Int { var r = x; while (parent[r] != r) { parent[r] = parent[parent[r]]; r = parent[r] }; return r }
    fun union(a: Int, b: Int) { val ra = find(a); val rb = find(b); if (ra != rb) parent[ra] = rb }
    for (x in nums) {
        var v = x
        while (v > 1) {
            val p = spf[v]
            Drill.compare(x, p)
            union(x, p)
            while (v % p == 0) v /= p
        }
    }
    val counts = HashMap<Int, Int>()
    var best = 0
    for (x in nums) {
        val root = find(x)
        val size = (counts[root] ?: 0) + 1
        counts[root] = size
        Drill.write(root, size)
        if (size > best) best = size
    }
    return best
}
""",
    mutants=[
        ("unions-only-smallest-prime", "MISSING_EDGE_CASE",
         "수를 가장 작은 소인수와만 묶는다. 6 과 15 처럼 큰 소인수로만 이어진 쌍이 끊긴다.",
         """
fun largestCommonFactorComponent(nums: IntArray): Int {
    val limit = nums.max()
    val spf = IntArray(limit + 1) { it }
    var i = 2
    while (i.toLong() * i <= limit) { if (spf[i] == i) { var k = i * i; while (k <= limit) { if (spf[k] == k) spf[k] = i; k += i } }; i += 1 }
    val parent = IntArray(limit + 1) { it }
    fun find(x: Int): Int { var r = x; while (parent[r] != r) { parent[r] = parent[parent[r]]; r = parent[r] }; return r }
    fun union(a: Int, b: Int) { val ra = find(a); val rb = find(b); if (ra != rb) parent[ra] = rb }
    for (x in nums) if (x > 1) union(x, spf[x])
    val counts = HashMap<Int, Int>()
    var best = 0
    for (x in nums) { val size = (counts[find(x)] ?: 0) + 1; counts[find(x)] = size; if (size > best) best = size }
    return best
}
"""),
        ("counts-primes-as-members", "WRONG_BRANCH",
         "무리의 크기를 소수 정점까지 세어 답한다. 배열에 없는 소수가 크기에 들어간다.",
         """
fun largestCommonFactorComponent(nums: IntArray): Int {
    val limit = nums.max()
    val spf = IntArray(limit + 1) { it }
    var i = 2
    while (i.toLong() * i <= limit) { if (spf[i] == i) { var k = i * i; while (k <= limit) { if (spf[k] == k) spf[k] = i; k += i } }; i += 1 }
    val parent = IntArray(limit + 1) { it }
    val size = IntArray(limit + 1) { 1 }
    fun find(x: Int): Int { var r = x; while (parent[r] != r) { parent[r] = parent[parent[r]]; r = parent[r] }; return r }
    fun union(a: Int, b: Int) { val ra = find(a); val rb = find(b); if (ra != rb) { parent[ra] = rb; size[rb] += size[ra] } }
    for (x in nums) { var v = x; while (v > 1) { val p = spf[v]; union(x, p); while (v % p == 0) v /= p } }
    var best = 0
    for (x in nums) best = maxOf(best, size[find(x)])
    return best
}
"""),
        ("one-joins-everything", "MISSING_EDGE_CASE",
         "1 을 소인수 1 과 묶고 다른 수도 1 과 묶는다 — 1 이 있으면 전부 한 무리가 된다.",
         """
fun largestCommonFactorComponent(nums: IntArray): Int {
    val limit = nums.max()
    val spf = IntArray(limit + 1) { it }
    var i = 2
    while (i.toLong() * i <= limit) { if (spf[i] == i) { var k = i * i; while (k <= limit) { if (spf[k] == k) spf[k] = i; k += i } }; i += 1 }
    val parent = IntArray(limit + 1) { it }
    fun find(x: Int): Int { var r = x; while (parent[r] != r) { parent[r] = parent[parent[r]]; r = parent[r] }; return r }
    fun union(a: Int, b: Int) { val ra = find(a); val rb = find(b); if (ra != rb) parent[ra] = rb }
    for (x in nums) { var v = x; while (v >= 1) { val p = spf[v]; union(x, p); if (p == 1) break; while (v % p == 0) v /= p } }
    val counts = HashMap<Int, Int>()
    var best = 0
    for (x in nums) { val size = (counts[find(x)] ?: 0) + 1; counts[find(x)] = size; if (size > best) best = size }
    return best
}
"""),
        ("pairwise-gcd--quadratic", "PERFORMANCE",
         "모든 쌍의 gcd 를 본다. O(n² log).",
         """
fun largestCommonFactorComponent(nums: IntArray): Int {
    fun gcd(a: Int, b: Int): Int { var x = a; var y = b; while (y != 0) { val t = x % y; x = y; y = t }; return x }
    val n = nums.size
    val parent = IntArray(n) { it }
    fun find(x: Int): Int { var r = x; while (parent[r] != r) { parent[r] = parent[parent[r]]; r = parent[r] }; return r }
    for (i in 0 until n) for (j in i + 1 until n) {
        Drill.compare(i, j)
        if (gcd(nums[i], nums[j]) > 1) { val a = find(i); val b = find(j); if (a != b) parent[a] = b }
    }
    val counts = IntArray(n)
    var best = 0
    for (i in 0 until n) { val r = find(i); counts[r] += 1; if (counts[r] > best) best = counts[r] }
    return best
}
"""),
    ],
))


# --- 146. 외계어 사전의 글자 순서 (위상 정렬, 사전순 최소) ---------------------------------------------

def _alien_order(words):
    import heapq
    letters = set("".join(words))
    after = {c: set() for c in letters}
    indegree = {c: 0 for c in letters}
    for a, b in zip(words, words[1:]):
        for x, y in zip(a, b):
            if x != y:
                if y not in after[x]:
                    after[x].add(y)
                    indegree[y] += 1
                break
        else:
            if len(a) > len(b):
                return ""
    heap = [c for c in letters if indegree[c] == 0]
    heapq.heapify(heap)
    out = []
    while heap:
        c = heapq.heappop(heap)
        out.append(c)
        for d in sorted(after[c]):
            indegree[d] -= 1
            if indegree[d] == 0:
                heapq.heappush(heap, d)
    return "".join(out) if len(out) == len(letters) else ""


def _alien_words(order, count, max_len, salt):
    """숨은 순서 `order` 에 맞게 정렬된 단어들. 정렬 후 인접 쌍만 순서를 준다."""
    rank = {c: i for i, c in enumerate(order)}
    lengths = randoms(count, 1, max_len, salt=salt)
    picks = randoms(count * max_len, 0, len(order) - 1, salt=salt + 1)
    words = []
    for i in range(count):
        words.append("".join(order[picks[i * max_len + j]] for j in range(lengths[i])))
    words.sort(key=lambda w: [rank[c] for c in w])
    return words


PROBLEMS.append(Problem(
    id="alien-dictionary-order",
    title="외계어 사전의 글자 순서",
    summary="""
소문자로 된 단어들이 **어떤 알 수 없는 글자 순서**로 정렬돼 있다. 그 순서와 모순되지 않는 글자
배열을 문자열로 반환한다 — 단어들에 나오는 글자 전부를 한 번씩. 가능한 배열이 여럿이면
**사전순(보통의 a..z 순)으로 가장 앞선 것**을, 모순이 있으면 빈 문자열 `""` 을 반환한다.
""",
    notes="""
인접한 두 단어에서 처음으로 다른 글자 쌍이 "앞 < 뒤" 하나를 준다 — 그 뒤의 글자는 아무 정보가
없다. 앞 단어가 뒤 단어의 진짜 접두사보다 길면(`["abc", "ab"]`) 모순이다. 간선들 위의 위상
정렬에서 들어오는 간선이 없는 글자를 **최소 힙**으로 뽑으면 사전순 최소이고, 다 뽑지 못하면
순환이라 모순이다. 같은 쌍의 간선을 두 번 세면 진입 차수가 어긋난다.
""",
    drill_doc="""
Drill.compare(x, y)           // 글자 x 가 y 보다 앞이라는 것을 얻었다
Drill.write(i, letter)        // 글자를 순서에 놓았다
""",
    constraints="""
- `1 <= words.length <= 10_000`, `1 <= words[i].length <= 20`, 소문자
""",
    signature=dict(name="alienDictionaryOrder", parameters=[("words", "STRING_ARRAY")], returns="STRING"),
    groups=standard_groups(),
    reference=_alien_order,
    cases={
        "sample": [("01", [["wrt", "wrf", "er", "ett", "rftt"]]), ("02", [["z", "x", "z"]])],
        "boundary": [
            ("01-single-word", [["zyx"]]),
            # 정보가 없으면 사전순.
            ("02-no-constraints", [["b", "b"]]),
            ("03-prefix-longer-first", [["abc", "ab"]]),
            ("04-prefix-shorter-first", [["ab", "abc"]]),
            # 같은 간선이 두 번 — 진입 차수를 두 번 올리면 안 된다.
            ("05-duplicate-edge", [["ba", "bb", "ca", "cb"]]),
            # 순환.
            ("06-cycle", [["ab", "ba", "ab"]]),
            # 여러 답 중 사전순 최소: c 가 먼저여야 하지만 a·b 는 자유.
            ("07-lexicographic-tie", [["cb", "ca"]]),
            ("08-unconstrained-letters", [["dz", "da"]]),
        ],
        "hidden": [
            ("01-permuted-small", [_alien_words("dcba", 8, 3, salt=8751)]),
            ("02-permuted-medium", [_alien_words("hgfedcba", 200, 5, salt=8753)]),
            ("03-permuted-full", [_alien_words("zyxwvutsrqponmlkjihgfedcba", 3000, 8, salt=8755)]),
            ("04-partial-order", [_alien_words("qwertyuiop", 1000, 20, salt=8757)]),
            ("05-large", [_alien_words("mnbvcxzlkjhgfdsapoiuytrewq", 10000, 20, salt=8759)]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 인접 쌍에서 간선, 최소 힙 Kahn.
fun alienDictionaryOrder(words: Array<String>): String {
    val present = BooleanArray(26)
    for (w in words) for (c in w) present[c - 'a'] = true
    val after = Array(26) { BooleanArray(26) }
    val indegree = IntArray(26)
    for (i in 0 until words.size - 1) {
        val a = words[i]; val b = words[i + 1]
        var j = 0
        while (j < a.length && j < b.length && a[j] == b[j]) j += 1
        if (j == minOf(a.length, b.length)) { if (a.length > b.length) return ""; continue }
        val x = a[j] - 'a'; val y = b[j] - 'a'
        if (!after[x][y]) { after[x][y] = true; indegree[y] += 1; Drill.compare(x, y) }
    }
    val heap = java.util.PriorityQueue<Int>()
    for (c in 0 until 26) if (present[c] && indegree[c] == 0) heap.add(c)
    val out = StringBuilder()
    while (heap.isNotEmpty()) {
        val c = heap.poll()
        Drill.write(out.length, c)
        out.append('a' + c)
        for (d in 0 until 26) if (after[c][d]) { indegree[d] -= 1; if (indegree[d] == 0) heap.add(d) }
    }
    val total = present.count { it }
    return if (out.length == total) out.toString() else ""
}
""",
    mutants=[
        ("ignores-prefix-conflict", "MISSING_EDGE_CASE",
         "앞 단어가 뒤 단어의 접두사보다 길어도 모순으로 보지 않는다.",
         """
fun alienDictionaryOrder(words: Array<String>): String {
    val present = BooleanArray(26)
    for (w in words) for (c in w) present[c - 'a'] = true
    val after = Array(26) { BooleanArray(26) }
    val indegree = IntArray(26)
    for (i in 0 until words.size - 1) {
        val a = words[i]; val b = words[i + 1]
        var j = 0
        while (j < a.length && j < b.length && a[j] == b[j]) j += 1
        if (j == minOf(a.length, b.length)) continue
        val x = a[j] - 'a'; val y = b[j] - 'a'
        if (!after[x][y]) { after[x][y] = true; indegree[y] += 1 }
    }
    val heap = java.util.PriorityQueue<Int>()
    for (c in 0 until 26) if (present[c] && indegree[c] == 0) heap.add(c)
    val out = StringBuilder()
    while (heap.isNotEmpty()) { val c = heap.poll(); out.append('a' + c); for (d in 0 until 26) if (after[c][d]) { indegree[d] -= 1; if (indegree[d] == 0) heap.add(d) } }
    return if (out.length == present.count { it }) out.toString() else ""
}
"""),
        ("counts-duplicate-edges", "OFF_BY_ONE",
         "같은 간선을 볼 때마다 진입 차수를 올린다. 두 번 나온 간선의 끝 글자가 영원히 안 나온다.",
         """
fun alienDictionaryOrder(words: Array<String>): String {
    val present = BooleanArray(26)
    for (w in words) for (c in w) present[c - 'a'] = true
    val after = Array(26) { BooleanArray(26) }
    val indegree = IntArray(26)
    for (i in 0 until words.size - 1) {
        val a = words[i]; val b = words[i + 1]
        var j = 0
        while (j < a.length && j < b.length && a[j] == b[j]) j += 1
        if (j == minOf(a.length, b.length)) { if (a.length > b.length) return ""; continue }
        val x = a[j] - 'a'; val y = b[j] - 'a'
        after[x][y] = true; indegree[y] += 1
    }
    val heap = java.util.PriorityQueue<Int>()
    for (c in 0 until 26) if (present[c] && indegree[c] == 0) heap.add(c)
    val out = StringBuilder()
    while (heap.isNotEmpty()) { val c = heap.poll(); out.append('a' + c); for (d in 0 until 26) if (after[c][d]) { indegree[d] -= 1; if (indegree[d] == 0) heap.add(d) } }
    return if (out.length == present.count { it }) out.toString() else ""
}
"""),
        ("plain-queue--not-lexicographic", "WRONG_BRANCH",
         "최소 힙 대신 보통 큐를 쓴다. 답이 여럿일 때 사전순 최소가 아니다.",
         """
fun alienDictionaryOrder(words: Array<String>): String {
    val present = BooleanArray(26)
    for (w in words) for (c in w) present[c - 'a'] = true
    val after = Array(26) { BooleanArray(26) }
    val indegree = IntArray(26)
    for (i in 0 until words.size - 1) {
        val a = words[i]; val b = words[i + 1]
        var j = 0
        while (j < a.length && j < b.length && a[j] == b[j]) j += 1
        if (j == minOf(a.length, b.length)) { if (a.length > b.length) return ""; continue }
        val x = a[j] - 'a'; val y = b[j] - 'a'
        if (!after[x][y]) { after[x][y] = true; indegree[y] += 1 }
    }
    val queue = ArrayDeque<Int>()
    for (c in 0 until 26) if (present[c] && indegree[c] == 0) queue.addLast(c)
    val out = StringBuilder()
    while (queue.isNotEmpty()) { val c = queue.removeFirst(); out.append('a' + c); for (d in 25 downTo 0) if (after[c][d]) { indegree[d] -= 1; if (indegree[d] == 0) queue.addLast(d) } }
    return if (out.length == present.count { it }) out.toString() else ""
}
"""),
        ("compares-all-positions", "WRONG_ALGORITHM",
         "처음 다른 글자 뒤의 자리들도 순서로 삼는다. 없는 제약이 생겨 모순이 난다.",
         """
fun alienDictionaryOrder(words: Array<String>): String {
    val present = BooleanArray(26)
    for (w in words) for (c in w) present[c - 'a'] = true
    val after = Array(26) { BooleanArray(26) }
    val indegree = IntArray(26)
    for (i in 0 until words.size - 1) {
        val a = words[i]; val b = words[i + 1]
        var j = 0
        var differed = false
        while (j < a.length && j < b.length) {
            if (a[j] != b[j]) { differed = true; val x = a[j] - 'a'; val y = b[j] - 'a'; if (!after[x][y]) { after[x][y] = true; indegree[y] += 1 } }
            j += 1
        }
        if (!differed && a.length > b.length) return ""
    }
    val heap = java.util.PriorityQueue<Int>()
    for (c in 0 until 26) if (present[c] && indegree[c] == 0) heap.add(c)
    val out = StringBuilder()
    while (heap.isNotEmpty()) { val c = heap.poll(); out.append('a' + c); for (d in 0 until 26) if (after[c][d]) { indegree[d] -= 1; if (indegree[d] == 0) heap.add(d) } }
    return if (out.length == present.count { it }) out.toString() else ""
}
"""),
    ],
))


# --- 150. 모든 정점을 지나는 가장 짧은 걸음 (비트마스크 BFS) --------------------------------------------

def _visit_all_nodes(n, edges):
    from collections import deque
    adj = [[] for _ in range(n)]
    for i in range(0, len(edges), 2):
        u, v = edges[i], edges[i + 1]
        adj[u].append(v)
        adj[v].append(u)
    full = (1 << n) - 1
    if n == 1:
        return 0
    dist = [[-1] * n for _ in range(1 << n)]
    queue = deque()
    for s in range(n):
        dist[1 << s][s] = 0
        queue.append((1 << s, s))
    while queue:
        mask, u = queue.popleft()
        d = dist[mask][u]
        for v in adj[u]:
            nm = mask | (1 << v)
            if dist[nm][v] < 0:
                if nm == full:
                    return d + 1
                dist[nm][v] = d + 1
                queue.append((nm, v))
    return -1


def _connected_edges(n, extra, salt):
    """트리 + 여분 간선. 연결을 보장한다."""
    parents = randoms(n - 1, 0, n - 1, salt=salt)
    edges = []
    for i in range(1, n):
        edges += [parents[i - 1] % i, i]
    a = randoms(extra, 0, n - 1, salt=salt + 1)
    b = randoms(extra, 0, n - 1, salt=salt + 2)
    for i in range(extra):
        if a[i] != b[i]:
            edges += [a[i], b[i]]
    return edges


PROBLEMS.append(Problem(
    id="shortest-path-visiting-all-nodes",
    title="모든 정점을 지나는 가장 짧은 걸음",
    summary="""
정점 `n` 개의 **연결된** 무향 그래프가 간선 목록 `edges = [u1, v1, u2, v2, ...]` 로 주어진다.
아무 정점에서 시작해 아무 정점에서 끝나며, 정점과 간선을 **다시 지나도 되는** 경로로 모든
정점을 한 번 이상 방문하는 데 필요한 최소 간선 수를 반환한다. 정점이 하나면 `0` 이다.
""",
    notes="""
방문 순서를 다 세면 n! 이다. 상태를 **(방문한 정점 집합, 지금 정점)** 으로 두면 `2^n · n` 개뿐이고,
간선 하나가 비용 1 이라 BFS 다. 모든 정점에서 동시에 출발시켜(다중 시작) 집합이 가득 차는 첫 순간의
거리가 답이다. 정점을 다시 지나도 되니 집합은 커지기만 하고, 같은 (집합, 정점)을 두 번 넣지 않는다.
""",
    drill_doc="""
Drill.compare(mask, node)     // 상태를 꺼냈다
Drill.write(0, distance)      // 답을 정했다
""",
    constraints="""
- `1 <= n <= 12`, 그래프는 연결돼 있다, 중복 간선이 있을 수 있다
- `edges.size` 는 짝수, `0 <= u, v < n`, `u != v`
""",
    signature=dict(name="shortestPathVisitingAllNodes", parameters=[("n", "INT"), ("edges", "INT_ARRAY")], returns="INT"),
    groups=perf_groups(time_multiplier=0.5),
    reference=_visit_all_nodes,
    cases={
        "sample": [("01", [4, [1, 0, 1, 2, 1, 3]]), ("02", [5, [0, 1, 0, 2, 1, 3, 2, 4]])],
        "boundary": [
            ("01-single-node", [1, []]),
            ("02-two-nodes", [2, [0, 1]]),
            # 별: 가운데를 세 번 지나야 한다 — 다시 지나도 된다.
            ("03-star", [5, [0, 1, 0, 2, 0, 3, 0, 4]]),
            ("04-path", [6, [0, 1, 1, 2, 2, 3, 3, 4, 4, 5]]),
            ("05-complete-triangle", [3, [0, 1, 1, 2, 0, 2]]),
            ("06-duplicate-edges", [3, [0, 1, 0, 1, 1, 2]]),
            # 사이클: 한 바퀴 돌 필요 없이 n-1 걸음.
            ("07-cycle", [6, [0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 0]]),
        ],
        "hidden": [
            ("01-random-small", [5, _connected_edges(5, 2, salt=8841)]),
            ("02-random-medium", [8, _connected_edges(8, 4, salt=8843)]),
            ("03-random-tree", [10, _connected_edges(10, 0, salt=8845)]),
            ("04-random-dense", [11, _connected_edges(11, 30, salt=8847)]),
        ],
        "performance": [
            ("01-small", [10, _connected_edges(10, 5, salt=8851)]),
            ("02-medium", [11, _connected_edges(11, 6, salt=8853)]),
            ("03-large", [12, _connected_edges(12, 8, salt=8855)]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). (집합, 정점) 상태의 다중 시작 BFS.
fun shortestPathVisitingAllNodes(n: Int, edges: IntArray): Int {
    if (n == 1) return 0
    val adj = Array(n) { ArrayList<Int>() }
    for (i in edges.indices step 2) { adj[edges[i]].add(edges[i + 1]); adj[edges[i + 1]].add(edges[i]) }
    val full = (1 shl n) - 1
    val seen = BooleanArray((1 shl n) * n)
    val queue = IntArray((1 shl n) * n)
    var head = 0; var tail = 0
    for (s in 0 until n) { seen[(1 shl s) * n + s] = true; queue[tail++] = (1 shl s) * n + s }
    var distance = 0
    while (head < tail) {
        val levelEnd = tail
        while (head < levelEnd) {
            val state = queue[head++]
            val mask = state / n; val u = state % n
            Drill.compare(mask, u)
            for (v in adj[u]) {
                val nm = mask or (1 shl v)
                if (nm == full) { Drill.write(0, distance + 1); return distance + 1 }
                val next = nm * n + v
                if (!seen[next]) { seen[next] = true; queue[tail++] = next }
            }
        }
        distance += 1
    }
    return -1
}
""",
    mutants=[
        ("no-revisit--simple-paths-only", "WRONG_ALGORITHM",
         "정점을 다시 지나지 못하게 한다. 별 모양에서 답이 없다.",
         """
fun shortestPathVisitingAllNodes(n: Int, edges: IntArray): Int {
    if (n == 1) return 0
    val adj = Array(n) { ArrayList<Int>() }
    for (i in edges.indices step 2) { adj[edges[i]].add(edges[i + 1]); adj[edges[i + 1]].add(edges[i]) }
    val full = (1 shl n) - 1
    val seen = BooleanArray((1 shl n) * n)
    val queue = IntArray((1 shl n) * n)
    var head = 0; var tail = 0
    for (s in 0 until n) { seen[(1 shl s) * n + s] = true; queue[tail++] = (1 shl s) * n + s }
    var distance = 0
    while (head < tail) {
        val levelEnd = tail
        while (head < levelEnd) {
            val state = queue[head++]
            val mask = state / n; val u = state % n
            for (v in adj[u]) {
                if (mask and (1 shl v) != 0) continue
                val nm = mask or (1 shl v)
                if (nm == full) return distance + 1
                val next = nm * n + v
                if (!seen[next]) { seen[next] = true; queue[tail++] = next }
            }
        }
        distance += 1
    }
    return n * n
}
"""),
        ("single-start-from-zero", "WRONG_BRANCH",
         "정점 0 에서만 출발한다. 시작점을 고를 수 없으니 답이 커진다.",
         """
fun shortestPathVisitingAllNodes(n: Int, edges: IntArray): Int {
    if (n == 1) return 0
    val adj = Array(n) { ArrayList<Int>() }
    for (i in edges.indices step 2) { adj[edges[i]].add(edges[i + 1]); adj[edges[i + 1]].add(edges[i]) }
    val full = (1 shl n) - 1
    val seen = BooleanArray((1 shl n) * n)
    val queue = IntArray((1 shl n) * n)
    var head = 0; var tail = 0
    seen[1 * n] = true; queue[tail++] = 1 * n
    var distance = 0
    while (head < tail) {
        val levelEnd = tail
        while (head < levelEnd) {
            val state = queue[head++]
            val mask = state / n; val u = state % n
            for (v in adj[u]) {
                val nm = mask or (1 shl v)
                if (nm == full) return distance + 1
                val next = nm * n + v
                if (!seen[next]) { seen[next] = true; queue[tail++] = next }
            }
        }
        distance += 1
    }
    return -1
}
"""),
        ("mask-only-state--drops-position", "WRONG_ALGORITHM",
         "상태를 집합만으로 둔다. 어디 서 있는지를 잊어 이어질 수 없는 걸음을 잇는다.",
         """
fun shortestPathVisitingAllNodes(n: Int, edges: IntArray): Int {
    if (n == 1) return 0
    val adj = Array(n) { ArrayList<Int>() }
    for (i in edges.indices step 2) { adj[edges[i]].add(edges[i + 1]); adj[edges[i + 1]].add(edges[i]) }
    val full = (1 shl n) - 1
    val seen = BooleanArray(1 shl n)
    val queue = IntArray(1 shl n)
    var head = 0; var tail = 0
    for (s in 0 until n) { if (!seen[1 shl s]) { seen[1 shl s] = true; queue[tail++] = 1 shl s } }
    var distance = 0
    while (head < tail) {
        val levelEnd = tail
        while (head < levelEnd) {
            val mask = queue[head++]
            for (u in 0 until n) {
                if (mask and (1 shl u) == 0) continue
                for (v in adj[u]) {
                    val nm = mask or (1 shl v)
                    if (nm == full) return distance + 1
                    if (!seen[nm]) { seen[nm] = true; queue[tail++] = nm }
                }
            }
        }
        distance += 1
    }
    return -1
}
"""),
        # 가지치기(total >= best 면 중단)를 두면 12 에서도 제한 안에 든다 — 맞는 풀이가 된다. 가지치기 없는 판이다.
        ("permutation-search--factorial", "PERFORMANCE",
         "방문 순서 전부를 시도하며 쌍마다 최단 거리를 더한다. 가지치기가 없어 O(n! · n).",
         """
fun shortestPathVisitingAllNodes(n: Int, edges: IntArray): Int {
    if (n == 1) return 0
    val adj = Array(n) { ArrayList<Int>() }
    for (i in edges.indices step 2) { adj[edges[i]].add(edges[i + 1]); adj[edges[i + 1]].add(edges[i]) }
    val dist = Array(n) { IntArray(n) { -1 } }
    for (s in 0 until n) {
        val queue = ArrayDeque<Int>(); dist[s][s] = 0; queue.add(s)
        while (queue.isNotEmpty()) { val u = queue.removeFirst(); for (v in adj[u]) if (dist[s][v] < 0) { dist[s][v] = dist[s][u] + 1; queue.add(v) } }
    }
    var best = Int.MAX_VALUE
    val used = BooleanArray(n)
    fun go(last: Int, count: Int, total: Int) {
        if (count == n) { if (total < best) best = total; return }
        for (v in 0 until n) if (!used[v]) { Drill.compare(last, v); used[v] = true; go(v, count + 1, total + dist[last][v]); used[v] = false }
    }
    for (s in 0 until n) { used[s] = true; go(s, 1, 0); used[s] = false }
    return best
}
"""),
    ],
))


# --- 157. 네트워크를 잇는 최소 작업 수 (유니온파인드) --------------------------------------------------

def _make_connected(n, cables):
    m = len(cables) // 2
    if m < n - 1:
        return -1
    parent = list(range(n))

    def find(x):
        while parent[x] != x:
            parent[x] = parent[parent[x]]
            x = parent[x]
        return x

    components = n
    for i in range(m):
        a, b = find(cables[2 * i]), find(cables[2 * i + 1])
        if a != b:
            parent[a] = b
            components -= 1
    return components - 1


def _cables(n, m, salt):
    a = randoms(m, 0, n - 1, salt=salt)
    b = randoms(m, 0, n - 1, salt=salt + 1)
    return flat([a[i], b[i]] for i in range(m))


PROBLEMS.append(Problem(
    id="make-network-connected",
    title="네트워크를 잇는 최소 작업 수",
    summary="""
컴퓨터 `n` 대(`0..n-1`)와 케이블 목록 `cables = [a1, b1, a2, b2, ...]` 가 주어진다. 케이블 하나를
뽑아 다른 두 컴퓨터 사이에 꽂는 것이 **작업 하나**다. 모든 컴퓨터가 서로 이어지게 하는 최소
작업 수를 반환한다. 케이블이 모자라 불가능하면 `-1`. 같은 쌍의 케이블이 여럿일 수 있다.
""",
    notes="""
`n` 대를 잇는 데 케이블은 최소 `n-1` 개다 — 그보다 적으면 `-1`. 충분하면 답은 **연결 요소 수 − 1**
이다: 남는 케이블(순환을 만드는 것)은 언제나 요소 수 − 1 개 이상이라 그것으로 요소들을 하나씩
잇는다. 요소 수는 유니온파인드로 센다 — 케이블마다 두 끝이 이미 같은 무리면 그 케이블이 여분이다.
""",
    drill_doc="""
Drill.compare(a, b)           // 케이블의 두 끝을 봤다
Drill.write(0, components)    // 무리 수를 줄였다
""",
    constraints="""
- `1 <= n <= 100_000`, `0 <= cables.size / 2 <= 200_000`
""",
    signature=dict(name="makeNetworkConnected", parameters=[("n", "INT"), ("cables", "INT_ARRAY")], returns="INT"),
    groups=perf_groups(time_multiplier=0.5),
    reference=_make_connected,
    cases={
        "sample": [("01", [4, [0, 1, 0, 2, 1, 2]]), ("02", [6, [0, 1, 0, 2, 0, 3, 1, 2, 1, 3]])],
        "boundary": [
            ("01-single-computer", [1, []]),
            ("02-two-unconnected-no-cable", [2, []]),
            ("03-already-connected", [3, [0, 1, 1, 2]]),
            # 케이블이 n-1 개인데 순환이 있어 모자란다? 아니다 — n-1 개면 요소 수 − 1 만큼 여분이 정확히 있다.
            ("04-exactly-enough-with-cycle", [4, [0, 1, 1, 0, 2, 3]]),
            ("05-not-enough", [6, [0, 1, 0, 2, 0, 3, 1, 2]]),
            # 같은 쌍이 여럿.
            ("06-duplicate-cables", [3, [0, 1, 0, 1, 0, 1]]),
            ("07-self-loop-cable", [2, [0, 0]]),
        ],
        "hidden": [
            ("01-random-small", [8, _cables(8, 7, salt=8911)]),
            ("02-random-medium", [300, _cables(300, 320, salt=8913)]),
            ("03-random-sparse", [1000, _cables(1000, 999, salt=8915)]),
            ("04-random-dense", [500, _cables(500, 3000, salt=8917)]),
        ],
        "performance": [
            ("01-small", [20000, _cables(20000, 30000, salt=8921)]),
            ("02-medium", [60000, _cables(60000, 100000, salt=8923)]),
            ("03-large", [100000, _cables(100000, 200000, salt=8925)]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 케이블이 모자라면 -1, 아니면 무리 수 − 1.
fun makeNetworkConnected(n: Int, cables: IntArray): Int {
    val m = cables.size / 2
    if (m < n - 1) return -1
    val parent = IntArray(n) { it }
    fun find(x: Int): Int { var r = x; while (parent[r] != r) { parent[r] = parent[parent[r]]; r = parent[r] }; return r }
    var components = n
    for (i in 0 until m) {
        val a = find(cables[2 * i]); val b = find(cables[2 * i + 1])
        Drill.compare(cables[2 * i], cables[2 * i + 1])
        if (a != b) { parent[a] = b; components -= 1; Drill.write(0, components) }
    }
    return components - 1
}
""",
    mutants=[
        ("no-shortage-check", "MISSING_EDGE_CASE",
         "케이블이 모자라도 무리 수 − 1 을 답한다.",
         """
fun makeNetworkConnected(n: Int, cables: IntArray): Int {
    val parent = IntArray(n) { it }
    fun find(x: Int): Int { var r = x; while (parent[r] != r) { parent[r] = parent[parent[r]]; r = parent[r] }; return r }
    var components = n
    for (i in 0 until cables.size / 2) { val a = find(cables[2 * i]); val b = find(cables[2 * i + 1]); if (a != b) { parent[a] = b; components -= 1 } }
    return components - 1
}
"""),
        ("counts-redundant-cables", "WRONG_ALGORITHM",
         "여분 케이블 수를 답한다. 필요한 것은 무리 수 − 1 이다.",
         """
fun makeNetworkConnected(n: Int, cables: IntArray): Int {
    val m = cables.size / 2
    if (m < n - 1) return -1
    val parent = IntArray(n) { it }
    fun find(x: Int): Int { var r = x; while (parent[r] != r) { parent[r] = parent[parent[r]]; r = parent[r] }; return r }
    var redundant = 0
    for (i in 0 until m) { val a = find(cables[2 * i]); val b = find(cables[2 * i + 1]); if (a != b) parent[a] = b else redundant += 1 }
    return redundant
}
"""),
        ("shortage-uses-n", "OFF_BY_ONE",
         "케이블이 n 개 미만이면 -1 이라고 한다. n-1 개면 충분하다.",
         """
fun makeNetworkConnected(n: Int, cables: IntArray): Int {
    val m = cables.size / 2
    if (m < n) return -1
    val parent = IntArray(n) { it }
    fun find(x: Int): Int { var r = x; while (parent[r] != r) { parent[r] = parent[parent[r]]; r = parent[r] }; return r }
    var components = n
    for (i in 0 until m) { val a = find(cables[2 * i]); val b = find(cables[2 * i + 1]); if (a != b) { parent[a] = b; components -= 1 } }
    return components - 1
}
"""),
        ("find-without-compression--chain", "PERFORMANCE",
         "경로 압축도 크기 합치기도 없이 항상 a 를 b 아래에 둔다. 사슬이 길어져 O(n²).",
         """
fun makeNetworkConnected(n: Int, cables: IntArray): Int {
    val m = cables.size / 2
    if (m < n - 1) return -1
    val parent = IntArray(n) { it }
    fun find(x: Int): Int { var r = x; while (parent[r] != r) { Drill.compare(r, parent[r]); r = parent[r] }; return r }
    var components = n
    for (i in 0 until m) { val a = find(cables[2 * i]); val b = find(cables[2 * i + 1]); if (a != b) { parent[b] = a; components -= 1 } }
    return components - 1
}
"""),
    ],
))


# --- 158. 색이 번갈아 바뀌는 최단 경로 (상태가 있는 BFS) ------------------------------------------------

def _alternating_paths(n, red, blue):
    from collections import deque
    adj = [[[] for _ in range(n)], [[] for _ in range(n)]]  # 0: 빨강, 1: 파랑
    for color, edges in ((0, red), (1, blue)):
        for i in range(0, len(edges), 2):
            adj[color][edges[i]].append(edges[i + 1])
    INF = float("inf")
    dist = [[INF] * n for _ in range(2)]
    dist[0][0] = dist[1][0] = 0
    queue = deque([(0, 0), (0, 1)])
    while queue:
        u, last = queue.popleft()
        nxt = 1 - last
        for v in adj[nxt][u]:
            if dist[nxt][v] == INF:
                dist[nxt][v] = dist[last][u] + 1
                queue.append((v, nxt))
    return [-1 if min(dist[0][v], dist[1][v]) == INF else min(dist[0][v], dist[1][v]) for v in range(n)]


def _colored_edges(n, m, salt):
    a = randoms(m, 0, n - 1, salt=salt)
    b = randoms(m, 0, n - 1, salt=salt + 1)
    return flat([a[i], b[i]] for i in range(m))


def _layered(layers, width):
    """층마다 width 개 정점, 이웃 층은 전부 잇고 색은 층마다 번갈아. 단순 경로가 width^layers 개다 —
    경로를 열거하는 풀이는 무작위 그래프에서는 빠르고 여기서만 지수다."""
    red, blue = [], []
    def node(layer, i): return 1 + (layer - 1) * width + i
    for layer in range(1, layers + 1):
        sources = [0] if layer == 1 else [node(layer - 1, i) for i in range(width)]
        target = red if layer % 2 == 1 else blue
        for u in sources:
            for i in range(width):
                target += [u, node(layer, i)]
    return 1 + layers * width, red, blue


PROBLEMS.append(Problem(
    id="alternating-color-paths",
    title="색이 번갈아 바뀌는 최단 경로",
    summary="""
정점 `n` 개(`0..n-1`)의 **방향** 그래프에 빨간 간선 `red = [u1, v1, ...]` 과 파란 간선 `blue` 가
있다. 정점 `0` 에서 출발해 간선의 색이 **번갈아** 바뀌는(빨강-파랑-빨강-… 또는 파랑-빨강-…)
경로로 각 정점에 이르는 최소 간선 수를 담은 배열을 반환한다. 이를 수 없으면 `-1`. 자기
자신으로 가는 간선과 같은 간선의 중복이 있을 수 있다.
""",
    notes="""
정점만으로는 상태가 모자란다 — 같은 정점에 빨간 간선으로 왔는지 파란 간선으로 왔는지에 따라
다음에 쓸 수 있는 간선이 다르다. 상태를 **(정점, 마지막 색)** 으로 두고 BFS 하면 상태가 `2n` 개다.
`0` 은 두 상태로 동시에 시작하고(첫 간선이 어느 색이든 되니까), 답은 두 상태의 거리 중 작은 것이다.
정점만 방문 표시하면 한 색으로 먼저 도착한 것이 다른 색으로의 도착을 막아 틀린다.
""",
    drill_doc="""
Drill.compare(u, v)           // 간선을 따라갔다
Drill.write(v, distance)      // 정점의 거리를 정했다
""",
    constraints="""
- `1 <= n <= 100_000`, 빨강·파랑 간선 각 `0..100_000` 개
""",
    signature=dict(name="alternatingColorPaths", parameters=[("n", "INT"), ("red", "INT_ARRAY"), ("blue", "INT_ARRAY")], returns="INT_ARRAY"),
    groups=perf_groups(time_multiplier=0.5),
    reference=_alternating_paths,
    limits={"timeMillis": 2000, "memoryMb": 256, "outputBytes": 2000000},
    cases={
        "sample": [("01", [3, [0, 1, 1, 2], []]), ("02", [3, [0, 1], [2, 1]]), ("03", [3, [0, 1], [1, 2]])],
        "boundary": [
            ("01-single-node", [1, [], []]),
            # 같은 정점을 두 색으로 두 번 지나야 한다.
            ("02-revisit-with-other-color", [3, [0, 1, 2, 1], [1, 2]]),
            # 자기 자신으로 가는 간선이 색을 바꾸는 데 쓰인다.
            ("03-self-loop-switches-color", [3, [0, 0, 0, 1], [0, 0, 1, 2]]),
            ("04-unreachable", [4, [0, 1], [0, 1]]),
            ("05-duplicate-edges", [2, [0, 1, 0, 1], []]),
            # 0 으로 돌아오는 거리는 0 이다.
            ("06-back-to-start", [2, [0, 1], [1, 0]]),
        ],
        "hidden": [
            ("01-random-small", [6, _colored_edges(6, 5, salt=8931), _colored_edges(6, 5, salt=8933)]),
            ("02-random-medium", [200, _colored_edges(200, 300, salt=8935), _colored_edges(200, 300, salt=8937)]),
            ("03-random-sparse", [2000, _colored_edges(2000, 1500, salt=8939), _colored_edges(2000, 1500, salt=8941)]),
            ("04-only-red", [50, _colored_edges(50, 200, salt=8943), []]),
        ],
        "performance": [
            ("01-small", [20000, _colored_edges(20000, 20000, salt=8951), _colored_edges(20000, 20000, salt=8953)]),
            ("02-medium", [60000, _colored_edges(60000, 60000, salt=8955), _colored_edges(60000, 60000, salt=8957)]),
            ("03-large", [100000, _colored_edges(100000, 100000, salt=8959), _colored_edges(100000, 100000, salt=8961)]),
            # 무작위 그래프는 경로가 짧아 열거하는 풀이도 빠르다. 층 40 개 × 3 개면 단순 경로가 3^40 개다.
            ("04-layered-exponential", list(_layered(40, 3))),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). (정점, 마지막 색) 상태의 BFS, 0 은 두 상태로 시작.
fun alternatingColorPaths(n: Int, red: IntArray, blue: IntArray): IntArray {
    val adj = Array(2) { Array(n) { ArrayList<Int>() } }
    for (i in red.indices step 2) adj[0][red[i]].add(red[i + 1])
    for (i in blue.indices step 2) adj[1][blue[i]].add(blue[i + 1])
    val dist = Array(2) { IntArray(n) { -1 } }
    dist[0][0] = 0; dist[1][0] = 0
    val queue = ArrayDeque<Int>()
    queue.addLast(0); queue.addLast(1)          // 상태 = 정점 * 2 + 마지막 색
    while (queue.isNotEmpty()) {
        val state = queue.removeFirst()
        val u = state / 2; val last = state % 2; val next = 1 - last
        for (v in adj[next][u]) {
            Drill.compare(u, v)
            if (dist[next][v] < 0) { dist[next][v] = dist[last][u] + 1; Drill.write(v, dist[next][v]); queue.addLast(v * 2 + next) }
        }
    }
    return IntArray(n) { v ->
        val a = dist[0][v]; val b = dist[1][v]
        if (a < 0) b else if (b < 0) a else minOf(a, b)
    }
}
""",
    mutants=[
        ("visited-by-vertex-only", "WRONG_ALGORITHM",
         "방문 표시를 정점만으로 한다. 한 색으로 먼저 온 정점을 다른 색으로 다시 지나지 못한다.",
         """
fun alternatingColorPaths(n: Int, red: IntArray, blue: IntArray): IntArray {
    val adj = Array(2) { Array(n) { ArrayList<Int>() } }
    for (i in red.indices step 2) adj[0][red[i]].add(red[i + 1])
    for (i in blue.indices step 2) adj[1][blue[i]].add(blue[i + 1])
    val dist = IntArray(n) { -1 }
    dist[0] = 0
    val queue = ArrayDeque<Int>()
    queue.addLast(0); queue.addLast(1)
    while (queue.isNotEmpty()) {
        val state = queue.removeFirst()
        val u = state / 2; val next = 1 - state % 2
        for (v in adj[next][u]) if (dist[v] < 0) { dist[v] = dist[u] + 1; queue.addLast(v * 2 + next) }
    }
    return dist
}
"""),
        ("starts-with-red-only", "MISSING_EDGE_CASE",
         "첫 간선을 빨강으로만 시작한다. 파랑으로 시작해야 닿는 정점을 놓친다.",
         """
fun alternatingColorPaths(n: Int, red: IntArray, blue: IntArray): IntArray {
    val adj = Array(2) { Array(n) { ArrayList<Int>() } }
    for (i in red.indices step 2) adj[0][red[i]].add(red[i + 1])
    for (i in blue.indices step 2) adj[1][blue[i]].add(blue[i + 1])
    val dist = Array(2) { IntArray(n) { -1 } }
    dist[1][0] = 0
    val queue = ArrayDeque<Int>()
    queue.addLast(1)
    while (queue.isNotEmpty()) {
        val state = queue.removeFirst()
        val u = state / 2; val last = state % 2; val next = 1 - last
        for (v in adj[next][u]) if (dist[next][v] < 0) { dist[next][v] = dist[last][u] + 1; queue.addLast(v * 2 + next) }
    }
    return IntArray(n) { v -> val a = dist[0][v]; val b = dist[1][v]; if (a < 0) b else if (b < 0) a else minOf(a, b) }
}
"""),
        ("takes-first-state-not-min", "WRONG_BRANCH",
         "두 상태 중 빨강으로 도착한 거리를 우선 답한다. 파랑이 더 짧아도 그렇다.",
         """
fun alternatingColorPaths(n: Int, red: IntArray, blue: IntArray): IntArray {
    val adj = Array(2) { Array(n) { ArrayList<Int>() } }
    for (i in red.indices step 2) adj[0][red[i]].add(red[i + 1])
    for (i in blue.indices step 2) adj[1][blue[i]].add(blue[i + 1])
    val dist = Array(2) { IntArray(n) { -1 } }
    dist[0][0] = 0; dist[1][0] = 0
    val queue = ArrayDeque<Int>()
    queue.addLast(0); queue.addLast(1)
    while (queue.isNotEmpty()) {
        val state = queue.removeFirst()
        val u = state / 2; val last = state % 2; val next = 1 - last
        for (v in adj[next][u]) if (dist[next][v] < 0) { dist[next][v] = dist[last][u] + 1; queue.addLast(v * 2 + next) }
    }
    return IntArray(n) { v -> if (dist[0][v] >= 0) dist[0][v] else dist[1][v] }
}
"""),
        ("dfs-all-paths", "PERFORMANCE",
         "번갈아 가는 모든 경로를 DFS 로 열거하며 최솟값을 갱신한다. 지수다.",
         """
fun alternatingColorPaths(n: Int, red: IntArray, blue: IntArray): IntArray {
    val adj = Array(2) { Array(n) { ArrayList<Int>() } }
    for (i in red.indices step 2) adj[0][red[i]].add(red[i + 1])
    for (i in blue.indices step 2) adj[1][blue[i]].add(blue[i + 1])
    val best = IntArray(n) { -1 }
    best[0] = 0
    val onPath = BooleanArray(n)
    fun go(u: Int, last: Int, depth: Int) {
        for (v in adj[1 - last][u]) {
            Drill.compare(u, v)
            if (best[v] < 0 || depth + 1 < best[v]) best[v] = depth + 1
            if (!onPath[v]) { onPath[v] = true; go(v, 1 - last, depth + 1); onPath[v] = false }
        }
    }
    onPath[0] = true
    go(0, 0, 0); go(0, 1, 0)
    return best
}
"""),
    ],
))


# --- 159. DAG 의 가장 긴 경로 (위상 정렬 위 DP) --------------------------------------------------------

def _longest_path_dag(n, edges):
    from collections import deque
    adj = [[] for _ in range(n)]
    indegree = [0] * n
    for i in range(0, len(edges), 2):
        adj[edges[i]].append(edges[i + 1])
        indegree[edges[i + 1]] += 1
    dist = [0] * n
    queue = deque(v for v in range(n) if indegree[v] == 0)
    seen = 0
    best = 0
    while queue:
        u = queue.popleft()
        seen += 1
        best = max(best, dist[u])
        for v in adj[u]:
            dist[v] = max(dist[v], dist[u] + 1)
            indegree[v] -= 1
            if indegree[v] == 0:
                queue.append(v)
    return best if seen == n else -1


def _dag_edges_forward(n, m, salt):
    a = randoms(m, 0, n - 2, salt=salt)
    span = randoms(m, 1, 6, salt=salt + 1)
    return flat([a[i], min(n - 1, a[i] + span[i])] for i in range(m))


PROBLEMS.append(Problem(
    id="longest-path-in-dag",
    title="DAG 의 가장 긴 경로",
    summary="""
정점 `n` 개(`0..n-1`)의 방향 그래프가 간선 목록 `edges = [u1, v1, ...]` 로 주어진다. 경로의 길이는
간선 수다. 가장 긴 경로의 길이를 반환한다 — 정점 하나도 길이 `0` 의 경로다. 그래프에 **순환이
있으면** `-1` 을 반환한다. 중복 간선이 있을 수 있다.
""",
    notes="""
순환이 없으면 가장 긴 경로는 위상 순서로 DP 한 번이다: 정점을 위상 순서로 꺼내며 나가는 간선마다
`dist[v] = max(dist[v], dist[u] + 1)`. Kahn 의 큐로 꺼낸 정점 수가 `n` 보다 적으면 순환이다 — 두
질문의 답이 한 번의 훑기에서 같이 나온다. 정점 10만에 사슬이 길 수 있으니 **재귀 DFS 는 스택을
넘긴다** — 반복으로 쓴다.
""",
    drill_doc="""
Drill.compare(u, v)           // 간선으로 거리를 완화했다
Drill.write(v, distance)      // 정점의 거리를 정했다
""",
    constraints="""
- `1 <= n <= 100_000`, `0 <= edges.size / 2 <= 200_000`
""",
    signature=dict(name="longestPathInDag", parameters=[("n", "INT"), ("edges", "INT_ARRAY")], returns="INT"),
    groups=perf_groups(time_multiplier=0.5),
    reference=_longest_path_dag,
    limits={"timeMillis": 2000, "memoryMb": 256, "outputBytes": 65536},
    cases={
        "sample": [("01", [4, [0, 1, 1, 2, 2, 3, 0, 3]]), ("02", [3, [0, 1, 1, 2, 2, 0]])],
        "boundary": [
            ("01-single-node", [1, []]),
            ("02-no-edges", [5, []]),
            ("03-self-loop", [2, [0, 1, 1, 1]]),
            # 순환이 아닌 정점들이 있어도 어딘가에 순환이 있으면 -1.
            ("04-cycle-off-to-the-side", [5, [0, 1, 1, 2, 3, 4, 4, 3]]),
            ("05-duplicate-edges", [3, [0, 1, 0, 1, 1, 2]]),
            # 긴 경로가 짧은 우회로보다 나중에 발견된다.
            ("06-long-over-short", [5, [0, 4, 0, 1, 1, 2, 2, 3, 3, 4]]),
            # 재귀 깊이: 정점 5만 개의 사슬.
            ("07-deep-chain", [50000, flat([i, i + 1] for i in range(49999))]),
        ],
        "hidden": [
            ("01-random-small", [8, _dag_edges_forward(8, 10, salt=8971)]),
            ("02-random-medium", [300, _dag_edges_forward(300, 600, salt=8973)]),
            ("03-random-with-cycle", [300, _dag_edges_forward(300, 600, salt=8975) + [299, 0]]),
            ("04-random-sparse", [5000, _dag_edges_forward(5000, 4000, salt=8977)]),
        ],
        "performance": [
            ("01-small", [20000, _dag_edges_forward(20000, 40000, salt=8981)]),
            ("02-medium", [60000, _dag_edges_forward(60000, 120000, salt=8983)]),
            ("03-large-chain", [100000, flat([i, i + 1] for i in range(99999)) + _dag_edges_forward(100000, 100000, salt=8985)]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). Kahn 순서로 DP, 다 꺼내지 못하면 순환.
fun longestPathInDag(n: Int, edges: IntArray): Int {
    val adj = Array(n) { ArrayList<Int>() }
    val indegree = IntArray(n)
    for (i in edges.indices step 2) { adj[edges[i]].add(edges[i + 1]); indegree[edges[i + 1]] += 1 }
    val dist = IntArray(n)
    val queue = IntArray(n)
    var head = 0; var tail = 0
    for (v in 0 until n) if (indegree[v] == 0) queue[tail++] = v
    var best = 0
    while (head < tail) {
        val u = queue[head++]
        if (dist[u] > best) best = dist[u]
        for (v in adj[u]) {
            Drill.compare(u, v)
            if (dist[u] + 1 > dist[v]) { dist[v] = dist[u] + 1; Drill.write(v, dist[v]) }
            indegree[v] -= 1
            if (indegree[v] == 0) queue[tail++] = v
        }
    }
    return if (tail == n) best else -1
}
""",
    mutants=[
        ("no-cycle-check", "MISSING_EDGE_CASE",
         "다 꺼냈는지 보지 않는다. 순환이 있어도 답을 낸다.",
         """
fun longestPathInDag(n: Int, edges: IntArray): Int {
    val adj = Array(n) { ArrayList<Int>() }
    val indegree = IntArray(n)
    for (i in edges.indices step 2) { adj[edges[i]].add(edges[i + 1]); indegree[edges[i + 1]] += 1 }
    val dist = IntArray(n); val queue = IntArray(n); var head = 0; var tail = 0
    for (v in 0 until n) if (indegree[v] == 0) queue[tail++] = v
    var best = 0
    while (head < tail) { val u = queue[head++]; if (dist[u] > best) best = dist[u]; for (v in adj[u]) { if (dist[u] + 1 > dist[v]) dist[v] = dist[u] + 1; indegree[v] -= 1; if (indegree[v] == 0) queue[tail++] = v } }
    return best
}
"""),
        ("relaxes-first-visit-only", "WRONG_BRANCH",
         "정점의 거리를 처음 닿을 때만 정한다. 더 긴 경로가 나중에 와도 갱신하지 않는다.",
         """
fun longestPathInDag(n: Int, edges: IntArray): Int {
    val adj = Array(n) { ArrayList<Int>() }
    val indegree = IntArray(n)
    for (i in edges.indices step 2) { adj[edges[i]].add(edges[i + 1]); indegree[edges[i + 1]] += 1 }
    val dist = IntArray(n) { -1 }; val queue = IntArray(n); var head = 0; var tail = 0
    for (v in 0 until n) if (indegree[v] == 0) { queue[tail++] = v; dist[v] = 0 }
    var best = 0
    while (head < tail) { val u = queue[head++]; if (dist[u] > best) best = dist[u]; for (v in adj[u]) { if (dist[v] < 0) dist[v] = dist[u] + 1; indegree[v] -= 1; if (indegree[v] == 0) queue[tail++] = v } }
    return if (tail == n) best else -1
}
"""),
        ("recursive-dfs--stack-overflow", "WRONG_ALGORITHM",
         "재귀 DFS 로 기억하며 푼다. 답은 맞지만 5만 개 사슬에서 스택이 넘친다.",
         """
fun longestPathInDag(n: Int, edges: IntArray): Int {
    val adj = Array(n) { ArrayList<Int>() }
    for (i in edges.indices step 2) adj[edges[i]].add(edges[i + 1])
    val memo = IntArray(n) { -1 }
    val state = IntArray(n)  // 0 미방문, 1 방문 중, 2 끝
    var cyclic = false
    fun go(u: Int): Int {
        if (state[u] == 1) { cyclic = true; return 0 }
        if (state[u] == 2) return memo[u]
        state[u] = 1
        var best = 0
        for (v in adj[u]) { Drill.compare(u, v); best = maxOf(best, go(v) + 1) }
        state[u] = 2; memo[u] = best
        return best
    }
    var answer = 0
    for (v in 0 until n) answer = maxOf(answer, go(v))
    return if (cyclic) -1 else answer
}
"""),
        ("bfs-from-every-source--quadratic", "PERFORMANCE",
         "진입 차수 0 인 정점마다 따로 가장 긴 경로를 훑는다. 사슬 + 여분 간선에서 O(n·m).",
         """
fun longestPathInDag(n: Int, edges: IntArray): Int {
    val adj = Array(n) { ArrayList<Int>() }
    val indegree = IntArray(n)
    for (i in edges.indices step 2) { adj[edges[i]].add(edges[i + 1]); indegree[edges[i + 1]] += 1 }
    // 순환 검사는 Kahn 으로.
    val deg = indegree.copyOf(); val queue = IntArray(n); var head = 0; var tail = 0
    for (v in 0 until n) if (deg[v] == 0) queue[tail++] = v
    while (head < tail) { val u = queue[head++]; for (v in adj[u]) { deg[v] -= 1; if (deg[v] == 0) queue[tail++] = v } }
    if (tail != n) return -1
    var best = 0
    for (s in 0 until n) {
        if (indegree[s] != 0) continue
        val dist = IntArray(n) { -1 }; dist[s] = 0
        val q = ArrayDeque<Int>(); q.addLast(s)
        while (q.isNotEmpty()) { val u = q.removeFirst(); if (dist[u] > best) best = dist[u]; for (v in adj[u]) { Drill.compare(u, v); if (dist[u] + 1 > dist[v]) { dist[v] = dist[u] + 1; q.addLast(v) } } }
    }
    return best
}
"""),
    ],
))


# --- 177. 계정 합치기 (이메일로 잇는 유니온파인드) ---------------------------------------------------------

def _merged_accounts(accounts):
    owner = {}
    parent = list(range(len(accounts)))

    def find(x):
        while parent[x] != x:
            parent[x] = parent[parent[x]]
            x = parent[x]
        return x

    for i, entry in enumerate(accounts):
        _, _, emails = entry.partition(":")
        for email in emails.split(","):
            if email in owner:
                a, b = find(owner[email]), find(i)
                if a != b:
                    parent[a] = b
            else:
                owner[email] = i
    return len({find(i) for i in range(len(accounts))})


def _account_entries(people, per_person, shared, salt):
    """사람마다 이메일 몇 개씩 계정을 만들고, 일부 계정이 다른 계정의 이메일을 함께 갖게 한다."""
    entries = []
    picks = randoms(people * per_person * 3, 0, 10 ** 6, salt=salt)
    k = 0
    for p in range(people):
        for _ in range(per_person):
            emails = [f"u{p}_{picks[k] % 5}@x.io", f"u{p}_{picks[k + 1] % 5}@x.io"]
            k += 2
            entries.append(f"p{p}:" + ",".join(dict.fromkeys(emails)))
    links = randoms(shared * 2, 0, people - 1, salt=salt + 1)
    for s in range(shared):
        a, b = links[2 * s], links[2 * s + 1]
        entries.append(f"p{a}:u{a}_0@x.io,u{b}_0@x.io")
    return shuffled(entries, salt=salt + 2)


PROBLEMS.append(Problem(
    id="merged-accounts-count",
    title="계정 합치기",
    summary="""
계정 목록 `accounts` 가 주어진다. 항목 하나는 `"이름:이메일1,이메일2,..."` 다. **같은 이메일을 가진
두 계정은 같은 사람**의 것이고, 같은 사람의 계정을 모두 합친다(이름이 같아도 이메일이 안 겹치면 다른
사람이다). 합친 뒤의 사람 수를 반환한다.
""",
    notes="""
계정을 정점, 같은 이메일을 간선으로 보면 사람은 연결 요소다. 이메일마다 "처음 본 계정"을 기억해 두고,
다시 보이면 그 계정과 지금 계정을 유니온파인드로 합친다. 이름은 아무 역할이 없다 — 이름으로 합치면
동명이인이 한 사람이 되고, 이름으로 나누면 한 사람이 둘이 된다.
""",
    drill_doc="""
Drill.compare(a, b)           // 두 계정을 합쳤다
Drill.write(0, count)         // 사람 수를 셌다
""",
    constraints="""
- `1 <= accounts.length <= 50_000`, 항목마다 이메일 `1..10` 개, 전체 이메일 `200_000` 개 이하
""",
    signature=dict(name="mergedAccountsCount", parameters=[("accounts", "STRING_ARRAY")], returns="INT"),
    groups=perf_groups(time_multiplier=0.5),
    reference=_merged_accounts,
    cases={
        "sample": [("01", [["john:j@a.io,jj@a.io", "john:jj@a.io,j3@a.io", "mary:m@a.io"]]), ("02", [["a:x@y", "a:z@y"]])],
        "boundary": [
            ("01-single", [["ann:a@b"]]),
            # 같은 이름, 다른 이메일 — 다른 사람.
            ("02-same-name-different-people", [["kim:k1@a", "kim:k2@a"]]),
            # 다른 이름, 같은 이메일 — 같은 사람 (이름은 오타일 수 있다).
            ("03-different-name-same-email", [["kim:k@a", "kin:k@a"]]),
            # 사슬로 이어진다: a-b, b-c.
            ("04-chain", [["p:a@x,b@x", "p:c@x,d@x", "p:b@x,c@x"]]),
            ("05-all-one-person", [["p:a@x", "p:a@x,b@x", "p:b@x,c@x", "p:c@x"]]),
            ("06-duplicate-email-in-one-entry", [["p:a@x,a@x", "q:b@x"]]),
        ],
        "hidden": [
            ("01-random-small", [_account_entries(4, 2, 1, salt=9461)]),
            ("02-random-medium", [_account_entries(50, 3, 10, salt=9464)]),
            ("03-random-many-links", [_account_entries(200, 2, 150, salt=9467)]),
            ("04-no-links", [_account_entries(100, 1, 0, salt=9470)]),
        ],
        "performance": [
            ("01-small", [_account_entries(2000, 2, 500, salt=9481)]),
            ("02-medium", [_account_entries(8000, 2, 2000, salt=9484)]),
            ("03-large", [_account_entries(20000, 2, 8000, salt=9487)]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 이메일 → 처음 본 계정, 다시 보이면 union.
fun mergedAccountsCount(accounts: Array<String>): Int {
    val n = accounts.size
    val parent = IntArray(n) { it }
    fun find(x: Int): Int { var r = x; while (parent[r] != r) { parent[r] = parent[parent[r]]; r = parent[r] }; return r }
    val owner = HashMap<String, Int>()
    for (i in 0 until n) {
        val emails = accounts[i].substringAfter(':').split(',')
        for (email in emails) {
            val seen = owner[email]
            if (seen == null) owner[email] = i
            else { val a = find(seen); val b = find(i); Drill.compare(a, b); if (a != b) parent[a] = b }
        }
    }
    var count = 0
    for (i in 0 until n) if (find(i) == i) count += 1
    Drill.write(0, count)
    return count
}
""",
    mutants=[
        ("merges-by-name", "WRONG_ALGORITHM",
         "이름이 같으면 합친다. 동명이인이 한 사람이 된다.",
         """
fun mergedAccountsCount(accounts: Array<String>): Int {
    val n = accounts.size
    val parent = IntArray(n) { it }
    fun find(x: Int): Int { var r = x; while (parent[r] != r) { parent[r] = parent[parent[r]]; r = parent[r] }; return r }
    val owner = HashMap<String, Int>()
    for (i in 0 until n) {
        val name = accounts[i].substringBefore(':')
        val seen = owner[name]
        if (seen == null) owner[name] = i else { val a = find(seen); val b = find(i); if (a != b) parent[a] = b }
        for (email in accounts[i].substringAfter(':').split(',')) { val s = owner[email]; if (s == null) owner[email] = i else { val a = find(s); val b = find(i); if (a != b) parent[a] = b } }
    }
    var count = 0
    for (i in 0 until n) if (find(i) == i) count += 1
    return count
}
"""),
        ("requires-same-name-to-merge", "WRONG_BRANCH",
         "이메일이 같아도 이름이 다르면 합치지 않는다.",
         """
fun mergedAccountsCount(accounts: Array<String>): Int {
    val n = accounts.size
    val parent = IntArray(n) { it }
    fun find(x: Int): Int { var r = x; while (parent[r] != r) { parent[r] = parent[parent[r]]; r = parent[r] }; return r }
    val owner = HashMap<String, Int>()
    for (i in 0 until n) {
        val name = accounts[i].substringBefore(':')
        for (email in accounts[i].substringAfter(':').split(',')) {
            val seen = owner[email]
            if (seen == null) owner[email] = i
            else if (accounts[seen].substringBefore(':') == name) { val a = find(seen); val b = find(i); if (a != b) parent[a] = b }
        }
    }
    var count = 0
    for (i in 0 until n) if (find(i) == i) count += 1
    return count
}
"""),
        ("owner-overwritten", "MISSING_EDGE_CASE",
         "이메일의 주인을 볼 때마다 덮어쓰고 합치지는 않는다. 사슬이 끊긴다.",
         """
fun mergedAccountsCount(accounts: Array<String>): Int {
    val n = accounts.size
    val parent = IntArray(n) { it }
    fun find(x: Int): Int { var r = x; while (parent[r] != r) { parent[r] = parent[parent[r]]; r = parent[r] }; return r }
    val owner = HashMap<String, Int>()
    for (i in 0 until n) {
        var merged = false
        for (email in accounts[i].substringAfter(':').split(',')) {
            val seen = owner[email]
            if (seen != null && !merged) { val a = find(seen); val b = find(i); if (a != b) parent[a] = b; merged = true }
            owner[email] = i
        }
    }
    var count = 0
    for (i in 0 until n) if (find(i) == i) count += 1
    return count
}
"""),
        ("pairwise-comparison", "PERFORMANCE",
         "계정 쌍마다 이메일이 겹치는지 본다. O(n² × 이메일).",
         """
fun mergedAccountsCount(accounts: Array<String>): Int {
    val n = accounts.size
    val emails = accounts.map { it.substringAfter(':').split(',').toHashSet() }
    val parent = IntArray(n) { it }
    fun find(x: Int): Int { var r = x; while (parent[r] != r) { parent[r] = parent[parent[r]]; r = parent[r] }; return r }
    for (i in 0 until n) for (j in i + 1 until n) {
        Drill.compare(i, j)
        if (emails[i].any { it in emails[j] }) { val a = find(i); val b = find(j); if (a != b) parent[a] = b }
    }
    var count = 0
    for (i in 0 until n) if (find(i) == i) count += 1
    return count
}
"""),
    ],
))


# --- 178. 선수 관계 질의 (위상 순서의 도달 집합) ---------------------------------------------------------

def _prerequisite_queries(n, edges, queries):
    from collections import deque
    adj = [[] for _ in range(n)]
    indegree = [0] * n
    for i in range(0, len(edges), 2):
        adj[edges[i]].append(edges[i + 1])
        indegree[edges[i + 1]] += 1
    order = []
    queue = deque(v for v in range(n) if indegree[v] == 0)
    while queue:
        u = queue.popleft()
        order.append(u)
        for v in adj[u]:
            indegree[v] -= 1
            if indegree[v] == 0:
                queue.append(v)
    reach = [0] * n  # 비트 집합: reach[v] 의 비트 u 가 켜져 있으면 u 에서 v 로 갈 수 있다.
    # 위상 순서로 앞에서부터: v 의 도달원 = 모든 직전 정점 u 의 도달원 ∪ {u}
    incoming = [[] for _ in range(n)]
    for i in range(0, len(edges), 2):
        incoming[edges[i + 1]].append(edges[i])
    for v in order:
        bits = 0
        for u in incoming[v]:
            bits |= reach[u] | (1 << u)
        reach[v] = bits
    out = []
    for i in range(0, len(queries), 2):
        u, v = queries[i], queries[i + 1]
        out.append(1 if (reach[v] >> u) & 1 else 0)
    return out


def _dag_forward(n, m, salt):
    a = randoms(m, 0, n - 2, salt=salt)
    span = randoms(m, 1, 8, salt=salt + 1)
    return flat([a[i], min(n - 1, a[i] + span[i])] for i in range(m))


def _pairs(n, q, salt):
    a = randoms(q, 0, n - 1, salt=salt)
    b = randoms(q, 0, n - 1, salt=salt + 1)
    return flat([a[i], b[i]] for i in range(q))


def _backward_pairs(n, q, salt):
    """앞쪽 정점 `u` 에서 그보다 앞의 `v` 를 묻는다 — 간선이 앞으로만 가는 그래프에서는 전부 0 이다. 질의마다
    탐색하는 풀이는 못 찾고 `u` 뒤의 그래프를 끝까지 뒤진다; 무작위 질의는 절반이 금방 찾아 끝나 오답이 빨랐고,
    `u` 가 무작위면 평균 절반만 뒤져 1.3배로 겨우 졌다."""
    a = randoms(q, 1, 100, salt=salt)
    b = randoms(q, 0, 100, salt=salt + 1)
    return flat([a[i], b[i] % a[i]] for i in range(q))


PROBLEMS.append(Problem(
    id="prerequisite-queries",
    title="선수 관계 질의",
    summary="""
과목 `n` 개(`0..n-1`)와 선수 관계 `edges = [a1, b1, ...]` ("`a` 를 들어야 `b` 를 들을 수 있다", 순환
없음)가 주어진다. 질의 `queries = [u1, v1, ...]` 마다 **`u` 가 `v` 의 (직접이든 간접이든) 선수 과목인가**를
`1`/`0` 으로 담은 배열을 반환한다. `u == v` 는 `0` 이다.
""",
    notes="""
질의마다 DFS 하면 질의 수 × 그래프 크기다. 질의가 많으니 **도달 관계를 미리 다 만든다** — 정점 `v` 에
"어떤 정점에서 올 수 있는가"를 비트 집합으로 두면, 위상 순서로 `v` 의 집합 = 직전 정점들의 집합 ∪ 직전
정점들이다. 비트 집합은 `n/64` 워드라 전체가 `n²/64` 이고 질의는 O(1) 이다.
""",
    drill_doc="""
Drill.compare(u, v)           // 도달 집합을 합쳤다
Drill.write(q, answer)        // 질의에 답했다
""",
    constraints="""
- `1 <= n <= 2_000`, 간선 `0..10_000` 개(순환 없음), 질의 `0..100_000` 개
""",
    signature=dict(name="prerequisiteQueries", parameters=[("n", "INT"), ("edges", "INT_ARRAY"), ("queries", "INT_ARRAY")], returns="INT_ARRAY"),
    groups=perf_groups(time_multiplier=0.25),
    reference=_prerequisite_queries,
    limits={"timeMillis": 2000, "memoryMb": 256, "outputBytes": 2000000},
    cases={
        "sample": [("01", [3, [0, 1, 1, 2], [0, 2, 2, 0, 0, 1]]), ("02", [2, [], [0, 1, 1, 0]])],
        "boundary": [
            ("01-self", [1, [], [0, 0]]),
            # 간접 선수: 사슬의 끝.
            ("02-long-chain", [5, [0, 1, 1, 2, 2, 3, 3, 4], [0, 4, 4, 0, 1, 3]]),
            # 여러 길로 닿는다 — 한 번만 답한다.
            ("03-diamond", [4, [0, 1, 0, 2, 1, 3, 2, 3], [0, 3, 1, 2]]),
            ("04-no-queries", [3, [0, 1], []]),
            # 방향이 중요하다.
            ("05-direction", [3, [0, 1, 1, 2], [2, 0, 1, 0, 2, 1]]),
            ("06-duplicate-edges", [2, [0, 1, 0, 1], [0, 1]]),
        ],
        "hidden": [
            ("01-random-small", [8, _dag_forward(8, 10, salt=9501), _pairs(8, 20, salt=9503)]),
            ("02-random-medium", [200, _dag_forward(200, 500, salt=9505), _pairs(200, 500, salt=9507)]),
            ("03-random-sparse", [1000, _dag_forward(1000, 1000, salt=9509), _pairs(1000, 2000, salt=9511)]),
            ("04-random-dense", [500, _dag_forward(500, 5000, salt=9513), _pairs(500, 5000, salt=9515)]),
        ],
        "performance": [
            ("01-small", [1000, _dag_forward(1000, 5000, salt=9521), _pairs(1000, 30000, salt=9523)]),
            ("02-medium", [2000, _dag_forward(2000, 10000, salt=9525), _backward_pairs(2000, 60000, salt=9527)]),
            ("03-large", [2000, _dag_forward(2000, 10000, salt=9529), _backward_pairs(2000, 100000, salt=9531)]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 위상 순서로 도달원 비트 집합을 합친다.
fun prerequisiteQueries(n: Int, edges: IntArray, queries: IntArray): IntArray {
    val adj = Array(n) { ArrayList<Int>() }
    val incoming = Array(n) { ArrayList<Int>() }
    val indegree = IntArray(n)
    for (i in edges.indices step 2) { adj[edges[i]].add(edges[i + 1]); incoming[edges[i + 1]].add(edges[i]); indegree[edges[i + 1]] += 1 }
    val order = IntArray(n); var head = 0; var tail = 0
    for (v in 0 until n) if (indegree[v] == 0) order[tail++] = v
    while (head < tail) { val u = order[head++]; for (v in adj[u]) { indegree[v] -= 1; if (indegree[v] == 0) order[tail++] = v } }
    val words = (n + 63) / 64
    val reach = Array(n) { LongArray(words) }
    for (idx in 0 until n) {
        val v = order[idx]
        for (u in incoming[v]) {
            Drill.compare(u, v)
            val ru = reach[u]; val rv = reach[v]
            for (w in 0 until words) rv[w] = rv[w] or ru[w]
            rv[u / 64] = rv[u / 64] or (1L shl (u % 64))
        }
    }
    val out = IntArray(queries.size / 2)
    for (q in out.indices) {
        val u = queries[2 * q]; val v = queries[2 * q + 1]
        out[q] = if ((reach[v][u / 64] ushr (u % 64)) and 1L == 1L) 1 else 0
        Drill.write(q, out[q])
    }
    return out
}
""",
    mutants=[
        ("direct-edges-only", "WRONG_ALGORITHM",
         "직접 선수 관계만 본다. 간접 선수를 놓친다.",
         """
fun prerequisiteQueries(n: Int, edges: IntArray, queries: IntArray): IntArray {
    val direct = HashSet<Long>()
    for (i in edges.indices step 2) direct.add(edges[i].toLong() * n + edges[i + 1])
    return IntArray(queries.size / 2) { q -> if (queries[2 * q].toLong() * n + queries[2 * q + 1] in direct) 1 else 0 }
}
"""),
        ("reversed-direction", "WRONG_BRANCH",
         "방향을 거꾸로 답한다 — v 가 u 의 선수인가.",
         """
fun prerequisiteQueries(n: Int, edges: IntArray, queries: IntArray): IntArray {
    val adj = Array(n) { ArrayList<Int>() }
    val incoming = Array(n) { ArrayList<Int>() }
    val indegree = IntArray(n)
    for (i in edges.indices step 2) { adj[edges[i]].add(edges[i + 1]); incoming[edges[i + 1]].add(edges[i]); indegree[edges[i + 1]] += 1 }
    val order = IntArray(n); var head = 0; var tail = 0
    for (v in 0 until n) if (indegree[v] == 0) order[tail++] = v
    while (head < tail) { val u = order[head++]; for (v in adj[u]) { indegree[v] -= 1; if (indegree[v] == 0) order[tail++] = v } }
    val words = (n + 63) / 64
    val reach = Array(n) { LongArray(words) }
    for (idx in 0 until n) { val v = order[idx]; for (u in incoming[v]) { val ru = reach[u]; val rv = reach[v]; for (w in 0 until words) rv[w] = rv[w] or ru[w]; rv[u / 64] = rv[u / 64] or (1L shl (u % 64)) } }
    return IntArray(queries.size / 2) { q -> val u = queries[2 * q]; val v = queries[2 * q + 1]; if ((reach[u][v / 64] ushr (v % 64)) and 1L == 1L) 1 else 0 }
}
"""),
        ("input-order-not-topological", "MISSING_EDGE_CASE",
         "정점을 번호 순으로 처리한다. 간선이 번호 역순이면 도달 집합이 덜 찬다.",
         """
fun prerequisiteQueries(n: Int, edges: IntArray, queries: IntArray): IntArray {
    val incoming = Array(n) { ArrayList<Int>() }
    for (i in edges.indices step 2) incoming[edges[i + 1]].add(edges[i])
    val words = (n + 63) / 64
    val reach = Array(n) { LongArray(words) }
    for (v in n - 1 downTo 0) { for (u in incoming[v]) { val ru = reach[u]; val rv = reach[v]; for (w in 0 until words) rv[w] = rv[w] or ru[w]; rv[u / 64] = rv[u / 64] or (1L shl (u % 64)) } }
    return IntArray(queries.size / 2) { q -> val u = queries[2 * q]; val v = queries[2 * q + 1]; if ((reach[v][u / 64] ushr (u % 64)) and 1L == 1L) 1 else 0 }
}
"""),
        ("dfs-per-query", "PERFORMANCE",
         "질의마다 DFS 한다. O(질의 × (n + m)).",
         """
fun prerequisiteQueries(n: Int, edges: IntArray, queries: IntArray): IntArray {
    val adj = Array(n) { ArrayList<Int>() }
    for (i in edges.indices step 2) adj[edges[i]].add(edges[i + 1])
    val seen = IntArray(n) { -1 }
    val stack = IntArray(n + edges.size / 2 + 1)
    return IntArray(queries.size / 2) { q ->
        val u = queries[2 * q]; val v = queries[2 * q + 1]
        var found = false
        var top = 0; stack[top++] = u; seen[u] = q
        while (top > 0 && !found) {
            val x = stack[--top]
            for (y in adj[x]) { Drill.compare(x, y); if (y == v) { found = true; break }; if (seen[y] != q) { seen[y] = q; stack[top++] = y } }
        }
        if (found) 1 else 0
    }
}
"""),
    ],
))


# --- 179. 음수 간선이 있는 최단 거리 (벨만-포드) -----------------------------------------------------------

def _shortest_with_negatives(n, edges):
    from collections import deque
    inf = 1_000_000_000
    dist = [inf] * n
    dist[0] = 0
    for _ in range(n - 1):
        changed = False
        for i in range(0, len(edges), 3):
            a, b, w = edges[i], edges[i + 1], edges[i + 2]
            if dist[a] != inf and dist[a] + w < dist[b]:
                dist[b] = dist[a] + w
                changed = True
        if not changed:
            break
    # n-1 번 뒤에도 줄어드는 간선의 끝은 음수 순환의 영향 안이다. 거기서 갈 수 있는 곳도 전부.
    tainted = [False] * n
    for i in range(0, len(edges), 3):
        a, b, w = edges[i], edges[i + 1], edges[i + 2]
        if dist[a] != inf and dist[a] + w < dist[b]:
            tainted[b] = True
    adj = [[] for _ in range(n)]
    for i in range(0, len(edges), 3):
        adj[edges[i]].append(edges[i + 1])
    queue = deque(v for v in range(n) if tainted[v])
    while queue:
        u = queue.popleft()
        for v in adj[u]:
            if not tainted[v]:
                tainted[v] = True
                queue.append(v)
    return [-1_000_000_000 if tainted[v] else dist[v] for v in range(n)]


def _reversed_chain(n, salt, low=-100, high=100):
    """0→1→…→n-1 사슬을 끝에서부터 적는다. 완화 순서가 거꾸로라 라운드를 n-1 번 다 돌아야 한다."""
    w = randoms(n - 1, low, high, salt=salt)
    return flat([i, i + 1, w[i]] for i in range(n - 2, -1, -1))


def _forward_edges(n, m, salt, low=-100, high=100):
    """앞으로만 가는 간선 — 순환이 없으니 음수 간선을 마음껏 섞어도 답이 유한하다."""
    a = randoms(m, 0, n - 2, salt=salt)
    span = randoms(m, 1, 30, salt=salt + 1)
    w = randoms(m, low, high, salt=salt + 2)
    return flat([a[i], min(n - 1, a[i] + span[i]), w[i]] for i in range(m))


PROBLEMS.append(Problem(
    id="negative-cycle-distance",
    title="음수 간선이 있는 최단 거리",
    summary="""
정점 `n` 개(`0..n-1`)와 방향 간선 `edges = [a1, b1, w1, ...]` (`a` 에서 `b` 로, 가중치 `w` 는 음수일 수 있다)가
주어진다. 정점 `0` 에서 각 정점까지의 **최단 거리** 배열을 반환한다.

- 닿을 수 없는 정점은 `1000000000`
- 아무리 줄여도 하한이 없는 정점(음수 순환을 지나 올 수 있는 정점)은 `-1000000000`
""",
    notes="""
음수 간선이 있으면 "가장 가까운 것부터 확정한다"는 가정이 깨진다 — 나중에 더 싼 길이 나타날 수 있다. 대신
**모든 간선을 n-1 번 훑어 완화**한다. 단순 경로는 간선을 n-1 개 넘게 쓸 수 없으니, 그래도 줄어드는 간선이
남아 있다면 그 끝은 음수 순환의 영향 안이다. 영향은 **거기서 갈 수 있는 곳으로 퍼진다.**
""",
    drill_doc="""
Drill.write(v, dist)          // v 의 거리를 줄였다
Drill.visit(v, 0)             // 음수 순환의 영향을 v 로 퍼뜨렸다
""",
    constraints="""
- `1 <= n <= 3_000`, 간선 `0..10_000` 개, `-1000 <= w <= 1000`
- 자기 자신으로 가는 간선과 같은 쌍의 간선이 여럿 있을 수 있다
""",
    signature=dict(name="shortestWithNegatives", parameters=[("n", "INT"), ("edges", "INT_ARRAY")], returns="INT_ARRAY"),
    # 모든 쌍을 구하는 오답은 n^3 인데, 안쪽 반복이 단순해 JIT 가 벡터화한다 — 2000 정점에서 한도의 1.5배도
    # 못 됐다. 정점을 3000 으로 올리고 성능 그룹의 시계를 조여 자릿수로 지게 했다.
    groups=perf_groups(time_multiplier=0.5),
    reference=_shortest_with_negatives,
    limits={"timeMillis": 2000, "memoryMb": 256, "outputBytes": 65536},
    cases={
        "sample": [
            ("01", [3, [0, 1, 5, 1, 2, -2]]),
            ("02", [3, [0, 1, 1, 1, 0, -3]]),
        ],
        "boundary": [
            ("01-single", [1, []]),
            # 닿을 수 없는 정점은 그대로 둔다 — 무한대에서 완화하면 안 된다.
            ("02-unreachable-with-negative-edge", [4, [0, 1, 5, 2, 3, -4]]),
            # 나중에 나타나는 더 싼 길. 확정하고 넘어가면 놓친다.
            ("03-late-improvement", [3, [0, 1, 2, 0, 2, 3, 2, 1, -2]]),
            ("04-negative-cycle-unreachable", [4, [0, 1, 1, 2, 3, -5, 3, 2, -5]]),
            ("05-cycle-taints-downstream", [4, [0, 1, 1, 1, 2, -3, 2, 1, 1, 2, 3, 7]]),
            ("06-negative-self-loop", [2, [0, 0, -1, 0, 1, 3]]),
            # 합이 0 인 순환은 음수 순환이 아니다.
            ("07-zero-cycle", [2, [0, 1, 1, 1, 0, -1]]),
        ],
        "hidden": [
            ("01-random-small", [8, _forward_edges(8, 15, salt=9601)]),
            ("02-random-medium", [200, _forward_edges(200, 600, salt=9603)]),
            ("03-random-sparse", [1000, _forward_edges(1000, 1200, salt=9605)]),
            # 뒤로 가는 큰 음수 간선 하나가 순환을 만든다. 영향이 어디까지 퍼지나.
            ("04-with-negative-cycle", [300, _forward_edges(300, 900, salt=9607) + [150, 100, -900, 100, 150, 1]]),
            ("05-all-negative", [400, _forward_edges(400, 1200, salt=9609, low=-1000, high=-1)]),
        ],
        "performance": [
            ("01-chain", [1500, _reversed_chain(1500, salt=9611) + _forward_edges(1500, 2000, salt=9613)]),
            ("02-long-chain", [3000, _reversed_chain(3000, salt=9615) + _forward_edges(3000, 1000, salt=9617)]),
            ("03-dense", [3000, _forward_edges(3000, 10000, salt=9619)]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 벨만-포드 n-1 라운드 + 음수 순환의 영향 전파.
// 거리는 Long 으로 센다 — 음수 순환 위에서는 Int 를 넘길 수 있고, 그 값들은 어차피 하한 없음으로 덮인다.
fun shortestWithNegatives(n: Int, edges: IntArray): IntArray {
    val inf = 1_000_000_000L
    val dist = LongArray(n) { inf }
    dist[0] = 0L
    for (round in 0 until n - 1) {
        var changed = false
        for (i in edges.indices step 3) {
            val a = edges[i]; val b = edges[i + 1]; val w = edges[i + 2]
            if (dist[a] != inf && dist[a] + w < dist[b]) {
                dist[b] = dist[a] + w
                Drill.write(b, dist[b].toInt())
                changed = true
            }
        }
        if (!changed) break
    }
    val tainted = BooleanArray(n)
    for (i in edges.indices step 3) {
        val a = edges[i]; val b = edges[i + 1]; val w = edges[i + 2]
        if (dist[a] != inf && dist[a] + w < dist[b]) tainted[b] = true
    }
    val adj = Array(n) { ArrayList<Int>() }
    for (i in edges.indices step 3) adj[edges[i]].add(edges[i + 1])
    val queue = IntArray(n); var head = 0; var tail = 0
    for (v in 0 until n) if (tainted[v]) queue[tail++] = v
    while (head < tail) {
        val u = queue[head++]
        for (v in adj[u]) if (!tainted[v]) { tainted[v] = true; Drill.visit(v, 0); queue[tail++] = v }
    }
    return IntArray(n) { v -> if (tainted[v]) -1_000_000_000 else dist[v].toInt() }
}
""",
    mutants=[
        ("dijkstra-with-negatives", "WRONG_ALGORITHM",
         "가장 가까운 정점부터 확정한다. 음수 간선이 나중에 더 싼 길을 만들면 놓친다.",
         """
fun shortestWithNegatives(n: Int, edges: IntArray): IntArray {
    val inf = 1_000_000_000
    val adj = Array(n) { ArrayList<IntArray>() }
    for (i in edges.indices step 3) adj[edges[i]].add(intArrayOf(edges[i + 1], edges[i + 2]))
    val dist = IntArray(n) { inf }
    dist[0] = 0
    val done = BooleanArray(n)
    repeat(n) {
        var best = -1
        for (v in 0 until n) if (!done[v] && dist[v] != inf && (best == -1 || dist[v] < dist[best])) best = v
        if (best == -1) return@repeat
        done[best] = true
        for (e in adj[best]) if (dist[best] + e[1] < dist[e[0]]) dist[e[0]] = dist[best] + e[1]
    }
    return dist
}
"""),
        ("relaxes-from-unreachable", "MISSING_EDGE_CASE",
         "닿을 수 없는 정점에서도 완화한다. 무한대에 가중치를 더해 닿지 않는 곳에 거리가 생긴다.",
         """
fun shortestWithNegatives(n: Int, edges: IntArray): IntArray {
    val inf = 1_000_000_000L
    val dist = LongArray(n) { inf }
    dist[0] = 0L
    for (round in 0 until n - 1) {
        var changed = false
        for (i in edges.indices step 3) {
            val a = edges[i]; val b = edges[i + 1]; val w = edges[i + 2]
            if (dist[a] + w < dist[b]) { dist[b] = dist[a] + w; changed = true }
        }
        if (!changed) break
    }
    val tainted = BooleanArray(n)
    for (i in edges.indices step 3) {
        val a = edges[i]; val b = edges[i + 1]; val w = edges[i + 2]
        if (dist[a] + w < dist[b]) tainted[b] = true
    }
    val adj = Array(n) { ArrayList<Int>() }
    for (i in edges.indices step 3) adj[edges[i]].add(edges[i + 1])
    val queue = IntArray(n); var head = 0; var tail = 0
    for (v in 0 until n) if (tainted[v]) queue[tail++] = v
    while (head < tail) { val u = queue[head++]; for (v in adj[u]) if (!tainted[v]) { tainted[v] = true; queue[tail++] = v } }
    return IntArray(n) { v -> if (tainted[v]) -1_000_000_000 else dist[v].toInt() }
}
"""),
        ("no-taint-propagation", "WRONG_BRANCH",
         "음수 순환에 직접 걸린 정점만 하한 없음으로 적는다. 그 뒤로 이어지는 정점은 놓친다.",
         """
fun shortestWithNegatives(n: Int, edges: IntArray): IntArray {
    val inf = 1_000_000_000L
    val dist = LongArray(n) { inf }
    dist[0] = 0L
    for (round in 0 until n - 1) {
        var changed = false
        for (i in edges.indices step 3) {
            val a = edges[i]; val b = edges[i + 1]; val w = edges[i + 2]
            if (dist[a] != inf && dist[a] + w < dist[b]) { dist[b] = dist[a] + w; changed = true }
        }
        if (!changed) break
    }
    val tainted = BooleanArray(n)
    for (i in edges.indices step 3) {
        val a = edges[i]; val b = edges[i + 1]; val w = edges[i + 2]
        if (dist[a] != inf && dist[a] + w < dist[b]) tainted[b] = true
    }
    return IntArray(n) { v -> if (tainted[v]) -1_000_000_000 else dist[v].toInt() }
}
"""),
        ("all-pairs-floyd", "PERFORMANCE",
         "모든 쌍의 최단 거리를 구하고 0 번 행만 쓴다. O(n^3) 이라 정점이 늘면 무너진다.",
         """
fun shortestWithNegatives(n: Int, edges: IntArray): IntArray {
    val inf = 1_000_000_000L
    val d = Array(n) { r -> LongArray(n) { c -> if (r == c) 0L else inf } }
    for (i in edges.indices step 3) {
        val a = edges[i]; val b = edges[i + 1]; val w = edges[i + 2]
        if (w < d[a][b]) d[a][b] = w.toLong()
    }
    for (k in 0 until n) for (r in 0 until n) {
        val drk = d[r][k]
        if (drk == inf) continue
        val dk = d[k]
        val dr = d[r]
        for (c in 0 until n) { val v = drk + dk[c]; if (dk[c] != inf && v < dr[c]) dr[c] = v }
    }
    val out = IntArray(n)
    for (v in 0 until n) {
        var lower = false
        for (k in 0 until n) if (d[0][k] != inf && d[k][k] < 0 && d[k][v] != inf) lower = true
        out[v] = if (lower) -1_000_000_000 else if (d[0][v] >= inf) 1_000_000_000 else d[0][v].toInt()
    }
    return out
}
"""),
    ],
))


# --- 180. 위상 순서가 유일한가 -----------------------------------------------------------------------

def _unique_course_order(n, edges):
    from collections import deque
    adj = [[] for _ in range(n)]
    indegree = [0] * n
    for i in range(0, len(edges), 2):
        adj[edges[i]].append(edges[i + 1])
        indegree[edges[i + 1]] += 1
    queue = deque(v for v in range(n) if indegree[v] == 0)
    seen = 0
    while queue:
        if len(queue) > 1:
            return 0
        u = queue.popleft()
        seen += 1
        for v in adj[u]:
            indegree[v] -= 1
            if indegree[v] == 0:
                queue.append(v)
    return 1 if seen == n else 0


def _chain_edges(n, salt):
    """사슬 0→1→…→n-1 을 섞어서 적는다. 순서는 유일하다."""
    return flat(shuffled([[i, i + 1] for i in range(n - 1)], salt=salt))


PROBLEMS.append(Problem(
    id="unique-course-order",
    title="수강 순서가 하나뿐인가",
    summary="""
과목 `n` 개(`0..n-1`)와 선수 관계 `edges = [a1, b1, ...]` ("`a` 를 들어야 `b` 를 들을 수 있다")가 주어진다.
**모든 과목을 한 학기에 하나씩 듣는 순서가 정확히 하나뿐인가**를 `1`/`0` 으로 반환한다.
순서가 아예 없으면(선수 관계에 순환이 있으면) `0` 이다.
""",
    notes="""
순서를 다 세어 볼 필요는 없다. 위상 정렬을 한 걸음씩 진행하며 **지금 들을 수 있는 과목이 몇 개인가**만 보면
된다. 두 개 이상이면 그 자리에서 갈라지니 순서가 여럿이고, 끝까지 늘 하나였다면 순서는 하나다. 마지막에
처리한 과목 수가 `n` 이 아니면 순환이다.
""",
    drill_doc="""
Drill.enqueue(v)              // 들을 수 있게 된 과목
Drill.dequeue(v)              // 이번 학기에 들은 과목
""",
    constraints="""
- `1 <= n <= 200_000`, 간선 `0..400_000` 개
- 같은 쌍의 간선이 여럿 있을 수 있고, 자기 자신으로 가는 간선은 없다
""",
    signature=dict(name="uniqueCourseOrder", parameters=[("n", "INT"), ("edges", "INT_ARRAY")], returns="INT"),
    # 빠른 CI 러너에서 느린 오답이 한도의 2.7배에 그쳤다 — 러너의 CPU 는 실행마다 다르다. 그 러너에서도 4.5배가
    # 되도록 성능 그룹의 시계를 조인다(v2).
    version=2,
    groups=perf_groups(time_multiplier=0.6),
    reference=_unique_course_order,
    limits={"timeMillis": 2000, "memoryMb": 256, "outputBytes": 65536},
    cases={
        "sample": [("01", [3, [0, 1, 1, 2]]), ("02", [3, [0, 1, 0, 2]])],
        "boundary": [
            ("01-single", [1, []]),
            ("02-two-independent", [2, []]),
            ("03-cycle", [3, [0, 1, 1, 2, 2, 0]]),
            # 갈라졌다 다시 모인다 — 가운데에서 둘 중 아무거나 먼저 들을 수 있다.
            ("04-diamond", [4, [0, 1, 0, 2, 1, 3, 2, 3]]),
            ("05-duplicate-edges", [2, [0, 1, 0, 1]]),
            # 마지막 한 과목만 떨어져 있다.
            ("06-isolated-tail", [3, [0, 1]]),
            # 순환이 사슬 뒤에 숨어 있다.
            ("07-chain-then-cycle", [5, [0, 1, 1, 2, 2, 3, 3, 4, 4, 2]]),
        ],
        "hidden": [
            ("01-chain-small", [10, _chain_edges(10, salt=9621)]),
            ("02-chain-with-extra", [10, _chain_edges(10, salt=9623) + [0, 5, 2, 7]]),
            ("03-random-small", [12, _dag_forward(12, 20, salt=9625)]),
            ("04-random-medium", [500, _dag_forward(500, 1500, salt=9627)]),
            ("05-two-chains", [20, _chain_edges(10, salt=9629) + flat([[i, i + 1] for i in range(10, 19)])]),
        ],
        "performance": [
            ("01-chain", [100000, _chain_edges(100000, salt=9631)]),
            ("02-long-chain", [200000, _chain_edges(200000, salt=9633)]),
            ("03-chain-with-shortcuts", [200000, _chain_edges(200000, salt=9635) + _dag_forward(200000, 200000, salt=9637)]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). Kahn 을 돌리며 큐에 둘 이상이 들어오는 순간 갈라진다.
fun uniqueCourseOrder(n: Int, edges: IntArray): Int {
    val head = IntArray(n) { -1 }
    val next = IntArray(edges.size / 2)
    val to = IntArray(edges.size / 2)
    val indegree = IntArray(n)
    for (i in edges.indices step 2) {
        val e = i / 2
        to[e] = edges[i + 1]; next[e] = head[edges[i]]; head[edges[i]] = e
        indegree[edges[i + 1]] += 1
    }
    val queue = IntArray(n); var front = 0; var back = 0
    for (v in 0 until n) if (indegree[v] == 0) { queue[back++] = v; Drill.enqueue(v) }
    var seen = 0
    while (front < back) {
        if (back - front > 1) return 0
        val u = queue[front++]
        Drill.dequeue(u)
        seen += 1
        var e = head[u]
        while (e != -1) {
            val v = to[e]
            indegree[v] -= 1
            if (indegree[v] == 0) { queue[back++] = v; Drill.enqueue(v) }
            e = next[e]
        }
    }
    return if (seen == n) 1 else 0
}
""",
    mutants=[
        ("ignores-queue-size", "WRONG_ALGORITHM",
         "순서가 있는지만 보고 하나뿐인지는 보지 않는다. 갈라지는 자리가 있어도 1 이다.",
         """
fun uniqueCourseOrder(n: Int, edges: IntArray): Int {
    val adj = Array(n) { ArrayList<Int>() }
    val indegree = IntArray(n)
    for (i in edges.indices step 2) { adj[edges[i]].add(edges[i + 1]); indegree[edges[i + 1]] += 1 }
    val queue = IntArray(n); var front = 0; var back = 0
    for (v in 0 until n) if (indegree[v] == 0) queue[back++] = v
    var seen = 0
    while (front < back) {
        val u = queue[front++]; seen += 1
        for (v in adj[u]) { indegree[v] -= 1; if (indegree[v] == 0) queue[back++] = v }
    }
    return if (seen == n) 1 else 0
}
"""),
        ("checks-only-first-step", "WRONG_BRANCH",
         "시작할 수 있는 과목이 하나인지만 보고 그 뒤로 갈라지는 자리는 보지 않는다.",
         """
fun uniqueCourseOrder(n: Int, edges: IntArray): Int {
    val adj = Array(n) { ArrayList<Int>() }
    val indegree = IntArray(n)
    for (i in edges.indices step 2) { adj[edges[i]].add(edges[i + 1]); indegree[edges[i + 1]] += 1 }
    var roots = 0
    for (v in 0 until n) if (indegree[v] == 0) roots += 1
    if (roots != 1) return 0
    val queue = IntArray(n); var front = 0; var back = 0
    for (v in 0 until n) if (indegree[v] == 0) queue[back++] = v
    var seen = 0
    while (front < back) {
        val u = queue[front++]; seen += 1
        for (v in adj[u]) { indegree[v] -= 1; if (indegree[v] == 0) queue[back++] = v }
    }
    return if (seen == n) 1 else 0
}
"""),
        ("ignores-cycle", "MISSING_EDGE_CASE",
         "순환을 보지 않는다. 처리하지 못한 과목이 남아도 갈라지지만 않았으면 1 이다.",
         """
fun uniqueCourseOrder(n: Int, edges: IntArray): Int {
    val adj = Array(n) { ArrayList<Int>() }
    val indegree = IntArray(n)
    for (i in edges.indices step 2) { adj[edges[i]].add(edges[i + 1]); indegree[edges[i + 1]] += 1 }
    val queue = IntArray(n); var front = 0; var back = 0
    for (v in 0 until n) if (indegree[v] == 0) queue[back++] = v
    while (front < back) {
        if (back - front > 1) return 0
        val u = queue[front++]
        for (v in adj[u]) { indegree[v] -= 1; if (indegree[v] == 0) queue[back++] = v }
    }
    return 1
}
"""),
        ("scan-all-edges-per-pair", "PERFORMANCE",
         "위상 순서를 하나 구한 뒤 이웃한 두 과목마다 간선 배열을 처음부터 훑는다. O(n*m).",
         """
fun uniqueCourseOrder(n: Int, edges: IntArray): Int {
    val adj = Array(n) { ArrayList<Int>() }
    val indegree = IntArray(n)
    for (i in edges.indices step 2) { adj[edges[i]].add(edges[i + 1]); indegree[edges[i + 1]] += 1 }
    val order = IntArray(n); var front = 0; var back = 0
    for (v in 0 until n) if (indegree[v] == 0) order[back++] = v
    while (front < back) {
        val u = order[front++]
        for (v in adj[u]) { indegree[v] -= 1; if (indegree[v] == 0) order[back++] = v }
    }
    if (back != n) return 0
    for (i in 0 until n - 1) {
        var linked = false
        for (j in edges.indices step 2) {
            Drill.compare(order[i], order[i + 1])
            if (edges[j] == order[i] && edges[j + 1] == order[i + 1]) { linked = true; break }
        }
        if (!linked) return 0
    }
    return 1
}
"""),
    ],
))


# --- 181. 간선을 지워 가며 세는 연결 요소 (거꾸로 처리) -----------------------------------------------

def _components_after_removals(n, edges, removals):
    m = len(edges) // 2
    removed = [False] * m
    for index in removals:
        removed[index] = True
    parent = list(range(n))

    def find(x):
        while parent[x] != x:
            parent[x] = parent[parent[x]]
            x = parent[x]
        return x

    count = n

    def union(a, b):
        nonlocal count
        ra, rb = find(a), find(b)
        if ra != rb:
            parent[ra] = rb
            count -= 1

    for i in range(m):
        if not removed[i]:
            union(edges[2 * i], edges[2 * i + 1])
    out = [count]
    for index in reversed(removals):
        union(edges[2 * index], edges[2 * index + 1])
        out.append(count)
    out.reverse()
    return out


def _random_graph(n, m, salt):
    a = randoms(m, 0, n - 1, salt=salt)
    span = randoms(m, 1, n - 1, salt=salt + 1)
    return flat([a[i], (a[i] + span[i]) % n] for i in range(m))


PROBLEMS.append(Problem(
    id="components-after-removals",
    title="간선을 지워 가며 세는 연결 요소",
    summary="""
정점 `n` 개(`0..n-1`)와 무방향 간선 `edges = [a1, b1, ...]` 가 주어진다. `removals` 는 지울 간선의 **번호**를
지우는 순서대로 담은 배열이다(번호는 `edges` 에서의 순서 `0..간선수-1`, 서로 다르다).

연결 요소의 개수를 `removals.length + 1` 개 반환한다 — 아무것도 지우지 않은 처음 상태부터, 매 제거 직후까지.
""",
    notes="""
유니온 파인드는 **합칠 수는 있어도 쪼갤 수는 없다.** 지우는 순서대로 따라가면 매번 처음부터 다시 만들어야 한다.

거꾸로 보면 제거는 추가가 된다. 지울 간선을 모두 뺀 **마지막 상태**를 먼저 만들고, 지운 역순으로 하나씩
되살리며 세면 각 시점의 답이 한 번씩 나온다. 마지막에 뒤집어 돌려준다.
""",
    drill_doc="""
Drill.match(a, b)             // 두 정점을 같은 무리로 합쳤다
Drill.write(t, count)         // t 시점의 연결 요소 수
""",
    constraints="""
- `1 <= n <= 100_000`, 간선 `0..200_000` 개, 지우는 간선 `0..간선 수` 개
- 간선은 서로 다른 두 정점을 잇고, 같은 쌍이 여럿 있을 수 있다
""",
    signature=dict(
        name="componentsAfterRemovals",
        parameters=[("n", "INT"), ("edges", "INT_ARRAY"), ("removals", "INT_ARRAY")],
        returns="INT_ARRAY",
    ),
    groups=perf_groups(),
    reference=_components_after_removals,
    # 출력 한도는 그룹의 케이스들이 나눠 쓴다 — 한 케이스가 1MB 면 셋이 2MB 를 넘는다.
    limits={"timeMillis": 2000, "memoryMb": 256, "outputBytes": 8000000},
    cases={
        "sample": [
            ("01", [4, [0, 1, 1, 2, 2, 3], [1, 0]]),
            ("02", [3, [0, 1, 1, 2], []]),
        ],
        "boundary": [
            ("01-single-vertex", [1, [], []]),
            ("02-remove-all", [3, [0, 1, 1, 2], [0, 1]]),
            # 같은 쌍의 간선이 둘. 하나를 지워도 아직 붙어 있다.
            ("03-parallel-edges", [2, [0, 1, 0, 1], [0, 1]]),
            # 순환 위의 간선은 지워도 요소 수가 그대로다.
            ("04-cycle", [3, [0, 1, 1, 2, 2, 0], [0, 1, 2]]),
            ("05-remove-out-of-order", [5, [0, 1, 1, 2, 2, 3, 3, 4], [2, 0, 3]]),
            ("06-isolated-vertices", [4, [0, 1], [0]]),
            ("07-no-edges-no-removals", [3, [], []]),
        ],
        "hidden": [
            ("01-random-small", [8, _random_graph(8, 12, salt=9641), [3, 0, 7, 1]]),
            ("02-random-medium", [200, _random_graph(200, 400, salt=9643), shuffled(range(0, 400, 3), salt=9645)]),
            ("03-sparse", [1000, _random_graph(1000, 700, salt=9647), shuffled(range(0, 700, 2), salt=9649)]),
            ("04-dense", [300, _random_graph(300, 2000, salt=9651), shuffled(range(2000), salt=9653)]),
            ("05-remove-none", [500, _random_graph(500, 800, salt=9655), []]),
        ],
        "performance": [
            ("01-medium", [50000, _random_graph(50000, 60000, salt=9661), shuffled(range(60000), salt=9663)]),
            ("02-large", [100000, _random_graph(100000, 150000, salt=9665), shuffled(range(150000), salt=9667)]),
            ("03-sparse-large", [100000, _random_graph(100000, 120000, salt=9669), shuffled(range(0, 120000, 2), salt=9671)]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 남은 간선으로 마지막 상태를 만들고, 지운 역순으로 되살린다.
fun componentsAfterRemovals(n: Int, edges: IntArray, removals: IntArray): IntArray {
    val m = edges.size / 2
    val removed = BooleanArray(m)
    for (index in removals) removed[index] = true
    val parent = IntArray(n) { it }
    fun find(start: Int): Int {
        var x = start
        while (parent[x] != x) { parent[x] = parent[parent[x]]; x = parent[x] }
        return x
    }
    var count = n
    fun union(a: Int, b: Int) {
        val ra = find(a); val rb = find(b)
        if (ra != rb) { parent[ra] = rb; count -= 1; Drill.match(a, b) }
    }
    for (i in 0 until m) if (!removed[i]) union(edges[2 * i], edges[2 * i + 1])
    val out = IntArray(removals.size + 1)
    out[removals.size] = count
    for (t in removals.size - 1 downTo 0) {
        val index = removals[t]
        union(edges[2 * index], edges[2 * index + 1])
        out[t] = count
        Drill.write(t, count)
    }
    return out
}
""",
    mutants=[
        ("keeps-removed-edges", "WRONG_ALGORITHM",
         "마지막 상태를 만들 때 지울 간선까지 합친다. 어느 시점에서도 개수가 줄지 않는다.",
         """
fun componentsAfterRemovals(n: Int, edges: IntArray, removals: IntArray): IntArray {
    val parent = IntArray(n) { it }
    fun find(start: Int): Int { var x = start; while (parent[x] != x) { parent[x] = parent[parent[x]]; x = parent[x] }; return x }
    var count = n
    fun union(a: Int, b: Int) { val ra = find(a); val rb = find(b); if (ra != rb) { parent[ra] = rb; count -= 1 } }
    for (i in 0 until edges.size / 2) union(edges[2 * i], edges[2 * i + 1])
    val out = IntArray(removals.size + 1)
    out[removals.size] = count
    for (t in removals.size - 1 downTo 0) { val index = removals[t]; union(edges[2 * index], edges[2 * index + 1]); out[t] = count }
    return out
}
"""),
        ("forward-not-reversed", "WRONG_BRANCH",
         "되살리는 순서대로 답을 적는다. 시간이 거꾸로 실려 나간다.",
         """
fun componentsAfterRemovals(n: Int, edges: IntArray, removals: IntArray): IntArray {
    val m = edges.size / 2
    val removed = BooleanArray(m)
    for (index in removals) removed[index] = true
    val parent = IntArray(n) { it }
    fun find(start: Int): Int { var x = start; while (parent[x] != x) { parent[x] = parent[parent[x]]; x = parent[x] }; return x }
    var count = n
    fun union(a: Int, b: Int) { val ra = find(a); val rb = find(b); if (ra != rb) { parent[ra] = rb; count -= 1 } }
    for (i in 0 until m) if (!removed[i]) union(edges[2 * i], edges[2 * i + 1])
    val out = IntArray(removals.size + 1)
    out[0] = count
    for (t in removals.indices) {
        val index = removals[removals.size - 1 - t]
        union(edges[2 * index], edges[2 * index + 1])
        out[t + 1] = count
    }
    return out
}
"""),
        ("drops-initial-state", "OFF_BY_ONE",
         "제거 직후의 개수만 돌려주고 처음 상태를 빼먹는다.",
         """
fun componentsAfterRemovals(n: Int, edges: IntArray, removals: IntArray): IntArray {
    val m = edges.size / 2
    val removed = BooleanArray(m)
    for (index in removals) removed[index] = true
    val parent = IntArray(n) { it }
    fun find(start: Int): Int { var x = start; while (parent[x] != x) { parent[x] = parent[parent[x]]; x = parent[x] }; return x }
    var count = n
    fun union(a: Int, b: Int) { val ra = find(a); val rb = find(b); if (ra != rb) { parent[ra] = rb; count -= 1 } }
    for (i in 0 until m) if (!removed[i]) union(edges[2 * i], edges[2 * i + 1])
    val out = IntArray(removals.size)
    if (removals.isNotEmpty()) out[removals.size - 1] = count
    for (t in removals.size - 2 downTo 0) {
        val index = removals[t + 1]
        union(edges[2 * index], edges[2 * index + 1])
        out[t] = count
    }
    return out
}
"""),
        ("rebuild-each-step", "PERFORMANCE",
         "시점마다 유니온 파인드를 새로 만들고 남은 간선을 전부 합친다. O(지운 수 * 간선 수).",
         """
fun componentsAfterRemovals(n: Int, edges: IntArray, removals: IntArray): IntArray {
    val m = edges.size / 2
    val out = IntArray(removals.size + 1)
    val removed = BooleanArray(m)
    for (t in 0..removals.size) {
        val parent = IntArray(n) { it }
        var count = n
        fun find(start: Int): Int { var x = start; while (parent[x] != x) { parent[x] = parent[parent[x]]; x = parent[x] }; return x }
        for (i in 0 until m) {
            if (removed[i]) continue
            Drill.compare(edges[2 * i], edges[2 * i + 1])
            val ra = find(edges[2 * i]); val rb = find(edges[2 * i + 1])
            if (ra != rb) { parent[ra] = rb; count -= 1 }
        }
        out[t] = count
        if (t < removals.size) removed[removals[t]] = true
    }
    return out
}
"""),
    ],
))


# --- 195. 금지된 사이를 지키며 친구 맺기 (유니온 파인드) ------------------------------------------

def _friend_requests(n, restrictions, requests):
    parent = list(range(n))

    def find(x):
        while parent[x] != x:
            parent[x] = parent[parent[x]]
            x = parent[x]
        return x

    out = []
    for i in range(0, len(requests), 2):
        a, b = find(requests[i]), find(requests[i + 1])
        allowed = True
        if a != b:
            for j in range(0, len(restrictions), 2):
                x, y = find(restrictions[j]), find(restrictions[j + 1])
                # 두 무리가 붙으면 금지된 쌍이 한 무리가 되는가
                if (x == a and y == b) or (x == b and y == a):
                    allowed = False
                    break
        if allowed:
            parent[a] = b
        out.append(1 if allowed else 0)
    return out


def _distinct_pairs(n, count, salt):
    a = randoms(count, 0, n - 1, salt=salt)
    span = randoms(count, 1, n - 1, salt=salt + 1)
    return flat([a[i], (a[i] + span[i]) % n] for i in range(count))


PROBLEMS.append(Problem(
    id="friend-requests",
    title="금지된 사이를 지키며 친구 맺기",
    summary="""
사람 `n` 명(`0..n-1`)과 **절대 같은 무리가 되면 안 되는 쌍** `restrictions = [x1, y1, ...]` 가 주어진다.
친구 요청 `requests = [a1, b1, ...]` 을 **주어진 순서대로** 처리한다. 요청을 받아들이면 두 사람의 무리가
하나로 합쳐진다 — 친구의 친구도 같은 무리다.

받아들여도 금지된 쌍이 같은 무리가 되지 **않을 때만** 받아들인다. 요청마다 `1`(받아들임) / `0`(거절)을
순서대로 담은 배열을 반환한다. 거절된 요청은 아무것도 바꾸지 않는다.
""",
    notes="""
"같은 무리"는 합치기와 묻기만 있으면 되니 유니온 파인드다. 어려운 것은 **미리 보기**다 — 합치기 전에
"합치면 무엇이 깨지는가"를 알아야 한다.

두 무리를 합쳤을 때 금지된 쌍 `(x, y)` 가 한 무리가 되는 경우는 하나뿐이다: `x` 가 한쪽 무리에, `y` 가 다른
쪽 무리에 있는 것. 그러니 요청마다 금지 목록을 훑어 **그 둘의 대표가 지금 두 무리와 맞는지**만 보면 된다.
""",
    drill_doc="""
Drill.match(a, b)             // 두 무리를 합쳤다
Drill.write(q, verdict)       // q 번째 요청의 결과
""",
    constraints="""
- `2 <= n <= 1_000`, 금지 쌍 `0..1_000` 개, 요청 `1..1_000` 개
- 금지 쌍과 요청의 두 사람은 서로 다르다
""",
    signature=dict(
        name="friendRequests",
        parameters=[("n", "INT"), ("restrictions", "INT_ARRAY"), ("requests", "INT_ARRAY")],
        returns="INT_ARRAY",
    ),
    # 정답 풀이 자체가 요청 × 금지라, 같은 접근을 조금 더 느리게 한 오답은 자릿수가 아니라 상수배로만
    # 진다. 그런 오답은 머신에 따라 갈리므로 성능 그룹을 두지 않는다 (§12.1 재현성).
    groups=standard_groups(),
    reference=_friend_requests,
    limits={"timeMillis": 2000, "memoryMb": 256, "outputBytes": 2000000},
    cases={
        "sample": [
            ("01", [3, [0, 1], [0, 2, 2, 1]]),
            ("02", [3, [0, 1], [1, 2, 0, 2]]),
        ],
        "boundary": [
            ("01-no-restrictions", [4, [], [0, 1, 1, 2, 2, 3]]),
            ("02-direct-restriction", [2, [0, 1], [0, 1]]),
            # 이미 같은 무리인 요청은 언제나 받아들인다 — 아무것도 바뀌지 않는다.
            ("03-already-together", [3, [0, 2], [0, 1, 1, 0]]),
            ("04-rejected-changes-nothing", [4, [0, 3], [0, 1, 2, 3, 1, 2, 1, 3]]),
            ("05-restriction-both-ways", [3, [2, 0], [0, 1, 1, 2]]),
            ("06-chain-then-block", [5, [0, 4], [0, 1, 1, 2, 2, 3, 3, 4]]),
            ("07-self-contained", [4, [1, 2], [0, 3]]),
        ],
        "hidden": [
            ("01-random-small", [8, _distinct_pairs(8, 3, salt=9951), _distinct_pairs(8, 12, salt=9953)]),
            ("02-random-medium", [100, _distinct_pairs(100, 40, salt=9955), _distinct_pairs(100, 200, salt=9957)]),
            ("03-many-restrictions", [50, _distinct_pairs(50, 200, salt=9959), _distinct_pairs(50, 100, salt=9961)]),
            ("04-few-restrictions", [200, _distinct_pairs(200, 5, salt=9963), _distinct_pairs(200, 400, salt=9965)]),
            ("05-no-requests-accepted", [4, [0, 1, 0, 2, 0, 3, 1, 2, 1, 3, 2, 3], _distinct_pairs(4, 10, salt=9967)]),
            ("06-large", [1000, _distinct_pairs(1000, 1000, salt=9975), _distinct_pairs(1000, 1000, salt=9977)]),
            ("07-dense-restrictions", [1000, _distinct_pairs(1000, 1000, salt=9979), _distinct_pairs(1000, 1000, salt=9981)]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 합치기 전에 금지 목록을 훑어 "합치면 깨지는가"를 본다.
fun friendRequests(n: Int, restrictions: IntArray, requests: IntArray): IntArray {
    val parent = IntArray(n) { it }
    fun find(start: Int): Int {
        var x = start
        while (parent[x] != x) { parent[x] = parent[parent[x]]; x = parent[x] }
        return x
    }
    val out = IntArray(requests.size / 2)
    for (q in out.indices) {
        val a = find(requests[2 * q])
        val b = find(requests[2 * q + 1])
        var allowed = true
        if (a != b) {
            var j = 0
            while (j < restrictions.size) {
                val x = find(restrictions[j])
                val y = find(restrictions[j + 1])
                if ((x == a && y == b) || (x == b && y == a)) { allowed = false; break }
                j += 2
            }
        }
        if (allowed) { parent[a] = b; Drill.match(a, b) }
        out[q] = if (allowed) 1 else 0
        Drill.write(q, out[q])
    }
    return out
}
""",
    mutants=[
        ("checks-only-direct-pair", "WRONG_ALGORITHM",
         "요청한 두 사람이 곧바로 금지된 쌍인지만 본다. 무리를 타고 이어지는 금지를 놓친다.",
         """
fun friendRequests(n: Int, restrictions: IntArray, requests: IntArray): IntArray {
    val parent = IntArray(n) { it }
    fun find(start: Int): Int { var x = start; while (parent[x] != x) { parent[x] = parent[parent[x]]; x = parent[x] }; return x }
    val out = IntArray(requests.size / 2)
    for (q in out.indices) {
        val u = requests[2 * q]; val v = requests[2 * q + 1]
        var allowed = true
        var j = 0
        while (j < restrictions.size) {
            if ((restrictions[j] == u && restrictions[j + 1] == v) || (restrictions[j] == v && restrictions[j + 1] == u)) { allowed = false; break }
            j += 2
        }
        if (allowed) parent[find(u)] = find(v)
        out[q] = if (allowed) 1 else 0
    }
    return out
}
"""),
        ("one-direction-only", "WRONG_BRANCH",
         "금지된 쌍을 한 방향으로만 견준다. 무리가 반대로 붙은 경우를 놓친다.",
         """
fun friendRequests(n: Int, restrictions: IntArray, requests: IntArray): IntArray {
    val parent = IntArray(n) { it }
    fun find(start: Int): Int { var x = start; while (parent[x] != x) { parent[x] = parent[parent[x]]; x = parent[x] }; return x }
    val out = IntArray(requests.size / 2)
    for (q in out.indices) {
        val a = find(requests[2 * q]); val b = find(requests[2 * q + 1])
        var allowed = true
        if (a != b) {
            var j = 0
            while (j < restrictions.size) {
                val x = find(restrictions[j]); val y = find(restrictions[j + 1])
                if (x == a && y == b) { allowed = false; break }
                j += 2
            }
        }
        if (allowed) parent[a] = b
        out[q] = if (allowed) 1 else 0
    }
    return out
}
"""),
        ("merges-before-checking", "MISSING_EDGE_CASE",
         "먼저 합치고 나서 금지가 깨졌는지 본다. 거절된 요청이 무리를 바꿔 놓는다.",
         """
fun friendRequests(n: Int, restrictions: IntArray, requests: IntArray): IntArray {
    val parent = IntArray(n) { it }
    fun find(start: Int): Int { var x = start; while (parent[x] != x) { parent[x] = parent[parent[x]]; x = parent[x] }; return x }
    val out = IntArray(requests.size / 2)
    for (q in out.indices) {
        val a = find(requests[2 * q]); val b = find(requests[2 * q + 1])
        if (a != b) parent[a] = b
        var allowed = true
        var j = 0
        while (j < restrictions.size) {
            if (find(restrictions[j]) == find(restrictions[j + 1])) { allowed = false; break }
            j += 2
        }
        out[q] = if (allowed) 1 else 0
    }
    return out
}
"""),
        ("checks-only-first-restriction", "MISSING_EDGE_CASE",
         "금지 목록의 첫 쌍만 본다. 뒤에 적힌 금지는 지켜지지 않는다.",
         """
fun friendRequests(n: Int, restrictions: IntArray, requests: IntArray): IntArray {
    val parent = IntArray(n) { it }
    fun find(start: Int): Int { var x = start; while (parent[x] != x) { parent[x] = parent[parent[x]]; x = parent[x] }; return x }
    val out = IntArray(requests.size / 2)
    for (q in out.indices) {
        val a = find(requests[2 * q]); val b = find(requests[2 * q + 1])
        var allowed = true
        if (a != b && restrictions.isNotEmpty()) {
            val x = find(restrictions[0]); val y = find(restrictions[1])
            if ((x == a && y == b) || (x == b && y == a)) allowed = false
        }
        if (allowed) parent[a] = b
        out[q] = if (allowed) 1 else 0
    }
    return out
}
"""),
    ],
))


# --- 196. 최단 경로의 개수 ------------------------------------------------------------------------

def _shortest_path_count(n, edges):
    import heapq
    mod = 1_000_000_007
    adj = [[] for _ in range(n)]
    for i in range(0, len(edges), 3):
        a, b, w = edges[i], edges[i + 1], edges[i + 2]
        adj[a].append((b, w))
        adj[b].append((a, w))
    inf = float("inf")
    dist = [inf] * n
    ways = [0] * n
    dist[0] = 0
    ways[0] = 1
    heap = [(0, 0)]
    done = [False] * n
    while heap:
        d, v = heapq.heappop(heap)
        if done[v]:
            continue
        done[v] = True
        for u, w in adj[v]:
            if d + w < dist[u]:
                dist[u] = d + w
                ways[u] = ways[v]
                heapq.heappush(heap, (dist[u], u))
            elif d + w == dist[u]:
                ways[u] = (ways[u] + ways[v]) % mod
    return ways[n - 1] % mod


def _layered_graph(layers, width, salt, weight=1):
    """0 → 층 → … → 마지막 정점. 층마다 width 갈래라 최단 경로가 width^layers 개다."""
    edges = []
    for i in range(width):
        edges += [0, 1 + i, weight]
    for layer in range(layers - 1):
        base = 1 + layer * width
        nxt = base + width
        for a in range(width):
            for b in range(width):
                edges += [base + a, nxt + b, weight]
    last = 1 + layers * width
    for i in range(width):
        edges += [1 + (layers - 1) * width + i, last, weight]
    return edges


PROBLEMS.append(Problem(
    id="shortest-path-count",
    title="최단 경로의 개수",
    summary="""
정점 `n` 개와 무방향 간선 `edges = [a1, b1, w1, ...]` 이 주어진다. 정점 `0` 에서 `n-1` 까지 가는 **최단 경로가
몇 가지인가**를 `1000000007` 로 나눈 나머지로 반환한다. 닿을 수 없으면 `0` 이다.
""",
    notes="""
거리를 구하면서 **가짓수를 같이 들고 간다.** 정점 `u` 의 거리가 줄어들면 가짓수는 새 앞 정점의 것으로
바뀌고, 같은 거리로 다시 닿으면 **더한다**.

거리가 확정된 순서대로 처리해야 더한 값이 다시 뒤집히지 않는다 — 가장 가까운 것부터 꺼내는 우선순위 큐가
그 순서를 준다. 가짓수는 금세 커지므로 더할 때마다 나머지를 취한다.
""",
    drill_doc="""
Drill.visit(v, distance)      // v 의 거리를 확정했다
Drill.write(v, ways)          // v 로 가는 최단 경로의 수
""",
    constraints="""
- `1 <= n <= 50_000`, 간선 `0..200_000` 개, `1 <= w <= 1_000_000`
- 같은 쌍의 간선이 여럿 있을 수 있고, 자기 자신으로 가는 간선은 없다
""",
    signature=dict(name="shortestPathCount", parameters=[("n", "INT"), ("edges", "INT_ARRAY")], returns="INT"),
    groups=perf_groups(),
    reference=_shortest_path_count,
    limits={"timeMillis": 2000, "memoryMb": 256, "outputBytes": 65536},
    cases={
        "sample": [
            ("01", [4, [0, 1, 1, 0, 2, 1, 1, 3, 1, 2, 3, 1]]),
            ("02", [3, [0, 1, 1, 1, 2, 1]]),
        ],
        "boundary": [
            ("01-single-vertex", [1, []]),
            ("02-unreachable", [3, [0, 1, 5]]),
            ("03-parallel-edges", [2, [0, 1, 4, 0, 1, 4]]),
            # 더 짧은 길이 나중에 나타난다 — 가짓수를 덮어써야 한다.
            ("04-shorter-later", [4, [0, 1, 10, 1, 3, 10, 0, 2, 1, 2, 3, 1]]),
            ("05-same-length-two-ways", [4, [0, 1, 2, 0, 2, 2, 1, 3, 3, 2, 3, 3]]),
            ("06-longer-path-ignored", [4, [0, 1, 1, 1, 3, 1, 0, 2, 5, 2, 3, 5]]),
            ("07-weights-differ", [5, [0, 1, 1, 1, 4, 3, 0, 2, 2, 2, 4, 2, 0, 3, 4, 3, 4, 1]]),
        ],
        "hidden": [
            ("01-random-small", [10, _forward_edges(10, 20, salt=9985, low=1, high=5)]),
            ("02-random-medium", [200, _forward_edges(200, 600, salt=9987, low=1, high=20)]),
            ("03-layered-small", [1 + 3 * 5 + 1, _layered_graph(5, 3, salt=0)]),
            ("04-equal-weights", [300, _forward_edges(300, 900, salt=9989, low=1, high=1)]),
            ("05-sparse", [1000, _forward_edges(1000, 1200, salt=9991, low=1, high=100)]),
        ],
        "performance": [
            # 층마다 갈래가 셋이라 최단 경로가 3^층 개다 — 경로를 하나씩 세는 풀이가 끝나지 않는다.
            ("01-layered", [1 + 3 * 2000 + 1, _layered_graph(2000, 3, salt=0)]),
            ("02-layered-wide", [1 + 4 * 5000 + 1, _layered_graph(5000, 4, salt=0)]),
            ("03-random-large", [50000, _forward_edges(50000, 150000, salt=9993, low=1, high=1000)]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 다익스트라로 거리를 확정하며 가짓수를 함께 옮긴다.
fun shortestPathCount(n: Int, edges: IntArray): Int {
    val mod = 1_000_000_007L
    val head = IntArray(n) { -1 }
    val next = IntArray(edges.size / 3 * 2)
    val to = IntArray(edges.size / 3 * 2)
    val weight = IntArray(edges.size / 3 * 2)
    var count = 0
    for (i in edges.indices step 3) {
        val a = edges[i]; val b = edges[i + 1]; val w = edges[i + 2]
        to[count] = b; weight[count] = w; next[count] = head[a]; head[a] = count; count += 1
        to[count] = a; weight[count] = w; next[count] = head[b]; head[b] = count; count += 1
    }
    val inf = Long.MAX_VALUE / 4
    val dist = LongArray(n) { inf }
    val ways = LongArray(n)
    val done = BooleanArray(n)
    dist[0] = 0
    ways[0] = 1
    val heap = java.util.PriorityQueue<LongArray>(compareBy { it[0] })
    heap.add(longArrayOf(0, 0))
    while (heap.isNotEmpty()) {
        val top = heap.poll()
        val v = top[1].toInt()
        if (done[v]) continue
        done[v] = true
        Drill.visit(v, dist[v].toInt())
        var e = head[v]
        while (e != -1) {
            val u = to[e]
            val candidate = dist[v] + weight[e]
            if (candidate < dist[u]) {
                dist[u] = candidate
                ways[u] = ways[v]
                heap.add(longArrayOf(candidate, u.toLong()))
                Drill.write(u, ways[u].toInt())
            } else if (candidate == dist[u]) {
                ways[u] = (ways[u] + ways[v]) % mod
                Drill.write(u, ways[u].toInt())
            }
            e = next[e]
        }
    }
    return (ways[n - 1] % mod).toInt()
}
""",
    mutants=[
        ("keeps-first-count", "MISSING_EDGE_CASE",
         "같은 거리로 다시 닿았을 때 가짓수를 더하지 않는다. 길이 같은 다른 길이 세어지지 않는다.",
         """
fun shortestPathCount(n: Int, edges: IntArray): Int {
    val mod = 1_000_000_007L
    val adj = Array(n) { ArrayList<IntArray>() }
    for (i in edges.indices step 3) { adj[edges[i]].add(intArrayOf(edges[i + 1], edges[i + 2])); adj[edges[i + 1]].add(intArrayOf(edges[i], edges[i + 2])) }
    val inf = Long.MAX_VALUE / 4
    val dist = LongArray(n) { inf }
    val ways = LongArray(n)
    val done = BooleanArray(n)
    dist[0] = 0; ways[0] = 1
    val heap = java.util.PriorityQueue<LongArray>(compareBy { it[0] })
    heap.add(longArrayOf(0, 0))
    while (heap.isNotEmpty()) {
        val top = heap.poll(); val v = top[1].toInt()
        if (done[v]) continue
        done[v] = true
        for (e in adj[v]) {
            val candidate = dist[v] + e[1]
            if (candidate < dist[e[0]]) { dist[e[0]] = candidate; ways[e[0]] = ways[v]; heap.add(longArrayOf(candidate, e[0].toLong())) }
        }
    }
    return (ways[n - 1] % mod).toInt()
}
"""),
        ("no-modulo", "WRONG_ALGORITHM",
         "가짓수에 나머지를 취하지 않는다. 갈래가 많으면 Int 를 넘겨 값이 뒤집힌다.",
         """
fun shortestPathCount(n: Int, edges: IntArray): Int {
    val adj = Array(n) { ArrayList<IntArray>() }
    for (i in edges.indices step 3) { adj[edges[i]].add(intArrayOf(edges[i + 1], edges[i + 2])); adj[edges[i + 1]].add(intArrayOf(edges[i], edges[i + 2])) }
    val inf = Long.MAX_VALUE / 4
    val dist = LongArray(n) { inf }
    val ways = IntArray(n)
    val done = BooleanArray(n)
    dist[0] = 0; ways[0] = 1
    val heap = java.util.PriorityQueue<LongArray>(compareBy { it[0] })
    heap.add(longArrayOf(0, 0))
    while (heap.isNotEmpty()) {
        val top = heap.poll(); val v = top[1].toInt()
        if (done[v]) continue
        done[v] = true
        for (e in adj[v]) {
            val candidate = dist[v] + e[1]
            if (candidate < dist[e[0]]) { dist[e[0]] = candidate; ways[e[0]] = ways[v]; heap.add(longArrayOf(candidate, e[0].toLong())) }
            else if (candidate == dist[e[0]]) ways[e[0]] = ways[e[0]] + ways[v]
        }
    }
    return ways[n - 1]
}
"""),
        ("ignores-weights", "WRONG_BRANCH",
         "간선을 한 걸음으로 세고 너비 우선으로 센다. 가중치가 다르면 최단이 아니다.",
         """
fun shortestPathCount(n: Int, edges: IntArray): Int {
    val mod = 1_000_000_007L
    val adj = Array(n) { ArrayList<Int>() }
    for (i in edges.indices step 3) { adj[edges[i]].add(edges[i + 1]); adj[edges[i + 1]].add(edges[i]) }
    val dist = IntArray(n) { -1 }
    val ways = LongArray(n)
    dist[0] = 0; ways[0] = 1
    val queue = java.util.ArrayDeque<Int>()
    queue.add(0)
    while (queue.isNotEmpty()) {
        val v = queue.poll()
        for (u in adj[v]) {
            if (dist[u] == -1) { dist[u] = dist[v] + 1; ways[u] = ways[v]; queue.add(u) }
            else if (dist[u] == dist[v] + 1) ways[u] = (ways[u] + ways[v]) % mod
        }
    }
    return (ways[n - 1] % mod).toInt()
}
"""),
        ("enumerates-paths", "PERFORMANCE",
         "최단 거리를 구한 뒤 경로를 하나씩 세어 나간다. 갈래가 늘면 경로 수만큼 돈다.",
         """
fun shortestPathCount(n: Int, edges: IntArray): Int {
    val mod = 1_000_000_007L
    val adj = Array(n) { ArrayList<IntArray>() }
    for (i in edges.indices step 3) { adj[edges[i]].add(intArrayOf(edges[i + 1], edges[i + 2])); adj[edges[i + 1]].add(intArrayOf(edges[i], edges[i + 2])) }
    val inf = Long.MAX_VALUE / 4
    val dist = LongArray(n) { inf }
    val done = BooleanArray(n)
    dist[0] = 0
    val heap = java.util.PriorityQueue<LongArray>(compareBy { it[0] })
    heap.add(longArrayOf(0, 0))
    while (heap.isNotEmpty()) {
        val top = heap.poll(); val v = top[1].toInt()
        if (done[v]) continue
        done[v] = true
        for (e in adj[v]) { val candidate = dist[v] + e[1]; if (candidate < dist[e[0]]) { dist[e[0]] = candidate; heap.add(longArrayOf(candidate, e[0].toLong())) } }
    }
    if (dist[n - 1] >= inf) return 0
    var total = 0L
    fun walk(v: Int, spent: Long) {
        Drill.compare(v, spent.toInt())
        if (v == n - 1) { total = (total + 1) % mod; return }
        for (e in adj[v]) {
            val next = spent + e[1]
            if (next + dist[n - 1] - dist[n - 1] <= dist[n - 1] && next == dist[e[0]]) walk(e[0], next)
        }
    }
    walk(0, 0)
    return (total % mod).toInt()
}
"""),
    ],
))


# --- 200. 가장 많은 색의 경로 ------------------------------------------------------------------

def _largest_color_path(colors, edges):
    import collections
    n = len(colors)
    adj = [[] for _ in range(n)]
    indeg = [0] * n
    for i in range(0, len(edges), 2):
        a, b = edges[i], edges[i + 1]
        adj[a].append(b)
        indeg[b] += 1
    dp = [[0] * 26 for _ in range(n)]
    queue = collections.deque(v for v in range(n) if indeg[v] == 0)
    seen = 0
    best = 0
    while queue:
        v = queue.popleft()
        seen += 1
        row = dp[v]
        row[ord(colors[v]) - 97] += 1
        best = max(best, row[ord(colors[v]) - 97])
        for u in adj[v]:
            target = dp[u]
            for k in range(26):
                if row[k] > target[k]:
                    target[k] = row[k]
            indeg[u] -= 1
            if indeg[u] == 0:
                queue.append(u)
    return best if seen == n else -1


def _colors(n, letters, salt):
    import random
    from author import SEED
    source = random.Random(SEED + salt)
    return "".join(source.choice(letters) for _ in range(n))


def _ladder(layers, width):
    """층마다 width 개의 정점, 이웃한 층끼리는 모두 잇는다 — 길이 width^layers 가지다."""
    edges = []
    for layer in range(layers - 1):
        for a in range(width):
            for b in range(width):
                edges += [layer * width + a, (layer + 1) * width + b]
    return edges


def _relabel(n, edges, salt):
    """정점 번호를 섞는다. 번호 순서가 곧 위상 순서면 번호대로 훑는 풀이가 우연히 맞는다."""
    order = shuffled(range(n), salt=salt)
    return [order[v] for v in edges]


PROBLEMS.append(Problem(
    id="largest-color-path",
    title="가장 많은 색의 경로",
    summary="""
정점 `n` 개의 방향 그래프다. 정점 `i` 의 색은 소문자 `colors[i]` 이고, 간선은 `edges = [a1, b1, a2, b2, ...]`
로 `a → b` 다. 어떤 경로의 **색 값**은 그 경로에서 가장 많이 나온 한 색의 개수다.

모든 경로의 색 값 중 가장 큰 것을 반환한다. 그래프에 **순환이 있으면 `-1`** 이다.
""",
    notes="""
경로가 끝나는 정점마다 "여기까지 오는 경로들에서 색마다 가장 많이 모을 수 있는 개수"를 26 칸으로 들고
있으면, 다음 정점은 앞 정점들의 칸을 칸마다 큰 값으로 합친 뒤 자기 색 칸에 하나를 더하면 된다.

이 합치기는 앞 정점이 **모두 끝난 뒤**에야 할 수 있다 — 들어오는 간선이 없는 정점부터 꺼내는 위상 정렬이
그 순서를 준다. 정렬이 모든 정점을 꺼내지 못했다면 남은 정점들이 순환을 이룬다.
""",
    drill_doc="""
Drill.dequeue(v)       // 앞이 모두 끝난 v 를 꺼냈다
Drill.write(v, count)  // v 에서 끝나는 경로의 자기 색 개수
""",
    constraints="""
- `1 <= n <= 100_000`, 간선 `0..200_000` 개
- 자기 자신으로 가는 간선(그 자체로 순환이다)과 같은 간선의 반복이 있을 수 있다
""",
    signature=dict(name="largestColorPath", parameters=[("colors", "STRING"), ("edges", "INT_ARRAY")], returns="INT"),
    groups=perf_groups(),
    reference=_largest_color_path,
    cases={
        "sample": [
            ("01", ["abaca", [0, 1, 0, 2, 2, 3, 3, 4]]),
            ("02", ["a", [0, 0]]),
        ],
        "boundary": [
            ("01-single", ["z", []]),
            ("02-no-edges", ["abc", []]),
            ("03-self-loop", ["ab", [0, 1, 1, 1]]),
            ("04-two-cycle", ["aa", [0, 1, 1, 0]]),
            # 두 갈래가 다시 만난다 — 한 번 본 정점을 다시 만난 것은 순환이 아니다.
            ("05-diamond", ["aaba", [0, 1, 0, 2, 1, 3, 2, 3]]),
            # 사이에 다른 색이 끼어도 센 개수는 이어진다.
            ("06-interleaved", ["abaca", [0, 1, 1, 2, 2, 3, 3, 4]]),
            # 처음 많던 색이 끝까지 가장 많지는 않다.
            ("07-leader-changes", ["aabbb", [0, 1, 1, 2, 2, 3, 3, 4]]),
            ("08-repeated-edge", ["aba", [0, 1, 0, 1, 1, 2]]),
            # 순환은 그래프 한쪽 구석에만 있어도 -1 이다.
            ("09-cycle-elsewhere", ["aaaab", [0, 1, 1, 2, 3, 4, 4, 3]]),
        ],
        "hidden": [
            ("01-random-small", [_colors(10, "ab", salt=10025), _relabel(10, _dag_forward(10, 15, salt=10027), salt=10029)]),
            ("02-random-medium", [_colors(300, "abc", salt=10031), _relabel(300, _dag_forward(300, 700, salt=10033), salt=10035)]),
            ("03-many-colors", [_colors(2000, "abcdefghijklmnopqrstuvwxyz", salt=10037), _relabel(2000, _dag_forward(2000, 6000, salt=10039), salt=10041)]),
            ("04-ladder", [_colors(40, "ab", salt=10043), _relabel(40, _ladder(20, 2), salt=10045)]),
            # 뒤로 가는 간선 하나가 순환을 닫는다.
            ("05-closing-back-edge", [_colors(500, "ab", salt=10047), _dag_forward(500, 900, salt=10049) + _dag_forward(500, 900, salt=10049)[1::-1]]),
            # 뒤로 가는 간선이 있어도 돌아올 길이 없으면 순환이 아니다.
            ("06-back-edge-no-cycle", [_colors(500, "ab", salt=10047), _dag_forward(500, 900, salt=10049) + [499, 0]]),
        ],
        "performance": [
            # 갈래가 둘씩인 층이 5 만 개라 경로가 2^50000 가지다 — 경로를 하나씩 따라가는 풀이가 끝나지 않는다.
            ("01-ladder", [_colors(100_000, "ab", salt=10051), _relabel(100_000, _ladder(50_000, 2), salt=10053)]),
            ("02-random-large", [_colors(100_000, "abcdefghijklmnopqrstuvwxyz", salt=10055), _relabel(100_000, _dag_forward(100_000, 200_000, salt=10057), salt=10059)]),
            ("03-long-chain", ["a" * 100_000, _relabel(100_000, flat([i, i + 1] for i in range(99_999)), salt=10061)]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 위상 순서대로 색마다 최댓값을 옮긴다.
fun largestColorPath(colors: String, edges: IntArray): Int {
    val n = colors.length
    val m = edges.size / 2
    val head = IntArray(n) { -1 }
    val next = IntArray(m)
    val to = IntArray(m)
    val indegree = IntArray(n)
    for (e in 0 until m) {
        val a = edges[2 * e]; val b = edges[2 * e + 1]
        to[e] = b; next[e] = head[a]; head[a] = e
        indegree[b] += 1
    }
    val best = IntArray(n * 26)
    val queue = IntArray(n)
    var tail = 0
    for (v in 0 until n) if (indegree[v] == 0) { queue[tail] = v; tail += 1 }
    var front = 0
    var answer = 0
    while (front < tail) {
        val v = queue[front]
        front += 1
        Drill.dequeue(v)
        val own = v * 26 + (colors[v] - 'a')
        best[own] += 1
        Drill.write(v, best[own])
        answer = maxOf(answer, best[own])
        var e = head[v]
        while (e != -1) {
            val u = to[e]
            for (k in 0 until 26) {
                if (best[v * 26 + k] > best[u * 26 + k]) best[u * 26 + k] = best[v * 26 + k]
            }
            indegree[u] -= 1
            if (indegree[u] == 0) { queue[tail] = u; tail += 1 }
            e = next[e]
        }
    }
    return if (front == n) answer else -1
}
""",
    mutants=[
        ("ignores-cycle", "MISSING_EDGE_CASE",
         "위상 정렬이 모든 정점을 꺼냈는지 보지 않는다. 순환이 있으면 -1 이어야 한다.",
         """
fun largestColorPath(colors: String, edges: IntArray): Int {
    val n = colors.length
    val adj = Array(n) { ArrayList<Int>() }
    val indegree = IntArray(n)
    for (i in edges.indices step 2) { adj[edges[i]].add(edges[i + 1]); indegree[edges[i + 1]] += 1 }
    val best = Array(n) { IntArray(26) }
    val queue = java.util.ArrayDeque<Int>()
    for (v in 0 until n) if (indegree[v] == 0) queue.add(v)
    var answer = 0
    while (queue.isNotEmpty()) {
        val v = queue.poll()
        best[v][colors[v] - 'a'] += 1
        answer = maxOf(answer, best[v][colors[v] - 'a'])
        for (u in adj[v]) {
            for (k in 0 until 26) best[u][k] = maxOf(best[u][k], best[v][k])
            indegree[u] -= 1
            if (indegree[u] == 0) queue.add(u)
        }
    }
    return answer
}
"""),
        ("runs-only", "WRONG_ALGORITHM",
         "같은 색이 연달아 나올 때만 센다. 사이에 다른 색이 끼면 그때까지 센 개수를 잃는다.",
         """
fun largestColorPath(colors: String, edges: IntArray): Int {
    val n = colors.length
    val adj = Array(n) { ArrayList<Int>() }
    val indegree = IntArray(n)
    for (i in edges.indices step 2) { adj[edges[i]].add(edges[i + 1]); indegree[edges[i + 1]] += 1 }
    val run = IntArray(n) { 1 }
    val queue = java.util.ArrayDeque<Int>()
    for (v in 0 until n) if (indegree[v] == 0) queue.add(v)
    var seen = 0
    var answer = 0
    while (queue.isNotEmpty()) {
        val v = queue.poll()
        seen += 1
        answer = maxOf(answer, run[v])
        for (u in adj[v]) {
            if (colors[u] == colors[v]) run[u] = maxOf(run[u], run[v] + 1)
            indegree[u] -= 1
            if (indegree[u] == 0) queue.add(u)
        }
    }
    return if (seen == n) answer else -1
}
"""),
        ("visited-as-cycle", "WRONG_BRANCH",
         "깊이 우선 탐색에서 한 번 본 정점을 다시 만나면 순환이라고 본다. 두 갈래가 다시 만나는 것은 순환이 아니다.",
         """
fun largestColorPath(colors: String, edges: IntArray): Int {
    val n = colors.length
    val adj = Array(n) { ArrayList<Int>() }
    for (i in edges.indices step 2) adj[edges[i]].add(edges[i + 1])
    val seen = BooleanArray(n)
    val best = Array(n) { IntArray(26) }
    var cycle = false
    fun dfs(v: Int) {
        seen[v] = true
        for (u in adj[v]) {
            if (seen[u]) { cycle = true; continue }
            dfs(u)
            for (k in 0 until 26) best[v][k] = maxOf(best[v][k], best[u][k])
        }
        best[v][colors[v] - 'a'] += 1
    }
    for (v in 0 until n) if (!seen[v]) dfs(v)
    if (cycle) return -1
    var answer = 0
    for (v in 0 until n) for (k in 0 until 26) answer = maxOf(answer, best[v][k])
    return answer
}
"""),
        ("walks-every-path", "PERFORMANCE",
         "시작점마다 모든 경로를 따라가며 색을 센다. 갈래가 겹치면 경로 수가 곱으로 불어난다.",
         """
fun largestColorPath(colors: String, edges: IntArray): Int {
    val n = colors.length
    val adj = Array(n) { ArrayList<Int>() }
    val indegree = IntArray(n)
    for (i in edges.indices step 2) { adj[edges[i]].add(edges[i + 1]); indegree[edges[i + 1]] += 1 }
    val left = indegree.copyOf()
    val queue = java.util.ArrayDeque<Int>()
    for (v in 0 until n) if (left[v] == 0) queue.add(v)
    var seen = 0
    while (queue.isNotEmpty()) { val v = queue.poll(); seen += 1; for (u in adj[v]) { left[u] -= 1; if (left[u] == 0) queue.add(u) } }
    if (seen < n) return -1
    val count = IntArray(26)
    var answer = 0
    val stack = java.util.ArrayDeque<IntArray>()
    for (s in 0 until n) {
        if (indegree[s] != 0) continue
        stack.push(intArrayOf(s, 0))
        count[colors[s] - 'a'] += 1
        answer = maxOf(answer, count[colors[s] - 'a'])
        while (stack.isNotEmpty()) {
            val top = stack.peek()
            val v = top[0]
            if (top[1] < adj[v].size) {
                val u = adj[v][top[1]]
                top[1] += 1
                Drill.compare(v, u)
                count[colors[u] - 'a'] += 1
                answer = maxOf(answer, count[colors[u] - 'a'])
                stack.push(intArrayOf(u, 0))
            } else {
                count[colors[v] - 'a'] -= 1
                stack.pop()
            }
        }
    }
    return answer
}
"""),
    ],
))


# --- 201. 만들 수 있는 물건 --------------------------------------------------------------------

def _craftable_items(n, recipes, supplies):
    import collections
    need = [0] * n
    users = [[] for _ in range(n)]
    for i in range(0, len(recipes), 2):
        a, b = recipes[i], recipes[i + 1]
        users[a].append(b)
        need[b] += 1
    have = [False] * n
    queue = collections.deque()
    for s in supplies:
        have[s] = True
        queue.append(s)
    while queue:
        x = queue.popleft()
        for p in users[x]:
            need[p] -= 1
            if need[p] == 0 and not have[p]:
                have[p] = True
                queue.append(p)
    supplied = set(supplies)
    return [v for v in range(n) if have[v] and v not in supplied]


def _raw_items(n, recipes, skip_every=0):
    """재료가 없는 물건들. skip_every 가 있으면 그중 몇을 일부러 빼 만들 수 없는 갈래를 남긴다."""
    products = set(recipes[1::2])
    raw = [v for v in range(n) if v not in products]
    return [v for i, v in enumerate(raw) if not skip_every or i % skip_every]


def _recipe_dag(n, m, salt):
    """서로 다른 (재료, 물건) 쌍 m 개. 재료는 언제나 더 큰 번호라 순환이 없다 — 번호를 섞어 쓴다."""
    import random
    from author import SEED
    source = random.Random(SEED + salt)
    seen = set()
    out = []
    while len(out) < 2 * m:
        b = source.randint(0, n - 2)
        a = source.randint(b + 1, min(n - 1, b + 6))
        if (a, b) not in seen:
            seen.add((a, b))
            out += [a, b]
    return out


PROBLEMS.append(Problem(
    id="craftable-items",
    title="만들 수 있는 물건",
    summary="""
물건이 `0..n-1` 번까지 있다. `recipes = [a1, b1, a2, b2, ...]` 의 한 쌍은 "물건 `b` 를 만들려면 재료 `a` 가
필요하다"는 뜻이고, 한 물건의 재료는 여럿일 수 있다. 처음에는 `supplies` 의 물건을 **얼마든지** 가지고 있다.

재료가 하나 이상 있고 그 재료를 **모두** 가졌거나 만들 수 있는 물건은 만들 수 있다. 재료가 없는 물건은
처음부터 가진 것이 아니면 만들 수 없다. 처음 가진 물건이 아니면서 **만들 수 있는 물건**의 번호를 오름차순으로
반환한다.
""",
    notes="""
물건마다 "아직 갖추지 못한 재료의 수"를 센다. 가진 물건에서 출발해, 하나를 갖출 때마다 그것을 재료로 쓰는
물건의 수를 하나씩 줄이고, 0 이 된 물건을 새로 갖춘다 — 들어오는 간선이 모두 풀린 정점을 꺼내는 위상
정렬이다. 시작점이 "재료가 없는 정점"이 아니라 **가진 물건**이라는 것만 다르다.

서로를 재료로 쓰는 물건들은 그 수가 끝내 0 이 되지 않으므로 저절로 빠진다.
""",
    drill_doc="""
Drill.dequeue(x)          // x 를 갖췄다
Drill.write(p, missing)   // p 에 아직 모자란 재료의 수
""",
    constraints="""
- `1 <= n <= 100_000`, 쌍 `0..200_000` 개, 같은 쌍은 두 번 나오지 않고 `a != b` 다
- `supplies` 의 번호는 서로 다르다
""",
    signature=dict(
        name="craftableItems",
        parameters=[("n", "INT"), ("recipes", "INT_ARRAY"), ("supplies", "INT_ARRAY")],
        returns="INT_ARRAY",
    ),
    groups=perf_groups(time_multiplier=0.5),
    reference=_craftable_items,
    limits={"timeMillis": 2000, "memoryMb": 256, "outputBytes": 4_000_000},
    cases={
        "sample": [
            # 빵(2)은 밀가루(0)·효모(1), 샌드위치(3)는 빵·고기(4) — 고기가 없다.
            ("01", [5, [0, 2, 1, 2, 2, 3, 4, 3], [0, 1]]),
            ("02", [4, [3, 2, 2, 1, 1, 0], [3]]),
        ],
        "boundary": [
            ("01-nothing-supplied", [3, [0, 1, 1, 2], []]),
            ("02-only-supplies", [3, [], [0, 1, 2]]),
            # 재료가 없는 물건은 가진 것이 아니면 만들 수 없다.
            ("03-raw-item-not-supplied", [3, [0, 2, 1, 2], [0]]),
            # 재료 하나만으로는 모자라다.
            ("04-needs-all", [4, [0, 3, 1, 3, 2, 3], [0, 1]]),
            # 서로를 재료로 쓰면 둘 다 만들 수 없다.
            ("05-mutual", [3, [0, 1, 1, 0, 2, 0], [2]]),
            # 가진 물건이 다른 물건의 결과이기도 하다 — 답에는 넣지 않는다.
            ("06-supply-also-craftable", [3, [0, 1, 1, 2], [0, 1]]),
            # 만든 순서와 번호 순서가 거꾸로다.
            ("07-reverse-numbered-chain", [5, [4, 3, 3, 2, 2, 1, 1, 0], [4]]),
            ("08-supply-in-cycle", [3, [0, 1, 1, 0, 1, 2], [0]]),
        ],
        "hidden": [
            ("01-random-small", [12, _relabel(12, _recipe_dag(12, 14, salt=10063), salt=10065), _raw_items(12, _relabel(12, _recipe_dag(12, 14, salt=10063), salt=10065))]),
            ("02-random-medium", [500, _relabel(500, _recipe_dag(500, 900, salt=10067), salt=10069), _raw_items(500, _relabel(500, _recipe_dag(500, 900, salt=10067), salt=10069), skip_every=5)]),
            ("03-random-large", [20_000, _relabel(20_000, _recipe_dag(20_000, 30_000, salt=10071), salt=10073), _raw_items(20_000, _relabel(20_000, _recipe_dag(20_000, 30_000, salt=10071), salt=10073), skip_every=9)]),
            ("04-cycle-with-exit", [6, [0, 1, 1, 2, 2, 1, 3, 4, 4, 5], [0, 3]]),
            ("05-wide-recipe", [1001, flat([i, 1000] for i in range(1000)), list(range(1000))]),
        ],
        "performance": [
            # 번호가 거꾸로 이어진 사슬 — 한 바퀴에 하나씩만 새로 만드는 풀이는 n 바퀴를 돈다.
            ("01-reverse-chain", [100_000, flat([i + 1, i] for i in range(99_999)), [99_999]]),
            ("02-random-large", [100_000, _relabel(100_000, _recipe_dag(100_000, 200_000, salt=10075), salt=10077), _raw_items(100_000, _relabel(100_000, _recipe_dag(100_000, 200_000, salt=10075), salt=10077), skip_every=50)]),
            ("03-reverse-chain-with-side", [100_000, flat([i + 1, i] for i in range(99_999)) + flat([99_999, i] for i in range(0, 99_999, 2)), [99_999]]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 가진 물건에서 출발하는 위상 정렬 — 모자란 재료의 수를 줄여 간다.
fun craftableItems(n: Int, recipes: IntArray, supplies: IntArray): IntArray {
    val m = recipes.size / 2
    val head = IntArray(n) { -1 }
    val next = IntArray(m)
    val to = IntArray(m)
    val missing = IntArray(n)
    for (e in 0 until m) {
        val a = recipes[2 * e]; val b = recipes[2 * e + 1]
        to[e] = b; next[e] = head[a]; head[a] = e
        missing[b] += 1
    }
    val have = BooleanArray(n)
    val supplied = BooleanArray(n)
    val queue = IntArray(n)
    var tail = 0
    for (s in supplies) { have[s] = true; supplied[s] = true; queue[tail] = s; tail += 1 }
    var front = 0
    while (front < tail) {
        val x = queue[front]
        front += 1
        Drill.dequeue(x)
        var e = head[x]
        while (e != -1) {
            val p = to[e]
            missing[p] -= 1
            Drill.write(p, missing[p])
            if (missing[p] == 0 && !have[p]) { have[p] = true; queue[tail] = p; tail += 1 }
            e = next[e]
        }
    }
    var count = 0
    for (v in 0 until n) if (have[v] && !supplied[v]) count += 1
    val out = IntArray(count)
    var k = 0
    for (v in 0 until n) if (have[v] && !supplied[v]) { out[k] = v; k += 1 }
    return out
}
""",
    mutants=[
        ("raw-items-free", "WRONG_BRANCH",
         "재료가 없는 물건을 처음부터 가진 것으로 본다. 재료가 없으면 가진 것만 쓸 수 있다.",
         """
fun craftableItems(n: Int, recipes: IntArray, supplies: IntArray): IntArray {
    val users = Array(n) { ArrayList<Int>() }
    val missing = IntArray(n)
    for (i in recipes.indices step 2) { users[recipes[i]].add(recipes[i + 1]); missing[recipes[i + 1]] += 1 }
    val have = BooleanArray(n)
    val supplied = BooleanArray(n)
    for (s in supplies) supplied[s] = true
    val queue = java.util.ArrayDeque<Int>()
    for (v in 0 until n) if (supplied[v] || missing[v] == 0) { have[v] = true; queue.add(v) }
    while (queue.isNotEmpty()) {
        val x = queue.poll()
        for (p in users[x]) { missing[p] -= 1; if (missing[p] == 0 && !have[p]) { have[p] = true; queue.add(p) } }
    }
    return (0 until n).filter { have[it] && !supplied[it] }.toIntArray()
}
"""),
        ("any-ingredient", "WRONG_ALGORITHM",
         "재료 하나만 갖춰도 만들 수 있다고 본다. 재료를 모두 갖춰야 한다.",
         """
fun craftableItems(n: Int, recipes: IntArray, supplies: IntArray): IntArray {
    val users = Array(n) { ArrayList<Int>() }
    for (i in recipes.indices step 2) users[recipes[i]].add(recipes[i + 1])
    val have = BooleanArray(n)
    val supplied = BooleanArray(n)
    val queue = java.util.ArrayDeque<Int>()
    for (s in supplies) { have[s] = true; supplied[s] = true; queue.add(s) }
    while (queue.isNotEmpty()) {
        val x = queue.poll()
        for (p in users[x]) if (!have[p]) { have[p] = true; queue.add(p) }
    }
    return (0 until n).filter { have[it] && !supplied[it] }.toIntArray()
}
"""),
        ("craft-order", "WRONG_BRANCH",
         "만든 순서대로 내놓는다. 답은 번호 오름차순이다.",
         """
fun craftableItems(n: Int, recipes: IntArray, supplies: IntArray): IntArray {
    val users = Array(n) { ArrayList<Int>() }
    val missing = IntArray(n)
    for (i in recipes.indices step 2) { users[recipes[i]].add(recipes[i + 1]); missing[recipes[i + 1]] += 1 }
    val have = BooleanArray(n)
    val queue = java.util.ArrayDeque<Int>()
    for (s in supplies) { have[s] = true; queue.add(s) }
    val made = ArrayList<Int>()
    while (queue.isNotEmpty()) {
        val x = queue.poll()
        for (p in users[x]) { missing[p] -= 1; if (missing[p] == 0 && !have[p]) { have[p] = true; made.add(p); queue.add(p) } }
    }
    return made.toIntArray()
}
"""),
        ("sweeps-until-stable", "PERFORMANCE",
         "더 만들 것이 없을 때까지 모든 물건을 처음부터 다시 훑는다. 한 바퀴에 하나씩만 늘면 물건 수만큼 돈다.",
         """
fun craftableItems(n: Int, recipes: IntArray, supplies: IntArray): IntArray {
    val ingredients = Array(n) { ArrayList<Int>() }
    for (i in recipes.indices step 2) ingredients[recipes[i + 1]].add(recipes[i])
    val have = BooleanArray(n)
    val supplied = BooleanArray(n)
    for (s in supplies) { have[s] = true; supplied[s] = true }
    var changed = true
    while (changed) {
        changed = false
        for (p in 0 until n) {
            if (have[p] || ingredients[p].isEmpty()) continue
            var ready = true
            for (a in ingredients[p]) { Drill.compare(p, a); if (!have[a]) { ready = false; break } }
            if (ready) { have[p] = true; changed = true }
        }
    }
    return (0 until n).filter { have[it] && !supplied[it] }.toIntArray()
}
"""),
    ],
))


def _weighted_graph(n, m, salt, low=1, high=100):
    """무방향 가중 간선 m 개 — [a, b, w, ...]. 같은 쌍이 여러 번 나올 수 있다."""
    pairs = _random_graph(n, m, salt)
    w = randoms(m, low, high, salt=salt + 2)
    return flat([pairs[2 * i], pairs[2 * i + 1], w[i]] for i in range(m))


# --- 203. 이웃이 가장 적은 도시 -----------------------------------------------------------------

def _city_fewest_neighbors(n, edges, threshold):
    inf = float("inf")
    dist = [[inf] * n for _ in range(n)]
    for i in range(n):
        dist[i][i] = 0
    for i in range(0, len(edges), 3):
        a, b, w = edges[i], edges[i + 1], edges[i + 2]
        if w < dist[a][b]:
            dist[a][b] = dist[b][a] = w
    for k in range(n):
        dk = dist[k]
        for i in range(n):
            di = dist[i]
            via = di[k]
            if via == inf:
                continue
            for j in range(n):
                if via + dk[j] < di[j]:
                    di[j] = via + dk[j]
    best, answer = None, -1
    for i in range(n):
        count = sum(1 for j in range(n) if j != i and dist[i][j] <= threshold)
        if best is None or count <= best:
            best, answer = count, i
    return answer


PROBLEMS.append(Problem(
    id="city-fewest-neighbors",
    title="이웃이 가장 적은 도시",
    summary="""
도시 `n` 개와 양방향 도로 `edges = [a1, b1, w1, ...]` 가 있다(`w` 는 길이). 도시 `i` 에서 **최단 거리가
`threshold` 이하**인 다른 도시를 `i` 의 이웃이라 한다. 이웃이 가장 적은 도시의 번호를 반환한다. 그런
도시가 여럿이면 **번호가 가장 큰** 것이다.
""",
    notes="""
모든 도시 쌍의 최단 거리가 필요하다. 도시가 100 개 이하라 플로이드–워셜(`n³`)이면 충분하다 — 다만 바깥
반복이 **거쳐 가는 도시 `k`** 여야 한다. `k` 를 안쪽에 두면 아직 확정되지 않은 거리를 이어 붙여 먼 쌍을 놓친다.
""",
    drill_doc="""
Drill.write(i * n + j, distance)   // i 에서 j 까지 더 짧은 길을 찾았다
""",
    constraints="""
- `2 <= n <= 100`, 도로 `0..n(n-1)/2` 개, `1 <= w <= 10_000`, `1 <= threshold <= 10_000`
- 같은 두 도시 사이에 도로가 여럿일 수 있다
""",
    signature=dict(name="cityFewestNeighbors", parameters=[("n", "INT"), ("edges", "INT_ARRAY"), ("threshold", "INT")], returns="INT"),
    groups=standard_groups(),
    reference=_city_fewest_neighbors,
    cases={
        "sample": [
            ("01", [4, [0, 1, 3, 1, 2, 1, 1, 3, 4, 2, 3, 1], 4]),
            ("02", [5, [0, 1, 2, 0, 4, 8, 1, 2, 3, 1, 4, 2, 2, 3, 1, 3, 4, 1], 2]),
        ],
        "boundary": [
            ("01-no-roads", [3, [], 5]),
            # 이웃 수가 모두 같으면 가장 큰 번호다.
            ("02-all-tied", [3, [0, 1, 1, 1, 2, 1, 0, 2, 1], 1]),
            # 거리가 문턱과 꼭 같으면 이웃이다.
            ("03-exactly-threshold", [3, [0, 1, 5, 1, 2, 5], 5]),
            # 직접 도로는 길고 돌아가는 길이 짧다.
            ("04-detour-shorter", [4, [0, 3, 100, 0, 1, 1, 1, 2, 1, 2, 3, 1], 3]),
            ("05-parallel-roads", [3, [0, 1, 9, 0, 1, 2, 1, 2, 9], 2]),
            # 큰 번호에서 작은 번호로 이어진 사슬 — 거쳐 가는 순서를 틀리면 먼 쌍을 놓친다.
            ("06-reverse-chain", [6, [5, 4, 1, 4, 3, 1, 3, 2, 1, 2, 1, 1, 1, 0, 1], 4]),
        ],
        "hidden": [
            ("01-random-small", [8, _weighted_graph(8, 12, salt=10093, low=1, high=10), 8]),
            ("02-random-medium", [40, _weighted_graph(40, 120, salt=10095, low=1, high=50), 60]),
            ("03-random-large", [100, _weighted_graph(100, 600, salt=10097, low=1, high=1000), 1500]),
            ("04-dense-short", [100, _weighted_graph(100, 3000, salt=10099, low=1, high=20), 15]),
            # 번호를 섞은 사슬 — 거쳐 가는 순서를 틀리면 먼 쌍을 놓친다.
            ("05-shuffled-chain", [60, flat([a, b, 1] for a, b in zip(shuffled(range(60), salt=10103), shuffled(range(60), salt=10103)[1:])), 20]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 플로이드–워셜 — 바깥이 거쳐 가는 도시다.
fun cityFewestNeighbors(n: Int, edges: IntArray, threshold: Int): Int {
    val inf = Int.MAX_VALUE / 2
    val dist = Array(n) { i -> IntArray(n) { j -> if (i == j) 0 else inf } }
    for (e in edges.indices step 3) {
        val a = edges[e]; val b = edges[e + 1]; val w = edges[e + 2]
        if (w < dist[a][b]) { dist[a][b] = w; dist[b][a] = w }
    }
    for (k in 0 until n) {
        for (i in 0 until n) {
            val via = dist[i][k]
            if (via == inf) continue
            for (j in 0 until n) {
                if (via + dist[k][j] < dist[i][j]) {
                    dist[i][j] = via + dist[k][j]
                    Drill.write(i * n + j, dist[i][j])
                }
            }
        }
    }
    var best = Int.MAX_VALUE
    var answer = -1
    for (i in 0 until n) {
        var count = 0
        for (j in 0 until n) if (j != i && dist[i][j] <= threshold) count += 1
        if (count <= best) { best = count; answer = i }
    }
    return answer
}
""",
    mutants=[
        ("smallest-id-on-tie", "WRONG_BRANCH",
         "이웃 수가 같으면 번호가 작은 도시를 고른다. 같으면 가장 큰 번호다.",
         """
fun cityFewestNeighbors(n: Int, edges: IntArray, threshold: Int): Int {
    val inf = Int.MAX_VALUE / 2
    val dist = Array(n) { i -> IntArray(n) { j -> if (i == j) 0 else inf } }
    for (e in edges.indices step 3) { val a = edges[e]; val b = edges[e + 1]; val w = edges[e + 2]; if (w < dist[a][b]) { dist[a][b] = w; dist[b][a] = w } }
    for (k in 0 until n) for (i in 0 until n) for (j in 0 until n) if (dist[i][k] + dist[k][j] < dist[i][j]) dist[i][j] = dist[i][k] + dist[k][j]
    var best = Int.MAX_VALUE; var answer = -1
    for (i in 0 until n) {
        var count = 0
        for (j in 0 until n) if (j != i && dist[i][j] <= threshold) count += 1
        if (count < best) { best = count; answer = i }
    }
    return answer
}
"""),
        ("direct-roads-only", "WRONG_ALGORITHM",
         "도로 하나로 닿는 도시만 센다. 여러 도로를 이어 문턱 안에 닿는 도시도 이웃이다.",
         """
fun cityFewestNeighbors(n: Int, edges: IntArray, threshold: Int): Int {
    val near = Array(n) { BooleanArray(n) }
    for (e in edges.indices step 3) if (edges[e + 2] <= threshold) { near[edges[e]][edges[e + 1]] = true; near[edges[e + 1]][edges[e]] = true }
    var best = Int.MAX_VALUE; var answer = -1
    for (i in 0 until n) {
        val count = near[i].count { it }
        if (count <= best) { best = count; answer = i }
    }
    return answer
}
"""),
        ("via-innermost", "WRONG_ALGORITHM",
         "거쳐 가는 도시를 가장 안쪽 반복에 둔다. 아직 확정되지 않은 거리를 이어 붙여 먼 쌍을 놓친다.",
         """
fun cityFewestNeighbors(n: Int, edges: IntArray, threshold: Int): Int {
    val inf = Int.MAX_VALUE / 2
    val dist = Array(n) { i -> IntArray(n) { j -> if (i == j) 0 else inf } }
    for (e in edges.indices step 3) { val a = edges[e]; val b = edges[e + 1]; val w = edges[e + 2]; if (w < dist[a][b]) { dist[a][b] = w; dist[b][a] = w } }
    for (i in 0 until n) for (j in 0 until n) for (k in 0 until n) if (dist[i][k] + dist[k][j] < dist[i][j]) dist[i][j] = dist[i][k] + dist[k][j]
    var best = Int.MAX_VALUE; var answer = -1
    for (i in 0 until n) {
        var count = 0
        for (j in 0 until n) if (j != i && dist[i][j] <= threshold) count += 1
        if (count <= best) { best = count; answer = i }
    }
    return answer
}
"""),
        ("strict-threshold", "OFF_BY_ONE",
         "거리가 문턱보다 작을 때만 이웃으로 센다. 문턱과 같아도 이웃이다.",
         """
fun cityFewestNeighbors(n: Int, edges: IntArray, threshold: Int): Int {
    val inf = Int.MAX_VALUE / 2
    val dist = Array(n) { i -> IntArray(n) { j -> if (i == j) 0 else inf } }
    for (e in edges.indices step 3) { val a = edges[e]; val b = edges[e + 1]; val w = edges[e + 2]; if (w < dist[a][b]) { dist[a][b] = w; dist[b][a] = w } }
    for (k in 0 until n) for (i in 0 until n) for (j in 0 until n) if (dist[i][k] + dist[k][j] < dist[i][j]) dist[i][j] = dist[i][k] + dist[k][j]
    var best = Int.MAX_VALUE; var answer = -1
    for (i in 0 until n) {
        var count = 0
        for (j in 0 until n) if (j != i && dist[i][j] < threshold) count += 1
        if (count <= best) { best = count; answer = i }
    }
    return answer
}
"""),
    ],
))


# --- 204. 두 번째로 짧은 길 --------------------------------------------------------------------

def _second_shortest(n, edges):
    import heapq
    adj = [[] for _ in range(n)]
    for i in range(0, len(edges), 3):
        a, b, w = edges[i], edges[i + 1], edges[i + 2]
        adj[a].append((b, w))
        adj[b].append((a, w))
    inf = float("inf")
    first = [inf] * n
    second = [inf] * n
    first[0] = 0
    heap = [(0, 0)]
    while heap:
        d, v = heapq.heappop(heap)
        if d > second[v]:
            continue
        for u, w in adj[v]:
            nd = d + w
            if nd < first[u]:
                first[u], nd = nd, first[u]
                heapq.heappush(heap, (first[u], u))
            if first[u] < nd < second[u]:
                second[u] = nd
                heapq.heappush(heap, (nd, u))
    return second[n - 1] if second[n - 1] < inf else -1


PROBLEMS.append(Problem(
    id="second-shortest-path",
    title="두 번째로 짧은 길",
    summary="""
정점 `n` 개와 무방향 간선 `edges = [a1, b1, w1, ...]` 가 있다. 정점 `0` 에서 `n-1` 로 가는 길 가운데 길이가
**가장 짧은 길이보다 엄격히 긴 것 중 가장 짧은 길이**를 반환한다. 길은 같은 정점과 간선을 몇 번이고 다시
지나도 된다. 그런 길이 없으면 `-1` 이다.
""",
    notes="""
정점마다 거리를 **둘** 들고 다닌다 — 가장 짧은 것과, 그보다 엄격히 긴 것 중 가장 짧은 것. 꺼낸 거리에서
나아간 새 거리가 첫째보다 짧으면 첫째를 밀어내 둘째로 내리고, 첫째와 둘째 사이면 둘째가 된다. 같은 정점을
두 번 꺼내야 하므로 "한 번 꺼내면 닫는다"를 쓰면 안 된다. 같은 간선을 되돌아오는 길도 길이다.
""",
    drill_doc="""
Drill.write(v, distance)   // v 의 첫째 또는 둘째 거리가 줄었다
""",
    constraints="""
- `1 <= n <= 20_000`, 간선 `0..100_000` 개, `1 <= w <= 10_000`
- 같은 쌍의 간선이 여럿일 수 있고, 자기 자신으로 가는 간선은 없다
""",
    signature=dict(name="secondShortestPath", parameters=[("n", "INT"), ("edges", "INT_ARRAY")], returns="INT"),
    groups=standard_groups(),
    reference=_second_shortest,
    cases={
        "sample": [
            ("01", [4, [0, 1, 1, 1, 3, 1, 0, 2, 1, 2, 3, 2]]),
            ("02", [2, [0, 1, 3]]),
        ],
        "boundary": [
            ("01-single-vertex", [1, []]),
            # 이웃을 다녀오면 둘째 길이다.
            ("02-single-vertex-with-edge", [2, [0, 1, 4]]),
            ("03-unreachable", [3, [0, 1, 2]]),
            # 가장 짧은 길이 둘이면 그 길이는 둘째가 아니다.
            ("04-two-shortest", [4, [0, 1, 1, 1, 3, 1, 0, 2, 1, 2, 3, 1]]),
            # 되돌아가는 것이 다른 길보다 짧다.
            ("05-back-and-forth-wins", [3, [0, 1, 1, 1, 2, 1, 0, 2, 10]]),
            ("06-parallel-edges", [2, [0, 1, 5, 0, 1, 6]]),
            ("07-parallel-equal-edges", [2, [0, 1, 5, 0, 1, 5]]),
            # 둘째가 지나는 정점은 첫째를 지나는 정점과 같다 — 같은 정점을 두 번 꺼내야 한다.
            ("08-second-through-same-vertex", [5, [0, 1, 1, 0, 2, 2, 1, 3, 1, 2, 3, 1, 3, 4, 10]]),
        ],
        "hidden": [
            ("01-random-small", [10, _weighted_graph(10, 20, salt=10105, low=1, high=5)]),
            ("02-random-medium", [500, _weighted_graph(500, 2000, salt=10107, low=1, high=100)]),
            ("03-random-large", [20_000, _weighted_graph(20_000, 100_000, salt=10109, low=1, high=10_000)]),
            ("04-path-graph", [1000, flat([i, i + 1, 7] for i in range(999))]),
            ("05-equal-weights", [2000, _weighted_graph(2000, 8000, salt=10111, low=3, high=3)]),
            ("06-grid-like", [900, flat([r * 30 + c, r * 30 + c + 1, 1] for r in range(30) for c in range(29)) + flat([r * 30 + c, (r + 1) * 30 + c, 1] for r in range(29) for c in range(30))]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 정점마다 거리 둘을 들고 다니는 다익스트라.
fun secondShortestPath(n: Int, edges: IntArray): Int {
    val m = edges.size / 3
    val head = IntArray(n) { -1 }
    val next = IntArray(2 * m)
    val to = IntArray(2 * m)
    val weight = IntArray(2 * m)
    var count = 0
    for (e in 0 until m) {
        val a = edges[3 * e]; val b = edges[3 * e + 1]; val w = edges[3 * e + 2]
        to[count] = b; weight[count] = w; next[count] = head[a]; head[a] = count; count += 1
        to[count] = a; weight[count] = w; next[count] = head[b]; head[b] = count; count += 1
    }
    val inf = Long.MAX_VALUE / 4
    val first = LongArray(n) { inf }
    val second = LongArray(n) { inf }
    first[0] = 0
    val heap = java.util.PriorityQueue<LongArray>(compareBy { it[0] })
    heap.add(longArrayOf(0, 0))
    while (heap.isNotEmpty()) {
        val top = heap.poll()
        val d = top[0]
        val v = top[1].toInt()
        if (d > second[v]) continue
        var e = head[v]
        while (e != -1) {
            val u = to[e]
            var candidate = d + weight[e]
            if (candidate < first[u]) {
                val pushed = first[u]
                first[u] = candidate
                heap.add(longArrayOf(candidate, u.toLong()))
                Drill.write(u, candidate.toInt())
                candidate = pushed
            }
            if (candidate > first[u] && candidate < second[u]) {
                second[u] = candidate
                heap.add(longArrayOf(candidate, u.toLong()))
                Drill.write(u, candidate.toInt())
            }
            e = next[e]
        }
    }
    return if (second[n - 1] >= inf) -1 else second[n - 1].toInt()
}
""",
    mutants=[
        ("equal-counts-as-second", "WRONG_BRANCH",
         "가장 짧은 길과 길이가 같은 다른 길을 둘째로 센다. 둘째는 엄격히 길어야 한다.",
         """
fun secondShortestPath(n: Int, edges: IntArray): Int {
    val adj = Array(n) { ArrayList<IntArray>() }
    for (i in edges.indices step 3) { adj[edges[i]].add(intArrayOf(edges[i + 1], edges[i + 2])); adj[edges[i + 1]].add(intArrayOf(edges[i], edges[i + 2])) }
    val inf = Long.MAX_VALUE / 4
    val first = LongArray(n) { inf }; val second = LongArray(n) { inf }
    first[0] = 0
    val heap = java.util.PriorityQueue<LongArray>(compareBy { it[0] })
    heap.add(longArrayOf(0, 0))
    while (heap.isNotEmpty()) {
        val top = heap.poll(); val d = top[0]; val v = top[1].toInt()
        if (d > second[v]) continue
        for (e in adj[v]) {
            val u = e[0]; var candidate = d + e[1]
            if (candidate < first[u]) { val pushed = first[u]; first[u] = candidate; heap.add(longArrayOf(candidate, u.toLong())); candidate = pushed }
            else if (candidate == first[u] && second[u] > candidate) { second[u] = candidate; heap.add(longArrayOf(candidate, u.toLong())); continue }
            if (candidate > first[u] && candidate < second[u]) { second[u] = candidate; heap.add(longArrayOf(candidate, u.toLong())) }
        }
    }
    return if (second[n - 1] >= inf) -1 else second[n - 1].toInt()
}
"""),
        ("closes-after-first-pop", "WRONG_BRANCH",
         "정점을 한 번 꺼내면 닫는다. 둘째 거리는 그 정점을 두 번째로 꺼낼 때 퍼진다.",
         """
fun secondShortestPath(n: Int, edges: IntArray): Int {
    val adj = Array(n) { ArrayList<IntArray>() }
    for (i in edges.indices step 3) { adj[edges[i]].add(intArrayOf(edges[i + 1], edges[i + 2])); adj[edges[i + 1]].add(intArrayOf(edges[i], edges[i + 2])) }
    val inf = Long.MAX_VALUE / 4
    val first = LongArray(n) { inf }; val second = LongArray(n) { inf }
    val done = BooleanArray(n)
    first[0] = 0
    val heap = java.util.PriorityQueue<LongArray>(compareBy { it[0] })
    heap.add(longArrayOf(0, 0))
    while (heap.isNotEmpty()) {
        val top = heap.poll(); val d = top[0]; val v = top[1].toInt()
        if (done[v]) continue
        done[v] = true
        for (e in adj[v]) {
            val u = e[0]; var candidate = d + e[1]
            if (candidate < first[u]) { val pushed = first[u]; first[u] = candidate; heap.add(longArrayOf(candidate, u.toLong())); candidate = pushed }
            if (candidate > first[u] && candidate < second[u]) second[u] = candidate
            val alt = second[v] + e[1]
            if (alt > first[u] && alt < second[u]) second[u] = alt
        }
    }
    return if (second[n - 1] >= inf) -1 else second[n - 1].toInt()
}
"""),
        ("simple-detours-only", "WRONG_ALGORITHM",
         "가장 짧은 길의 간선을 하나씩 빼고 다시 구한다. 같은 간선을 되돌아오는 길을 놓친다.",
         """
fun secondShortestPath(n: Int, edges: IntArray): Int {
    val m = edges.size / 3
    val inf = Long.MAX_VALUE / 4
    fun dijkstra(banned: Int, parent: IntArray?): LongArray {
        val adj = Array(n) { ArrayList<IntArray>() }
        for (e in 0 until m) if (e != banned) { adj[edges[3 * e]].add(intArrayOf(edges[3 * e + 1], edges[3 * e + 2], e)); adj[edges[3 * e + 1]].add(intArrayOf(edges[3 * e], edges[3 * e + 2], e)) }
        val dist = LongArray(n) { inf }
        dist[0] = 0
        val heap = java.util.PriorityQueue<LongArray>(compareBy { it[0] })
        heap.add(longArrayOf(0, 0))
        while (heap.isNotEmpty()) {
            val top = heap.poll(); val v = top[1].toInt()
            if (top[0] > dist[v]) continue
            for (e in adj[v]) if (dist[v] + e[1] < dist[e[0]]) { dist[e[0]] = dist[v] + e[1]; parent?.set(e[0], e[2]); heap.add(longArrayOf(dist[e[0]], e[0].toLong())) }
        }
        return dist
    }
    val parent = IntArray(n) { -1 }
    val base = dijkstra(-1, parent)
    if (base[n - 1] >= inf) return -1
    var best = inf
    var v = n - 1
    while (v != 0) {
        val e = parent[v]
        val d = dijkstra(e, null)[n - 1]
        if (d > base[n - 1] && d < best) best = d
        v = if (edges[3 * e] == v) edges[3 * e + 1] else edges[3 * e]
    }
    return if (best >= inf) -1 else best.toInt()
}
"""),
    ],
))


# --- 205. 가장 작은 글자열로 바꾸기 ------------------------------------------------------------

def _smallest_with_swaps(s, pairs):
    n = len(s)
    parent = list(range(n))

    def find(x):
        while parent[x] != x:
            parent[x] = parent[parent[x]]
            x = parent[x]
        return x

    for i in range(0, len(pairs), 2):
        a, b = find(pairs[i]), find(pairs[i + 1])
        if a != b:
            parent[a] = b
    groups = {}
    for i in range(n):
        groups.setdefault(find(i), []).append(i)
    out = list(s)
    for idx in groups.values():
        for i, ch in zip(idx, sorted(s[i] for i in idx)):
            out[i] = ch
    return "".join(out)


def _letters(n, letters, salt):
    import random
    from author import SEED
    source = random.Random(SEED + salt)
    return "".join(source.choice(letters) for _ in range(n))


PROBLEMS.append(Problem(
    id="smallest-string-with-swaps",
    title="바꿔서 만드는 가장 앞선 글자열",
    summary="""
소문자 글자열 `s` 와 자리 쌍 `pairs = [a1, b1, a2, b2, ...]` 가 주어진다. 쌍 `(a, b)` 는 `s[a]` 와 `s[b]` 를
**몇 번이든** 맞바꿀 수 있다는 뜻이다. 맞바꾸기를 원하는 만큼 해서 만들 수 있는 글자열 중 **사전순으로 가장
앞선 것**을 반환한다.
""",
    notes="""
`(a, b)` 와 `(b, c)` 를 쓸 수 있으면 `a` 와 `c` 도 사실상 맞바꿀 수 있다 — 맞바꿀 수 있는 자리는 **무리**를
이루고, 한 무리 안에서는 글자를 어떤 순서로든 늘어놓을 수 있다. 무리를 유니온 파인드로 묶고, 무리마다 글자를
정렬해 그 무리의 자리를 앞에서부터 채운다.
""",
    drill_doc="""
Drill.edge("a", "b")   // 두 자리를 한 무리로 묶었다
Drill.write(i, ch)     // 자리 i 에 글자를 놓았다
""",
    constraints="""
- `1 <= s.length <= 100_000`, 쌍 `0..100_000` 개, `0 <= a, b < s.length`
- `a == b` 인 쌍, 같은 쌍의 반복이 있을 수 있다
""",
    signature=dict(name="smallestStringWithSwaps", parameters=[("s", "STRING"), ("pairs", "INT_ARRAY")], returns="STRING"),
    groups=perf_groups(),
    reference=_smallest_with_swaps,
    limits={"timeMillis": 2000, "memoryMb": 256, "outputBytes": 1_000_000},
    cases={
        "sample": [
            ("01", ["dcab", [0, 3, 1, 2]]),
            ("02", ["dcab", [0, 3, 1, 2, 0, 2]]),
        ],
        "boundary": [
            ("01-no-pairs", ["zyx", []]),
            ("02-single-letter", ["q", [0, 0]]),
            # 맞바꾸기를 이어 붙여야 닿는다.
            ("03-chain", ["cba", [0, 1, 1, 2]]),
            # 한 번 바꾸면 손해처럼 보여도 끝은 더 앞선다.
            ("04-needs-worse-step", ["bca", [0, 1, 1, 2]]),
            ("05-two-groups", ["dcbaz", [0, 2, 1, 3]]),
            ("06-repeated-letters", ["bbaa", [0, 3, 1, 2]]),
            # 무리의 자리가 흩어져 있다 — 정렬된 글자를 정렬된 자리에 놓는다.
            ("07-scattered-group", ["zaybxc", [4, 0, 2, 4]]),
            # 이미 무리에 붙은 자리를 다시 다른 자리에 잇는다 — 뿌리끼리 이어야 무리가 지켜진다.
            ("08-relink", ["dcba", [0, 1, 0, 2, 0, 3]]),
        ],
        "hidden": [
            ("01-random-small", [_letters(12, "abcde", salt=10113), _random_graph(12, 6, salt=10115)]),
            ("02-random-medium", [_letters(1000, "abcdefghij", salt=10117), _random_graph(1000, 400, salt=10119)]),
            ("03-random-large", [_letters(50_000, "abcdefghijklmnopqrstuvwxyz", salt=10121), _random_graph(50_000, 30_000, salt=10123)]),
            ("04-all-connected", [_letters(2000, "zyxw", salt=10125), flat([i, i + 1] for i in range(1999))]),
            ("05-self-pairs", ["edcba", [0, 0, 1, 1, 2, 2]]),
        ],
        "performance": [
            # 한 무리가 십만 자리다 — 자리마다 무리를 다시 찾는 풀이가 끝나지 않는다.
            ("01-one-group", [_letters(100_000, "abcdefghijklmnopqrstuvwxyz", salt=10127), flat([i, i + 1] for i in range(99_999))]),
            ("02-one-group-shuffled", [_letters(100_000, "abc", salt=10129), _relabel(100_000, flat([i, i + 1] for i in range(99_999)), salt=10131)]),
            ("03-random-large", [_letters(100_000, "abcdefghijklmnopqrstuvwxyz", salt=10133), _random_graph(100_000, 100_000, salt=10135)]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 무리를 유니온 파인드로 묶고, 무리마다 글자를 세어 자리 순서대로 채운다.
fun smallestStringWithSwaps(s: String, pairs: IntArray): String {
    val n = s.length
    val parent = IntArray(n) { it }
    fun find(x: Int): Int {
        var r = x
        while (parent[r] != r) { parent[r] = parent[parent[r]]; r = parent[r] }
        return r
    }
    for (i in pairs.indices step 2) {
        val a = find(pairs[i]); val b = find(pairs[i + 1])
        if (a != b) {
            parent[a] = b
            Drill.edge(a.toString(), b.toString())
        }
    }
    val counts = HashMap<Int, IntArray>()
    for (i in 0 until n) counts.getOrPut(find(i)) { IntArray(26) }[s[i] - 'a'] += 1
    val out = CharArray(n)
    for (i in 0 until n) {
        val c = counts.getValue(find(i))
        var k = 0
        while (c[k] == 0) k += 1
        c[k] -= 1
        out[i] = 'a' + k
        Drill.write(i, k)
    }
    return String(out)
}
""",
    mutants=[
        ("swaps-each-pair-once", "WRONG_ALGORITHM",
         "쌍마다 한 번씩, 나아질 때만 맞바꾼다. 맞바꾸기를 이어 붙여야 닿는 자리가 있다.",
         """
fun smallestStringWithSwaps(s: String, pairs: IntArray): String {
    val out = s.toCharArray()
    for (i in pairs.indices step 2) {
        val a = minOf(pairs[i], pairs[i + 1]); val b = maxOf(pairs[i], pairs[i + 1])
        if (out[b] < out[a]) { val t = out[a]; out[a] = out[b]; out[b] = t }
    }
    return String(out)
}
"""),
        ("links-slots-not-roots", "WRONG_BRANCH",
         "뿌리가 아니라 자리 자체를 잇는다. 이미 다른 자리에 붙어 있던 자리의 옛 연결이 끊긴다.",
         """
fun smallestStringWithSwaps(s: String, pairs: IntArray): String {
    val n = s.length
    val parent = IntArray(n) { it }
    fun find(x: Int): Int { var r = x; while (parent[r] != r) r = parent[r]; return r }
    for (i in pairs.indices step 2) if (find(pairs[i]) != find(pairs[i + 1])) parent[pairs[i]] = pairs[i + 1]
    val counts = HashMap<Int, IntArray>()
    for (i in 0 until n) counts.getOrPut(find(i)) { IntArray(26) }[s[i] - 'a'] += 1
    val out = CharArray(n)
    for (i in 0 until n) {
        val c = counts.getValue(find(i))
        var k = 0
        while (c[k] == 0) k += 1
        c[k] -= 1
        out[i] = 'a' + k
    }
    return String(out)
}
"""),
        ("discovery-order-slots", "WRONG_BRANCH",
         "글자는 정렬하지만 자리는 무리를 훑은 순서대로 채운다. 가장 앞선 글자는 가장 앞 자리에 가야 한다.",
         """
fun smallestStringWithSwaps(s: String, pairs: IntArray): String {
    val n = s.length
    val adj = Array(n) { ArrayList<Int>() }
    for (i in pairs.indices step 2) { adj[pairs[i]].add(pairs[i + 1]); adj[pairs[i + 1]].add(pairs[i]) }
    val seen = BooleanArray(n)
    val out = CharArray(n)
    for (start in 0 until n) {
        if (seen[start]) continue
        val slots = ArrayList<Int>()
        val stack = java.util.ArrayDeque<Int>()
        stack.push(start); seen[start] = true
        while (stack.isNotEmpty()) {
            val v = stack.pop(); slots.add(v)
            for (u in adj[v]) if (!seen[u]) { seen[u] = true; stack.push(u) }
        }
        val letters = slots.map { s[it] }.sorted()
        for (k in slots.indices) out[slots[k]] = letters[k]
    }
    return String(out)
}
"""),
        ("regroups-per-slot", "PERFORMANCE",
         "자리마다 그 자리가 속한 무리를 처음부터 다시 찾는다. 무리가 크면 자리 수의 제곱이 된다.",
         """
fun smallestStringWithSwaps(s: String, pairs: IntArray): String {
    val n = s.length
    val adj = Array(n) { ArrayList<Int>() }
    for (i in pairs.indices step 2) { adj[pairs[i]].add(pairs[i + 1]); adj[pairs[i + 1]].add(pairs[i]) }
    val out = CharArray(n)
    val mark = IntArray(n) { -1 }
    for (i in 0 until n) {
        val slots = ArrayList<Int>()
        val stack = java.util.ArrayDeque<Int>()
        stack.push(i); mark[i] = i
        while (stack.isNotEmpty()) {
            val v = stack.pop(); slots.add(v)
            for (u in adj[v]) if (mark[u] != i) { mark[u] = i; stack.push(u) }
        }
        slots.sort()
        val letters = slots.map { s[it] }.sorted()
        out[i] = letters[slots.binarySearch(i)]
    }
    return String(out)
}
"""),
    ],
))


# --- 206. 무게 제한이 있는 길 묻기 -------------------------------------------------------------

def _limited_path_queries(n, edges, queries):
    parent = list(range(n))

    def find(x):
        while parent[x] != x:
            parent[x] = parent[parent[x]]
            x = parent[x]
        return x

    order = sorted(range(len(edges) // 3), key=lambda e: edges[3 * e + 2])
    asked = sorted(range(len(queries) // 3), key=lambda q: queries[3 * q + 2])
    out = [0] * (len(queries) // 3)
    k = 0
    for q in asked:
        p, t, limit = queries[3 * q], queries[3 * q + 1], queries[3 * q + 2]
        while k < len(order) and edges[3 * order[k] + 2] < limit:
            e = order[k]
            a, b = find(edges[3 * e]), find(edges[3 * e + 1])
            if a != b:
                parent[a] = b
            k += 1
        out[q] = 1 if find(p) == find(t) else 0
    return out


def _queries(n, q, salt, low, high):
    a = randoms(q, 0, n - 1, salt=salt)
    b = randoms(q, 0, n - 1, salt=salt + 1)
    limit = randoms(q, low, high, salt=salt + 2)
    return flat([a[i], b[i], limit[i]] for i in range(q))


PROBLEMS.append(Problem(
    id="limited-path-queries",
    title="무게 제한이 있는 길 묻기",
    summary="""
정점 `n` 개와 무방향 간선 `edges = [a1, b1, w1, ...]` 가 있다. 질문 `queries = [p1, q1, limit1, ...]` 마다
`p` 에서 `q` 로 가는 길 중 **모든 간선의 무게가 `limit` 보다 엄격히 작은** 길이 있으면 `1`, 없으면 `0` 을
질문 순서대로 담아 반환한다.
""",
    notes="""
질문마다 길을 찾으면 질문 수 × 그래프 크기다. 대신 **질문을 `limit` 순으로 정렬**해 두고, 간선도 무게 순으로
정렬해 지금 질문의 `limit` 보다 가벼운 간선만 유니온 파인드에 더해 가면, 질문마다 "같은 무리인가" 한 번으로
답한다. 답은 원래 질문 순서로 돌려놓는다.
""",
    drill_doc="""
Drill.edge("a", "b")    // 무게가 limit 보다 작은 간선을 더했다
Drill.match(p, q)       // 질문 하나에 답했다
""",
    constraints="""
- `2 <= n <= 100_000`, 간선 `0..100_000` 개, 질문 `1..100_000` 개
- `1 <= w, limit <= 1_000_000_000`, 같은 쌍의 간선이 여럿일 수 있다. `p == q` 인 질문은 언제나 `1` 이다
""",
    signature=dict(name="limitedPathQueries", parameters=[("n", "INT"), ("edges", "INT_ARRAY"), ("queries", "INT_ARRAY")], returns="INT_ARRAY"),
    groups=perf_groups(),
    reference=_limited_path_queries,
    limits={"timeMillis": 2000, "memoryMb": 256, "outputBytes": 1_000_000},
    cases={
        "sample": [
            ("01", [3, [0, 1, 2, 1, 2, 4, 2, 0, 8, 1, 0, 16], [0, 1, 2, 0, 2, 5]]),
            ("02", [5, [0, 1, 10, 1, 2, 5, 2, 3, 9, 3, 4, 13], [0, 4, 14, 1, 4, 13]]),
        ],
        "boundary": [
            ("01-no-edges", [2, [], [0, 1, 5, 0, 0, 1]]),
            # 무게가 limit 과 같으면 쓸 수 없다.
            ("02-weight-equals-limit", [2, [0, 1, 5], [0, 1, 5, 0, 1, 6]]),
            # 질문의 limit 이 거꾸로 온다 — 답은 질문 순서대로.
            ("03-descending-limits", [3, [0, 1, 1, 1, 2, 10], [0, 2, 11, 0, 2, 5, 0, 1, 2]]),
            # 직접 이어진 간선은 무겁지만 돌아가는 길은 가볍다.
            ("04-detour-light", [3, [0, 2, 100, 0, 1, 1, 1, 2, 1], [0, 2, 2]]),
            ("05-parallel-edges", [2, [0, 1, 9, 0, 1, 3], [0, 1, 4]]),
            ("06-max-weights", [3, [0, 1, 1_000_000_000, 1, 2, 999_999_999], [0, 2, 1_000_000_000, 0, 1, 1_000_000_000]]),
        ],
        "hidden": [
            ("01-random-small", [10, _weighted_graph(10, 15, salt=10137, low=1, high=20), _queries(10, 20, salt=10139, low=1, high=25)]),
            ("02-random-medium", [500, _weighted_graph(500, 800, salt=10141, low=1, high=1000), _queries(500, 1000, salt=10143, low=1, high=1200)]),
            ("03-random-large", [50_000, _weighted_graph(50_000, 80_000, salt=10145, low=1, high=1_000_000_000), _queries(50_000, 50_000, salt=10147, low=1, high=1_000_000_000)]),
            ("04-same-limit", [100, _weighted_graph(100, 300, salt=10149, low=1, high=50), _queries(100, 300, salt=10151, low=25, high=25)]),
        ],
        "performance": [
            # 긴 사슬과 많은 질문 — 질문마다 길을 찾는 풀이는 사슬을 매번 끝까지 걷는다.
            ("01-long-chain", [100_000, flat([i, i + 1, 1] for i in range(99_999)), flat([0, 99_999, 2] for _ in range(100_000))]),
            ("02-random-large", [100_000, _weighted_graph(100_000, 100_000, salt=10153, low=1, high=1_000_000), _queries(100_000, 100_000, salt=10155, low=1, high=1_000_000)]),
            ("03-chain-unreachable", [100_000, flat([i, i + 1, 1] for i in range(99_998)), flat([0, 99_999, 2] for _ in range(100_000))]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 질문과 간선을 무게 순으로 정렬하고 가벼운 간선부터 유니온 파인드에 더한다.
fun limitedPathQueries(n: Int, edges: IntArray, queries: IntArray): IntArray {
    val parent = IntArray(n) { it }
    fun find(x: Int): Int {
        var r = x
        while (parent[r] != r) { parent[r] = parent[parent[r]]; r = parent[r] }
        return r
    }
    val m = edges.size / 3
    val q = queries.size / 3
    val order = (0 until m).sortedBy { edges[3 * it + 2] }
    val asked = (0 until q).sortedBy { queries[3 * it + 2] }
    val out = IntArray(q)
    var k = 0
    for (i in asked) {
        val limit = queries[3 * i + 2]
        while (k < m && edges[3 * order[k] + 2] < limit) {
            val e = order[k]
            val a = find(edges[3 * e]); val b = find(edges[3 * e + 1])
            if (a != b) {
                parent[a] = b
                Drill.edge(edges[3 * e].toString(), edges[3 * e + 1].toString())
            }
            k += 1
        }
        out[i] = if (find(queries[3 * i]) == find(queries[3 * i + 1])) 1 else 0
        Drill.match(queries[3 * i], queries[3 * i + 1])
    }
    return out
}
""",
    mutants=[
        ("inclusive-limit", "OFF_BY_ONE",
         "무게가 limit 과 같은 간선도 쓴다. 엄격히 작아야 한다.",
         """
fun limitedPathQueries(n: Int, edges: IntArray, queries: IntArray): IntArray {
    val parent = IntArray(n) { it }
    fun find(x: Int): Int { var r = x; while (parent[r] != r) { parent[r] = parent[parent[r]]; r = parent[r] }; return r }
    val m = edges.size / 3; val q = queries.size / 3
    val order = (0 until m).sortedBy { edges[3 * it + 2] }
    val asked = (0 until q).sortedBy { queries[3 * it + 2] }
    val out = IntArray(q)
    var k = 0
    for (i in asked) {
        while (k < m && edges[3 * order[k] + 2] <= queries[3 * i + 2]) { val e = order[k]; val a = find(edges[3 * e]); val b = find(edges[3 * e + 1]); if (a != b) parent[a] = b; k += 1 }
        out[i] = if (find(queries[3 * i]) == find(queries[3 * i + 1])) 1 else 0
    }
    return out
}
"""),
        ("answers-in-sorted-order", "WRONG_BRANCH",
         "답을 정렬한 질문의 순서대로 담는다. 답은 원래 질문 순서다.",
         """
fun limitedPathQueries(n: Int, edges: IntArray, queries: IntArray): IntArray {
    val parent = IntArray(n) { it }
    fun find(x: Int): Int { var r = x; while (parent[r] != r) { parent[r] = parent[parent[r]]; r = parent[r] }; return r }
    val m = edges.size / 3; val q = queries.size / 3
    val order = (0 until m).sortedBy { edges[3 * it + 2] }
    val asked = (0 until q).sortedBy { queries[3 * it + 2] }
    val out = IntArray(q)
    var k = 0
    for ((slot, i) in asked.withIndex()) {
        while (k < m && edges[3 * order[k] + 2] < queries[3 * i + 2]) { val e = order[k]; val a = find(edges[3 * e]); val b = find(edges[3 * e + 1]); if (a != b) parent[a] = b; k += 1 }
        out[slot] = if (find(queries[3 * i]) == find(queries[3 * i + 1])) 1 else 0
    }
    return out
}
"""),
        ("direct-edge-only", "WRONG_ALGORITHM",
         "두 정점을 바로 잇는 간선만 본다. 여러 간선을 이어 가는 길도 길이다.",
         """
fun limitedPathQueries(n: Int, edges: IntArray, queries: IntArray): IntArray {
    val lightest = HashMap<Long, Int>()
    for (e in edges.indices step 3) {
        val a = minOf(edges[e], edges[e + 1]).toLong(); val b = maxOf(edges[e], edges[e + 1]).toLong()
        val key = a * 1_000_000 + b
        lightest[key] = minOf(lightest[key] ?: Int.MAX_VALUE, edges[e + 2])
    }
    val q = queries.size / 3
    return IntArray(q) { i ->
        val p = queries[3 * i]; val t = queries[3 * i + 1]
        if (p == t) 1 else {
            val w = lightest[minOf(p, t).toLong() * 1_000_000 + maxOf(p, t)]
            if (w != null && w < queries[3 * i + 2]) 1 else 0
        }
    }
}
"""),
        ("searches-per-query", "PERFORMANCE",
         "질문마다 가벼운 간선으로 너비 우선 탐색을 한다. 질문 수 × 그래프 크기다.",
         """
fun limitedPathQueries(n: Int, edges: IntArray, queries: IntArray): IntArray {
    val adj = Array(n) { ArrayList<IntArray>() }
    for (e in edges.indices step 3) { adj[edges[e]].add(intArrayOf(edges[e + 1], edges[e + 2])); adj[edges[e + 1]].add(intArrayOf(edges[e], edges[e + 2])) }
    val q = queries.size / 3
    val seen = IntArray(n) { -1 }
    val queue = IntArray(n)
    return IntArray(q) { i ->
        val p = queries[3 * i]; val t = queries[3 * i + 1]; val limit = queries[3 * i + 2]
        var head = 0; var tail = 0
        queue[tail++] = p; seen[p] = i
        var found = p == t
        while (head < tail && !found) {
            val v = queue[head++]
            for (e in adj[v]) {
                if (e[1] < limit && seen[e[0]] != i) {
                    Drill.compare(v, e[0])
                    if (e[0] == t) { found = true; break }
                    seen[e[0]] = i; queue[tail++] = e[0]
                }
            }
        }
        if (found) 1 else 0
    }
}
"""),
    ],
))


# --- 225. 좋은 경로의 수 -----------------------------------------------------------------------

def _good_paths(vals, edges):
    n = len(vals)
    parent = list(range(n))

    def find(x):
        while parent[x] != x:
            parent[x] = parent[parent[x]]
            x = parent[x]
        return x

    adj = [[] for _ in range(n)]
    for i in range(0, len(edges), 2):
        a, b = edges[i], edges[i + 1]
        adj[a].append(b)
        adj[b].append(a)
    by_value = {}
    for v in range(n):
        by_value.setdefault(vals[v], []).append(v)
    total = 0
    for value in sorted(by_value):
        group = by_value[value]
        for v in group:
            for u in adj[v]:
                if vals[u] <= value:
                    ru, rv = find(u), find(v)
                    if ru != rv:
                        parent[ru] = rv
        counts = {}
        for v in group:
            r = find(v)
            counts[r] = counts.get(r, 0) + 1
        for c in counts.values():
            total += c * (c + 1) // 2
    return total


def _tree_edges(n, salt):
    picks = randoms(n, 0, 10 ** 9, salt=salt)
    order = shuffled(range(n), salt=salt + 1)
    return flat([order[picks[i] % i], order[i]] for i in range(1, n))


PROBLEMS.append(Problem(
    id="good-paths",
    title="좋은 경로의 수",
    summary="""
정점 `0..n-1` 의 트리가 간선 `edges = [a1, b1, a2, b2, ...]` 로 주어지고, 정점 `i` 의 값은 `vals[i]` 다. **좋은
경로**는 양 끝 정점의 값이 같고, 경로 위의 모든 정점의 값이 그 값 이하인 경로다. 정점 하나도 좋은 경로다. 좋은
경로의 수를 반환한다 — `a` 에서 `b` 로 가는 것과 `b` 에서 `a` 로 가는 것은 같은 경로다.
""",
    notes="""
값이 작은 정점부터 차례로 그래프에 더해 간다고 하자. 값 `x` 의 정점들을 더할 때, 이웃 중 값이 `x` 이하인 정점(이미
더해진 정점)과 잇는다. 그러면 이 순간 같은 무리에 든 두 정점 사이의 경로는 모두 `x` 이하의 정점만 지난다 — 무리는
`x` 이하의 정점들로만 이어졌으니까. 그래서 값 `x` 의 정점들을 무리마다 세어 `c` 개면 `c(c+1)/2` 개의 좋은 경로가
있다(자기 자신 포함). **같은 값의 정점을 모두 이은 뒤에** 센다.
""",
    drill_doc="""
Drill.edge("a", "b")      // 두 무리를 이었다
Drill.write(value, c)     // 이 값의 정점이 한 무리에 c 개
""",
    constraints="""
- `1 <= n <= 30_000`, 간선은 `n - 1` 개(트리), `0 <= vals[i] <= 100_000`
""",
    signature=dict(name="goodPaths", parameters=[("vals", "INT_ARRAY"), ("edges", "INT_ARRAY")], returns="INT"),
    groups=perf_groups(time_multiplier=0.25),
    reference=_good_paths,
    cases={
        "sample": [
            ("01", [[1, 3, 2, 1, 3], [0, 1, 0, 2, 2, 3, 2, 4]]),
            ("02", [[1, 1, 2, 2, 3], [0, 1, 1, 2, 2, 3, 2, 4]]),
        ],
        "boundary": [
            ("01-single", [[7], []]),
            ("02-two-equal", [[5, 5], [0, 1]]),
            ("03-two-different", [[5, 6], [0, 1]]),
            # 사이에 더 큰 값이 끼면 좋은 경로가 아니다.
            ("04-blocked-by-larger", [[2, 9, 2], [0, 1, 1, 2]]),
            # 사이가 작으면 좋은 경로다.
            ("05-valley-between", [[4, 1, 4], [0, 1, 1, 2]]),
            # 같은 값이 셋 — 셋 다 이어지면 쌍이 셋이다.
            ("06-three-equal-star", [[3, 3, 3, 3], [0, 1, 0, 2, 0, 3]]),
            ("07-chain-of-equals", [[2, 2, 2, 2, 2], [0, 1, 1, 2, 2, 3, 3, 4]]),
        ],
        "hidden": [
            ("01-random-small", [randoms(12, 0, 3, salt=10343), _tree_edges(12, salt=10345)]),
            ("02-random-medium", [randoms(1000, 0, 20, salt=10347), _tree_edges(1000, salt=10349)]),
            ("03-all-equal", [[7] * 3000, _tree_edges(3000, salt=10351)]),
            ("04-distinct", [shuffled(range(5000), salt=10353), _tree_edges(5000, salt=10355)]),
        ],
        "performance": [
            # 정점마다 트리를 다 훑으면 3 만 × 3 만 = 9×10^8 걸음이다.
            ("01-random-large", [randoms(30_000, 0, 50, salt=10357), _tree_edges(30_000, salt=10359)]),
            ("02-all-equal-large", [[1] * 30_000, _tree_edges(30_000, salt=10361)]),
            ("03-chain-large", [randoms(30_000, 0, 5, salt=10363), flat([i, i + 1] for i in range(29_999))]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 값이 작은 정점부터 유니온 파인드에 더하고, 같은 값을 다 이은 뒤 무리마다 센다.
fun goodPaths(vals: IntArray, edges: IntArray): Int {
    val n = vals.size
    val parent = IntArray(n) { it }
    fun find(x: Int): Int {
        var r = x
        while (parent[r] != r) { parent[r] = parent[parent[r]]; r = parent[r] }
        return r
    }
    val adj = Array(n) { ArrayList<Int>() }
    for (i in edges.indices step 2) { adj[edges[i]].add(edges[i + 1]); adj[edges[i + 1]].add(edges[i]) }
    val order = (0 until n).sortedBy { vals[it] }
    var total = 0L
    var i = 0
    while (i < n) {
        var j = i
        while (j < n && vals[order[j]] == vals[order[i]]) j += 1
        val value = vals[order[i]]
        for (k in i until j) {
            val v = order[k]
            for (u in adj[v]) {
                if (vals[u] <= value) {
                    val ru = find(u); val rv = find(v)
                    if (ru != rv) { parent[ru] = rv; Drill.edge(u.toString(), v.toString()) }
                }
            }
        }
        val counts = HashMap<Int, Int>()
        for (k in i until j) { val r = find(order[k]); counts[r] = (counts[r] ?: 0) + 1 }
        for (c in counts.values) { total += c.toLong() * (c + 1) / 2; Drill.write(value, c) }
        i = j
    }
    return total.toInt()
}
""",
    mutants=[
        ("forgets-single-vertices", "OFF_BY_ONE",
         "무리마다 c(c-1)/2 로 쌍만 센다. 정점 하나도 좋은 경로다.",
         """
fun goodPaths(vals: IntArray, edges: IntArray): Int {
    val n = vals.size
    val parent = IntArray(n) { it }
    fun find(x: Int): Int { var r = x; while (parent[r] != r) { parent[r] = parent[parent[r]]; r = parent[r] }; return r }
    val adj = Array(n) { ArrayList<Int>() }
    for (i in edges.indices step 2) { adj[edges[i]].add(edges[i + 1]); adj[edges[i + 1]].add(edges[i]) }
    val order = (0 until n).sortedBy { vals[it] }
    var total = 0L
    var i = 0
    while (i < n) {
        var j = i
        while (j < n && vals[order[j]] == vals[order[i]]) j += 1
        val value = vals[order[i]]
        for (k in i until j) for (u in adj[order[k]]) if (vals[u] <= value) { val ru = find(u); val rv = find(order[k]); if (ru != rv) parent[ru] = rv }
        val counts = HashMap<Int, Int>()
        for (k in i until j) { val r = find(order[k]); counts[r] = (counts[r] ?: 0) + 1 }
        for (c in counts.values) total += c.toLong() * (c - 1) / 2
        i = j
    }
    return total.toInt()
}
"""),
        ("counts-while-joining", "WRONG_BRANCH",
         "같은 값의 정점을 하나 이을 때마다 센다. 그 값의 정점을 모두 이은 뒤에 세야 나중에 이어지는 쌍을 놓치지 않는다.",
         """
fun goodPaths(vals: IntArray, edges: IntArray): Int {
    val n = vals.size
    val parent = IntArray(n) { it }
    fun find(x: Int): Int { var r = x; while (parent[r] != r) { parent[r] = parent[parent[r]]; r = parent[r] }; return r }
    val adj = Array(n) { ArrayList<Int>() }
    for (i in edges.indices step 2) { adj[edges[i]].add(edges[i + 1]); adj[edges[i + 1]].add(edges[i]) }
    val order = (0 until n).sortedBy { vals[it] }
    var total = 0L
    val sameValueInRoot = HashMap<Int, Int>()
    var current = -1
    for (v in order) {
        if (vals[v] != current) { sameValueInRoot.clear(); current = vals[v] }
        for (u in adj[v]) if (vals[u] <= vals[v]) { val ru = find(u); val rv = find(v); if (ru != rv) parent[ru] = rv }
        val r = find(v)
        val before = sameValueInRoot[r] ?: 0
        total += before + 1
        sameValueInRoot[r] = before + 1
    }
    return total.toInt()
}
"""),
        ("ignores-larger-between", "WRONG_ALGORITHM",
         "트리 전체에서 같은 값의 쌍을 모두 센다. 사이에 더 큰 값이 있으면 좋은 경로가 아니다.",
         """
fun goodPaths(vals: IntArray, edges: IntArray): Int {
    val counts = HashMap<Int, Long>()
    for (v in vals) counts[v] = (counts[v] ?: 0L) + 1
    var total = 0L
    for (c in counts.values) total += c * (c + 1) / 2
    return total.toInt()
}
"""),
        ("walks-from-each-vertex", "PERFORMANCE",
         "정점마다 트리를 훑으며 지나온 최댓값을 들고 같은 값의 정점을 센다. 정점 수의 제곱이다.",
         """
fun goodPaths(vals: IntArray, edges: IntArray): Int {
    val n = vals.size
    val adj = Array(n) { ArrayList<Int>() }
    for (i in edges.indices step 2) { adj[edges[i]].add(edges[i + 1]); adj[edges[i + 1]].add(edges[i]) }
    var total = 0L
    val stack = IntArray(n); val from = IntArray(n)
    for (s in 0 until n) {
        total += 1
        var top = 0
        stack[top] = s; from[top] = -1; top += 1
        while (top > 0) {
            top -= 1
            val v = stack[top]; val p = from[top]
            for (u in adj[v]) {
                if (u == p || vals[u] > vals[s]) continue
                Drill.compare(s, u)
                if (u > s && vals[u] == vals[s]) total += 1
                stack[top] = u; from[top] = v; top += 1
            }
        }
    }
    return total.toInt()
}
"""),
    ],
))


# --- DAG 의 최단 거리 ---------------------------------------------------------

_UNREACHABLE = 2_147_483_647


def _dag_shortest_paths(n, edges, source):
    from collections import deque
    adj = [[] for _ in range(n)]
    indegree = [0] * n
    for i in range(0, len(edges), 3):
        u, v, w = edges[i], edges[i + 1], edges[i + 2]
        adj[u].append((v, w))
        indegree[v] += 1
    queue = deque(v for v in range(n) if indegree[v] == 0)
    order = []
    while queue:
        u = queue.popleft()
        order.append(u)
        for v, _ in adj[u]:
            indegree[v] -= 1
            if indegree[v] == 0:
                queue.append(v)
    dist = [None] * n
    dist[source] = 0
    for u in order:
        if dist[u] is None:
            continue
        for v, w in adj[u]:
            if dist[v] is None or dist[u] + w < dist[v]:
                dist[v] = dist[u] + w
    return [_UNREACHABLE if d is None else d for d in dist]


def _random_dag(n, m, low, high, salt):
    """정점 번호를 섞은 무작위 DAG. 간선은 섞인 순서에서 앞 → 뒤로만 가고, 목록도 섞는다."""
    import random
    from author import SEED
    source = random.Random(SEED + salt)
    order = list(range(n))
    source.shuffle(order)
    edges = []
    for _ in range(m):
        i = source.randrange(n - 1)
        j = min(n - 1, i + 1 + int(source.expovariate(1 / 20)))
        edges.append((order[i], order[j], source.randint(low, high)))
    source.shuffle(edges)
    return flat(list(e) for e in edges), order[0]


def _shuffled_chain(n, low, high, extra, salt):
    """길게 이어진 사슬 + 지름길. 간선 목록이 섞여 있어, 목록 순서대로 완화하면 한 바퀴에 한 칸씩만 나아간다."""
    import random
    from author import SEED
    source = random.Random(SEED + salt)
    order = list(range(n))
    source.shuffle(order)
    edges = [(order[i], order[i + 1], source.randint(low, high)) for i in range(n - 1)]
    for _ in range(extra):
        i = source.randrange(n - 2)
        j = source.randint(i + 2, min(n - 1, i + 50))
        edges.append((order[i], order[j], source.randint(low, high)))
    source.shuffle(edges)
    return flat(list(e) for e in edges), order[0]


_DAG_LARGE = _random_dag(200_000, 400_000, -1000, 1000, salt=10383)
_CHAIN_LARGE = _shuffled_chain(200_000, -1000, 1000, 100_000, salt=10385)
_CHAIN_NEG = _shuffled_chain(200_000, -1000, -1, 0, salt=10387)


PROBLEMS.append(Problem(
    id="dag-shortest-paths",
    title="DAG 의 최단 거리",
    summary="""
정점 `0..n-1` 의 방향 비순환 그래프(DAG)가 간선 `edges = [u1, v1, w1, u2, v2, w2, ...]` 로 주어진다 — `u` 에서 `v` 로
가는 무게 `w` 의 간선이다. **무게는 음수일 수 있다.** 정점 `source` 에서 각 정점까지의 최단 거리를 담은 배열을
반환한다. 갈 수 없는 정점은 `2147483647` 이다.
""",
    notes="""
순환이 없으니 위상 순서가 있다. 위상 순서대로 정점을 꺼내 그 정점에서 나가는 간선을 완화하면, 어떤 정점을 꺼낼 때는
그리로 들어오는 간선이 모두 이미 완화됐다 — 거리가 확정된다. 음수 간선이 있어도 상관없다.

다익스트라는 음수 간선에서 틀린다. 한 번 확정한 정점이 나중에 음수 간선으로 더 짧아질 수 있다. 벨만–포드는 맞지만
간선 목록을 정점 수만큼 훑는다.

`source` 에서 갈 수 없는 정점에서 나가는 간선은 완화하지 않는다 — "무한대 + 음수"를 거리로 삼으면 안 된다.
""",
    drill_doc="""
Drill.visit(u, dist)          // 위상 순서로 꺼낸 정점과 그 거리
Drill.edge("u", "v")          // 이 간선으로 거리가 줄었다
""",
    constraints="""
- `1 <= n <= 200_000`, 간선은 `0..400_000` 개, `0 <= u, v < n`, `u != v`, 같은 쌍의 간선이 여럿일 수 있다
- `-1000 <= w <= 1000`, 그래프에 순환이 없다
- `0 <= source < n`
""",
    signature=dict(name="dagShortestPaths",
                   parameters=[("n", "INT"), ("edges", "INT_ARRAY"), ("source", "INT")], returns="INT_ARRAY"),
    groups=perf_groups(),
    reference=_dag_shortest_paths,
    limits={"timeMillis": 2000, "memoryMb": 256, "outputBytes": 8000000},
    cases={
        "sample": [
            ("01", [4, [0, 1, 5, 0, 2, 3, 2, 1, -4, 1, 3, 2], 0]),
            ("02", [3, [1, 2, 7], 0]),
        ],
        "boundary": [
            ("01-single", [1, [], 0]),
            ("02-no-edges", [3, [], 1]),
            # 먼저 확정한 정점이 음수 간선으로 다시 짧아진다 — 다익스트라가 틀린다.
            ("03-negative-shortcut", [4, [0, 1, 1, 0, 2, 5, 2, 1, -10, 1, 3, 1], 0]),
            # 갈 수 없는 정점에서 나가는 음수 간선은 무시해야 한다.
            ("04-unreachable-negative", [4, [1, 2, -5, 0, 3, 4, 2, 3, -100], 0]),
            ("05-source-in-middle", [5, [0, 1, 1, 1, 2, 1, 2, 3, 1, 3, 4, 1, 0, 4, 1], 2]),
            ("06-parallel-edges", [2, [0, 1, 5, 0, 1, -3, 0, 1, 2], 0]),
            ("07-zero-and-negative-sum", [3, [0, 1, -1000, 1, 2, 1000], 0]),
            # 간선 목록이 위상 순서의 반대로 놓였다.
            ("08-edges-reversed", [5, [3, 4, -1, 2, 3, -1, 1, 2, -1, 0, 1, -1], 0]),
        ],
        "hidden": [
            ("01-random-small", [12, *_random_dag(12, 25, -5, 5, salt=10389)]),
            ("02-random-medium", [2000, *_random_dag(2000, 6000, -100, 100, salt=10391)]),
            ("03-positive-only", [5000, *_random_dag(5000, 15000, 0, 50, salt=10393)]),
            ("04-chain-medium", [3000, *_shuffled_chain(3000, -50, 50, 3000, salt=10395)]),
        ],
        # 벨만–포드는 섞인 사슬에서 한 바퀴에 두 칸쯤 나아간다 — 10 만 바퀴 × 20 만 간선.
        "performance": [
            ("01-random-large", [200_000, *_DAG_LARGE]),
            ("02-chain-large", [200_000, *_CHAIN_LARGE]),
            ("03-negative-chain", [200_000, *_CHAIN_NEG]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). Kahn 의 위상 정렬로 순서를 얻고, 그 순서대로 나가는 간선을 완화한다.
fun dagShortestPaths(n: Int, edges: IntArray, source: Int): IntArray {
    val m = edges.size / 3
    val head = IntArray(n) { -1 }
    val next = IntArray(m)
    val indegree = IntArray(n)
    for (e in 0 until m) { val u = edges[3 * e]; next[e] = head[u]; head[u] = e; indegree[edges[3 * e + 1]] += 1 }
    val order = IntArray(n)
    var size = 0
    for (v in 0 until n) if (indegree[v] == 0) { order[size] = v; size += 1 }
    var read = 0
    while (read < size) {
        val u = order[read]; read += 1
        var e = head[u]
        while (e != -1) {
            val v = edges[3 * e + 1]
            indegree[v] -= 1
            if (indegree[v] == 0) { order[size] = v; size += 1 }
            e = next[e]
        }
    }
    val unreachable = Long.MAX_VALUE
    val dist = LongArray(n) { unreachable }
    dist[source] = 0
    for (u in order) {
        if (dist[u] == unreachable) continue
        Drill.visit(u, dist[u].toInt())
        var e = head[u]
        while (e != -1) {
            val v = edges[3 * e + 1]
            val candidate = dist[u] + edges[3 * e + 2]
            if (candidate < dist[v]) { dist[v] = candidate; Drill.edge(u.toString(), v.toString()) }
            e = next[e]
        }
    }
    return IntArray(n) { if (dist[it] == unreachable) Int.MAX_VALUE else dist[it].toInt() }
}
""",
    mutants=[
        ("dijkstra", "WRONG_ALGORITHM",
         "다익스트라로 푼다. 한 번 확정한 정점이 나중에 음수 간선으로 더 짧아질 수 있다.",
         """
fun dagShortestPaths(n: Int, edges: IntArray, source: Int): IntArray {
    val adj = Array(n) { ArrayList<IntArray>() }
    for (e in 0 until edges.size / 3) adj[edges[3 * e]].add(intArrayOf(edges[3 * e + 1], edges[3 * e + 2]))
    val dist = LongArray(n) { Long.MAX_VALUE }
    val done = BooleanArray(n)
    dist[source] = 0
    val queue = java.util.PriorityQueue<LongArray>(compareBy { it[0] })
    queue.add(longArrayOf(0, source.toLong()))
    while (queue.isNotEmpty()) {
        val top = queue.poll(); val u = top[1].toInt()
        if (done[u]) continue
        done[u] = true
        for (edge in adj[u]) {
            val v = edge[0]
            if (!done[v] && dist[u] + edge[1] < dist[v]) { dist[v] = dist[u] + edge[1]; queue.add(longArrayOf(dist[v], v.toLong())) }
        }
    }
    return IntArray(n) { if (dist[it] == Long.MAX_VALUE) Int.MAX_VALUE else dist[it].toInt() }
}
"""),
        ("relaxes-from-unreachable", "MISSING_EDGE_CASE",
         "갈 수 없는 정점에서 나가는 간선도 완화한다. 무한대에 음수를 더한 값이 거리로 들어간다.",
         """
fun dagShortestPaths(n: Int, edges: IntArray, source: Int): IntArray {
    val m = edges.size / 3
    val adj = Array(n) { ArrayList<Int>() }
    val indegree = IntArray(n)
    for (e in 0 until m) { adj[edges[3 * e]].add(e); indegree[edges[3 * e + 1]] += 1 }
    val queue = ArrayDeque<Int>()
    for (v in 0 until n) if (indegree[v] == 0) queue.addLast(v)
    val order = ArrayList<Int>()
    while (queue.isNotEmpty()) {
        val u = queue.removeFirst(); order.add(u)
        for (e in adj[u]) { val v = edges[3 * e + 1]; indegree[v] -= 1; if (indegree[v] == 0) queue.addLast(v) }
    }
    val inf = Int.MAX_VALUE.toLong()
    val dist = LongArray(n) { inf }
    dist[source] = 0
    for (u in order) for (e in adj[u]) {
        val v = edges[3 * e + 1]
        dist[v] = minOf(dist[v], dist[u] + edges[3 * e + 2])
    }
    return IntArray(n) { dist[it].toInt() }
}
"""),
        ("one-pass-in-list-order", "WRONG_BRANCH",
         "간선 목록을 주어진 순서대로 한 번만 완화한다. 목록이 위상 순서로 놓여 있지 않으면 거리가 덜 줄어든다.",
         """
fun dagShortestPaths(n: Int, edges: IntArray, source: Int): IntArray {
    val dist = LongArray(n) { Long.MAX_VALUE }
    dist[source] = 0
    for (e in 0 until edges.size / 3) {
        val u = edges[3 * e]; val v = edges[3 * e + 1]
        if (dist[u] != Long.MAX_VALUE && dist[u] + edges[3 * e + 2] < dist[v]) dist[v] = dist[u] + edges[3 * e + 2]
    }
    return IntArray(n) { if (dist[it] == Long.MAX_VALUE) Int.MAX_VALUE else dist[it].toInt() }
}
"""),
        ("bellman-ford", "PERFORMANCE",
         "벨만–포드로 간선 목록을 바뀌지 않을 때까지 훑는다. 맞지만, 섞인 긴 사슬에서는 정점 수만큼 바퀴를 돈다.",
         """
fun dagShortestPaths(n: Int, edges: IntArray, source: Int): IntArray {
    val dist = LongArray(n) { Long.MAX_VALUE }
    dist[source] = 0
    var changed = true
    var rounds = 0
    while (changed && rounds < n) {
        changed = false
        rounds += 1
        for (e in 0 until edges.size / 3) {
            val u = edges[3 * e]; val v = edges[3 * e + 1]
            if (dist[u] != Long.MAX_VALUE && dist[u] + edges[3 * e + 2] < dist[v]) { dist[v] = dist[u] + edges[3 * e + 2]; changed = true }
        }
    }
    return IntArray(n) { if (dist[it] == Long.MAX_VALUE) Int.MAX_VALUE else dist[it].toInt() }
}
"""),
    ],
))


# --- 화살표를 고쳐 끝까지 가기 -------------------------------------------------

_ARROWS = {1: (0, 1), 2: (0, -1), 3: (1, 0), 4: (-1, 0)}


def _min_cost_valid_path(grid):
    from collections import deque
    m, n = len(grid), len(grid[0])
    dist = [[None] * n for _ in range(m)]
    dist[0][0] = 0
    queue = deque([(0, 0)])
    while queue:
        r, c = queue.popleft()
        d = dist[r][c]
        for sign, (dr, dc) in _ARROWS.items():
            a, b = r + dr, c + dc
            if 0 <= a < m and 0 <= b < n:
                w = 0 if grid[r][c] == sign else 1
                if dist[a][b] is None or d + w < dist[a][b]:
                    dist[a][b] = d + w
                    if w == 0:
                        queue.appendleft((a, b))
                    else:
                        queue.append((a, b))
    return dist[m - 1][n - 1]


def _arrow_grid(m, n, salt):
    return [randoms(n, 1, 4, salt=salt + r) for r in range(m)]


def _snake_arrows(m, n):
    """줄마다 오른쪽·왼쪽을 번갈아 가는 뱀 — 화살표만 따라가면 칸을 모두 돌고 끝에 닿는다."""
    rows = []
    for r in range(m):
        row = [1] * n if r % 2 == 0 else [2] * n
        row[-1 if r % 2 == 0 else 0] = 3
        rows.append(row)
    return rows


PROBLEMS.append(Problem(
    id="min-cost-valid-path",
    title="화살표를 고쳐 끝까지 가기",
    summary="""
`m × n` 격자의 칸마다 화살표가 하나 있다: `1` 오른쪽, `2` 왼쪽, `3` 아래, `4` 위. 왼쪽 위 칸에서 출발해 화살표를 따라
움직인다(격자 밖을 가리키는 화살표도 있다). 칸의 화살표는 비용 `1` 을 내고 다른 방향으로 바꿀 수 있고, 한 칸은 한 번만
바꿀 수 있다. 오른쪽 아래 칸에 닿는 데 드는 최소 비용을 반환한다.
""",
    notes="""
칸에서 이웃으로 가는 간선은 넷이고, 화살표 방향이면 무게 `0`, 아니면 무게 `1` 이다(그 칸의 화살표를 바꾸는 값). 최단
경로라 한 칸을 두 번 지날 일이 없으니 "한 칸은 한 번만"은 저절로 지켜진다.

무게가 0 과 1 뿐이면 덱 하나로 다익스트라를 대신한다(0-1 BFS): 무게 0 으로 줄어든 칸은 덱 앞에, 무게 1 이면 뒤에 넣는다.
덱 앞에서 꺼내는 칸은 언제나 거리가 가장 작다. 칸이 더 짧은 거리로 다시 줄어들 수 있으니 **거리가 줄었을 때만** 넣고,
처음 넣을 때 방문을 확정하면 안 된다.
""",
    drill_doc="""
Drill.visit(r * n + c, dist)   // 덱에서 꺼낸 칸과 그 거리
""",
    constraints="""
- `1 <= m, n <= 400`
- `grid[r][c]` 는 `1..4`
""",
    signature=dict(name="minCostValidPath", parameters=[("grid", "INT_MATRIX")], returns="INT"),
    groups=standard_groups(),
    reference=_min_cost_valid_path,
    cases={
        "sample": [
            ("01", [[[1, 1, 1, 1], [2, 2, 2, 2], [1, 1, 1, 1], [2, 2, 2, 2]]]),
            ("02", [[[1, 1, 3], [3, 2, 2], [1, 1, 4]]]),
        ],
        "boundary": [
            ("01-single", [[[4]]]),
            ("02-one-row-right", [[[1, 1, 1, 1]]]),
            ("03-one-row-left", [[[2, 2, 2, 2]]]),
            ("04-one-column-down", [[[3], [3], [3]]]),
            ("05-one-column-up", [[[4], [4], [4]]]),
            # 처음 닿은 길이 비싸고, 나중에 덱 앞으로 들어온 길이 더 싸다.
            ("06-cheaper-later", [[[1, 1, 3], [4, 4, 3], [2, 2, 2]]]),
            ("07-free-snake", [_snake_arrows(4, 3)]),
        ],
        "hidden": [
            ("01-random-small", [_arrow_grid(5, 6, salt=10525)]),
            ("02-random-large", [_arrow_grid(400, 400, salt=10527)]),
            ("03-snake-large", [_snake_arrows(400, 400)]),
            ("04-all-up", [[[4] * 300 for _ in range(300)]]),
            ("05-tall-thin", [_arrow_grid(400, 3, salt=10529)]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 0-1 BFS — 화살표 방향은 무게 0 이라 덱 앞에, 아니면 무게 1 이라 덱 뒤에 넣는다.
fun minCostValidPath(grid: Array<IntArray>): Int {
    val m = grid.size
    val n = grid[0].size
    val dr = intArrayOf(0, 0, 0, 1, -1)
    val dc = intArrayOf(0, 1, -1, 0, 0)
    val dist = IntArray(m * n) { Int.MAX_VALUE }
    dist[0] = 0
    val queue = ArrayDeque<Int>()
    queue.addLast(0)
    while (queue.isNotEmpty()) {
        val cell = queue.removeFirst()
        val r = cell / n; val c = cell % n
        Drill.visit(cell, dist[cell])
        for (sign in 1..4) {
            val a = r + dr[sign]; val b = c + dc[sign]
            if (a < 0 || a >= m || b < 0 || b >= n) continue
            val w = if (grid[r][c] == sign) 0 else 1
            val next = a * n + b
            if (dist[cell] + w < dist[next]) {
                dist[next] = dist[cell] + w
                if (w == 0) queue.addFirst(next) else queue.addLast(next)
            }
        }
    }
    return dist[m * n - 1]
}
""",
    mutants=[
        ("counts-steps", "WRONG_ALGORITHM",
         "칸 수를 센다(보통의 BFS). 화살표를 따라가는 걸음은 공짜다.",
         """
fun minCostValidPath(grid: Array<IntArray>): Int {
    val m = grid.size; val n = grid[0].size
    val dist = IntArray(m * n) { -1 }
    dist[0] = 0
    val queue = ArrayDeque<Int>(); queue.addLast(0)
    while (queue.isNotEmpty()) {
        val cell = queue.removeFirst(); val r = cell / n; val c = cell % n
        for ((a, b) in listOf(r to c + 1, r to c - 1, r + 1 to c, r - 1 to c)) {
            if (a < 0 || a >= m || b < 0 || b >= n || dist[a * n + b] >= 0) continue
            dist[a * n + b] = dist[cell] + 1; queue.addLast(a * n + b)
        }
    }
    return dist[m * n - 1]
}
"""),
        ("settled-when-pushed", "WRONG_BRANCH",
         "칸을 처음 덱에 넣을 때 거리를 확정한다. 무게 1 로 먼저 닿은 칸에 나중에 무게 0 의 더 싼 길이 와도 고치지 않는다.",
         """
fun minCostValidPath(grid: Array<IntArray>): Int {
    val m = grid.size; val n = grid[0].size
    val dr = intArrayOf(0, 0, 0, 1, -1); val dc = intArrayOf(0, 1, -1, 0, 0)
    val dist = IntArray(m * n) { Int.MAX_VALUE }
    val seen = BooleanArray(m * n)
    dist[0] = 0; seen[0] = true
    val queue = ArrayDeque<Int>(); queue.addLast(0)
    while (queue.isNotEmpty()) {
        val cell = queue.removeFirst(); val r = cell / n; val c = cell % n
        for (sign in 1..4) {
            val a = r + dr[sign]; val b = c + dc[sign]
            if (a < 0 || a >= m || b < 0 || b >= n) continue
            val next = a * n + b
            if (seen[next]) continue
            val w = if (grid[r][c] == sign) 0 else 1
            seen[next] = true; dist[next] = dist[cell] + w
            if (w == 0) queue.addFirst(next) else queue.addLast(next)
        }
    }
    return dist[m * n - 1]
}
"""),
        ("up-and-down-swapped", "WRONG_BRANCH",
         "3 을 위, 4 를 아래로 읽는다. 3 이 아래, 4 가 위다.",
         """
fun minCostValidPath(grid: Array<IntArray>): Int {
    val m = grid.size; val n = grid[0].size
    val dr = intArrayOf(0, 0, 0, -1, 1); val dc = intArrayOf(0, 1, -1, 0, 0)
    val dist = IntArray(m * n) { Int.MAX_VALUE }
    dist[0] = 0
    val queue = ArrayDeque<Int>(); queue.addLast(0)
    while (queue.isNotEmpty()) {
        val cell = queue.removeFirst(); val r = cell / n; val c = cell % n
        for (sign in 1..4) {
            val a = r + dr[sign]; val b = c + dc[sign]
            if (a < 0 || a >= m || b < 0 || b >= n) continue
            val w = if (grid[r][c] == sign) 0 else 1
            val next = a * n + b
            if (dist[cell] + w < dist[next]) { dist[next] = dist[cell] + w; if (w == 0) queue.addFirst(next) else queue.addLast(next) }
        }
    }
    return dist[m * n - 1]
}
"""),
        ("follows-arrows-greedily", "WRONG_ALGORITHM",
         "화살표를 따라가다 막히면 그 자리에서 끝 쪽으로 화살표를 바꾼다. 지금 바꾸는 것이 가장 싼 길이라는 보장이 없다.",
         """
fun minCostValidPath(grid: Array<IntArray>): Int {
    val m = grid.size; val n = grid[0].size
    val dr = intArrayOf(0, 0, 0, 1, -1); val dc = intArrayOf(0, 1, -1, 0, 0)
    val seen = BooleanArray(m * n)
    var r = 0; var c = 0; var cost = 0
    while (r != m - 1 || c != n - 1) {
        seen[r * n + c] = true
        val a = r + dr[grid[r][c]]; val b = c + dc[grid[r][c]]
        if (a in 0 until m && b in 0 until n && !seen[a * n + b]) { r = a; c = b; continue }
        cost += 1
        if (c < n - 1 && !seen[r * n + c + 1]) c += 1 else if (r < m - 1) r += 1 else c += 1
    }
    return cost
}
"""),
    ],
))


# --- 조건대로 놓은 행렬 --------------------------------------------------------

def _smallest_topo(k, conditions):
    import heapq
    adj = [[] for _ in range(k + 1)]
    indegree = [0] * (k + 1)
    for i in range(0, len(conditions), 2):
        adj[conditions[i]].append(conditions[i + 1])
        indegree[conditions[i + 1]] += 1
    ready = [v for v in range(1, k + 1) if indegree[v] == 0]
    heapq.heapify(ready)
    order = []
    while ready:
        v = heapq.heappop(ready)
        order.append(v)
        for u in adj[v]:
            indegree[u] -= 1
            if indegree[u] == 0:
                heapq.heappush(ready, u)
    return order if len(order) == k else None


def _build_matrix(k, row_conditions, col_conditions):
    rows = _smallest_topo(k, row_conditions)
    cols = _smallest_topo(k, col_conditions)
    if rows is None or cols is None:
        return []
    row_of = {v: i for i, v in enumerate(rows)}
    col_of = {v: i for i, v in enumerate(cols)}
    matrix = [[0] * k for _ in range(k)]
    for v in range(1, k + 1):
        matrix[row_of[v]][col_of[v]] = v
    return matrix


def _dag_conditions(k, m, salt):
    """1..k 를 섞은 순서에서 앞 → 뒤로만 가는 조건 m 개. 순환이 없다."""
    import random
    from author import SEED
    source = random.Random(SEED + salt)
    order = list(range(1, k + 1))
    source.shuffle(order)
    out = []
    for _ in range(m):
        i, j = sorted(source.sample(range(k), 2))
        out += [order[i], order[j]]
    return out


PROBLEMS.append(Problem(
    id="matrix-from-conditions",
    title="조건대로 놓은 행렬",
    summary="""
`1..k` 를 `k × k` 행렬에 하나씩 놓고 나머지 칸은 `0` 으로 둔다. `rowConditions = [a1, b1, a2, b2, ...]` 의 쌍마다 `a`
가 `b` 보다 **위 행**에, `colConditions` 의 쌍마다 `a` 가 `b` 보다 **왼쪽 열**에 있어야 한다. 행의 순서와 열의 순서는 각각
조건을 지키는 순서 가운데 **매번 놓을 수 있는 가장 작은 수를 먼저 놓는** 순서로 정한다. 그렇게 만든 행렬을 반환하고,
조건을 지킬 수 없으면 빈 배열을 반환한다.
""",
    notes="""
행과 열은 따로 논다. 행의 조건만 보면 "a 가 b 보다 위"는 방향 간선 `a → b` 이고, 행의 순서는 그 그래프의 위상 순서다.
열도 같다. 수 `v` 는 행 순서에서의 자리와 열 순서에서의 자리가 만나는 칸에 놓인다.

위상 순서는 여럿일 수 있어 답을 하나로 정하려고 "놓을 수 있는(진입 차수가 0 인) 수 가운데 가장 작은 것부터"로 정했다 —
Kahn 알고리즘에서 큐 대신 최소 힙을 쓴다. 어느 한쪽에 순환이 있으면 모든 수를 꺼내지 못하므로 빈 배열이다.
""",
    drill_doc="""
Drill.dequeue(v)   // 행(또는 열) 순서에 놓았다
""",
    constraints="""
- `2 <= k <= 400`
- 조건은 각각 `1..10_000` 쌍, `1 <= a, b <= k`, `a != b`, 같은 쌍이 되풀이될 수 있다
""",
    signature=dict(name="buildMatrix",
                   parameters=[("k", "INT"), ("rowConditions", "INT_ARRAY"), ("colConditions", "INT_ARRAY")],
                   returns="INT_MATRIX"),
    groups=standard_groups(),
    reference=_build_matrix,
    limits={"timeMillis": 2000, "memoryMb": 256, "outputBytes": 4000000},
    cases={
        "sample": [
            ("01", [3, [1, 2, 3, 2], [2, 1, 3, 2]]),
            ("02", [3, [1, 2, 2, 3, 3, 1, 2, 3], [2, 1]]),
        ],
        "boundary": [
            ("01-one-condition", [2, [1, 2], [2, 1]]),
            # 가장 작은 수부터 — 큐(들어온 순서)로 꺼내면 다른 행렬이 나온다.
            ("02-smallest-first", [4, [4, 1, 3, 2], [4, 3]]),
            ("03-row-cycle", [3, [1, 2, 2, 1], [1, 2]]),
            ("04-column-cycle", [3, [1, 2], [1, 2, 2, 3, 3, 1]]),
            ("05-repeated-pair", [3, [3, 1, 3, 1, 3, 1], [1, 3, 1, 3]]),
            ("06-chain", [4, [4, 3, 3, 2, 2, 1], [1, 2, 2, 3, 3, 4]]),
        ],
        "hidden": [
            ("01-random-small", [6, _dag_conditions(6, 5, salt=10547), _dag_conditions(6, 4, salt=10549)]),
            ("02-random-large", [400, _dag_conditions(400, 10_000, salt=10551), _dag_conditions(400, 10_000, salt=10553)]),
            ("03-sparse-large", [400, _dag_conditions(400, 50, salt=10555), _dag_conditions(400, 30, salt=10557)]),
            ("04-cycle-deep-inside", [300, _dag_conditions(300, 2000, salt=10559) + [7, 8, 8, 9, 9, 7],
                                      _dag_conditions(300, 2000, salt=10561)]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 행과 열 각각 최소 힙 Kahn 으로 위상 순서를 얻고, 두 자리가 만나는 칸에 수를 놓는다.
fun buildMatrix(k: Int, rowConditions: IntArray, colConditions: IntArray): Array<IntArray> {
    fun order(conditions: IntArray): IntArray? {
        val adj = Array(k + 1) { ArrayList<Int>() }
        val indegree = IntArray(k + 1)
        for (i in conditions.indices step 2) { adj[conditions[i]].add(conditions[i + 1]); indegree[conditions[i + 1]] += 1 }
        val ready = java.util.PriorityQueue<Int>()
        for (v in 1..k) if (indegree[v] == 0) ready.add(v)
        val out = IntArray(k)
        var size = 0
        while (ready.isNotEmpty()) {
            val v = ready.poll()
            out[size++] = v
            Drill.dequeue(v)
            for (u in adj[v]) { indegree[u] -= 1; if (indegree[u] == 0) ready.add(u) }
        }
        return if (size == k) out else null
    }
    val rows = order(rowConditions) ?: return arrayOf()
    val cols = order(colConditions) ?: return arrayOf()
    val rowOf = IntArray(k + 1); val colOf = IntArray(k + 1)
    for (i in 0 until k) { rowOf[rows[i]] = i; colOf[cols[i]] = i }
    val matrix = Array(k) { IntArray(k) }
    for (v in 1..k) matrix[rowOf[v]][colOf[v]] = v
    return matrix
}
""",
    mutants=[
        ("fifo-order", "WRONG_ALGORITHM",
         "Kahn 알고리즘을 보통의 큐로 돌린다. 위상 순서는 맞지만 가장 작은 수부터가 아니다.",
         """
fun buildMatrix(k: Int, rowConditions: IntArray, colConditions: IntArray): Array<IntArray> {
    fun order(conditions: IntArray): IntArray? {
        val adj = Array(k + 1) { ArrayList<Int>() }
        val indegree = IntArray(k + 1)
        for (i in conditions.indices step 2) { adj[conditions[i]].add(conditions[i + 1]); indegree[conditions[i + 1]] += 1 }
        val ready = java.util.ArrayDeque<Int>()
        for (v in 1..k) if (indegree[v] == 0) ready.add(v)
        val out = IntArray(k)
        var size = 0
        while (ready.isNotEmpty()) {
            val v = ready.poll()
            out[size++] = v
            Drill.dequeue(v)
            for (u in adj[v]) { indegree[u] -= 1; if (indegree[u] == 0) ready.add(u) }
        }
        return if (size == k) out else null
    }
    val rows = order(rowConditions) ?: return arrayOf()
    val cols = order(colConditions) ?: return arrayOf()
    val rowOf = IntArray(k + 1); val colOf = IntArray(k + 1)
    for (i in 0 until k) { rowOf[rows[i]] = i; colOf[cols[i]] = i }
    val matrix = Array(k) { IntArray(k) }
    for (v in 1..k) matrix[rowOf[v]][colOf[v]] = v
    return matrix
}
"""),
        ("rows-and-columns-swapped", "WRONG_BRANCH",
         "행 조건으로 열을, 열 조건으로 행을 정한다.",
         """
fun buildMatrix(k: Int, rowConditions: IntArray, colConditions: IntArray): Array<IntArray> {
    fun order(conditions: IntArray): IntArray? {
        val adj = Array(k + 1) { ArrayList<Int>() }
        val indegree = IntArray(k + 1)
        for (i in conditions.indices step 2) { adj[conditions[i]].add(conditions[i + 1]); indegree[conditions[i + 1]] += 1 }
        val ready = java.util.PriorityQueue<Int>()
        for (v in 1..k) if (indegree[v] == 0) ready.add(v)
        val out = IntArray(k)
        var size = 0
        while (ready.isNotEmpty()) {
            val v = ready.poll()
            out[size++] = v
            Drill.dequeue(v)
            for (u in adj[v]) { indegree[u] -= 1; if (indegree[u] == 0) ready.add(u) }
        }
        return if (size == k) out else null
    }
    val rows = order(colConditions) ?: return arrayOf()
    val cols = order(rowConditions) ?: return arrayOf()
    val rowOf = IntArray(k + 1); val colOf = IntArray(k + 1)
    for (i in 0 until k) { rowOf[rows[i]] = i; colOf[cols[i]] = i }
    val matrix = Array(k) { IntArray(k) }
    for (v in 1..k) matrix[rowOf[v]][colOf[v]] = v
    return matrix
}
"""),
        ("cycle-ignored", "MISSING_EDGE_CASE",
         "순환을 보지 않는다. 꺼내지 못한 수를 순서 끝에 붙여 행렬을 낸다 — 조건을 지킬 수 없으면 빈 배열이다.",
         """
fun buildMatrix(k: Int, rowConditions: IntArray, colConditions: IntArray): Array<IntArray> {
    fun order(conditions: IntArray): IntArray? {
        val adj = Array(k + 1) { ArrayList<Int>() }
        val indegree = IntArray(k + 1)
        for (i in conditions.indices step 2) { adj[conditions[i]].add(conditions[i + 1]); indegree[conditions[i + 1]] += 1 }
        val ready = java.util.PriorityQueue<Int>()
        for (v in 1..k) if (indegree[v] == 0) ready.add(v)
        val out = IntArray(k)
        var size = 0
        while (ready.isNotEmpty()) {
            val v = ready.poll()
            out[size++] = v
            Drill.dequeue(v)
            for (u in adj[v]) { indegree[u] -= 1; if (indegree[u] == 0) ready.add(u) }
        }
        for (v in 1..k) if (v !in out) { out[size++] = v }
        return out
    }
    val rows = order(rowConditions) ?: return arrayOf()
    val cols = order(colConditions) ?: return arrayOf()
    val rowOf = IntArray(k + 1); val colOf = IntArray(k + 1)
    for (i in 0 until k) { rowOf[rows[i]] = i; colOf[cols[i]] = i }
    val matrix = Array(k) { IntArray(k) }
    for (v in 1..k) matrix[rowOf[v]][colOf[v]] = v
    return matrix
}
"""),
        ("largest-first", "WRONG_BRANCH",
         "놓을 수 있는 수 가운데 가장 큰 것부터 꺼낸다.",
         """
fun buildMatrix(k: Int, rowConditions: IntArray, colConditions: IntArray): Array<IntArray> {
    fun order(conditions: IntArray): IntArray? {
        val adj = Array(k + 1) { ArrayList<Int>() }
        val indegree = IntArray(k + 1)
        for (i in conditions.indices step 2) { adj[conditions[i]].add(conditions[i + 1]); indegree[conditions[i + 1]] += 1 }
        val ready = java.util.PriorityQueue<Int>(compareByDescending { it })
        for (v in 1..k) if (indegree[v] == 0) ready.add(v)
        val out = IntArray(k)
        var size = 0
        while (ready.isNotEmpty()) {
            val v = ready.poll()
            out[size++] = v
            Drill.dequeue(v)
            for (u in adj[v]) { indegree[u] -= 1; if (indegree[u] == 0) ready.add(u) }
        }
        return if (size == k) out else null
    }
    val rows = order(rowConditions) ?: return arrayOf()
    val cols = order(colConditions) ?: return arrayOf()
    val rowOf = IntArray(k + 1); val colOf = IntArray(k + 1)
    for (i in 0 until k) { rowOf[rows[i]] = i; colOf[cols[i]] = i }
    val matrix = Array(k) { IntArray(k) }
    for (v in 1..k) matrix[rowOf[v]][colOf[v]] = v
    return matrix
}
"""),
    ],
))
