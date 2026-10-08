# 기술 선택의 이유와 대안

> **역할**: 초기 스택의 각 선택은 무엇을 필요로 했고, 어떤 대안과 견줘 무엇을 감수했는가
> **단일 출처**: 없음 — 결정 자체는 [specs/code_drill_technical_design.docx](specs/code_drill_technical_design.docx)(§0.1, §18.2, 부록 C)가 원본이고, 이 문서는 그 이유와 대안 비교를 한곳에 모은다
> **갱신 트리거**: 스택 선택이 바뀌거나 §18.2의 재검토 조건이 충족되어 결정을 다시 볼 때

이 문서는 2026-10-05에 작성했다. 첫 커밋 `becfe5c`부터 첫날의 마지막 커밋 `703dc42`까지(2026-09-05)에
보이는 선택과 기존 문서(기술 설계서 v1.2, PRD v1.2, [project-context.md](project-context.md))를 바탕으로
이유를 재구성한 것이다. 각 항목의 "근거" 줄은 그 이유가 저장소 문서에 적혀 있는지(원문), 아니면
이 문서에서 재구성한 것인지(재구성)를 밝힌다. 기술의 성질은 아래 참고 링크의 공식 문서에서 가져왔다.

## 판단 기준이 된 요구

선택을 비교한 기준은 기술 설계서와 PRD가 정한 아래 요구다.

| 요구 | 출처 |
|---|---|
| 품질 속성 우선순위: 판정 정확성 → 격리·보안 → 가용성 → 응답성 → 관측성 → 확장성 | 기술 설계서 §0.2 |
| 초기 용량 가정: 동시 사용자 300명, 피크 제출 20건/초 | §1.2 |
| 채점 작업은 중복 전달될 수 있고 모든 소비자는 멱등 | §1.2, ADR-003 |
| 작업 유실 0, 판정 재현성 99.99%, SYSTEM_ERROR 0.1% 미만 | §12.1, §12.3, PRD §0.5 |
| 큐 대기 p95 2초, 판정 반영 p95 1초 | §12.1 |
| 사용자 코드는 비신뢰 입력, Runner는 Control DB·인터넷에 직접 연결하지 않음 | §2.3, §5.1 |
| 운영 복잡도 과잉을 위험으로 관리 (완화: 모듈형 모놀리스, 구성요소 최소화) | §18.1 |
| 채점 언어: Java, Kotlin, Python | §1.1 |

## 1. 구조: 모듈형 모놀리스 + 독립 Judge

근거: 원문(§18.2, 부록 C ADR-001·002, §18.1). 세 번째 대안은 재구성.

| 대안 | 요구에 비춘 평가 |
|---|---|
| 전면 마이크로서비스 | 도메인 8개를 처음부터 서비스로 나누면 배포·관측·분산 트랜잭션 비용이 먼저 든다. §18.1이 "운영 복잡도 과잉"을 위험으로 꼽았고, §18.2는 "팀/배포 충돌이 측정됨"을 재검토 조건으로 둔다 |
| 채점까지 포함한 단일 앱 (재구성) | 가장 단순하지만 사용자 코드를 실행하는 프로세스가 Control DB 자격증명과 같은 경계에 놓인다. §5.1·INV-04와 충돌한다 |
| 선택: 제어 영역은 모놀리스, 실행 영역은 별도 배포 | 신뢰 경계가 다른 곳만 나눈다. 제어 영역 안의 경계는 `checkModuleBoundaries` 태스크로 빌드에서 강제한다(`f68dbaf`) |

감수한 것: 제어 영역 모듈은 한 번에 배포된다. 모듈 경계는 런타임이 아니라 빌드 검사로만 지켜지므로, 검사를 우회하는 의존(예: 같은 DB 테이블을 직접 읽기)은 리뷰가 잡아야 한다.

## 2. 언어·프레임워크: Kotlin + Spring Boot

