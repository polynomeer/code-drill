package dev.codedrill.judge.protocol

import dev.codedrill.platform.problempackage.GroupPolicy
import dev.codedrill.platform.problempackage.Limits
import dev.codedrill.platform.problempackage.Signature
import dev.codedrill.platform.problempackage.TestCase

/**
 * Orchestrator 가 Runner 에게 보내는 실행 요청 (기술 설계서 §4.1, §5.3).
 *
 * Runner 는 Control DB 에 접근할 수 없으므로 채점에 필요한 것이 전부 이 봉투에 담겨야
 * 한다. 문제 버전과 [packageDigest] 를 함께 실어, Runner 가 자기가 받은 테스트 번들이
 * 요청과 같은 것인지 검증할 수 있게 한다 (§4.1).
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
    val groups: List<RequestedGroup>,
) {
    companion object {
        const val SCHEMA_VERSION = "1.0"
    }
}

/** 그룹 정책과 그 그룹에 속한 케이스. 실행 순서는 목록 순서를 따른다. */
data class RequestedGroup(val policy: GroupPolicy, val cases: List<TestCase>)

/** MVP 지원 언어 (§1.1). 슬라이스는 KOTLIN 하나만 구현한다. */
enum class Language { KOTLIN }
