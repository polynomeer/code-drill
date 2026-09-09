package dev.codedrill.judge.runner.execution

import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

/**
 * 자식 JVM 이 쓸 Kotlin 런타임 위치를 찾는다.
 *
 * 슬라이스는 Runner 프로세스의 클래스패스에 이미 있는 stdlib 을 그대로 재사용한다.
 * 운영에서는 runtime_manifest 의 image_digest 가 이 경로를 고정한다 (§5.5).
 */
object RuntimeClasspath {

    val kotlinStdlib: Path by lazy { locate("kotlin.jvm.internal.Intrinsics") }

    val annotations: Path by lazy { locate("org.jetbrains.annotations.NotNull") }

    val all: List<Path> get() = listOf(kotlinStdlib, annotations).filter { it.exists() }

    /**
     * 같은 jar 를 [root] 아래로 옮겨 담고 그 경로를 돌려준다.
     *
     * **Runner 가 컨테이너 안에서 돌 때 필요하다.** 이 jar 들은 Runner 이미지 안에
     * 있는데, 샌드박스 컨테이너에 붙일 때 그 경로를 해석하는 것은 Runner 가 아니라
     * 호스트 데몬이다. 데몬이 보기에 `/app/runner-agent/lib/...` 는 없는 경로이므로
     * 빈 디렉터리를 만들어 붙이고, Kotlin 제출은 stdlib 없이 돌다가 전부
     * SYSTEM_ERROR 가 된다. 실행 디렉터리만 맞춰 두고 이것을 빼먹었다가 실제로 그랬다.
     *
     * [root] 는 호스트와 이름이 같은 디렉터리다 (`codedrill.sandbox.work-root`).
     * 거기로 옮기면 안과 밖이 같은 곳을 가리킨다.
     */
    fun sharedInto(root: Path): List<Path> {
        val runtime = root.resolve("runtime").also { it.createDirectories() }
        return all.map { jar ->
            val target = runtime.resolve(jar.fileName.toString())
            // 매 기동마다 덮어쓴다. 이미지를 올렸는데 지난 jar 가 남아 있으면, 판정은
            // 새 이미지로 도는데 stdlib 만 옛것인 조합이 된다.
            jar.copyTo(target, StandardCopyOption.REPLACE_EXISTING)
            target
        }
    }

    private fun locate(className: String): Path {
        val type = Class.forName(className)
        val source = type.protectionDomain?.codeSource?.location
            ?: error("$className 의 코드 소스를 찾지 못했다")
        return Path.of(source.toURI())
    }

    /** 자식 JVM 실행에 쓸 java 바이너리. Runner 와 같은 JVM 을 쓴다. */
    val javaBinary: Path by lazy {
        Path.of(System.getProperty("java.home"), "bin", "java")
    }
}
