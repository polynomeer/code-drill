import { useQuery } from '@tanstack/react-query'
import { ArrowRight, Swords } from 'lucide-react'
import { Link } from 'wouter'
import { getPrescription } from '../../api/client'
import { Badge, Skeleton } from '../../design'
import { COMPETENCY_LABEL, REASON_LABEL } from '../../shared/types'
import { markOpenSource } from '../../shared/analytics'
import { problemLabel, useProblemIndex } from '../problems/useProblemIndex'
import { splitQuests } from './questView'
import styles from './Quest.module.css'

/**
 * 오늘의 퀘스트 — 처방을 퀘스트 모양으로 (docs/ui-overhaul.md §6.10).
 *
 * 교체·미루기는 여기 두지 않는다. 행동은 `/training` 한 곳에만 있다 — 두 곳에 두면 한쪽에서 바꾼 것이
 * 다른 쪽에 늦게 비친다 (TodaySummary 와 같은 이유).
 */
export function QuestBoard() {
  const query = useQuery({ queryKey: ['me', 'prescription'], queryFn: getPrescription })
  const index = useProblemIndex()
  const quests = query.data ? splitQuests(query.data.items) : null

  return (
    <section className={styles.card} aria-labelledby="quest-title">
      <div className={styles.cardHead}>
        <h2 id="quest-title" className={styles.cardTitle}>
          오늘의 퀘스트
        </h2>
        <Link href="/training" className={styles.more}>
          바꾸기·미루기 <ArrowRight size={14} aria-hidden="true" />
        </Link>
      </div>

      {query.isError ? (
        <p className={styles.muted}>퀘스트를 불러오지 못했습니다.</p>
      ) : !quests ? (
        <Skeleton height={120} />
      ) : !quests.main ? (
        <p className={styles.muted}>오늘 권할 퀘스트가 없습니다. 아무 문제나 풀어도 기록이 쌓입니다.</p>
      ) : (
        <>
          <div className={styles.mainQuest}>
            <p className={styles.questKind}>메인 퀘스트 · {REASON_LABEL[quests.main.reason]}</p>
            <h3 className={styles.questTitle}>{problemLabel(index, quests.main.problemId)}</h3>
            {quests.main.detail && <p className={styles.questDetail}>{quests.main.detail}</p>}
            {quests.main.competency && <Badge tone="brand">{COMPETENCY_LABEL[quests.main.competency] ?? quests.main.competency}</Badge>}
            <Link
              href={`/problems/${quests.main.problemId}/solve`}
              className={styles.challenge}
              onClick={() => markOpenSource('prescription')}
            >
              <Swords size={18} aria-hidden="true" />
              도전하기
            </Link>
          </div>

          {quests.side.length > 0 && (
            <ul className={styles.sideQuests} aria-label="사이드 퀘스트">
              {quests.side.map((quest) => (
                <li key={quest.problemId}>
                  <span className={styles.sideKind}>{REASON_LABEL[quest.reason]}</span>
                  <Link
                    href={`/problems/${quest.problemId}/solve`}
                    className={styles.sideLink}
                    onClick={() => markOpenSource('prescription')}
                  >
                    {problemLabel(index, quest.problemId)}
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </>
      )}
    </section>
  )
}
