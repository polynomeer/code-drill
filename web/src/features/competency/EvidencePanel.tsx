import { useQuery } from '@tanstack/react-query'
import { CircleCheck, CircleX, Clapperboard, FileCode, X } from 'lucide-react'
import { Link } from 'wouter'
import { getEvidence } from '../../api/client'
import { Badge, IconButton, InlineAlert, Skeleton } from '../../design'
import { COMPETENCY_LABEL, CONFIDENCE_LABEL, LEVEL_LABEL, SOURCE_LABEL } from '../../shared/types'
import type { EvidenceView, MasteryView } from '../../shared/types'
import { fullTime, relativeTime } from '../../shared/time'
import { problemLabel, useProblemIndex } from '../problems/useProblemIndex'
import { helpLabel, needsMoreEvidence } from './competencyView'
import styles from './CompetenciesPage.module.css'

/**
 * Evidence drawer (UI 디자인 문서 §9.4 "문제, 응답, 테스트, 리플레이, 도움 수준 — 원본 근거 열기").
 *
 * > 모든 변화는 Evidence drawer에서 설명 가능해야 합니다.
 *
 * 수준이 왜 이런지에 답하는 자리다. 근거마다 맞혔는지, 어디서 나왔는지(제출·풀기 전 질문·전이…),
 * 어느 문제인지, 받은 도움이 무게를 얼마나 깎았는지를 보이고, 제출에서 나온 것은 그 제출과
 * 리플레이로 바로 간다.
 */
export function EvidencePanel({ item, onClose }: { item: MasteryView; onClose?: () => void }) {
  const query = useQuery({ queryKey: ['me', 'competencies', item.competency], queryFn: () => getEvidence(item.competency) })
  const index = useProblemIndex()
  const name = COMPETENCY_LABEL[item.competency] ?? item.competency
  // 최근 것이 위 — "요즘 어떤가"가 먼저 궁금하다
  const evidence = [...(query.data ?? [])].sort((a, b) => b.occurredAt.localeCompare(a.occurredAt))

  return (
    <div className={styles.evidence}>
      {onClose && (
        <header className={styles.evidenceHead}>
          <h2 className={styles.evidenceTitle}>{name} 근거</h2>
          <IconButton label="근거 닫기" size="dense" icon={<X size={16} />} onClick={onClose} />
        </header>
      )}

      <dl className={styles.evidenceSummary}>
        <div>
          <dt>수준</dt>
          <dd>{LEVEL_LABEL[item.level]}</dd>
        </div>
        <div>
          <dt>근거</dt>
          <dd>{CONFIDENCE_LABEL[item.confidence]}</dd>
        </div>
        <div>
          <dt>맞힘</dt>
          <dd>
            {item.successCount}/{item.evidenceCount}
          </dd>
        </div>
      </dl>
      {needsMoreEvidence(item) && (
        <InlineAlert tone="warning" title="추가 확인 필요">
          근거가 아직 적어 약점이라고 단정하지 않습니다. 이 역량이 걸린 문제를 몇 개 더 풀면 수준이 자리를 잡습니다.
        </InlineAlert>
      )}

      {query.isError ? (
        <InlineAlert tone="danger">근거를 불러오지 못했습니다.</InlineAlert>
      ) : !query.data ? (
        <div role="status" aria-busy="true" className={styles.loading}>
          <span className="visually-hidden">불러오는 중</span>
          <Skeleton height={64} />
          <Skeleton height={64} />
        </div>
      ) : (
        <ol className={styles.evidenceList} aria-label={`${name} 근거 ${evidence.length}개`}>
          {evidence.map((row, position) => (
            <li key={`${row.occurredAt}-${position}`}>
              <EvidenceItem row={row} label={problemLabel(index, row.problemId)} />
            </li>
          ))}
        </ol>
      )}
    </div>
  )
}

function EvidenceItem({ row, label }: { row: EvidenceView; label: string }) {
  const submission = row.source === 'SUBMISSION' && row.reference ? row.reference : null
  return (
    <article className={styles.evidenceItem}>
      <div className={styles.evidenceTop}>
        {/* 색만으로 말하지 않는다 — 아이콘과 글자 */}
        <span className={row.success ? styles.success : styles.failure}>
          {row.success ? <CircleCheck size={16} aria-hidden="true" /> : <CircleX size={16} aria-hidden="true" />}
          {row.success ? '맞힘' : '못 맞힘'}
        </span>
        <Badge>{SOURCE_LABEL[row.source] ?? row.source}</Badge>
        <time dateTime={row.occurredAt} title={fullTime(row.occurredAt)} className={styles.time}>
          {relativeTime(row.occurredAt)}
        </time>
      </div>
      <Link href={`/problems/${row.problemId}`} className={styles.evidenceProblem}>
        {label}
      </Link>
      {row.detail && <p className={styles.evidenceDetail}>{row.detail}</p>}
      <div className={styles.evidenceFoot}>
        <span className={row.weight >= 1 ? styles.muted : styles.helped}>{helpLabel(row.weight)}</span>
        {submission && (
          <span className={styles.evidenceLinks}>
            <Link href={`/submissions/${submission}`}>
              <FileCode size={14} aria-hidden="true" />
              제출 열기
            </Link>
            <Link href={`/submissions/${submission}/replay`}>
              <Clapperboard size={14} aria-hidden="true" />
              리플레이
            </Link>
          </span>
        )}
      </div>
    </article>
  )
}
