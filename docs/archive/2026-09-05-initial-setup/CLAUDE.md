# code-drill

코딩 연습(드릴) 저장소. 아직 언어/스택이 확정되지 않았으므로, 첫 코드를 추가할 때
이 문서의 "프로젝트 구조"와 "명령어" 섹션을 함께 갱신한다.

## 프로젝트 구조

```
.
├── CLAUDE.md            # 이 문서 (Claude Code 작업 규칙)
├── CONTRIBUTING.md      # 커밋 컨벤션 상세
├── .gitmessage          # 커밋 메시지 템플릿
└── .claude/settings.json # 프로젝트 공용 Claude Code 설정
```

## 명령어

아직 빌드/테스트 도구 없음. 도구를 도입하면 여기에 실제 명령어를 기록한다.

## 작업 규칙

### 1. 모든 작업은 반드시 커밋한다

- 사용자가 요청한 작업을 완료하면 **항상 커밋까지 수행한다.** 별도로 "커밋해줘"라는
  말을 기다리지 않는다.
- 하나의 논리적 변경 = 하나의 커밋. 서로 무관한 변경을 한 커밋에 섞지 않는다.
- 커밋은 하되 **push는 사용자가 명시적으로 요청할 때만** 한다.
- 커밋 전 `git status`와 `git diff`로 의도한 파일만 스테이징됐는지 확인한다.
- 비밀정보(.env, 키, 토큰)는 절대 커밋하지 않는다.

### 2. 브랜치

- 기본 브랜치는 `main`. 별도 요청이 없으면 `main`에서 직접 작업하고 커밋한다.
- 사용자가 브랜치를 요청하면 `<type>/<짧은-설명>` 형식을 쓴다. 예: `feat/binary-search`.

## 커밋 컨벤션 — Conventional Commits 1.0.0

```
<type>(<scope>): <subject>

<body>

<footer>
```

### type (필수)

| type | 용도 |
|---|---|
| `feat` | 새로운 기능 추가 |
| `fix` | 버그 수정 |
| `docs` | 문서만 변경 |
| `style` | 동작에 영향 없는 포맷팅(공백, 세미콜론 등) |
| `refactor` | 기능 변경 없는 코드 구조 개선 |
| `perf` | 성능 개선 |
| `test` | 테스트 추가/수정 |
| `build` | 빌드 시스템, 의존성 변경 |
| `ci` | CI 설정 변경 |
| `chore` | 그 외 잡무(설정, 스크립트 등) |
| `revert` | 이전 커밋 되돌리기 |

### 규칙

- `scope`는 선택. 변경 범위를 한 단어로. 예: `feat(graph): ...`
- `subject`는 명령형 현재시제, 소문자로 시작, 마침표 없음, 72자 이내.
  - 좋음: `fix(parser): handle empty input`
  - 나쁨: `fixed the parser.` / `Parser 수정함.`
- `body`는 선택. **무엇을·왜** 바꿨는지 설명(어떻게는 코드가 말한다). 72자에서 줄바꿈.
- 파괴적 변경은 type 뒤에 `!`를 붙이거나 footer에 `BREAKING CHANGE: <설명>`을 쓴다.
- 이슈 연결은 footer에: `Closes #12`
- Claude가 만든 커밋은 마지막 줄에 다음을 포함한다:
  `Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>`

### 예시

```
feat(sort): add iterative quicksort implementation

재귀 버전이 깊은 입력에서 스택 오버플로를 일으켜 명시적 스택을 쓰는
반복 버전을 추가했다. 재귀 버전은 비교용으로 유지한다.

Closes #7
```
