package dev.codedrill.judge.runner.execution

import dev.codedrill.judge.protocol.Language
import dev.codedrill.judge.runner.execution.adapter.caseFileName
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.copyToRecursively
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readBytes

/**
 * 컴파일 산출물 캐시 (§5.3 compile).
 *
 * 아레나·축소·실험실은 **같은 소스를 입력만 바꿔** 수십 번 돌린다. 축소 한 라운드가 참조
 * 풀이와 오답을 한 번씩 다시 돌리고, 시도 하나가 오답마다 그것을 몇 라운드 한다. 그때마다
 * Kotlin 을 다시 컴파일하면(3초 남짓) 실행(0.5초 남짓)보다 컴파일이 일곱 배 길고, 오답 넷짜리
 * 아레나 시도가 부하가 걸린 머신에서 2분을 넘겼다.
 *
 * 키는 **컴파일러가 읽는 파일의 내용**이다 — 언어와, 케이스 파일을 뺀 소스 디렉터리 전부.
 * 하네스·계측 SDK 도 그 안에 있으므로 시그니처나 모드(TRACE)가 달라지면 키가 달라진다.
 * 케이스 파일([caseFileName])은 실행이 읽는 데이터라 키에 넣지 않는다; 넣으면 축소의 후보마다
 * 키가 달라져 캐시가 쓸모없다.
 *
 * 산출물은 **복사해 들이고 복사해 낸다.** 실행 디렉터리를 캐시와 공유하면 한 실행이 산출물을
 * 건드렸을 때 다음 실행이 그것을 물려받는다. 같은 키는 같은 입력이므로, 다른 사용자의 같은
 * 소스에 산출물을 내주는 것은 다시 컴파일하는 것과 같다.
 *
 * 실패한 컴파일은 담지 않는다. 다시 실패할 뿐이고, 실패 로그는 산출물이 아니다.
 *
 * 프로세스 안에서만 산다. 컴파일러나 런타임이 바뀌는 것은 Runner 를 다시 띄울 때뿐이고,
 * 그때 캐시는 새 임시 디렉터리에서 비어 시작한다.
 */
@OptIn(ExperimentalPathApi::class)
class CompileCache(
    private val root: Path = Files.createTempDirectory("codedrill-compiled-"),
    private val capacity: Int = CAPACITY,
) {

    private val entries = object : LinkedHashMap<String, Path>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Path>): Boolean {
            if (size <= capacity) return false
            eldest.value.deleteRecursively()
            return true
        }
    }

    fun key(language: Language, sourceDir: Path): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(language.name.toByteArray())
        sourceDir.listDirectoryEntries()
            .filter { it.isRegularFile() && !isCaseFile(it) }
            .sortedBy { it.name }
            .forEach { file ->
                digest.update(0)
                digest.update(file.name.toByteArray())
                digest.update(0)
                digest.update(file.readBytes())
            }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /** 담아 둔 산출물이 있으면 [outputDir] 에 복사하고 참이다. */
    @Synchronized
    fun restore(key: String, outputDir: Path): Boolean {
        val cached = entries[key] ?: return false
        cached.copyToRecursively(outputDir, followLinks = false, overwrite = true)
        return true
    }

    /** 방금 성공한 컴파일의 산출물을 담는다. */
    @Synchronized
    fun store(key: String, outputDir: Path) {
        if (key in entries) return
        val target = root.resolve(key)
        target.deleteRecursively()
        target.createDirectories()
        outputDir.copyToRecursively(target, followLinks = false, overwrite = true)
        entries[key] = target
    }

    private fun isCaseFile(file: Path) = file.name.startsWith(CASE_PREFIX) && file.name.endsWith(CASE_SUFFIX)

    private companion object {
        /** 아레나 시도 하나가 쓰는 소스는 참조와 오답 몇 개다. 동시에 도는 시도 몇 개를 넉넉히 덮는다. */
        const val CAPACITY = 64

        // caseFileName("x") == "cases_x.txt"
        val CASE_PREFIX = caseFileName("").substringBefore(".")
        val CASE_SUFFIX = "." + caseFileName("").substringAfterLast(".")
    }
}
