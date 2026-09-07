# 제품 맥락

> **역할**: CodeDrill은 무엇이고, 코드를 쓸 때 어떤 스택·용어·경계를 전제하는가
> **단일 출처**: 없음 — [specs/](specs/)의 `.docx`가 원본이고 이 문서는 캐시다
> **갱신 트리거**: 기획 `.docx`가 개정되어 아래 항목과 어긋날 때, 스택·모듈 경계·용어가 바뀔 때

`.docx` 원본은 바이너리라 열어보는 비용이 크다. 이 문서는 코드를 쓸 때마다 필요한
결정만 뽑아 둔 것이다. 어긋나면 `.docx`가 이긴다.

## 한 문장

실행 증거를 바탕으로 알고리즘 역량을 분해해 진단하고, 약한 역량을 코칭하며,
새로운 문제로 전이를 검증하는 온라인 저지.

## 설계 원칙

| 원칙 | 의미 |
|---|---|
| Judge first | 제출·채점·기록이 빠르고 정확한 것이 최우선. 다른 기능이 이를 가리거나 늦추지 않는다 |
| Evidence over answers | 힌트와 AI 설명은 코드·상태·반례라는 근거로 검증 가능해야 한다 |
| Progressive disclosure | 기본 화면은 간결하게, 상세 로그와 상태는 요청할 때 연다 |
| No false precision | 데이터가 부족하면 역량 점수나 성능 우열을 단정하지 않는다 |

## 스택

| 영역 | 선택 |
|---|---|
| Web | React + TypeScript + Monaco Editor |
| API / Control Plane | Kotlin + Spring Boot |
| Judge Orchestrator | Kotlin (독립 배포 모듈) |
| Runner Agent | 경량 네이티브 프로세스 또는 JVM 데몬 |
| DB | PostgreSQL (JSONB 메타데이터 활용) |
| Cache / Rate limit | Redis |
| Job broker | RabbitMQ quorum queue |
| Artifact store | S3 호환 오브젝트 스토리지 |
| Realtime | SSE 우선, 필요 시 WebSocket |
| Observability | OpenTelemetry + Prometheus + Grafana |
| 채점 지원 언어 | Java, Kotlin, Python (언어별 `RuntimeAdapter` 구현 하나씩) |

MVP에서 **선택하지 않은 것**: 마이크로서비스 전면 분리, Kafka, 서비스 메시,
요청마다 Kubernetes Job 생성(상시 워커 풀을 쓴다).

## 배포 단위

저장소는 신뢰 경계를 따라 나뉜다. 같은 저장소에 있어도 **배포 단위는 따로다**.

| 경로 | 배포 단위 | 경계 |
|---|---|---|
| `platform/` | 라이브러리 | 세 영역이 공유하는 기반(오류 코드·아웃박스·상관관계 ID) |
| `control-plane/` | Spring Boot 앱 1개 | 도메인 모듈 8개를 조립하는 모듈형 모놀리스 |
| `judge/orchestrator`, `judge/runner-agent` | 각각 별도 앱 | Control DB·인터넷에 직접 접근하지 않는 실행 영역 |
| `judge/protocol` | 라이브러리 | 제어 영역 ↔ 실행 영역 메시지 계약 |
| `content/` | 데이터 | 문제 패키지. 실행 영역의 read-only 아티팩트 자리 |
| `web/` | 정적 자산 | React + TS + Monaco |

도메인 모듈이 서로를 직접 참조하면 `./gradlew checkModuleBoundaries` 가 빌드를 깬다.
협력은 조립 지점인 `:control-plane:app` 에서 연결한다.

**실행 영역에는 JDBC 가 들어가면 안 된다.** Runner 와 Orchestrator 는 Control DB 에
접근하지 않는다 (§2.3). 공유 모듈에 JDBC 의존성을 넣으면 이 경계가 조용히 무너진다 —
아웃박스 퍼블리셔가 `control-plane/app` 에 있는 이유다.

## 모듈 경계

제어 영역은 모듈형 모놀리스다. 각 모듈은 자기 데이터만 소유하고, 아래 금지 의존성을 넘지 않는다.

