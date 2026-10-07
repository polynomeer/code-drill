/**
 * 에디터 테마 (디자인 설계서 §11.2 Editor, docs/ui-overhaul.md §2 "에디터 테마").
 *
 * **Monaco 를 import 하지 않는 순수 데이터다.** 에디터 둘레(틀·리플레이 코드 창)의 색을 맞추는 일은 첫 화면에서
 * 하고, Monaco 에 테마를 등록하는 일은 에디터 청크(monacoSetup.ts)가 한다. 여기서 Monaco 를 끌어오면 첫 화면이
 * 에디터를 통째로 받게 된다.
 *
 * 기본은 `auto` — 앱이 다크면 픽셀 어둡게, 라이트면 픽셀 밝게. 대비는 editorThemes.test.ts 가 잰다.
 */
export type EditorThemeId = 'pixel-dark' | 'pixel-light' | 'forest' | 'paper' | 'vs-dark' | 'vs' | 'hc-black'
export type EditorThemeChoice = 'auto' | EditorThemeId

export interface EditorTheme {
  id: EditorThemeId
  label: string
  /** Monaco 의 바탕 테마 */
  base: 'vs' | 'vs-dark' | 'hc-black'
  /** 우리가 정의하는가. 아니면 Monaco 내장이다 */
  custom: boolean
  colors: {
    background: string
    foreground: string
    /** 에디터 둘레의 보조 글자 (불러오는 중, 리플레이 줄 번호) */
    muted: string
    comment: string
    keyword: string
    string: string
    number: string
    type: string
    lineNumber: string
    activeLineNumber: string
    lineHighlight: string
    selection: string
    cursor: string
  }
}

export const EDITOR_THEMES: EditorTheme[] = [
  {
    id: 'pixel-dark',
    label: '픽셀 · 어둡게',
    base: 'vs-dark',
    custom: true,
    colors: {
      background: '#0b0c1c',
      foreground: '#e6e3ff',
      muted: '#b3b8dc',
      comment: '#8a90c4',
      keyword: '#ffcc4d',
      string: '#6ee07a',
      number: '#ff9fd0',
      type: '#4de1ff',
      lineNumber: '#6970a8',
      activeLineNumber: '#ffcc4d',
      lineHighlight: '#1b1e3d',
      selection: '#3b4080',
      cursor: '#ffcc4d',
    },
  },
  {
    id: 'pixel-light',
    label: '픽셀 · 밝게',
    base: 'vs',
    custom: true,
    colors: {
      background: '#f7f8fd',
      foreground: '#12142b',
      muted: '#4c5180',
      comment: '#5d6290',
      keyword: '#835800',
      string: '#1d6f37',
      number: '#a3266a',
      type: '#0a6a83',
      lineNumber: '#7d82ad',
      activeLineNumber: '#835800',
      lineHighlight: '#eceef8',
      selection: '#c9cde6',
      cursor: '#835800',
    },
  },
  {
    id: 'forest',
    label: '숲',
    base: 'vs-dark',
    custom: true,
    colors: {
      background: '#0f1a14',
      foreground: '#dfeadf',
      muted: '#a9bfae',
      comment: '#86a08e',
      keyword: '#9ad17a',
      string: '#e6c27a',
      number: '#f2a07b',
      type: '#7fc8c0',
      lineNumber: '#6a8a74',
      activeLineNumber: '#9ad17a',
      lineHighlight: '#18261d',
      selection: '#2a4232',
      cursor: '#9ad17a',
    },
  },
  {
    id: 'paper',
    label: '종이',
    base: 'vs',
    custom: true,
    colors: {
      background: '#fbf6ea',
      foreground: '#2b2a26',
      muted: '#5e5a4e',
      comment: '#6e6a5e',
      keyword: '#8a3b12',
      string: '#3d6b1e',
      number: '#7a3d8a',
      type: '#1f5a7a',
      lineNumber: '#8a8472',
      activeLineNumber: '#8a3b12',
      lineHighlight: '#f2ebd8',
      selection: '#e6dcc0',
      cursor: '#8a3b12',
    },
  },
  {
    id: 'vs-dark',
    label: 'Visual Studio · 어둡게',
    base: 'vs-dark',
    custom: false,
    colors: {
      background: '#1e1e1e',
      foreground: '#d4d4d4',
      muted: '#a0a0a0',
      comment: '#6a9955',
      keyword: '#569cd6',
      string: '#ce9178',
      number: '#b5cea8',
      type: '#4ec9b0',
      lineNumber: '#858585',
      activeLineNumber: '#c6c6c6',
      lineHighlight: '#282828',
      selection: '#264f78',
      cursor: '#aeafad',
    },
  },
  {
    id: 'vs',
    label: 'Visual Studio · 밝게',
    base: 'vs',
    custom: false,
    colors: {
      background: '#fffffe',
      foreground: '#000000',
      muted: '#616161',
      comment: '#008000',
      keyword: '#0000ff',
      string: '#a31515',
      number: '#098658',
      type: '#267f99',
      lineNumber: '#237893',
      activeLineNumber: '#0b216f',
      lineHighlight: '#f3f3f3',
      selection: '#add6ff',
      cursor: '#000000',
    },
  },
  {
    id: 'hc-black',
    label: '고대비',
    base: 'hc-black',
    custom: false,
    colors: {
      background: '#000000',
      foreground: '#ffffff',
      muted: '#ffffff',
      comment: '#7ca668',
      keyword: '#569cd6',
      string: '#ce9178',
      number: '#b5cea8',
      type: '#4ec9b0',
      lineNumber: '#ffffff',
      activeLineNumber: '#f38518',
      lineHighlight: '#000000',
      selection: '#ffffff',
      cursor: '#ffffff',
    },
  },
]

export const DEFAULT_EDITOR_THEME: EditorThemeChoice = 'auto'

export function isEditorThemeChoice(value: unknown): value is EditorThemeChoice {
  return value === 'auto' || EDITOR_THEMES.some((theme) => theme.id === value)
}

/** `auto` 는 앱 테마를 따른다 */
export function resolveEditorTheme(choice: EditorThemeChoice, app: 'light' | 'dark'): EditorTheme {
  const id = choice === 'auto' ? (app === 'dark' ? 'pixel-dark' : 'pixel-light') : choice
  return EDITOR_THEMES.find((theme) => theme.id === id) ?? EDITOR_THEMES[0]!
}

/** Monaco 에 넘기는 테마 이름. 우리 테마는 이름이 겹치지 않게 접두를 붙여 등록한다 */
export function monacoThemeName(theme: EditorTheme): string {
  return theme.custom ? `codedrill-${theme.id}` : theme.id
}
