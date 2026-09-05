package dev.codedrill.platform.problempackage

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readText

/**
 * 디렉터리에 놓인 문제 패키지를 읽는다 (기술 설계서 §6.1, §6.3).
 *
 * 운영에서는 검증·서명된 불변 번들을 아티팩트 스토어에서 내려받지만, 슬라이스에서는
 * 저장소의 `content/problems/<id>` 를 그대로 읽는다. 로딩 시 스키마와 참조 무결성을
 * 검사해 깨진 패키지가 채점에 들어가지 못하게 막는다.
 */
class ProblemPackageLoader(private val root: Path) {

    private val yaml = ObjectMapper(YAMLFactory()).registerKotlinModule()
    private val json = ObjectMapper().registerKotlinModule()

    fun load(problemId: String): ProblemPackage = try {
        read(problemId)
    } catch (e: InvalidProblemPackageException) {
        throw e
    } catch (e: IllegalArgumentException) {
        throw InvalidProblemPackageException(problemId, e.message ?: "검증 실패", e)
    } catch (e: com.fasterxml.jackson.core.JacksonException) {
        // 파서 예외의 원인이 우리 require() 인 경우가 많다. 원인의 메시지를 살려 올린다.
        throw InvalidProblemPackageException(problemId, e.cause?.message ?: "파싱 실패", e)
    }

    private fun read(problemId: String): ProblemPackage {
        val dir = root.resolve(problemId)
        require(dir.isDirectory()) { "문제 디렉터리가 없다: $dir" }

        val manifestText = dir.resolve("manifest.yaml").readText()
        val manifest = yaml.readValue<ProblemManifest>(manifestText)
        require(manifest.id == problemId) {
            "manifest id 와 디렉터리 이름이 다르다: ${manifest.id} != $problemId"
        }

        val statement = dir.resolve(manifest.statement).readText()

        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(manifestText.toByteArray())

        val groups = manifest.groups.map { policy ->
            val groupDir = dir.resolve("tests").resolve(policy.id)
            require(groupDir.isDirectory()) { "그룹 ${policy.id} 의 테스트 디렉터리가 없다" }

            // 파일 순서가 digest 를 바꾸지 않도록 이름으로 정렬한다.
            val cases = groupDir.listDirectoryEntries("*.json")
                .filter { it.isRegularFile() }
                .sortedBy { it.name }
                .map { file ->
                    val text = file.readText()
                    digest.update(file.name.toByteArray())
                    digest.update(text.toByteArray())
                    val raw = json.readValue<RawCase>(text)
                    TestCase(
                        id = file.name.removeSuffix(".json"),
                        groupId = policy.id,
                        args = raw.args,
                        expected = raw.expected,
                    )
                }

            require(cases.isNotEmpty()) { "그룹 ${policy.id} 에 테스트가 없다" }
            cases.forEach { case ->
                require(case.args.size == manifest.signature.parameters.size) {
                    "${case.id}: 인자 개수가 시그니처와 다르다"
                }
            }
            TestGroup(policy, cases)
        }

        return ProblemPackage(
            manifest = manifest,
            statementMarkdown = statement,
            groups = groups,
            packageDigest = digest.digest().joinToString("") { "%02x".format(it) },
        )
    }

    private data class RawCase(val args: List<Any>, val expected: Any)
}
