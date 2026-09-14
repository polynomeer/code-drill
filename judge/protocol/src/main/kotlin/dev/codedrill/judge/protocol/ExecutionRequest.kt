package dev.codedrill.judge.protocol

import dev.codedrill.platform.problempackage.GroupPolicy
import dev.codedrill.platform.problempackage.Limits
import dev.codedrill.platform.problempackage.Signature
import dev.codedrill.platform.problempackage.TestCase

/**
 * Orchestrator 가 Runner 에게 보내는 실행 요청 (기술 설계서 §4.1, §5.3).
 *
 * Runner 는 Control DB 에 접근할 수 없으므로 채점에 필요한 것이 전부 이 봉투에 담기거나,
 * 봉투가 가리키는 곳에 있어야 한다. 문제 버전과 [packageDigest] 를 함께 실어, Runner 가
 * 자기가 받은 테스트 번들이 요청과 같은 것인지 검증할 수 있게 한다 (§4.1).
 *
 * **테스트는 두 길로 온다.** [groups] 에 그대로 실리거나(시험 실행·축소·실험실처럼 사용자가
 * 적은 입력), [bundle] 이 가리키는 오브젝트 스토어의 번들에 있다(판정, §8.3). 판정의
 * 테스트를 메시지에 실으면 큰 문제가 브로커의 메시지 크기 한계에 먼저 걸린다 — 문제 하나의
 * 테스트가 수 MB 다. Runner 는 [bundle] 을 [groups] 로 풀어 낸 뒤 둘을 구분하지 않는다.
 */
data class ExecutionRequest(
    val schemaVersion: String = SCHEMA_VERSION,
    val executionId: String,
    val submissionId: String,
    val attempt: Int,
    val fencingToken: FencingToken,
    val correlationId: String,
    val problemVersionId: String,
    val packageDigest: String,
    val language: Language,
    val source: String,
    val signature: Signature,
    val limits: Limits,
    /** 메시지에 실린 케이스. [bundle] 이 있으면 비어 있다. */
    val groups: List<RequestedGroup> = emptyList(),
    /** JUDGE 는 계측 없이, TRACE 는 공개 케이스만 계측해 실행한다 (§7.1). */
    val mode: ExecutionMode = ExecutionMode.JUDGE,
    /** 판정의 테스트가 있는 곳. Runner 가 받아 digest 를 확인하고 [groups] 로 푼다. */
    val bundle: BundleRef? = null,
    /**
     * 번들에서 고를 케이스. null 이면 전부다.
     *
     * 트레이스는 케이스 하나만 되짚는다 (§7.1). 그 하나를 메시지에 실어도 되지만, 그러면
     * 테스트가 메시지로 오는 길이 하나 더 생기고 그 길은 숨은 케이스도 지날 수 있다.
     */
    val selection: List<CaseSelection>? = null,
) {
    companion object {
        const val SCHEMA_VERSION = "1.1"
    }
}

/** 그룹 정책과 그 그룹에 속한 케이스. 실행 순서는 목록 순서를 따른다. */
data class RequestedGroup(val policy: GroupPolicy, val cases: List<TestCase>)

/** 오브젝트 스토어의 번들 하나. [digest] 는 내용의 SHA-256 이며 받는 쪽이 대조한다. */
data class BundleRef(val key: String, val digest: String)

data class CaseSelection(val groupId: String, val caseId: String)

/**
 * MVP 지원 언어 (§1.1).
 *
 * 언어마다 [dev.codedrill.judge.runner.execution.adapter.RuntimeAdapter] 구현이 하나씩
 * 있다. 시그니처와 테스트 데이터는 언어 중립이므로 문제 패키지는 언어를 알지 못한다.
 */
enum class Language { KOTLIN, JAVA, PYTHON }
