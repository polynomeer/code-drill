package dev.codedrill.judge.protocol

import dev.codedrill.platform.problempackage.DefectKind
import dev.codedrill.platform.problempackage.Limits
import dev.codedrill.platform.problempackage.MutantSource
import dev.codedrill.platform.problempackage.Signature
import dev.codedrill.platform.problempackage.TestCase

/**
 * 사용자 테스트를 대표 오답에 겨눠 본다 (PRD FR-804).
 *
 * > 사용자 테스트를 변이 구현에 실행해 mutation score와 누락 유형을 제공합니다.
 * > 숨은 정답 데이터는 노출하지 않고 탐지한 결함군을 설명합니다.
 *
 * **한 통에 다 담아 한 번에 보낸다.** 오답마다 실행 요청을 하나씩 보내면 제어 영역이
 * N+1 개의 결과를 짝지어야 하고, 그중 하나가 유실됐을 때 "아직 안 왔다"와 "영영 안
 * 온다"를 가릴 방법이 없다. 하나로 묶으면 그 문제가 없어진다 — 오거나 안 오거나다.
 *
 * 언어는 항상 Kotlin 이다. 여기서 도는 것은 사용자 코드가 아니라 **저작자의 정답과
 * 오답**이고, 그것들은 Kotlin 으로만 쓰여 있다 (§6.1). 사용자가 Python 으로 풀었어도
 * 그가 적은 테스트(입력과 기대 출력)는 언어를 타지 않으므로 그대로 겨눌 수 있다.
 */
data class MutationRequest(
    val schemaVersion: String = SCHEMA_VERSION,
    val evaluationId: String,
    val correlationId: String,
    val problemVersionId: String,
    val packageDigest: String,
    val signature: Signature,
    val limits: Limits,
    /** 무엇이 정답인지 정하는 기준. 사용자가 적은 기대 출력을 이것으로 검산한다. */
    val reference: String,
    val mutants: List<MutantSource>,
    /** 사용자가 적은 케이스. `expected` 가 없는 것은 시험이 아니므로 제외하고 보낸다. */
    val cases: List<TestCase>,
) {
    companion object {
        const val SCHEMA_VERSION = "1.0"
    }
}

/**
 * 변이 평가 결과.
 *
 * **오답의 이름도 소스도 담지 않는다.** 담으면 그것을 저장하는 표와 그것을 그리는
 * 화면이 생기고, 언젠가 사용자에게 나간다. 나가도 되는 것은 [DefectKind] 까지다.
 */
data class MutationReport(
    val schemaVersion: String = MutationRequest.SCHEMA_VERSION,
    val evaluationId: String,
    val status: MutationStatus,
    /** 실패했을 때의 사유. 성공이면 null 이다. */
    val message: String? = null,
    /**
     * 기대 출력이 실제 정답과 다른 케이스의 번호 (1부터).
     *
     * 정답 값은 알려 주지 않는다. "당신의 기대가 틀렸다"까지가 사용자가 스스로 고칠 수
     * 있는 정보이고, 그 너머는 답을 주는 것이다.
     */
    val mistakenCases: List<Int> = emptyList(),
    val mutants: List<MutantOutcome> = emptyList(),
) {
    /**
     * mutation score.
     *
     * [DefectKind.PERFORMANCE] 를 뺀 나머지로 센다 — 이유는 그 상수에 적었다. 셀 것이
     * 하나도 없으면 null 이다. 0.0 으로 내리면 화면이 "0% 잡았다"를 말하는데, 그것은
     * 사용자가 못 잡은 것이 아니라 **잴 것이 없었다**는 뜻이다.
     */
    fun score(): Double? = mutants.filter { it.kind.reachableByHandWrittenCase }
        .takeIf { it.isNotEmpty() }
        ?.let { scored -> scored.count { it.killed }.toDouble() / scored.size }
}

/**
 * 오답 하나에 대한 결과.
 *
 * [killedBy] 는 **사용자 자신이 적은** 케이스 번호다. 자기 입력이므로 돌려줘도 새로
 * 알려 주는 것이 없고, "몇 번 케이스가 이 결함을 잡았다"는 다음에 무엇을 더 적을지
 * 정하는 데 실제로 쓰인다.
 */
data class MutantOutcome(
    val kind: DefectKind,
    val killed: Boolean,
    val killedBy: List<Int> = emptyList(),
)

enum class MutationStatus {
    COMPLETED,

    /**
     * 기대 출력을 적은 케이스가 하나도 없었다.
     *
     * 실패가 아니다. 입력만 넣고 돌려 본 것은 시험이 아니므로 잴 것이 없을 뿐이며,
     * 화면은 "0% 잡았다"가 아니라 "기대 출력을 적어야 잴 수 있다"를 말해야 한다.
     */
    NO_CASES,

    /** 정답이나 오답이 돌지 않았다. 사용자 잘못이 아니라 콘텐츠나 플랫폼 문제다. */
    FAILED,
}