| 모듈 | 소유 데이터 | 금지 |
|---|---|---|
| Identity | user, role, consent | Judge 내부 모델 참조 |
| Problem | problem, version, competency tag, pack | Submission 테이블 직접 수정 |
| Workspace | draft, custom test, response, preference | 판정 상태 변경 |
| Submission | submission, test result summary | Runner 직접 호출 |
| Competency | ontology, evidence, mastery projection | 원본 증거 수정 |
| Coaching | session, prescription, assistance | 판정을 주관적으로 변경 |
| Learning | problem state, collection, activity | 채점 원본 로그 의존 |
| Admin | review, publish, rejudge, audit | 감사 로그 우회 |

## 문제 패키지 값 타입

`INT`, `INT_ARRAY`, `STRING`, `STRING_ARRAY`. 하네스가 이 타입으로 사용자 함수를 부르고
기대 출력과 견준다.

문자열은 프로토콜 위에서 **Base64** 로 싣는다. 케이스 파일이 탭으로 필드를, 쉼표로 배열
원소를 나누는데 문자열에는 그 둘이 그냥 들어 있을 수 있어서다. 세 언어에서 똑같이
동작하는 이스케이프를 만드는 것보다 구분자가 없는 알파벳으로 옮기는 편이 틀릴 자리가
적다. `STRING_ARRAY` 는 개수를 앞에 둔다 — 없으면 빈 배열과 "빈 문자열 하나짜리 배열"을
구분할 수 없다.

기대 출력의 타입은 **시그니처가 정한다.** 값의 JSON 모양에서 추론하면 문자열과 정수를
가를 수 없고, 무엇을 돌려주기로 했는지는 manifest 에 이미 적혀 있다.

## 용어

| 용어 | 뜻 |
|---|---|
| Drill | 역량을 세분화해 약한 능력만 연습하고 전이 문제로 검증하는 훈련 단위 |
| Competency Pack | 문제·테스트·오답·반례·해설·시각화를 묶은 콘텐츠 패키지 |
| Evidence Ledger | 제출·질문·테스트·트레이스·도움 이력을 append-only로 쌓는 증거 원장 |
| Projection | 증거에서 재계산 가능한 파생값. Level과 Confidence를 분리해 계산한다 |
| Mutant | 대표 오답·성능 결함 구현. 테스트가 이를 잡아내는 비율로 문제 품질을 잰다 |
| 검증 보고서 | §6.3 파이프라인의 산출물. 이 digest 가 맞는 버전만 공개할 수 있다 |
| Trace Event | 코드 줄이 아니라 의미 단위(compare, swap, push, enqueue…)로 기록하는 실행 이벤트 |
| Trace manifest | 트레이스 목차. 청크 범위와 요약을 담고 이벤트 본문은 담지 않는다 (§7.4) |
| Transfer | 힌트 없이 변형 문제를 푸는 것. 역량 증거의 최상위 가중치 |

## 판정 상태 머신

```
CREATED → QUEUED → LEASED → COMPILING → RUNNING → AGGREGATING → COMPLETED
```

`COMPLETED`는 불변이다. 재채점은 상태를 되돌리지 않고 판정만 갈아 끼우며 `revision`을
올린다 — 바뀌는 것은 "무엇으로 판정됐는가"이지 "끝났는가"가 아니다. 최초 판정을 포함한
모든 판정이 `submission_judgement`에 남으므로 "왜 점수가 바뀌었나"에 답할 수 있다.

지켜야 할 불변식:

- 제출 생성은 `submission`과 `outbox_event`를 한 트랜잭션으로 커밋한다.
- 상태 전이는 현재 상태 + `version` 컬럼 조건의 낙관적 갱신을 쓴다.
- 작업은 at-least-once로 전달되므로 모든 소비자는 멱등해야 한다.
- 워커 유실 후 늦게 도착한 결과는 fencing token 불일치로 거절하고 감사 기록만 남긴다.
- **임대 만료는 "워커가 죽었다"만 뜻해야 한다.** 실행을 집어 든 Runner 가 심장 박동으로
  임대를 연장하고, 아무도 집어 들지 않은 요청에는 훨씬 긴 dispatch timeout 을 따로 둔다.
  큐 대기를 임대에 넣으면 밀렸을 뿐인 제출이 유실로 처리돼 `SYSTEM_ERROR` 로 끝난다.
- 플랫폼 장애(`SYSTEM_ERROR`)를 사용자 코드 실패로 덮지 않는다.

## 신뢰 경계

