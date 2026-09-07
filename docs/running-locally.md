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
예산)을 돌려 보고서를 남기고, 뒤 명령이 통과한 버전만 등록·공개한다. 시딩 스크립트는
`ADMIN_OPERATORS` 에서 서로 다른 두 운영자를 골라 쓰므로 로컬·CI 전용이다 — 사람이 하는
공개는 등록과 승인을 각각 다른 사람이 해야 2인 승인이 의미를 갖는다.

개발 중에 공개 절차를 건너뛰려면 `codedrill.content.require-publish=false` 로 띄운다.
**공개 환경에서는 절대 끄지 않는다.**

## 5. 웹

```bash
cd web && pnpm install && pnpm dev
```

`http://localhost:5173` 에서 열린다. `/api` 는 8080 으로 프록시된다.

## 6. 확인

```bash
python3 scripts/smoke.py
```

판정 5종(AC/WA/CE/RE/TLE), 3개 언어, 부분 점수, 초안 CAS, 목록 커서, 트레이스 목차·청크,
관리자 API 인증과 역할 분리, 콘텐츠 공개와 2인 승인, 재채점 승인, 멱등성, SSE, 숨은
테스트 비노출까지 실제 서비스로 확인한다. 68개 항목이 전부 통과해야 한다.

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
| Python 만 MEMORY_LIMIT 이 안 잡힌다 | macOS 는 RLIMIT_AS 를 낮추지 못한다. 컨테이너로 돌려야 한다 |
| 초안 저장이 409 만 낸다 | 다른 탭이 같은 초안을 열고 있다. 충돌 배너에서 한쪽을 고른다 |
| 리플레이가 텍스트 목록으로만 보인다 | 트레이스가 INVALID 이거나 스키마가 클라이언트보다 높다 |
| 리플레이에 상자가 하나도 없다 | 풀이가 `Drill.*` 를 부르지 않았다. 계측은 선택이다 |
| 문제 목록이 비어 있다 | 아직 공개하지 않았다. 위 4번 절차를 돌린다 |
| 공개가 409 로 거부된다 | 등록자와 승인자가 같거나 보고서 digest 가 다르다 |
| 관리자 API 가 전부 503 | `ADMIN_OPERATORS` 를 앱 터미널에서 export 하지 않았다 |
| 관리자 API 가 401 | 스크립트 터미널의 `ADMIN_OPERATORS` 가 앱의 것과 다르다 |
| 관리자 API 가 403 | 그 토큰의 역할로는 못 하는 작업이다. 다른 운영자로 부른다 |
