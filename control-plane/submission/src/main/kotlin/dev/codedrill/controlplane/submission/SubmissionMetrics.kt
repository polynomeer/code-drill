package dev.codedrill.controlplane.submission

import dev.codedrill.judge.protocol.Verdict
import dev.codedrill.platform.observability.Metrics
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * 제어 영역이 내보내는 SLI (기술 설계서 §12.1, §13.2).
 *
 * 측정 지점을 한 클래스에 모아 둔다. 흩어 두면 어떤 SLO 가 어디에서 재지는지 알 수
 * 없고, 이름이 조금씩 어긋나 대시보드가 빈다.
 *
 * SLO 목표는 여기 KDoc 이 아니라 `deploy/observability/alerts.yml` 이 단일 출처다.
 * 값을 두 곳에 적으면 경보와 문서가 갈라진다.
 */
@Component
class SubmissionMetrics(private val registry: MeterRegistry) {

    /** 제출 수락(§12.1 Submit acceptance latency). 재는 구간은 API 수신 → 커밋이다. */
    fun <T> timeAccept(block: () -> T): T =
        Timer.builder(Metrics.SUBMIT_ACCEPT)
            .publishPercentileHistogram()
            .register(registry)
            .recordCallable(block)!!

    /** 새로 만들었는지, 멱등 키가 맞아 기존 것을 돌려줬는지 (§4.3). */
    fun created(language: String, idempotentHit: Boolean) {
        registry.counter(
            Metrics.SUBMISSION_CREATED,
            Metrics.Tag.LANGUAGE, language,
            Metrics.Tag.RESULT, if (idempotentHit) "idempotent_hit" else "created",
        ).increment()
    }

    /**
     * 판정 종료.
     *
     * verdict 분포와 SYSTEM_ERROR 를 나눠 센다. 둘을 한 카운터에 담으면 "플랫폼이
     * 고장났다"와 "사용자가 틀렸다"가 같은 선으로 보인다 (§4.4).
     */
    fun completed(language: String, verdict: Verdict, waited: Duration, propagation: Duration) {
        registry.counter(
            Metrics.VERDICT,
            Metrics.Tag.VERDICT, verdict.name,
            Metrics.Tag.LANGUAGE, language,
        ).increment()

        if (verdict == Verdict.SYSTEM_ERROR) {
            registry.counter(Metrics.SYSTEM_ERROR, Metrics.Tag.LANGUAGE, language).increment()
        }

        Timer.builder(Metrics.SUBMISSION_DURATION)
            .tag(Metrics.Tag.LANGUAGE, language)
            .tag(Metrics.Tag.VERDICT, verdict.name)
            .publishPercentileHistogram()
            .register(registry)
            .record(waited)

        Timer.builder(Metrics.VERDICT_PROPAGATION)
            .publishPercentileHistogram()
            .register(registry)
            .record(propagation)
    }
}