근거: §0.1의 이유("도메인 모델링, 트랜잭션, 운영 생태계, 팀 생산성")는 원문. 대안 비교는 재구성.

| 대안 | 요구에 비춘 평가 |
|---|---|
| Java + Spring Boot (재구성) | 같은 생태계라 트랜잭션·AMQP·JDBC 지원은 동일하다. 차이는 언어 표현력과 null 안전성 정도다. Kotlin은 Java 기반 프레임워크와 완전히 호환되므로 생태계를 잃지 않는다 |
| Go (재구성) | Runner처럼 프로세스를 띄우고 자원을 재는 데몬에는 후보가 된다. 그러나 채점 언어 셋 중 둘(Java, Kotlin)이 JVM이고, 첫 슬라이스는 `kotlin-compiler-embeddable`로 Runner 프로세스 안에서 Kotlin을 컴파일했다(`857bf3f`). 제어·실행 영역이 언어를 달리하면 `judge/protocol` 계약을 두 언어로 유지해야 한다 |
| Node.js + TypeScript (재구성) | 웹과 언어를 맞출 수 있지만, §3.2의 트랜잭션 경계(submission + outbox 한 트랜잭션, version 조건 갱신)와 RabbitMQ·Flyway 통합을 직접 조립해야 한다 |
| 선택: Kotlin + Spring Boot (3.4 로 시작, 4.1 로 올림) | Spring이 Kotlin을 1급으로 지원하고, 제어·Orchestrator·Runner 셋이 한 Gradle 빌드와 한 계약 모듈을 공유한다 |

감수한 것: JVM 기동과 메모리 비용. Kotlin 컴파일 지연은 §18.1이 가능성 "높음"으로 적은 위험이다. 첫날 Runner는 `kotlin-compiler-embeddable`이 fat jar 안에서 자기 설정을 찾지 못해 `installDist` 배포로 바꿔야 했다(`48b188a`).

## 3. 주 저장소: PostgreSQL

근거: §0.1의 이유("트랜잭션, 버전 관리, JSONB 메타데이터")는 원문. 대안 비교는 재구성.

| 대안 | 요구에 비춘 평가 |
|---|---|
| MySQL (재구성) | 트랜잭션, `SKIP LOCKED`, JSON 타입을 모두 갖춰 기능상 큰 차이는 없다. 이 저장소가 PostgreSQL을 고른 이유로 문서에 남은 것은 JSONB뿐이며, MySQL을 배제한 근거는 기록되어 있지 않다 |
| MongoDB (재구성) | 다중 문서 트랜잭션을 지원하지만 공식 문서가 단일 문서 쓰기보다 비용이 크다고 하고 비정규화 설계를 권한다. 이 제품의 핵심은 submission·outbox·execution_attempt 사이의 유일 제약과 한 트랜잭션 커밋(§3.2, §8.2)이라 관계형 모델이 더 맞는다 |
| 선택: PostgreSQL 17 | `UNIQUE(user_id, idempotency_key)`, version 조건 갱신, 아웃박스 발행기의 `FOR UPDATE SKIP LOCKED`(`48b188a`), 판정 그룹의 `JSONB` 컬럼(`V2__submission.sql`)을 한 저장소에서 쓴다 |

감수한 것: 아웃박스를 DB 테이블로 두므로 발행 지연과 폴링 부하가 DB에 얹힌다. 소스·로그·트레이스는 DB 밖(오브젝트 스토어)에 두기로 했지만(§8.3), 첫 슬라이스는 소스를 테이블과 메시지에 직접 담았다.

## 4. 캐시·쿼터: Redis

근거: §0.1의 용도("세션 보조, 쿼터, 짧은 상태 캐시")와 "진실의 원천은 DB"(§10.1, compose 주석)는 원문. 대안 비교는 재구성.

