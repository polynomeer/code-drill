import { useQuery } from '@tanstack/react-query'
import { CircleCheck, CircleX } from 'lucide-react'
import { useState } from 'react'
import { Button, InlineAlert, Skeleton, TextField } from '../../design'
import { adminApi } from './adminApi'
import type { Me, PendingVersion } from './adminApi'
import { Clear, DecisionDialog, Id, When, useDecision } from './common'
import styles from './AdminPage.module.css'

/**
 * 문제 버전 검수 (§6.3 검증 보고서, §11.2 2인 원칙).
 *
 * 공개는 등록자가 아닌 PUBLISHER 가 한다. 그리고 **자기가 돌린 검증 보고서의 digest 를 적어서** 한다 —
 * 이 표의 digest 를 그대로 되돌려 보내는 단추를 두면 대조가 아니라 확인 클릭이 된다. 화면은 적은 값이
 * 등록된 값과 같은지만 미리 보여 준다.
 */
export function VersionsQueue({ me }: { me: Me }) {
  const query = useQuery({ queryKey: ['admin', 'versions'], queryFn: adminApi.pendingVersions })
  const [target, setTarget] = useState<PendingVersion | null>(null)
  const publisher = me.roles.includes('PUBLISHER')

  if (query.isError) return <InlineAlert tone="danger">{query.error.message}</InlineAlert>
  if (!query.data) return <Skeleton height={200} />
  if (query.data.length === 0) return <Clear>공개를 기다리는 버전이 없습니다.</Clear>

  return (
    <>
      {!publisher && <InlineAlert tone="info">공개는 PUBLISHER 역할이 합니다. 이 계정은 목록만 봅니다.</InlineAlert>}
      <div className={styles.tableWrap} tabIndex={0} role="region" aria-label="공개를 기다리는 버전 표">
        <table className={styles.table}>
          <thead>
            <tr>
              <th scope="col">문제</th>
              <th scope="col">버전</th>
              <th scope="col">검증기</th>
              <th scope="col">보고서 digest</th>
              <th scope="col">등록</th>
              <th scope="col">
                <span className="visually-hidden">결정</span>
              </th>
            </tr>
          </thead>
          <tbody>
            {query.data.map((version) => {
              const own = version.registeredBy === me.userId
              return (
                <tr key={version.versionId}>
                  <td>
                    <code>{version.problemId}</code>
                  </td>
                  <td className={styles.num}>
                    {version.publishedVersion === null ? `v${version.version} (첫 공개)` : `v${version.publishedVersion} → v${version.version}`}
                  </td>
                  <td>
                    <code>{version.validatorVersion}</code>
                  </td>
                  <td>
                    <code className={styles.digest} title={version.reportDigest}>
                      {version.reportDigest.slice(0, 16)}…
                    </code>
                  </td>
                  <td>
                    <Id value={version.registeredBy} mine={me.userId} /> · <When iso={version.registeredAt} />
                  </td>
                  <td className={styles.actionsCell}>
                    {publisher && (
                      <Button
                        size="dense"
                        variant="primary"
                        disabled={own}
                        title={own ? '등록한 사람은 공개할 수 없습니다 (2인 원칙)' : undefined}
                        onClick={() => setTarget(version)}
                      >
                        공개
                      </Button>
                    )}
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>
      {target && <PublishDialog version={target} onClose={() => setTarget(null)} />}
    </>
  )
}

function PublishDialog({ version, onClose }: { version: PendingVersion; onClose: () => void }) {
  const [digest, setDigest] = useState('')
  const [validator, setValidator] = useState(version.validatorVersion)
  const publish = useDecision(
    () => adminApi.publish(version.problemId, version.version, digest.trim(), validator.trim()),
    `${version.problemId} v${version.version} 을(를) 공개했습니다`,
    [['admin', 'versions'], ['admin', 'count', 'versions']],
  )
  const typed = digest.trim()
  const matches = typed.length > 0 && typed === version.reportDigest && validator.trim() === version.validatorVersion

  return (
    <DecisionDialog
      open
      onClose={onClose}
      title={`${version.problemId} v${version.version} 공개`}
      description="공개하면 새 제출이 이 버전으로 채점됩니다. 내가 돌린 검증 보고서의 digest 를 적으세요 — 등록된 값과 다르면 서버가 거절합니다."
      confirm="공개"
      pending={publish.isPending}
      error={publish.error}
      onConfirm={() => publish.mutate(undefined, { onSuccess: onClose })}
    >
      <TextField
        label="내 검증 보고서 digest"
        hint="./gradlew :judge:runner-agent:validateContent 가 남긴 보고서의 digest"
        value={digest}
        onChange={(event) => setDigest(event.target.value)}
        autoComplete="off"
        spellCheck={false}
        className={styles.mono}
      />
      <TextField label="검증기 버전" value={validator} onChange={(event) => setValidator(event.target.value)} className={styles.mono} />
      {typed.length > 0 && (
        <p className={matches ? styles.match : styles.mismatch} role="status">
          {matches ? <CircleCheck size={16} aria-hidden="true" /> : <CircleX size={16} aria-hidden="true" />}
          {matches ? '등록된 보고서와 같습니다' : '등록된 보고서와 다릅니다 — 패키지나 검증기가 바뀌었는지 확인하세요'}
        </p>
      )}
    </DecisionDialog>
  )
}
