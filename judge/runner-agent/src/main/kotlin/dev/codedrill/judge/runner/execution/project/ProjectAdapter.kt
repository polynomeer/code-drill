package dev.codedrill.judge.runner.execution.project

import dev.codedrill.judge.protocol.Language
import dev.codedrill.judge.runner.execution.adapter.PythonAdapter
import java.nio.file.Path
import kotlin.io.path.absolutePathString

/**
 * 언어마다 다른 것 — 프로젝트를 어떻게 빌드하고 어떻게 스위트를 돌리나.
 *
 * [dev.codedrill.judge.runner.execution.adapter.RuntimeAdapter] 의 짝이지만 훨씬 얇다.
 * 저기는 시그니처로 하네스를 **생성**하고 케이스 프로토콜을 주고받는데, 여기는 명령 둘과
 * 하네스 파일 하나다. 스위트가 무엇을 시험하는지는 하네스가 아니라 숨은 테스트가 정한다.
 */
interface ProjectAdapter {

    val language: Language

    /** 샌드박스에 함께 넣을 파일 (이름 → 내용). 하네스가 여기 있다. 사용자 파일과 섞이지 않는다. */
    fun harnessFiles(): Map<String, String>

    /**
     * 빌드 명령. 실패하면 [dev.codedrill.judge.protocol.Verdict.COMPILE_ERROR] 다.
     *
     * null 이면 빌드 단계가 없다. Python 도 null 이 아니다 — 문법 검사가 빌드다. 문법이
     * 깨진 파일은 테스트 로딩에서 import 오류로 나타나고, 그것은 "테스트 전부 실패"가
     * 아니라 "컴파일 오류"여야 한다.
     */
    fun buildCommand(workspace: Path): List<String>?

    /**
     * 스위트 실행 명령. 리포트를 [reportFile] 에 적어야 한다 ([ProjectReport] 의 모양).
     *
     * [nonceFile] 은 하네스가 사용자 코드를 들이기 전에 읽고 지워야 하는 파일이다. 리포트에 그
     * 값이 실려야 채점기가 믿는다 — 사용자 코드가 꾸며 쓴 리포트에는 그 값이 없다.
     */
    fun testCommand(workspace: Path, harnessDir: Path, outputDir: Path, reportFile: Path, nonceFile: Path, memoryMb: Int): List<String>

    fun env(): Map<String, String> = emptyMap()

    /** 샌드박스에 읽기 전용으로 들여보낼 경로 — 런타임·컴파일러 jar. */
    fun readOnlyPaths(): List<Path> = emptyList()

    /** 빌드 단계의 메모리. 컴파일러가 문제의 한도보다 더 쓸 수 있다 — 그 몫은 사용자의 것이 아니다. */
    fun buildMemoryMb(limitMb: Int): Int = limitMb
}

/**
 * Python 프로젝트. 테스트 기반은 `unittest` — 표준 라이브러리라 이미지에 있고, 사용자가
 * 의존성을 더할 길이 없다 (샌드박스에 네트워크가 없다).
 */
class PythonProjectAdapter(private val interpreter: String = PythonAdapter.DEFAULT_INTERPRETER) : ProjectAdapter {

    override val language = Language.PYTHON

    override fun harnessFiles(): Map<String, String> = mapOf(HARNESS to harnessSource)

    /**
     * 워크스페이스의 모든 .py 를 컴파일해 본다. 바이트코드는 쓰지 않는다 — 소스 디렉터리가
     * 읽기 전용이다.
     */
    override fun buildCommand(workspace: Path): List<String> = listOf(
        interpreter, "-B", "-c", SYNTAX_CHECK, workspace.absolutePathString(),
    )

    override fun testCommand(workspace: Path, harnessDir: Path, outputDir: Path, reportFile: Path, nonceFile: Path, memoryMb: Int): List<String> = listOf(
        interpreter, "-B",
        harnessDir.resolve(HARNESS).absolutePathString(),
        workspace.absolutePathString(),
        reportFile.absolutePathString(),
        memoryMb.toString(),
        nonceFile.absolutePathString(),
    )

    override fun env() = PythonAdapter.DETERMINISM_ENV

    private val harnessSource: String by lazy {
        PythonProjectAdapter::class.java.getResourceAsStream("/project/$HARNESS")
            ?.bufferedReader()?.readText()
            ?: error("프로젝트 하네스가 리소스에 없다: /project/$HARNESS")
    }

