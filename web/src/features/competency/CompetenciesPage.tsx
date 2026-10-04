import { useQuery } from '@tanstack/react-query'
import { ChevronRight, CircleHelp } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'wouter'
import { getCompetencyMap } from '../../api/client'
import { Badge, Dialog, EmptyState, InlineAlert, Skeleton, Tabs } from '../../design'
import { COMPETENCY_LABEL, CONFIDENCE_LABEL, GROUP_LABEL, LEVEL_LABEL } from '../../shared/types'
import type { MasteryLevel, MasteryView } from '../../shared/types'
import { setParam } from '../../shared/url'
import { useMediaQuery } from '../../shared/useMediaQuery'
import { CONFIDENCE_STEPS, LEVEL_STEPS, isMeasured, needsMoreEvidence, summarize } from './competencyView'
import { EvidencePanel } from './EvidencePanel'
import { WeeklyReportView } from './WeeklyReportView'
import styles from './CompetenciesPage.module.css'

type Tab = 'map' | 'weekly'

/**
 * 역량 `/competencies` (UI 디자인 문서 §9.4, 디자인 설계서 §10.5, docs/ui-overhaul.md §6.5).
 *
 * > 표시는 역량별 Level + Confidence + Evidence 로 구성합니다. 단일 백분율을 피하고 (PRD §3.4)
 *
 * 그래서 종합 점수도, 레이더 차트도 없다. 쟀으면 Level 은 칸 막대, Confidence 는 점 — 모양이 달라야
 * 둘을 하나로 읽지 않는다. 재지 않은 역량은 한 줄로 접는다: 열아홉 줄 중 대부분이 "아직 재지 않음"이면
 * 지도가 빈칸의 목록이 된다. 그러나 **감추지는 않는다** (FR-801) — 몇 개가 비었는지는 늘 보인다.
 *
 * 역량을 고르면 근거가 오른쪽 drawer 에 열리고, 거기서 원본 제출로 간다 (UI §9.4 "원본 근거 열기").
 * 고른 역량과 탭은 주소에 적힌다.
 */
export function CompetenciesPage() {
  const params = new URLSearchParams(window.location.search)
  const [tab, setTab] = useState<Tab>(params.get('tab') === 'weekly' ? 'weekly' : 'map')
  const [selected, setSelected] = useState<string | null>(params.get('c'))
  const wide = useMediaQuery('(min-width: 1024px)')
  const query = useQuery({ queryKey: ['me', 'competencies'], queryFn: getCompetencyMap })

  const changeTab = (next: Tab) => {
    setTab(next)
    setParam('tab', next === 'map' ? null : next)
  }
  const select = (competency: string | null) => {
    setSelected(competency)
    setParam('c', competency)
  }

  return (
    <div className={styles.page}>
      <header className={styles.head}>
        <h1 className={styles.title}>역량</h1>
        <Link href="/training" className={styles.headLink}>
          오늘의 훈련
        </Link>
      </header>

      <Tabs
        label="역량 보기"
        value={tab}
        onChange={changeTab}
        items={[
          { key: 'map', label: '역량 지도' },
          { key: 'weekly', label: '이번 주' },
        ]}
      >
        <div className={styles.tabBody}>
          {tab === 'weekly' ? (
            <WeeklyReportView
              onSelectCompetency={(competency) => {
                changeTab('map')
                select(competency)
              }}
            />
          ) : query.isError ? (
            <InlineAlert tone="danger" title="역량 지도를 불러오지 못했습니다" />
          ) : !query.data ? (
            <div role="status" aria-busy="true" className={styles.loading}>
              <span className="visually-hidden">불러오는 중</span>
              <Skeleton height={96} />
              <Skeleton height={240} />
            </div>
          ) : (
            <MapView
              items={query.data.competencies}
              diagnosed={query.data.diagnosed}
              selected={selected}
              wide={wide}
              onSelect={select}
            />
          )}
        </div>
      </Tabs>
    </div>
  )
}

