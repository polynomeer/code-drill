package dev.codedrill.controlplane.workspace

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 변이 평가 한도 (§10.2 backpressure).
 *
 * 시험 실행보다 **훨씬 짜다.** 시험 실행 한 번은 케이스 수만큼 샌드박스를 띄우지만,
 * 변이 평가 한 번은 `(1 + 오답 수) × 케이스 수`만큼 띄운다 — 오답 세 개에 케이스 열
 * 건이면 마흔 번이고, 그 사이 그 Runner 는 아무의 채점도 하지 않는다.
 *
 * 자주 누를 이유도 없다. 테스트를 고쳐 다시 재는 것은 몇 분에 한 번 하는 일이지
 * 몇 초에 한 번 하는 일이 아니다.
 */
@ConfigurationProperties(prefix = "codedrill.mutation")
data class MutationLimits(
    /** 한 시간에 몇 번까지. */
    val perHour: Int = 10,
    /** 한 번에 몇 케이스까지. */
    val maxCases: Int = 10,
)
