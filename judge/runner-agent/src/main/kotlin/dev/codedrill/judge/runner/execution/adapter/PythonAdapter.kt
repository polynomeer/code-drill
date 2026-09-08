package dev.codedrill.judge.runner.execution.adapter

import dev.codedrill.judge.protocol.ExecutionMode
import dev.codedrill.judge.protocol.ExecutionRequest
import dev.codedrill.judge.protocol.Language
import dev.codedrill.judge.runner.execution.qualifiedId
import dev.codedrill.platform.problempackage.Signature
import dev.codedrill.platform.problempackage.ValueType
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.absolutePathString
import kotlin.io.path.writeText

/**
 * Python 런타임 어댑터 (기술 설계서 §5.4).
 *
 * 컴파일 단계가 없으므로 `py_compile` 로 문법만 검사한다. **모듈을 import 하지는
 * 않는다** — import 는 사용자 코드를 실행하는 것이고, 그 실행은 샌드박스 밖에서
 * 일어나서는 안 된다 (§5.1 비신뢰 입력).
 *
 * 그래서 "문법은 맞지만 함수가 없다"는 문법 검사로 잡히지 않는다. 하네스가 import 를
 * 감싸 [SandboxProtocol.FATAL] 로 보고하고, Runner 가 그것을 COMPILE_ERROR 로 분류한다.
 *
 * 결정성을 위해 해시 시드를 고정한다. 고정하지 않으면 dict/set 순회 순서가 실행마다
 * 달라져 같은 코드가 다른 판정을 낼 수 있다 (§5.4, §12.1 재현성).
 */
class PythonAdapter(private val interpreter: String = DEFAULT_INTERPRETER) : RuntimeAdapter {

    override val language = Language.PYTHON

    override fun prepare(request: ExecutionRequest, sourceDir: Path) {
        sourceDir.resolve("solution.py").writeText(request.source)
        sourceDir.resolve("drill.py").writeText(drill(request.mode))
        sourceDir.resolve("main.py").writeText(harness(request.signature))
        for (group in request.groups) {
            sourceDir.resolve(caseFileName(group.policy.id)).writeText(
                group.cases.joinToString("\n") {
                    encodeCaseLine(it.qualifiedId(), request.signature.parameters, it.args)
                },
            )
        }
    }

    /**
     * `RLIMIT_AS` 는 Linux 에서만 실질적으로 동작한다. macOS 는 주소 공간 상한을 낮추는
     * 것을 허용하지 않아 하한선이 서지 않는다. 그 플랫폼에서 메모리 상한을 강제할 수
     * 있는 것은 컨테이너의 cgroup 뿐이다.
     */
    override fun enforcesMemoryWithoutContainer(): Boolean =
        System.getProperty("os.name").orEmpty().lowercase().contains("linux")

