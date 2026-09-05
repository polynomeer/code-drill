package dev.codedrill.judge.runner.execution

import java.nio.file.Path
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.config.Services
import kotlin.io.path.absolutePathString

/**
 * 사용자 소스와 하네스를 JVM 클래스로 컴파일한다 (기술 설계서 §5.3 compile).
 *
 * 컴파일은 Runner 프로세스 안에서 수행한다. 위험한 것은 실행이지 컴파일이 아니며,
 * 컴파일러를 in-process 로 두면 로컬 `kotlinc` 설치에 기대지 않아도 된다. 운영에서는
 * 이 단계도 고정 digest 의 런타임 이미지 안에서 돈다 (§5.5).
 */
class KotlinSourceCompiler(private val stdlibJars: List<Path>) {

    fun compile(sourceDir: Path, outputDir: Path): CompileOutcome {
        val collector = CollectingMessageCollector()
        val arguments = K2JVMCompilerArguments().apply {
            freeArgs = listOf(sourceDir.absolutePathString())
            destination = outputDir.absolutePathString()
            classpath = stdlibJars.joinToString(java.io.File.pathSeparator) { it.absolutePathString() }
            noStdlib = true
            noReflect = true
            // 채점 결과가 컴파일러 경고 정책에 흔들리지 않도록 경고는 판정에 쓰지 않는다.
            suppressWarnings = true
        }

        val exitCode = K2JVMCompiler().exec(collector, Services.EMPTY, arguments)
        return if (exitCode == ExitCode.OK) {
            CompileOutcome.Success
        } else {
            CompileOutcome.Failure(collector.errors())
        }
    }

    sealed interface CompileOutcome {
        data object Success : CompileOutcome

        /** 사용자에게 그대로 보여줄 수 있는 컴파일 로그. 내부 경로는 담지 않는다. */
        data class Failure(val log: String) : CompileOutcome
    }

    private class CollectingMessageCollector : MessageCollector {
        private val messages = mutableListOf<String>()
        private var hasErrors = false

        override fun clear() {
            messages.clear()
            hasErrors = false
        }

        override fun hasErrors(): Boolean = hasErrors

        override fun report(
            severity: CompilerMessageSeverity,
            message: String,
            location: CompilerMessageSourceLocation?,
        ) {
            if (!severity.isError) return
            hasErrors = true
            // 파일 절대 경로는 빼고 줄·칸만 남긴다. 사용자에게 서버 경로를 노출하지 않는다.
            val where = location?.let { "(${it.line}:${it.column}) " } ?: ""
            messages += "$where$message"
        }

        fun errors(): String = messages.joinToString("\n").take(MAX_LOG_CHARS)
    }

    private companion object {
        const val MAX_LOG_CHARS = 8_000
    }
}
