import { useState } from 'react'
import { Button, Dialog, Kbd, Select } from '../../design'
import { FONT_SIZES, updateEditorSettings, useEditorSettings } from './editorSettings'
import { EDITOR_THEMES, isEditorThemeChoice } from './editorThemes'
import { ALT, MOD } from './useShortcuts'
import styles from './SolvePage.module.css'

/**
 * 에디터 설정과 단축키 안내 (디자인 설계서 §11.2 Editor, §6.4). 테마 목록은 editorThemes.ts.
 *
 * 코드 초기화는 여기 안쪽에 둔다 — 위험 행동은 메뉴 안 + 확인 대화상자다 (§6.2 Danger).
 * 툴바에 바로 두면 실행 옆에서 잘못 눌린다.
 */
export function EditorSettingsDialog({
  open,
  onClose,
  onReset,
}: {
  open: boolean
  onClose: () => void
  onReset: () => void
}) {
  const settings = useEditorSettings()
  const [confirming, setConfirming] = useState(false)

  return (
    <>
      <Dialog open={open && !confirming} onClose={onClose} title="에디터 설정" description="이 기기에 저장됩니다.">
        <div className={styles.settingsGrid}>
          <Select
            label="테마"
            value={settings.theme}
            onChange={(event) => {
              const value = event.target.value
              if (isEditorThemeChoice(value)) updateEditorSettings({ theme: value })
            }}
          >
            <option value="auto">앱 테마 따라가기 (픽셀)</option>
            {EDITOR_THEMES.map((theme) => (
              <option key={theme.id} value={theme.id}>
                {theme.label}
              </option>
            ))}
          </Select>
          <Select
            label="글꼴 크기"
            value={settings.fontSize}
            onChange={(event) => updateEditorSettings({ fontSize: Number(event.target.value) })}
          >
            {FONT_SIZES.map((size) => (
              <option key={size} value={size}>
                {size}px
              </option>
            ))}
          </Select>
          <Select
            label="들여쓰기"
            value={settings.tabSize}
            onChange={(event) => updateEditorSettings({ tabSize: Number(event.target.value) === 2 ? 2 : 4 })}
          >
            <option value={2}>공백 2칸</option>
            <option value={4}>공백 4칸</option>
          </Select>
          <label className={styles.check}>
            <input
              type="checkbox"
              checked={settings.minimap}
              onChange={(event) => updateEditorSettings({ minimap: event.target.checked })}
            />
            미니맵
          </label>
          <label className={styles.check}>
            <input
              type="checkbox"
              checked={settings.wordWrap}
              onChange={(event) => updateEditorSettings({ wordWrap: event.target.checked })}
            />
            긴 줄 접기
          </label>
        </div>

        <h3 className={styles.settingsHeading}>단축키</h3>
        <dl className={styles.shortcuts}>
          <Shortcut keys={[MOD, '↵']}>실행</Shortcut>
          <Shortcut keys={[MOD, '⇧', '↵']}>제출</Shortcut>
          <Shortcut keys={[MOD, 'J']}>결과 창 열기·접기</Shortcut>
          <Shortcut keys={[MOD, '\\']}>문제 창 열기·접기</Shortcut>
          <Shortcut keys={[ALT, '1']}>문제로 이동</Shortcut>
          <Shortcut keys={[ALT, '2']}>에디터로 이동</Shortcut>
        </dl>

        <h3 className={styles.settingsHeading}>코드</h3>
        <div className={styles.settingsDanger}>
          <p>지금 언어의 코드를 시작 코드로 되돌립니다.</p>
          <Button variant="danger" size="dense" onClick={() => setConfirming(true)}>
            코드 초기화
          </Button>
        </div>
      </Dialog>

      <Dialog
        open={open && confirming}
        onClose={() => setConfirming(false)}
        title="코드를 초기화할까요?"
        description="지금 작성한 코드가 시작 코드로 바뀌고, 자동 저장이 초안을 덮어씁니다. 되돌릴 수 없습니다."
        footer={
          <>
            <Button variant="tertiary" onClick={() => setConfirming(false)}>
              취소
            </Button>
            <Button
              variant="danger"
              onClick={() => {
                onReset()
                setConfirming(false)
                onClose()
              }}
            >
              초기화
            </Button>
          </>
        }
      />
    </>
  )
}

function Shortcut({ keys, children }: { keys: string[]; children: string }) {
  return (
    <div className={styles.shortcut}>
      <dt>{children}</dt>
      <dd>
        {keys.map((key) => (
          <Kbd key={key}>{key}</Kbd>
        ))}
      </dd>
    </div>
  )
}
