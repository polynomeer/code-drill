import { useQuery } from '@tanstack/react-query'
import { CheckCircle2, XCircle } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'wouter'
import { decideDevice, getDeviceRequest } from '../../api/client'
import { Button, EmptyState, InlineAlert, TextField } from '../../design'
import { fullTime } from '../../shared/format'
import { normalizeUserCode } from './deviceCode'
import styles from './DevicePage.module.css'

/**
 * CLI 로그인 승인 `/device` (RFC 8628 — feature-roadmap 11단계 이어서, 2단계, 디자인 시안 22).
 *
 * `codedrill login` 이 보여 준 코드를 맞춰 승인하면 그 기기가 연결된다. 사람이 하는 일은 **코드가 같은지 보는 것**
 * 하나다 — 그래서 코드를 크게 보이고, 내가 연 요청이 아니면 승인하지 말라고 먼저 말한다. 남이 코드를 보내 승인을
 * 부탁하는 것이 이 방식의 대표적인 속임수다.
 */
export function DevicePage() {
  const [typed, setTyped] = useState(() => new URLSearchParams(window.location.search).get('code') ?? '')
  const [code, setCode] = useState(() => normalizeUserCode(typed))
  const [decided, setDecided] = useState<'approved' | 'denied' | 'gone' | null>(null)
  const [busy, setBusy] = useState(false)

  const request = useQuery({
    queryKey: ['device-request', code],
    queryFn: () => getDeviceRequest(code!),
    enabled: code !== null && decided === null,
    retry: false,
  })

  const decide = async (approve: boolean) => {
    if (!code) return
    setBusy(true)
    try {
      setDecided((await decideDevice(code, approve)) ? (approve ? 'approved' : 'denied') : 'gone')
    } finally {
      setBusy(false)
    }
  }

  return (
    <main className={styles.page} id="main">
      <div className={styles.card}>
        <h1 className={styles.title}>CLI 로그인 승인</h1>

        {decided === 'approved' ? (
          <EmptyState title="승인했습니다" icon={<CheckCircle2 size={28} />}>
            터미널로 돌아가세요 — 잠시 뒤 연결됩니다. 연결은 계정 설정의 "연결된 기기"에서 끊을 수 있습니다.
          </EmptyState>
        ) : decided === 'denied' ? (
          <EmptyState title="거절했습니다" icon={<XCircle size={28} />}>
            그 기기는 연결되지 않습니다.
          </EmptyState>
        ) : code === null || decided === 'gone' || (request.isSuccess && request.data === null) ? (
          <>
            {(decided === 'gone' || request.data === null) && (
              <InlineAlert tone="warning" title="기다리는 요청이 없습니다">
                코드가 만료됐거나 이미 결정했습니다. 터미널에서 <code>codedrill login</code> 을 다시 하세요.
              </InlineAlert>
            )}
            <form
              className={styles.form}
              onSubmit={(event) => {
                event.preventDefault()
                setDecided(null)
                setCode(normalizeUserCode(typed))
              }}
            >
              <TextField
                label="터미널에 보이는 코드"
                value={typed}
                onChange={(event) => setTyped(event.target.value)}
                placeholder="WQXR-KDPB"
                autoComplete="off"
                hint="여덟 글자. 대소문자와 하이픈은 상관없습니다."
              />
              <Button type="submit" variant="primary" disabled={normalizeUserCode(typed) === null}>
                확인
              </Button>
            </form>
          </>
        ) : request.isPending ? (
          <p className={styles.muted} role="status">
            요청을 찾는 중…
          </p>
        ) : request.isError ? (
          <InlineAlert tone="danger" title="요청을 불러오지 못했습니다" />
        ) : (
          request.data && (
            <>
              <p className={styles.lead}>터미널에 보이는 코드와 같은지 확인하세요.</p>
              <div className={styles.code} aria-label={`승인 코드 ${request.data.userCode}`}>
                {request.data.userCode}
              </div>
              <dl className={styles.facts}>
                <dt>기기</dt>
                <dd>{request.data.deviceName}</dd>
                <dt>클라이언트</dt>
                <dd>{request.data.client}</dd>
                <dt>요청</dt>
                <dd>{fullTime(request.data.createdAt)} · {fullTime(request.data.expiresAt)} 까지 승인</dd>
              </dl>
              <div>
                <p className={styles.scopeTitle}>이 기기가 할 수 있는 것</p>
                <ul className={styles.scopes}>
                  <li>프로젝트형 문제의 시작 저장소와 공개 테스트 받기</li>
                  <li>프로젝트형 문제 제출하고 결과 보기</li>
                  <li>초안 저장 (웹에서 이어 쓰기)</li>
                </ul>
                <p className={styles.muted}>비밀번호 변경, 계정 삭제, 다른 기기 승인은 할 수 없습니다.</p>
              </div>
              <InlineAlert tone="warning" title="내가 터미널에서 방금 연 요청이 아니면 승인하지 마세요">
                누군가 이 코드를 보내며 승인을 부탁했다면, 그 사람이 내 계정으로 제출하려는 것입니다.
              </InlineAlert>
              <div className={styles.actions}>
                <Button variant="secondary" onClick={() => void decide(false)} disabled={busy}>
                  거절
                </Button>
                <Button variant="primary" onClick={() => void decide(true)} loading={busy}>
                  승인
                </Button>
              </div>
            </>
          )
        )}
        <p className={styles.foot}>
          <Link href="/problems?kind=project">프로젝트형 문제로</Link>
        </p>
      </div>
    </main>
  )
}
