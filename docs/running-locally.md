# 로컬에서 돌리기

> **역할**: 첫 vertical slice 를 내 머신에서 띄우고 확인하는 방법
> **단일 출처**: 로컬 실행 절차와 알려진 함정
> **갱신 트리거**: 배포 단위가 늘거나, 실행 방식·포트·필수 도구가 바뀔 때

## 필요한 것

JDK 21, Node 22+, pnpm, Docker. Gradle 은 wrapper 를 쓴다.

로컬 기본 JDK 가 21 보다 높아도 된다. `gradle/gradle-daemon-jvm.properties` 가 데몬 JVM 을
21 로 고정하고, `./gradlew` 가 설치된 21 을 찾아 쓴다.

## 1. 인프라

```bash
docker compose -f deploy/docker-compose.yml up -d
```

포트가 이미 쓰이고 있으면 환경변수로 바꾼다.

```bash
POSTGRES_PORT=55432 REDIS_PORT=56379 docker compose -f deploy/docker-compose.yml up -d
```

## 2. 세 앱

배포 단위가 셋이다. 각각 별도 터미널에서 띄운다.

```bash
DB_URL=jdbc:postgresql://localhost:5432/codedrill ./gradlew :control-plane:app:bootRun
```

```bash
./gradlew :judge:orchestrator:bootRun
```

```bash
./gradlew :judge:runner-agent:installDist
judge/runner-agent/build/install/runner-agent/bin/runner-agent
```

**Runner 는 컨테이너로 채점한다.** 기동 로그에 어떤 이미지로 격리하는지, digest 까지
찍힌다. 이미지가 로컬에 없으면 경고와 함께 프로세스 샌드박스로 내려가므로, 로그를 보고
격리가 실제로 섰는지 확인한다.

```bash
docker pull eclipse-temurin:21-jre
docker pull python:3.12-alpine
```

다른 이미지를 쓰려면 환경변수로 바꾼다.

```bash
KOTLIN_IMAGE=gradle:8.10.2-jdk21 JAVA_IMAGE=gradle:8.10.2-jdk21 PYTHON_IMAGE=python:3.12-slim \
  judge/runner-agent/build/install/runner-agent/bin/runner-agent
```

공개 환경에서는 `REQUIRE_ISOLATION=true` 로 두어, 컨테이너 런타임이 없으면 기동 자체가
중단되게 한다.

기동 로그에 언어별 seccomp 프로파일의 허용 개수와 digest 가 함께 찍힌다. `런타임 기본값`
이라고 나오면 allowlist 가 걸리지 않은 것이다 — 그 상태는 §5.2 의 시스템 호출 통제만
빠진 채 나머지가 다 서 있어서 로그를 보지 않으면 알아채기 어렵다.

**Runner 만 fat jar 가 아니다.** `kotlin-compiler-embeddable` 이 자기 jar 안의
`extensions/compiler.xml` 을 클래스패스에서 직접 찾는데, Spring Boot fat jar 의 중첩 jar
안에서는 찾지 못하고 컴파일이 통째로 실패한다. 그래서 `installDist` 로 평범한
클래스패스 배포를 만들어 실행한다.

기본 포트는 Control Plane 8080, Orchestrator 8081, Runner 8082. 셋 다
`/actuator/health` 를 연다.

`CONTENT_ROOT` 는 문제 패키지 경로다. 기본값은 `content/problems` 이며, 저장소 루트가
아닌 곳에서 실행하면 절대 경로로 지정해야 한다.

## 3. 관리자 토큰

**운영자를 설정하지 않으면 관리자 API 는 통째로 닫힌다** (§11.2). 기본 토큰을 심어
두면 그 토큰이 반드시 어느 운영 환경에 그대로 남기 때문이다. 제어 영역과 스크립트가
같은 환경변수를 읽으므로, 두 터미널에서 같은 값을 export 한다.

```bash
export ADMIN_OPERATORS="\
content-editor:$(openssl rand -hex 16):CONTENT_EDITOR;\
release-manager:$(openssl rand -hex 16):CONTENT_EDITOR,PUBLISHER;\
release-approver:$(openssl rand -hex 16):PUBLISHER;\
judge-operator:$(openssl rand -hex 16):JUDGE_OPERATOR,REVIEWER;\
judge-reviewer:$(openssl rand -hex 16):REVIEWER;\
security-admin:$(openssl rand -hex 16):SECURITY_ADMIN"
```

