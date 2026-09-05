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

## 3. 웹

```bash
cd web && pnpm install && pnpm dev
```

`http://localhost:5173` 에서 열린다. `/api` 는 8080 으로 프록시된다.

## 4. 확인

```bash
python3 scripts/smoke.py
```

판정 5종(AC/WA/CE/RE/TLE), 3개 언어, 부분 점수, 트레이스, 멱등성, SSE, 숨은 테스트
비노출까지 실제 서비스로 확인한다. 24개 항목이 전부 통과해야 한다.

## 겪게 되는 것들

| 증상 | 원인 |
|---|---|
| 큐 선언 실패로 앱이 안 뜬다 | RabbitMQ 가 아직 healthy 가 아니다. 10초쯤 기다린다 |
| 제출이 `QUEUED` 에서 멈춘다 | Orchestrator 가 안 떴거나 `CONTENT_ROOT` 가 틀렸다 |
| 모든 제출이 `COMPILE_ERROR` | Runner 를 fat jar 로 띄웠다. `installDist` 배포를 쓴다 |
| 판정은 오는데 리플레이가 없다 | 풀이가 `Drill.*` 를 호출하지 않는다. 계측은 선택이다 |
| 기동 로그에 "프로세스 샌드박스로 내려간다" | 런타임 이미지가 로컬에 없다. 미리 pull 한다 |
| Python 만 MEMORY_LIMIT 이 안 잡힌다 | macOS 는 RLIMIT_AS 를 낮추지 못한다. 컨테이너로 돌려야 한다 |
