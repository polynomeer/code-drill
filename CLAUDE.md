# CodeDrill (code-drill)

실행 증거로 알고리즘 문제 해결 역량을 진단·코칭·검증하는 온라인 저지.
판정(Judge)이 제품의 1차 흐름이고, 리플레이·역량 진단은 그 위에 얹힌다.

제품 맥락, 스택, 도메인 용어 → [docs/project-context.md](docs/project-context.md)

## 작업 완료 기준

**작업은 커밋되어야 끝난다.** 사용자가 커밋을 따로 요청하지 않아도 아래를 수행한다.

1. `git status --short`로 변경 파일을 확인한다.
2. 이번 요청과 관련된 파일만 스테이징한다.
3. [커밋 컨벤션](docs/commit-convention.md)에 맞춰 커밋한다.
4. `git log -1 --stat`으로 의도한 파일만 들어갔는지 확인한다.

논리적 변경 하나 = 커밋 하나. 한 요청이 여러 논리적 변경을 만들면 커밋도 나눈다.
`push`와 브랜치 생성은 사용자가 명시적으로 요청할 때만 한다.

한 줄 형식은 `<type>(<scope>): <subject>` — 명령형, 소문자 시작, 마침표 없음, 72자 이내.
type 목록과 예시는 위 커밋 컨벤션 문서에 있다.
Claude가 만든 커밋은 마지막 줄에 `Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>`.

## 문서를 고칠 때

문서 지도와 갱신 트리거 → [docs/README.md](docs/README.md)

규칙 하나는 한 문서에만 산다. 같은 규칙을 두 곳에 쓰는 대신 링크한다.

## 명령어

빌드·테스트 도구 미도입. 도구를 세팅하면 실제 명령어를 이 섹션에 기록한다.
