import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useId, useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'wouter'
import { getProfileSettings, updateProfileSettings } from '../../api/client'
import { Button, InlineAlert, Skeleton, TextField } from '../../design'
import styles from './ProfileSettingsForm.module.css'

/**
 * 공개 프로필 설정 — 핸들과 공개 여부 (docs/ui-overhaul.md §6.7).
 *
 * **공개는 기본이 아니다.** 체크 상자는 꺼진 채로 시작하고, 켜기 전에 무엇이 보이는지(푼 문제 수·1년
 * 활동·연속 일수·레이팅·공개한 풀이·기여 등급)와 무엇이 보이지 않는지(역량 수준·코드·제출 내용)를
 * 같은 자리에서 말한다. 무엇을 내거는지 모르고 켠 동의는 동의가 아니다.
 */
export function ProfileSettingsForm({ onSaved }: { onSaved?: (handle: string | null) => void }) {
  const client = useQueryClient()
  const query = useQuery({ queryKey: ['me', 'profile-settings'], queryFn: getProfileSettings })
  const [handle, setHandle] = useState('')
  const [isPublic, setPublic] = useState(false)
  const checkId = useId()

  useEffect(() => {
    if (!query.data) return
    setHandle(query.data.handle ?? '')
    setPublic(query.data.public)
  }, [query.data])

  const save = useMutation({
    mutationFn: () => updateProfileSettings({ handle: handle.trim() || null, public: isPublic }),
    onSuccess: (saved) => {
      client.setQueryData(['me', 'profile-settings'], saved)
      void client.invalidateQueries({ queryKey: ['profile'] })
      onSaved?.(saved.handle)
    },
  })

  if (query.isError) return <InlineAlert tone="danger">공개 프로필 설정을 불러오지 못했습니다.</InlineAlert>
  if (!query.data) return <Skeleton height={120} />

  const submit = (event: FormEvent) => {
    event.preventDefault()
    save.mutate()
  }

  return (
    <form className={styles.settings} onSubmit={submit}>
      <TextField
        label="핸들"
        hint="프로필 주소가 됩니다 — /u/핸들. 영문 소문자·숫자·-·_ 로 3~20자."
        value={handle}
        onChange={(event) => setHandle(event.target.value)}
        autoComplete="off"
        spellCheck={false}
        error={save.error instanceof Error ? save.error.message : undefined}
      />
      <div className={styles.check}>
        <input
          id={checkId}
          type="checkbox"
          checked={isPublic}
          onChange={(event) => setPublic(event.target.checked)}
          disabled={handle.trim().length === 0}
        />
        <label htmlFor={checkId}>
          <strong>프로필을 공개합니다</strong>
          <span>
            링크를 아는 누구나 푼 문제 수, 1년 활동, 연속 일수, 레이팅, 공개한 풀이, 기여 등급을 봅니다. 역량 수준과
            코드·제출 내용은 보이지 않습니다.
          </span>
        </label>
      </div>
      <div className={styles.settingsActions}>
        <Button type="submit" variant="primary" size="dense" loading={save.isPending}>
          저장
        </Button>
        {save.isSuccess && save.data.handle && (
          <Link href={`/u/${save.data.handle}`} className="linklike">
            {save.data.public ? '공개 프로필 보기' : '내 프로필 미리 보기'}
          </Link>
        )}
      </div>
    </form>
  )
}