역할은 §11.2 의 다섯 가지다. 스모크는 각 역할을 가진 사람이 **둘씩** 있어야 2인 승인을
확인할 수 있고, `release-manager` 와 `judge-operator` 처럼 **두 단계를 모두 할 수 있는
계정**이 하나씩 있어야 한다 — 권한이 과하게 열린 계정에서도 등록자·승인자 분리가
남아 있는지 보기 위한 것이다.

토큰은 24자 이상이어야 하며, 짧으면 기동 시점에 거절된다.

## 4. 문제 공개

**문제는 검증하고 공개해야 목록에 나온다** (§3.2). 디렉터리에 파일을 놓는 것만으로
공개되면 §6.3 검증과 §11.2 2인 승인이 모두 우회되기 때문이다.

```bash
./gradlew :judge:runner-agent:validateContent
python3 scripts/publish-content.py
```

앞 명령이 §6.3 파이프라인(스키마·공식 해답·결정성·제약 여유·mutant kill rate·시각화
예산)을 돌려 보고서를 남기고, 뒤 명령이 통과한 버전만 등록·공개한다. 30문제 전체를
돌리므로 몇 분 걸린다. 문제를 추가하는 방법은 [content/tools/README.md](../content/tools/README.md)
에 있다. 시딩 스크립트는
`ADMIN_OPERATORS` 에서 서로 다른 두 운영자를 골라 쓰므로 로컬·CI 전용이다 — 사람이 하는
공개는 등록과 승인을 각각 다른 사람이 해야 2인 승인이 의미를 갖는다.

개발 중에 공개 절차를 건너뛰려면 `codedrill.content.require-publish=false` 로 띄운다.
**공개 환경에서는 절대 끄지 않는다.**

### 검증 파이프라인을 고친 뒤

보고서 digest 는 파이프라인의 결과에서 나온다. 그래서 §6.3 의 검사를 고치면 **이미
등록된 모든 버전의 보고서가 무효**가 되고, 공개가 409 로 거부된다. 패키지를 고친 적이
없어도 그렇다.

**파이프라인의 판단이 달라졌으면 `ContentValidator.VALIDATOR_VERSION` 을 올린다.** 그러면
거부 사유가 원인을 이름으로 말한다 (§15.3).

```
검증 파이프라인이 바뀌었다: 등록은 1, 지금은 2. 패키지는 그대로여도 보고서는 다시 만들어야 한다.
검증 보고서 digest 가 다르다. 파이프라인은 같으므로(1) 패키지가 바뀐 것이다.
```

버전을 올렸으면 행을 지울 필요가 없다. `validateContent` 를 돌리고 `publish-content.py`
를 그대로 돌리면 **같은 버전에 보고서만 갈아 끼운다** — 문제는 하나도 바뀌지 않았고
검사 기준만 바뀐 것이라, 새 버전 번호를 만들면 바뀐 것 없는 버전이 하나씩 쌓인다.
그 교체는 감사 로그에 `PROBLEM_VERSION_REVALIDATED` 로 따로 남는다.

패키지가 조금이라도 다르면 이 경로로 못 들어온다 — 고친 패키지는 새 버전이어야 한다
(§6.1 공개 후 불변).

**참조 풀이나 오답만 고쳤을 때도 같은 경로다.** 패키지 digest 는 manifest 와 tests 만
덮으므로(사용자에게 나가는 것이 그 둘이다), 그 둘을 안 건드렸으면 패키지는 그대로이고
보고서만 달라진다. 오답 하나를 고쳤다고 사용자에게 보이는 버전 번호가 올라가지는 않는다.

### 값 타입을 늘렸으면 앱부터 다시 띄운다

`ValueType` 에 값을 더하는 것은 **읽는 쪽에 대한 파괴적 변경**이다 (§15.3). 새 타입을 쓰는
문제를 공개하면, 그 타입을 모르는 제어 영역은 manifest 를 파싱하지 못한다. 그런데 목록
API 는 패키지를 전부 읽어 만들므로 **문제 하나가 목록 전체를 500 으로 만든다** — 새 문제만
안 보이는 것이 아니다.

그래서 순서가 있다. 빌드 → 세 앱 재기동 → `validateContent` → `publish-content.py`.
거꾸로 하면 재기동 전까지 목록이 죽는다.

```bash
./gradlew :judge:runner-agent:validateContent && python3 scripts/publish-content.py
```

## 5. 웹

```bash
cd web && pnpm install && pnpm dev
```

`http://localhost:5173` 에서 열린다. `/api` 는 8080 으로 프록시된다.

