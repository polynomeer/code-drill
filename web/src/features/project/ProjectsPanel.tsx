import { Suspense, lazy, useCallback, useEffect, useRef, useState } from 'react'
import {
  getProject,
  getProjectSubmission,
  listProjectSubmissions,
  listProjects,
  submitProject,
} from '../../api/client'
import { DIFFICULTY_LABEL, VERDICT_LABEL } from '../../shared/types'
import type { ProjectSubmission, ProjectSummary, ProjectView } from '../../shared/types'

const MonacoWorkspace = lazy(() => import('../workspace/MonacoWorkspace'))

const EDITOR_LANGUAGE: Record<string, string> = { PYTHON: 'python', KOTLIN: 'kotlin', JAVA: 'java' }

/**
 * 프로젝트형 문제 (feature-roadmap 11단계 — 두 번째 판정기).
 *
 * 알고리즘 문제와 다른 것은 셋이다. 파일이 여럿이라 편집기 위에 파일 탭이 있고, 채점이
 * 분 단위라 결과를 기다리는 동안 상태를 보여 주며, 결과는 케이스가 아니라 **테스트**다 —
 * 공개 테스트는 이름과 사유로, 숨은 테스트는 "몇 개 중 몇 개"로만 온다. 숨은 테스트의
 * 이름은 무엇을 시험하는지의 힌트고, 사유는 기대값 그 자체라 서버가 애초에 보내지 않는다.
 *
 * 파일 트리는 편집기의 탭이다. 파일을 더하고 지울 수 있되 `tests/` 아래의 것은 채점 때
 * 숨은 스위트로 덮인다고 문제 본문이 말한다.
 */
