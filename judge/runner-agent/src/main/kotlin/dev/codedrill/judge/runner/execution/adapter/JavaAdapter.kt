package dev.codedrill.judge.runner.execution.adapter

import dev.codedrill.judge.protocol.ExecutionMode
import dev.codedrill.judge.protocol.ExecutionRequest
import dev.codedrill.judge.protocol.Language
import dev.codedrill.judge.runner.execution.qualifiedId
import dev.codedrill.platform.problempackage.Signature
import dev.codedrill.platform.problempackage.ValueType
import java.io.StringWriter
import java.nio.file.Path
import javax.tools.DiagnosticCollector
import javax.tools.JavaFileObject
import javax.tools.ToolProvider
import kotlin.io.path.absolutePathString
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.writeText

/**
 * Java 런타임 어댑터 (기술 설계서 §5.4).
 *
 * 사용자는 `Solution` 클래스에 메서드를 작성한다. Java 에는 최상위 함수가 없으므로
 * 클래스 규약이 필요하고, 이 규약은 문제 본문에 명시된다.
 *
 * 컴파일은 JDK 의 `javax.tools` 컴파일러를 in-process 로 쓴다. 외부 `javac` 실행 파일에
 * 기대지 않으므로 런타임 이미지가 JDK 하나만 담으면 된다.
 */
class JavaAdapter : RuntimeAdapter {

    override val language = Language.JAVA

    override fun prepare(request: ExecutionRequest, sourceDir: Path) {
        sourceDir.resolve("Solution.java").writeText(request.source)
        sourceDir.resolve("Drill.java").writeText(drill(request.mode))
        sourceDir.resolve("Main.java").writeText(harness(request.signature))
        for (group in request.groups) {
            sourceDir.resolve(caseFileName(group.policy.id)).writeText(
                group.cases.joinToString("\n") {
                    encodeCaseLine(it.qualifiedId(), request.signature.parameters, it.args)
                },
            )
        }
    }

    override fun compile(sourceDir: Path, outputDir: Path): RuntimeAdapter.CompileOutcome {
        val compiler = ToolProvider.getSystemJavaCompiler()
            ?: error("JDK 의 java 컴파일러를 찾지 못했다. JRE 로 실행 중일 수 있다")

        val diagnostics = DiagnosticCollector<JavaFileObject>()
        val output = StringWriter()

        compiler.getStandardFileManager(diagnostics, null, null).use { fileManager ->
            val sources = fileManager.getJavaFileObjectsFromPaths(
                sourceDir.listDirectoryEntries("*.java").sorted(),
            )
            val options = listOf("-d", outputDir.absolutePathString(), "-encoding", "UTF-8")
            val task = compiler.getTask(output, fileManager, diagnostics, options, null, sources)

            if (task.call() == true) return RuntimeAdapter.CompileOutcome.Success
        }

        // 진단 메시지에는 소스 파일의 절대 경로가 들어간다. 줄·칸만 남겨 서버 경로가
        // 사용자에게 노출되지 않게 한다 (§11.3).
        val log = diagnostics.diagnostics
            .filter { it.kind == javax.tools.Diagnostic.Kind.ERROR }
            .joinToString("\n") { "(${it.lineNumber}:${it.columnNumber}) ${it.getMessage(null)}" }
            .ifBlank { output.toString() }

        return RuntimeAdapter.CompileOutcome.Failure(log.take(MAX_LOG_CHARS))
    }

    override fun command(
        sourceDir: Path,
        outputDir: Path,
        groupId: String,
        memoryMb: Int,
    ): List<String> = listOf(
        "java",
        "-Xmx${memoryMb}m",
        "-XX:+UseSerialGC",
        "-XX:-UsePerfData",
        "-Dfile.encoding=UTF-8",
        "-cp",
        outputDir.absolutePathString(),
        "Main",
        groupId,
        sourceDir.absolutePathString(),
    )

    // --- 코드 생성 ---

    private fun drill(mode: ExecutionMode): String = if (!mode.instrumented()) {
        // 공식 판정 실행용 no-op 계측 (§7.1). 시그니처는 계측 버전과 같아야 한다.
        "final class Drill {\n" + methods(instrumented = false) + "\n}"
    } else {
        buildString {
            appendLine("import java.io.PrintStream;")
            appendLine()
            appendLine("final class Drill {")
            appendLine("    private static long seq = 0;")
            appendLine("    private static int budgetLeft = $EVENT_BUDGET;")
            appendLine()
            appendLine("    private static void emit(String type, String ref, String after, int importance) {")
            appendLine("        if (budgetLeft <= 0) return;")
            appendLine("        budgetLeft -= 1;")
            appendLine("        seq += 1;")
            appendLine("        PrintStream out = Main.protocol();")
            appendLine("        out.println(\"${SandboxProtocol.EVENT}\\t\" + seq + \"\\t\" + type + \"\\t\" + ref")
            appendLine("            + \"\\t\\t\" + after + \"\\t\" + importance);")
            appendLine("        out.flush();")
            appendLine("    }")
            appendLine()
            appendLine(methods(instrumented = true))
            appendLine("}")
        }
    }

