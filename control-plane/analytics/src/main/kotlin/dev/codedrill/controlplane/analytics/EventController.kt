package dev.codedrill.controlplane.analytics

import dev.codedrill.platform.common.Principal
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * `POST /events` — 로그인 없이도 받는다(둘러보는 사람의 탐색도 센다). 로그인했으면 그 사실만 쓴다; 누구인지는
 * 쓰지 않는다. 받지 않은 것의 수를 돌려준다 — 클라이언트 시험이 스키마에서 벗어난 이벤트를 알아챈다.
 */
@RestController
@RequestMapping("/api/v1/events")
class EventController(private val service: EventService) {

    data class Batch(val session: String, val uiVersion: String = "", val events: List<EventService.Incoming> = emptyList())

    @PostMapping
    fun record(
        @RequestAttribute(Principal.ATTRIBUTE, required = false) principal: Principal?,
        @RequestBody batch: Batch,
    ): ResponseEntity<EventService.Result> =
        ResponseEntity.accepted().body(service.record(batch.session, principal != null, batch.uiVersion, batch.events))
}
