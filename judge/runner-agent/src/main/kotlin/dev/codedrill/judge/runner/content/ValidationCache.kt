package dev.codedrill.judge.runner.content

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteExisting
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readBytes
import kotlin.io.path.readText
import kotlin.io.path.relativeTo
import kotlin.io.path.writeText

/**
 * 전체 검증 결과의 캐시 — 바뀐 것만 다시 검증한다.
 *
 * 전체 검증은 문제당 10~30초라, 문제가 수백이 되자 CI 의 `validateContent` 하나가 두 시간 가까이 걸렸다.
 * 커밋 하나가 바꾸는 패키지는 대개 한두 개다. 같은 입력이면 같은 보고서가 나오므로(보고서 digest 는 결정적이다)
 * 지난번 통과한 보고서를 그대로 쓴다.
 *
 * **키에 든 것이 바뀌면 다시 잰다**:
 * - 패키지 디렉터리의 모든 파일. `packageDigest` 로는 안 된다 — 그것은 판정 입력만 덮고 카탈로그·해답·오답·
 *   해설은 덮지 않는다. 오답만 고쳤는데 지난 결과를 쓰면 안 된다.
 * - 패키지 밖에서 검증이 읽는 것: 태그 어휘(`content/tags.yaml`), 선수 관계 그래프, 그 문제의 번호
 *   (`content/problems/numbers.yaml`). 그래프는 간선과 가리키는 문제가 있는지만, 번호표는 그 문제의 번호만
 *   넣는다 — 문제를 하나 더해도 다른 문제의 키는 그대로다.
 * - 검증기 자신: 클래스패스의 모든 파일(하네스·컴파일러 포함)과 JDK·python 판. 검증 코드나 툴체인이 바뀌면
 *   전부 다시 잰다.
 *
 * **통과한 보고서만 담는다.** 실패는 매번 다시 돈다 — 고쳤는지 보려고 돌리는 것이니까.
 *
 * 남는 위험은 성능 여유다. 지난번 머신에서 3배를 넘긴 오답이 더 빠른 머신에서 못 넘길 수 있는데, 캐시는
 * 다시 재지 않는다. 그래서 캐시 없이 전부 다시 재는 실행을 주기적으로 따로 둔다(`.github/workflows/content-full.yml`).
 */
class ValidationCache(private val dir: Path, private val validatorFingerprint: String) {

    private val json = ObjectMapper().registerKotlinModule()
    private val used = mutableSetOf<String>()

    init {
        dir.createDirectories()
    }

    /** 키에 맞는 통과 보고서. 없거나 읽을 수 없으면 null — 그러면 검증한다. */
    fun find(key: String): ValidationReport? {
        val file = dir.resolve("$key.json")
        if (!file.isRegularFile()) return null
        val report = runCatching { json.readValue<ValidationReport>(file.readText()) }.getOrNull() ?: return null
        if (!report.passed || report.validatorVersion != ContentValidator.VALIDATOR_VERSION) return null
        used += key
        return report
    }

    fun store(key: String, report: ValidationReport) {
        if (!report.passed) return
        dir.resolve("$key.json").writeText(json.writeValueAsString(report))
        used += key
    }

    /** 이번 실행에서 쓰지 않은 항목을 지운다 — 캐시는 지금 저장소의 패키지만큼만 남는다. */
    fun prune(): Int {
        val stale = dir.listDirectoryEntries("*.json").filter { it.name.removeSuffix(".json") !in used }
        stale.forEach { it.deleteExisting() }
        return stale.size
    }

    fun key(kind: String, id: String, packageFingerprint: String, sharedFingerprint: String): String =
        sha256(listOf(validatorFingerprint, kind, id, packageFingerprint, sharedFingerprint).joinToString("\n"))

    companion object {
        /** 디렉터리 아래 모든 파일의 경로와 내용. 시각·권한은 넣지 않는다 — 내용이 같으면 같은 키다. */
        fun directoryFingerprint(root: Path): String {
            val digest = MessageDigest.getInstance("SHA-256")
            Files.walk(root).use { paths ->
                paths.filter { it.isRegularFile() && it.name != ".DS_Store" && "__pycache__" !in it.toString() }
                    .map { it.relativeTo(root).toString().replace(File.separatorChar, '/') to it }
                    .sorted(compareBy { it.first })
                    .forEach { (relative, file) ->
                        digest.update(relative.toByteArray())
                        digest.update(0)
                        digest.update(file.readBytes())
                        digest.update(0)
                    }
            }
            return hex(digest.digest())
        }

        /**
         * 패키지 밖에서 검증이 읽는 것. [edges] 는 문제 id → 선수 문제 id 들, [exists] 는 가리킨 문제가 있는가.
         */
        fun sharedFingerprint(tags: String, edges: Map<String, List<String>>, exists: (String) -> Boolean): String {
            val graph = edges.filterValues { it.isNotEmpty() }.toSortedMap()
                .map { (id, prerequisites) -> "$id<-${prerequisites.sorted().joinToString(",")}" }
            val targets = edges.values.flatten().toSortedSet().map { "$it=${exists(it)}" }
            return sha256((listOf(tags) + graph + targets).joinToString("\n"))
        }

        /**
         * 검증기의 지문 — 지금 JVM 의 클래스패스(디렉터리면 그 아래 파일까지)와 툴체인 판.
         * Gradle 이 넘기는 클래스 디렉터리의 내용은 같은 소스에서 같으므로, 소스가 그대로면 지문도 그대로다.
         */
        fun validatorFingerprint(
            classpath: String = System.getProperty("java.class.path"),
            toolchain: String = toolchain(),
        ): String {
            val digest = MessageDigest.getInstance("SHA-256")
            digest.update(toolchain.toByteArray())
            classpath.split(File.pathSeparatorChar).filter(String::isNotBlank).sorted().forEach { entry ->
                val path = Path.of(entry)
                when {
                    path.isDirectory() -> digest.update(directoryFingerprint(path).toByteArray())
                    path.exists() -> digest.update(path.readBytes())
                }
            }
            return hex(digest.digest())
        }

        private fun toolchain(): String {
            val python = runCatching {
                val process = ProcessBuilder("python3", "--version").redirectErrorStream(true).start()
                process.waitFor(10, TimeUnit.SECONDS)
                process.inputStream.bufferedReader().readText().trim()
            }.getOrDefault("python3 없음")
            return listOf(System.getProperty("java.vendor"), System.getProperty("java.version"), python).joinToString(" | ")
        }

        private fun sha256(text: String) = hex(MessageDigest.getInstance("SHA-256").digest(text.toByteArray()))

        private fun hex(bytes: ByteArray) = bytes.joinToString("") { "%02x".format(it) }
    }
}
