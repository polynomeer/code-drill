import { useQuery } from '@tanstack/react-query'
import { Check } from 'lucide-react'
import { useState } from 'react'
import type { ReactNode } from 'react'
import { Link } from 'wouter'
import { listProjects } from '../../api/client'
import { useSession } from '../../api/session'
import { Button, Chip, DifficultyBadge, EmptyState, InlineAlert, Skeleton } from '../../design'
import { DIFFICULTIES, DIFFICULTY_LABEL, LANGUAGE_LABEL } from '../../shared/types'
import { EMPTY_PROJECT_FILTER, filterProjects, projectHref, projectLanguages } from './projectFilter'
import type { ProjectFilter } from './projectFilter'
import styles from './ProblemsPage.module.css'

const toggle = <T,>(list: T[], value: T): T[] => (list.includes(value) ? list.filter((item) => item !== value) : [...list, value])

/**
 * 프로젝트형 탭 (docs/ui-overhaul.md §6.1) — 파일 여럿을 빌드해 숨은 테스트 스위트로 채점하는 문제.
 *
 * 둘러보는 사람에게도 보인다 (목록은 공개 경로다). 열면 작업 공간으로 가고, 작업 공간은 초안·제출이
 * 계정의 것이라 로그인을 거친다.
 */
export function ProjectList() {
  const session = useSession()
  const [filter, setFilter] = useState<ProjectFilter>(EMPTY_PROJECT_FILTER)
  const query = useQuery({
    queryKey: ['projects', session?.userId ?? null],
    queryFn: listProjects,
  })
  const all = query.data
  const items = all ? filterProjects(all, filter) : null
  const filtered = filter.languages.length > 0 || filter.difficulty.length > 0

  return (
    <div className={styles.main}>
      <p className={styles.kindLead}>
        시작 저장소의 파일 여럿을 고쳐 제출하면, 빌드한 뒤 숨은 테스트 스위트로 채점합니다. 증거는 실무 역량에 쌓입니다.
      </p>

      <section className={styles.filters} aria-label="프로젝트형 필터">
        <div className={styles.filterRow}>
          <Group label="언어">
            {(all ? projectLanguages(all) : []).map((language) => (
              <Chip
                key={language}
                pressed={filter.languages.includes(language)}
                onClick={() =>
                  setFilter((prev) => ({
                    ...prev,
                    languages: toggle(prev.languages, language),
                  }))
                }
              >
                {LANGUAGE_LABEL[language as keyof typeof LANGUAGE_LABEL] ?? language}
              </Chip>
            ))}
          </Group>
          <Group label="난이도">
            {DIFFICULTIES.map((level) => (
              <Chip
                key={level}
                pressed={filter.difficulty.includes(level)}
                onClick={() =>
                  setFilter((prev) => ({
                    ...prev,
                    difficulty: toggle(prev.difficulty, level),
                  }))
                }
              >
                {DIFFICULTY_LABEL[level]}
              </Chip>
            ))}
          </Group>
        </div>
      </section>

      {query.isError ? (
        <InlineAlert
          tone="danger"
          title="프로젝트형 문제를 불러오지 못했습니다"
          action={
            <Button size="dense" onClick={() => void query.refetch()}>
              다시 시도
            </Button>
          }
        />
      ) : !items ? (
        <div className={styles.skeleton} role="status" aria-busy="true">
          <span className="visually-hidden">프로젝트형 문제를 불러오는 중</span>
          {Array.from({ length: 5 }, (_, index) => (
            <div key={index} className={styles.skeletonRow}>
              <Skeleton width={`${50 + ((index * 13) % 30)}%`} />
              <Skeleton width={60} />
            </div>
          ))}
        </div>
      ) : items.length === 0 ? (
        <div className={styles.empty}>
          <EmptyState
            title={filtered ? '조건에 맞는 프로젝트형 문제가 없습니다' : '공개된 프로젝트형 문제가 없습니다'}
            action={filtered ? <Button onClick={() => setFilter(EMPTY_PROJECT_FILTER)}>필터 초기화</Button> : undefined}
          >
            {' '}
          </EmptyState>
        </div>
      ) : (
        <table className={styles.table}>
          <caption className="visually-hidden">프로젝트형 문제 {items.length}개</caption>
          <thead>
            <tr>
              {session && (
                <th scope="col" className={styles.colStatus}>
                  <span className="visually-hidden">상태</span>
                </th>
              )}
              <th scope="col" className={styles.colTitle}>
                제목
              </th>
              <th scope="col">언어</th>
              <th scope="col" className={styles.colDifficulty}>
                난이도
              </th>
            </tr>
          </thead>
          <tbody>
            {items.map((item) => (
              <tr key={item.id}>
                {session && (
                  <td className={styles.colStatus}>
                    {item.solved && <Check size={16} aria-label="완료" className={styles.solved} />}
                  </td>
                )}
                <td className={styles.colTitle}>
                  <Link href={projectHref(item.id, session !== null)} className={styles.rowLink}>
                    {item.title}
                  </Link>
                  <span className={styles.tags}>{item.summary}</span>
                </td>
                <td className={styles.colLanguage}>
                  {LANGUAGE_LABEL[item.language as keyof typeof LANGUAGE_LABEL] ?? item.language}
                </td>
                <td className={styles.colDifficulty}>
                  <DifficultyBadge level={item.difficulty} />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}

function Group({ label, children }: { label: string; children: ReactNode }) {
  return (
    <fieldset className={styles.group}>
      <legend className={styles.groupLabel}>{label}</legend>
      {children}
    </fieldset>
  )
}
