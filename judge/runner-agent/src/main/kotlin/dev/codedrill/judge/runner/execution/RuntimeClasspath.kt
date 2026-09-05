package dev.codedrill.judge.runner.execution

import java.nio.file.Path
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
