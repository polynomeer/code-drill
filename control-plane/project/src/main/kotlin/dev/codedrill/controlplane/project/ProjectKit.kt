package dev.codedrill.controlplane.project

import com.fasterxml.jackson.databind.ObjectMapper
import dev.codedrill.judge.protocol.Workspaces
import dev.codedrill.platform.problempackage.ProjectHarness
import dev.codedrill.platform.problempackage.ProjectPackage
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 개인 IDE 로 받아 가는 꾸러미 (feature-roadmap 11단계 이어서 — 로컬에서 풀기 1단계).
 *
 * 들어가는 것: 시작 저장소, 공개 테스트(시작 저장소에 있다), **채점기와 같은 하네스**([ProjectHarness]),
 * 로컬 실행기, 판 정보, IDE 가 바로 여는 빌드 파일. **들어가지 않는 것**: 숨은 테스트·참조·오답 — 패키지의
 * `hidden` 은 여기서 읽지도 않는다. 상세 API 가 이미 시작 저장소를 공개하므로 이것도 공개다.
 *
 * 같은 패키지(digest)와 같은 [VERSION] 이면 바이트가 같다 — 항목 순서와 시각을 고정한다. 그래서 ETag 로 캐시된다.
 */
object ProjectKit {

    /** 키트 모양의 판. 실행기·빌드 파일·문서를 바꾸면 올린다 — ETag 가 갈린다 */
    const val VERSION = 1

    /** 웹의 "폴더 가져오기"가 빼고 읽는 키트 파일. web/src/features/project/kitFiles.ts 와 같아야 한다 */
    val KIT_PATHS = listOf(".codedrill/", "CODEDRILL.md", "build.gradle.kts", "settings.gradle.kts", ".gitignore")

    private val json = ObjectMapper()

    fun fileName(pkg: ProjectPackage): String = "${pkg.manifest.id}-v${pkg.manifest.version}.zip"

    fun etag(pkg: ProjectPackage): String = "\"kit$VERSION-${pkg.packageDigest.take(16)}\""

    /** 경로 → 내용. 첫 디렉터리는 프로젝트 id 다 (풀면 폴더 하나) */
    fun files(pkg: ProjectPackage): Map<String, String> {
        val language = pkg.manifest.language.uppercase()
        val out = sortedMapOf<String, String>()
        out.putAll(pkg.starter)
        for ((name, content) in ProjectHarness.files(language)) out[".codedrill/harness/$name"] = content
        out[".codedrill/project.json"] = projectJson(pkg)
        when (language) {
            "PYTHON" -> out[".codedrill/run.py"] = resource("run.py")
            "JAVA" -> {
                out[".codedrill/Run.java"] = resource("Run.java")
                putGradle(out, pkg, java = true)
            }
            "KOTLIN" -> putGradle(out, pkg, java = false)
        }
        if (".gitignore" !in pkg.starter) out[".gitignore"] = resource("gitignore")
        out["CODEDRILL.md"] = readme(pkg)
        return out.mapKeys { (path, _) -> "${pkg.manifest.id}/$path" }
    }

