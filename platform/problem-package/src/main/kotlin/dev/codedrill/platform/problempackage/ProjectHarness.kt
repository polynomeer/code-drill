package dev.codedrill.platform.problempackage

/**
 * 프로젝트형 테스트 하네스의 소스 (content/projects/README.md "언어").
 *
 * **채점기와 사용자 키트가 같은 파일을 쓴다.** Runner 는 이것을 샌드박스에 넣어 숨은 스위트를 돌리고,
 * 제어 영역은 같은 것을 키트(`/projects/{id}/kit`)에 실어 사용자가 공개 테스트를 로컬에서 돌리게 한다.
 * 둘이 갈리면 "로컬에선 통과했는데 채점에선 다르다"가 생기므로 사본을 두지 않고 이 모듈 한 곳에 둔다.
 *
 * 하네스에는 비밀이 없다 — 조작을 막는 nonce 는 채점 때마다 Runner 가 만들고, 숨은 테스트는 여기 없다.
 */
object ProjectHarness {

    /** 언어별 하네스 파일. 하네스 디렉터리 상대 경로 → 내용. 언어 이름은 manifest 의 것(대문자) */
    fun files(language: String): Map<String, String> = when (language.uppercase()) {
        "PYTHON" -> mapOf(PYTHON to read("python_harness.py"))
        "KOTLIN" -> mapOf(KOTLIN to read("kotlin_harness.kt"))
        "JAVA" -> mapOf(
            "codedrill/CodedrillHarness.java" to read("java_harness.java"),
            "codedrill/Assertions.java" to read("java_assertions.java"),
        )
        else -> throw IllegalArgumentException("하네스가 없는 언어다: $language")
    }

    /** Python 하네스 파일 이름 */
    const val PYTHON = "python_harness.py"

    /** Kotlin 하네스 파일 이름. 클래스 `codedrill.CodedrillHarnessKt` 가 여기서 나온다 */
    const val KOTLIN = "CodedrillHarness.kt"

    private fun read(name: String): String =
        ProjectHarness::class.java.getResourceAsStream("/project-harness/$name")?.bufferedReader()?.readText()
            ?: error("프로젝트 하네스가 리소스에 없다: /project-harness/$name")
}
