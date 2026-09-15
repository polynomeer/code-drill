import { useCallback, useEffect, useState } from 'react'
import { answerQuestion, askQuestion, getThread, listQuestions, markHelpful, reportPost } from '../../api/client'
import type {
  DiscussionAnchorRequest,
  DiscussionPost,
  DiscussionThread,
  Submission,
} from '../../shared/types'

/**
 * 문제별 질문 게시판 (기획서 §8.5 "질문 게시판과 코드 구간 링크", "리플레이 시점을
 * 공유하는 주석").
 *
 * 막힌 사람이 묻고 맞힌 사람이 답한다. 글에는 **자기 제출의 한 자리**를 붙일 수 있다 —
 * 코드의 줄 범위, 리플레이의 걸음, 또는 둘 다. 붙인 코드 구간은 글에 실려 오고, 붙인
 * 리플레이는 그 글을 볼 수 있는 사람이 열 수 있다.
 *
 * 풀이를 드러내는 글은 글쓴이가 표시하고, 맞힌 사람에게만 열린다. 이름은 어디에도
 * 나가지 않는다 — 내 것인가만.
 */
export function DiscussionPanel({
  problemId,
  submission,
  replayStep,
  onOpenReplay,
}: {
  problemId: string
  /** 지금 열어 둔 내 제출. 글에 붙일 수 있는 것은 이것이다. */
  submission: Submission | null
  /** 리플레이가 서 있는 걸음. 리플레이가 없으면 null. */
  replayStep: number | null
  onOpenReplay: (submissionId: string, step: number | null) => void
}) {
  const [questions, setQuestions] = useState<DiscussionPost[] | null>(null)
  const [open, setOpen] = useState<DiscussionThread | null>(null)
  const [asking, setAsking] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const refresh = useCallback(() => {
    listQuestions(problemId)
      .then(setQuestions)
      .catch(() => setQuestions([]))
  }, [problemId])

  useEffect(() => {
    setQuestions(null)
    setOpen(null)
    setAsking(false)
    setError(null)
    refresh()
  }, [refresh])

  const openThread = (id: string) => {
    setError(null)
    getThread(id)
      .then(setOpen)
      .catch((e: Error) => setError(e.message))
  }

  // 붙일 수 있는 것은 이 문제의 내 제출뿐이다. 남의 것을 열어 둔 상태에서는 붙일 것이 없다.
  const anchorable =
    submission && submission.problemId === problemId && submission.mine !== false ? submission : null

  return (
    <section className="panel discussion">
      <h3>
        질문 {questions && questions.length > 0 && <span className="muted">{questions.length}</span>}
      </h3>
      {error && <p className="warn small">{error}</p>}

      {open ? (
        <ThreadView
          thread={open}
          anchorable={anchorable}
          replayStep={replayStep}
          onOpenReplay={onOpenReplay}
          onBack={() => {
            setOpen(null)
            refresh()
          }}
          onChanged={() => openThread(open.question.id)}
        />
      ) : (
        <>
          {questions === null && <p className="muted small">읽는 중…</p>}
          {questions && questions.length === 0 && !asking && (
            <p className="muted small">아직 질문이 없습니다. 막힌 곳이 있으면 먼저 물어 보세요.</p>
          )}
          {questions && questions.length > 0 && (
            <ul className="discussion-list">
              {questions.map((q) => (
                <li key={q.id}>
                  <button type="button" className="linklike" onClick={() => openThread(q.id)}>
                    {q.title}
                  </button>
                  <span className="muted small">
                    {' '}
                    답 {q.answerCount}
                    {q.spoiler && ' · 풀이 노출'}
                    {q.anchor && ' · 코드'}
                    {q.mine && ' · 내 질문'}
                  </span>
                </li>
              ))}
            </ul>
          )}
          {asking ? (
            <Compose
              kind="question"
              anchorable={anchorable}
              replayStep={replayStep}
              onCancel={() => setAsking(false)}
              onSubmit={async (title, body, anchor, spoiler) => {
                const posted = await askQuestion(problemId, title, body, anchor, spoiler)
                setAsking(false)
                setQuestions([posted, ...(questions ?? [])])
              }}
            />
          ) : (
            <button type="button" onClick={() => setAsking(true)}>
              질문하기
            </button>
          )}
        </>
      )}
    </section>
  )
}