    /**
     * 계측 메서드는 [TraceApi] 에서 생성한다.
     *
     * Java 에는 기본값이 없으므로 오버로드를 함께 만든다. 없으면 Kotlin·Python 에서는
     * 되는 `Drill.visit(i)` 가 Java 에서만 컴파일 실패한다.
     */
    private fun methods(instrumented: Boolean): String = TraceApi.methods.flatMap { method ->
        val overloads = mutableListOf(method.params)
        if (method.params.any { it.default != null }) {
            overloads += method.params.filter { it.default == null }
        }
        overloads.map { params -> render(method, params, instrumented) }
    }.joinToString("\n")

    private fun render(
        method: TraceApi.Method,
        params: List<TraceApi.Param>,
        instrumented: Boolean,
    ): String {
        val declared = params.joinToString(", ") { param ->
            val type = if (param.type == TraceApi.SdkType.INT) "int" else "String"
            "$type ${param.name}"
        }
        val head = "    static void ${method.name}($declared)"
        if (!instrumented) return "$head {}"

        val supplied = params.map { it.name }.toSet()
        // 생략된 파라미터는 기본값으로 채운다.
        fun value(reference: String?): String = when {
            reference == null -> "\"\""
            TraceApi.isLiteral(reference) -> "\"${TraceApi.literalValue(reference)}\""
            reference !in supplied ->
                "String.valueOf(${method.params.first { it.name == reference }.default})"
            method.params.first { it.name == reference }.type == TraceApi.SdkType.INT ->
                "String.valueOf($reference)"
            else -> reference
        }
        return "$head { emit(\"${method.eventType}\", ${value(method.ref)}, ${value(method.after)}, " +
            "${method.eventType.defaultImportance}); }"
    }

