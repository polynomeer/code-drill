import { useCallback, useEffect, useState } from 'react'
import { listProjects } from '../../api/client'
import { DIFFICULTY_LABEL } from '../../shared/types'
import type { ProjectSummary } from '../../shared/types'

/**
 * 프로젝트형 문제의 목록 (feature-roadmap 11단계 — 두 번째 판정기).
 *
 * 알고리즘 문제 목록과 섞이지 않는다 — 두 번째 판정기의 문제라 필터도 태그도 다르다. 고르면
 * 화면이 [ProjectWorkspace] 로 바뀐다; 파일 여럿과 긴 요구사항은 목록 아래 패널에 들어가지
 * 않는다.
 */
export function ProjectsPanel({ refreshKey, onOpen }: { refreshKey: number; onOpen: (id: string) => void }) {
  const [projects, setProjects] = useState<ProjectSummary[] | null>(null)

  const refresh = useCallback(() => {
    listProjects()
      .then(setProjects)
      .catch(() => setProjects([]))
  }, [])

  useEffect(refresh, [refresh, refreshKey])

  return (
    <section className="panel projects">
      <h3>프로젝트형 문제</h3>
      {projects === null ? (
        <p className="muted">불러오는 중…</p>
      ) : projects.length === 0 ? (
        <p className="muted">공개된 프로젝트형 문제가 없습니다.</p>
      ) : (
        <ul className="project-list">
          {projects.map((project) => (
            <li key={project.id}>
              <button type="button" className="linklike" onClick={() => onOpen(project.id)}>
                {project.title}
              </button>{' '}
              <span className="muted small">
                {DIFFICULTY_LABEL[project.difficulty]} · {project.language} · {project.tags.join(', ')}
                {project.solved && ' · 완료'}
              </span>
              <p className="muted small">{project.summary}</p>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
