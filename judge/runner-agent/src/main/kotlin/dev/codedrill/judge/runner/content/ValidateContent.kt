package dev.codedrill.judge.runner.content

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import dev.codedrill.judge.protocol.Language
import dev.codedrill.judge.runner.execution.ExecutionEngine
import dev.codedrill.judge.runner.execution.adapter.KotlinAdapter
import dev.codedrill.judge.runner.execution.sandbox.ProcessSandbox
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.system.exitProcess

/**
 * 콘텐츠 검증 CLI (기술 설계서 §6.3, §15.1 파이프라인).
 *
 * 문제를 만든 사람이 손으로 돌리고, CI 가 같은 명령을 돌린다. 보고서는 파일로 남겨
 * 공개 요청에 첨부한다 — 제어 영역은 보고서 digest 가 맞는 버전만 공개한다 (§3.2).
 *
 * ```
 * ./gradlew :judge:runner-agent:validateContent
 * ```
 */
object ValidateContent {

    @JvmStatic
    fun main(args: Array<String>) {
        val contentRoot = Path.of(args.getOrElse(0) { "content/problems" })
        val reportRoot = Path.of(args.getOrElse(1) { "content/reports" })

        val validator = ContentValidator(
            engine = ExecutionEngine(
                adapters = mapOf(Language.KOTLIN to KotlinAdapter()),
                sandboxes = { ProcessSandbox() },
            ),
            contentRoot = contentRoot,
        )

        val json = ObjectMapper().registerKotlinModule()
            .enable(SerializationFeature.INDENT_OUTPUT)

        reportRoot.createDirectories()
        val reports = validator.validateAll()

        for (report in reports) {
            val marker = if (report.passed) "PASS" else "FAIL"
            println("$marker  ${report.problemVersionId}  (report ${report.reportDigest.take(12)})")
            report.checks.forEach { check ->
                val mark = when {
                    !check.passed -> "✗"
                    check.warning -> "⚠"
                    else -> "·"
                }
                println("        $mark ${check.stage}: ${check.detail}")
            }
            report.mutations.forEach { mutation ->
                val killed = if (mutation.killed) "잡힘 (${mutation.killedBy.joinToString()})" else "살아남음"
                println("        · mutant ${mutation.name} [${mutation.kind}]: $killed")
            }

            reportRoot.resolve("${report.problemVersionId.replace('@', '-')}.json")
                .writeText(json.writeValueAsString(report))
        }

        val failed = reports.count { !it.passed }
        val warned = reports.filter { report -> report.checks.any { it.warning } }

        println()
        println("${reports.size - failed}/${reports.size} 통과")

        // 경고는 공개를 막지 않는다. 그래서 더더욱 마지막에 한 번 더 모아 보여 준다 —
        // 통과 줄만 보고 닫으면 그대로 묻힌다.
        if (warned.isNotEmpty()) {
            println()
            println("경고 ${warned.size}건 — 공개는 막지 않는다")
            warned.forEach { report ->
                report.checks.filter { it.warning }.forEach { check ->
                    println("  ⚠ ${report.problemVersionId}  ${check.stage}: ${check.detail}")
                }
            }
        }
        if (failed > 0) exitProcess(1)
    }
}