    override fun compile(sourceDir: Path, outputDir: Path): RuntimeAdapter.CompileOutcome {
        val process = ProcessBuilder(
            interpreter,
            "-m",
            "py_compile",
            sourceDir.resolve("solution.py").absolutePathString(),
        )
            .directory(sourceDir.toFile())
            .redirectErrorStream(true)
            .start()

        val log = process.inputStream.bufferedReader().readText()
        if (!process.waitFor(SYNTAX_CHECK_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            error("문법 검사가 끝나지 않았다")
        }
        if (process.exitValue() == 0) return RuntimeAdapter.CompileOutcome.Success

        return RuntimeAdapter.CompileOutcome.Failure(
            log.replace(sourceDir.absolutePathString(), "").take(MAX_LOG_CHARS),
        )
    }

    override fun command(
        sourceDir: Path,
        outputDir: Path,
        groupId: String,
        memoryMb: Int,
    ): List<String> = listOf(
        interpreter,
        // 바이트코드 캐시를 만들지 않는다. 읽기 전용 샌드박스에서 쓰기를 시도하지 않도록.
        "-B",
        sourceDir.resolve("main.py").absolutePathString(),
        groupId,
        sourceDir.absolutePathString(),
        memoryMb.toString(),
    )

    // --- 코드 생성 ---

    private fun drill(mode: ExecutionMode): String = if (!mode.instrumented()) {
        // 공식 판정 실행용 no-op 계측 (§7.1). 시그니처는 계측 버전과 같아야 한다.
        "class Drill:\n" + methods(instrumented = false)
    } else {
        buildString {
            appendLine("import sys")
            appendLine()
            appendLine()
            appendLine("class Drill:")
            appendLine("    _seq = 0")
            appendLine("    _budget_left = $EVENT_BUDGET")
            appendLine("    _out = sys.__stdout__")
            appendLine()
            appendLine("    @classmethod")
            appendLine("    def _emit(cls, kind, ref, after, importance):")
            appendLine("        if cls._budget_left <= 0:")
            appendLine("            return")
            appendLine("        cls._budget_left -= 1")
            appendLine("        cls._seq += 1")
            appendLine("        cls._out.write(")
            appendLine("            \"${SandboxProtocol.EVENT}\\t%d\\t%s\\t%s\\t\\t%s\\t%d\\n\"")
            appendLine("            % (cls._seq, kind, ref, after, importance)")
            appendLine("        )")
            appendLine("        cls._out.flush()")
            appendLine()
            appendLine(methods(instrumented = true))
        }
    }

    /** 계측 메서드는 [TraceApi] 에서 생성한다. */
    private fun methods(instrumented: Boolean): String = TraceApi.methods.joinToString("\n\n") { method ->
        val params = method.params.joinToString(", ") { param ->
            param.name + (param.default?.let { "=$it" } ?: "")
        }
        if (!instrumented) {
            "    @staticmethod\n    def ${method.name}($params):\n        pass"
        } else {
            "    @classmethod\n    def ${method.name}(cls, $params):\n" +
                "        cls._emit(\"${method.eventType}\", ${expr(method.ref)}, ${expr(method.after)}, " +
                "${method.eventType.defaultImportance})"
        }
    }

    private fun expr(reference: String?): String = when {
        reference == null -> "\"\""
        TraceApi.isLiteral(reference) -> "\"${TraceApi.literalValue(reference)}\""
        else -> "str($reference)"
    }

    private fun harness(signature: Signature): String = buildString {
        appendLine("import base64")
        appendLine("import io")
        appendLine("import os")
        appendLine("import resource")
        appendLine("import sys")
        appendLine("import time")
        appendLine("import traceback")
        appendLine()
        appendLine("sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))")
        appendLine()
        appendLine("PROTOCOL = sys.__stdout__")
        appendLine()
        appendLine("")
        appendLine("class Counting(io.TextIOBase):")
        appendLine("    \"\"\"사용자 출력은 세기만 하고 버린다. 무제한 버퍼링은 OLE 를 MLE 로 오분류한다.\"\"\"")
        appendLine("    def __init__(self):")
        appendLine("        self.total = 0")
        appendLine()
        appendLine("    def write(self, s):")
        appendLine("        self.total += len(s)")
        appendLine("        return len(s)")
        appendLine()
        appendLine("")
        appendLine("USER_OUT = Counting()")
        appendLine()
        appendLine("")
        // 파이썬은 타입을 값에서 추론할 수 있지만 그러지 않는다. 사용자가 int 를
        // 돌려줘야 할 자리에 문자열을 돌려주면 조용히 다른 형식으로 찍혀, 오답이
        // "형식이 달라 틀렸다"가 아니라 "값이 틀렸다"로 보인다.
        appendLine("def b64(value):")
        appendLine("    return base64.b64encode(str(value).encode(\"utf-8\")).decode(\"ascii\")")
        appendLine()
        appendLine("")
        appendLine("def encode_int(value):")
        appendLine("    return str(int(value))")
        appendLine()
        appendLine("")
        appendLine("def encode_ints(value):")
        appendLine("    return \",\".join(str(int(v)) for v in value)")
        appendLine()
        appendLine("")
        appendLine("def encode_str(value):")
        appendLine("    return b64(value)")
        appendLine()
        appendLine("")
        appendLine("def encode_strs(value):")
        appendLine("    items = list(value)")
        appendLine("    return \",\".join([str(len(items))] + [b64(v) for v in items])")
        appendLine()
        appendLine("")
        appendLine("def encode_grid(value):")
        appendLine("    grid = [list(row) for row in value]")
        appendLine("    cols = len(grid[0]) if grid else 0")
        appendLine("    head = [str(len(grid)), str(cols)]")
        appendLine("    return \",\".join(head + [str(int(v)) for row in grid for v in row])")
        appendLine()
        appendLine("")
        appendLine("def ints(field):")
        appendLine("    return [int(x) for x in field.split(\",\")] if field else []")
        appendLine()
        appendLine("")
        appendLine("def text(field):")
        appendLine("    return base64.b64decode(field).decode(\"utf-8\")")
        appendLine()
        appendLine("")
        appendLine("def texts(field):")
        appendLine("    parts = field.split(\",\")")
        appendLine("    count = int(parts[0])")
        appendLine("    return [text(p) for p in parts[1:1 + count]]")
        appendLine()
        appendLine("")
        appendLine("def grid(field):")
        appendLine("    parts = field.split(\",\")")
        appendLine("    rows = int(parts[0])")
        appendLine("    cols = int(parts[1])")
        appendLine("    return [")
        appendLine("        [int(parts[2 + r * cols + c]) for c in range(cols)] for r in range(rows)")
        appendLine("    ]")
        appendLine()
        appendLine("")
        // 반환 타입은 시그니처가 정한다. 값에서 추론하면 사용자가 엉뚱한 타입을 돌려줬을 때
        // 조용히 다른 형식으로 찍혀, 형식 불일치가 값 오류로 보인다.
        appendLine("encoder = ${encoder(signature.returns)}")
        appendLine()
        appendLine("")
        appendLine("def run_case(case_id, body):")
        appendLine("    PROTOCOL.write(\"${SandboxProtocol.START}\\t%s\\n\" % case_id)")
        appendLine("    PROTOCOL.flush()")
        appendLine("    started = time.perf_counter()")
        appendLine("    try:")
        appendLine("        outcome = \"OK\\t\" + encoder(body())")
        appendLine("    except BaseException as error:")
        appendLine("        outcome = \"ERROR\\t\" + type(error).__module__ + \".\" + type(error).__name__")
        appendLine("    elapsed_ms = int((time.perf_counter() - started) * 1000)")
        appendLine("    used = resource.getrusage(resource.RUSAGE_SELF).ru_maxrss")
        appendLine("    PROTOCOL.write(")
        appendLine("        \"${SandboxProtocol.RESULT}\\t%s\\t%s\\t%d\\t%d\\t%d\\n\"")
        appendLine("        % (case_id, outcome, elapsed_ms, used, USER_OUT.total)")
        appendLine("    )")
        appendLine("    PROTOCOL.flush()")
        appendLine()
        appendLine("")
        appendLine("def main():")
        appendLine("    group = sys.argv[1]")
        appendLine("    source_dir = sys.argv[2]")
        appendLine("    memory_mb = int(sys.argv[3]) if len(sys.argv) > 3 else 0")
        appendLine("    if memory_mb > 0:")
        appendLine("        # 힙 상한이 없는 언어라 주소 공간으로 메모리를 제한한다. Linux 에서만")
        appendLine("        # 실효가 있고, 그 밖에서는 컨테이너 cgroup 이 유일한 방어선이다.")
        appendLine("        limit = memory_mb * 1024 * 1024")
        appendLine("        try:")
        appendLine("            resource.setrlimit(resource.RLIMIT_AS, (limit, limit))")
        appendLine("        except (ValueError, OSError):")
        appendLine("            pass")
        appendLine("    try:")
        appendLine("        from solution import ${signature.name}")
        appendLine("    except BaseException:")
        appendLine("        # 문법은 맞지만 불러올 수 없다. 컴파일 단계가 잡지 못한 같은 종류의 실패다.")
        appendLine("        PROTOCOL.write(")
        appendLine("            \"${SandboxProtocol.FATAL}\\t%s\\n\" % traceback.format_exc().strip().replace(\"\\n\", \" | \")")
        appendLine("        )")
        appendLine("        PROTOCOL.flush()")
        appendLine("        return")
        appendLine("    sys.stdout = USER_OUT")
        appendLine("    path = os.path.join(source_dir, \"cases_\" + group + \".txt\")")
        appendLine("    with open(path, encoding=\"utf-8\") as handle:")
        appendLine("        lines = handle.read().splitlines()")
        appendLine("    for line in lines:")
        appendLine("        if not line:")
        appendLine("            continue")
        appendLine("        f = line.split(\"\\t\")")
        appendLine("        run_case(f[0], lambda f=f: " + call(signature) + ")")
        appendLine("    PROTOCOL.write(\"${SandboxProtocol.DONE}\\n\")")
        appendLine("    PROTOCOL.flush()")
        appendLine()
        appendLine("")
        appendLine("main()")
    }

    /** 시그니처대로 필드를 풀어 사용자 함수를 부르는 표현식. */
    private fun call(signature: Signature): String {
        val args = signature.parameters.mapIndexed { index, parameter ->
            when (parameter.type) {
                ValueType.INT -> "int(f[${index + 1}])"
                ValueType.INT_ARRAY -> "ints(f[${index + 1}])"
                ValueType.STRING -> "text(f[${index + 1}])"
                ValueType.STRING_ARRAY -> "texts(f[${index + 1}])"
                ValueType.INT_MATRIX -> "grid(f[${index + 1}])"
            }
        }
        return "${signature.name}(${args.joinToString(", ")})"
    }

    private fun encoder(returns: ValueType) = when (returns) {
        ValueType.INT -> "encode_int"
        ValueType.INT_ARRAY -> "encode_ints"
        ValueType.STRING -> "encode_str"
        ValueType.STRING_ARRAY -> "encode_strs"
        ValueType.INT_MATRIX -> "encode_grid"
    }

    private fun quote(value: String) = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    companion object {
        const val DEFAULT_INTERPRETER = "python3"

        /** 해시 시드를 고정해 dict/set 순회 순서를 실행마다 같게 만든다 (§12.1). */
        val DETERMINISM_ENV = mapOf("PYTHONHASHSEED" to "0", "PYTHONDONTWRITEBYTECODE" to "1")

        private const val EVENT_BUDGET = 1_000
        private const val MAX_LOG_CHARS = 8_000
        private const val SYNTAX_CHECK_TIMEOUT_SECONDS = 10L
    }
}
