import { describe, expect, it } from 'vitest'
import { parseCompileLog } from './compileLog'

describe('parseCompileLog', () => {
  it('Kotlin 은 줄과 열을 읽는다', () => {
    const log = [
      "Solution.kt:1:53: error: unresolved reference '이건'.",
      'fun twoSum(nums: IntArray, target: Int): IntArray { 이건 코드가 아니다 }',
      '                                                    ^^',
      "Solution.kt:3:5: error: unresolved reference '아니다'.",
    ].join('\n')
    expect(parseCompileLog(log)).toEqual([
      { line: 1, column: 53, message: "unresolved reference '이건'." },
      { line: 3, column: 5, message: "unresolved reference '아니다'." },
    ])
  })

  it('Java 는 줄만 있다', () => {
    expect(parseCompileLog('Solution.java:4: error: illegal start of expression\n   ^\n1 error')).toEqual([
      { line: 4, column: null, message: 'illegal start of expression' },
    ])
  })

  it('Python 은 사용자 파일을 가리키는 줄과 끝의 오류를 짝짓고 하네스 파일은 건너뛴다', () => {
    const log =
      'Traceback (most recent call last): |   File "/sandbox/src/main.py", line 111, in main |   ' +
      'File "/sandbox/src/solution.py", line 7 |     return x + | SyntaxError: invalid syntax'
    expect(parseCompileLog(log)).toEqual([{ line: 7, column: null, message: 'SyntaxError: invalid syntax' }])
  })

  it('위치가 없는 로그는 빈 목록이다', () => {
    expect(
      parseCompileLog("ImportError: cannot import name 'twoSum' from 'solution'. Did you mean: 'two_sum'?"),
    ).toEqual([])
  })

  it('같은 줄은 한 번만', () => {
    expect(parseCompileLog('Solution.kt:2:1: error: a\nSolution.kt:2:9: error: b')).toHaveLength(1)
  })
})