    companion object {
        const val HARNESS = "python_harness.py"

        private const val SYNTAX_CHECK =
            "import pathlib, sys\n" +
                "failed = 0\n" +
                "for path in sorted(pathlib.Path(sys.argv[1]).rglob('*.py')):\n" +
                "    try:\n" +
                "        compile(path.read_text(encoding='utf-8'), str(path), 'exec')\n" +
                "    except SyntaxError as error:\n" +
                "        failed += 1\n" +
                "        print(f'{path}:{error.lineno}: {error.msg}')\n" +
                "sys.exit(1 if failed else 0)\n"
    }
}


/**
 * Kotlin 프로젝트. 알고리즘 판정의 Kotlin 어댑터와 같은 jar 를 쓴다 — Runner 가 가진 컴파일러와
 * 런타임을 읽기 전용으로 들여보내고, 컴파일도 샌드박스 안에서 돈다 (§5.5).
 *
 * 워크스페이스의 `.kt` 전부(사용자 코드와 테스트)와 하네스를 한 번에 컴파일한다. 숨은 테스트가
 * 요구사항의 API 를 부르므로 API 를 어기면 컴파일 오류다 — 그것이 맞다. 테스트 기반은
 * kotlin-test 가 아니라 하네스의 단언 몇 개다; 샌드박스에는 표준 라이브러리뿐이다.
 */
class KotlinProjectAdapter(
    private val runtime: List<Path>,
    private val compiler: List<Path>,
) : ProjectAdapter {

    override val language = Language.KOTLIN

    override fun harnessFiles(): Map<String, String> = mapOf(HARNESS to harnessSource)

    override fun buildCommand(workspace: Path): List<String> = listOf(
        "java",
        "-Xmx${COMPILER_HEAP_MB}m",
        "-XX:+UseSerialGC",
        "-XX:TieredStopAtLevel=1",
        "-XX:-UsePerfData",
        "-Dkotlin.colors.enabled=false",
        "-Dfile.encoding=UTF-8",
        "-cp", compiler.joinToString(java.io.File.pathSeparator) { it.absolutePathString() },
        "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler",
        "-no-stdlib", "-no-reflect", "-nowarn",
        "-cp", runtime.joinToString(java.io.File.pathSeparator) { it.absolutePathString() },
        "-d", workspace.resolveSibling("out").resolve(CLASSES).absolutePathString(),
        workspace.absolutePathString(),
        workspace.resolveSibling("harness").absolutePathString(),
    )

    override fun testCommand(workspace: Path, harnessDir: Path, outputDir: Path, reportFile: Path, nonceFile: Path, memoryMb: Int): List<String> = listOf(
        "java",
        "-Xmx${memoryMb}m",
        "-XX:+UseSerialGC",
        "-XX:-UsePerfData",
        "-Dfile.encoding=UTF-8",
        // plusElement 다. Path 는 Iterable<Path> 라 `+` 는 경로를 조각으로 펼친다 — 알고리즘 어댑터와 같은 함정.
        "-cp", runtime.plusElement(outputDir.resolve(CLASSES)).joinToString(java.io.File.pathSeparator) { it.absolutePathString() },
        "codedrill.CodedrillHarnessKt",
        outputDir.resolve(CLASSES).absolutePathString(),
        reportFile.absolutePathString(),
        nonceFile.absolutePathString(),
    )

    override fun env() = mapOf("TZ" to "UTC")

    override fun readOnlyPaths(): List<Path> = (runtime + compiler).distinct()

    override fun buildMemoryMb(limitMb: Int): Int = maxOf(limitMb, COMPILER_HEAP_MB)

    private val harnessSource: String by lazy {
        KotlinProjectAdapter::class.java.getResourceAsStream("/project/kotlin_harness.kt")
            ?.bufferedReader()?.readText()
            ?: error("프로젝트 하네스가 리소스에 없다: /project/kotlin_harness.kt")
    }

    companion object {
        /** 하네스 파일 이름. 클래스 `codedrill.CodedrillHarnessKt` 가 여기서 나온다. */
        const val HARNESS = "CodedrillHarness.kt"
        const val CLASSES = "classes"

        /** 알고리즘 판정의 컴파일러와 같은 값. 컴파일러는 문제의 한도와 무관하게 이만큼 쓴다. */
        const val COMPILER_HEAP_MB = 768
    }
}
