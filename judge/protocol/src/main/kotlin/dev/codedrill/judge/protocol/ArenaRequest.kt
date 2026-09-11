package dev.codedrill.judge.protocol

import dev.codedrill.platform.problempackage.DefectKind
import dev.codedrill.platform.problempackage.Limits
import dev.codedrill.platform.problempackage.MutantSource
import dev.codedrill.platform.problempackage.Signature

/**
 * 반례 아레나 (기획서 §8.3).
 *
 * > 익명화된 오답을 실패시키는 입력을 작성합니다. 성공한 반례를 자동 축소하고 깨뜨린
 * > 가정을 분류합니다.
 *
 * 변이 평가(FR-804)의 거울상이다. 저기서는 사용자의 **테스트 묶음**이 오답을 얼마나 잡는지
 * 재고, 여기서는 사용자가 **입력 하나**로 특정 오답을 깨뜨린다. 그래서 봉투에 오답의
 * 이름이 실린다 — 변이 평가는 이름을 감추지만, 아레나는 무엇을 깨뜨리려는지 알아야 한다.
 * 그것이 허용되는 이유는 아레나가 그 문제를 맞힌 사람에게만 열리기 때문이다.
 */
data class ArenaRequest(
    val schemaVersion: String = SCHEMA_VERSION,
    val attemptId: String,
    val correlationId: String,
    val problemVersionId: String,
    val packageDigest: String,
    val signature: Signature,
    val limits: Limits,
    /** 무엇이 정답인지 정하는 기준이자 입력의 유효성 검사기. */
    val reference: String,
    val mutants: List<MutantSource>,
    /** 사용자가 적은 입력. */
    val args: List<Any>,
) {
    companion object {
        const val SCHEMA_VERSION = "1.0"
    }
}

data class ArenaReport(
    val schemaVersion: String = ArenaRequest.SCHEMA_VERSION,
    val attemptId: String,
    val status: ArenaStatus,
    val message: String? = null,
    val results: List<ArenaResult> = emptyList(),
)

/**
 * 오답 하나에 대한 결과.
 *
 * [minimalArgs] 는 깨뜨린 입력을 축소한 것이다 (§8.3 "성공한 반례를 자동 축소"). 사용자가
 * 적은 입력이 이미 작으면 그대로다. 축소가 실제로 줄였다면 그 사실이 곧 "더 작은 반례가
 * 있었다"는 배움이다.
 */
data class ArenaResult(
    val name: String,
    val kind: DefectKind,
    val broken: Boolean,
    /** 오답이 내놓은 것. 깨뜨리지 못했으면 정답과 같다. */
    val actual: String?,
    val minimalArgs: List<Any>? = null,
    val minimalSize: Int? = null,
)

enum class ArenaStatus {
    COMPLETED,

    /** 참조 풀이가 이 입력을 다루지 못했다 — 문제의 전제를 깨뜨린 입력이다. */
    INVALID_INPUT,

    FAILED,
}
