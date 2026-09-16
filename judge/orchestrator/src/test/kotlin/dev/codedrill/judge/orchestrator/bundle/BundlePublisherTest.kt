package dev.codedrill.judge.orchestrator.bundle

import dev.codedrill.judge.protocol.Bundles
import dev.codedrill.platform.problempackage.ProblemPackageLoader
import dev.codedrill.platform.storage.BlobStore
import dev.codedrill.platform.storage.DirectoryBlobStore
import java.nio.file.Path
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class BundlePublisherTest {

    private val pkg = ProblemPackageLoader(Path.of("../../content/problems")).load("two-sum")
    private val directory = createTempDirectory("bundles")
    private val backing = DirectoryBlobStore(directory)
    private var now: Instant = Instant.parse("2026-09-14T00:00:00Z")
    private val clock = object : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = this
        override fun instant(): Instant = now
    }

    /** 스토어 호출을 센다. 제출마다 물으면 안 되고, 영원히 안 물어도 안 된다. */
    private var heads = 0
    private var puts = 0
    private val store = object : BlobStore {
        override fun put(key: String, bytes: ByteArray, contentType: String, digest: String?) { puts += 1; backing.put(key, bytes, contentType, digest) }
        override fun get(key: String) = backing.get(key)
        override fun digestOf(key: String): String? { heads += 1; return backing.digestOf(key) }
        override fun delete(key: String) = backing.delete(key)
    }

    @Test
    fun `참조의 digest 는 올린 내용의 digest 다`() {
        val ref = BundlePublisher(store, clock).ensure(pkg)

        val stored = assertNotNull(backing.get(ref.key))
        assertEquals(Bundles.digest(stored), ref.digest)
        assertContentEquals(Bundles.encode(pkg.groups.map { dev.codedrill.judge.protocol.RequestedGroup(it.policy, it.cases) }), stored)
    }

    @Test
    fun `한동안은 다시 묻지 않고, 지나면 다시 묻고, 없어졌으면 다시 올린다`() {
        val publisher = BundlePublisher(store, clock, recheck = Duration.ofMinutes(10))
        val ref = publisher.ensure(pkg)
        repeat(5) { publisher.ensure(pkg) }
        assertEquals(1, heads, "제출마다 묻지 않는다")
        assertEquals(1, puts)

        // 스토어 쪽 사고로 번들이 사라졌다.
        directory.resolve(ref.key).toFile().delete()
        now = now.plus(Duration.ofMinutes(11))
        publisher.ensure(pkg)

        assertEquals(2, heads, "기한이 지나면 다시 묻는다")
        assertEquals(2, puts, "없어졌으면 다시 올린다")

        // 이번에는 손상됐다. 있기는 하다 — 있는지만 물으면 여기서 멈춘다.
        directory.resolve(ref.key).toFile().writeText("[{\"tampered\":true}]")
        now = now.plus(Duration.ofMinutes(11))
        publisher.ensure(pkg)

        assertEquals(3, puts, "다르면 다시 올린다")
        assertContentEquals(Bundles.encode(pkg.groups.map { dev.codedrill.judge.protocol.RequestedGroup(it.policy, it.cases) }), backing.get(ref.key))
    }
}
