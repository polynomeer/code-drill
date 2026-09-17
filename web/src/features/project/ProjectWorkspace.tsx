import { Suspense, lazy, useEffect, useRef, useState } from 'react'
import {
  discardProjectDraft,
  getProject,
  getProjectDraft,
  getProjectSubmission,
  listProjectSubmissions,
  saveProjectDraft,
  submitProject,
} from '../../api/client'
import { DIFFICULTY_LABEL, VERDICT_LABEL } from '../../shared/types'
import type { ProjectDraft, ProjectSubmission, ProjectView } from '../../shared/types'
import { SaveIndicator } from '../workspace/Workspace'
import { useDraftSync } from '../workspace/useDraftSync'

const MonacoWorkspace = lazy(() => import('../workspace/MonacoWorkspace'))

const EDITOR_LANGUAGE: Record<string, string> = {
  PYTHON: 'python',
  KOTLIN: 'kotlin',
  JAVA: 'java',
}
const SOURCE_EXTENSION: Record<string, string> = {
  PYTHON: 'py',
  KOTLIN: 'kt',
  JAVA: 'java',
}

/** 서버의 파일 한도(256KB)와 같다. 넘는 것은 어차피 제출이 거절된다. */
const MAX_IMPORT_BYTES = 256 * 1024

/**
 * 프로젝트형 문제의 작업 공간 (feature-roadmap 11단계 — 두 번째 판정기).
 *
 * 알고리즘 문제의 작업 공간이 아니라 **따로 설계한 화면**이다. 알고리즘 문제는 지문이 왼쪽
 * 열의 패널 하나이고 편집기가 오른쪽 열의 패널 하나라 두 열로 충분하지만, 프로젝트형은 파일
 * 여럿과 긴 요구사항을 같이 봐야 한다. 그래서 두 열 화면을 통째로 이 화면으로 바꾼다 —
 * 왼쪽에 요구사항과 기록, 오른쪽에 파일 탭·편집기·판정. 목록으로 돌아가면 원래 화면이다.
 *
 * 알고리즘 문제와 다른 것은 셋이다. 파일이 여럿이라 편집기 위에 파일 탭이 있고, 채점이
 * 분 단위라 결과를 기다리는 동안 상태를 보여 주며, 결과는 케이스가 아니라 **테스트**다 —
 * 공개 테스트는 이름과 사유로, 숨은 테스트는 "몇 개 중 몇 개"로만 온다. 숨은 테스트의
 * 이름은 무엇을 시험하는지의 힌트고, 사유는 기대값 그 자체라 서버가 애초에 보내지 않는다.
 *
 * 파일 트리는 편집기의 탭이다. 파일을 더하고 지울 수 있되 `tests/` 아래의 것은 채점 때
 * 숨은 스위트로 덮인다고 문제 본문이 말한다.
 *
 * 파일들은 초안으로 자동 저장된다 (§8.1) — 알고리즘 문제의 소스와 같은 규칙, 같은 뼈대
 * ([useDraftSync]). 다시 열면 시작 저장소가 아니라 초안이 열리고, "시작 저장소로 되돌리기"가
 * 초안을 버린다.
 */
