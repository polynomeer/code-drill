package dev.codedrill.controlplane.profile

import dev.codedrill.platform.common.Principal
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 공개 프로필 API. 로그인 없이 열린다 — 공개로 켠 프로필은 링크로 나눌 수 있어야 한다. 로그인했으면
 * 알아본다: 본인은 비공개여도 자기 프로필을 미리 본다.
 */
@RestController
@RequestMapping("/api/v1/profiles")
class ProfileController(private val service: ProfileService) {

    @GetMapping("/{handle}")
    fun profile(
        @RequestAttribute(Principal.ATTRIBUTE, required = false) principal: Principal?,
        @PathVariable handle: String,
    ): ResponseEntity<PublicProfile> {
        val profile = service.profile(principal?.id, handle) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(profile)
    }
}
