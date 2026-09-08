import type { SubmissionLanguage } from '../../shared/types'

/**
 * 언어별 시작 코드.
 *
 * 함수 시그니처는 문제 패키지가 정하지만, 그것을 어떤 모양으로 감싸는지는 언어마다
 * 다르다 (Kotlin 최상위 함수, Java `Solution` 클래스, Python 모듈 함수). 그 차이를
 * 사용자가 추측하게 두지 않는다.
 */
export function starterFor(language: SubmissionLanguage, signature: string, name: string): string {
  switch (language) {
    case 'KOTLIN':
      return `${signature} {\n    TODO("풀이를 작성하세요")\n}\n`
    case 'JAVA':
      return [
        'class Solution {',
        `    ${javaSignature(signature, name)} {`,
        '        throw new UnsupportedOperationException("풀이를 작성하세요");',
        '    }',
        '}',
        '',
      ].join('\n')
    case 'PYTHON':
      return `def ${name}(${pythonParams(signature)}):\n    raise NotImplementedError("풀이를 작성하세요")\n`
  }
}

/** `fun twoSum(nums: IntArray, target: Int): IntArray` → `public int[] twoSum(int[] nums, int target)` */
function javaSignature(signature: string, name: string): string {
  const params = pythonParams(signature)
    .split(', ')
    .filter(Boolean)
    .map((param) => `${javaType(paramType(signature, param))} ${param}`)
    .join(', ')
  return `public ${javaType(returnType(signature))} ${name}(${params})`
}

function pythonParams(signature: string): string {
  const inside = signature.slice(signature.indexOf('(') + 1, signature.lastIndexOf(')'))
  return inside
    .split(',')
    .map((part) => part.split(':')[0]?.trim() ?? '')
    .filter(Boolean)
    .join(', ')
}

function paramType(signature: string, name: string): string {
  const inside = signature.slice(signature.indexOf('(') + 1, signature.lastIndexOf(')'))
  const match = inside.split(',').find((part) => part.trim().startsWith(name + ':'))
  return match?.split(':')[1]?.trim() ?? 'Int'
}

function returnType(signature: string): string {
  return signature.slice(signature.lastIndexOf('):') + 2).trim()
}

/**
 * 코틀린 시그니처의 타입 이름을 자바 것으로 옮긴다.
 *
 * 서버가 내려보내는 시그니처가 유일한 입력이라, 여기 없는 이름은 `int` 로 떨어진다.
 * 값 타입을 늘리면 이 표도 함께 늘려야 한다 — 빠뜨리면 사용자가 받는 시작 코드가
 * 컴파일되지 않는다.
 */
function javaType(kotlinType: string): string {
  switch (kotlinType) {
    case 'IntArray':
      return 'int[]'
    case 'String':
      return 'String'
    case 'Array<String>':
      return 'String[]'
    case 'Array<IntArray>':
      return 'int[][]'
    default:
      return 'int'
  }
}
