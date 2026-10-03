import { useEffect, useState } from 'react'
import { useLocation } from 'wouter'
import { listProjects } from '../api/client'
import { CompetencyMapPanel } from '../features/competency/CompetencyMapPanel'
import { ContestsPanel } from '../features/contest/ContestsPanel'
import { CollectionsPanel } from '../features/learning/CollectionsPanel'
import { TodayPanel } from '../features/learning/TodayPanel'
import { WeeklyReportPanel } from '../features/learning/WeeklyReportPanel'
import { ProjectWorkspace } from '../features/project/ProjectWorkspace'
import { ProjectsPanel } from '../features/project/ProjectsPanel'

/**
 * 홈 — 무엇을 풀지 고르는 곳 (디자인 설계서 §2.1).
 *
 * 풀이(에디터·판정·리플레이·기록)는 U1 에서 `/problems/:slug/solve` 로, 문제 목록은 U2 에서
 * `/problems` 로 나갔다. 남은 패널들도 각자 라우트를 얻으면 여기서 빠진다 — 처방·역량은 U5,
 * 대회는 U6 (docs/ui-overhaul.md §9).
 */
export function HomePage() {
  const [, navigate] = useLocation()
  // 대회의 문제 목록에는 프로젝트형 문제도 온다 (11단계). 어느 화면으로 열지 여기서 가른다.
  const [projectIds, setProjectIds] = useState<Set<string>>(() => new Set())
  // 열린 프로젝트. 있으면 두 열 대신 프로젝트 작업 공간을 통째로 그린다 — 파일 여럿과
  // 긴 요구사항은 목록 아래 패널에 들어가지 않는다.
  const [openProject, setOpenProject] = useState<string | null>(null)
  const [projectsJudged, setProjectsJudged] = useState(0)

  useEffect(() => {
    listProjects()
      .then((projects) => setProjectIds(new Set(projects.map((project) => project.id))))
      .catch(() => undefined)
  }, [])

  const openProblem = (slug: string) => navigate(`/problems/${slug}/solve`)
  const openSubmission = (id: string, step: number | null = null) =>
    navigate(`/submissions/${id}${step === null ? '' : `?step=${step}`}`)

  return (
    <div className="app">
      {openProject && (
        <div>
          <ProjectWorkspace
            id={openProject}
            onClose={() => setOpenProject(null)}
            onJudged={() => setProjectsJudged((n) => n + 1)}
          />
        </div>
      )}
      {/* 프로젝트가 열려 있으면 두 열은 그리지 않는다 — 같은 화면에 작업 공간이 둘이면 어느 것이 내 일인지 헷갈린다. */}
      <div className="columns" hidden={openProject !== null}>
        <div className="stack">
          {/* 목록보다 위다. "무엇을 풀지 모를 때 현재 수준과 약점을 기준으로 고른다"가
              PRD §2.3 의 첫 번째 JTBD 이고, 그 답은 목록이 아니라 처방이다 (FR-808). */}
          <TodayPanel onOpenProblem={openProblem} refreshKey={0} />
          {/* 목록 아래. 두 번째 판정기의 문제라 알고리즘 문제와 섞이지 않는다 (11단계). */}
          <ProjectsPanel refreshKey={projectsJudged} onOpen={setOpenProject} />
        </div>

        <div className="stack">
          {/* 대회 중이면 무엇을 풀지는 대회가 정한다 (§8.4). */}
          <ContestsPanel
            currentProblem={null}
            onOpenProblem={(id) => (projectIds.has(id) ? setOpenProject(id) : openProblem(id))}
            refreshKey={0}
          />
          {/* "무엇을 풀었나" 다음에 "그래서 무엇이 늘었나"가 온다. */}
          <CompetencyMapPanel onOpenSubmission={openSubmission} />
          <WeeklyReportPanel onOpenProblem={openProblem} />
          <CollectionsPanel currentProblemId={null} onOpenProblem={openProblem} />
        </div>
      </div>
    </div>
  )
}
