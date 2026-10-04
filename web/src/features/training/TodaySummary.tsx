import { useQuery } from '@tanstack/react-query'
import { ArrowRight, Flame } from 'lucide-react'
import { Link } from 'wouter'
import { getPrescription } from '../../api/client'
import { Badge, Panel, Skeleton } from '../../design'
import { REASON_LABEL } from '../../shared/types'
import { problemLabel, useProblemIndex } from '../problems/useProblemIndex'
import styles from './TodaySummary.module.css'

/**
 * 오늘의 처방 요약 — 홈과 문제 목록 보조 열의 자리 (UI 디자인 문서 §3.2 "학습 맥락은 보조 열에만").
 *
 * 처방의 본체는 `/training` 이다. 여기는 무엇이 기다리는지와 연속 일수만 보이고 그리로 보낸다.
 * 교체·미루기를 두 곳에 두면 한쪽에서 바꾼 것이 다른 쪽에 늦게 비친다 — 행동은 한 화면에만 있다.
 */
export function TodaySummary() {
  const query = useQuery({ queryKey: ['me', 'prescription'], queryFn: getPrescription })
  const index = useProblemIndex()
  const prescription = query.data

  return (
    <Panel
      title="오늘의 훈련"
      actions={
        <Link href="/training" className={styles.more}>
          전체 <ArrowRight size={14} aria-hidden="true" />
        </Link>
      }
    >
      {query.isError ? (
        <p className={styles.muted}>처방을 불러오지 못했습니다.</p>
      ) : !prescription ? (
        <Skeleton height={60} />
      ) : (
        <>
          <p className={styles.streak}>
            <Flame size={14} aria-hidden="true" />
            {prescription.streak.days > 0 ? `${prescription.streak.days}일째` : '아직 시작 전'}
            {prescription.streak.atRisk && !prescription.streak.activeToday && ' · 오늘 안 하면 끊깁니다'}
          </p>
          {prescription.items.length === 0 ? (
            <p className={styles.muted}>오늘 권할 것이 없습니다. 아무 문제나 풀어도 기록이 쌓입니다.</p>
          ) : (
            <ul className={styles.items}>
              {prescription.items.slice(0, 3).map((item) => (
                <li key={item.problemId}>
                  <Badge>{REASON_LABEL[item.reason]}</Badge>
                  <Link href={`/problems/${item.problemId}/solve`} className={styles.link}>
                    {problemLabel(index, item.problemId)}
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </>
      )}
    </Panel>
  )
}
