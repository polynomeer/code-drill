# 커밋 컨벤션

> **역할**: 커밋 메시지를 어떤 형식으로 쓰는가
> **단일 출처**: 커밋 메시지 형식, type 목록, 커밋 분할 기준
> **갱신 트리거**: type을 추가·삭제하거나 형식 규칙을 바꿀 때 (`.gitmessage`도 함께 고친다)

[Conventional Commits 1.0.0](https://www.conventionalcommits.org/ko/v1.0.0/)을 따른다.

```
<type>(<scope>): <subject>

<body>

<footer>
```

## type

| type | 용도 |
|---|---|
| `feat` | 사용자에게 보이는 기능 추가 |
| `fix` | 버그 수정 |
| `docs` | 문서만 변경 |
| `style` | 동작에 영향 없는 포맷팅 |
| `refactor` | 동작 변화 없는 구조 개선 |
| `perf` | 성능 개선 |
| `test` | 테스트 추가·수정 |
| `build` | 빌드 시스템·의존성 변경 |
| `ci` | CI 설정 변경 |
| `chore` | 그 외 설정·스크립트 |
| `revert` | 이전 커밋 되돌리기 |

`feat`과 `fix`를 가르는 기준은 크기가 아니라 **이전에 동작했는가**다. 동작하던 것을
고치면 `fix`, 없던 것을 만들면 `feat`.

## scope

선택. 변경이 닿은 모듈을 한 단어로 쓴다. 모듈 이름은
[project-context.md](project-context.md)의 모듈 경계를 따른다.
예: `feat(judge):`, `fix(submission):`, `docs(competency):`

## subject

명령형 현재시제, 소문자 시작, 마침표 없음, 72자 이내. 영어로 쓴다.

- 좋음: `fix(judge): reject late results with stale fencing token`
- 나쁨: `fixed the judge.` / `Judge 수정함`

## body

선택. **무엇을·왜** 바꿨는지 쓴다 — 어떻게는 코드가 말한다. 한글로 써도 된다.
72자에서 줄바꿈한다.

## footer

- 파괴적 변경: type 뒤에 `!` (`feat(api)!:`) 또는 `BREAKING CHANGE: <설명>`
- 이슈 연결: `Closes #12`
- Claude가 만든 커밋: `Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>`

## 커밋 분할

논리적 변경 하나 = 커밋 하나. 리팩터링과 기능 추가를 한 커밋에 섞지 않는다.
한 커밋의 subject에 "and"나 쉼표가 필요하면 커밋을 나눌 신호다.

## 예시

```
feat(judge): add iterative aggregation for test groups

재귀 집계가 그룹 수가 많은 문제에서 스택을 깊게 써서 명시적 스택을 쓰는
반복 버전으로 교체했다. 집계 결과의 결정성은 그대로 유지한다.

Closes #7
```

## 메시지 템플릿

루트의 `.gitmessage`를 `git commit` 편집기에 띄우려면 클론 후 한 번 실행한다.

```
git config commit.template .gitmessage
```
