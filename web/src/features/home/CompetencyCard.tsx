import { useQuery } from '@tanstack/react-query'
import { ArrowRight } from 'lucide-react'
import { Link } from 'wouter'
import { getCompetencyMap } from '../../api/client'
import { Skeleton } from '../../design'
import { GROUP_LABEL, LEVEL_LABEL } from '../../shared/types'
import { summarize } from '../competency/competencyView'
import { groupTiles } from './homeView'
import styles from './Home.module.css'

/**
 * 역량 요약 — 역량군마다 한 칸, 잰 역량의 수준별 개수 (docs/ui-overhaul.md §6.10).
 * 평균을 내지 않는다 (competencyView.ts). 잰 것이 없는 역량군은 점선 칸에 "아직 근거 없음".
 */
export function CompetencyCard() {
  const query = useQuery({ queryKey: ['me', 'competencies'], queryFn: getCompetencyMap })
  const tiles = query.data ? groupTiles(summarize(query.data.competencies)) : null

  return (
    <section className={styles.card} aria-labelledby="competency-title">
      <div className={styles.cardHead}>
        <h2 id="competency-title" className={styles.cardTitle}>
          역량 요약
        </h2>
        <Link href="/competencies" className={styles.more}>
          근거 보기 <ArrowRight size={14} aria-hidden="true" />
        </Link>
      </div>

      {query.isError ? (
        <p className={styles.muted}>역량 지도를 불러오지 못했습니다.</p>
      ) : !tiles ? (
        <Skeleton height={120} />
      ) : !query.data!.diagnosed ? (
        <p className={styles.muted}>
          아직 잰 역량이 없습니다. <Link href="/training">진단 시작</Link>
        </p>
      ) : (
        <ul className={styles.groups}>
          {tiles.map((tile) => (
            <li key={tile.group} className={tile.unmeasured ? `${styles.group} ${styles.unmeasured}` : styles.group}>
              <span className={styles.groupName}>{GROUP_LABEL[tile.group] ?? tile.group}</span>
              {tile.unmeasured ? (
                <span className={styles.groupNote}>아직 근거 없음</span>
              ) : (
                <>
                  <span className={styles.levels}>{tile.levels.map(({ level, count }) => `${LEVEL_LABEL[level]} ${count}`).join(' · ')}</span>
                  <span className={styles.groupNote}>
                    역량 {tile.measured} / {tile.total} 잼{tile.needsMoreEvidence > 0 && ` · ${tile.needsMoreEvidence}개 추가 확인 필요`}
                  </span>
                </>
              )}
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
