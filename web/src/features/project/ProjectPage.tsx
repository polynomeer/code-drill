import { ChevronLeft, FilePlus, FolderOpen, MoreHorizontal, Send, Terminal, Trash2, Upload } from 'lucide-react'
import { Suspense, lazy, useEffect, useState } from 'react'
import { Link } from 'wouter'
import { Badge, Button, Dialog, EmptyState, IconButton, Skeleton, Tabs, VerdictBadge } from '../../design'
import { DIFFICULTY_LABEL, LANGUAGE_LABEL } from '../../shared/types'
import type { ProjectProbeOutcome, ProjectSubmission, ProjectView } from '../../shared/types'
import { fullTime } from '../../shared/format'
import { useEditorSettings } from '../workspace/editorSettings'
import { SaveIndicator } from '../workspace/SaveIndicator'
import { Markdown } from '../workspace/StatementView'
import { LocalKitDialog } from './LocalKitDialog'
import { JUDGING_STEPS, fileTree, hiddenCells, judgingStep, statementBody } from './projectView'
import { useProjectWorkspace } from './useProjectWorkspace'
import styles from './ProjectPage.module.css'

const MonacoWorkspace = lazy(() => import('../workspace/MonacoWorkspace'))

const EDITOR_LANGUAGE: Record<string, string> = { PYTHON: 'python', KOTLIN: 'kotlin', JAVA: 'java' }

/**
 * 프로젝트형 작업 공간 `/projects/:id` (docs/ui-overhaul.md §6.11, 디자인 시안 18·19).
 *
 * 알고리즘 풀이 화면과 다른 화면이다 — 파일이 여럿이라 파일 트리가 있고, 채점이 분 단위라 기다리는 동안 단계를
 * 보이고, 결과는 케이스가 아니라 **테스트**다. 공개 테스트는 이름과 사유로, 숨은 테스트는 "몇 개 중 몇 개"로만
 * 온다 — 숨은 테스트의 이름은 무엇을 시험하는지의 힌트고, 사유는 기대값 그 자체라 서버가 보내지 않는다.
 *
 * 전에는 홈 안의 패널이었다(`/?project=`). 그 주소는 여기로 옮겨 준다.
 */
export function ProjectPage({ id }: { id: string }) {
  const workspace = useProjectWorkspace(id)
  const { project, error } = workspace

  if (!project) {
    return (
      <div className={styles.loading}>
        {error ? (
          <EmptyState title="프로젝트형 문제를 열지 못했습니다" action={<Link href="/problems?kind=project">프로젝트형 목록으로</Link>}>
            {error}
          </EmptyState>
        ) : (
          <div role="status" aria-busy="true">
            <span className="visually-hidden">불러오는 중</span>
            <Skeleton height={44} />
            <Skeleton height={480} />
          </div>
        )}
      </div>
    )
  }
  return <Workspace project={project} workspace={workspace} />
}

type WorkspaceState = ReturnType<typeof useProjectWorkspace>

