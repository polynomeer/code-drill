import { Code, Play, RotateCcw, Send, Settings, Trophy } from 'lucide-react'
import { useState } from 'react'
import type { ReactNode } from 'react'
import { DIFFICULTIES } from '../shared/types'
import type { SubmissionStatus, Verdict } from '../shared/types'
import {
  Badge,
  Button,
  Chip,
  Dialog,
  DifficultyBadge,
  EmptyState,
  IconButton,
  InlineAlert,
  JudgeStatusBadge,
  Kbd,
  Panel,
  ProgressBar,
  SearchField,
  Select,
  Skeleton,
  Spinner,
  Tabs,
  TextField,
  Textarea,
  VerdictBadge,
  getThemePreference,
  setThemePreference,
  useToast,
} from '.'
import type { ThemePreference } from '.'
import styles from './DesignPage.module.css'

/**
 * 컴포넌트 카탈로그 (docs/ui-overhaul.md §5.2).
 *
 * 컴포넌트마다 **모든 상태**를 한 화면에 늘어놓는다. 새 상태를 만들면 여기에도 둔다 —
 * 이 화면이 Playwright 접근성 검사와 시각 회귀의 기준이다 (e2e/design.spec.ts).
 * 개발 빌드에만 있다 (app/router.tsx).
 */
const VERDICTS: Verdict[] = [
  'ACCEPTED',
  'WRONG_ANSWER',
  'COMPILE_ERROR',
  'RUNTIME_ERROR',
  'TIME_LIMIT',
  'MEMORY_LIMIT',
  'OUTPUT_LIMIT',
  'SYSTEM_ERROR',
]
const STATUSES: SubmissionStatus[] = ['QUEUED', 'COMPILING', 'RUNNING', 'AGGREGATING']

const COLOR_TOKENS = [
  'bg-app',
  'bg-surface',
  'bg-subtle',
  'bg-inset',
  'bg-editor',
  'text-primary',
  'text-muted',
  'border-default',
  'border-strong',
  'brand',
  'trace',
  'success',
  'warning',
  'danger',
  'system',
]

