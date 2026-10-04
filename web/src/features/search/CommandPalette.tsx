import { CornerDownLeft, FileCode2, Search } from 'lucide-react'
import { useEffect, useId, useMemo, useRef, useState } from 'react'
import type { KeyboardEvent } from 'react'
import { DifficultyBadge } from '../../design'
import { useProblemIndex } from '../problems/useProblemIndex'
import { search } from './search'
import type { Command, Hit } from './search'
import styles from './CommandPalette.module.css'

/**
 * 전역 검색·명령 팔레트 (⌘K, docs/ui-overhaul.md §4·§6.2).
 *
 * 문제를 번호·제목·태그로 찾아 바로 풀이 화면으로 가고, 화면 이동과 테마 같은 행동도 같은 칸에서 한다.
 * 키보드만으로 끝난다 — ↑/↓ 로 고르고 Enter, Esc 로 닫는다. WAI-ARIA combobox 패턴이다: 포커스는 입력에
 * 머물고 고른 줄은 `aria-activedescendant` 로 알린다.
 *
 * 처음 열 때 따로 받아 온다 — 문제 목록과 이 화면은 첫 화면 번들에 들지 않는다.
 */
export default function CommandPalette({
  open,
  onClose,
  commands,
  onOpenProblem,
}: {
  open: boolean
  onClose: () => void
  commands: Command[]
  onOpenProblem: (problemId: string) => void
}) {
  const dialog = useRef<HTMLDialogElement>(null)
  const input = useRef<HTMLInputElement>(null)
  const [query, setQuery] = useState('')
  const [active, setActive] = useState(0)
  const index = useProblemIndex()
  const base = useId()
  const listId = `${base}-list`

  const hits = useMemo(() => search(query, [...index.values()], commands), [query, index, commands])

  useEffect(() => {
    const element = dialog.current
    if (!element) return
    if (open && !element.open) {
      setQuery('')
      setActive(0)
      element.showModal()
      input.current?.focus()
    } else if (!open && element.open) {
      element.close()
    }
  }, [open])

  useEffect(() => setActive(0), [query])

  const choose = (hit: Hit | undefined) => {
    if (!hit) return
    onClose()
    if (hit.kind === 'problem') onOpenProblem(hit.problem.id)
    else hit.command.run()
  }

  const onKeyDown = (event: KeyboardEvent<HTMLInputElement>) => {
    if (event.key === 'ArrowDown') {
      event.preventDefault()
      setActive((current) => Math.min(hits.length - 1, current + 1))
    } else if (event.key === 'ArrowUp') {
      event.preventDefault()
      setActive((current) => Math.max(0, current - 1))
    } else if (event.key === 'Enter') {
      event.preventDefault()
      choose(hits[active])
    }
  }

  // 고른 줄이 보이게 — 목록 안에서만 스크롤한다
  useEffect(() => {
    document.getElementById(`${base}-option-${active}`)?.scrollIntoView({ block: 'nearest' })
  }, [active, base])

  const loading = index.size === 0 && query.trim().length > 0

  return (
    <dialog
      ref={dialog}
      className={styles.dialog}
      aria-label="검색"
      onCancel={(event) => {
        event.preventDefault()
        onClose()
      }}
      onClick={(event) => {
        if (event.target === dialog.current) onClose()
      }}
    >
      {open && (
        <div className={styles.panel}>
          <div className={styles.field}>
            <Search size={18} aria-hidden="true" />
            <input
              ref={input}
              className={styles.input}
              role="combobox"
              aria-expanded={hits.length > 0}
              aria-controls={listId}
              aria-activedescendant={hits.length > 0 ? `${base}-option-${active}` : undefined}
              aria-autocomplete="list"
              aria-label="문제 번호·제목·태그, 또는 이동할 곳"
              placeholder="문제 번호·제목·태그, 또는 이동할 곳"
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              onKeyDown={onKeyDown}
              autoComplete="off"
              spellCheck={false}
            />
            <kbd className={styles.esc}>Esc</kbd>
          </div>
          <ul id={listId} role="listbox" aria-label="검색 결과" className={styles.list}>
            {hits.map((hit, position) => {
              const selected = position === active
              const groupStart = position === 0 || groupOf(hits[position - 1]!) !== groupOf(hit)
              return (
                <li
                  key={hit.kind === 'problem' ? `p-${hit.problem.id}` : `c-${hit.command.id}`}
                  id={`${base}-option-${position}`}
                  role="option"
                  aria-selected={selected}
                  className={selected ? `${styles.option} ${styles.active}` : styles.option}
                  onMouseMove={() => setActive(position)}
                  onClick={() => choose(hit)}
                  data-group={groupStart ? groupOf(hit) : undefined}
                >
                  {hit.kind === 'problem' ? (
                    <>
                      <FileCode2 size={16} aria-hidden="true" className={styles.icon} />
                      <span className={styles.number}>{hit.problem.number}</span>
                      <span className={styles.label}>{hit.problem.title}</span>
                      <DifficultyBadge level={hit.problem.difficulty} />
                    </>
                  ) : (
                    <>
                      <span className={styles.icon} aria-hidden="true">
                        →
                      </span>
                      <span className={styles.label}>{hit.command.label}</span>
                      <span className={styles.group}>{hit.command.group}</span>
                    </>
                  )}
                  {selected && <CornerDownLeft size={14} aria-hidden="true" className={styles.enter} />}
                </li>
              )
            })}
          </ul>
          {hits.length === 0 && (
            <p className={styles.empty} role="status">
              {loading ? '문제 목록을 불러오는 중…' : `"${query}" 에 맞는 것이 없습니다`}
            </p>
          )}
          <p className={styles.hint} aria-hidden="true">
            <kbd>↑</kbd>
            <kbd>↓</kbd> 고르기 · <kbd>Enter</kbd> 열기 · <kbd>⌘K</kbd> 언제든 열기
          </p>
        </div>
      )}
    </dialog>
  )
}

function groupOf(hit: Hit): string {
  return hit.kind === 'problem' ? '문제' : hit.command.group
}
