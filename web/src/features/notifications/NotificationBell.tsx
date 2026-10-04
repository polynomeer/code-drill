import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Award, Bell, CalendarClock, Gavel, Lightbulb, MessageSquareReply, ThumbsUp } from 'lucide-react'
import { useState } from 'react'
import type { ReactNode } from 'react'
import { Link } from 'wouter'
import { getNotifications, markNotificationsRead } from '../../api/client'
import type { NotificationFeed, NotificationKind } from '../../shared/types'
import { fullTime, relativeTime } from '../../shared/format'
import styles from './NotificationBell.module.css'

const ICON: Record<NotificationKind, ReactNode> = {
  CONTEST_STARTING: <CalendarClock size={16} />,
  RATING_CHANGED: <Award size={16} />,
  ANSWERED: <MessageSquareReply size={16} />,
  HELPFUL: <ThumbsUp size={16} />,
  TRANSFER_DONE: <Lightbulb size={16} />,
  SANCTION: <Gavel size={16} />,
  APPEAL_RESOLVED: <Gavel size={16} />,
}

/**
 * 알림 (docs/ui-overhaul.md §4 전역 헤더 "알림").
 *
 * 알리는 것은 **내가 보고 있지 않을 때 생긴 일**이다 — 대회 시작, 레이팅 변화, 내 질문의 답, 내 글의 도움됐다,
 * 전이 확인, 제재와 이의 판단. 판정 완료는 알리지 않는다; 풀이 화면이 이미 실시간으로 보여 준다.
 *
 * 목록을 열면 지금까지를 읽음으로 한다 — 하나씩 눌러 지우게 하면 알림이 할 일 목록이 된다. 안 읽은 수는
 * 숫자와 함께 단추 이름에도 실린다 (색 점만으로 말하지 않는다). 1분마다 다시 묻는다.
 */
export function NotificationBell() {
  const client = useQueryClient()
  const query = useQuery({ queryKey: ['me', 'notifications'], queryFn: getNotifications, refetchInterval: 60_000 })
  const read = useMutation({
    mutationFn: markNotificationsRead,
    onSuccess: () =>
      client.setQueryData<NotificationFeed>(['me', 'notifications'], (feed) =>
        feed ? { unread: 0, items: feed.items.map((item) => ({ ...item, unread: false })) } : feed,
      ),
  })
  const unread = query.data?.unread ?? 0
  const items = query.data?.items ?? []
  // 열 때 안 읽었던 것 — 읽음으로 보낸 뒤에도 이번에 연 동안은 무엇이 새것이었는지 보인다
  const [fresh, setFresh] = useState<Set<string>>(() => new Set())

  return (
    <>
      <button
        type="button"
        className={styles.bell}
        popoverTarget="notifications"
        aria-label={unread > 0 ? `알림, 안 읽은 것 ${unread}개` : '알림'}
      >
        <Bell size={18} aria-hidden="true" />
        {unread > 0 && (
          <span className={styles.badge} aria-hidden="true">
            {unread > 9 ? '9+' : unread}
          </span>
        )}
      </button>
      <div
        id="notifications"
        popover="auto"
        className={styles.panel}
        onToggle={(event) => {
          // 열었을 때 읽음으로 — 지금 보이는 "안 읽음" 표시는 이번에 열었을 때만 남아 무엇이 새것이었는지 보인다
          if ((event as unknown as ToggleEvent).newState !== 'open') return
          setFresh(new Set(items.filter((item) => item.unread).map((item) => item.id)))
          if (unread > 0 && !read.isPending) read.mutate()
        }}
      >
        <p className={styles.heading}>알림</p>
        {query.isError ? (
          <p className={styles.empty}>알림을 불러오지 못했습니다.</p>
        ) : items.length === 0 ? (
          <p className={styles.empty}>새 알림이 없습니다. 대회 시작, 답, 레이팅 변화가 생기면 여기 옵니다.</p>
        ) : (
          <ul className={styles.list}>
            {items.map((item) => (
              <li key={item.id}>
                <Link
                  href={item.link}
                  className={item.unread || fresh.has(item.id) ? `${styles.item} ${styles.unread}` : styles.item}
                  onClick={() => document.getElementById('notifications')?.hidePopover?.()}
                >
                  <span className={styles.icon} aria-hidden="true">
                    {ICON[item.kind]}
                  </span>
                  <span className={styles.text}>
                    <span className={styles.title}>
                      {(item.unread || fresh.has(item.id)) && <span className="visually-hidden">새 알림: </span>}
                      {item.title}
                    </span>
                    {item.body && <span className={styles.body}>{item.body}</span>}
                    <time dateTime={item.at} title={fullTime(item.at)} className={styles.time}>
                      {relativeTime(item.at)}
                    </time>
                  </span>
                </Link>
              </li>
            ))}
          </ul>
        )}
      </div>
    </>
  )
}
