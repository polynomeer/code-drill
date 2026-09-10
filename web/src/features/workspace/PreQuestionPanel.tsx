import { useEffect, useState } from 'react'
import { answerPreQuestion, getPreQuestions } from '../../api/client'
import { MISCONCEPTION_LABEL } from '../../shared/types'
import type { AnsweredQuestion, PreQuestion, PreQuestionSet, QuestionKind } from '../../shared/types'

/**
 * 풀이 전 질문 (PRD FR-803, 기획서 §4.3).
 *
 * 코드를 쓰기 **전에** 접근과 비용을 말해 보게 한다. 다 풀고 나서 묻는 것과는 다른 질문이다 —
 * 앞에서 물으면 예측이고 뒤에서 물으면 회상이며, 역량으로 재려는 것은 예측 쪽이다.
 *
 * **관문이 아니다.** 답하지 않아도 제출할 수 있고, 틀려도 아무것도 막지 않는다. 막으면
 * 사용자는 답을 맞히는 데 집중하고, 그러면 예측이 아니라 시험이 된다.
 *
 * 기본은 접힌 상태다. 매번 펼쳐 두면 문제를 열 때마다 넘어야 할 것이 하나 늘고, 그것이
 * 관문이 아니라는 말과 어긋난다.
 */
export function PreQuestionPanel({ problemId }: { problemId: string }) {
  const [set, setSet] = useState<PreQuestionSet | null>(null)
  const [open, setOpen] = useState(false)
  const [busy, setBusy] = useState<QuestionKind | null>(null)
  const [rationale, setRationale] = useState<Record<string, string>>({})

  useEffect(() => {
    setSet(null)
    setOpen(false)
    setRationale({})
    getPreQuestions(problemId).then(setSet).catch(() => setSet(null))
  }, [problemId])

  if (!set || set.questions.length === 0) return null

  const answerOf = (kind: QuestionKind): AnsweredQuestion | undefined =>
    set.answered.find((item) => item.kind === kind)

  const submit = async (question: PreQuestion, choice: string) => {
    setBusy(question.kind)
    try {
      const result = await answerPreQuestion(
        problemId,
        question.kind,
        choice,
        rationale[question.kind]?.trim() || null,
      )
      // 서버가 준 채점 결과로 갈아 끼운다. 같은 질문에 다시 답하면 마지막 것만 보인다.
      setSet((prev) =>
        prev
          ? {
              ...prev,
              answered: [...prev.answered.filter((a) => a.kind !== question.kind), result],
            }
          : prev,
      )
    } finally {
      setBusy(null)
    }
  }

  const done = set.questions.filter((q) => answerOf(q.kind)).length

  return (
    <section className="panel prequestions">
      <div className="problem-head">
        <h3>풀기 전에</h3>
        <button type="button" className="linklike" onClick={() => setOpen((v) => !v)}>
          {open ? '접기' : `펼치기 (${done}/${set.questions.length})`}
        </button>
      </div>

      {!open && (
        <p className="muted small">
          접근과 비용을 먼저 말해 보면, 어디를 잘못 보고 있는지 코드를 쓰기 전에 드러납니다.
        </p>
      )}

      {open &&
        set.questions.map((question) => {
          const answered = answerOf(question.kind)
          return (
            <div key={question.kind} className="settings-block">
              <p className="question-prompt">{question.prompt}</p>

              <div className="filters">
                <fieldset>
                  {question.choices.map((choice) => (
                    <button
                      key={choice}
                      type="button"
                      className={answered?.answer === choice ? 'chip on mono' : 'chip mono'}
                      aria-pressed={answered?.answer === choice}
                      disabled={busy === question.kind}
                      onClick={() => void submit(question, choice)}
                    >
                      {choice}
                    </button>
                  ))}
                </fieldset>
              </div>

              {/* 근거는 선택이다. 필수로 하면 질문 자체를 건너뛰고, 그러면 아무 증거도
                  남지 않는다. */}
              {!answered && (
                <input
                  className="rationale"
                  placeholder="왜 그렇게 생각했나요? (선택)"
                  value={rationale[question.kind] ?? ''}
                  onChange={(event) =>
                    setRationale((prev) => ({ ...prev, [question.kind]: event.target.value }))
                  }
                  aria-label={`${question.prompt} 근거`}
                />
              )}

              {answered && (
                <p className={answered.correct ? 'ok small' : 'warn small'}>
                  {/* 색만으로 알리지 않는다. 기호와 문장이 먼저 읽힌다. */}
                  {answered.correct ? '✓ 맞습니다' : '✗ '}
                  {!answered.correct && answered.misconception && (
                    <>{MISCONCEPTION_LABEL[answered.misconception]} · </>
                  )}
                  {!answered.correct && answered.expected && <>실제로는 {answered.expected}</>}
                </p>
              )}
            </div>
          )
        })}
    </section>
  )
}
