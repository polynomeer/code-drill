import { IN_FLIGHT, VERDICT_LABEL } from '../../shared/types'
import type { Submission } from '../../shared/types'

/**
 * 판정 결과 (디자인 설계서 §1.2 Judge first).
 *
 * 상태와 판정을 색상만으로 구분하지 않는다. 색을 못 보는 사용자도 텍스트로 같은 정보를
 * 얻어야 한다 (§1.1 디자인 목표).
 */
export function VerdictPanel({ submission }: { submission: Submission }) {
  const inFlight = IN_FLIGHT.has(submission.status)

  return (
    <section className="panel">
      <h3>판정</h3>

      {inFlight ? (
        // 데이터가 부족할 때 결과를 단정하지 않는다 (§0.2 No false precision).
        <p className="status">
          <span className="spinner" aria-hidden="true" />
          채점 중 — {submission.status}
        </p>
      ) : (
        <p className={`verdict ${submission.verdict === 'ACCEPTED' ? 'ok' : 'bad'}`}>
          {submission.verdict ? VERDICT_LABEL[submission.verdict] : '알 수 없음'}
          {submission.score !== null && <span className="score">{submission.score}점</span>}
        </p>
      )}

      {submission.compileLog && <pre className="log">{submission.compileLog}</pre>}

      {submission.groups && submission.groups.length > 0 && (
        <table className="groups">
          <thead>
            <tr>
              <th>그룹</th>
              <th>판정</th>
              <th>점수</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {submission.groups.map((group) => (
              <tr key={group.groupId}>
                <td>{group.groupId}</td>
                <td>{VERDICT_LABEL[group.verdict]}</td>
                <td>
                  {group.score} / {group.maxScore}
                </td>
                <td>
                  {group.maxScore > 0 && (
                    <div className="score-bar" aria-hidden="true">
                      <span style={{ width: `${(group.score / group.maxScore) * 100}%` }} />
                    </div>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {/* 숨은 그룹의 케이스 내역은 서버가 이미 잘라 보낸다. 여기서 채우지 않는다. */}
      {submission.groups
        ?.filter((group) => group.cases.length > 0)
        .map((group) => (
          <ul key={group.groupId} className="cases">
            {group.cases.map((testCase) => (
              <li key={testCase.caseId}>
                <span>{testCase.caseId}</span>
                <span>{VERDICT_LABEL[testCase.verdict]}</span>
                <span className="muted">{testCase.measurements.wallTimeMillis}ms</span>
                {testCase.message && <span className="muted">{testCase.message}</span>}
              </li>
            ))}
          </ul>
        ))}
    </section>
  )
}
