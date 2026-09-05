package dev.codedrill.judge.runner

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Runner Agent (기술 설계서 §5).
 *
 * 샌드박스를 만들고, 컴파일·실행하고, cgroup 으로 자원을 측정하고, 로그를 제한해
 * 수집한다. 사용자 소스와 그 출력은 전부 비신뢰 입력으로 다룬다 (§5.1).
 *
 * 이 프로세스는 Control Plane 자격증명을 갖지 않고, 인터넷과 Control DB 로 직접
 * 연결하지 않는다. 결과는 제한된 내부 엔드포인트나 브로커로만 돌려보낸다 (§2.3).
 */
@SpringBootApplication
class RunnerAgentApplication

fun main(args: Array<String>) {
    runApplication<RunnerAgentApplication>(*args)
}
