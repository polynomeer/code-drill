package dev.codedrill.platform.problempackage

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.relativeTo
import kotlin.streams.asSequence

/**
 * 디렉터리에 놓인 프로젝트형 문제를 읽는다 ([ProjectPackage] 에 모양이 있다).
 *
 * [ProblemPackageLoader] 와 같은 자리다 — 제어 영역은 화면에 낼 것을, 오케스트레이터는
 * 채점에 보낼 것을 여기서 읽고, 둘이 같은 digest 를 본다.
 */
class ProjectPackageLoader(private val root: Path) {

    private val yaml = ObjectMapper(YAMLFactory()).registerKotlinModule()

    fun load(projectId: String): ProjectPackage = try {
        read(projectId)
    } catch (e: InvalidProblemPackageException) {
        throw e
    } catch (e: IllegalArgumentException) {
        throw InvalidProblemPackageException(projectId, e.message ?: "검증 실패", e)
    } catch (e: com.fasterxml.jackson.core.JacksonException) {
        throw InvalidProblemPackageException(projectId, e.cause?.message ?: "파싱 실패", e)
    }

    /** 이 루트에 있는 프로젝트 id 전부. manifest 가 있는 디렉터리만 센다. */
    fun ids(): List<String> {
        if (!root.isDirectory()) return emptyList()
        return root.listDirectoryEntries()
            .filter { it.resolve("manifest.yaml").isRegularFile() }
            .map { it.name }
            .sorted()
    }

    private fun read(projectId: String): ProjectPackage {
        val dir = root.resolve(projectId)
        require(dir.isDirectory()) { "프로젝트 디렉터리가 없다: $dir" }

        val manifestText = dir.resolve("manifest.yaml").readText()
        val manifest = yaml.readValue<ProjectManifest>(manifestText)
        require(manifest.id == projectId) { "manifest id 와 디렉터리 이름이 다르다: ${manifest.id} != $projectId" }

        val statement = dir.resolve(manifest.statement).readText()
        val catalogFile = dir.resolve("catalog.yaml")
        require(catalogFile.isRegularFile()) { "catalog.yaml 이 없다" }
        val catalog = yaml.readValue<ProjectCatalog>(catalogFile.readText())

        val starter = files(dir.resolve("starter"))
        require(starter.isNotEmpty()) { "starter/ 가 비어 있다. 사용자가 받을 파일이 없다" }
        val hidden = files(dir.resolve("hidden"))
        require(hidden.isNotEmpty()) { "hidden/ 이 비어 있다. 숨은 테스트가 없으면 채점할 것이 없다" }

        // 순서가 digest 를 바꾸지 않도록 경로로 정렬한다 (files 가 정렬해 준다).
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(manifestText.toByteArray())
        for ((path, content) in starter) {
            digest.update("starter/$path".toByteArray()); digest.update(content.toByteArray())
        }
        for ((path, content) in hidden) {
            digest.update("hidden/$path".toByteArray()); digest.update(content.toByteArray())
        }

        return ProjectPackage(
            manifest = manifest,
            statementMarkdown = statement,
            catalog = catalog,
            starter = starter,
            hidden = hidden,
            packageDigest = digest.digest().joinToString("") { "%02x".format(it) },
        )
    }

    /**
     * 참조 구현 (`reference/`). starter 위에 덮는 파일들이다.
     *
     * 패키지에 담지 않고 따로 읽는다 — [ProblemPackageLoader.referenceSolution] 과 같은 이유다.
     */
    fun reference(projectId: String): Map<String, String>? =
        root.resolve(projectId).resolve("reference").takeIf { it.isDirectory() }?.let(::files)

    /**
     * 대표 오답 (`mutants/<name>/`). 각각 starter 위에 덮는 파일 묶음이다.
     *
     * 종류는 묶음 안 아무 파일의 `# kind:` 줄에서 읽고, 설명은 그 아래 주석이다 —
     * 알고리즘 문제의 오답 파일과 같은 약속이다.
     */
    fun mutants(projectId: String): List<ProjectMutant> {
        val dir = root.resolve(projectId).resolve("mutants")
        if (!dir.isDirectory()) return emptyList()
        return dir.listDirectoryEntries()
            .filter { it.isDirectory() }
            .sortedBy { it.name }
            .map { mutant ->
                val overlay = files(mutant)
                val tagged = overlay.values.firstOrNull { DefectKind.readFrom(it) != DefectKind.UNSPECIFIED }
                ProjectMutant(
                    name = mutant.name,
                    kind = tagged?.let(DefectKind::readFrom) ?: DefectKind.UNSPECIFIED,
                    overlay = overlay,
                    note = tagged?.let(::noteOf).orEmpty(),
                )
            }
    }

    fun editorial(projectId: String): String? =
        root.resolve(projectId).resolve("editorial.md").takeIf { it.isRegularFile() }?.readText()

    /** 디렉터리 아래 파일 전부를 루트 상대 경로로. 텍스트만 — 프로젝트형은 소스 파일의 묶음이다. */
    private fun files(dir: Path): Map<String, String> {
        if (!dir.exists()) return emptyMap()
        return Files.walk(dir).use { stream ->
            stream.asSequence()
                .filter { it.isRegularFile() }
                .filterNot { path -> path.relativeTo(dir).any { it.name.startsWith(".") || it.name == "__pycache__" } }
                .map { it.relativeTo(dir).joinToString("/") to it.readText() }
                .sortedBy { it.first }
                .toMap(linkedMapOf())
        }
    }

    private fun noteOf(source: String): String = source.lineSequence()
        .map { it.trim() }
        .dropWhile { !it.matches(Regex("""(//|#)\s*kind:.*""")) }
        .drop(1)
        .takeWhile { it.startsWith("//") || it.startsWith("#") }
        .map { it.removePrefix("//").removePrefix("#").trim() }
        .filter { it.isNotEmpty() }
        .joinToString(" ")
}

/** 프로젝트형의 대표 오답 하나. [overlay] 는 사용자에게 절대 나가지 않는다. */
data class ProjectMutant(
    val name: String,
    val kind: DefectKind,
    val overlay: Map<String, String>,
    val note: String = "",
)
