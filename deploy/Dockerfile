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
# 셋 중 유일하게 무거운 이미지다. 필요한 것이 셋이다.
#
#   JDK        javax.tools 로 자바를 in-process 컴파일한다 (JRE 로는 안 된다)
#   python3    py_compile 로 파이썬 문법을 검사한다
#   docker CLI 사용자 코드를 격리 컨테이너에서 돌린다 (§5.2)
#
# **이 이미지는 컨테이너 런타임에 접근할 수 있어야 제 일을 한다.** 그 접근을 어떻게
# 주느냐가 이 배포에서 가장 위험한 결정이며, docs/deploying.md 에 따로 적었다.

FROM eclipse-temurin:21-jdk AS runner-agent
WORKDIR /app

RUN apt-get update \
    && apt-get install --yes --no-install-recommends python3 docker.io \
    && rm -rf /var/lib/apt/lists/*

COPY --from=builder /src/judge/runner-agent/build/install/runner-agent /app/runner-agent

EXPOSE 8082
ENTRYPOINT ["/app/runner-agent/bin/runner-agent"]
