import { Download } from 'lucide-react'
import { projectKitUrl } from '../../api/client'
import { Button, Dialog } from '../../design'
import type { ProjectView } from '../../shared/types'
import styles from './LocalKitDialog.module.css'

/** 언어별 로컬 실행 명령 — 키트의 CODEDRILL.md 와 같다 (ProjectKit.readme) */
const RUN: Record<string, { command: string; note: string }> = {
  PYTHON: { command: 'python3 .codedrill/run.py', note: 'Python 3.10 이상' },
  JAVA: { command: 'java .codedrill/Run.java', note: 'JDK 17 이상 · IntelliJ 에서는 Gradle 의 codedrillTest' },
  KOTLIN: { command: 'gradle codedrillTest', note: 'IntelliJ 로 폴더를 열면 Gradle 창에 codedrillTest · Gradle 은 JDK 17~21 로' },
}

/**
 * 로컬에서 풀기 (feature-roadmap 11단계 이어서 — 1단계: ZIP 키트 + 웹으로 올리기).
 *
 * 받는 것은 시작 저장소·공개 테스트·채점기와 같은 하네스뿐이다. 숨은 테스트는 키트에 없다. 제출은 받은 폴더를
 * 작업 공간의 "폴더 가져오기"로 올려서 한다 — 키트 파일은 빼고 읽는다.
 */
export function LocalKitDialog({ project, open, onClose }: { project: ProjectView; open: boolean; onClose: () => void }) {
  const run = RUN[project.language] ?? RUN.PYTHON!
  return (
    <Dialog
      open={open}
      onClose={onClose}
      title="로컬에서 풀기"
      description="내 IDE에서 고치고 공개 테스트를 돌린 뒤, 여기로 올려 제출합니다. 판정은 웹에서 쓴 것과 같습니다."
      footer={
        <Button variant="tertiary" onClick={onClose}>
          닫기
        </Button>
      }
    >
      <ol className={styles.steps}>
        <li>
          <strong>키트 받기</strong>
          <p className={styles.muted}>
            시작 저장소 {Object.keys(project.files).length}파일 · 공개 테스트 · 채점기와 같은 하네스 · IDE 빌드 파일. 판 v{project.version}.
          </p>
          <Button variant="primary" icon={<Download size={16} />} onClick={() => window.location.assign(projectKitUrl(project.id))}>
            {project.id}-v{project.version}.zip 받기
          </Button>
        </li>
        <li>
          <strong>압축을 풀고 공개 테스트 돌리기</strong>
          <code className={styles.command}>{run.command}</code>
          <p className={styles.muted}>{run.note}. 자세한 것은 폴더 안의 CODEDRILL.md 에 있습니다.</p>
        </li>
        <li>
          <strong>올려서 제출하기</strong>
          <p className={styles.muted}>
            작업 공간의 <b>폴더 가져오기</b>로 그 폴더를 고르고 <b>제출</b>을 누릅니다. 키트 파일(.codedrill, CODEDRILL.md, 빌드 파일)과 빌드
            산출물은 빼고 읽습니다.
          </p>
        </li>
      </ol>
      <p className={styles.note}>
        숨은 테스트는 키트에 없습니다 — 로컬에서는 공개 테스트와 내가 더 쓴 테스트만 돕니다. 더 쓴 테스트는 제출하면 서버가 대표 오답 위에서도
        시험합니다.
      </p>
    </Dialog>
  )
}