| 대안 | 요구에 비춘 평가 |
|---|---|
| 프로세스 내부 메모리 (재구성) | 인스턴스가 하나면 충분하다. 첫 슬라이스의 `AttemptRegistry`와 SSE 구독 목록이 실제로 이렇게 시작했고, 코드 주석이 replica가 늘면 Redis 등으로 옮겨야 한다고 적는다 |
| PostgreSQL 카운터 (재구성) | 저장소를 하나 줄이지만, 제출마다 쿼터 갱신이 주 DB의 쓰기 부하가 된다 |
| 선택: Redis | 원자적 카운터로 쿼터를, 짧은 TTL로 상태 캐시를 맡긴다. 로컬 compose는 RDB·AOF를 모두 끈다 — 잃어도 DB에서 다시 만들 수 있는 값만 둔다는 전제다 |

감수한 것: 구성요소가 하나 늘어난다. §12.2는 Redis 장애 시 "DB/로컬 제한, 보수적 허용"으로 폴백한다. 첫날 코드는 Redis 연결만 설정했고 실제로 쓰지 않았다.

## 5. 작업 브로커: RabbitMQ quorum queue

근거: 원문(§0.1 "작업 확인·재전달·우선순위·운영 단순성", §18.2 보류 대안 Kafka·Redis Streams와 재검토 조건, §0.3 Kafka 비선택). 아래 성질 비교는 공식 문서.

| 대안 | 요구에 비춘 평가 |
|---|---|
| Kafka | 소비 후에도 이벤트를 보존하고 파티션 안 순서를 보장한다. 재처리·분석에는 강하다. 그런데 채점 작업에 필요한 것은 보존보다 작업 하나하나의 확인·재전달·우선순위이고, §0.1은 이것을 RabbitMQ를 고른 이유로 적는다. §17.3은 "이벤트 소비자·재처리 급증"이 오면 도메인 이벤트만 Kafka 계열로 분리하는 것을 검토한다 |
| Redis Streams | 소비자 그룹, PEL, `XACK`, `XAUTOCLAIM`으로 작업 큐를 만들 수 있다. 다만 내구성이 Redis 영속화 설정(AOF fsync 주기 등)에 묶인다. "작업 유실 0"(§12.3)을 Redis에 맡기면 Redis를 캐시로만 쓰려는 4번의 전제와 충돌한다 |
| 선택: RabbitMQ quorum queue | Raft 기반 복제 큐로 데이터 안전을 위해 설계되었고, 수동 ack + publisher confirm으로 at-least-once를 만든다. 재배달 횟수를 추적하고 우선순위를 지원한다. 첫날 네 큐(`judge.submissions`, `executions`, `results`, `progress`)를 모두 durable quorum으로 선언했다 |

감수한 것: 메시지는 ack 후 사라지므로 과거 작업을 다시 읽는 재처리는 브로커가 아니라 DB(아웃박스·제출 행)에서 해야 한다. 중복 전달은 소비자 멱등성과 fencing token으로 흡수한다(§4.3).

## 6. 실시간 전송: SSE

근거: 원문(§0.1 "단방향 상태 갱신에 단순하고 재연결이 쉬움", §18.2 보류 대안 WebSocket, 재검토 조건 "양방향 협업 기능 도입"). 폴링 대안은 재구성.

| 대안 | 요구에 비춘 평가 |
|---|---|
| WebSocket | 양방향이 필요할 때의 선택이다. 판정 진행은 서버→클라이언트 단방향이라 그 능력을 쓰지 않는다 |
| 짧은 주기 폴링 (재구성) | 가장 단순하지만 판정 반영 p95 1초(§12.1)를 맞추려면 1초 이하 주기로 조회해야 한다 |
| 선택: SSE | 브라우저가 끊기면 자동 재연결하고 `Last-Event-ID`를 보낸다. Spring MVC `SseEmitter`로 바로 쓸 수 있다 |

