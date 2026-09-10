package dev.codedrill.controlplane.workspace

import dev.codedrill.platform.problempackage.ValueType

/**
 * 사용자가 적은 인자가 시그니처와 맞는지 (§6.1).
 *
 * 맞지 않는 입력을 그대로 실행에 넘기면 사용자 코드가 아니라 **하네스가** 깨지고,
 * 사용자에게는 "내 코드가 틀렸다"로 보인다.
 *
 * JSON 은 정수와 실수를 구분해 주지 않고 배열의 원소 타입도 말해 주지 않는다. 여기서
 * 보지 않으면 그 값이 세 언어의 하네스에 그대로 들어가고, 각 언어가 **서로 다른 방식으로**
 * 깨진다.
 *
 * 시험 실행과 변이 평가가 같은 검사를 쓴다. 나눠 두면 한쪽에서 통과한 케이스가 다른
 * 쪽에서 거절되고, 사용자는 같은 입력이 왜 어떤 버튼에서만 되는지 알 수 없다.
 */
internal object CaseShape {

    /** 맞으면 null, 아니면 사람이 읽을 사유. */
    fun mismatch(args: List<Any>, types: List<ValueType>): String? {
        if (args.size != types.size) {
            return "인자 ${types.size}개가 필요한데 ${args.size}개다"
        }
        types.forEachIndexed { index, type ->
            if (!matches(args[index], type)) {
                return "${index + 1}번 인자가 $type 이 아니다"
            }
        }
        return null
    }

    private fun matches(value: Any?, type: ValueType): Boolean = when (type) {
        ValueType.INT -> value is Number && value.toDouble() % 1.0 == 0.0
        ValueType.STRING -> value is String
        ValueType.INT_ARRAY -> value is List<*> && value.all { matches(it, ValueType.INT) }
        ValueType.STRING_ARRAY -> value is List<*> && value.all { matches(it, ValueType.STRING) }
        ValueType.INT_MATRIX -> value is List<*> && value.all { matches(it, ValueType.INT_ARRAY) }
    }
}
