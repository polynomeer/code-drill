import { useQuery } from '@tanstack/react-query'
import { Suspense, lazy, useState } from 'react'
import { useLocation } from 'wouter'
import { Badge, Button, InlineAlert, Skeleton } from '../../design'
import { EDITOR_LANGUAGE } from '../../shared/types'
import type { SubmissionLanguage } from '../../shared/types'
import { useMediaQuery } from '../../shared/useMediaQuery'
import { adminApi } from './adminApi'
import type { FlaggedPair, Me } from './adminApi'
import { Clear, DecisionDialog, Id, When, useDecision } from './common'
import styles from './AdminPage.module.css'

const CodeDiffEditor = lazy(() => import('../submissions/CodeDiffEditor'))

const REFRESH = [['admin', 'integrity'], ['admin', 'count', 'integrity']]

/**
 * 유사도 신호 (§11.4 부정행위 방어, §10.4). REVIEWER.
 *
 * 결정은 사람이 **두 소스를 나란히 보고** 한다. 점수는 판단의 근거가 아니라 줄을 세우는 순서다 —
 * "탐지 신호를 단독 유죄 근거로 사용하지 않는다"(§10.4). 확인해도 판정도 계정도 바뀌지 않는다; 제재는
 * 보안 관리자가 이 신호를 근거로 따로 발부한다.
 */
export function IntegrityQueue({ me }: { me: Me }) {
  const query = useQuery({ queryKey: ['admin', 'integrity'], queryFn: adminApi.integrityQueue })
  const [selected, setSelected] = useState<string | null>(null)

  if (query.isError) return <InlineAlert tone="danger">{query.error.message}</InlineAlert>
  if (!query.data) return <Skeleton height={200} />
  if (query.data.length === 0) return <Clear>열린 유사도 신호가 없습니다.</Clear>

  const current = query.data.find((pair) => pair.flag.id === selected) ?? query.data[0]!

  return (
    <div className={styles.split}>
      <ul className={styles.pickList} aria-label="신호 목록">
        {query.data.map((pair) => (
          <li key={pair.flag.id}>
            <button
              type="button"
              className={styles.pick}
              aria-pressed={pair.flag.id === current.flag.id}
              onClick={() => setSelected(pair.flag.id)}
            >
              <span className={styles.pickTitle}>
                <code>{pair.flag.problemId}</code>
              </span>
              <span className={styles.cardMeta}>
                {Math.round(pair.flag.score * 100)}% · {pair.flag.language} · <When iso={pair.flag.createdAt} />
              </span>
            </button>
          </li>
        ))}
      </ul>
      <PairView key={current.flag.id} pair={current} me={me} />
    </div>
  )
}

function PairView({ pair, me }: { pair: FlaggedPair; me: Me }) {
  const [dialog, setDialog] = useState<'confirm' | 'dismiss' | null>(null)
  const [, navigate] = useLocation()
  const wide = useMediaQuery('(min-width: 1100px)')
  const resolve = useDecision(
    ({ yes, note }: { yes: boolean; note: string }) => adminApi.resolveFlag(pair.flag.id, yes, note || null),
    '신호를 처리했습니다',
    REFRESH,
  )
  const { flag } = pair
  const language = EDITOR_LANGUAGE[flag.language as SubmissionLanguage] ?? 'plaintext'

  return (
    <article className={styles.card} aria-label={`유사도 신호 ${flag.problemId}`}>
      <header className={styles.cardHead}>
        <Badge tone="warning">유사도 {Math.round(flag.score * 100)}%</Badge>
        <code className={styles.cardTitle}>{flag.problemId}</code>
        <span className={styles.cardMeta}>
          {flag.language} · <When iso={flag.createdAt} />
        </span>
      </header>
      <dl className={styles.pairMeta}>
        <div>
          <dt>왼쪽</dt>
          <dd>
            제출 <Id value={flag.submissionId} /> · 사용자 <Id value={flag.userId} />
          </dd>
        </div>
        <div>
          <dt>오른쪽</dt>
          <dd>
            제출 <Id value={flag.otherSubmissionId} /> · 사용자 <Id value={flag.otherUserId} />
          </dd>
        </div>
      </dl>
      {pair.source === null || pair.otherSource === null ? (
        <InlineAlert tone="warning">한쪽 소스가 남아 있지 않습니다 (계정 삭제·보관 기간). 나란히 볼 수 없으면 확인하지 마세요.</InlineAlert>
      ) : (
        <div className={styles.diff}>
          <Suspense fallback={<Skeleton height={420} />}>
            <CodeDiffEditor original={pair.source} modified={pair.otherSource} language={language} sideBySide={wide} />
          </Suspense>
        </div>
      )}
      <div className={styles.cardActions}>
        <Button size="dense" variant="danger" onClick={() => setDialog('confirm')} disabled={pair.source === null || pair.otherSource === null}>
          베낀 것으로 확인
        </Button>
        <Button size="dense" onClick={() => setDialog('dismiss')}>
          기각
        </Button>
      </div>
      {dialog && (
        <DecisionDialog
          open
          onClose={() => setDialog(null)}
          title={dialog === 'confirm' ? '베낀 것으로 확인' : '신호 기각'}
          description="결정은 기록으로만 남습니다 — 판정도 계정도 바뀌지 않습니다. 제재는 보안 관리자가 이 신호를 근거로 따로 발부합니다."
          confirm={dialog === 'confirm' ? '확인' : '기각'}
          danger={dialog === 'confirm'}
          reasonLabel="메모"
          reasonRequired={dialog === 'confirm'}
          pending={resolve.isPending}
          error={resolve.error}
          onConfirm={(note) =>
            resolve.mutate(
              { yes: dialog === 'confirm', note },
              {
                onSuccess: () => {
                  setDialog(null)
                  // 확인한 신호는 큐를 떠난다. 보안 관리자라면 이 신호를 근거로 든 제재 검토로 바로 간다
                  if (dialog === 'confirm' && me.roles.includes('SECURITY_ADMIN')) {
                    navigate(`/admin/sanctions?user=${flag.userId ?? ''}&evidence=similarity:${flag.id}`)
                  }
                },
              },
            )
          }
        />
      )}
    </article>
  )
}
