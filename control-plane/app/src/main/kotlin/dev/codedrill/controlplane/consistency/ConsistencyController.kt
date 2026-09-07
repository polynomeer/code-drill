package dev.codedrill.controlplane.consistency

import dev.codedrill.controlplane.admin.AdminRole
import dev.codedrill.controlplane.admin.RequiresRole
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 일관성 점검 조회 (기술 설계서 §12.4).
 *
 * 대시보드는 건수만 보여 준다. 조사에 들어가면 "어느 행인가"가 필요하므로 표본을
 * 함께 돌려준다. 게이지가 갱신되기를 기다리지 않고 즉시 다시 돌리는 경로이기도 하다 —
 * 조치 후 해소를 확인할 때 1분을 기다리지 않아도 된다.
 */
@RestController
@RequestMapping("/api/v1/admin/consistency")
class ConsistencyController(private val checker: ConsistencyChecker) {

    @RequiresRole(AdminRole.JUDGE_OPERATOR)
    @GetMapping
    fun sweep(): Map<String, Any> {
        val results = checker.sweep()
        return mapOf(
            "healthy" to results.all { it.count == 0 },
            "checks" to results,
        )
    }
}
