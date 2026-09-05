package dev.codedrill.platform.problempackage

/**
 * 로딩된 문제 패키지 (기술 설계서 §6.1).
 *
 * [packageDigest] 는 manifest 와 모든 테스트 케이스 내용에서 결정적으로 계산한다.
 * 같은 내용이면 어느 머신에서 로드하든 같은 값이 나와야 하며, 재채점 시 원래 번들을
 * 그대로 썼는지 확인하는 근거가 된다 (§12.4).
 */
data class ProblemPackage(
    val manifest: ProblemManifest,
    val statementMarkdown: String,
    val groups: List<TestGroup>,
    val packageDigest: String,
) {
    val problemVersionId: String get() = "${manifest.id}@${manifest.version}"

    /** 사용자에게 노출해도 되는 케이스만 추린다. 숨은 입력은 DTO 단계에서 제거한다 (§9.1). */
    fun publicCases(): List<TestCase> = groups.filter { it.policy.exposesInput }.flatMap { it.cases }

    fun group(id: String): TestGroup =
        groups.firstOrNull { it.policy.id == id } ?: error("그룹을 찾지 못했다: $id")
}

data class TestGroup(val policy: GroupPolicy, val cases: List<TestCase>)

/**
 * 테스트 케이스 하나.
 *
 * [args] 와 [expected] 는 JSON 값이며 [ValueType] 에 따라 해석한다. 숨은 그룹의 값은
 * 서명 URL 을 발급하지 않고 API 응답에도 싣지 않는다 (§8.3).
 */
data class TestCase(
    val id: String,
    val groupId: String,
    val args: List<Any>,
    val expected: Any,
)
