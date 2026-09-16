package dev.codedrill.judge.orchestrator.bundle

import dev.codedrill.judge.protocol.BundleRef
import dev.codedrill.judge.protocol.Bundles
import dev.codedrill.judge.protocol.RequestedGroup
import dev.codedrill.judge.protocol.Workspaces
import dev.codedrill.platform.problempackage.ProblemPackage
import dev.codedrill.platform.problempackage.ProjectPackage
import dev.codedrill.platform.storage.BlobStore
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * 문제 패키지의 테스트를 오브젝트 스토어에 올리고 참조를 돌려준다 (기술 설계서 §8.3).
 *
 * 실행을 걸기 전에 부른다. 처음 보는 패키지면 올리고, 이미 있으면 그대로 둔다 — 키가
 * 내용(패키지 digest)이라 같은 것을 두 번 올려도 해가 없고, 오케스트레이터가 여럿이어도
 * 서로 조율할 것이 없다.
 *
 * 한 번 올린 것은 한동안 기억한다. 제출마다 스토어에 묻는 것은 낭비다. 그렇다고 영원히
 * 믿지는 않는다 — 스토어에서 지워지거나 손상됐다면 Runner 가 매번 시스템 오류로 끝내는데,
 * 오케스트레이터가 재시작할 때까지 그것이 이어지면 안 된다. [RECHECK] 마다 저장된 digest
 * 를 다시 묻고, 없거나 다르면 다시 올린다. 있는지만 물으면 손상된 번들은 "있다"다.
 */
class BundlePublisher(
    private val store: BlobStore,
    private val clock: Clock = Clock.systemUTC(),
    private val recheck: Duration = RECHECK,
) {

    private val log = LoggerFactory.getLogger(javaClass)
    private val verifiedAt = ConcurrentHashMap<String, Instant>()

    fun ensure(pkg: ProblemPackage): BundleRef =
        ensure(Bundles.key(pkg.packageDigest), Bundles.encode(pkg.groups.map { RequestedGroup(it.policy, it.cases) }), "테스트 번들")

    /**
     * 프로젝트형의 숨은 테스트 스위트 (feature-roadmap 11단계). 번들과 같은 길을 간다 —
     * 키는 패키지 digest, 모양은 [Workspaces] 의 것이다.
     */
    fun ensureSuite(pkg: ProjectPackage): BundleRef =
        ensure(Workspaces.suiteKey(pkg.packageDigest), Workspaces.encode(pkg.hidden), "숨은 스위트")

    private fun ensure(key: String, bytes: ByteArray, what: String): BundleRef {
        val digest = Bundles.digest(bytes)
        val now = clock.instant()
        val last = verifiedAt[key]
        if (last == null || Duration.between(last, now) >= recheck) {
            val stored = store.digestOf(key)
            if (stored != digest) {
                store.put(key, bytes, "application/json", digest)
                log.info(
                    "{}을 올렸다: {} ({}KB){}", what, key, bytes.size / 1024,
                    if (stored == null) "" else " — 저장된 것이 요청과 달랐다 (${stored.take(12)})",
                )
            }
            verifiedAt[key] = now
        }
        return BundleRef(key, digest)
    }

    companion object {
        val RECHECK: Duration = Duration.ofMinutes(10)
    }
}
