package dev.codedrill.controlplane.notification

import dev.codedrill.platform.common.Principal
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/me/notifications")
class NotificationController(private val service: NotificationService) {

    @GetMapping
    fun feed(@RequestAttribute(Principal.ATTRIBUTE) principal: Principal): Feed = service.feed(principal.id)

    /** 지금까지를 읽음으로. 화면이 알림 목록을 열 때 부른다. */
    @PostMapping("/read")
    fun read(@RequestAttribute(Principal.ATTRIBUTE) principal: Principal): ResponseEntity<Void> {
        service.markRead(principal.id)
        return ResponseEntity.noContent().build()
    }
}
