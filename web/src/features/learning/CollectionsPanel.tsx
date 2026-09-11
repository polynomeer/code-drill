import { useEffect, useState } from 'react'
import { addToCollection, createCollection, getCollections, removeFromCollection } from '../../api/client'
import type { Collection } from '../../shared/types'

/**
 * 개인 문제집 (PRD FR-205).
 *
 * > 사용자는 문제를 개인 문제집에 저장합니다. 저장 상태가 즉시 반영되고 중복 저장되지 않습니다.
 *
 * 열려 있는 문제를 담는 버튼 하나와 목록이 전부다. 중복은 서버가 무시하므로 화면이
 * 따로 막지 않는다 — 두 곳에서 막으면 언젠가 한쪽만 고쳐진다.
 */
export function CollectionsPanel({
  currentProblemId,
  onOpenProblem,
}: {
  currentProblemId: string | null
  onOpenProblem: (problemId: string) => void
}) {
  const [collections, setCollections] = useState<Collection[]>([])
  const [name, setName] = useState('')

  const reload = () => getCollections().then(setCollections).catch(() => setCollections([]))
  useEffect(() => { void reload() }, [])

  const create = async () => {
    if (!name.trim()) return
    await createCollection(name.trim()).catch(() => null)
    setName('')
    await reload()
  }

  const add = async (collectionId: string) => {
    if (!currentProblemId) return
    await addToCollection(collectionId, currentProblemId).catch(() => null)
    await reload()
  }

  const remove = async (collectionId: string, problemId: string) => {
    await removeFromCollection(collectionId, problemId).catch(() => null)
    await reload()
  }

  return (
    <section className="panel collections">
      <h3>문제집</h3>
      <ul className="plain">
        {collections.map((c) => (
          <li key={c.id}>
            <div className="prescription-head">
              <strong className="small">{c.name}</strong>
              <span className="muted small">{c.problems.length}문제</span>
              {currentProblemId && !c.problems.includes(currentProblemId) && (
                <button type="button" className="linklike small" onClick={() => void add(c.id)}>
                  지금 문제 담기
                </button>
              )}
            </div>
            <ul className="plain indent">
              {c.problems.map((p) => (
                <li key={p} className="small">
                  <button type="button" className="linklike mono" onClick={() => onOpenProblem(p)}>{p}</button>{' '}
                  <button type="button" className="linklike muted" onClick={() => void remove(c.id, p)} aria-label={`${p} 빼기`}>
                    ×
                  </button>
                </li>
              ))}
            </ul>
          </li>
        ))}
      </ul>
      <div className="prescription-head">
        <input
          className="rationale"
          placeholder="새 문제집 이름"
          value={name}
          onChange={(e) => setName(e.target.value)}
          onKeyDown={(e) => { if (e.key === 'Enter') void create() }}
          aria-label="새 문제집 이름"
        />
        <button type="button" onClick={() => void create()} disabled={!name.trim()}>만들기</button>
      </div>
    </section>
  )
}