function MapView({
  items,
  diagnosed,
  selected,
  wide,
  onSelect,
}: {
  items: MasteryView[]
  diagnosed: boolean
  selected: string | null
  wide: boolean
  onSelect: (competency: string | null) => void
}) {
  if (!diagnosed) {
    // 0점이 아니라 "아직 재지 않았다"를 말한다. 둘은 다르다 (FR-801).
    return (
      <EmptyState
        title="아직 아무것도 재지 않았습니다"
        action={
          <Link href="/training" className="linklike">
            진단 시작 — 오늘의 훈련으로
          </Link>
        }
      >
        문제를 풀거나 풀기 전 질문에 답하면 역량마다 근거가 쌓입니다. 근거가 생긴 역량부터 여기에 그려집니다.
      </EmptyState>
    )
  }

  const summaries = summarize(items)
  const unmeasured = items.filter((item) => !isMeasured(item))
  const current = items.find((item) => item.competency === selected) ?? null

  const list = (
    <div className={styles.map}>
      <ul className={styles.summaries} aria-label="역량군 요약">
        {summaries.map((summary) => (
          <li key={summary.group}>
            <a href={`#group-${summary.group}`} className={styles.summary}>
              <span className={styles.summaryName}>{GROUP_LABEL[summary.group] ?? summary.group}</span>
              <span className={styles.summaryCount}>
                {summary.measured}/{summary.total} 측정
              </span>
              <span className={styles.summaryLevels}>
                {summary.measured === 0
                  ? '아직 근거 없음'
                  : (['STRONG', 'PROFICIENT', 'DEVELOPING'] as MasteryLevel[])
                      .filter((level) => summary.levels[level])
                      .map((level) => `${LEVEL_LABEL[level]} ${summary.levels[level]}`)
                      .join(' · ')}
              </span>
              {summary.needsMoreEvidence > 0 && (
                <span className={styles.summaryNote}>추가 확인 필요 {summary.needsMoreEvidence}</span>
              )}
            </a>
          </li>
        ))}
      </ul>

      {unmeasured.length > 0 && (
        <div className={styles.unmeasured}>
          <details>
            <summary>
              <CircleHelp size={16} aria-hidden="true" />
              {unmeasured.length}개 역량은 아직 근거가 없습니다
            </summary>
            <p>{unmeasured.map((item) => COMPETENCY_LABEL[item.competency] ?? item.competency).join(' · ')}</p>
          </details>
          <Link href="/training" className={styles.unmeasuredLink}>
            진단 시작
          </Link>
        </div>
      )}

      {summaries
        .filter((summary) => summary.measured > 0)
        .map((summary) => (
          <section key={summary.group} id={`group-${summary.group}`} aria-labelledby={`group-title-${summary.group}`} className={styles.group}>
            <h2 id={`group-title-${summary.group}`} className={styles.groupTitle}>
              {GROUP_LABEL[summary.group] ?? summary.group}
            </h2>
            <div className={styles.rowsHead} aria-hidden="true">
              <span>역량</span>
              <span>수준</span>
              <span>근거</span>
              <span>맞힘/근거</span>
            </div>
            <ul className={styles.rows}>
              {items
                .filter((item) => item.group === summary.group && isMeasured(item))
                .map((item) => (
                  <li key={item.competency}>
                    <CompetencyRow item={item} selected={item.competency === selected} onSelect={() => onSelect(item.competency)} />
                  </li>
                ))}
            </ul>
          </section>
        ))}
    </div>
  )

  if (!wide) {
    return (
      <>
        {list}
        <Dialog
          open={current !== null}
          onClose={() => onSelect(null)}
          title={current ? `${COMPETENCY_LABEL[current.competency] ?? current.competency} 근거` : '근거'}
        >
          {current && <EvidencePanel item={current} />}
        </Dialog>
      </>
    )
  }

  return (
    <div className={styles.split}>
      {list}
      <aside className={styles.drawer} aria-label="근거">
        {current ? (
          <EvidencePanel item={current} onClose={() => onSelect(null)} />
        ) : (
          <p className={styles.drawerEmpty}>
            역량을 고르면 그 수준이 어떤 근거에서 나왔는지 — 문제, 제출, 받은 도움 — 가 여기 나옵니다.
          </p>
        )}
      </aside>
    </div>
  )
}

/**
 * 한 줄. Level 은 세 칸 막대, Confidence 는 세 점 — 같은 줄에 나란히, 그러나 따로 (PRD §3.4).
 * 근거가 적으면 "추가 확인 필요"를 붙인다 — 기르는 중이어도 약점이라고 단정하지 않는다 (DS §10.5).
 */
function CompetencyRow({ item, selected, onSelect }: { item: MasteryView; selected: boolean; onSelect: () => void }) {
  const name = COMPETENCY_LABEL[item.competency] ?? item.competency
  const steps = LEVEL_STEPS[item.level as keyof typeof LEVEL_STEPS] ?? 0
  const dots = CONFIDENCE_STEPS[item.confidence]
  return (
    <button
      type="button"
      className={styles.row}
      aria-pressed={selected}
      aria-label={`${name} — ${LEVEL_LABEL[item.level]}, ${CONFIDENCE_LABEL[item.confidence]}, 근거 ${item.evidenceCount}개 중 ${item.successCount}개 맞힘${needsMoreEvidence(item) ? ', 추가 확인 필요' : ''}`}
      onClick={onSelect}
    >
      <span className={styles.rowName}>
        {name}
        {needsMoreEvidence(item) && <Badge tone="warning">추가 확인 필요</Badge>}
      </span>
      <span className={styles.level}>
        <span className={styles.levelBar} aria-hidden="true">
          {[1, 2, 3].map((step) => (
            <span key={step} className={step <= steps ? styles.levelOn : styles.levelOff} />
          ))}
        </span>
        {LEVEL_LABEL[item.level]}
      </span>
      <span className={styles.confidence}>
        <span className={styles.dots} aria-hidden="true">
          {[1, 2, 3].map((step) => (
            <span key={step} className={step <= dots ? styles.dotOn : styles.dotOff} />
          ))}
        </span>
        {CONFIDENCE_LABEL[item.confidence]}
      </span>
      <span className={styles.count}>
        {item.successCount}/{item.evidenceCount}
      </span>
      <ChevronRight size={16} aria-hidden="true" className={styles.chevron} />
    </button>
  )
}
