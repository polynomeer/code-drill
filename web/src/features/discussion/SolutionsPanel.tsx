import { useEffect, useState } from 'react'
import { listSolutions } from '../../api/client'
import type { DiscussionPost } from '../../shared/types'
import { PostView } from './DiscussionPanel'

/**
 * 공유된 풀이 (기획서 §8.5 "정적 풀이").
 *
 * 해설 아래, 맞힌 사람에게만 그린다. 남이 올린 접근을 코드와 함께 읽고, 도움이 됐으면
 * 한 번 남긴다 — 그것이 기여자 평판의 재료다. 올리는 것은 판정 화면에서 한다: 맞힌
 * 제출의 판정 아래에 "풀이로 올리기"가 있다.
 */
export function SolutionsPanel({ problemId }: { problemId: string }) {
  const [solutions, setSolutions] = useState<DiscussionPost[] | null>(null)

  const refresh = () => {
    listSolutions(problemId)
      .then(setSolutions)
      .catch(() => setSolutions([]))
  }

  useEffect(() => {
    setSolutions(null)
    listSolutions(problemId)
      .then(setSolutions)
      .catch(() => setSolutions([]))
  }, [problemId])

  if (solutions === null) return null
  return (
    <div className="solutions">
      <h4>
        공유된 풀이 {solutions.length > 0 && <span className="muted">{solutions.length}</span>}
      </h4>
      {solutions.length === 0 ? (
        <p className="muted small">아직 없습니다. 맞힌 제출의 판정 아래에서 내 접근을 올릴 수 있습니다.</p>
      ) : (
        solutions.map((s) => <PostView key={s.id} post={s} onOpenReplay={() => {}} onChanged={refresh} />)
      )}
    </div>
  )
}
