package dev.codedrill.platform.messaging

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BrokerDefinitionsTest {

    private val file: Path = Path.of("../../deploy/broker/definitions.json")

    @Test
    fun `저장소의 정의 파일은 코드에서 만든 것과 같다`() {
        assertTrue(Files.exists(file), "없다: ./gradlew :platform:messaging:writeBrokerDefinitions")
        assertEquals(
            BrokerDefinitions.render(), Files.readString(file),
            "deploy/broker/definitions.json 이 낡았다: ./gradlew :platform:messaging:writeBrokerDefinitions",
        )
    }

    @Test
    fun `Runner 는 자기 출구에만 쓰고 자기 큐만 읽는다`() {
        val access = BrokerDefinitions.accessOf(JudgeIdentity.RUNNER)
        assertEquals("^$", access.configure)
        assertTrue(Regex(access.write).matches("judge.from.runner"))
        assertFalse(Regex(access.write).matches("judge.from.control-plane"))
        assertFalse(Regex(access.write).matches("amq.default"))
        assertTrue(Regex(access.read).matches(JudgeQueues.EXECUTIONS))
        assertFalse(Regex(access.read).matches(JudgeQueues.SUBMISSIONS))
        assertFalse(Regex(access.read).matches(JudgeQueues.RESULTS))
    }

    @Test
    fun `제어 영역만 익명 큐를 만들 수 있고, 아무도 판정 큐를 만들거나 지우지 못한다`() {
        for (identity in JudgeIdentity.entries) {
            val configure = Regex(BrokerDefinitions.accessOf(identity).configure)
            for (queue in JudgeQueues.all) assertFalse(configure.matches(queue), "$identity configure $queue")
        }
        val anonymous = "spring.gen-R73zCj0_SqWF_z5rbKutDA"
        assertTrue(Regex(BrokerDefinitions.accessOf(JudgeIdentity.CONTROL_PLANE).configure).matches(anonymous))
        assertFalse(Regex(BrokerDefinitions.accessOf(JudgeIdentity.ORCHESTRATOR).configure).matches(anonymous))
        assertFalse(Regex(BrokerDefinitions.accessOf(JudgeIdentity.CONTROL_PLANE).configure).matches(JudgeQueues.SUBMISSION_EVENTS))
    }

    @Test
    fun `모든 큐는 정확히 한 신원이 넣고 한 신원이 뺀다`() {
        for (queue in JudgeQueues.all) {
            val writers = JudgeIdentity.entries.filter { queue in JudgeTopology.publishedBy(it) }
            val readers = JudgeIdentity.entries.filter { queue in JudgeTopology.consumedBy(it) }
            assertEquals(1, writers.size, "$queue writers=$writers")
            assertEquals(1, readers.size, "$queue readers=$readers")
        }
    }
}