감수한 것: 스트림은 진실의 원천이 아니다. 첫날 클라이언트는 SSE 이벤트마다 제출을 재조회해 DB 상태로 수렴하도록 만들었다(`4ae960d`). 구독은 단일 인스턴스 메모리에 있어 replica가 늘면 팬아웃 수단이 필요하다.

## 7. 샌드박스: gVisor 계열 우선 검증

근거: 원문(§18.2 선택 "gVisor 등 격리 컨테이너", 보류 대안 "일반 runc, Firecracker", 재검토 조건 "보안 등급 또는 밀도 변화", ADR-008 Proposed, §0.3 "요청마다 Kubernetes Job 생성 비선택").

| 대안 | 요구에 비춘 평가 |
|---|---|
| 일반 runc 컨테이너 + seccomp | Docker 기본 seccomp 프로필은 300여 개 시스템 호출 중 약 44개를 막는 허용 목록이다. 시스템 호출은 결국 호스트 커널로 간다. §11.1이 "샌드박스 탈출"을 가능성 중·영향 매우 높음으로 둔다 |
| Firecracker microVM | KVM으로 microVM을 만들어 가상 머신 경계로 격리한다. 하드웨어 가상화 지원 CPU가 필요하고, §17.3은 "격리 수준 요구 상승" 시에 microVM pool로 강화하는 것을 다음 단계로 둔다 |
| 선택: gVisor 계열 | 사용자 공간 응용 커널이 시스템 호출을 가로채 호스트 커널에 그대로 넘기지 않는다. OCI 런타임(`runsc`)이라 기존 컨테이너 도구를 그대로 쓴다 |

감수한 것: 공식 문서가 밝히는 호환성 저하와 시스템 호출당 오버헤드. 첫날 Runner는 이 중 무엇도 아직 쓰지 않았다. 별도 JVM, 힙 상한, 벽시계 데드라인, 출력 한도까지만 강제했고 `SandboxProcess` 주석이 나머지(§5.2)가 비어 있음을 명시한다.

## 8. 실행 트레이스: SDK·규약 계측

근거: 원문(§18.2 보류 대안 "범용 AST/디버거 추론", §0.3, ADR-004).

| 대안 | 요구에 비춘 평가 |
|---|---|
| 범용 AST 변환·디버거 추론 | 코드를 고치지 않아도 되지만 세 언어의 임의 코드에서 compare·swap 같은 의미 이벤트를 안정적으로 뽑아야 한다. PRD 비목표("임의의 모든 사용자 코드를 완벽하게 의미 분석한다고 약속하지 않습니다")와 맞지 않는다 |
| 선택: 문제별 계측 규약 + SDK | 같은 `Drill` API를 판정 모드에서는 no-op으로 컴파일해 계측 오버헤드가 판정 시간에 섞이지 않는다(`00a6aec`) |

감수한 것: 사용자가 `Drill.*`를 호출하지 않으면 리플레이가 없다. 트레이스는 공개 그룹 첫 케이스에서만 만든다.

## 9. 웹: React + TypeScript + Monaco

근거: §0.1의 이유("문제·편집·리플레이 UI 생태계와 타입 안정성")와 Monaco를 CDN이 아니라 번들에서 쓰는 이유(`4ae960d`)는 원문. CodeMirror 대안은 재구성.

| 대안 | 요구에 비춘 평가 |
|---|---|
| CodeMirror 6 (재구성) | 모듈식이고 모바일의 기본 선택·편집 기능을 쓰며 스크린 리더 지원을 내세운다. 대신 VS Code와 같은 편집 경험은 확장을 조립해 만들어야 한다 |
| 선택: Monaco | VS Code의 편집기를 그대로 쓴다. 공식 저장소가 모바일 브라우저를 지원하지 않는다고 밝힌다 — PRD가 "모바일 전체 코딩 경험"을 MVP에서 제외했으므로 이 비용을 받아들일 수 있다 |

