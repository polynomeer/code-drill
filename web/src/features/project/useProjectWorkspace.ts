import { useEffect, useRef, useState } from 'react'
import {
  discardProjectDraft,
  getProject,
  getProjectDraft,
  getProjectSubmission,
  listProjectSubmissions,
  saveProjectDraft,
  submitProject,
} from '../../api/client'
import type { ProjectDraft, ProjectSubmission, ProjectView } from '../../shared/types'
import { useDraftSync } from '../workspace/useDraftSync'
import { isKitOrBuildFile } from './kitFiles'
import { firstFile } from './projectView'

/** 서버의 파일 한도(256KB)와 같다. 넘는 것은 어차피 제출이 거절된다. */
const MAX_IMPORT_BYTES = 256 * 1024

/**
 * 프로젝트형 작업 공간의 상태와 행동 (feature-roadmap 11단계, docs/ui-overhaul.md §6.11).
 *
 * 화면과 떼어 둔다 — 그리는 법이 바뀌어도(홈 안의 패널 → 전용 화면) 초안·판정·가져오기의 규칙은 그대로다.
 *
 * - 초안이 있으면 그것을, 없으면 시작 저장소를 연다. 손대기 전에는 저장하지 않는다 — 시작 저장소를 초안으로
 *   적어 두면 아무것도 안 한 사람에게 초안이 생긴다 (§8.1, [useDraftSync]).
 * - 판정은 분 단위라 끝날 때까지 1초마다 묻는다. SSE 는 알고리즘 제출의 것이다.
 * - 파일·폴더 가져오기는 브라우저에서 읽어 얹는다. 키트 파일·빌드 산출물은 빼고([isKitOrBuildFile]), 너무 큰
 *   파일은 건너뛰고 그 사실을 말한다 — 조용히 빠지면 제출이 왜 다른지 아무도 모른다.
 */
export function useProjectWorkspace(id: string) {
  const [project, setProject] = useState<ProjectView | null>(null)
  const [files, setFiles] = useState<Record<string, string>>({})
  const [current, setCurrent] = useState<string | null>(null)
  const [result, setResult] = useState<ProjectSubmission | null>(null)
  const [history, setHistory] = useState<ProjectSubmission[]>([])
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [fromDraft, setFromDraft] = useState(false)
  const [notice, setNotice] = useState<string | null>(null)
  const [touched, setTouched] = useState(false)
  const poll = useRef<number | null>(null)

  const draftSync = useDraftSync<Record<string, string>, ProjectDraft>({
    key: project?.id ?? null,
    value: files,
    enabled: project !== null && touched,
    load: () => (project ? getProjectDraft(project.id) : Promise.resolve(null)),
    save: (next, version) => saveProjectDraft(project!.id, next, version),
  })

  const refreshHistory = () =>
    listProjectSubmissions(id)
      .then(setHistory)
      .catch(() => setHistory([]))

  useEffect(() => {
    // 초안을 못 읽어도 시작 저장소는 열린다
    Promise.all([getProject(id), getProjectDraft(id).catch(() => null)])
      .then(([view, draft]) => {
        const opened = draft?.files ?? view.files
        setProject(view)
        setFiles(opened)
        setFromDraft(draft !== null)
        setCurrent(firstFile(opened))
      })
      .catch((e: Error) => setError(e.message))
    void refreshHistory()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id])

  useEffect(() => {
    if (!result || result.status === 'COMPLETED') return
    poll.current = window.setInterval(() => {
      getProjectSubmission(result.id)
        .then((next) => {
          setResult(next)
          if (next.status === 'COMPLETED') void refreshHistory()
        })
        .catch(() => undefined)
    }, 1000)
    return () => {
      if (poll.current !== null) window.clearInterval(poll.current)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [result, id])

  const judging = result !== null && result.status !== 'COMPLETED'

  const submit = async () => {
    if (!project || submitting || judging) return
    setSubmitting(true)
    setError(null)
    try {
      setResult(await submitProject(project.id, files))
      void refreshHistory()
    } catch (e) {
      setError(e instanceof Error ? e.message : '제출에 실패했습니다')
    } finally {
      setSubmitting(false)
    }
  }

  const edit = (path: string, content: string) => {
    setTouched(true)
    setFiles((prev) => ({ ...prev, [path]: content }))
  }

  /** 새 파일. 이미 있는 경로면 그 파일을 연다 */
  const addFile = (raw: string): boolean => {
    const path = raw.trim().replace(/^\/+/, '')
    if (!path) return false
    if (files[path] === undefined) {
      setFiles({ ...files, [path]: '' })
      setTouched(true)
    }
    setCurrent(path)
    return true
  }

  const removeFile = (path: string) => {
    const next = { ...files }
    delete next[path]
    setFiles(next)
    setTouched(true)
    if (current === path) setCurrent(firstFile(next))
  }

  const importFiles = async (list: FileList | null) => {
    if (!list || !project) return
    const next = { ...files }
    let taken = 0
    let kit = 0
    const skipped: string[] = []
    for (const file of Array.from(list)) {
      const relative = file.webkitRelativePath || file.name
      const parts = relative.split('/')
      const path = (file.webkitRelativePath ? parts.slice(1) : parts).join('/')
      if (!path) {
        skipped.push(relative)
        continue
      }
      // 키트 파일·빌드 산출물·숨은 파일은 하나하나 말하지 않고 수만 센다 — 받은 폴더면 수십 개다
      if (isKitOrBuildFile(path)) {
        kit += 1
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
    setCurrent((cur) => cur ?? firstFile(next))
    setNotice(
      `${taken}개 파일을 가져왔습니다.` +
        (kit ? ` 키트·빌드 파일 ${kit}개는 빼고 읽었습니다.` : '') +
        (skipped.length ? ` 건너뜀: ${skipped.join(', ')}` : ''),
    )
  }

  const startOver = async () => {
    if (!project) return
    try {
      await discardProjectDraft(project.id)
    } catch {
      // 초안을 못 버려도 화면은 시작 저장소로 간다. 다음 저장이 충돌로 알려 줄 것이다.
    }
    setFiles(project.files)
    setCurrent(firstFile(project.files))
    setFromDraft(false)
    setTouched(false)
    draftSync.reset()
  }

  /** 기록의 제출 하나를 결과 창에 연다 */
  const openSubmission = (submissionId: string) =>
    getProjectSubmission(submissionId)
      .then(setResult)
      .catch(() => undefined)

  /** 저장 충돌 해소 — 서버 것을 가져오거나 내 것으로 덮는다 */
  const resolveConflict = (server: ProjectDraft | null, version: number) => {
    if (server) {
      setFiles(server.files)
      draftSync.resolveConflict(version)
    } else {
      draftSync.resolveConflict(version, files)
    }
  }

  return {
    project,
    files,
    current,
    setCurrent,
    result,
    history,
    submitting,
    judging,
    error,
    fromDraft,
    notice,
    saveState: draftSync.state,
    submit,
    edit,
    addFile,
    removeFile,
    importFiles,
    startOver,
    openSubmission,
    resolveConflict,
  }
}
