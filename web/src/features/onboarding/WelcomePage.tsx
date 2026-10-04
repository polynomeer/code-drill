import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import type { FormEvent, ReactNode } from 'react'
import { Link, useLocation } from 'wouter'
import { saveOnboarding } from '../../api/client'
import { safeNext } from '../../app/navigation'
import { Button, InlineAlert } from '../../design'
import { LANGUAGE_LABEL } from '../../shared/types'
import type { LearnerProfile } from '../../shared/types'
import styles from './WelcomePage.module.css'

const GOALS: { value: LearnerProfile['dailyGoal']; label: string; hint: string }[] = [
  { value: 1, label: '하루 한 문제', hint: '가볍게 꾸준히' },
  { value: 2, label: '하루 두 문제', hint: '복습과 새 문제를 함께' },
  { value: 3, label: '하루 세 문제', hint: '바짝 준비하는 중' },
]

const LEVELS: { value: LearnerProfile['level']; label: string; hint: string }[] = [
  { value: 'BEGINNER', label: '이제 시작', hint: '배열·반복문으로 푸는 문제부터' },
  { value: 'INTERMEDIATE', label: '기본은 한다', hint: '정렬·해시·투 포인터가 익숙하다' },
  { value: 'ADVANCED', label: '꽤 푼다', hint: '그래프·DP 를 스스로 고른다' },
]

const LANGUAGES = Object.keys(LANGUAGE_LABEL) as LearnerProfile['language'][]

/**
 * 가입 직후 세 문항 `/welcome` (docs/ui-overhaul.md §6.9).
 *
 * 묻는 것은 처방이 실제로 쓰는 것뿐이다 — 하루 목표는 처방 칸 수, 주 언어는 풀이 화면의 기본 언어,
 * 수준은 첫 진단 문제의 난이도. 답하면 바로 진단이 든 첫 처방으로 간다: 가입 → 진단 → 첫 처방이 끊기지
 * 않게. 건너뛸 수 있다 — 관문이 아니다. 나중에 오늘의 훈련에서 다시 열 수 있다.
 *
 * 수준은 자기 보고라 역량 지도에는 들어가지 않는다고 미리 말한다.
 */
export function WelcomePage() {
  const [, navigate] = useLocation()
  const client = useQueryClient()
  const next = safeNext(new URLSearchParams(window.location.search).get('next'))
  const [dailyGoal, setGoal] = useState<LearnerProfile['dailyGoal']>(2)
  const [language, setLanguage] = useState<LearnerProfile['language']>('PYTHON')
  const [level, setLevel] = useState<LearnerProfile['level']>('BEGINNER')

  const save = useMutation({
    mutationFn: () => saveOnboarding({ dailyGoal, language, level }),
    onSuccess: (prescription) => {
      client.setQueryData(['me', 'prescription'], prescription)
      client.setQueryData(['me', 'onboarding'], { dailyGoal, language, level })
      // 원래 가려던 곳(풀던 문제)이 있으면 거기로, 아니면 첫 처방으로
      navigate(next === '/' ? '/training' : next, { replace: true })
    },
  })

  const submit = (event: FormEvent) => {
    event.preventDefault()
    save.mutate()
  }

  return (
    <div className={styles.page}>
      <h1 className={styles.title}>세 가지만 묻겠습니다</h1>
      <p className={styles.lead}>답에 맞춰 첫 진단 문제를 고릅니다. 맞히든 틀리든 기록이 되고, 다음 처방이 그 근거에서 나옵니다.</p>

      <form className={styles.form} onSubmit={submit}>
        <Choice legend="하루 목표" hint="오늘의 훈련에 권하는 문제 수가 됩니다.">
          {GOALS.map((goal) => (
            <Option key={goal.value} name="goal" checked={dailyGoal === goal.value} onChange={() => setGoal(goal.value)} label={goal.label} hint={goal.hint} />
          ))}
        </Choice>

        <Choice legend="주 언어" hint="풀이 화면이 이 언어로 시작합니다. 언제든 바꿀 수 있습니다.">
          {LANGUAGES.map((value) => (
            <Option key={value} name="language" checked={language === value} onChange={() => setLanguage(value)} label={LANGUAGE_LABEL[value]} />
          ))}
        </Choice>

        <Choice legend="지금 수준" hint="첫 진단 문제의 난이도만 정합니다. 역량 지도에는 실행 증거만 들어갑니다.">
          {LEVELS.map((item) => (
            <Option key={item.value} name="level" checked={level === item.value} onChange={() => setLevel(item.value)} label={item.label} hint={item.hint} />
          ))}
        </Choice>

        {save.error instanceof Error && <InlineAlert tone="danger">{save.error.message}</InlineAlert>}

        <div className={styles.actions}>
          <Button type="submit" variant="primary" loading={save.isPending}>
            진단 시작
          </Button>
          <Link href={next === '/' ? '/problems' : next} className={styles.skip}>
            나중에 — 바로 둘러보기
          </Link>
        </div>
      </form>
    </div>
  )
}

function Choice({ legend, hint, children }: { legend: string; hint: string; children: ReactNode }) {
  return (
    <fieldset className={styles.fieldset}>
      <legend className={styles.legend}>{legend}</legend>
      <p className={styles.hint}>{hint}</p>
      <div className={styles.options}>{children}</div>
    </fieldset>
  )
}

function Option({ name, checked, onChange, label, hint }: { name: string; checked: boolean; onChange: () => void; label: string; hint?: string }) {
  return (
    <label className={checked ? `${styles.option} ${styles.optionOn}` : styles.option}>
      <input type="radio" name={name} checked={checked} onChange={onChange} className={styles.radio} />
      <span className={styles.optionLabel}>{label}</span>
      {hint && <span className={styles.optionHint}>{hint}</span>}
    </label>
  )
}
