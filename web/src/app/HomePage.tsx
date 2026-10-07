import { Redirect, useLocation } from 'wouter'
import type { Session } from '../api/session'
import { ContestSummaryPanel } from '../features/contest/ContestSummaryPanel'
import { CollectionsPanel } from '../features/learning/CollectionsPanel'
import { ProjectsPanel } from '../features/project/ProjectsPanel'
import { CompetencyCard } from '../features/home/CompetencyCard'
import { RecordCard } from '../features/home/RecordCard'
import { TodayCard } from '../features/home/TodayCard'
import styles from './HomePage.module.css'

/**
 * 홈 — 무엇을 풀지 고르는 곳 (디자인 설계서 §2.1).
 *
 * 풀이(에디터·판정·리플레이·기록)는 U1 에서 `/problems/:slug/solve` 로, 문제 목록은 U2 에서
 * `/problems` 로, 처방·역량·주간 리포트는 U5 에서 `/training`·`/competencies` 로, 대회는 U6 에서
 * `/contests` 로 나갔다. 여기에는 요약과 프로젝트·문제집만 남는다 (docs/ui-overhaul.md §9).
 *
 * 요약은 세 장의 카드다 (§6.10) — 내 기록 · 오늘의 훈련 · 역량 요약.
 */
export function HomePage({ session }: { session: Session }) {
  const [, navigate] = useLocation()
  // 프로젝트형 작업 공간은 전용 화면이 됐다 (`/projects/:id`, ui-overhaul.md §6.11). 예전 주소 `/?project=` 는 옮겨 준다
  const legacyProject = new URLSearchParams(window.location.search).get('project')
  if (legacyProject) return <Redirect to={`/projects/${encodeURIComponent(legacyProject)}`} replace />

  const openProblem = (slug: string) => navigate(`/problems/${slug}/solve`)

  return (
    <div className="app">
      <div className={styles.board}>
        <div className={styles.column}>
          <RecordCard session={session} />
        </div>

        <div className={styles.column}>
          {/* 맨 위다. "무엇을 풀지 모를 때 현재 수준과 약점을 기준으로 고른다"가
              PRD §2.3 의 첫 번째 JTBD 이고, 그 답은 목록이 아니라 처방이다 (FR-808). */}
          <TodayCard />
          <CompetencyCard />
          {/* 두 번째 판정기의 문제라 알고리즘 문제와 섞이지 않는다 (11단계). */}
          <ProjectsPanel onOpen={(id) => navigate(`/projects/${encodeURIComponent(id)}`)} />
        </div>

        <div className={styles.column}>
          {/* 대회 중이면 무엇을 풀지는 대회가 정한다 (§8.4). 본체는 /contests 다 */}
          <ContestSummaryPanel />
          <CollectionsPanel currentProblemId={null} onOpenProblem={openProblem} />
        </div>
      </div>
    </div>
  )
}