export function DesignPage() {
  const [theme, setTheme] = useState<ThemePreference>(getThemePreference)
  const [tab, setTab] = useState<'statement' | 'editorial' | 'solutions' | 'submissions'>('statement')
  const [chips, setChips] = useState<string[]>(['dp'])
  const [dialog, setDialog] = useState(false)
  const toast = useToast()

  const toggleChip = (value: string) =>
    setChips((all) => (all.includes(value) ? all.filter((v) => v !== value) : [...all, value]))

  return (
    <main id="main" className={styles.page}>
      <header className={styles.header}>
        <div>
          <h1 className={styles.title}>디자인 시스템</h1>
          <p className={styles.lead}>web/src/design — 토큰과 컴포넌트의 모든 상태</p>
        </div>
        <Select
          label="테마"
          hideLabel
          value={theme}
          onChange={(event) => {
            const value = event.target.value as ThemePreference
            setThemePreference(value)
            setTheme(value)
          }}
        >
          <option value="system">시스템 설정</option>
          <option value="light">라이트</option>
          <option value="dark">다크</option>
        </Select>
      </header>

      <Section title="색 토큰">
        <ul className={styles.swatches}>
          {COLOR_TOKENS.map((token) => (
            <li key={token} className={styles.swatch}>
              <span className={styles.swatchColor} style={{ background: `var(--color-${token})` }} />
              <code>--color-{token}</code>
            </li>
          ))}
        </ul>
      </Section>

      <Section title="타이포그래피">
        <p style={{ font: 'var(--text-display)', margin: 0 }}>Display 맞았습니다</p>
        <p style={{ font: 'var(--text-h1)', margin: 0 }}>H1 구간 더하기 뒤의 배열</p>
        <p style={{ font: 'var(--text-h2)', margin: 0 }}>H2 테스트 결과</p>
        <p style={{ font: 'var(--text-h3)', margin: 0 }}>H3 패널 제목</p>
        <p style={{ font: 'var(--text-reading)', margin: 0 }}>
          Reading 길이 n 의 0 배열에 갱신을 차례로 적용한다. 지문은 길게 읽으므로 한 단계 크다.
        </p>
        <p style={{ font: 'var(--text-body)', margin: 0 }}>Body 표와 패널의 기본 글. 0123456789</p>
        <p style={{ font: 'var(--text-label)', margin: 0 }}>Label 언어 · 저장됨</p>
        <p style={{ font: 'var(--text-caption)', margin: 0, color: 'var(--color-text-muted)' }}>Caption 2000ms · 256MB</p>
        <pre style={{ font: 'var(--text-code)', margin: 0, whiteSpace: 'pre-wrap', overflowWrap: 'anywhere' }}>{'fun applyRangeUpdates(n: Int, updates: IntArray): IntArray'}</pre>
      </Section>

      <Section title="Button">
        <Row>
          <Button variant="primary" icon={<Send size={16} />}>
            제출
          </Button>
          <Button icon={<Play size={16} />}>실행</Button>
          <Button variant="tertiary" icon={<RotateCcw size={16} />}>
            초기화
          </Button>
          <Button variant="danger">계정 삭제</Button>
        </Row>
        <Row>
          <Button variant="primary" loading>
            제출 중
          </Button>
          <Button variant="primary" disabled>
            제출
          </Button>
          <Button disabled>실행</Button>
          <Button size="dense">작은 버튼</Button>
          <IconButton label="에디터 설정" icon={<Settings size={18} />} />
          <IconButton label="코드 보기" icon={<Code size={16} />} size="dense" />
        </Row>
      </Section>

      <Section title="입력">
        <div className={styles.grid}>
          <TextField label="표시 이름" placeholder="비워 두면 이메일 앞부분" />
          <TextField label="비밀번호" type="password" hint="10자 이상" />
          <TextField label="이메일" defaultValue="not-an-email" error="이메일 형식이 아닙니다" />
          <TextField label="비활성" disabled defaultValue="고칠 수 없음" />
          <Select label="언어" defaultValue="KOTLIN">
            <option value="KOTLIN">Kotlin</option>
            <option value="JAVA">Java</option>
            <option value="PYTHON">Python</option>
          </Select>
          <SearchField label="문제 검색" placeholder="제목으로 검색" />
        </div>
        <Textarea label="커스텀 입력" placeholder="[5, [1,3,2]]" hint="한 줄에 한 케이스" />
      </Section>

      <Section title="표시">
        <Row>
          {DIFFICULTIES.map((level) => (
            <DifficultyBadge key={level} level={level} />
          ))}
        </Row>
        <Row>
          {VERDICTS.map((verdict) => (
            <VerdictBadge key={verdict} verdict={verdict} />
          ))}
        </Row>
        <Row>
          {STATUSES.map((status) => (
            <JudgeStatusBadge key={status} status={status} />
          ))}
        </Row>
        <Row>
          <Badge>neutral</Badge>
          <Badge tone="brand">brand</Badge>
          <Badge tone="trace" icon={<Trophy size={14} />}>
            trace
          </Badge>
          <Badge tone="success">success</Badge>
          <Badge tone="warning">warning</Badge>
          <Badge tone="danger">danger</Badge>
          <Badge tone="system">system</Badge>
        </Row>
        <Row>
          {['array', 'dp', 'graph', 'two-pointers'].map((tag, index) => (
            <Chip key={tag} pressed={chips.includes(tag)} count={[68, 29, 31, 19][index]} onClick={() => toggleChip(tag)}>
              {tag}
            </Chip>
          ))}
          <Chip pressed={false} disabled>
            비활성
          </Chip>
        </Row>
        <Row>
          <span>
            실행 <Kbd>⌘</Kbd> <Kbd>↵</Kbd>
          </span>
          <span>
            제출 <Kbd>⌘</Kbd> <Kbd>⇧</Kbd> <Kbd>↵</Kbd>
          </span>
        </Row>
        <div className={styles.grid}>
          <ProgressBar label="boundary 그룹 점수" value={20} max={20} tone="success" />
          <ProgressBar label="performance 그룹 점수" value={14} max={40} tone="warning" />
          <ProgressBar label="주간 목표" value={3} max={5} />
        </div>
      </Section>

      <Section title="피드백">
        <InlineAlert tone="info" title="최초 분기점을 찾고 있습니다">
          동일한 최소 반례에서 두 실행을 비교합니다.
        </InlineAlert>
        <InlineAlert tone="success" title="저장됨" />
        <InlineAlert tone="warning" title="이 문제는 새 버전이 나왔습니다" action={<Button size="dense">새 버전 보기</Button>}>
          이미 연 풀이는 이전 버전으로 채점됩니다.
        </InlineAlert>
        <InlineAlert tone="danger" title="변경 내용을 저장하지 못했습니다">
          인터넷 연결 후 다시 시도합니다.
        </InlineAlert>
        <Row>
          <Button onClick={() => toast.show('코드를 복사했습니다', 'success')}>성공 토스트</Button>
          <Button onClick={() => toast.show('연결이 잠시 끊겼습니다', 'warning')}>경고 토스트</Button>
          <Button onClick={() => toast.show('저장하지 못했습니다', 'danger')}>오류 토스트 (남음)</Button>
          <Spinner />
        </Row>
        <div role="status" aria-busy="true" className={styles.skeletons}>
          <span className="visually-hidden">불러오는 중</span>
          <Skeleton width="40%" height={18} />
          <Skeleton />
          <Skeleton width="85%" />
        </div>
        <Panel>
          <EmptyState
            title="조건에 맞는 문제가 없습니다"
            action={<Button onClick={() => setChips([])}>필터 초기화</Button>}
          >
            난이도: 최상 · 태그: dp, graph
          </EmptyState>
        </Panel>
      </Section>

      <Section title="구조">
        <Panel title="패널 제목" actions={<Button size="dense">행동</Button>}>
          <p style={{ margin: 0 }}>패널 본문. 경계는 장식이라 연하고, 입력 경계는 진하다.</p>
        </Panel>
        <Panel>
          <Tabs
            label="문제 pane"
            value={tab}
            onChange={setTab}
            items={[
              { key: 'statement', label: '문제' },
              { key: 'editorial', label: '해설' },
              { key: 'solutions', label: '풀이', disabled: true },
              { key: 'submissions', label: '제출' },
            ]}
          >
            <p style={{ margin: 0 }}>지금 탭: {tab}. ←/→ 로 옮기고 Home/End 로 끝으로 간다.</p>
          </Tabs>
        </Panel>
        <Row>
          <Button onClick={() => setDialog(true)}>대화상자 열기</Button>
        </Row>
        <Dialog
          open={dialog}
          onClose={() => setDialog(false)}
          title="코드를 초기화할까요?"
          description="지금 작성한 코드가 시작 코드로 바뀝니다. 되돌릴 수 없습니다."
          footer={
            <>
              <Button variant="tertiary" onClick={() => setDialog(false)}>
                취소
              </Button>
              <Button variant="danger" onClick={() => setDialog(false)}>
                초기화
              </Button>
            </>
          }
        />
      </Section>
    </main>
  )
}

function Section({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className={styles.section} aria-label={title}>
      <h2 className={styles.sectionTitle}>{title}</h2>
      <div className={styles.sectionBody}>{children}</div>
    </section>
  )
}

function Row({ children }: { children: ReactNode }) {
  return <div className={styles.row}>{children}</div>
}