export function ProjectWorkspace({
  id,
  onClose,
  onJudged,
}: {
  id: string
  onClose: () => void
  /** 판정이 끝났다 — 목록의 완료 표시와 기록을 새로 읽을 때다. */
  onJudged?: () => void
}) {
  const [open, setOpen] = useState<ProjectView | null>(null)
  const [files, setFiles] = useState<Record<string, string>>({})
  const [current, setCurrent] = useState<string | null>(null)
  const [newPath, setNewPath] = useState('')
  const [pending, setPending] = useState<ProjectSubmission | null>(null)
  const [history, setHistory] = useState<ProjectSubmission[]>([])
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [fromDraft, setFromDraft] = useState(false)
  const [imported, setImported] = useState<string | null>(null)
  const [touched, setTouched] = useState(false)
  const poll = useRef<number | null>(null)

  // 초안 자동 저장. 손대기 전에는 보내지 않는다 — 시작 저장소를 초안으로 적어 두면 아무것도 안 한 사람에게 초안이 생긴다.
  const draftSync = useDraftSync<Record<string, string>, ProjectDraft>({
    key: open?.id ?? null,
    value: files,
    enabled: open !== null && touched,
    load: () => (open ? getProjectDraft(open.id) : Promise.resolve(null)),
    save: (next, version) => saveProjectDraft(open!.id, next, version),
  })

  useEffect(() => {
    setError(null)
    setPending(null)
    setTouched(false)
    setOpen(null)
    // 초안이 있으면 그것을, 없으면 시작 저장소를 연다. 초안을 못 읽어도 시작 저장소는 열린다.
    Promise.all([getProject(id), getProjectDraft(id).catch(() => null)])
      .then(([view, draft]) => {
        setOpen(view)
        setFiles(draft?.files ?? view.files)
        setFromDraft(draft !== null)
        // 고칠 파일이 먼저 열려야 한다. 시작 저장소는 그 자리를 TODO 로 비워 두니 TODO 가 있는
        // 파일이 그것이고, 없으면(초안에서 이미 지웠으면) 테스트와 패키지 선언이 아닌 첫 파일이다.
        const opened = draft?.files ?? view.files
        const paths = Object.keys(opened).sort()
        const sources = paths.filter((path) => !path.startsWith('tests/') && !path.endsWith('__init__.py'))
        setCurrent(sources.find((path) => (opened[path] ?? '').includes('TODO')) ?? sources[0] ?? paths[0] ?? null)
      })
      .catch((e: Error) => setError(e.message))
    listProjectSubmissions(id)
      .then(setHistory)
      .catch(() => setHistory([]))
  }, [id])

  // 판정은 분 단위다. 끝날 때까지 1초마다 묻는다 — SSE 는 알고리즘 제출의 것이고, 여기서는
  // 기다리는 시간이 길어 폴링의 비용이 문제가 되지 않는다.
  useEffect(() => {
    if (!pending || pending.status === 'COMPLETED') return
    poll.current = window.setInterval(() => {
      getProjectSubmission(pending.id)
        .then((next) => {
          setPending(next)
          if (next.status === 'COMPLETED') {
            onJudged?.()
            listProjectSubmissions(id)
              .then(setHistory)
              .catch(() => undefined)
          }
        })
        .catch(() => undefined)
    }, 1000)
    return () => {
      if (poll.current !== null) window.clearInterval(poll.current)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pending, id])

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
    setTouched(true)
  }

  /**
   * 파일·폴더 가져오기 (§8.1). 브라우저가 고른 파일을 읽어 워크스페이스에 얹는다 — 서버는
   * 모른다. 폴더를 고르면 맨 위 폴더 이름을 떼고 그 아래 경로를 쓴다. 숨은 파일과 너무 큰
   * 파일은 건너뛰고 그 사실을 말한다; 조용히 빠지면 제출이 왜 다른지 아무도 모른다.
   */
  const importFiles = async (list: FileList | null) => {
    if (!list || !open) return
    const next = { ...files }
    let taken = 0
    const skipped: string[] = []
    for (const file of Array.from(list)) {
      const relative = file.webkitRelativePath || file.name
      const parts = relative.split('/')
      const path = (file.webkitRelativePath ? parts.slice(1) : parts).join('/')
      if (!path || parts.some((part) => part.startsWith('.'))) {
        skipped.push(relative)
        continue
      }
      if (file.size > MAX_IMPORT_BYTES) {
        skipped.push(`${relative} (너무 큼)`)
        continue
      }
      next[path] = await file.text()
      taken += 1
    }
    setFiles(next)
    setTouched(true)
    setCurrent((cur) => cur ?? Object.keys(next)[0] ?? null)
    setImported(`${taken}개 파일을 가져왔습니다.` + (skipped.length ? ` 건너뜀: ${skipped.join(', ')}` : ''))
  }

  const removeFile = (path: string) => {
    const next = { ...files }
    delete next[path]
    setFiles(next)
    setTouched(true)
    if (current === path) setCurrent(Object.keys(next)[0] ?? null)
  }

  const startOver = async () => {
    if (!open) return
    try {
      await discardProjectDraft(open.id)
    } catch {
      // 초안을 못 버려도 화면은 시작 저장소로 간다. 다음 저장이 충돌로 알려 줄 것이다.
    }
    setFiles(open.files)
    setFromDraft(false)
    setTouched(false)
    draftSync.reset()
  }

  if (!open) {
    return (
      <section className="panel project-workspace">
        <button type="button" className="linklike" onClick={onClose}>
          ← 목록
        </button>
        {error ? <p className="warn small">{error}</p> : <p className="muted">불러오는 중…</p>}
      </section>
    )
  }

  // 공개 테스트 모듈 이름 → 파일 경로. 언어마다 확장자가 다르다 — 모듈 이름은 서버가 언어와 무관하게 준다.
  const extension = SOURCE_EXTENSION[open.language] ?? 'py'
  const publicTestFiles = open.publicTests.map((module) => `${module.replace(/\./g, '/')}.${extension}`)

  return (
    <section className="project-workspace">
      <header className="project-header">
        <button type="button" className="linklike" onClick={onClose}>
          ← 목록
        </button>
        <h3>{open.title}</h3>
        <span className="muted small">
          {DIFFICULTY_LABEL[open.difficulty]} · {open.language} · 빌드 {open.limits.buildSeconds}초 · 테스트 {open.limits.testSeconds}초 ·{' '}
          {open.limits.memoryMb}MB · 파일 {Object.keys(files).length}/{open.limits.maxFiles}
        </span>
      </header>
      {error && <p className="warn small">{error}</p>}

      <div className="project-columns">
        <div className="stack">
          <section className="panel">
            <h4>요구사항</h4>
            <pre className="statement-body project-statement">{open.statement}</pre>
          </section>
          {history.length > 0 && (
            <section className="panel project-history">
              <h4>내 제출</h4>
              <ul>
                {history.map((item) => (
                  <li key={item.id}>
                    <button
                      type="button"
                      className="linklike"
                      onClick={() =>
                        getProjectSubmission(item.id)
                          .then(setPending)
                          .catch(() => undefined)
                      }
                    >
                      {new Date(item.createdAt).toLocaleString()}
                    </button>{' '}
                    <span className={item.verdict === 'ACCEPTED' ? 'ok' : 'muted'}>
                      {item.status !== 'COMPLETED' ? '채점 중' : item.verdict ? VERDICT_LABEL[item.verdict] : ''}
                      {item.score !== null && ` · ${item.score}점`}
                    </span>
                  </li>
                ))}
              </ul>
            </section>
          )}
        </div>

        <div className="stack">
          <section className="panel">
            {fromDraft && (
              <p className="muted small">
                저장해 둔 초안을 열었습니다.{' '}
                <button type="button" className="linklike" onClick={() => void startOver()}>
                  시작 저장소로 되돌리기
                </button>
              </p>
            )}
            <div className="project-files" role="tablist">
              {Object.keys(files)
                .sort()
                .map((path) => (
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
                <label className="import-label">
                  가져오기
                  <input type="file" multiple aria-label="파일 가져오기" onChange={(event) => void importFiles(event.target.files)} />
                </label>
                <label className="import-label">
                  폴더
                  <input
                    type="file"
                    aria-label="폴더 가져오기"
                    // 표준 속성이 아니라 ref 로 붙인다. 브라우저가 폴더 선택 대화상자를 연다.
                    ref={(node) => node?.setAttribute('webkitdirectory', '')}
                    onChange={(event) => void importFiles(event.target.files)}
                  />
                </label>
              </span>
            </div>
            {imported && <p className="muted small">{imported}</p>}
            <SaveIndicator
              state={draftSync.state}
              onResolve={(server, version) => {
                if (server) {
                  // 서버 것 가져오기: 편집기를 서버 초안으로 맞추고 저장은 하지 않는다.
                  setFiles(server.files)
                  draftSync.resolveConflict(version)
                } else {
                  draftSync.resolveConflict(version, files)
                }
              }}
            />
            <div className="editor project-editor">
              {current !== null && (
                <Suspense fallback={<p className="muted editor-loading">에디터를 불러오는 중…</p>}>
                  <MonacoWorkspace
                    key={current}
                    source={files[current] ?? ''}
                    language={EDITOR_LANGUAGE[open.language] ?? 'plaintext'}
                    onChange={(next) => {
                      setTouched(true)
                      setFiles((prev) => ({ ...prev, [current]: next }))
                    }}
                  />
                </Suspense>
              )}
            </div>
            <div className="editor-actions">
              <button
                className="primary"
                onClick={() => void submit()}
                disabled={submitting || (pending !== null && pending.status !== 'COMPLETED')}
              >
                {submitting ? '제출 중…' : '제출'}
              </button>
              <span className="muted small">공개 테스트({publicTestFiles.join(', ')})와 숨은 테스트를 함께 돌립니다</span>
            </div>
          </section>

          {pending && <ProjectResult submission={pending} />}
        </div>
      </div>
    </section>
  )
}

/** 결과. 공개 테스트는 하나씩, 숨은 테스트는 수로만 — 서버가 그렇게 보낸다. */
function ProjectResult({ submission }: { submission: ProjectSubmission }) {
  if (submission.status !== 'COMPLETED') {
    return (
      <section className="panel project-result">
        <p className="muted">{submission.status === 'LEASED' ? '빌드와 테스트를 돌리는 중…' : '채점을 기다리는 중…'}</p>
      </section>
    )
  }
  return (
    <section className="panel project-result">
      <p>
        <strong className={submission.verdict === 'ACCEPTED' ? 'ok' : 'warn'}>
          {submission.verdict ? VERDICT_LABEL[submission.verdict] : ''}
        </strong>{' '}
        <span className="muted">{submission.score}점</span>
        {submission.revision > 1 && <span className="muted small"> · 재채점 {submission.revision - 1}회</span>}
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
    </section>
  )
}
