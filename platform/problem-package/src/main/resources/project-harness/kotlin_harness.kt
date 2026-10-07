// 프로젝트형 문제의 Kotlin 테스트 하네스 (ProjectEngine 이 워크스페이스와 함께 컴파일한다).
//
//   java -cp <runtime>:<classes> codedrill.CodedrillHarnessKt <클래스 디렉터리> <리포트 파일> <nonce 파일>
//
// 테스트는 이름이 `Test` 로 끝나는 클래스의, 인자 없는 공개 메서드 중 이름이 `test` 로 시작하는
// 것이다. 클래스마다 인스턴스를 새로 만들어 메서드를 하나씩 부르고, 예외가 나면 실패다.
// `module` 은 클래스의 완전한 이름(`tests.LedgerTest`)이라 숨은 테스트 파일 `tests/LedgerTest.kt`
// 와 맞아떨어진다 — 저작 검증이 파일 하나에 같은 이름의 클래스 하나라는 약속을 본다.
//
// 리포트는 파일에 쓴다 — stdout 은 사용자 코드가 무엇을 얼마나 찍든 상관없어야 한다. 리포트의
// nonce 는 하네스가 사용자 클래스를 하나라도 들이기 전에 읽고 **지운** 파일에서 온다. 사용자
// 코드가 리포트를 꾸며 쓰고 끝내도 nonce 를 모른다.

package codedrill

import java.io.File

// 테스트가 쓰는 단언. kotlin-test 는 샌드박스에 없다 — 표준 라이브러리만 있다. 테스트는
// `import codedrill.*` 로 쓴다.
fun assertEquals(expected: Any?, actual: Any?, message: String? = null) {
    if (expected != actual) throw AssertionError((message?.let { "$it: " } ?: "") + "expected <$expected> but was <$actual>")
}

fun assertTrue(condition: Boolean, message: String? = null) {
    if (!condition) throw AssertionError(message ?: "expected true")
}

fun assertFalse(condition: Boolean, message: String? = null) {
    if (condition) throw AssertionError(message ?: "expected false")
}

fun assertNull(actual: Any?, message: String? = null) {
    if (actual != null) throw AssertionError(message ?: "expected null but was <$actual>")
}

inline fun <reified T : Throwable> assertThrows(message: String? = null, block: () -> Unit): T {
    try {
        block()
    } catch (e: Throwable) {
        if (e is T) return e
        throw AssertionError((message?.let { "$it: " } ?: "") + "expected ${T::class.simpleName} but got ${e::class.simpleName}: ${e.message}")
    }
    throw AssertionError(message ?: "expected ${T::class.simpleName} but nothing was thrown")
}

private class Row(val module: String, val name: String, val passed: Boolean, val message: String?)

private fun json(value: String?): String =
    if (value == null) "null" else "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "").replace("\t", "\\t") + "\""

fun main(args: Array<String>) {
    val classes = File(args[0])
    val report = File(args[1])
    val nonceFile = File(args[2])
    // 사용자 클래스를 들이기 전에 읽고 지운다.
    val nonce = nonceFile.readText().trim()
    nonceFile.delete()

    val rows = ArrayList<Row>()
    var tampered: String? = null

    // 반드시 실패해야 하는 카나리. 실패가 기록되지 않으면 실행 자체가 손댄 것이다.
    var canaryFailed = false
    try {
        assertEquals(1, 2)
    } catch (e: AssertionError) {
        canaryFailed = true
    }
    if (!canaryFailed) tampered = "실패해야 하는 카나리 단언이 통과했다"

    val names = classes.walkTopDown()
        .filter { it.isFile && it.name.endsWith("Test.class") }
        .map { it.relativeTo(classes).path.removeSuffix(".class").replace(File.separatorChar, '.') }
        .sorted()
        .toList()

    for (className in names) {
        val type = try {
            Class.forName(className, true, ClassLoader.getSystemClassLoader())
        } catch (e: Throwable) {
            rows.add(Row(className, "<load>", false, "${e::class.simpleName}: ${e.message}".take(500)))
            continue
        }
        val methods = type.methods.filter { it.name.startsWith("test") && it.parameterCount == 0 }.sortedBy { it.name }
        for (method in methods) {
            val instance = try {
                type.getDeclaredConstructor().newInstance()
            } catch (e: Throwable) {
                rows.add(Row(className, method.name, false, "인스턴스를 만들지 못했다: ${(e.cause ?: e).message}".take(500)))
                continue
            }
            try {
                method.invoke(instance)
                rows.add(Row(className, method.name, true, null))
            } catch (e: java.lang.reflect.InvocationTargetException) {
                val cause = e.cause ?: e
                rows.add(Row(className, method.name, false, "${cause::class.simpleName}: ${cause.message}".take(500)))
            } catch (e: Throwable) {
                rows.add(Row(className, method.name, false, "${e::class.simpleName}: ${e.message}".take(500)))
            }
        }
    }

    val body = buildString {
        append("{\"nonce\":").append(json(nonce))
        append(",\"tampered\":").append(json(tampered))
        append(",\"loadError\":null,\"tests\":[")
        rows.forEachIndexed { index, row ->
            if (index > 0) append(',')
            append("{\"module\":").append(json(row.module))
            append(",\"name\":").append(json(row.name))
            append(",\"passed\":").append(row.passed)
            append(",\"message\":").append(json(row.message)).append('}')
        }
        append("]}")
    }
    report.writeText(body)
}
