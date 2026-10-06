import { useQuery } from '@tanstack/react-query'
import { ArrowRight, Lock, Star } from 'lucide-react'
import { Link } from 'wouter'
import { getCompetencyMap } from '../../api/client'
import { Skeleton } from '../../design'
import { GROUP_LABEL, LEVEL_LABEL } from '../../shared/types'
import { summarize } from '../competency/competencyView'
import { skillTiles } from './questView'
import styles from './Quest.module.css'

/**
 * 스킬 트리 — 역량군마다 한 칸 (docs/ui-overhaul.md §6.10). 별 하나가 잰 역량 하나이고, 별의 색이 수준이다.
 * 평균을 내지 않는다 (competencyView.ts). 잰 것이 없으면 잠긴 칸 — 점수 대신 "근거 부족".
 */
export function SkillTree() {
  const query = useQuery({ queryKey: ['me', 'competencies'], queryFn: getCompetencyMap })
  const tiles = query.data ? skillTiles(summarize(query.data.competencies)) : null

  return (
    <section className={styles.card} aria-labelledby="skill-title">
      <div className={styles.cardHead}>
        <h2 id="skill-title" className={styles.cardTitle}>
          스킬 트리
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
          아직 잰 역량이 없습니다. <Link href="/training">진단 퀘스트</Link>로 첫 근거를 만드세요.
        </p>
      ) : (
        <ul className={styles.skills}>
          {tiles.map((tile) => (
            <li key={tile.group} className={tile.locked ? `${styles.skill} ${styles.locked}` : styles.skill}>
              <span className={styles.skillName}>
                {tile.locked && <Lock size={14} aria-hidden="true" />}
                {GROUP_LABEL[tile.group] ?? tile.group}
              </span>
              {tile.locked ? (
                <span className={styles.skillNote}>근거 부족 — 잠김</span>
              ) : (
                <>
                  <span className={styles.stars}>
                    {tile.stars.map(({ level, count }) =>
                      Array.from({ length: count }, (_, index) => (
                        <Star key={`${level}-${index}`} size={14} className={styles[`star${level}`]} aria-hidden="true" />
                      )),
                    )}
                  </span>
                  <span className="visually-hidden">
                    {tile.stars.map(({ level, count }) => `${LEVEL_LABEL[level]} ${count}개`).join(', ')}
                  </span>
                  <span className={styles.skillNote}>
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
