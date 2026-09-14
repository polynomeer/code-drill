package dev.codedrill.judge.orchestrator.lease

import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate
import java.net.URI
import java.time.Duration
import kotlin.test.assertTrue

/**
 * Redis 구현에 대한 같은 계약.
 *
 * Redis 가 닿는 곳에서만 돈다 — `CODEDRILL_REDIS_URL`, 없으면 localhost:6379. 닿지 않으면
 * 건너뛰되, `CODEDRILL_REQUIRE_REDIS=true` 면 실패한다. 건너뛴 검사는 초록으로 보이고,
 * 운영이 쓰는 구현을 검증하지 않은 빌드가 초록이면 안 된다.
 */
class RedisLeaseRegistryTest : LeaseRegistryContract() {

    @BeforeEach
    fun requireRedis() {
        val why = "Redis 가 닿지 않아 Redis 임대를 검증할 수 없다 (${System.getenv("CODEDRILL_REDIS_URL") ?: "redis://localhost:6379"})"
        if (System.getenv("CODEDRILL_REQUIRE_REDIS") == "true") assertTrue(template != null, why)
        assumeTrue(template != null, why)
    }

    override fun registry() = RedisLeaseRegistry(
        redis = template!!, clock = clock,
        leaseDuration = Duration.ofSeconds(30), dispatchTimeout = Duration.ofMinutes(5),
        // 테스트끼리, 그리고 같은 Redis 를 쓰는 개발 스택과 섞이지 않게.
        prefix = "judge-test",
    )

    private companion object {
        val template: StringRedisTemplate? by lazy {
            val uri = URI(System.getenv("CODEDRILL_REDIS_URL") ?: "redis://localhost:6379")
            val factory = LettuceConnectionFactory(RedisStandaloneConfiguration(uri.host, uri.port)).apply {
                afterPropertiesSet()
            }
            runCatching { StringRedisTemplate(factory).also { it.connectionFactory?.connection?.use { c -> c.ping() } } }.getOrNull()
        }
    }
}
