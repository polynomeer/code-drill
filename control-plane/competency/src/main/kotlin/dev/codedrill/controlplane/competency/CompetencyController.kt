package dev.codedrill.controlplane.competency

import dev.codedrill.platform.common.Principal
import dev.codedrill.platform.problempackage.Competency
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

/**
 * 역량 지도 API (PRD FR-801, FR-806).
 *
 * > 숙련도·신뢰도·근거를 역량 지도에서 **분리 표시**합니다.
 * > 근거 상세에서 문제, 시각, 도움 수준, 평가 방식을 확인합니다.
 *
 * 그래서 응답도 셋을 합치지 않는다. 한 숫자로 내면 화면이 그 숫자를 그릴 수밖에 없다.
 */
@RestController
@RequestMapping("/api/v1/me/competencies")
class CompetencyController(private val service: CompetencyService) {

    @GetMapping
    fun map(@RequestAttribute(Principal.ATTRIBUTE) principal: Principal): CompetencyMap {
        val mastery = service.mapOf(principal.id)
        return CompetencyMap(
            // 아직 아무 증거도 없으면 진단이 시작되지 않은 것이다. 화면이 이것을 명시해야
            // 사용자가 "0점"과 "아직 재지 않음"을 구분한다 (FR-801).
            diagnosed = mastery.any { it.evidenceCount > 0 },
            competencies = mastery.map(MasteryView::of),
        )
    }

    /** 한 역량의 근거. 목록에서 여기로 내려온다. */
    @GetMapping("/{competency}")
    fun evidence(
        @PathVariable competency: String,
        @RequestAttribute(Principal.ATTRIBUTE) principal: Principal,
    ): ResponseEntity<List<EvidenceView>> {
        val parsed = runCatching { Competency.valueOf(competency.uppercase()) }.getOrNull()
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(service.evidenceOf(principal.id, parsed).map(EvidenceView::of))
    }
}

data class CompetencyMap(
    /** 증거가 하나라도 있는가. false 면 진단 미완료다 (FR-801). */
    val diagnosed: Boolean,
    val competencies: List<MasteryView>,
)

data class MasteryView(
    val competency: String,
    val group: String,
    val level: MasteryLevel,
    val confidence: Confidence,
    val evidenceCount: Int,
    val successCount: Int,
) {
    companion object {
        fun of(mastery: Mastery) = MasteryView(
            competency = mastery.competency.name,
            group = mastery.group.name,
            level = mastery.level,
            confidence = mastery.confidence,
            evidenceCount = mastery.evidenceCount,
            successCount = mastery.successCount,
        )
    }
}

data class EvidenceView(
    val source: EvidenceSource,
    val success: Boolean,
    /** 도움 수준. 1.0 이면 도움 없이 얻은 증거다 (§3.4). */
    val weight: Double,
    val problemId: String,
    val reference: String?,
    val detail: String?,
    val occurredAt: Instant,
) {
    companion object {
        fun of(evidence: Evidence) = EvidenceView(
            source = evidence.source,
            success = evidence.success,
            weight = evidence.weight,
            problemId = evidence.problemId,
            reference = evidence.reference,
            detail = evidence.detail,
            occurredAt = evidence.occurredAt,
        )
    }
}