function ThreadView({
  thread,
  anchorable,
  replayStep,
  onOpenReplay,
  onBack,
  onChanged,
}: {
  thread: DiscussionThread
  anchorable: Submission | null
  replayStep: number | null
  onOpenReplay: (submissionId: string, step: number | null) => void
  onBack: () => void
  onChanged: () => void
}) {
  const [answering, setAnswering] = useState(false)
  return (
    <div className="discussion-thread">
      <button type="button" className="linklike" onClick={onBack}>
        ← 목록
      </button>
      <PostView post={thread.question} onOpenReplay={onOpenReplay} onChanged={onChanged} />
      {thread.answers.map((a) => (
        <PostView key={a.id} post={a} onOpenReplay={onOpenReplay} onChanged={onChanged} />
      ))}
      {answering ? (
        <Compose
          kind="answer"
          anchorable={anchorable}
          replayStep={replayStep}
          onCancel={() => setAnswering(false)}
          onSubmit={async (_title, body, anchor, spoiler) => {
            await answerQuestion(thread.question.id, body, anchor, spoiler)
            setAnswering(false)
            onChanged()
          }}
        />
      ) : (
        <button type="button" onClick={() => setAnswering(true)}>
          답하기
        </button>
      )}
    </div>
  )
}

const KIND_LABEL: Record<DiscussionPost['kind'], string> = { QUESTION: '질문', ANSWER: '답', SOLUTION: '풀이' }
const TIER_LABEL = { NEW: '', ACTIVE: ' · 기여자', TRUSTED: ' · 믿을 만한 기여자' }

export function PostView({
  post,
  onOpenReplay,
  onChanged,
}: {
  post: DiscussionPost
  onOpenReplay: (submissionId: string, step: number | null) => void
  onChanged: () => void
}) {
  const [reporting, setReporting] = useState(false)
  const [reason, setReason] = useState('')
  const [reported, setReported] = useState(false)
  const [marked, setMarked] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const helpful = async () => {
    setError(null)
    try {
      await markHelpful(post.id)
      setMarked(true)
      onChanged()
    } catch (e) {
      setError(e instanceof Error ? e.message : '남기지 못했습니다')
    }
  }

  const report = async () => {
    setError(null)
    try {
      await reportPost(post.id, reason)
      setReported(true)
      setReporting(false)
      onChanged()
    } catch (e) {
      setError(e instanceof Error ? e.message : '신고하지 못했습니다')
    }
  }

  return (
    <article className={`discussion-post${post.parentId ? ' discussion-answer' : ''}`}>
      {post.title && <h4>{post.title}</h4>}
      <p className="muted small">
        {KIND_LABEL[post.kind]}
        {post.mine && ' · 내 글'}
        {post.erased && ' · 지운 계정'}
        {!post.mine && post.contributor && TIER_LABEL[post.contributor]}
        {post.spoiler && post.kind !== 'SOLUTION' && ' · 풀이 노출'}
        {post.helpful > 0 && ` · 도움됐다 ${post.helpful}`}
        {' · '}
        {new Date(post.createdAt).toLocaleString()}
      </p>
      {post.locked ? (
        <p className="muted small">풀이를 드러내는 글입니다. 이 문제를 맞힌 뒤에 열립니다.</p>
      ) : (
        <>
          {post.body && <p className="discussion-body">{post.body}</p>}
          {post.anchor && (
            <div className="discussion-anchor">
              {post.anchor.excerpt !== null && (
                <>
                  <p className="muted small">
                    {post.kind === 'SOLUTION' ? (
                      '코드 전체'
                    ) : (
                      <>
                        코드 {post.anchor.lineFrom}
                        {post.anchor.lineTo !== post.anchor.lineFrom && `–${post.anchor.lineTo}`}줄
                      </>
                    )}
                  </p>
                  <pre className="code">{post.anchor.excerpt}</pre>
                </>
              )}
              <button
                type="button"
                className="linklike"
                onClick={() => onOpenReplay(post.anchor!.submissionId, post.anchor!.step)}
              >
                {post.anchor.step !== null ? `리플레이 ${post.anchor.step}걸음 열기` : '판정 열기'}
              </button>
            </div>
          )}
        </>
      )}
      {/* 도움됐다는 맞힌 사람이 남의 글에 한 번. 잠긴 글에는 남길 것이 없다 (§8.5). */}
      {!post.mine && !post.locked && (
        <p className="small">
          {post.markedHelpful || marked ? (
            <span className="muted">도움됐다고 남겼습니다</span>
          ) : (
            <button type="button" className="linklike" onClick={() => void helpful()}>
              도움됐다
            </button>
          )}
        </p>
      )}
      {!post.mine && !reported && (
        <div className="discussion-report">
          {reporting ? (
            <>
              <input
                className="rationale"
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                aria-label="신고 사유"
                placeholder="무엇이 문제인지 (10자 이상)"
              />
              <button type="button" onClick={() => void report()} disabled={reason.trim().length < 10}>
                신고
              </button>
              <button type="button" className="linklike" onClick={() => setReporting(false)}>
                취소
              </button>
            </>
          ) : (
            <button type="button" className="linklike small" onClick={() => setReporting(true)}>
              신고
            </button>
          )}
          {error && <p className="warn small">{error}</p>}
        </div>
      )}
      {reported && <p className="muted small">신고했습니다. 검수자가 봅니다.</p>}
    </article>
  )
}

