import { useEffect, useState } from 'react'
import { getStats, getWeeklyReport } from '../../api/client'
import { COMPETENCY_LABEL, LEVEL_LABEL, REASON_LABEL, VERDICT_LABEL } from '../../shared/types'
import type { Stats, Verdict, WeeklyReport } from '../../shared/types'

/**
 * 주간 리포트와 통계 (PRD FR-808, 부록 A 통계).
 *
 * 약점·재발·성장 세 칸이 요구사항의 세 단어 그대로다. 행동 칸은 오늘의 처방과 같은
 * 것을 보여준다 — 리포트가 행동을 따로 지어내면 처방과 리포트가 서로 다른 말을 한다.
 *
 * 접혀 있다. 매일 볼 것이 아니라 주에 한 번 볼 것이고, 펼쳐 두면 오늘의 처방을 밀어낸다.
 */
export function WeeklyReportPanel({ onOpenProblem }: { onOpenProblem: (problemId: string) => void }) {
  const [open, setOpen] = useState(false)
  const [report, setReport] = useState<WeeklyReport | null>(null)
  const [stats, setStats] = useState<Stats | null>(null)

  useEffect(() => {
    if (!open) return
    getWeeklyReport().then(setReport).catch(() => setReport(null))
    getStats().then(setStats).catch(() => setStats(null))
  }, [open])

  return (
    <section className="panel weekly">
      <div className="problem-head">
        <h3>이번 주</h3>
        <button type="button" className="linklike" onClick={() => setOpen((v) => !v)}>
          {open ? '접기' : '펼치기'}
        </button>
      </div>

      {open && report && (
        <>
          <p className="muted small">
            {report.from} ~ {report.to} · 시도 {report.activity.attempts}회 · 맞힘{' '}
            {report.activity.accepted}회 · 푼 문제 {report.activity.problemsSolved}개 · 활동{' '}
            {report.activity.activeDays}일
          </p>

          <h4 className="muted small">약점</h4>
          {report.weakest.length === 0 ? (
            <p className="small">지금 흔들리는 역량이 없습니다.</p>
          ) : (
            <p className="small">{report.weakest.map((c) => COMPETENCY_LABEL[c] ?? c).join(' · ')}</p>
          )}

          <h4 className="muted small">재발</h4>
          {report.recurrences.length === 0 ? (
            <p className="small">전에 맞힌 문제를 다시 틀린 일은 없습니다.</p>
          ) : (
            <ul className="plain">
              {report.recurrences.map((r) => (
                <li key={r.problemId} className="small">
                  <button type="button" className="linklike mono" onClick={() => onOpenProblem(r.problemId)}>
                    {r.problemId}
                  </button>{' '}
                  — {new Date(r.solvedAt).toLocaleDateString()}에 맞혔는데{' '}
                  {new Date(r.failedAt).toLocaleDateString()}에 다시 틀렸습니다
                </li>
              ))}
            </ul>
          )}

          <h4 className="muted small">성장</h4>
          {report.growth.length === 0 ? (
            // "아직 없다"를 감추지 않는다. 지난주보다 오른 것이 없다는 것도 사실이다.
            <p className="small">지난주보다 오른 역량이 아직 없습니다.</p>
          ) : (
            <ul className="plain">
              {report.growth.map((g) => (
                <li key={g.competency} className="small">
                  {COMPETENCY_LABEL[g.competency] ?? g.competency}: {LEVEL_LABEL[g.from]} → {LEVEL_LABEL[g.to]}
                </li>
              ))}
            </ul>
          )}

          <h4 className="muted small">다음 행동</h4>
          <ul className="plain">
            {report.actions.map((a) => (
              <li key={a.problemId} className="small">
                <span className="reason">{REASON_LABEL[a.reason]}</span>{' '}
                <button type="button" className="linklike mono" onClick={() => onOpenProblem(a.problemId)}>
                  {a.problemId}
                </button>
              </li>
            ))}
          </ul>
          <p className="muted small">다음 측정: {new Date(report.nextMeasurement).toLocaleDateString()}</p>

          {stats && (
            <>
              <h4 className="muted small">통계</h4>
              <p className="small">
                시도한 문제 {stats.problemsAttempted}개 중 {stats.problemsSolved}개 해결 · 제출{' '}
                {stats.submissions}회 중 {stats.accepted}회 정답
              </p>
              {Object.keys(stats.verdicts).length > 0 && (
                <p className="muted small">
                  최근 30일:{' '}
                  {Object.entries(stats.verdicts)
                    .map(([v, n]) => `${VERDICT_LABEL[v as Verdict] ?? v} ${n}`)
                    .join(' · ')}
                </p>
              )}
              {Object.keys(stats.byTag).length > 0 && (
                <p className="muted small">
                  주제별:{' '}
                  {Object.entries(stats.byTag)
                    .map(([tag, t]) => `${tag} ${t.solved}/${t.attempted}`)
                    .join(' · ')}
                </p>
              )}
            </>
          )}
        </>
      )}
    </section>
  )
}
