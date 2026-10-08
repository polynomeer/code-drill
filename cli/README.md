# codedrill CLI

프로젝트형 문제를 내 IDE 에서 푼다 — 받기·로컬 시험·제출. 판정은 서버가 하고, 숨은 테스트는 키트에도
CLI 에도 없다. 설계와 범위 → [feature-roadmap 11단계 이어서](../docs/feature-roadmap.md).

```bash
codedrill login              # 브라우저에서 코드를 맞춰 승인 (RFC 8628)
codedrill get job-queue      # ./job-queue 에 시작 저장소·공개 테스트·하네스
cd job-queue
codedrill test               # 채점기와 같은 하네스로 공개 테스트
codedrill submit             # 제출하고 판정을 기다린다
```

서버는 `--server` 나 `CODEDRILL_SERVER` (기본 `http://localhost:8080`). 로컬에서 웹이 다른 포트면 승인
링크가 웹을 가리키도록 `CODEDRILL_WEB=http://localhost:5173`. 세션은 사용자 설정 폴더의
`codedrill/credentials.json`(0600)에 둔다 — 웹의 계정 설정 → 연결된 기기에서 끊으면 거기서 끝난다.

## 설치

| 길 | 명령 |
| --- | --- |
| macOS·Linux | `curl -fsSL https://github.com/polynomeer/code-drill/releases/latest/download/install.sh \| sh` |
| Homebrew | `brew install --cask polynomeer/tap/codedrill` |
| Scoop | `scoop bucket add codedrill https://github.com/polynomeer/scoop-bucket` 뒤 `scoop install codedrill` |
| 직접 | [Releases](https://github.com/polynomeer/code-drill/releases) 의 묶음 |

설치 스크립트는 체크섬을 맞춰 보고, cosign 이 있으면 `checksums.txt` 의 서명(이 저장소 `cli.yml` 워크플로의
신원)도 확인한다.

## 개발

```bash
cd cli && go test ./...
go run . help
```

의존성은 표준 라이브러리뿐이다. 제출할 파일을 고르는 규칙(`internal/kit`)은 서버의 `ProjectKit.KIT_PATHS`,
웹의 `kitFiles.ts` 와 같아야 한다.

## 릴리스

`v*` 태그를 밀면 `.github/workflows/cli.yml` 이 GoReleaser 로 여섯 판을 올린다. 앱은 커밋 해시로 배포하고
태그를 쓰지 않으므로 `v*` 는 CLI 의 것이다. Homebrew 탭·Scoop 버킷은 `polynomeer/homebrew-tap`(`Casks/`),
`polynomeer/scoop-bucket`(`bucket/`)에 쓴다. 두 저장소에 Contents 쓰기 권한만 있는 fine-grained 토큰을
이 저장소의 `TAP_GITHUB_TOKEN` 비밀로 넣어야 올라간다 — 없으면 Releases 만.
