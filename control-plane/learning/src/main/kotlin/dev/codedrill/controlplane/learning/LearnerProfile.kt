package dev.codedrill.controlplane.learning

/**
 * 가입 직후 세 문항 (docs/ui-overhaul.md §6.9 온보딩).
 *
 * 묻는 것은 처방이 실제로 쓰는 것뿐이다 — 답이 아무것도 바꾸지 않는 질문은 묻지 않는다.
 * - [dailyGoal]: 하루 몇 문제. 처방의 칸 수가 된다 (1~3).
 * - [language]: 주 언어. 풀이 화면의 기본 언어가 된다.
 * - [level]: 지금 수준. 진단 문제의 난이도 띠가 된다.
 *
 * 수준은 자기 보고다. 그래서 진단으로만 쓴다 — 첫 세 문제를 고르는 데까지이고, 역량 지도에는 들어가지
 * 않는다. 거기는 실행 증거만 들어간다.
 */
data class LearnerProfile(
    val dailyGoal: Int,
    val language: String,
    val level: Level,
) {
    enum class Level { BEGINNER, INTERMEDIATE, ADVANCED }

    companion object {
        val LANGUAGES = setOf("KOTLIN", "JAVA", "PYTHON")
    }
}
