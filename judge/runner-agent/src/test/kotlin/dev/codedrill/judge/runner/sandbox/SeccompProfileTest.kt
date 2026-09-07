package dev.codedrill.judge.runner.sandbox

import dev.codedrill.judge.protocol.Language
import dev.codedrill.judge.runner.execution.sandbox.SeccompProfile
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * seccomp allowlist 자체의 회귀 (기술 설계서 §5.2, §11.4).
 *
 * 컨테이너 없이 도는 테스트다. [SandboxRegressionTest] 는 실제로 막히는지를 보고,
 * 여기서는 **목록에 무엇이 있으면 안 되는지**를 본다. 두 검사가 필요한 이유는 컨테이너
 * 런타임이 없는 빌드에서도 목록이 잘못 자라는 것은 잡아야 하기 때문이다.
 */
class SeccompProfileTest {

    @Test
    fun `금지된 시스템 호출은 어느 언어의 목록에도 없다`() {
        for (language in Language.entries) {
            val allowed = SeccompProfile.forLanguage(language).toSet()
            val leaked = SeccompProfile.FORBIDDEN.intersect(allowed)

            assertEquals(
                emptySet(), leaked,
                "$language 프로파일이 금지된 호출을 허용한다. 편의를 위해 추가했다면 되돌린다",
            )
        }
    }

    @Test
    fun `세 언어 모두 실행에 필요한 최소한을 갖춘다`() {
        // 하나라도 빠지면 런타임이 기동조차 못 한다. 목록을 줄이다 실수하면 여기서 걸린다.
        val essential = listOf(
            "read", "write", "openat", "close", "mmap", "munmap", "mprotect",
            "futex", "exit_group", "clock_gettime", "execve",
        )
        for (language in Language.entries) {
            val allowed = SeccompProfile.forLanguage(language).toSet()
            assertTrue(
                allowed.containsAll(essential),
                "$language 에 빠진 것: ${essential.filterNot(allowed::contains)}",
            )
        }
    }

    @Test
    fun `기본 동작은 거부다`() {
        val json = SeccompProfile.toJson(Language.KOTLIN)

        assertTrue(
            json.contains("\"defaultAction\": \"SCMP_ACT_ERRNO\""),
            "allowlist 는 기본이 거부여야 한다. 허용이 기본이면 목록은 장식일 뿐이다",
        )
        // 죽이지 않고 EPERM 을 돌려준다. 목록이 조금 모자랄 때 죽이면 멀쩡한 제출이
        // 사용자 코드 탓처럼 실패한다.
        assertTrue(json.contains("\"defaultErrnoRet\": 1"))
    }

    @Test
    fun `clone 은 새 네임스페이스를 만들지 못하게 걸러 허용한다`() {
        val json = SeccompProfile.toJson(Language.PYTHON)

        // 스레드는 만들어야 하고 네임스페이스는 못 만들게 해야 한다. clone 을 통째로
        // 허용하면 두 번째가 무너지고, 통째로 막으면 첫 번째가 무너진다.
        assertTrue(json.contains("\"names\": [\"clone\"]"))
        assertTrue(json.contains("SCMP_CMP_MASKED_EQ"))
        assertTrue(
            json.contains("\"value\": 2114060288"),
            "네임스페이스 플래그 마스크가 바뀌었다. 무엇을 막고 있는지 다시 확인한다",
        )
    }

    /** 손으로 도커에 물려 확인할 수 있도록 프로파일을 빌드 디렉터리에도 남긴다. */
    @Test
    fun `프로파일을 빌드 산출물로 남긴다`() {
        val written = SeccompProfile.materialize(java.nio.file.Path.of("build/seccomp"))
        assertEquals(Language.entries.toSet(), written.keys)
    }

    @Test
    fun `내보낸 프로파일은 언어마다 따로 있다`(@org.junit.jupiter.api.io.TempDir directory: java.nio.file.Path) {
        val written = SeccompProfile.materialize(directory)

        assertEquals(Language.entries.toSet(), written.keys)
        for ((language, path) in written) {
            assertTrue(path.readText().contains("SCMP_ACT_ERRNO"), "$language 프로파일이 비어 있다")
        }
    }
}
