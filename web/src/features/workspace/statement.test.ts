import { describe, expect, it } from 'vitest'
import { functionName, splitStatement } from './statement'

const STATEMENT = `# 구간 더하기 뒤의 배열

길이 \`n\` 의 0 배열에 갱신을 적용한다.

\`\`\`kotlin
fun applyRangeUpdates(n: Int, updates: IntArray): IntArray
\`\`\`

## 실행 리플레이 계측 (선택)

풀이 과정을 되짚고 싶으면 \`Drill\` SDK 를 호출한다.

\`\`\`kotlin
Drill.write(l, v)
\`\`\`

## 제약

- \`1 <= n <= 100_000\`
`

describe('splitStatement', () => {
  it('제목과 같은 H1 을 빼고 계측 절을 따로 떼어 낸다', () => {
    const parts = splitStatement(STATEMENT, '구간 더하기 뒤의 배열')
    expect(parts.body.startsWith('길이 `n`')).toBe(true)
    expect(parts.body).not.toContain('실행 리플레이 계측')
    expect(parts.body).not.toContain('Drill.write')
    // 계측 절 뒤의 절은 본문에 남는다
    expect(parts.body).toContain('## 제약')
    expect(parts.instrumentation).toContain('Drill.write(l, v)')
    expect(parts.instrumentation).not.toContain('## 제약')
  })

  it('제목이 다르면 H1 을 지우지 않는다', () => {
    expect(splitStatement('# 다른 제목\n\n본문', '제목').body).toBe('# 다른 제목\n\n본문')
  })

  it('계측 절이 없으면 본문 그대로다', () => {
    expect(splitStatement('# 제목\n\n본문\n\n## 제약\n- x', '제목')).toEqual({
      body: '본문\n\n## 제약\n- x',
      instrumentation: null,
    })
  })

  it('계측 절이 마지막이면 끝까지가 그 절이다', () => {
    const parts = splitStatement('본문\n\n## 실행 리플레이 계측\n\nDrill.visit(i)\n', '제목')
    expect(parts.body).toBe('본문')
    expect(parts.instrumentation).toBe('Drill.visit(i)')
  })
})

describe('functionName', () => {
  it('시그니처에서 함수 이름을 꺼낸다', () => {
    expect(functionName('fun twoSum(nums: IntArray, target: Int): IntArray')).toBe('twoSum')
  })
})
