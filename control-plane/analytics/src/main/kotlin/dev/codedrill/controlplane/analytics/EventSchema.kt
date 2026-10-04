package dev.codedrill.controlplane.analytics

/**
 * 받는 이벤트와 속성 (디자인 설계서 §16.1). **여기 없는 것은 받지 않는다.**
 *
 * §16.2 "소스 코드, 입력 전문, 출력 전문을 분석 이벤트에 포함하지 않습니다"를 서버가 지킨다. 클라이언트도
 * 같은 표를 따르지만, 클라이언트의 실수(속성 하나를 잘못 넘김)가 곧 개인정보 사고가 되면 안 된다. 그래서:
 * - 속성 이름이 표에 없으면 버린다
 * - 문자열은 [MAX_STRING] 자까지, 정해진 값이 있는 속성은 그 값만 — 코드 한 줄도 들어올 자리가 없다
 * - 수는 수만
 */
object EventSchema {
    const val MAX_STRING = 64
    const val MAX_BATCH = 20

    sealed interface Prop {
        data class OneOf(val values: Set<String>) : Prop
        data object Id : Prop
        data object Number : Prop
        data object Bool : Prop
    }

    private val LANGUAGE = Prop.OneOf(setOf("KOTLIN", "JAVA", "PYTHON"))

    val EVENTS: Map<String, Map<String, Prop>> = mapOf(
        "problem_list_view" to mapOf("filters" to Prop.Number, "resultCount" to Prop.Number, "sort" to Prop.Id),
        "problem_open" to mapOf(
            "source" to Prop.OneOf(setOf("list", "search", "prescription", "contest", "link", "direct")),
            "problemId" to Prop.Id,
        ),
        "run_requested" to mapOf("type" to Prop.OneOf(setOf("sample", "custom")), "language" to LANGUAGE),
        "submission_created" to mapOf("problem" to Prop.Id, "language" to LANGUAGE),
        "verdict_viewed" to mapOf("verdict" to Prop.Id, "latency" to Prop.Number),
        "replay_opened" to mapOf("entry" to Prop.OneOf(setOf("drawer", "page", "link")), "traceType" to Prop.Id),
        "replay_seeked" to mapOf(
            "from" to Prop.Number,
            "to" to Prop.Number,
            "method" to Prop.OneOf(setOf("scrub", "key", "button", "line", "marker", "divergence", "list")),
        ),
        "code_trace_link_used" to mapOf(
            "direction" to Prop.OneOf(setOf("code-to-state", "state-to-code")),
            "eventType" to Prop.Id,
        ),
        "post_ac_action" to mapOf("action" to Prop.OneOf(setOf("replay", "editorial", "next", "compare"))),
    )

    /** 받을 수 있게 다듬은 속성, 또는 이벤트 자체를 받지 않으면 null. */
    fun clean(name: String, props: Map<String, Any?>): Map<String, Any>? {
        val schema = EVENTS[name] ?: return null
        return props.mapNotNull { (key, value) ->
            val rule = schema[key] ?: return@mapNotNull null
            accepted(rule, value)?.let { key to it }
        }.toMap()
    }

    private fun accepted(rule: Prop, value: Any?): Any? = when (rule) {
        is Prop.OneOf -> (value as? String)?.takeIf { it in rule.values }
        // 식별자 모양만 — 글자·숫자·`-_.:` 로 MAX_STRING 자. 공백·줄바꿈·괄호가 든 코드는 여기서 걸린다
        Prop.Id -> (value as? String)?.takeIf { it.length <= MAX_STRING && ID.matches(it) }
        Prop.Number -> (value as? Number)?.toDouble()?.takeIf { it.isFinite() }
        Prop.Bool -> value as? Boolean
    }

    private val ID = Regex("^[A-Za-z0-9_.:\\-]+$")
}
