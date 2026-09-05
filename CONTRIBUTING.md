# 기여 가이드

## 커밋 컨벤션

이 저장소는 [Conventional Commits 1.0.0](https://www.conventionalcommits.org/ko/v1.0.0/)을
따른다. 전체 규칙과 예시는 [CLAUDE.md](CLAUDE.md#커밋-컨벤션--conventional-commits-100)에 있다.

요약:

```
<type>(<scope>): <subject>

<body>

<footer>
```

- **type**: `feat` `fix` `docs` `style` `refactor` `perf` `test` `build` `ci` `chore` `revert`
- **subject**: 명령형·소문자 시작·마침표 없음·72자 이내
- **파괴적 변경**: `feat!:` 또는 footer에 `BREAKING CHANGE:`
- **이슈 연결**: footer에 `Closes #12`

## 커밋 메시지 템플릿

저장소를 클론한 뒤 한 번 실행하면 `git commit` 시 템플릿이 뜬다.

```
git config commit.template .gitmessage
```

## 커밋 단위

하나의 논리적 변경 = 하나의 커밋. 리팩터링과 기능 추가를 한 커밋에 섞지 않는다.
