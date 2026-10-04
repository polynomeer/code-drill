import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { Button, InlineAlert, Skeleton, TextField } from '../../design'
import { adminApi } from './adminApi'
import { Clear, Id, When } from './common'
import styles from './AdminPage.module.css'

/** 감사 행위의 사람 말. 모르는 것은 enum 이름 그대로 — 새 행위가 생겨도 줄이 사라지지 않게. */
const ACTION_LABEL: Record<string, string> = {
  PROBLEM_VERSION_REGISTERED: '문제 버전 등록',
  PROBLEM_VERSION_REVALIDATED: '문제 버전 재검증',
  PROBLEM_PUBLISHED: '문제 공개',
  PROBLEM_ARCHIVED: '문제 보관',
  REJUDGE_REQUESTED: '재채점 요청',
  REJUDGE_APPROVED: '재채점 승인',
  REJUDGE_REJECTED: '재채점 반려',
  REJUDGE_DISPATCHED: '재채점 실행',
  JUDGEMENT_REVISED: '판정 변경',
  ARENA_DONATION_APPROVED: '기부 세움',
  ARENA_DONATION_REJECTED: '기부 반려',
  ARENA_DONATION_RETIRED: '과녁 내림',
  ARENA_REPORT_DISMISSED: '과녁 신고 기각',
  DISCUSSION_POST_HIDDEN: '글 내림',
  DISCUSSION_REPORT_DISMISSED: '글 신고 기각',
  SIMILARITY_CONFIRMED: '유사도 확인',
  SIMILARITY_DISMISSED: '유사도 기각',
  SANCTION_ISSUED: '제재 발부',
  SANCTION_LIFTED: '제재 해제',
  APPEAL_UPHELD: '이의 — 유지',
  APPEAL_LIFTED: '이의 — 해제',
  CONTEST_CREATED: '대회 생성',
  CONTEST_PUBLISHED: '대회 공개',
  ADMIN_ROLE_GRANT_REQUESTED: '역할 요청',
  ADMIN_ROLE_GRANT_REJECTED: '역할 요청 반려',
  ADMIN_ROLE_GRANTED: '역할 부여',
  ADMIN_ROLE_REVOKED: '역할 회수',
  ADMIN_ACCESS_DENIED: '접근 거부',
}

/**
 * 감사 로그 (§13.3). SECURITY_ADMIN, 읽기 전용.
 *
 * 대상(subject)으로 거른다 — 문제 id, 제출 id, 사용자 id 하나를 넣으면 그 대상에 일어난 결정이 시간순으로
 * 나온다. 거부 기록도 섞여 나온다; 성공만 보면 공격의 앞부분이 비어 보인다.
 */
export function AuditView() {
  const [subject, setSubject] = useState<string | null>(null)
  const [draft, setDraft] = useState('')
  const query = useQuery({ queryKey: ['admin', 'audit', subject], queryFn: () => adminApi.audit(subject) })

  return (
    <>
      <form
        className={styles.formRow}
        onSubmit={(event) => {
          event.preventDefault()
          setSubject(draft.trim() || null)
        }}
      >
        <TextField label="대상" hint="문제 id · 제출 id · 사용자 id. 비우면 최근 100건" value={draft} onChange={(event) => setDraft(event.target.value)} className={styles.mono} spellCheck={false} />
        <div className={styles.formButton}>
          <Button type="submit">거르기</Button>
        </div>
      </form>
      {query.isError ? (
        <InlineAlert tone="danger">{query.error.message}</InlineAlert>
      ) : !query.data ? (
        <Skeleton height={240} />
      ) : query.data.length === 0 ? (
        <Clear>기록이 없습니다.</Clear>
      ) : (
        <div className={styles.tableWrap} tabIndex={0} role="region" aria-label="감사 로그 표">
          <table className={styles.table}>
            <thead>
              <tr>
                <th scope="col">시각</th>
                <th scope="col">행위</th>
                <th scope="col">대상</th>
                <th scope="col">행위자</th>
                <th scope="col">내용</th>
              </tr>
            </thead>
            <tbody>
              {query.data.map((entry) => (
                <tr key={entry.id} className={entry.action === 'ADMIN_ACCESS_DENIED' ? styles.denied : undefined}>
                  <td className={styles.num}>
                    <When iso={entry.createdAt} />
                  </td>
                  <td>{ACTION_LABEL[entry.action] ?? entry.action}</td>
                  <td>
                    <button type="button" className="linklike" onClick={() => { setDraft(entry.subject); setSubject(entry.subject) }}>
                      <code>{entry.subject}</code>
                    </button>
                  </td>
                  <td>
                    <Id value={entry.actor} />
                  </td>
                  <td>
                    <code className={styles.detail}>{entry.detail}</code>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  )
}
