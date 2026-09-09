package dev.codedrill.judge.orchestrator

import dev.codedrill.judge.orchestrator.lease.AttemptRegistry
import dev.codedrill.judge.protocol.Language
import dev.codedrill.platform.observability.Metrics
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import java.time.Duration

/**
 * 실행 영역이 내보내는 SLI (기술 설계서 §12.1, §13.2 Judge Quality).
 *
 * 조정자는 브로커 없이도 돌아야 하므로 (테스트가 그렇게 쓴다) 기본값으로 버려지는
 * 레지스트리를 둔다. 계측 때문에 불변식 테스트가 스프링을 띄우게 만들지 않는다.
 */
class JudgeMetrics(private val registry: MeterRegistry = SimpleMeterRegistry()) {

    /** 큐 대기 (§12.1 Queue wait). 찍은 시각이 없는 이전 버전 메시지는 건너뛴다. */
    fun queueWait(waited: Duration) {
        Timer.builder(Metrics.QUEUE_WAIT)
            .publishPercentileHistogram()
            .register(registry)
            .record(waited)
    }

    /**
     * 도착한 결과를 어떻게 처리했는지 (§4.3).
     *
     * duplicate 는 정상이고 stale 은 워커 유실의 흔적이며 already_completed 는 조사
     * 대상이다. 한 카운터에 태그로 담아야 대시보드에서 비율로 볼 수 있다.
     *
     * unleased 는 **정상 경로에서 드물어야 한다.** 재시작이나 인스턴스 증설 직후에는
     * 자연스럽지만, 꾸준히 나온다면 임대가 제 일을 못 하고 있다는 뜻이고 그러면 만료로
     * 워커 유실을 잡는 장치도 함께 무너져 있다.
     */
    fun acceptance(outcome: AttemptRegistry.Acceptance) {
        val label = when (outcome) {
            AttemptRegistry.Acceptance.Accepted -> "accepted"
            AttemptRegistry.Acceptance.Duplicate -> "duplicate"
            AttemptRegistry.Acceptance.AlreadyCompleted -> "already_completed"
            is AttemptRegistry.Acceptance.Stale -> "stale"
            AttemptRegistry.Acceptance.Unleased -> "unleased"
        }
        registry.counter(Metrics.RESULT_ACCEPTANCE, Metrics.Tag.OUTCOME, label).increment()
    }

    /** 만료된 임대를 회수해 다시 실행에 걸었다 (§4.3 워커 유실). */
    fun leaseReclaimed(language: Language) {
        registry.counter(Metrics.LEASE_RECLAIMED, Metrics.Tag.LANGUAGE, language.name).increment()
    }

    /** 트레이스 가공 (§12.1 Trace manifest ready). */
    fun traceProcessed(took: Duration, captured: Int, kept: Int) {
        Timer.builder(Metrics.TRACE_PROCESS)
            .publishPercentileHistogram()
            .register(registry)
            .record(took)
        registry.counter(Metrics.TRACE_EVENTS, Metrics.Tag.OUTCOME, "kept").increment(kept.toDouble())
        registry.counter(Metrics.TRACE_EVENTS, Metrics.Tag.OUTCOME, "dropped")
            .increment((captured - kept).coerceAtLeast(0).toDouble())
    }

    /** 검증에서 걸러진 트레이스. 판정에는 영향이 없지만 리플레이가 비는 이유다 (§7.3). */
    fun traceInvalid(reason: String) {
        registry.counter(Metrics.TRACE_INVALID, Metrics.Tag.REASON, reason).increment()
    }
}
