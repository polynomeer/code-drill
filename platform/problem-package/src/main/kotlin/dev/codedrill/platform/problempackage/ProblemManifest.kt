package dev.codedrill.platform.problempackage

import com.fasterxml.jackson.annotation.JsonIgnore

/**
 * Problem Package manifest (기술 설계서 §6.1).
 *
 * 문제 메타·제한·함수 시그니처·그룹 정책을 담는다. 공개된 버전의 manifest 와 테스트는
 * 불변이며, 그 내용의 digest 가 [ProblemPackage.packageDigest] 로 고정된다. 제출은 이
 * digest 를 고정해 두므로 나중에 문제가 개정돼도 과거 판정을 재현할 수 있다 (§8.1).
 */
data class ProblemManifest(
    val id: String,
    val version: Int,
    val title: String,
    val statement: String,
    val limits: Limits,
    val signature: Signature,
    val groups: List<GroupPolicy>,
) {
    init {
        require(groups.isNotEmpty()) { "테스트 그룹이 최소 하나는 있어야 한다" }
        require(groups.map { it.id }.distinct().size == groups.size) { "그룹 id 는 버전 내 유일하다" }
        val weight = groups.sumOf { it.weight }
        require(weight == 100) { "그룹 weight 합은 100 이어야 한다: $weight" }
    }
}

/** 실행 제한. 그룹별 [GroupPolicy.limitMultiplier] 로 조정된다 (§6.2). */
data class Limits(
    val timeMillis: Long,
    val memoryMb: Int,
    val outputBytes: Long,
)

/** 함수형 풀이 시그니처. Runner 가 이걸로 하네스를 생성한다 (§5.3). */
data class Signature(
    val name: String,
    val parameters: List<Parameter>,
    val returns: ValueType,
)

data class Parameter(val name: String, val type: ValueType)

/**
 * 하네스 생성과 기대 출력 비교에 쓰는 값 타입.
 *
 * 새 타입을 추가하면 세 언어 어댑터의 하네스와 [dev.codedrill.platform.problempackage]
 * 바깥의 인코딩이 함께 늘어야 한다. 컴파일러가 `when` 을 빠짐없이 채우게 만들어 두어,
 * 하나를 빠뜨린 채로는 빌드가 되지 않는다.
 */
enum class ValueType { INT, INT_ARRAY, STRING, STRING_ARRAY }

/** 테스트 그룹 정책 (§6.2). */
data class GroupPolicy(
    val id: String,
    val weight: Int,
    val visibility: Visibility,
    val aggregation: Aggregation,
    val stopPolicy: StopPolicy,
    val limitMultiplier: LimitMultiplier = LimitMultiplier(),
) {
    /**
     * 공개 그룹만 입력·기대 출력을 사용자에게 보여줄 수 있다 (§7.1, §11.1).
     *
     * 파생값이므로 와이어에 싣지 않는다. 직렬화하면 수신 측에서 알 수 없는 필드가 되고,
     * 무엇보다 진실의 원천이 두 곳이 된다.
     */
    @get:JsonIgnore
    val exposesInput: Boolean get() = visibility == Visibility.PUBLIC
}

enum class Visibility { PUBLIC, PROPERTY, HIDDEN }

enum class Aggregation { ALL_OR_NOTHING, SUM }

/** 그룹 안에서 실패를 만났을 때 계속 돌릴지 멈출지. */
enum class StopPolicy { CONTINUE, FAIL_FAST }

data class LimitMultiplier(val time: Double = 1.0, val memory: Double = 1.0)
