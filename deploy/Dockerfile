# 배포 이미지 (기술 설계서 §5.5 런타임 이미지 릴리스, §15.1 파이프라인).
#
# 세 배포 단위를 한 파일에서 만든다. 빌더 단계를 공유하므로 세 번 빌드해도 컴파일은
# 한 번이고, 빌드 방법이 한 곳에만 있어 셋이 어긋나지 않는다.
#
#   docker build -f deploy/Dockerfile --target control-plane -t codedrill/control-plane .
#   docker build -f deploy/Dockerfile --target orchestrator  -t codedrill/orchestrator  .
#   docker build -f deploy/Dockerfile --target runner-agent  -t codedrill/runner-agent  .
#
# 태그 규칙과 롤백 경로는 docs/deploying.md 에 있다.

# --- 빌더 -------------------------------------------------------------------

FROM eclipse-temurin:21-jdk AS builder
WORKDIR /src

# 래퍼와 의존성 선언을 먼저 넣는다. 소스만 바뀐 재빌드에서 의존성 내려받기를 건너뛴다.
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
COPY --chmod=755 gradlew ./gradlew
RUN --mount=type=cache,target=/root/.gradle ./gradlew --version > /dev/null

COPY . .
# bootJar 는 실행 가능한 단일 jar 다. Runner 만 예외로 installDist 를 쓴다 —
# kotlin-compiler-embeddable 이 fat jar 안에서 extensions/compiler.xml 을 찾지 못한다.
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon --quiet \
      :control-plane:app:bootJar \
      :judge:orchestrator:bootJar \
      :judge:runner-agent:installDist

# --- 제어 영역 ---------------------------------------------------------------

FROM eclipse-temurin:21-jre AS control-plane
WORKDIR /app

# 문제 패키지를 이미지에 굽는다. 목록·상세 API 가 이 디렉터리를 읽으므로(§9.2),
# 볼륨으로 빼면 "어느 콘텐츠로 뜬 서비스인가"를 이미지 태그로 말할 수 없다.
COPY content/problems /app/content/problems
COPY --from=builder /src/control-plane/app/build/libs/app-*-SNAPSHOT.jar /app/app.jar

ENV CONTENT_ROOT=/app/content/problems
EXPOSE 8080
# 비특권 사용자로 돈다. 제어 영역은 사용자 코드를 실행하지 않지만, 실행하지 않는다는
# 사실이 root 로 도는 이유가 되지는 않는다.
USER 65534:65534
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# --- 오케스트레이터 -----------------------------------------------------------

FROM eclipse-temurin:21-jre AS orchestrator
WORKDIR /app

COPY content/problems /app/content/problems
COPY --from=builder /src/judge/orchestrator/build/libs/orchestrator-*-SNAPSHOT.jar /app/app.jar

ENV CONTENT_ROOT=/app/content/problems
EXPOSE 8081
USER 65534:65534
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# --- Runner ------------------------------------------------------------------
#
# JRE 하나에 컨테이너 CLI 를 얹는다. **이 이미지에는 컴파일러가 없다.** 컴파일은 사용자
# 코드를 읽는 첫 프로그램이라 실행과 같은 샌드박스 안에서 돈다 (§5.5) — Java 는 런타임
# 이미지(JDK)의 javac 으로, Kotlin 은 Runner 가 자기 클래스패스의 컴파일러 jar 를 읽기
# 전용으로 들여보내서, Python 은 인터프리터 자신으로. 전에는 JDK 와 python3 를 여기 담고
# Runner 프로세스 안에서 컴파일했다. 그 시절의 1.35GB 가 이제 필요 없다.
#
# **이 이미지는 컨테이너 런타임에 접근할 수 있어야 제 일을 한다.** 그 접근을 어떻게
# 주느냐가 이 배포에서 가장 위험한 결정이며, docs/deploying.md 에 따로 적었다.

FROM eclipse-temurin:21-jre AS runner-agent
WORKDIR /app

RUN apt-get update \
    && apt-get install --yes --no-install-recommends docker.io \
    && rm -rf /var/lib/apt/lists/*

COPY --from=builder /src/judge/runner-agent/build/install/runner-agent /app/runner-agent

EXPOSE 8082
ENTRYPOINT ["/app/runner-agent/bin/runner-agent"]

# --- 웹 ----------------------------------------------------------------------
#
# 배포 단위 넷째다. 개발에서는 Vite 가 정적 자산도 내주고 /api 프록시도 했지만, 배포에는
# Vite 가 없다. 그 두 가지를 nginx 가 대신한다 (deploy/web.nginx.conf).

FROM node:22-alpine AS web-builder
WORKDIR /web

RUN corepack enable

# 잠금 파일과 선언을 먼저 넣어 의존성 설치를 캐시한다. 소스만 바뀐 재빌드에서 건너뛴다.
COPY web/package.json web/pnpm-lock.yaml web/pnpm-workspace.yaml ./
RUN --mount=type=cache,target=/root/.local/share/pnpm/store \
    pnpm install --frozen-lockfile

COPY web ./
# `pnpm build` 는 타입 검사·테스트·번들에 **번들 예산**까지 돈다. 예산을 이미지 빌드에서
# 돌리는 이유는, 크기가 무너진 채로 이미지가 만들어지면 배포하기 전에는 아무도 모르기
# 때문이다 (web/scripts/bundle-budget.mjs).
RUN pnpm build

# 1.27-alpine 은 openssl 3.3.3 을 담고 있었고 거기 고칠 수 있는 CRITICAL 이 있었다
# (§11.4 이미지 스캔이 잡았다). 1.29-alpine 은 3.5.x 다.
FROM nginxinc/nginx-unprivileged:1.29-alpine AS web
COPY --from=web-builder /web/dist /usr/share/nginx/html
COPY deploy/web.nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 8080
