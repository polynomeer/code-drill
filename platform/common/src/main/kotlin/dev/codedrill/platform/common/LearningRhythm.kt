package dev.codedrill.platform.common

import java.time.Duration

/**
 * 학습의 박자 (기획서 §8.1 복습 간격).
 *
 * 두 모듈이 같은 값을 본다 — Learning 은 이것으로 복습 시점을 잡고, Competency 는 이것으로
 * "얼마나 오래된 증거까지 신뢰도에 세는가"를 정한다. 따로 두면 복습을 권하는 시점과 증거가
 * 낡았다고 보는 시점이 어긋나고, 그 둘은 같은 사실이어야 한다.
 *
 * 값은 저작자 판단이다. 간격 반복(spaced repetition)의 첫 간격으로 흔히 쓰는 일주일을
 * 그대로 두었고, 실제 재발률이 쌓이면 그때 조정한다.
 */
object LearningRhythm {
    /** 맞힌 뒤 이만큼 지나면 다시 풀어 볼 때다. */
    val REVIEW_INTERVAL: Duration = Duration.ofDays(7)

    /**
     * 신뢰도에 세는 증거의 나이 상한. 복습 간격의 네 배다.
     *
     * 이보다 오래된 증거는 등급에는 남되 **신뢰도에는 세지 않는다.** 한 달 전에 열 번 맞힌
     * 것으로 오늘 "근거 충분"이라 말하면, 그 사이에 잊었는지 아무도 모른다.
     */
    val RECENCY_WINDOW: Duration = REVIEW_INTERVAL.multipliedBy(4)
}