function Workspace({ project, workspace }: { project: ProjectView; workspace: WorkspaceState }) {
  const settings = useEditorSettings()
  const [kitOpen, setKitOpen] = useState(false)
  const [resetOpen, setResetOpen] = useState(false)
  const [left, setLeft] = useState<'statement' | 'history'>('statement')
  const { files, current, result, judging, submitting } = workspace
  const fileCount = Object.keys(files).length

  // 제출 단축키 — 알고리즘 풀이 화면과 같다 (⌘⇧↵)
  useEffect(() => {
    const onKey = (event: KeyboardEvent) => {
      if ((event.metaKey || event.ctrlKey) && event.shiftKey && event.key === 'Enter') {
        event.preventDefault()
        void workspace.submit()
      }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [workspace])

  return (
    <div className={styles.page}>
      <LocalKitDialog project={project} open={kitOpen} onClose={() => setKitOpen(false)} />
      <Dialog
        open={resetOpen}
        onClose={() => setResetOpen(false)}
        title="시작 저장소로 되돌릴까요?"
        description="저장해 둔 초안을 버리고 시작 저장소의 파일로 돌아갑니다. 되돌릴 수 없습니다. 제출 기록은 남습니다."
        footer={
          <>
            <Button variant="tertiary" onClick={() => setResetOpen(false)}>
              취소
            </Button>
            <Button
              variant="danger"
              onClick={() => {
                void workspace.startOver()
                setResetOpen(false)
              }}
            >
              되돌리기
            </Button>
          </>
        }
      />

      <header className={styles.toolbar}>
        <Link href="/problems?kind=project" className={styles.back} aria-label="프로젝트형 목록으로">
          <ChevronLeft size={18} aria-hidden="true" />
        </Link>
        <h1 className={styles.title}>{project.title}</h1>
        <Badge>{LANGUAGE_LABEL[project.language as keyof typeof LANGUAGE_LABEL] ?? project.language}</Badge>
        <span className={styles.difficulty}>{DIFFICULTY_LABEL[project.difficulty]}</span>
        <span className={styles.limits}>
          빌드 {project.limits.buildSeconds}s · 테스트 {project.limits.testSeconds}s · {project.limits.memoryMb}MB · 파일 {fileCount}/
          {project.limits.maxFiles}
        </span>
        <span className={styles.spacer} />
        <SaveIndicator state={workspace.saveState} onResolve={workspace.resolveConflict} />
        <IconButton label="더 보기" icon={<MoreHorizontal size={18} />} popoverTarget="project-more" />
        <div id="project-more" popover="auto" className={styles.menu}>
          <button
            type="button"
            className={styles.menuItem}
            onClick={() => {
              document.getElementById('project-more')?.hidePopover?.()
              setResetOpen(true)
            }}
          >
            시작 저장소로 되돌리기
          </button>
        </div>
        <Button variant="secondary" icon={<Terminal size={16} />} onClick={() => setKitOpen(true)}>
          로컬에서 풀기
        </Button>
        <Button
          variant="primary"
          icon={<Send size={16} />}
          onClick={() => void workspace.submit()}
          disabled={judging}
          loading={submitting}
          aria-keyshortcuts="Meta+Shift+Enter Control+Shift+Enter"
        >
          {judging ? '채점 중…' : '제출'}
        </Button>
      </header>

      {workspace.error && (
        <p className={styles.error} role="alert">
          {workspace.error}
        </p>
      )}

      <div className={styles.split}>
        <section className={styles.pane} aria-label="요구사항과 기록">
          <Tabs
            label="왼쪽 창"
            items={[
              { key: 'statement', label: '요구사항' },
              { key: 'history', label: `내 제출 ${workspace.history.length}` },
            ]}
            value={left}
            onChange={setLeft}
          >
            {left === 'statement' ? (
              <div className={styles.statement}>
                <Markdown source={statementBody(project.statement, project.title)} />
              </div>
            ) : (
              <History history={workspace.history} selected={result?.id ?? null} onOpen={(item) => void workspace.openSubmission(item)} />
            )}
          </Tabs>
        </section>

        <div className={styles.right}>
          <section className={styles.work} aria-label="파일과 편집기">
            <FileTree project={project} workspace={workspace} />
            <div className={styles.editorColumn}>
              <div className={styles.editorBar}>
                <span className={styles.path}>{current ?? '파일을 고르세요'}</span>
                {workspace.fromDraft && <span className={styles.draftNote}>저장해 둔 초안</span>}
              </div>
              <div className={styles.editor}>
                {current !== null && (
                  <Suspense fallback={<p className={styles.editorLoading}>에디터를 불러오는 중…</p>}>
                    <MonacoWorkspace
                      key={current}
                      source={files[current] ?? ''}
                      language={EDITOR_LANGUAGE[project.language] ?? 'plaintext'}
                      settings={settings}
                      label={`${current} 편집기`}
                      onChange={(next) => workspace.edit(current, next)}
                    />
                  </Suspense>
                )}
              </div>
            </div>
          </section>

          <ResultPanel project={project} result={result} />
        </div>
      </div>
    </div>
  )
}

/* ─── 파일 트리 ─── */

function FileTree({ project, workspace }: { project: ProjectView; workspace: WorkspaceState }) {
  const [newPath, setNewPath] = useState('')
  const rows = fileTree(Object.keys(workspace.files))
  return (
    <nav className={styles.tree} aria-label="파일">
      <h2 className={styles.treeTitle}>파일</h2>
      <ul className={styles.treeList}>
        {rows.map((row) => {
          const added = !row.folder && !(row.path in project.files)
          return (
            <li key={row.path} style={{ paddingLeft: `calc(var(--space-3) + ${row.depth} * var(--space-3))` }} className={styles.treeRow}>
              {row.folder ? (
                <span className={styles.folder}>{row.name}/</span>
              ) : (
                <>
                  <button
                    type="button"
                    className={styles.file}
                    aria-current={row.path === workspace.current ? 'true' : undefined}
                    onClick={() => workspace.setCurrent(row.path)}
                  >
                    {row.name}
                  </button>
                  {added && (
                    <>
                      <span className={styles.newTag}>새 파일</span>
                      <IconButton size="dense" label={`${row.path} 삭제`} icon={<Trash2 size={14} />} onClick={() => workspace.removeFile(row.path)} />
                    </>
                  )}
                </>
              )}
            </li>
          )
        })}
      </ul>
      <form
        className={styles.treeFoot}
        onSubmit={(event) => {
          event.preventDefault()
          if (workspace.addFile(newPath)) setNewPath('')
        }}
      >
        <input
          className={styles.newPath}
          value={newPath}
          onChange={(event) => setNewPath(event.target.value)}
          placeholder="새 파일 경로"
          aria-label="새 파일 경로"
        />
        <div className={styles.treeActions}>
          <Button size="dense" variant="secondary" type="submit" icon={<FilePlus size={14} />}>
            추가
          </Button>
          <label className={styles.importLabel}>
            <Upload size={14} aria-hidden="true" />
            파일
            <input type="file" multiple aria-label="파일 가져오기" onChange={(event) => void workspace.importFiles(event.target.files)} />
          </label>
          <label className={styles.importLabel}>
            <FolderOpen size={14} aria-hidden="true" />
            폴더
            <input
              type="file"
              aria-label="폴더 가져오기"
              // 표준 속성이 아니라 ref 로 붙인다. 브라우저가 폴더 선택 대화상자를 연다.
              ref={(node) => node?.setAttribute('webkitdirectory', '')}
              onChange={(event) => void workspace.importFiles(event.target.files)}
            />
          </label>
        </div>
        {workspace.notice && (
          <p className={styles.notice} role="status">
            {workspace.notice}
          </p>
        )}
      </form>
    </nav>
  )
}

/* ─── 기록 ─── */

function History({ history, selected, onOpen }: { history: ProjectSubmission[]; selected: string | null; onOpen: (id: string) => void }) {
  if (history.length === 0) return <p className={styles.empty}>아직 제출이 없습니다.</p>
  return (
    <ul className={styles.history}>
      {history.map((item) => (
        <li key={item.id}>
          <button type="button" className={styles.historyRow} aria-current={item.id === selected ? 'true' : undefined} onClick={() => onOpen(item.id)}>
            <span className={styles.historyTime}>{fullTime(item.createdAt)}</span>
            {item.status !== 'COMPLETED' ? <Badge tone="warning">채점 중</Badge> : item.verdict && <VerdictBadge verdict={item.verdict} />}
            {item.score !== null && <span className={styles.historyScore}>{item.score}점</span>}
            <Source submission={item} />
          </button>
        </li>
      ))}
    </ul>
  )
}

/* ─── 결과 ─── */

function ResultPanel({ project, result }: { project: ProjectView; result: ProjectSubmission | null }) {
  const [tab, setTab] = useState<'tests' | 'log'>('tests')
  return (
    <section className={styles.result} aria-label="결과">
      <Tabs
        label="결과"
        items={[
          { key: 'tests', label: '테스트 결과' },
          { key: 'log', label: '빌드 로그', disabled: !result?.log },
        ]}
        value={tab}
        onChange={setTab}
      >
        {!result ? (
          <p className={styles.empty}>
            제출하면 공개 테스트({project.publicTests.join(', ')})와 숨은 테스트를 함께 돌립니다. 테스트를 더 쓰면 그 테스트가 대표 오답을
            잡는지도 잽니다.
          </p>
        ) : result.status !== 'COMPLETED' ? (
          <Judging status={result.status} />
        ) : tab === 'log' && result.log ? (
          <pre className={styles.log}>{result.log}</pre>
        ) : (
          <Outcome project={project} result={result} />
        )}
      </Tabs>
    </section>
  )
}

function Judging({ status }: { status: ProjectSubmission['status'] }) {
  const step = judgingStep(status)
  return (
    <div className={styles.judging}>
      <ol className={styles.steps} aria-label="채점 단계">
        {JUDGING_STEPS.map((label, index) => (
          <li
            key={label}
            className={index < step ? styles.stepDone : index === step ? styles.stepNow : styles.stepNext}
            aria-current={index === step ? 'step' : undefined}
          >
            {label}
          </li>
        ))}
      </ol>
      <p className={styles.muted}>빌드와 테스트는 분 단위로 걸립니다. 이 화면을 떠나도 채점은 이어지고, 결과는 "내 제출"에 남습니다.</p>
    </div>
  )
}

function Outcome({ project, result }: { project: ProjectView; result: ProjectSubmission }) {
  const publicModules = new Set(project.publicTests)
  const publicTests = result.tests.filter((test) => publicModules.has(test.module))
  const mine = result.tests.filter((test) => !publicModules.has(test.module))
  const cells = hiddenCells(result.hiddenPassed, result.hiddenTotal)
  return (
    <div className={styles.outcome}>
      <div>
        <div className={styles.verdict}>
          {result.verdict && <VerdictBadge verdict={result.verdict} />}
          {result.score !== null && <span className={styles.score}>{result.score}점</span>}
          <Source submission={result} />
          {result.revision > 1 && <span className={styles.muted}>재채점 {result.revision - 1}회</span>}
        </div>
        <TestList title="공개 테스트" tests={publicTests} />
        {mine.length > 0 && <TestList title="내가 더 쓴 테스트" tests={mine} />}
      </div>
      <div className={styles.side}>
        {cells.length > 0 && (
          <div className={styles.box}>
            <h3 className={styles.boxTitle}>숨은 테스트</h3>
            <div className={styles.cells} role="img" aria-label={`숨은 테스트 ${result.hiddenTotal}개 중 ${result.hiddenPassed}개 통과`}>
              {cells.map((passed, index) => (
                <span key={index} className={passed ? styles.cellPass : styles.cellFail} />
              ))}
            </div>
            <span className={styles.big}>
              {result.hiddenPassed} / {result.hiddenTotal}
            </span>
            <span className={styles.muted}>이름과 사유는 보이지 않습니다</span>
          </div>
        )}
        {result.probe && <Probe probe={result.probe} />}
      </div>
    </div>
  )
}

function TestList({ title, tests }: { title: string; tests: ProjectSubmission['tests'] }) {
  if (tests.length === 0) return null
  return (
    <>
      <h3 className={styles.listTitle}>{title}</h3>
      <ul className={styles.tests}>
        {tests.map((test) => (
          <li key={`${test.module}.${test.name}`}>
            <span className={test.passed ? styles.pass : styles.fail} aria-label={test.passed ? '통과' : '실패'}>
              {test.passed ? '✓' : '✕'}
            </span>
            <code>{test.name}</code>
            {test.message && <span className={styles.why}>{test.message}</span>}
          </li>
        ))}
      </ul>
    </>
  )
}

/**
 * 테스트 점검 — 내가 더 쓴 테스트가 대표 오답을 잡았는가 (실무군 결함 검출). 오답의 이름은 무엇을 놓쳤는지의
 * 힌트고 내용은 오지 않는다. 참조에서 떨어졌으면 그 테스트가 틀린 것을 기대하고 있다.
 */
function Probe({ probe }: { probe: ProjectProbeOutcome }) {
  const total = probe.killed.length + probe.survived.length
  const given = probe.alreadyCaught?.length ?? 0
  return (
    <div className={styles.box}>
      <h3 className={styles.boxTitle}>테스트 점검 — 내가 더 쓴 테스트</h3>
      {!probe.referencePassed ? (
        <>
          <span className={styles.fail}>정답 구현에서 떨어졌습니다 — 틀린 것을 기대하고 있습니다</span>
          {probe.log && <pre className={styles.log}>{probe.log}</pre>}
        </>
      ) : total === 0 ? (
        <span className={styles.muted}>공개 테스트가 이미 모든 오답을 잡습니다 — 더 쓴 테스트로 잴 것이 없습니다</span>
      ) : (
        <>
          <span className={`${styles.big} ${probe.killed.length * 2 >= total ? styles.pass : styles.fail}`}>
            오답 {total}개 중 {probe.killed.length}개
          </span>
          {probe.survived.length > 0 && (
            <span className={styles.muted}>놓친 오답: {probe.survived.map((name) => name.replace(/--.*$/, '')).join(', ')}</span>
          )}
        </>
      )}
      {given > 0 && <span className={styles.muted}>공개 테스트가 이미 잡는 오답 {given}개는 세지 않았습니다</span>}
    </div>
  )
}

/** 어디서 낸 제출인가 (디자인 시안 24) — CLI 제출은 기기 이름까지. 웹 초안과 다를 수 있다는 단서다 */
function Source({ submission }: { submission: ProjectSubmission }) {
  if (submission.source !== 'CLI') return <span className={styles.source}>웹</span>
  return <span className={`${styles.source} ${styles.sourceCli}`}>CLI{submission.device ? ` · ${submission.device}` : ''}</span>
}
