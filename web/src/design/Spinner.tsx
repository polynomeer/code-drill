import styles from './Feedback.module.css'

/** 진행 표시. 장식이다 — 무엇이 진행 중인지는 옆의 글이 말한다 (UI 디자인 문서 §9.2). */
export function Spinner({ size = 16 }: { size?: number }) {
  return <span className={styles.spinner} style={{ width: size, height: size }} aria-hidden="true" />
}

/**
 * 자리 표시. 불러올 내용과 같은 모양으로 그려 레이아웃이 튀지 않게 한다.
 * 조각마다 읽히지 않도록 장식으로 두고, 감싸는 쪽이 role="status" 와 숨은 "불러오는 중" 글을 단다.
 */
export function Skeleton({
  width = '100%',
  height = 14,
  radius = 'var(--radius-sm)',
}: {
  width?: number | string
  height?: number | string
  radius?: string
}) {
  return <span className={styles.skeleton} style={{ width, height, borderRadius: radius }} aria-hidden="true" />
}
