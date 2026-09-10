package dev.codedrill.judge.protocol

import dev.codedrill.platform.problempackage.Limits
import dev.codedrill.platform.problempackage.Signature

/**
 * 최소 반례 축소 (기술 설계서 §6.3, PRD §3.4 검증군 증거).
 *
 * 떨어진 입력에서 시작해 **더 작으면서도 여전히 틀리는** 입력을 찾는다. 20만 원소짜리
 * 입력에서 틀렸다는 사실은 아무것도 알려 주지 않지만, 세 원소에서 틀렸다는 사실은
 * 대개 원인을 그대로 가리킨다.
 *
 * **참조 풀이가 전제 조건 검사기 노릇을 한다.** 원소를 지우다 보면 "정답은 항상 정확히
 * 하나 존재한다" 같은 문제의 전제를 깨뜨린 입력이 나오는데, 그런 입력에서는 참조 풀이도
 * 터진다. 그것을 후보에서 떨어뜨리면 문제별 shrinker 를 따로 쓰지 않고도 유효한 입력만
 * 남는다 — 무엇이 유효한 입력인지 이미 아는 코드가 저장소에 있다.
 */
data class ShrinkRequest(
    val schemaVersion: String = SCHEMA_VERSION,
    val shrinkId: String,
    val correlationId: String,
    val problemVersionId: String,
    val packageDigest: String,
    val signature: Signature,
    val limits: Limits,
    /** 무엇이 정답인지 정하는 기준이자 전제 조건 검사기. */
    val reference: String,
    val language: Language,
    val source: String,
    /**
     * 축소를 시작할 후보들.
     *
     * **어느 케이스가 떨어졌는지는 제어 영역이 모른다.** 숨은 그룹의 케이스 내역은 판정
     * 봉투에서 잘려 나가기 때문이다 (§8.3). 그래서 한 건을 골라 보내는 대신 후보를 전부
     * 보내고, 어느 것이 재현되는지는 **여기서 실제로 돌려 보고** 정한다.
     *
     * 작은 것부터 보낸다. 재현되는 가장 작은 입력에서 시작하면 줄일 것이 이미 적다.
     */
    val starts: List<List<Any>>,
) {
    companion object {
        const val SCHEMA_VERSION = "1.0"

        /**
         * 축소 라운드 상한.
         *
         * 라운드마다 두 번 컴파일하고 후보들을 돌린다. 끝까지 줄이는 것보다 **답을 언제
         * 주는지**가 중요하므로, 더 줄일 여지가 남아도 여기서 멈추고 지금까지의 최선을 낸다.
         */
        const val MAX_ROUNDS = 12

        /** 한 라운드에 시험할 후보 수. 한 번의 샌드박스 실행에 이만큼이 들어간다. */
        const val CANDIDATES_PER_ROUND = 24

        /**
         * 시작 후보에 실을 원소 수의 상한.
         *
         * 20만 원소짜리 케이스가 여럿이면 메시지 하나가 수십 MB 가 된다. 작은 것부터
         * 채우다 이 예산을 넘기면 멈춘다 — 큰 입력에서만 재현되는 결함은 놓치지만,
         * 그런 결함은 대개 값이 아니라 시간·메모리 쪽이라 축소가 답할 문제가 아니다.
         */
        const val START_BUDGET = 50_000
    }
}

/**
 * 축소 결과.
 *
 * [expected] 는 **축소된 그 입력 하나에 대한** 정답이다. 작은 입력의 답 하나를 알려 주는
 * 것이 이 기능의 요점이며 — 그것 없이는 "여기서 틀렸다"까지만 말하고 무엇이 옳은지는
 * 말하지 못한다 — 숨은 테스트 묶음과는 다른 이야기다.
 */
data class ShrinkReport(
    val schemaVersion: String = ShrinkRequest.SCHEMA_VERSION,
    val shrinkId: String,
    val status: ShrinkStatus,
    val message: String? = null,
    /** 줄인 입력. 못 줄였으면 시작 입력 그대로다. */
    val args: List<Any>? = null,
    /** 시작 입력의 크기와 줄인 뒤의 크기. 배열 원소 수의 합이다. */
    val originalSize: Int = 0,
    val minimalSize: Int = 0,
    val rounds: Int = 0,
    /** 이 입력에서 사용자 코드가 내놓은 것. */
    val actual: String? = null,
    /** 이 입력의 정답. */
    val expected: String? = null,
)

enum class ShrinkStatus {
    /** 반례를 찾았다. 줄었을 수도, 시작 입력이 이미 최소일 수도 있다. */
    FOUND,

    /**
     * 시작 입력에서 두 풀이가 같은 답을 냈다.
     *
     * 실패가 아니다. 재채점으로 판정이 바뀌었거나, 떨어진 이유가 값이 아니라 시간·메모리일
     * 수 있다 — 그때는 작은 입력에서 재현되지 않는 것이 정상이다.
     */
    NOT_REPRODUCED,

    /** 참조나 사용자 코드가 돌지 않았다. 사용자 잘못이 아니다. */
    FAILED,
}
