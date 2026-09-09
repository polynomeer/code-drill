# 배포하기

> **역할**: 이 제품을 개발 머신 밖에서 어떻게 띄우는가
> **단일 출처**: 이미지 경계, 태그 규칙, 롤백 경로, Runner 배치 제약
> **갱신 트리거**: 배포 단위가 늘거나, 이미지 내용·태그 규칙이 바뀔 때

로컬에서 띄우는 방법은 [running-locally.md](running-locally.md) 에 있다. 이 문서는
이미지로 만들어 다른 곳에서 돌리는 이야기다.

## 이미지 넷

[deploy/Dockerfile](../deploy/Dockerfile) 하나에서 네 개를 만든다. 빌더 단계를 공유하므로
컴파일은 한 번이고, 빌드 방법이 한 곳에만 있어 넷이 어긋나지 않는다.

```bash
for t in control-plane orchestrator runner-agent web; do
  docker build -f deploy/Dockerfile --target $t -t codedrill/$t:$TAG .
done
```

| 이미지 | 담는 것 | 크기 |
|---|---|---|
| control-plane | JRE + bootJar + **문제 패키지** | ~590MB |
| orchestrator | JRE + bootJar + 문제 패키지 | ~580MB |
| runner-agent | **JDK** + installDist + python3 + docker CLI | ~1.35GB |
| web | nginx + 정적 번들 | ~82MB |

**웹은 정적 자산만이 아니다.** 개발에서는 Vite 가 자산을 내주고 `/api` 를 제어 영역으로
넘겼는데(§9.1), 배포에는 Vite 가 없다. 그 두 가지를 [nginx 설정](../deploy/web.nginx.conf)
이 대신한다 — SPA 폴백, 자산 영구 캐시와 `index.html` 무캐시, 그리고 **SSE 버퍼링 끄기**.
마지막 것을 빠뜨리면 판정이 실시간으로 오지 않고 한참 뒤에 뭉쳐서 도착한다.

**문제 패키지를 이미지에 굽는다.** 목록·상세 API 가 그 디렉터리를 읽으므로(§9.2),
볼륨으로 빼면 "어느 콘텐츠로 뜬 서비스인가"를 태그로 말할 수 없다. 콘텐츠를 고치면
이미지를 다시 만든다.

**Runner 만 JDK 다.** `javax.tools` 로 자바를 in-process 컴파일하는데 JRE 에는 그
컴파일러가 없다. 파이썬 문법 검사에 `python3`, 격리 실행에 컨테이너 CLI 가 더 든다.
그래서 셋 중 유일하게 무겁다.

**Runner 는 fat jar 로 못 만든다.** `kotlin-compiler-embeddable` 이 jar 안에서
`extensions/compiler.xml` 을 찾지 못한다. 그래서 이것만 `installDist` 다.

## Runner 를 어디서 돌리나

**이 결정이 이 배포에서 가장 위험한 것이다.**

Runner 는 사용자 코드를 격리 컨테이너에서 돌린다(§5.2). 그러려면 컨테이너 런타임에
접근할 수 있어야 한다. 그런데 Runner 는 **신뢰 경계의 바깥**이다 — 사용자 코드에 가장
가까이 있는 컴포넌트다 (§2.3).

호스트의 컨테이너 소켓을 Runner 컨테이너에 그대로 내주면 그 둘이 겹친다. Runner 를
깨는 데 성공한 사람이 호스트 전체를 얻는다. **격리하려고 만든 컴포넌트에 격리를 무너뜨릴
권한을 주는 셈이다.** 그래서 [docker-compose.apps.yml](../deploy/docker-compose.apps.yml)
에는 Runner 서비스가 없다.

지금 서 있는 선택지는 하나다. **Runner 를 전용 노드에서 돌린다** — 그 노드에는 Runner
말고 아무것도 없고, 노드 자체가 폐기 가능한 격리 단위가 된다.
[deploy/docker-compose.runner.yml](../deploy/docker-compose.runner.yml) 이 그 노드용이며,
앱 스택과 **같이 띄우지 않는다.**

```bash
BROKER_URL=amqp://codedrill:codedrill@<브로커 호스트>:5672 CODEDRILL_TAG=<SHA> \
  docker compose -f deploy/docker-compose.runner.yml up -d
```

### 경로가 같아야 한다

Runner 는 샌드박스 컨테이너를 띄우면서 두 가지를 마운트한다. **그 경로를 해석하는 것은
Runner 가 아니라 호스트 데몬이다.** Runner 안에만 있는 경로를 주면 데몬은 그런 경로가
없으니 빈 디렉터리를 만들어 붙이고, 사용자 코드는 자기 소스나 런타임이 사라진 채로 돌아
전부 SYSTEM_ERROR 가 된다. 로그에는 아무 오류도 남지 않는다.

| 무엇 | 맞추는 방법 |
|---|---|
| 실행 디렉터리 | 호스트의 한 경로를 **같은 이름으로** 마운트하고 `SANDBOX_WORK_ROOT` 에 준다 |
| Kotlin 런타임 jar | Runner 가 기동할 때 그 디렉터리 아래 `runtime/` 로 복제한다 (자동) |

