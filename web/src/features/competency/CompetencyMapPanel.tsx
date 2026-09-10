import { useEffect, useState } from 'react'
import { getCompetencyMap, getEvidence } from '../../api/client'
import {
  COMPETENCY_LABEL,
  CONFIDENCE_LABEL,
  GROUP_LABEL,
  LEVEL_LABEL,
  SOURCE_LABEL,
} from '../../shared/types'
import type { CompetencyMap, EvidenceView, MasteryView } from '../../shared/types'

/**
 * 역량 지도 (PRD §3.4, FR-801·FR-806).
 *
 * > 표시는 역량별 **Level + Confidence + Evidence** 로 구성합니다. 단일 백분율을 피하고,
 * > 표본 부족·힌트 사용·AI 도움을 신뢰도와 증거 가중치에 반영합니다.
 *
 * 그래서 레이더 차트도, 종합 점수도 그리지 않는다. 하나의 숫자로 합치는 순간 "세 번 풀어
 * 두 번 맞힘"과 "백 번 풀어 일흔두 번 맞힘"이 같아 보이는데, 그 둘에게 필요한 다음 행동은
 * 정반대다.
 *
 * **아직 재지 않은 역량을 감추지 않는다** (FR-801). 감추면 사용자는 자기가 무엇을 아직
 * 보이지 않았는지 모르고, 지도는 실제보다 촘촘해 보인다.
 */
export function CompetencyMapPanel({ onOpenSubmission }: { onOpenSubmission: (id: string) => void }) {
  const [map, setMap] = useState<CompetencyMap | null>(null)
  const [open, setOpen] = useState<string | null>(null)
  const [evidence, setEvidence] = useState<EvidenceView[]>([])

  useEffect(() => {
    getCompetencyMap().then(setMap).catch(() => setMap(null))
  }, [])

  const toggle = async (competency: string) => {
    if (open === competency) {
      setOpen(null)
      return
    }
    setOpen(competency)
    setEvidence(await getEvidence(competency).catch(() => []))
  }

  if (!map) return null

  const measured = map.competencies.filter((item) => item.evidenceCount > 0)
  const groups = [...new Set(map.competencies.map((item) => item.group))]

  return (
    <section className="panel competency">
      <div className="problem-head">
        <h3>역량</h3>
        <span className="muted small">
          {measured.length}/{map.competencies.length} 측정됨
        </span>
      </div>

      {!map.diagnosed && (
        // 0점이 아니라 "아직 재지 않았다"를 말한다. 둘은 다르다.
        <p className="muted small">
          아직 아무 것도 재지 않았습니다. 문제를 풀거나 풀기 전 질문에 답하면 쌓입니다.
        </p>
      )}

      {groups.map((group) => {
        const rows = map.competencies.filter((item) => item.group === group)
        return (
          <div key={group} className="competency-group">
            <h4 className="muted small">{GROUP_LABEL[group] ?? group}</h4>
            <ul className="competency-list">
              {rows.map((item) => (
                <Row
                  key={item.competency}
                  item={item}
                  open={open === item.competency}
                  evidence={open === item.competency ? evidence : []}
                  onToggle={() => void toggle(item.competency)}
                  onOpenSubmission={onOpenSubmission}
                />
              ))}
            </ul>
          </div>
        )
      })}
    </section>
  )
}

function Row({
  item,
  open,
  evidence,
  onToggle,
  onOpenSubmission,
}: {
  item: MasteryView
  open: boolean
  evidence: EvidenceView[]
  onToggle: () => void
  onOpenSubmission: (id: string) => void
}) {
  const measured = item.evidenceCount > 0
  return (
    <li className={measured ? '' : 'unmeasured'}>
      <button type="button" onClick={onToggle} disabled={!measured} aria-expanded={open}>
        <span className="competency-name">{COMPETENCY_LABEL[item.competency] ?? item.competency}</span>
        {/* 등급과 신뢰도를 나란히, 그러나 따로 보여준다. 합치지 않는 것이 요구사항이다. */}
        <span className={`level ${item.level.toLowerCase()}`}>{LEVEL_LABEL[item.level]}</span>
        <span className="muted small">{CONFIDENCE_LABEL[item.confidence]}</span>
        <span className="muted small mono">
          {measured ? `${item.successCount}/${item.evidenceCount}` : '—'}
        </span>
      </button>

      {open && (
        <ul className="evidence-list">
          {evidence.map((row, index) => (
            <li key={index}>
              <span className="test-mark">{row.success ? '✓' : '✗'}</span>
              <span className="mono small">{row.problemId}</span>
              <span className="muted small">{SOURCE_LABEL[row.source]}</span>
              <span className="muted small">{row.detail}</span>
              {/* 도움 수준 (FR-806 — 근거 상세에서 확인할 수 있어야 한다).
                  지금은 도움 기능이 없어 늘 "도움 없음"이다. 그래도 적어 두는 이유는,
                  4단계에서 힌트가 붙었을 때 **그 전 증거가 무엇이었는지** 화면에서
                  바로 갈리게 하기 위해서다. */}
              <span className="muted small">
                {row.weight >= 1 ? '도움 없음' : `도움 받음 (${row.weight})`}
              </span>
              <span className="muted small">{new Date(row.occurredAt).toLocaleDateString()}</span>
              {row.source === 'SUBMISSION' && row.reference && (
                <button
                  type="button"
                  className="linklike"
                  onClick={() => onOpenSubmission(row.reference as string)}
                >
                  열기
                </button>
              )}
            </li>
          ))}
          {evidence.length === 0 && <li className="muted small">근거를 불러오는 중…</li>}
        </ul>
      )}
    </li>
  )
}
