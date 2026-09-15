"""그래프 (역량: 관계를 자료구조로 옮기고 탐색하기).

간선은 평탄화한 정수 배열로 준다. `[a1, b1, a2, b2, ...]` 이며, 정점 수는 따로 받는다.
그래프를 인접 리스트로 옮기는 것부터가 문제의 일부다.
"""

from author import Problem, standard_groups, perf_groups, randoms, flat

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
