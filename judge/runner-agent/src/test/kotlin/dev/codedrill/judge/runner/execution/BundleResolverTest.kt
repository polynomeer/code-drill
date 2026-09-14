package dev.codedrill.judge.runner.execution

import dev.codedrill.judge.protocol.BundleRef
import dev.codedrill.judge.protocol.Bundles
import dev.codedrill.judge.protocol.CaseSelection
import dev.codedrill.judge.protocol.ExecutionMode
import dev.codedrill.judge.protocol.ExecutionRequest
import dev.codedrill.judge.protocol.FencingToken
import dev.codedrill.judge.protocol.Language
import dev.codedrill.judge.protocol.RequestedGroup
import dev.codedrill.judge.protocol.Verdict
import dev.codedrill.judge.runner.TestAdapters
import dev.codedrill.judge.runner.execution.adapter.JavaAdapter
import dev.codedrill.judge.runner.execution.adapter.PythonAdapter
import dev.codedrill.judge.runner.execution.sandbox.ProcessSandbox
import dev.codedrill.judge.runner.golden.GoldenSources
import dev.codedrill.platform.problempackage.ProblemPackage
import dev.codedrill.platform.problempackage.ProblemPackageLoader
import dev.codedrill.platform.storage.BlobStore
import dev.codedrill.platform.storage.DirectoryBlobStore
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 번들이 가리키는 테스트로 채점한다 (§8.3). 받은 것이 요청과 다르면 채점하지 않는다.
 */
class BundleResolverTest {

    private val pkg: ProblemPackage = ProblemPackageLoader(Path.of("../../content/problems")).load("two-sum")
    private val groups = pkg.groups.map { RequestedGroup(it.policy, it.cases) }
    private val bytes = Bundles.encode(groups)
    private val ref = BundleRef(Bundles.key(pkg.packageDigest), Bundles.digest(bytes))

    private val store = DirectoryBlobStore(createTempDirectory("bundles")).also { it.put(ref.key, bytes) }

    @Test
    fun `번들을 케이스로 풀어 메시지에 실린 것과 같게 만든다`() {
        val resolved = BundleResolver(store).resolve(request(bundle = ref))

        assertEquals(groups, resolved.groups)
        assertNull(resolved.bundle)
    }

    @Test
    fun `고른 케이스만 남긴다`() {
        val first = groups.first()
        val wanted = CaseSelection(first.policy.id, first.cases.first().id)

        val resolved = BundleResolver(store).resolve(request(bundle = ref, selection = listOf(wanted)))

        assertEquals(1, resolved.groups.size)
        assertEquals(listOf(first.cases.first()), resolved.groups.single().cases)
    }

    @Test
    fun `digest 가 다르면 채점하지 않는다`() {
        val tampered = BundleRef(ref.key, "0".repeat(64))

        assertFailsWith<IllegalStateException> { BundleResolver(store).resolve(request(bundle = tampered)) }
    }

    @Test
    fun `없는 번들은 시스템 오류이지 오답이 아니다`() {
        val engine = ExecutionEngine(
            adapters = mapOf(Language.KOTLIN to TestAdapters.kotlin),
            sandboxes = { ProcessSandbox() },
            bundles = BundleResolver(store),
        )
        val result = engine.execute(request(bundle = BundleRef("bundles/missing.json", ref.digest)))

        assertEquals(Verdict.SYSTEM_ERROR, result.terminalVerdict)
        assertTrue(result.compileLog.orEmpty().contains("번들"), "${result.compileLog}")
    }

    @Test
    fun `번들 없는 요청은 그대로 지나간다`() {
        val inline = request(bundle = null).copy(groups = groups)
        assertEquals(inline, BundleResolver.NONE.resolve(inline))
    }

    @Test
    fun `한 번 받은 번들은 다시 받지 않는다`() {
        var reads = 0
        val counting = object : BlobStore {
            override fun put(key: String, bytes: ByteArray, contentType: String, digest: String?) = error("쓰지 않는다")
            override fun get(key: String): ByteArray? = store.get(key).also { reads += 1 }
            override fun digestOf(key: String) = store.digestOf(key)
        }
        val resolver = BundleResolver(counting)
        repeat(3) { resolver.resolve(request(bundle = ref)) }

        assertEquals(1, reads)
    }

    private fun request(bundle: BundleRef?, selection: List<CaseSelection>? = null) = ExecutionRequest(
        executionId = "exec-bundle",
        submissionId = "sub-bundle",
        attempt = 1,
        fencingToken = FencingToken(1),
        correlationId = "corr-bundle",
        problemVersionId = pkg.problemVersionId,
        packageDigest = pkg.packageDigest,
        language = Language.KOTLIN,
        source = GoldenSources.ACCEPTED.getValue(Language.KOTLIN),
        signature = pkg.manifest.signature,
        limits = pkg.manifest.limits,
        mode = ExecutionMode.JUDGE,
        bundle = bundle,
        selection = selection,
    )
}