    private fun harness(signature: Signature): String = buildString {
        appendLine("import java.io.OutputStream;")
        appendLine("import java.io.PrintStream;")
        appendLine("import java.nio.charset.StandardCharsets;")
        appendLine("import java.nio.file.Files;")
        appendLine("import java.nio.file.Paths;")
        appendLine("import java.util.Arrays;")
        appendLine("import java.util.Base64;")
        appendLine("import java.util.List;")
        appendLine("import java.util.stream.Collectors;")
        appendLine()
        appendLine("public final class Main {")
        appendLine("    private static final PrintStream PROTOCOL = System.out;")
        appendLine("    private static final Counting USER_OUT = new Counting();")
        appendLine()
        appendLine("    /** 계측 SDK 가 사용자 출력 리다이렉션 이전의 원본 스트림을 잡는다. */")
        appendLine("    static PrintStream protocol() { return PROTOCOL; }")
        appendLine()
        // 사용자 출력은 세기만 하고 버린다. 무제한 버퍼링은 OLE 를 MLE 로 오분류한다.
        appendLine("    private static final class Counting extends OutputStream {")
        appendLine("        long total = 0;")
        appendLine("        @Override public void write(int b) { total += 1; }")
        appendLine("        @Override public void write(byte[] b, int off, int len) { total += len; }")
        appendLine("    }")
        appendLine()
        appendLine("    private static String b64(String v) {")
        appendLine("        return Base64.getEncoder().encodeToString(v.getBytes(StandardCharsets.UTF_8));")
        appendLine("    }")
        appendLine()
        // 반환 타입마다 이름을 다르게 둔다. 오버로드로 두면 사용자 메서드의 반환 타입이
        // 조금만 달라도 해소가 흔들리고, 그 실패는 사용자에게 컴파일 오류로 보인다.
        appendLine("    private static String encodeInt(int v) { return Integer.toString(v); }")
        appendLine()
        appendLine("    private static String encodeInts(int[] v) {")
        appendLine("        return Arrays.stream(v).mapToObj(Integer::toString).collect(Collectors.joining(\",\"));")
        appendLine("    }")
        appendLine()
        appendLine("    private static String encodeStr(String v) { return b64(v); }")
        appendLine()
        appendLine("    private static String encodeStrs(String[] v) {")
        appendLine("        StringBuilder out = new StringBuilder().append(v.length);")
        appendLine("        for (String item : v) out.append(',').append(b64(item));")
        appendLine("        return out.toString();")
        appendLine("    }")
        appendLine()
        appendLine("    private static int[] ints(String field) {")
        appendLine("        if (field.isEmpty()) return new int[0];")
        appendLine("        String[] parts = field.split(\",\");")
        appendLine("        int[] out = new int[parts.length];")
        appendLine("        for (int i = 0; i < parts.length; i++) out[i] = Integer.parseInt(parts[i]);")
        appendLine("        return out;")
        appendLine("    }")
        appendLine()
        appendLine("    private static String str(String field) {")
        appendLine("        return new String(Base64.getDecoder().decode(field), StandardCharsets.UTF_8);")
        appendLine("    }")
        appendLine()
        appendLine("    private static String encodeGrid(int[][] v) {")
        appendLine("        int cols = v.length == 0 ? 0 : v[0].length;")
        appendLine("        StringBuilder out = new StringBuilder().append(v.length).append(',').append(cols);")
        appendLine("        for (int[] row : v) for (int x : row) out.append(',').append(x);")
        appendLine("        return out.toString();")
        appendLine("    }")
        appendLine()
        appendLine("    private static int[][] grid(String field) {")
        appendLine("        String[] parts = field.split(\",\");")
        appendLine("        int rows = Integer.parseInt(parts[0]);")
        appendLine("        int cols = Integer.parseInt(parts[1]);")
        appendLine("        int[][] out = new int[rows][cols];")
        appendLine("        for (int r = 0; r < rows; r++) {")
        appendLine("            for (int c = 0; c < cols; c++) out[r][c] = Integer.parseInt(parts[2 + r * cols + c]);")
        appendLine("        }")
        appendLine("        return out;")
        appendLine("    }")
        appendLine()
        appendLine("    private static String[] strs(String field) {")
        appendLine("        String[] parts = field.split(\",\", -1);")
        appendLine("        int count = Integer.parseInt(parts[0]);")
        appendLine("        String[] out = new String[count];")
        appendLine("        for (int i = 0; i < count; i++) out[i] = str(parts[i + 1]);")
        appendLine("        return out;")
        appendLine("    }")
        appendLine()
        appendLine("    private interface Body { String run(); }")
        appendLine()
        appendLine("    private static void runCase(String id, Body body) {")
        appendLine("        PROTOCOL.println(\"${SandboxProtocol.START}\\t\" + id);")
        appendLine("        PROTOCOL.flush();")
        appendLine("        long started = System.nanoTime();")
        appendLine("        String outcome;")
        appendLine("        try {")
        appendLine("            outcome = \"OK\\t\" + body.run();")
        appendLine("        } catch (Throwable t) {")
        appendLine("            outcome = \"ERROR\\t\" + t.getClass().getName();")
        appendLine("        }")
        appendLine("        long elapsedMs = (System.nanoTime() - started) / 1_000_000L;")
        appendLine("        Runtime runtime = Runtime.getRuntime();")
        appendLine("        long used = runtime.totalMemory() - runtime.freeMemory();")
        appendLine("        PROTOCOL.println(")
        appendLine("            \"${SandboxProtocol.RESULT}\\t\" + id + \"\\t\" + outcome + \"\\t\" + elapsedMs")
        appendLine("                + \"\\t\" + used + \"\\t\" + USER_OUT.total);")
        appendLine("        PROTOCOL.flush();")
        appendLine("    }")
        appendLine()
        appendLine("    public static void main(String[] args) throws Exception {")
        appendLine("        String group = args[0];")
        appendLine("        System.setOut(new PrintStream(USER_OUT, true));")
        appendLine("        Solution solution = new Solution();")
        appendLine("        List<String> lines = Files.readAllLines(")
        appendLine("            Paths.get(args[1], \"cases_\" + group + \".txt\"), StandardCharsets.UTF_8);")
        appendLine("        for (String line : lines) {")
        appendLine("            if (line.isEmpty()) continue;")
        appendLine("            String[] f = line.split(\"\\\\t\", -1);")
        appendLine("            runCase(f[0], () -> ${encoder(signature.returns)}(" + call(signature) + "));")
        appendLine("        }")
        appendLine("        PROTOCOL.println(\"${SandboxProtocol.DONE}\");")
        appendLine("        PROTOCOL.flush();")
        appendLine("    }")
        appendLine("}")
    }

    /** 시그니처대로 필드를 풀어 사용자 메서드를 부르는 표현식. */
    private fun call(signature: Signature): String {
        val args = signature.parameters.mapIndexed { index, parameter ->
            when (parameter.type) {
                ValueType.INT -> "Integer.parseInt(f[${index + 1}])"
                ValueType.INT_ARRAY -> "ints(f[${index + 1}])"
                ValueType.STRING -> "str(f[${index + 1}])"
                ValueType.STRING_ARRAY -> "strs(f[${index + 1}])"
                ValueType.INT_MATRIX -> "grid(f[${index + 1}])"
            }
        }
        return "solution.${signature.name}(${args.joinToString(", ")})"
    }

    private fun encoder(returns: ValueType) = when (returns) {
        ValueType.INT -> "encodeInt"
        ValueType.INT_ARRAY -> "encodeInts"
        ValueType.STRING -> "encodeStr"
        ValueType.STRING_ARRAY -> "encodeStrs"
        ValueType.INT_MATRIX -> "encodeGrid"
    }

    private fun quote(value: String) = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    private companion object {
        const val EVENT_BUDGET = 1_000
        const val MAX_LOG_CHARS = 8_000
    }
}
