import { useEffect, useState } from 'react'
import { createSubmission, getProblem } from './api/client'
import { ReplayView } from './features/replay/ReplayView'
import { useSubmissionEvents } from './features/submissions/useSubmissionEvents'
import { VerdictPanel } from './features/submissions/VerdictPanel'
import { Workspace } from './features/workspace/Workspace'
import type { Problem } from './shared/types'

const PROBLEM_SLUG = 'two-sum'

const STARTER = `fun twoSum(nums: IntArray, target: Int): IntArray {
    val seen = HashMap<Int, Int>()
    for (i in nums.indices) {
        Drill.visit(i, nums[i])
        val j = seen[target - nums[i]]
        if (j != null) {
            Drill.match(j, i)
            return intArrayOf(j, i)
        }
        Drill.compare(i, target - nums[i])
        seen.putIfAbsent(nums[i], i)
    }
    error("정답은 항상 존재한다")
}
`

/**
 * 첫 vertical slice 의 화면 (기술 설계서 §16.2).
 *
 * 문제 열기 → 코드 작성 → 제출 → SSE → 판정 → 리플레이. Judge first 원칙에 따라 판정
 * 결과가 항상 먼저 보이고, 리플레이는 그 아래에 붙는다 (디자인 설계서 §0.2).
 */
export function App() {
  const [problem, setProblem] = useState<Problem | null>(null)
  const [source, setSource] = useState(STARTER)
  const [submissionId, setSubmissionId] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const { submission, trace } = useSubmissionEvents(submissionId)

  useEffect(() => {
    getProblem(PROBLEM_SLUG).then(setProblem).catch((e: Error) => setError(e.message))
  }, [])

  const submit = async () => {
    if (!problem) return
    setSubmitting(true)
    setError(null)
    try {
      const created = await createSubmission(problem.id, problem.version, source)
      setSubmissionId(created.id)
    } catch (e) {
      setError(e instanceof Error ? e.message : '제출에 실패했습니다')
    } finally {
      setSubmitting(false)
    }
  }

  // 리플레이에 쓸 입력. 트레이스가 가리키는 공개 케이스의 nums 배열이다.
  const replayInput = numberArray(problem?.samples[0]?.args[0])

  return (
    <div className="app">
      <header className="top">
        <strong>CodeDrill</strong>
        <span className="muted">문제를 푸는 것이 아니라, 문제 해결 역량을 훈련합니다</span>
      </header>

      {error && <p className="warn">{error}</p>}

      <main className="columns">
        <section className="panel statement">
          <h3>{problem?.title ?? '불러오는 중…'}</h3>
          {problem && (
            <>
              <p className="muted">
                {problem.timeMillis}ms · {problem.memoryMb}MB · {problem.signature}
              </p>
              <pre className="statement-body">{problem.statement}</pre>
            </>
          )}
        </section>

        <div className="stack">
          <Workspace
            source={source}
            onChange={setSource}
            onSubmit={submit}
            submitting={submitting}
          />
          {submission && <VerdictPanel submission={submission} />}
          {trace && <ReplayView capture={trace} input={replayInput} />}
        </div>
      </main>
    </div>
  )
}

function numberArray(value: unknown): number[] {
  return Array.isArray(value) ? value.filter((item): item is number => typeof item === 'number') : []
}