감수한 것: 무게. 첫날 Monaco를 별도 청크로 지연 로드해 메인 번들을 4.2MB에서 203KB로 줄였다(`4ae960d`).

## 10. 관측성: OpenTelemetry + Prometheus + Grafana

근거: §0.1의 이유("요청-제출-실행의 종단 추적")는 원문. 대안 비교는 재구성.

| 대안 | 요구에 비춘 평가 |
|---|---|
| 특정 벤더의 APM 에이전트 (재구성) | 계측을 벤더 도구에 맞추면 백엔드를 바꿀 때 계측도 바꿔야 한다. 이 대안을 검토한 기록은 저장소에 없다 |
| 선택: OpenTelemetry로 생성, Prometheus·Grafana로 저장·조회 | OpenTelemetry는 벤더 중립 계측·내보내기 도구이고 스스로 백엔드가 아니다. 그래서 백엔드를 따로 고른다 |

감수한 것: 수집·저장·대시보드를 직접 운영한다. 첫날은 Micrometer·OTLP 의존성과 상관관계 ID 체인(`CorrelationIds`)만 두었고 대시보드는 없었다.

## 11. 대용량 객체: S3 호환 오브젝트 스토리지

근거: 원문(§0.1, §8.3 "소스, 컴파일 로그, 테스트 원본, stdout/stderr, 트레이스는 DB에 직접 저장하지 않고 객체 참조와 digest만 보관"). 대안 비교는 생략한다 — 저장소 문서에 다른 후보가 없고, 재구성할 만한 실질적 경쟁 대안(DB에 바이너리 저장)은 §8.3이 이미 배제했다.

감수한 것: 첫 슬라이스는 이 원칙을 아직 따르지 않았다. 소스를 `submission.source` 컬럼과 `SubmissionQueued` 메시지에 직접 실었고, 두 곳의 주석이 운영에서는 참조와 digest로 바꿔야 한다고 적는다. 로컬 compose는 MinIO를 띄운다.

## 참고

- RabbitMQ, [Quorum Queues](https://www.rabbitmq.com/docs/quorum-queues), [Consumer Acknowledgements and Publisher Confirms](https://www.rabbitmq.com/docs/confirms)
- Apache Kafka, [Introduction](https://kafka.apache.org/intro)
- Redis, [Redis Streams](https://redis.io/docs/latest/develop/data-types/streams/), [Redis persistence](https://redis.io/docs/latest/operate/oss_and_stack/management/persistence/)
- PostgreSQL 17, [SELECT — The Locking Clause](https://www.postgresql.org/docs/17/sql-select.html), [JSON Types](https://www.postgresql.org/docs/17/datatype-json.html)
- MySQL 8.4, [Locking Reads](https://dev.mysql.com/doc/refman/8.4/en/innodb-locking-reads.html)
- MongoDB, [Transactions](https://www.mongodb.com/docs/manual/core/transactions/)
- WHATWG HTML, [Server-sent events](https://html.spec.whatwg.org/multipage/server-sent-events.html); IETF [RFC 6455 The WebSocket Protocol](https://datatracker.ietf.org/doc/html/rfc6455)
- Spring Framework, [Kotlin](https://docs.spring.io/spring-framework/reference/languages/kotlin.html), [Asynchronous Requests (SseEmitter)](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-async.html)
- Kotlin, [Kotlin for server side](https://kotlinlang.org/docs/server-overview.html)
- gVisor, [What is gVisor?](https://gvisor.dev/docs/); [Firecracker](https://firecracker-microvm.github.io/); Docker, [Seccomp security profiles](https://docs.docker.com/engine/security/seccomp/)
- [Monaco Editor](https://github.com/microsoft/monaco-editor); [CodeMirror](https://codemirror.net/)
- OpenTelemetry, [What is OpenTelemetry?](https://opentelemetry.io/docs/what-is-opentelemetry/)
