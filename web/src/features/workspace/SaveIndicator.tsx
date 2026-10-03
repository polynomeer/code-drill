import type { DraftSyncState } from './useDraftSync'

/**
 * 자동 저장 상태.
 *
 * 충돌은 배너로 남긴다. 토스트처럼 사라지면 사용자가 선택하기 전에 놓친다 —
 * 그 순간 잃는 것이 작성 중이던 코드다.
 */
export function SaveIndicator<C extends { version: number }>({
  state,
  onResolve,
}: {
  state: DraftSyncState<C>
  /** 서버 것을 가져오면 [current] 가 오고, 내 것을 유지하면 null 이 온다. */
  onResolve: (current: C | null, version: number) => void
}) {
  if (state.status === 'conflict') {
    return (
      <div className="conflict">
        <p>
          다른 곳에서 이 초안이 저장됐습니다. 어느 쪽을 남길지 고르세요.
          <span className="muted"> (서버 버전 {state.current.version})</span>
        </p>
        <div className="conflict-actions">
          <button onClick={() => onResolve(state.current, state.current.version)}>
            서버 것 가져오기
          </button>
          <button className="primary" onClick={() => onResolve(null, state.current.version)}>
            내 것 유지하고 덮어쓰기
          </button>
        </div>
      </div>
    )
  }

  const label = {
    idle: '',
    saving: '저장 중…',
    saved: '저장됨',
    failed: state.status === 'failed' ? `저장 실패: ${state.message}` : '',
  }[state.status]

  return <p className={`save-state ${state.status === 'failed' ? 'warn' : 'muted'}`}>{label}</p>
}
