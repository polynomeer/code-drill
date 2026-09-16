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

    /** 스위트 실행 명령. 리포트를 [reportFile] 에 적어야 한다 ([ProjectReport] 의 모양). */
    fun testCommand(workspace: Path, harnessDir: Path, reportFile: Path, memoryMb: Int): List<String>

    fun env(): Map<String, String> = emptyMap()
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

    override fun testCommand(workspace: Path, harnessDir: Path, reportFile: Path, memoryMb: Int): List<String> = listOf(
        interpreter, "-B",
        harnessDir.resolve(HARNESS).absolutePathString(),
        workspace.absolutePathString(),
        reportFile.absolutePathString(),
        memoryMb.toString(),
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
