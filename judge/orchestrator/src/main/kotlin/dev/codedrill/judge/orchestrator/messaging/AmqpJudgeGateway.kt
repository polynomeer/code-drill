package dev.codedrill.judge.orchestrator.messaging

import dev.codedrill.judge.orchestrator.JudgeGateway
import dev.codedrill.judge.orchestrator.ProjectGateway
import dev.codedrill.judge.protocol.ExecutionRequest
import dev.codedrill.judge.protocol.JudgeCompleted
import dev.codedrill.judge.protocol.JudgeProgressed
import dev.codedrill.judge.protocol.ProjectCompleted
import dev.codedrill.judge.protocol.ProjectRequest
import dev.codedrill.judge.protocol.TraceReady
import dev.codedrill.platform.messaging.JudgeQueues
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Component

/** [JudgeGateway]·[ProjectGateway] 의 브로커 구현. 실행 영역은 브로커로만 바깥과 말한다 (§2.3). */
@Component
class AmqpJudgeGateway(private val rabbit: RabbitTemplate) : JudgeGateway, ProjectGateway {

    override fun requestExecution(request: ExecutionRequest) =
        rabbit.convertAndSend(JudgeQueues.EXECUTIONS, request)

    override fun publishProgress(progress: JudgeProgressed) =
        rabbit.convertAndSend(JudgeQueues.PROGRESS, progress)

    override fun publishCompleted(completed: JudgeCompleted) =
        rabbit.convertAndSend(JudgeQueues.PROGRESS, completed)

    override fun publishTraceReady(ready: TraceReady) =
        rabbit.convertAndSend(JudgeQueues.PROGRESS, ready)

    // 프로젝트형 (11단계). 큐가 다르다 — 분 단위 실행이 초 단위 채점 앞에 서면 안 된다.
    override fun requestProject(request: ProjectRequest) =
        rabbit.convertAndSend(JudgeQueues.PROJECTS, request)

    override fun publishProjectProgress(progress: JudgeProgressed) =
        rabbit.convertAndSend(JudgeQueues.PROJECT_PROGRESS, progress)

    override fun publishProjectCompleted(completed: ProjectCompleted) =
        rabbit.convertAndSend(JudgeQueues.PROJECT_PROGRESS, completed)
}