둘째를 빠뜨렸다가 실제로 겪었다. 실행 디렉터리만 맞춰 두면 Java·Python 은 통과하고
**Kotlin 만** SYSTEM_ERROR 가 된다 — 셋 중 Kotlin 만 자기 런타임 jar 를 샌드박스에
들여보내기 때문이다. 언어 하나만 깨지는 실패라 격리 문제로 보이지 않는다.

### 아직 남은 위험

**런타임을 소켓 말고 다른 방식으로 줘야 한다.** rootless 런타임이나 노드마다 하나씩 두는
격리 데몬이 후보다. 지금은 소켓을 그대로 내주고 **노드를 격리 단위로 삼아** 위험을
가둔다. 이것은 §11.2 워크로드 신원과 함께 풀어야 한다.

**격리 없이는 뜨지 못하게 하라.** 이미지는 컨테이너 런타임을 못 찾으면 경고를 남기고
프로세스 샌드박스로 내려간다 — 개발 편의다. 공개 환경에서는 반드시 막는다.

```bash
docker run -e REQUIRE_ISOLATION=true codedrill/runner-agent:$TAG
# → 컨테이너 런타임을 찾지 못했다: KOTLIN. ... 기동을 중단한다
```

## 태그 규칙

`<커밋 SHA 앞 12자>` 를 진실의 원천으로 삼고, 사람이 읽을 이름은 별칭으로 단다.

```
codedrill/control-plane:a1b2c3d4e5f6   ← 배포는 항상 이것으로 지정한다
codedrill/control-plane:2026.09.1      ← 릴리스 이름 (별칭)
codedrill/control-plane:dev            ← 로컬 빌드
```

`latest` 는 쓰지 않는다. **무엇이 떠 있는지 물었을 때 답이 시간에 따라 달라지는 태그는
롤백의 근거가 되지 못한다.**

세 이미지는 **같은 SHA 로 함께 올린다.** 판정 프로토콜(§5.3)과 메시지 스키마는 셋이
공유하므로, 섞어 배포하면 스키마가 어긋나는 조합이 생긴다.

## 큐 설정을 바꿀 때

**큐의 인자는 바꿔 선언할 수 없다.** 이미 있는 큐를 다른 인자로 선언하면 브로커가
`PRECONDITION_FAILED` 로 거절하고, 앱은 그 큐를 쓰지 못한 채 뜬다.

배달 한도나 dead-letter 설정을 바꾸는 릴리스는 그래서 무중단이 아니다. 순서가 있다.

1. 소비자를 모두 내린다 (큐가 비어 있어야 한다 — 남은 메시지는 사라진다)
2. 큐를 지운다: `rabbitmqctl delete_queue judge.submissions` …
3. 새 이미지를 띄운다 — 앱이 새 인자로 다시 선언한다

**메시지가 남아 있으면 지우기 전에 옮긴다.** 아웃박스에 남아 있는 것은 다시 발행되지만,
이미 발행된 것은 큐에만 있다.

## 롤백

이미지 태그를 이전 SHA 로 되돌리는 것이 기본 경로다. 다만 **되돌릴 수 없는 것이 둘**
있다.

- **DB 마이그레이션.** Flyway 는 앞으로만 간다. 이전 이미지가 새 스키마에서 돌 수 있는지가
  롤백 가능 여부를 정한다 (§15.2). 열 추가처럼 뒤로 호환되는 변경만 하고, 지우는 것은
  다음 릴리스로 미룬다.
- **공개된 문제 버전.** 공개는 포인터 교체라 되돌릴 수 있지만(§3.2), 그 사이에 받은
  제출은 그 버전으로 채점돼 있다. 되돌린다고 판정이 되돌아가지 않는다.

경보가 울렸을 때의 첫 대응은 [runbook.md](runbook.md) 에 있다.

## 레지스트리와 출처

[images.yml](../.github/workflows/images.yml) 이 넷을 만들고, **main 에 들어온 것만**
`ghcr.io/<owner>/<repo>/<이미지>:<SHA>` 로 올린다.

**만들기와 올리기를 나눈다.** 먼저 로컬로 만들어 기동을 확인하고 통과한 것만 올린다.
순서를 바꾸면 뜨지 않는 이미지가 태그를 갖고, 그 태그를 롤백 대상으로 믿게 된다.

올릴 때 SBOM 과 출처 증명(provenance)이 함께 붙는다. **비밀 키가 없다** — OIDC 로
서명하므로 보관할 키가 생기지 않는다. 키를 두면 그것이 새로운 장기 비밀이 되고, 관리자
토큰을 없앤 이유(§11.2)가 다른 자리에서 되살아난다.

```bash
gh attestation verify oci://ghcr.io/<owner>/<repo>/control-plane:<SHA> --repo <owner>/<repo>
```

이 검증이 §11.4 이미지 스캔 게이트(B4)가 설 자리다.

## 아직 없는 것

여러 노드에 걸치는 매니페스트(Kubernetes 든 무엇이든)와 스캔 게이트가 없다. 지금 서 있는
것은 **단일 호스트 compose** 다 — 앱 스택 한 벌과 Runner 노드 하나. 그 이상으로 늘릴 때가
매니페스트가 필요해지는 때다. [production-readiness.md](production-readiness.md) 의 A1 과
B4 가 이 자리를 가리킨다.