**처음 열면 로그인 화면이다.** 제출·초안·기록은 전부 인증을 요구하므로(§11.2), 가입하고
들어간다. 비밀번호는 10자 이상이어야 한다. 문제 목록과 상세는 로그인 없이도 열린다.

## 6. 확인

```bash
python3 scripts/smoke.py
```

판정 5종(AC/WA/CE/RE/TLE), 3개 언어, 부분 점수, 초안 CAS, 목록 커서, 트레이스 목차·청크,
사용자 인증과 객체 소유권, 문자열·격자 값 타입의 3개 언어 왕복, 관리자 API 인증과 역할 분리,
콘텐츠 공개와 2인 승인,
재채점 승인·실행·dry-run, 멱등성, SSE, 숨은 테스트 비노출까지 실제 서비스로 확인한다.
115개 항목이 전부 통과해야 한다.

스크립트는 실행할 때마다 계정을 새로 만든다. 고정 계정을 두면 그 비밀번호가 저장소에
남고, 실행할 때마다 남의 기록이 섞인다.

웹 리듀서 불변식은 따로 돈다.

```bash
cd web && pnpm test
```

SLO 와 장애 회복은 별도 스크립트가 잰다. 절차와 해석은 [runbook.md](runbook.md) 에 있다.

```bash
python3 scripts/loadtest.py       # §12.1 SLO 네 가지
python3 scripts/drill.py all      # 장애 주입 훈련 — 컨테이너와 Runner 를 실제로 죽인다
```

## 겪게 되는 것들

| 증상 | 원인 |
|---|---|
| 큐 선언 실패로 앱이 안 뜬다 | RabbitMQ 가 아직 healthy 가 아니다. 10초쯤 기다린다 |
| 제출이 `QUEUED` 에서 멈춘다 | Orchestrator 가 안 떴거나 `CONTENT_ROOT` 가 틀렸다 |
| 모든 제출이 `COMPILE_ERROR` | Runner 를 fat jar 로 띄웠다. `installDist` 배포를 쓴다 |
| 판정은 오는데 리플레이가 없다 | 풀이가 `Drill.*` 를 호출하지 않는다. 계측은 선택이다 |
| 기동 로그에 "프로세스 샌드박스로 내려간다" | 런타임 이미지가 로컬에 없다. 미리 pull 한다 |
| 기동 로그에 seccomp "런타임 기본값" | 프로파일을 못 썼다. 임시 디렉터리 권한을 본다 |
| 특정 풀이만 `Operation not permitted` | allowlist 가 좁다. `SeccompProfile` 에 근거와 함께 추가한다 |
| Python 만 MEMORY_LIMIT 이 안 잡힌다 | macOS 는 RLIMIT_AS 를 낮추지 못한다. 컨테이너로 돌려야 한다 |
| 초안 저장이 409 만 낸다 | 다른 탭이 같은 초안을 열고 있다. 충돌 배너에서 한쪽을 고른다 |
| 리플레이가 텍스트 목록으로만 보인다 | 트레이스가 INVALID 이거나 스키마가 클라이언트보다 높다 |
| 리플레이에 상자가 하나도 없다 | 풀이가 `Drill.*` 를 부르지 않았다. 계측은 선택이다 |
| 문제 목록이 비어 있다 | 아직 공개하지 않았다. 위 4번 절차를 돌린다 |
| 문제 목록 전체가 500 | 앱이 새 값 타입을 모른다. 세 앱을 다시 띄운다 (아래) |
| 공개가 409 로 거부된다 | 사유를 읽는다. 등록자·승인자 동일, 패키지 변경, 파이프라인 변경이 각각 다른 문구다 |
| 관리자 API 가 전부 503 | `ADMIN_OPERATORS` 를 앱 터미널에서 export 하지 않았다 |
| 관리자 API 가 401 | 스크립트 터미널의 `ADMIN_OPERATORS` 가 앱의 것과 다르다 |
| 관리자 API 가 403 | 그 토큰의 역할로는 못 하는 작업이다. 다른 운영자로 부른다 |
| 제출·초안이 401 | 로그인이 만료됐다. 웹은 자동 갱신하지만 스크립트는 다시 만든다 |
| 남의 제출이 404 | 정상이다. 소유자가 아니면 있는 것조차 알려 주지 않는다 (§11.3) |
| 이전 슬라이스의 제출이 안 보인다 | `X-User-Id` 시절 행은 어떤 계정에도 속하지 않는다 |
