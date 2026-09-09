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

```bash
./gradlew build                   # 백엔드 전체 빌드 + 테스트 + 모듈 경계 검사
./gradlew test                    # 테스트만
./gradlew checkModuleBoundaries   # 도메인 모듈이 서로를 참조하는지 검사
./gradlew :judge:runner-agent:validateContent   # 콘텐츠 검증 (§6.3)
python3 scripts/up.py             # 의존성+앱 셋+웹을 한 번에 (포트가 차 있으면 비켜 간다)
python3 scripts/smoke.py          # E2E 스모크 (앱 세 개가 떠 있어야 한다)
python3 scripts/loadtest.py       # 부하 시험과 §12.1 SLO 판정
python3 scripts/drill.py all      # 장애 주입 훈련 (컨테이너와 Runner 를 실제로 죽인다)
python3 scripts/backup.py verify  # 백업을 임시 DB 에 복원해 본다 (§12.3)
python3 scripts/scan-images.py    # 배포 이미지 취약점 게이트 (§11.4)
```

경보가 울렸을 때의 첫 대응은 → [docs/runbook.md](docs/runbook.md)

```bash
cd web && pnpm build   # tsc --noEmit + vitest + vite build + 번들 예산
cd web && pnpm test    # 리플레이 리듀서 불변식, Monaco import 경계
cd web && pnpm dev     # :8080 으로 /api 프록시
```

세 앱을 띄우는 절차와 함정은 → [docs/running-locally.md](docs/running-locally.md)
