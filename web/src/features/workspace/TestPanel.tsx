import { useEffect, useRef, useState } from 'react'
import { getTrial, startTrial } from '../../api/client'
import { VERDICT_LABEL } from '../../shared/types'
import type { Problem, SubmissionLanguage, Trial } from '../../shared/types'

/**
 * 테스트 패널 (PRD §5.2, 기획서 부록 A 실행 도메인).
 *
 * **제출하지 않고 돌려 본다.** 지금까지는 입력 하나를 확인하려면 제출해야 했고, 그래서
 * 판정 이력에 확인용 오답이 쌓였다.
 *
 * 기대 출력은 적지 않아도 된다. 모르는 채로 "무엇이 나오나" 보려는 경우가 실제로 더
 * 흔하고, 그때 억지로 적게 하면 아무 값이나 넣게 된다. 적지 않은 케이스는 맞고 틀림을
 * 말하지 않고 나온 값만 보여준다.
 */
export function TestPanel({
  problem,
  language,
  source,
}: {
  problem: Problem
  language: SubmissionLanguage
  source: string
}) {
  const [text, setText] = useState('')
  const [trial, setTrial] = useState<Trial | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [running, setRunning] = useState(false)

  // 폴링 타이머. 문제를 바꾸거나 화면을 떠나면 멈춰야 한다.
  const timer = useRef<number | null>(null)
  useEffect(() => () => { if (timer.current) window.clearTimeout(timer.current) }, [])

  // 문제가 바뀌면 예제로 다시 채운다. 이전 문제의 입력이 남아 있으면 인자 개수부터
  // 맞지 않아, 사용자는 자기 코드가 틀린 줄 안다.
  useEffect(() => {
    setText(problem.samples.map((sample) => JSON.stringify(sample.args)).join('\n'))
    setTrial(null)
    setError(null)
  }, [problem.id])

  const run = async () => {
    const parsed = parseCases(text)
    if ('error' in parsed) {
      setError(parsed.error)
      return
    }
    setError(null)
    setRunning(true)
    try {
      const started = await startTrial(problem.id, language, source, parsed.cases)
      setTrial(started)
      poll(started.id)
    } catch (e) {
      setRunning(false)
      setError(e instanceof Error ? e.message : '실행에 실패했습니다')
    }
  }

  const poll = (id: string) => {
    timer.current = window.setTimeout(async () => {
      try {
        const next = await getTrial(id)
        setTrial(next)
        if (next.status === 'PENDING') poll(id)
        else setRunning(false)
      } catch {
        // 한 번 실패했다고 포기하지 않는다. 결과는 이미 큐에 있고, 다음 폴링에서 온다.
        poll(id)
      }
    }, 600)
  }

  const inputs = parseCases(text)

  return (
    <section className="panel test-panel">
      <div className="editor-header">
        <h3>테스트</h3>
        <button onClick={() => void run()} disabled={running || !source.trim()}>
          {running ? '실행 중…' : '실행'}
        </button>
      </div>

      <label className="muted small" htmlFor="test-input">
        한 줄에 한 케이스. 인자를 JSON 배열로 적는다 — {problem.signature}
      </label>
      <textarea
        id="test-input"
        className="test-input mono"
        rows={Math.min(6, Math.max(3, text.split('\n').length))}
        value={text}
        spellCheck={false}
        onChange={(event) => setText(event.target.value)}
      />
      <p className="muted small">
        기대 출력을 확인하려면 <code className="mono">[인자들] =&gt; 기대값</code> 으로 적는다.
        적지 않으면 나온 값만 보여준다.
      </p>

      {error && <p className="warn">{error}</p>}
      {'error' in inputs && text.trim() !== '' && <p className="warn">{inputs.error}</p>}

      {trial?.compileLog && (
        <pre className="compile-log mono">{trial.compileLog}</pre>
      )}

      {trial && trial.cases.length > 0 && (
        <ul className="test-results">
          {trial.cases.map((result) => {
            const expected = 'cases' in inputs ? inputs.cases[result.index]?.expected : undefined
            const judged = expected !== undefined
            return (
              <li key={result.index}>
                <span className="test-mark">
                  {/* 기대를 적지 않았으면 맞고 틀림을 말하지 않는다. 모르는 것을 "틀림"으로
                      표시하면 사용자는 멀쩡한 출력을 보고 고치려 든다. */}
                  {!judged ? '·' : result.outcome === 'ACCEPTED' ? '✓' : '✗'}
                </span>
                <span className="mono test-actual">{result.actual ?? '—'}</span>
                <span className="muted small">
                  {judged && result.outcome !== 'ACCEPTED' && (
                    <>기대 {JSON.stringify(expected)} · </>
                  )}
                  {result.outcome !== 'ACCEPTED' && !judged
                    ? VERDICT_LABEL[result.outcome]
                    : result.message ?? ''}
                  {' '}
                  {result.wallTimeMillis}ms
                </span>
              </li>
            )
          })}
        </ul>
      )}
    </section>
  )
}

/**
 * 사용자가 적은 텍스트를 케이스로 읽는다.
 *
 * 한 줄에 한 케이스, `[인자들]` 또는 `[인자들] => 기대값` 이다. JSON 을 그대로 쓰는 이유는
 * 값 타입이 다섯 가지(정수·문자열·배열·문자열 배열·격자)라 별도 문법을 만들면 그것을 또
 * 배워야 하기 때문이다.
 *
 * 틀린 줄은 **몇 번째 줄인지** 말해 준다. "JSON 이 잘못됐다"만으로는 열 줄 중 어디인지
 * 찾을 수 없다.
 */
function parseCases(
  text: string,
): { cases: { args: unknown[]; expected?: unknown }[] } | { error: string } {
  const lines = text.split('\n').map((line) => line.trim()).filter((line) => line !== '')
  if (lines.length === 0) return { error: '케이스를 한 줄 이상 적는다' }

  const cases: { args: unknown[]; expected?: unknown }[] = []
  for (const [index, line] of lines.entries()) {
    const at = line.indexOf('=>')
    const argsText = at >= 0 ? line.slice(0, at).trim() : line
    const expectedText = at >= 0 ? line.slice(at + 2).trim() : null

    let args: unknown
    try {
      args = JSON.parse(argsText)
    } catch {
      return { error: `${index + 1}번 줄: 인자를 JSON 배열로 읽지 못했다` }
    }
    if (!Array.isArray(args)) return { error: `${index + 1}번 줄: 인자는 배열이어야 한다` }

    if (expectedText === null) {
      cases.push({ args })
      continue
    }
    try {
      cases.push({ args, expected: JSON.parse(expectedText) })
    } catch {
      return { error: `${index + 1}번 줄: 기대값을 JSON 으로 읽지 못했다` }
    }
  }
  return { cases }
}
