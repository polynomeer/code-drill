import DOMPurify from 'dompurify'
import { marked } from 'marked'
import { useMemo } from 'react'
import type { Problem } from '../../shared/types'
import styles from './StatementView.module.css'

/**
 * 지문 (디자인 설계서 §5.3, §6.1 — 본문·예제·제약).
 *
 * 마크다운을 그려서 보여 준다. 예전에는 원문을 `<pre>` 에 그대로 넣어 `#`, `**`, ``` 가
 * 지문의 일부처럼 보였다.
 *
 * 지문은 저작자가 쓴 신뢰된 콘텐츠지만 그래도 정화한다 — 같은 렌더러를 질문 게시판 본문에도
 * 쓸 것이고, 그때 정화를 빠뜨리지 않으려면 처음부터 늘 거치는 편이 안전하다.
 */
export function Markdown({ source, className }: { source: string; className?: string }) {
  const html = useMemo(() => renderMarkdown(source), [source])
  return (
    <div
      className={[styles.prose, className].filter(Boolean).join(' ')}
      // 정화한 HTML 이다 (renderMarkdown)
      dangerouslySetInnerHTML={{ __html: html }}
    />
  )
}

export function renderMarkdown(source: string): string {
  const raw = marked.parse(source, { async: false, gfm: true, breaks: false })
  // 긴 코드 블록은 옆으로 흐른다. 키보드로도 스크롤하려면 포커스를 받아야 한다 (WCAG 2.1.1).
  return DOMPurify.sanitize(raw, { USE_PROFILES: { html: true } }).replaceAll('<pre>', '<pre tabindex="0">')
}

export function StatementView({ problem, body }: { problem: Problem; body: string }) {
  return (
    <article className={styles.statement} aria-label="문제">
      <dl className={styles.limits}>
        <div>
          <dt>시간 제한</dt>
          <dd>{formatMillis(problem.timeMillis)}</dd>
        </div>
        <div>
          <dt>메모리 제한</dt>
          <dd>{problem.memoryMb}MB</dd>
        </div>
      </dl>

      <Markdown source={body} />

      {problem.samples.length > 0 && (
        <section className={styles.section} aria-labelledby="samples-title">
          <h3 id="samples-title" className={styles.sectionTitle}>
            예제
          </h3>
          <table className={styles.samples}>
            <thead>
              <tr>
                <th scope="col">#</th>
                <th scope="col">입력 (인자)</th>
                <th scope="col">출력</th>
              </tr>
            </thead>
            <tbody>
              {problem.samples.map((sample, index) => (
                <tr key={sample.id}>
                  <td className={styles.index}>{index + 1}</td>
                  <td>
                    <code>{sample.args.map((arg) => JSON.stringify(arg)).join(', ')}</code>
                  </td>
                  <td>
                    <code>{JSON.stringify(sample.expected)}</code>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      )}

      {/* 채점 기준은 읽다가 필요할 때 연다. 지문보다 앞에 두면 문제보다 점수가 먼저 읽힌다. */}
      <details className={styles.grading}>
        <summary>채점 기준 · 그룹 {problem.groups.length}개</summary>
        <table className={styles.groups}>
          <thead>
            <tr>
              <th scope="col">그룹</th>
              <th scope="col">배점</th>
              <th scope="col">방식</th>
              <th scope="col">케이스</th>
            </tr>
          </thead>
          <tbody>
            {problem.groups.map((group) => (
              <tr key={group.id}>
                <td>
                  <code>{group.id}</code>
                </td>
                <td>{group.weight}점</td>
                <td>{group.aggregation === 'SUM' ? '부분 점수' : '전부 통과해야 만점'}</td>
                <td>{group.caseCount}</td>
              </tr>
            ))}
          </tbody>
        </table>
        <p className={styles.signature}>
          <code>{problem.signature}</code>
        </p>
      </details>
    </article>
  )
}

function formatMillis(ms: number): string {
  return ms % 1000 === 0 ? `${ms / 1000}초` : `${ms}ms`
}