export function ProjectsPanel({ refreshKey }: { refreshKey: number }) {
  const [projects, setProjects] = useState<ProjectSummary[] | null>(null)
  const [open, setOpen] = useState<ProjectView | null>(null)
  const [files, setFiles] = useState<Record<string, string>>({})
  const [current, setCurrent] = useState<string | null>(null)
  const [newPath, setNewPath] = useState('')
  const [pending, setPending] = useState<ProjectSubmission | null>(null)
  const [history, setHistory] = useState<ProjectSubmission[]>([])
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const poll = useRef<number | null>(null)

  const refresh = useCallback(() => {
    listProjects()
      .then(setProjects)
      .catch(() => setProjects([]))
  }, [])

  useEffect(refresh, [refresh, refreshKey])

  const show = useCallback((id: string) => {
    setError(null)
    setPending(null)
    getProject(id)
      .then((view) => {
        setOpen(view)
        setFiles(view.files)
        // 고칠 파일이 먼저 열려야 한다. 패키지 선언과 테스트는 대개 그 파일이 아니다.
        const paths = Object.keys(view.files).sort()
        setCurrent(paths.find((path) => !path.startsWith('tests/') && !path.endsWith('__init__.py')) ?? paths[0] ?? null)
      })
      .catch((e: Error) => setError(e.message))
    listProjectSubmissions(id)
      .then(setHistory)
      .catch(() => setHistory([]))
  }, [])

  // 판정은 분 단위다. 끝날 때까지 1초마다 묻는다 — SSE 는 알고리즘 제출의 것이고, 여기서는
  // 기다리는 시간이 길어 폴링의 비용이 문제가 되지 않는다.
  useEffect(() => {
    if (!pending || pending.status === 'COMPLETED') return
    poll.current = window.setInterval(() => {
      getProjectSubmission(pending.id)
        .then((next) => {
          setPending(next)
          if (next.status === 'COMPLETED') {
            refresh()
            if (open) listProjectSubmissions(open.id).then(setHistory).catch(() => undefined)
          }
        })
        .catch(() => undefined)
    }, 1000)
    return () => {
      if (poll.current !== null) window.clearInterval(poll.current)
    }
  }, [pending, open, refresh])

  const submit = async () => {
    if (!open) return
    setSubmitting(true)
    setError(null)
    try {
      setPending(await submitProject(open.id, files))
    } catch (e) {
      setError(e instanceof Error ? e.message : '제출에 실패했습니다')
    } finally {
      setSubmitting(false)
    }
  }

  const addFile = () => {
    const path = newPath.trim()
    if (!path || files[path] !== undefined) return
    setFiles({ ...files, [path]: '' })
    setCurrent(path)
    setNewPath('')
  }

  const removeFile = (path: string) => {
    const next = { ...files }
    delete next[path]
    setFiles(next)
    if (current === path) setCurrent(Object.keys(next)[0] ?? null)
  }

  return (
    <section className="panel projects">
      <h3>프로젝트형 문제</h3>
      {error && <p className="warn small">{error}</p>}
      {open ? (
        <div>
          <button type="button" className="linklike" onClick={() => setOpen(null)}>
            ← 목록
          </button>
          <h4>{open.title}</h4>
          <p className="muted small">
            {DIFFICULTY_LABEL[open.difficulty]} · {open.language} · 빌드 {open.limits.buildSeconds}초 · 테스트{' '}
            {open.limits.testSeconds}초 · {open.limits.memoryMb}MB · 파일 {Object.keys(files).length}/{open.limits.maxFiles}
          </p>
          <pre className="statement-body project-statement">{open.statement}</pre>

          <div className="project-files" role="tablist">
            {Object.keys(files).sort().map((path) => (
              <span key={path} className={path === current ? 'file-tab active' : 'file-tab'}>
                <button type="button" role="tab" aria-selected={path === current} onClick={() => setCurrent(path)}>
                  {path}
                </button>
                {!(path in open.files) && (
                  <button type="button" className="linklike" aria-label={`${path} 삭제`} onClick={() => removeFile(path)}>
                    ×
                  </button>
                )}
              </span>
            ))}
            <span className="file-add">
              <input
                value={newPath}
                onChange={(event) => setNewPath(event.target.value)}
                placeholder="새 파일 경로"
                aria-label="새 파일 경로"
                onKeyDown={(event) => {
                  if (event.key === 'Enter') addFile()
                }}
              />
              <button type="button" onClick={addFile}>
                추가
              </button>
            </span>
          </div>
          <div className="editor project-editor">
            {current !== null && (
              <Suspense fallback={<p className="muted editor-loading">에디터를 불러오는 중…</p>}>
                <MonacoWorkspace
                  key={current}
                  source={files[current] ?? ''}
                  language={EDITOR_LANGUAGE[open.language] ?? 'plaintext'}
                  onChange={(next) => setFiles((prev) => ({ ...prev, [current]: next }))}
                />
              </Suspense>
            )}
          </div>
          <div className="editor-actions">
            <button className="primary" onClick={() => void submit()} disabled={submitting || (pending !== null && pending.status !== 'COMPLETED')}>
              {submitting ? '제출 중…' : '제출'}
            </button>
            <span className="muted small">
              공개 테스트({open.publicTests.map((module) => `${module.replace(/\./g, '/')}.py`).join(', ')})와 숨은 테스트를 함께 돌립니다
            </span>
          </div>

          {pending && <ProjectResult submission={pending} />}

          {history.length > 0 && (
            <div className="project-history">
              <h4>내 제출</h4>
              <ul>
                {history.map((item) => (
                  <li key={item.id}>
                    <button type="button" className="linklike" onClick={() => getProjectSubmission(item.id).then(setPending).catch(() => undefined)}>
                      {new Date(item.createdAt).toLocaleString()}
                    </button>{' '}
                    <span className={item.verdict === 'ACCEPTED' ? 'ok' : 'muted'}>
                      {item.status !== 'COMPLETED' ? '채점 중' : item.verdict ? VERDICT_LABEL[item.verdict] : ''}
                      {item.score !== null && ` · ${item.score}점`}
                    </span>
                  </li>
                ))}
              </ul>
            </div>
          )}
        </div>
      ) : projects === null ? (
        <p className="muted">불러오는 중…</p>
      ) : projects.length === 0 ? (
        <p className="muted">공개된 프로젝트형 문제가 없습니다.</p>
      ) : (
        <ul className="project-list">
          {projects.map((project) => (
            <li key={project.id}>
              <button type="button" className="linklike" onClick={() => show(project.id)}>
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

/** 결과. 공개 테스트는 하나씩, 숨은 테스트는 수로만 — 서버가 그렇게 보낸다. */
function ProjectResult({ submission }: { submission: ProjectSubmission }) {
  if (submission.status !== 'COMPLETED') {
    return (
      <div className="project-result">
        <p className="muted">{submission.status === 'LEASED' ? '빌드와 테스트를 돌리는 중…' : '채점을 기다리는 중…'}</p>
      </div>
    )
  }
  return (
    <div className="project-result">
      <p>
        <strong className={submission.verdict === 'ACCEPTED' ? 'ok' : 'warn'}>
          {submission.verdict ? VERDICT_LABEL[submission.verdict] : ''}
        </strong>{' '}
        <span className="muted">{submission.score}점</span>
      </p>
      {submission.log && <pre className="compile-log">{submission.log}</pre>}
      {submission.tests.length > 0 && (
        <ul className="project-tests">
          {submission.tests.map((test) => (
            <li key={`${test.module}.${test.name}`}>
              <span>{test.passed ? '✓' : '✗'}</span> <span>{test.name}</span>
              {test.message && <span className="muted small"> — {test.message}</span>}
            </li>
          ))}
        </ul>
      )}
      {submission.hiddenTotal !== null && submission.hiddenTotal > 0 && (
        <p className="muted small">
          숨은 테스트 {submission.hiddenPassed}/{submission.hiddenTotal} 통과 — 이름과 사유는 보이지 않습니다
        </p>
      )}
    </div>
  )
}
