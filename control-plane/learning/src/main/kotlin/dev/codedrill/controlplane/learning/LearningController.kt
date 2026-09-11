package dev.codedrill.controlplane.learning

import dev.codedrill.platform.common.Principal
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.UUID
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 학습 루프 API (PRD FR-808, FR-205).
 *
 * 전부 `/me` 아래다. 처방·리포트·문제집은 남에게 보여줄 것이 없고, 남의 것을 열 수 있는
 * 길을 아예 만들지 않는 편이 §11.1 을 지키기 쉽다.
 */
@RestController
@RequestMapping("/api/v1/me")
class LearningController(private val service: LearningService) {

    @GetMapping("/prescription")
    fun prescription(@RequestAttribute(Principal.ATTRIBUTE) principal: Principal): Prescription =
        service.prescription(principal.id)

    /** 오늘의 처방에서 밀어낸다 (FR-808 "사용자가 조정할 수 있다"). 바뀐 처방을 돌려준다. */
    @PostMapping("/prescription/{problemId}/skip")
    fun skip(
        @RequestAttribute(Principal.ATTRIBUTE) principal: Principal,
        @PathVariable problemId: String,
    ): Prescription = service.skip(principal.id, problemId)

    @GetMapping("/report/weekly")
    fun weekly(@RequestAttribute(Principal.ATTRIBUTE) principal: Principal): WeeklyReport =
        service.weekly(principal.id)

    @GetMapping("/stats")
    fun stats(@RequestAttribute(Principal.ATTRIBUTE) principal: Principal): Stats =
        service.stats(principal.id)

    // --- 문제집 (FR-205) ---

    @GetMapping("/collections")
    fun collections(@RequestAttribute(Principal.ATTRIBUTE) principal: Principal): List<Collection> =
        service.collections(principal.id)

    @PostMapping("/collections")
    fun create(
        @RequestAttribute(Principal.ATTRIBUTE) principal: Principal,
        @Valid @RequestBody request: CreateCollectionRequest,
    ): ResponseEntity<Collection> =
        ResponseEntity.status(201).body(service.createCollection(principal.id, request.name))

    /** 담는다. 이미 있으면 그대로 204 다 — 중복 저장은 오류가 아니라 무시다 (FR-205). */
    @PostMapping("/collections/{id}/problems/{problemId}")
    fun add(
        @RequestAttribute(Principal.ATTRIBUTE) principal: Principal,
        @PathVariable id: UUID,
        @PathVariable problemId: String,
    ): ResponseEntity<Void> =
        if (service.addToCollection(principal.id, id, problemId)) ResponseEntity.noContent().build()
        else ResponseEntity.notFound().build()

    @DeleteMapping("/collections/{id}/problems/{problemId}")
    fun remove(
        @RequestAttribute(Principal.ATTRIBUTE) principal: Principal,
        @PathVariable id: UUID,
        @PathVariable problemId: String,
    ): ResponseEntity<Void> =
        if (service.removeFromCollection(principal.id, id, problemId)) ResponseEntity.noContent().build()
        else ResponseEntity.notFound().build()

    @DeleteMapping("/collections/{id}")
    fun delete(
        @RequestAttribute(Principal.ATTRIBUTE) principal: Principal,
        @PathVariable id: UUID,
    ): ResponseEntity<Void> =
        if (service.deleteCollection(principal.id, id)) ResponseEntity.noContent().build()
        else ResponseEntity.notFound().build()
}

data class CreateCollectionRequest(@field:NotBlank @field:Size(max = 60) val name: String)
