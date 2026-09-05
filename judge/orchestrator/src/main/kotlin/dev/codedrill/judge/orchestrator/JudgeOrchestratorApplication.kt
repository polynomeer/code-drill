package dev.codedrill.judge.orchestrator

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * 채점 오케스트레이터 (기술 설계서 §2.2).
 *
 * 작업을 임대하고, 런타임을 고르고, 재시도를 관리하고, Runner 결과를 검증해 결정적으로
 * 집계한다. Control Plane 과 **별도 배포 단위·별도 서비스 계정**이며 Control DB 에
 * 직접 접근하지 않는다 (§2.3).
 */
@SpringBootApplication
class JudgeOrchestratorApplication

fun main(args: Array<String>) {
    runApplication<JudgeOrchestratorApplication>(*args)
}
