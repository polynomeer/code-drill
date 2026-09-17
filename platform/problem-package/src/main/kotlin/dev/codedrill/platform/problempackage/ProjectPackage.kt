package dev.codedrill.platform.problempackage

/**
 * 프로젝트형 문제 패키지 (feature-roadmap 11단계, 원본 §9.2).
 *
 * 알고리즘 문제와 **다른 모양의 문제**다. 소스 한 문자열과 케이스 목록이 아니라 파일
 * 여럿과 숨은 테스트 스위트 하나이고, 채점은 케이스별 출력 비교가 아니라 테스트 리포트에서
 * 나온다. 그래서 [ProblemPackage] 를 늘리지 않고 따로 둔다 — 봉투도 따로다.
 *
 * 디렉터리 모양:
 *
 * ```
 * content/projects/<id>/
 *   manifest.yaml     id, version, title, language, limits, hidden 모듈
 *   statement.md      요구사항. 사용자가 읽는 것
 *   catalog.yaml      difficulty, tags, summary
 *   starter/          사용자가 받는 파일. 공개 테스트도 여기 있다
 *   hidden/           숨은 테스트. 채점 때 starter 위에 덮인다. 사용자에게 절대 나가지 않는다
 *   reference/        참조 구현. starter 위에 덮어 숨은 테스트를 전부 통과해야 한다
 *   mutants/<name>/   대표 오답. 각각 starter 위에 덮어 숨은 테스트에 하나는 떨어져야 한다
 *   editorial.md      해설
 * ```
 *
 * [packageDigest] 는 manifest·starter·hidden 에서 나온다 — 사용자에게 나가는 것과 채점하는
 * 것이 그 셋이다. 참조와 오답은 알고리즘 문제의 solutions/·mutants/ 와 같은 이유로 밖이다.
 */
data class ProjectPackage(
    val manifest: ProjectManifest,
    val statementMarkdown: String,
    val catalog: ProjectCatalog,
    /** 사용자가 받는 파일. 경로 → 내용. 경로는 `/` 로 잇고 루트 상대다. */
    val starter: Map<String, String>,
    /** 숨은 테스트. 경로 → 내용. 채점 때 사용자의 워크스페이스 위에 덮인다. */
    val hidden: Map<String, String>,
    val packageDigest: String,
) {
    val projectVersionId: String get() = "${manifest.id}@${manifest.version}"

    /**
     * 숨은 테스트의 모듈 이름. 리포트에서 숨은 것을 가른다.
     *
     * 파일 경로에서 나온다 — `tests/test_hidden.py` → `tests.test_hidden`, `tests/LedgerTest.kt` →
     * `tests.LedgerTest`. Python 은 모듈 이름이 곧 파일이고, Kotlin·Java 는 파일 하나에 같은 이름의
     * 클래스 하나라는 약속을 저작 검증이 지킨다 (`hidden-modules`).
     */
    val hiddenModules: Set<String>
        get() = hidden.keys.filter(::isTestModule).map(::moduleName).toSet()

    /** 시작 저장소의 공개 테스트 모듈. 결과 화면이 "공개"와 "숨은"을 가르는 기준이다. */
    val publicModules: List<String>
        get() = starter.keys.filter(::isTestModule).map(::moduleName)

    companion object {
        /** 테스트 모듈인 경로. Python 은 `tests/test_*.py`, Kotlin·Java 는 `tests/…Test.kt`·`.java` 다. */
        fun isTestModule(path: String): Boolean =
            path.matches(PYTHON_TEST) || path.matches(JVM_TEST)

        fun moduleName(path: String): String =
            path.removeSuffix(".py").removeSuffix(".kt").removeSuffix(".java").replace('/', '.')

        /**
         * 파일 안의 테스트 수. Python 은 들여쓴 `def test_`, Kotlin 은 `fun test`. 사용자가 시작
         * 저장소보다 얼마나 더 썼는지를 세는 데 쓴다 — 실행 결과가 아니라 소스에서 센다.
         */
        fun testMethods(path: String, content: String): Int = when {
            path.endsWith(".py") -> PYTHON_METHOD.findAll(content).count()
            path.endsWith(".kt") -> KOTLIN_METHOD.findAll(content).count()
            path.endsWith(".java") -> JAVA_METHOD.findAll(content).count()
            else -> 0
        }

        private val PYTHON_TEST = Regex("""tests/test_[A-Za-z0-9_]+\.py""")
        private val JVM_TEST = Regex("""tests/(?:[A-Za-z0-9_]+/)*[A-Za-z0-9_]+Test\.(?:kt|java)""")
        private val PYTHON_METHOD = Regex("""^\s+def test_\w+\s*\(""", RegexOption.MULTILINE)
        private val KOTLIN_METHOD = Regex("""^\s+fun test\w+\s*\(""", RegexOption.MULTILINE)
        private val JAVA_METHOD = Regex("""^\s+public void test\w+\s*\(""", RegexOption.MULTILINE)
    }
}

data class ProjectManifest(
    val id: String,
    val version: Int,
    val title: String,
    /** 프로젝트 하나는 언어 하나다. 빌드·테스트 명령이 언어에서 나온다. */
    val language: String,
    val statement: String = "statement.md",
    val limits: ProjectLimits,
) {
    init {
        require(id.matches(Regex("[a-z0-9]+(-[a-z0-9]+)*"))) { "프로젝트 id 는 kebab-case 여야 한다: $id" }
        require(version >= 1) { "version 은 1 이상이어야 한다" }
        require(title.isNotBlank()) { "title 이 비어 있다" }
    }
}

/**
 * 프로젝트형의 한도. 초가 아니라 **분** 단위다 — 빌드 한 번, 스위트 한 번이다.
 *
 * [buildSeconds] 와 [testSeconds] 는 각 단계의 벽시계 한도이고, [memoryMb] 는 두 단계에
 * 같이 걸린다.
 */
data class ProjectLimits(
    val buildSeconds: Int,
    val testSeconds: Int,
    val memoryMb: Int,
) {
    init {
        require(buildSeconds in 1..600) { "buildSeconds 는 1~600 이어야 한다" }
        require(testSeconds in 1..1800) { "testSeconds 는 1~1800 이어야 한다" }
        require(memoryMb in 64..4096) { "memoryMb 는 64~4096 이어야 한다" }
    }
}

/**
 * 목록에 보이는 것. 패키지 digest 밖이다 — 요약 문장을 고쳤다고 판정이 무효가 되면 안 된다.
 *
 * [competencies] 는 **실무군만** 달 수 있다 ([Competency.engineering]). 프로젝트형이 재는 것은
 * 처음의 16개 안에 없었고, 있는 칸에 끼워 넣는 대신 늘렸다 (feature-roadmap 11단계). 저작
 * 검증이 이 경계를 지킨다.
 */
data class ProjectCatalog(
    val difficulty: Difficulty,
    val tags: List<String>,
    val summary: String,
    val competencies: List<Competency>,
)
