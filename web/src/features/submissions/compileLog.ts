/**
 * 컴파일 오류에서 줄·열을 찾는다 (디자인 설계서 §6.3 — 오류 위치를 누르면 그 코드로).
 *
 * 하네스는 사용자 코드를 감싸지 않고 그대로 파일에 넣는다 — Kotlin 은 `Solution.kt`, Java 는
 * `Solution.java`, Python 은 `solution.py`. 그래서 로그의 줄 번호가 곧 편집기의 줄 번호다.
 * 하네스 자신의 파일(`main.py` 등)을 가리키는 줄은 고를 이유가 없으므로 건너뛴다.
 */
export type CompileError = { line: number; column: number | null; message: string }

const KOTLIN = /Solution\.kt:(\d+):(\d+):\s*(?:error|warning):\s*(.*)/
const JAVA = /Solution\.java:(\d+):\s*error:\s*(.*)/
const PYTHON = /solution\.py",\s*line\s*(\d+)/

export function parseCompileLog(log: string): CompileError[] {
  // 파이썬 로그는 줄바꿈 대신 ` | ` 로 이어져 오기도 한다.
  const lines = log.split(/\n| \| /)
  const found: CompileError[] = []

  lines.forEach((text, index) => {
    const kotlin = KOTLIN.exec(text)
    if (kotlin) {
      found.push({ line: Number(kotlin[1]), column: Number(kotlin[2]), message: kotlin[3]!.trim() })
      return
    }
    const java = JAVA.exec(text)
    if (java) {
      found.push({ line: Number(java[1]), column: null, message: java[2]!.trim() })
      return
    }
    const python = PYTHON.exec(text)
    if (python) {
      // 사유는 트레이스백의 맨 끝 줄이다 (`SyntaxError: …`, `NameError: …`)
      const reason = lines.slice(index + 1).reverse().find((rest) => /^\s*\w+(Error|Exception)\b/.test(rest))
      found.push({ line: Number(python[1]), column: null, message: reason?.trim() ?? '' })
    }
  })

  // 같은 줄을 여러 번 가리키면 처음 것만 — 버튼이 줄마다 하나면 된다
  return found.filter((item, index) => found.findIndex((other) => other.line === item.line) === index)
}
