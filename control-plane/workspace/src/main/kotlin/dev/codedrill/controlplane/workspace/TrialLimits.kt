package dev.codedrill.controlplane.workspace

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 시험 실행 한도 (§10.2 backpressure).
 *
 * 제출 쿼터와 따로 둔다. 시험 실행은 제출보다 자주 눌리는 것이 정상이고, 같은 한도를 씌우면
 * 시험을 몇 번 돌렸다는 이유로 제출이 막힌다 — 그건 "돌려 보고 제출하라"는 흐름을 벌하는 것이다.
 *
 * 반대 방향도 막아야 한다. 시험 실행에 한도가 없으면 한 사람이 Runner 를 통째로 차지할 수
 * 있고, 그때 밀리는 것은 다른 사람의 **채점**이다.
 */
@ConfigurationProperties(prefix = "codedrill.trial")
data class TrialLimits(
    /** 한 시간에 몇 번까지. */
    val perHour: Int = 60,
    /** 한 번에 몇 케이스까지. 케이스마다 샌드박스 실행이 한 번씩 돈다. */
    val maxCases: Int = 10,
)