/**
 * 글쓰기. 붙일 자리는 지금 열어 둔 내 제출에서 고른다 — 줄 범위, 리플레이가 서 있는
 * 걸음, 또는 둘 다. 제출이 없으면 붙일 것도 없다.
 */
function Compose({
  kind,
  anchorable,
  replayStep,
  onCancel,
  onSubmit,
}: {
  kind: 'question' | 'answer'
  anchorable: Submission | null
  replayStep: number | null
  onCancel: () => void
  onSubmit: (title: string, body: string, anchor: DiscussionAnchorRequest | null, spoiler: boolean) => Promise<void>
}) {
  const [title, setTitle] = useState('')
  const [body, setBody] = useState('')
  const [spoiler, setSpoiler] = useState(kind === 'answer')
  const [attachCode, setAttachCode] = useState(false)
  const [lineFrom, setLineFrom] = useState('')
  const [lineTo, setLineTo] = useState('')
  const [attachStep, setAttachStep] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const submit = async () => {
    setError(null)
    setBusy(true)
    try {
      let anchor: DiscussionAnchorRequest | null = null
      if (anchorable && (attachCode || attachStep)) {
        anchor = { submissionId: anchorable.id }
        if (attachCode) {
          anchor.lineFrom = Number(lineFrom)
          anchor.lineTo = Number(lineTo || lineFrom)
        }
        if (attachStep && replayStep !== null) anchor.step = replayStep
      }
      await onSubmit(title, body, anchor, spoiler)
    } catch (e) {
      setError(e instanceof Error ? e.message : '올리지 못했습니다')
    } finally {
      setBusy(false)
    }
  }

  const ready = body.trim().length >= 10 && (kind === 'answer' || title.trim().length >= 3)

  return (
    <div className="discussion-compose">
      {kind === 'question' && (
        <input
          className="rationale"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          aria-label="질문 제목"
          placeholder="무엇이 막히는지 한 줄 (3자 이상)"
        />
      )}
      <textarea
        className="rationale"
        rows={4}
        value={body}
        onChange={(e) => setBody(e.target.value)}
        aria-label={kind === 'question' ? '질문 본문' : '답 본문'}
        placeholder={kind === 'question' ? '무엇을 해 봤고 어디서 어긋나는지 (10자 이상)' : '어디를 보면 되는지 (10자 이상)'}
      />
      <label className="small">
        <input type="checkbox" checked={spoiler} onChange={(e) => setSpoiler(e.target.checked)} /> 풀이를 드러냅니다 — 맞힌
        사람에게만 보입니다
      </label>
      {anchorable ? (
        <div className="discussion-attach small">
          <label>
            <input type="checkbox" checked={attachCode} onChange={(e) => setAttachCode(e.target.checked)} /> 지금 연 제출의
            코드 구간 붙이기
          </label>
          {attachCode && (
            <span>
              {' '}
              <input
                type="number"
                min={1}
                value={lineFrom}
                onChange={(e) => setLineFrom(e.target.value)}
                aria-label="시작 줄"
                placeholder="시작 줄"
                className="discussion-line"
              />
              –
              <input
                type="number"
                min={1}
                value={lineTo}
                onChange={(e) => setLineTo(e.target.value)}
                aria-label="끝 줄"
                placeholder="끝 줄"
                className="discussion-line"
              />
            </span>
          )}
          {replayStep !== null && (
            <label>
              <input type="checkbox" checked={attachStep} onChange={(e) => setAttachStep(e.target.checked)} /> 리플레이{' '}
              {replayStep}걸음 붙이기
            </label>
          )}
        </div>
      ) : (
        <p className="muted small">제출을 열어 두면 코드 구간이나 리플레이 시점을 붙일 수 있습니다.</p>
      )}
      <div>
        <button type="button" onClick={() => void submit()} disabled={!ready || busy}>
          {kind === 'question' ? '올리기' : '답 올리기'}
        </button>{' '}
        <button type="button" className="linklike" onClick={onCancel}>
          취소
        </button>
      </div>
      {error && <p className="warn small">{error}</p>}
    </div>
  )
}