    fun zip(pkg: ProjectPackage): ByteArray {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            for ((path, content) in files(pkg)) {
                // 시각을 고정해야 같은 패키지의 키트가 같은 바이트다
                zip.putNextEntry(ZipEntry(path).apply { timeLocal = FIXED_TIME })
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
        return bytes.toByteArray()
    }

    private fun projectJson(pkg: ProjectPackage): String {
        val limits = pkg.manifest.limits
        return json.writerWithDefaultPrettyPrinter().writeValueAsString(
            linkedMapOf(
                "kit" to VERSION,
                "id" to pkg.manifest.id,
                "version" to pkg.manifest.version,
                "title" to pkg.manifest.title,
                "language" to pkg.manifest.language.uppercase(),
                "packageDigest" to pkg.packageDigest,
                "limits" to linkedMapOf(
                    "buildSeconds" to limits.buildSeconds,
                    "testSeconds" to limits.testSeconds,
                    "memoryMb" to limits.memoryMb,
                    "maxFiles" to Workspaces.MAX_FILES,
                    "maxTotalBytes" to Workspaces.MAX_TOTAL_BYTES,
                ),
                "publicTests" to pkg.publicModules.sorted(),
            ),
        ) + "\n"
    }

    private fun putGradle(out: MutableMap<String, String>, pkg: ProjectPackage, java: Boolean) {
        val public = pkg.publicModules.sorted().joinToString(", ") { "\"$it\"" }
        out["build.gradle.kts"] = resource("build.gradle.kts.template")
            .replace("@@PLUGINS@@", if (java) "plugins { java }" else "plugins { kotlin(\"jvm\") version \"$KOTLIN_VERSION\" }")
            .replace(
                "@@SOURCES@@",
                if (java) "java.setSrcDirs(listOf(\"src\", \"tests\", \".codedrill/harness\"))"
                else "kotlin.setSrcDirs(listOf(\"src\", \"tests\", \".codedrill/harness\"))",
            )
            .replace("@@CLASSES@@", if (java) "classes/java/main" else "classes/kotlin/main")
            .replace("@@MAIN@@", if (java) "codedrill.CodedrillHarness" else "codedrill.CodedrillHarnessKt")
            .replace("@@MEMORY@@", pkg.manifest.limits.memoryMb.toString())
            .replace("@@PUBLIC@@", public)
        out["settings.gradle.kts"] = "rootProject.name = \"${pkg.manifest.id}\"\n"
    }

    private fun readme(pkg: ProjectPackage): String {
        val language = pkg.manifest.language.uppercase()
        val run = when (language) {
            "PYTHON" -> "```sh\npython3 .codedrill/run.py\n```\n\nPython 3.10 이상. 표준 라이브러리 `unittest` 만 씁니다."
            "JAVA" -> "```sh\njava .codedrill/Run.java\n```\n\nJDK 17 이상. IntelliJ 로 이 폴더를 열면 Gradle 프로젝트로 읽히고, Gradle 창의 `codedrill › codedrillTest` 로도 돕니다 (Gradle 은 JDK 17~21 로 — 더 새 JDK 에서는 Gradle 의 Kotlin 스크립트가 읽히지 않습니다)."
            else -> "IntelliJ 로 이 폴더를 열면 Gradle 프로젝트로 읽힙니다. Gradle 창의 `codedrill › codedrillTest` 를 실행하거나:\n\n```sh\ngradle codedrillTest\n```\n\nKotlin $KOTLIN_VERSION · JDK 17~21 (더 새 JDK 에서는 Gradle 의 Kotlin 스크립트가 읽히지 않습니다). 처음 한 번은 Kotlin 플러그인을 내려받습니다."
        }
        val limits = pkg.manifest.limits
        return resource("CODEDRILL.md.template")
            .replace("@@TITLE@@", pkg.manifest.title)
            .replace("@@ID@@", pkg.manifest.id)
            .replace("@@VERSION@@", pkg.manifest.version.toString())
            .replace("@@LANGUAGE_LABEL@@", LANGUAGE_LABEL[language] ?: language)
            .replace("@@RUN@@", run)
            .replace("@@BUILD@@", limits.buildSeconds.toString())
            .replace("@@TEST@@", limits.testSeconds.toString())
            .replace("@@MEMORY@@", limits.memoryMb.toString())
            .replace("@@MAX_FILES@@", Workspaces.MAX_FILES.toString())
            .replace("@@MAX_KB@@", (Workspaces.MAX_TOTAL_BYTES / 1024).toString())
    }

    private fun resource(name: String): String =
        ProjectKit::class.java.getResourceAsStream("/project-kit/$name")?.bufferedReader()?.readText()
            ?: error("키트 리소스가 없다: /project-kit/$name")

    /** 채점기의 Kotlin 판과 같다 (gradle/libs.versions.toml). 로컬 결과가 채점과 갈리지 않게 */
    private const val KOTLIN_VERSION = "2.1.20"

    private val LANGUAGE_LABEL = mapOf("PYTHON" to "Python", "KOTLIN" to "Kotlin", "JAVA" to "Java")

    /** ZIP 이 담을 수 있는 가장 이른 시각 */
    private val FIXED_TIME: LocalDateTime = LocalDateTime.of(1980, 1, 1, 0, 0)
}
