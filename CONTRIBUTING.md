# 기여 가이드

## 처음 한 번

커밋 메시지 템플릿을 활성화한다.

```
git config commit.template .gitmessage
```

## 무엇을 읽어야 하나

| 알고 싶은 것 | 문서 |
|---|---|
| 커밋 메시지 형식과 type | [docs/commit-convention.md](docs/commit-convention.md) |
| 제품 맥락, 스택, 도메인 용어 | [docs/project-context.md](docs/project-context.md) |
| 문서를 어디에 어떻게 쓰는가 | [docs/README.md](docs/README.md) |
| Claude Code가 이 저장소에서 지키는 규칙 | [CLAUDE.md](CLAUDE.md) |

## 작업 흐름

기본 브랜치는 `main`이다. 작은 변경은 `main`에서 직접 커밋하고, 리뷰가 필요한 변경은
`<type>/<짧은-설명>` 브랜치를 판다. 예: `feat/judge-aggregation`

논리적 변경 하나당 커밋 하나로 나눈다. 자세한 기준은
[커밋 분할](docs/commit-convention.md#커밋-분할)에 있다.
