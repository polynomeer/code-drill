import { useState } from 'react'
import { useLocation } from 'wouter'
import { ContestSummaryPanel } from '../features/contest/ContestSummaryPanel'
import { CollectionsPanel } from '../features/learning/CollectionsPanel'
import { ProjectWorkspace } from '../features/project/ProjectWorkspace'
import { ProjectsPanel } from '../features/project/ProjectsPanel'
import { TodaySummary } from '../features/training/TodaySummary'
import { setParam } from '../shared/url'

/**
 * 홈 — 무엇을 풀지 고르는 곳 (디자인 설계서 §2.1).
 *
 * 풀이(에디터·판정·리플레이·기록)는 U1 에서 `/problems/:slug/solve` 로, 문제 목록은 U2 에서
 * `/problems` 로, 처방·역량·주간 리포트는 U5 에서 `/training`·`/competencies` 로, 대회는 U6 에서
 * `/contests` 로 나갔다. 여기에는 요약과 프로젝트·문제집만 남는다 (docs/ui-overhaul.md §9).
 */
export function HomePage() {
  const [, navigate] = useLocation()
  // 열린 프로젝트. 있으면 두 열 대신 프로젝트 작업 공간을 통째로 그린다 — 파일 여럿과
  // 긴 요구사항은 목록 아래 패널에 들어가지 않는다. 대회 화면은 프로젝트형 문제를 `?project=` 로 연다.
  const [openProject, setOpenProject] = useState<string | null>(() => new URLSearchParams(window.location.search).get('project'))
  const [projectsJudged, setProjectsJudged] = useState(0)

  const openProblem = (slug: string) => navigate(`/problems/${slug}/solve`)
  const open = (id: string | null) => {
    setOpenProject(id)
    setParam('project', id)
  }

  return (
    <div className="app">
      {openProject && (
        <div>
          <ProjectWorkspace id={openProject} onClose={() => open(null)} onJudged={() => setProjectsJudged((n) => n + 1)} />
        </div>
      )}
      {/* 프로젝트가 열려 있으면 두 열은 그리지 않는다 — 같은 화면에 작업 공간이 둘이면 어느 것이 내 일인지 헷갈린다. */}
      <div className="columns" hidden={openProject !== null}>
        <div className="stack">
          {/* 목록보다 위다. "무엇을 풀지 모를 때 현재 수준과 약점을 기준으로 고른다"가
              PRD §2.3 의 첫 번째 JTBD 이고, 그 답은 목록이 아니라 처방이다 (FR-808). */}
          <TodaySummary />
          {/* 목록 아래. 두 번째 판정기의 문제라 알고리즘 문제와 섞이지 않는다 (11단계). */}
          <ProjectsPanel refreshKey={projectsJudged} onOpen={open} />
        </div>

        <div className="stack">
          {/* 대회 중이면 무엇을 풀지는 대회가 정한다 (§8.4). 본체는 /contests 다 */}
          <ContestSummaryPanel />
          <CollectionsPanel currentProblemId={null} onOpenProblem={openProblem} />
        </div>
      </div>
    </div>
  )
}
