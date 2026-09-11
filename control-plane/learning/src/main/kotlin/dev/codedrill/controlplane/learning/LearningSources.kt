package dev.codedrill.controlplane.learning

import dev.codedrill.platform.problempackage.Competency
import java.time.Instant

/**
 * 추천이 기대는 사실들 (기술 설계서 §3.1 조립 지점).
 *
 * Learning 은 제출도 코칭도 숙련도도 모른다. 추천에 필요한 질문만 여기 열어 두고, 조립
 * 지점인 `:control-plane:app` 이 각 도메인에 연결한다.
 *
 * 기획서 §8.1 이 추천에 넣으라고 한 넷이 그대로 질문이 된다 — **최근 오답 원인**(시도
 * 이력), **힌트 의존도**(도움 단계), **복습 간격**(마지막으로 맞힌 시각), **전이 성과**
 * (걸려 있는 전이 과제).
 */
interface LearningSources {

    /** 이 사용자의 시도. 시간순. */
    fun attempts(userId: String, since: Instant): List<Attempt>

    /** 한 번이라도 맞힌 문제. */
    fun solved(userId: String): Set<String>

    /** 이 문제에서 받은 가장 깊은 도움 단계. 0 이면 없다. */
    fun helpLevel(userId: String, problemId: String): Int

    /** 걸려 있는 전이 과제의 변형 문제. 없으면 null. */
    fun pendingTransfer(userId: String): String?

    /**
     * 역량별 상태. [asOf] 시점까지의 증거로 계산한다.
     *
     * 주간 리포트가 "지난주보다 무엇이 올랐나"를 말하려면 지난주 시점의 상태가 있어야
     * 한다. 숙련도를 저장하지 않고 증거에서 계산하는 구조(3단계)라 이것이 가능하다.
     */
    fun standing(userId: String, asOf: Instant): Map<Competency, Standing>

    /** 공개된 문제. 공개되지 않은 문제를 권하면 사용자는 열 수 없는 문제를 풀라는 말을 듣는다. */
    fun published(): Set<String>

    companion object {
        val NONE = object : LearningSources {
            override fun attempts(userId: String, since: Instant) = emptyList<Attempt>()
            override fun solved(userId: String) = emptySet<String>()
            override fun helpLevel(userId: String, problemId: String) = 0
            override fun pendingTransfer(userId: String): String? = null
            override fun standing(userId: String, asOf: Instant) = emptyMap<Competency, Standing>()
            override fun published() = emptySet<String>()
        }
    }
}

/** 시도 한 번. 판정 이름은 통계에 쓰고, 추천은 [accepted] 만 본다. */
data class Attempt(
    val problemId: String,
    val accepted: Boolean,
    val verdict: String,
    val at: Instant,
)

/**
 * 역량의 상태. Competency 의 등급을 그대로 옮긴 것이다.
 *
 * 저쪽 타입을 직접 쓰지 않는 이유는 §3.1 — 도메인 모듈은 서로를 참조하지 않는다.
 * 이름으로 옮기는 것은 조립 지점이 한다.
 */
enum class Standing { UNMEASURED, DEVELOPING, PROFICIENT, STRONG }