사용자 소스·컴파일 산출물·사용자 출력은 모두 **비신뢰 입력**이다.
Runner는 Control Plane 자격증명을 갖지 않고, 인터넷과 Control DB로 직접 연결하지 않는다.
샌드박스는 비root UID, read-only rootfs, 네트워크 네임스페이스 분리, seccomp allowlist,
cgroup v2 CPU·메모리 제한을 전제한다.

공식 채점 실행에는 트레이스 계측을 넣지 않는다 — 학습용 트레이스는 별도 저우선순위 실행이다.

격리는 컨테이너 런타임에 위임한다. `codedrill.sandbox.require-isolation=true` 이면 런타임이
없을 때 기동을 중단하고, false 이면 경고와 함께 프로세스 샌드박스로 내려간다. **프로세스
샌드박스는 개발 편의용이며 §5.2 의 통제를 거의 갖추지 못한다.**

리플레이의 상태 리듀서는 웹에만 있다. 서버가 상태 스냅샷을 만들려면 같은 리듀서를 Kotlin
으로 한 번 더 구현해야 하는데, 두 구현이 갈라지면 "seek 한 결과와 처음부터 재생한 결과가
다르다"가 된다. 그래서 서버는 청크 경계만 목차로 주고 복원은 클라이언트가 한다.

공개는 검증 보고서 digest 대조와 2인 승인을 모두 통과해야 한다 (§3.2, §11.2). 감사 로그는
DB 트리거로 UPDATE·DELETE 를 막아 append-only 를 강제한다 (§13.3) — 애플리케이션 버그나
운영자 실수로도 지워지지 않는다.

사용자 인증은 불투명 토큰이다. 서명 토큰(JWT)이 아니라 저장소에 둔 임의 값이라 하나를 즉시
무효로 만들 수 있다 — 서명 토큰은 발급 후 만료까지 취소할 방법이 없어, 유출을 알아차려도
기다리는 것 말고 할 수 있는 일이 없다. 대신 요청마다 조회가 한 번 든다. access token 은
짧고 refresh 는 회전하며, 이미 쓴 refresh 가 다시 오면 그 사용자의 세션을 전부 끊는다.

**소유자가 아닌 자원은 404 다.** 403 은 "그 제출은 있는데 네 것이 아니다"를 알려 주고,
그것만으로 ID 를 훑어 누가 무엇을 언제 제출했는지 셀 수 있다 (§11.1 소스 노출).

관리자 API 는 별도의 운영자 토큰으로 신원을 확인하고 §11.2 의 다섯 역할로 엔드포인트를 가른다. 운영자를
설정하지 않으면 API 전체가 닫힌다 — 기본 토큰을 심어 두면 그 토큰은 반드시 어느 운영
환경에 그대로 남는다. 역할 분리와 등록자·승인자 비교는 서로를 대신하지 않는다.

재채점은 요청·승인·실행을 세 단계로 나눈다. 승인만으로는 아무것도 돌지 않으므로 "승인했지만
아직 돌지 않은" 구간이 상태로 드러난다. dry-run 은 판정을 바꾸지 않고 무엇이 바뀔지만 본다.

이전 슬라이스가 `X-User-Id` 헤더로 만든 제출·초안은 어떤 계정에도 속하지 않는다. 새 사용자로
옮기지 않고 그대로 둔다 — 소유자가 없는 행은 어떤 로그인으로도 열리지 않으며, 이것이 의도한
결과다.

시스템 호출은 언어별 allowlist 로 막는다. 목록에 없으면 `EPERM` 이며, 죽이지 않는 이유는
목록이 조금 모자랄 때 멀쩡한 제출이 사용자 코드 탓처럼 실패하기 때문이다. 프로파일은
JSON 이 아니라 소스에 둔다 — 왜 허용하는지가 함께 있어야 몇 달 뒤에도 줄일 수 있다.
컨테이너 런타임이 사용자 코드를 띄우기 전에 쓰는 `capset`·`setpgid`·`getpgid` 는 예외이며,
`--cap-drop ALL` 과 `no-new-privileges` 가 그 힘을 이미 없앴기 때문에 안전하다.

컴파일은 여전히 Runner 호스트에서 돈다 — 파서만 도는 단계지만 §5.5 는 이것도
런타임 이미지 안에서 하기를 요구한다. 관리자 토큰은 설정에 든 장기 비밀이라, Identity 모듈이
붙으면 워크로드 ID 와 짧은 수명 토큰으로 옮겨야 한다.
