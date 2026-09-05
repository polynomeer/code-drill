package dev.codedrill.controlplane

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Control Plane 애플리케이션 (기술 설계서 §2.2).
 *
 * Auth, Problem, Workspace, Submission, Competency, Coaching, Admin 모듈을 하나의
 * 배포 단위로 묶는 모듈형 모놀리스다. 채점 실행은 이 프로세스 밖 독립 신뢰 경계에 있다.
 */
@SpringBootApplication
class ControlPlaneApplication

fun main(args: Array<String>) {
    runApplication<ControlPlaneApplication>(*args)
}
