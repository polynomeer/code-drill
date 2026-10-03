import { useState } from 'react'
import { Link } from 'wouter'
import { openDuel } from '../../api/client'
import { Button, Dialog, InlineAlert, TextField } from '../../design'

/**
 * 지금 푸는 문제로 미니 대결 열기 (§8.4).
 *
 * 예전에는 대회 패널이 "고른 문제"를 알아서 거기서 열었다. 풀이가 자기 화면으로 나가면서
 * 고른 문제는 풀이 화면에만 있으므로, 여는 일도 여기로 왔다. 붙기(코드 입력)와 순위표는
 * 대회 패널에 그대로 있다.
 */
export function DuelDialog({ problemId, open, onClose }: { problemId: string; open: boolean; onClose: () => void }) {
  const [minutes, setMinutes] = useState(30)
  const [code, setCode] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const close = () => {
    setCode(null)
    setError(null)
    onClose()
  }

  const create = async () => {
    setBusy(true)
    setError(null)
    try {
      setCode((await openDuel(problemId, minutes)).joinCode)
    } catch (e) {
      setError(e instanceof Error ? e.message : '대결을 열지 못했습니다')
    } finally {
      setBusy(false)
    }
  }

  return (
    <Dialog
      open={open}
      onClose={close}
      title="미니 대결 열기"
      description="이 문제를 둘이 동시에 풀고 끝난 뒤 순위표를 봅니다. 참가하면 표시 이름이 공개됩니다."
      footer={
        code ? (
          <>
            <Link href="/" className="linklike">
              대회 패널에서 보기
            </Link>
            <Button variant="primary" onClick={close}>
              계속 풀기
            </Button>
          </>
        ) : (
          <>
            <Button variant="tertiary" onClick={close}>
              취소
            </Button>
            <Button variant="primary" loading={busy} onClick={() => void create()}>
              대결 열기
            </Button>
          </>
        )
      }
    >
      {error && <InlineAlert tone="danger" title={error} />}
      {code ? (
        <InlineAlert tone="success" title={`상대에게 알릴 코드: ${code}`}>
          상대가 이 코드로 붙는 순간 대결이 시작합니다.
        </InlineAlert>
      ) : (
        <TextField
          label="대결 시간 (분)"
          type="number"
          min={5}
          max={120}
          value={minutes}
          onChange={(event) => setMinutes(Number(event.target.value))}
          hint="5분에서 120분"
        />
      )}
    </Dialog>
  )
}
