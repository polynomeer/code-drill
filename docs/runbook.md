# 운영 런북

> **역할**: 경보가 울렸을 때 무엇을 보고 무엇을 하는가
> **단일 출처**: 경보별 첫 대응, 장애 주입 훈련 절차, 롤백·재채점·복구 경로
> **갱신 트리거**: 경보 규칙이 늘거나, 복구 절차·훈련 시나리오가 바뀔 때

경보 규칙과 임계값은 [`deploy/observability/alerts.yml`](../deploy/observability/alerts.yml)
이 단일 출처다. 이 문서는 **울린 다음에 무엇을 하는가**만 정한다. 각 규칙의 `runbook`
주석이 아래 절을 가리킨다.

## 대시보드 켜기

```bash
docker compose -f deploy/docker-compose.yml --profile observability up -d
```

Grafana `http://localhost:3001`, Prometheus `http://localhost:9090`,
Alertmanager `http://localhost:9093`.
대시보드 패널 순서가 조사 순서다 — SLO 위반 → 판정 품질 → 큐 → Runner → 일관성.

앱은 호스트에서 돌고 Prometheus 는 컨테이너에서 돈다. 타깃이 전부 down 이면
`host.docker.internal` 이 풀리지 않는 것이므로, Linux 에서는 compose 의 `extra_hosts`
가 살아 있는지 본다.

## 경보는 어디로 가나

규칙(`alerts.yml`)은 **무엇이 잘못됐는지**를 정하고, 라우팅
([`alertmanager.yml`](../deploy/observability/alertmanager.yml))은 **그것이 누구에게 언제
가는지**를 정한다. 둘 중 하나만 있으면 경보 체계는 없는 것과 같다.

| severity | 어디로 | 다시 알림 | 뜻 |
|---|---|---|---|
| `page` | 당번 | 4시간 | 지금 사람이 봐야 한다 |
| `ticket` | 백로그 | 24시간 | 근무 시간에 처리한다 |
| `watchdog` | 배달 감시 | 5분 | 아래 참고 |
| (없음) | unrouted | 24시간 | 라벨을 빠뜨린 규칙 |

같은 문제를 `alertname` 과 `plane` 으로 묶어 한 번만 알린다. 인스턴스별로 묶으면 장애
한 번에 알림이 인스턴스 수만큼 오고, 사람은 곧 그것을 무시하는 법을 배운다. `page` 가
떠 있는 동안 같은 대상의 `ticket` 은 억제한다 — 큰 장애에 딸려 오는 작은 신호가 호출
위에 쌓이면 정작 봐야 할 것이 묻힌다.

**라벨 없는 경보도 어딘가로 간다.** 조용히 버려지면, 라벨을 빠뜨린 새 규칙은 만들어
놓고도 아무도 모른다.

<a id="alert-routing"></a>
### alert-routing

**증상**: `Watchdog` 이 오지 않는다.

`Watchdog` 은 항상 켜져 있는 경보다. 문제가 없어서 조용한 것과 **배달 경로가 끊겨서
조용한 것**은 화면에서 똑같아 보이는데, 이것이 둘을 가른다. 받는 쪽에서 "5분 안에 오지
않으면 알린다"로 걸어 둔다.

오지 않으면 위에서부터 짚는다.

1. Prometheus 가 Alertmanager 를 찾았나 — `curl -s localhost:9090/api/v1/alertmanagers`
2. 규칙이 켜져 있나 — `curl -s localhost:9090/api/v1/alerts`
3. Alertmanager 까지 왔나 — `curl -s localhost:9093/api/v2/alerts`
4. 배달이 됐나 — `alertmanager_notifications_failed_total`

**수신자가 아직 채워지지 않았다면 4에서 멈춘다.** 기본 설정의 주소는 절대 풀리지 않는
`.invalid` 다 — 빈 수신자로 두면 배달이 조용히 성공한 것처럼 보이기 때문이다. 실제
주소로 바꾸는 것이 이 항목을 닫는 일이며, **누가 당번인가는 조직이 정한다.**

## 경보별 첫 대응

### queue-lag

**증상**: 발행되지 않은 아웃박스 이벤트가 쌓인다. 사용자에게는 "제출했는데 채점이
시작되지 않는다"로 보인다.

제출은 아웃박스와 함께 커밋되므로 **유실은 아니다**. 늦을 뿐이다. 이 사실을 먼저
확인하고 나서 원인을 찾는다.

1. 브로커가 살아 있는지 — `docker compose -f deploy/docker-compose.yml ps rabbitmq`
2. 제어 영역이 발행하고 있는지 — 로그의 `OutboxPublisher`, 대시보드의 "아웃박스 적체"
3. Runner 가 소비하고 있는지 — `codedrill_runner_execute_seconds_count` 가 증가하는가
4. 큐 깊이는 늘고 소비는 도는데도 밀린다면 **용량 문제다**. → [capacity](#capacity)

브로커를 되살리면 아웃박스가 스스로 재발행한다. 손으로 재발행하지 않는다 — 같은
이벤트를 두 번 밀게 되고, 멱등성이 흡수하더라도 원인 추적이 어려워진다.

### dead-letter

**증상**: `judge.*.dead` 큐에 메시지가 쌓인다. 그 제출들은 SYSTEM_ERROR 로 끝나 있다.

처리할 수 없다고 판단된 메시지다. **큐는 막히지 않는다** — 그것이 dead 큐를 둔 이유다.
급한 불은 없으니 원인부터 본다.

```bash
docker exec code-drill-rabbitmq-1 rabbitmqctl list_queues name messages | grep dead
```

1. 왜 치웠는지는 소비자 로그에 있다 — `처리할 수 없는 메시지다` 또는 배달 한도 초과
2. 한 문제에 몰려 있으면 **콘텐츠 문제다.** 그 버전이 공개돼 있는지, 패키지가 디스크에
   있는지 본다
3. 여러 문제에 흩어져 있으면 **메시지 스키마가 어긋난 것이다.** 배포된 세 이미지가 같은
   SHA 인지 확인한다 (§15.3)

고친 뒤에는 [rejudge](#rejudge) 로 되돌린다. dead 큐의 메시지를 손으로 다시 넣지
않는다 — 그 사이 제출은 이미 SYSTEM_ERROR 로 끝났고, 다시 넣으면 같은 제출에 두 개의
판정 경로가 생긴다.

dead 큐의 메시지는 7일 뒤 사라진다. 그 전에 원인을 봐야 한다.

### seccomp

**증상**: 특정 언어·풀이만 `Operation not permitted` 로 실패한다.

허용 목록(`SeccompProfile`)이 그 런타임이 실제로 쓰는 호출을 덮지 못한 것이다. 사용자
코드 탓처럼 보이지만 우리 쪽 문제다.

1. 같은 코드를 프로파일 없이 돌려 재현되는지 본다 — 재현되면 seccomp 문제가 아니다
2. 컨테이너에서 `strace` 로 막힌 호출을 찾는다. 런타임 이미지에 없으면 `-alpine` 대신
   도구가 든 이미지로 한 번만 재현한다
3. `SeccompProfile` 에 **왜 필요한지와 함께** 추가한다. 근거 없이 늘어난 목록은 다시
   줄일 수 없다
4. `SeccompProfileTest` 가 금지 목록과 겹치지 않는지 확인한다

급하면 `codedrill.sandbox.require-isolation=false` 로 내려갈 수 있지만, 그러면 격리가
통째로 빠진다. **신뢰할 수 없는 코드를 받는 환경에서는 선택지가 아니다.**

### system-error-spike

**증상**: SYSTEM_ERROR 비율이 오른다. 사용자 코드의 실패가 아니라 **우리 쪽 실패**다
(§4.4).

1. 언어별로 갈리는가 — 갈리면 그 언어의 런타임 이미지 배포를 먼저 본다
2. 특정 노드에서만 나는가 — 그 노드를 빼고 재현되는지 본다
3. 최근 배포가 있으면 canary 를 멈추고 되돌린다 → [rollback](#rollback)
4. 임대 회수가 함께 늘었다면 워커 유실이다 → [worker-loss](#worker-loss)

**SYSTEM_ERROR 로 끝난 제출은 재채점 대상이다.** 사용자 잘못이 아니므로 기록에
남겨 두지 않는다 → [rejudge](#rejudge)

### worker-loss

**증상**: 임대 회수가 잦다.

회수 자체는 정상 동작이다 — 워커가 죽어도 판정이 끝난다는 뜻이다. 잦다는 것이 문제다.

1. Runner 프로세스가 실제로 죽는가 — OOM kill, 노드 축출, 배포 중 종료
2. 죽지 않는데도 회수된다면 **임대가 너무 짧다.** `codedrill.judge.lease-seconds` 는
   심장 박동 주기(5초)의 두 배보다 충분히 커야 한다
3. `dispatch-timeout-seconds` 초과로 회수된다면 워커 유실이 아니라 용량 부족이다
   → [capacity](#capacity)

임대 시간이 재는 것은 **실행 시간이 아니라 워커의 생존**이다. 실행이 오래 걸려도
박동이 오는 한 회수되지 않으므로, 느린 문제 때문에 임대를 늘릴 이유는 없다.

### conflicting-result

**증상**: 종료된 제출에 다른 판정 결과가 도착했다. **한 건이라도 조사한다.**

fencing 이 막았으므로 사용자에게 보인 판정은 바뀌지 않았다. 문제는 왜 두 실행이
서로 다른 결과를 냈는가다 — 판정 재현성(§12.1 99.99%)이 깨졌다는 뜻일 수 있다.

1. 감사 로그에서 해당 제출의 실행 이력을 뽑는다
2. 두 결과의 `resultDigest` 와 `problemVersionId` 를 비교한다
3. 문제 버전이 다르면 공개 포인터가 실행 중에 바뀐 것이다 (§3.2)
4. 같은 버전인데 결과가 다르면 **재현성 결함이다.** 골든 코퍼스로 재현을 시도하고,
   재현되면 출시를 막는다 (§14.4 Correctness)

### verdict-anomaly

**증상**: 특정 언어·문제의 판정 분포가 기준선에서 벗어났다.

1. 최근 문제 패키지 공개가 있었는가 — `GET /api/v1/admin/audit`
2. 최근 런타임 이미지 배포가 있었는가
3. 둘 다 아니면 사용자 유입의 변화일 수 있다. 성급히 되돌리지 않는다

콘텐츠가 원인이면 되돌리는 방법은 이전 버전을 다시 공개하는 것이다 → [rollback](#rollback)

### trace-overload

**증상**: 트레이스 이벤트가 예산에서 대량으로 잘리거나, 가공이 느리다.

**판정에는 영향이 없다** (§12.2). 먼저 그 사실을 확인하고 안심한 뒤 다룬다.

1. 판정 SLO 가 함께 나빠졌는가 — 아니라면 격리가 제대로 서 있는 것이다
2. 특정 문제에서만 잘리는가 — 그 문제의 계측이 과하다. 콘텐츠 수정 대상이다
3. 전반적으로 느리면 트레이스 요청을 일시적으로 줄인다. 제출 시 `requestTrace=false`
   가 기본이 되도록 클라이언트를 내리는 것이 가장 빠른 완화다

### db-saturation

**증상**: DB 커넥션을 기다리는 스레드가 있다. 제출 수락 지연이 함께 오른다.

1. 일관성 점검이 돌고 있는가 — 전수 검사에 가까운 쿼리라 주기를 늘려 완화할 수 있다
   (`codedrill.consistency.interval-ms`)
2. 느린 쿼리를 찾는다 — `pg_stat_activity` 에서 대기 중인 것
3. 읽기 부하면 목록·검색을 먼저 제한한다. 제출 경로를 마지막까지 지킨다

### consistency

**증상**: 일관성 점검이 어긋난 행을 찾았다 (§12.4).

```bash
curl -H "Authorization: Bearer $JUDGE_OPERATOR_TOKEN" \
  http://localhost:8080/api/v1/admin/consistency
```

| 점검 | 뜻 | 첫 대응 |
|---|---|---|
| `queued_without_outbox` | 아웃박스를 우회해 제출이 만들어졌다 | 코드 경로를 찾는다. 그 제출은 영영 채점되지 않는다 |
| `queued_unpublished` | 발행이 멈췄다 | [queue-lag](#queue-lag) |
| `stuck_in_flight` | 임대 회수가 동작하지 않는다 | 오케스트레이터가 재시작됐는지 본다. 회수 대상은 메모리에 있다 |
| `completed_without_verdict` | 종료됐는데 판정이 없다 | 해당 제출을 재채점한다 → [rejudge](#rejudge) |
| `orphan_trace` | 없는 제출의 트레이스 | 무해하다. 보존 기간 정리에서 함께 지운다 |
| `published_version_missing` | 공개 포인터가 빈 곳을 가리킨다 | 목록에는 보이는데 열리지 않는다. 즉시 이전 버전으로 되돌린다 |

**자동 복구는 없다.** 어긋난 이유를 모르는 채로 고치면 원인은 남고 증거만 사라진다.

## 복구 경로

### rollback

문제 콘텐츠를 되돌리는 것은 이전 버전을 **다시 공개**하는 것이다. 버전 행은 불변이라
지우거나 고치지 않는다 (§3.2).

```bash
curl -X POST -H "Authorization: Bearer $PUBLISHER_TOKEN" -H 'Content-Type: application/json' \
  -d '{"version": 1, "reportDigest": "<그 버전의 보고서 digest>", "validatorVersion": "<그때의 파이프라인 버전>"}' \
  http://localhost:8080/api/v1/admin/problems/<problemId>/publish
```

`reportDigest` 는 그 버전을 등록할 때 쓴 §6.3 보고서의 digest이고, `validatorVersion` 은 그
보고서를 만든 파이프라인의 버전이다 (§15.3). 둘 다 등록 시점의 감사 로그에 있다.

```bash
curl -H "Authorization: Bearer $PUBLISHER_TOKEN" \
  "http://localhost:8080/api/v1/admin/audit?subject=<problemId>@<version>"
```

`PROBLEM_VERSION_REGISTERED` 행의 `detail` 에 두 값이 함께 있다. 파이프라인이 바뀌어 다시
검증한 적이 있으면 `PROBLEM_VERSION_REVALIDATED` 행이 더 최근이고, 그 행의 값을 쓴다.
`GET /api/v1/admin/problems/{id}` 는 공개 포인터와 버전 수만 돌려주므로 여기서는 쓸 수 없다.

**공개는 등록자와 다른 사람이 해야 한다** (§11.2) — 되돌리는 상황에서도 예외를 두지 않는다.

되돌리려는 보고서가 지금 파이프라인보다 옛 버전이면 공개가 거부된다. 그때는 값을 억지로
맞추지 말고 `validateContent` 를 다시 돌려 같은 버전으로 등록한 뒤 공개한다 — 옛 기준이
통과시킨 문제를 지금 기준으로 다시 보지 않고 내보내는 것이 사고의 시작이다.

문제를 아예 감추려면 archive 한다. 이미 채점된 제출이 참조하므로 행은 남는다.

### rejudge

재채점은 이미 사용자에게 보여 준 판정을 바꾸는 행위다. 요청과 승인을 다른 사람이 한다.

**먼저 dry-run 으로 무엇이 바뀔지 본다.** 몇 명의 점수가 움직이는지 모르는 채로 대량
재채점을 돌리지 않는다.

```bash
# 1. 요청 (JUDGE_OPERATOR). dryRun 이면 판정을 바꾸지 않고 바뀔 것만 본다
curl -X POST -H "Authorization: Bearer $JUDGE_OPERATOR_TOKEN" -H 'Content-Type: application/json' \
  -d '{"scope": "problem:two-sum", "reason": "런타임 이미지 결함으로 SYSTEM_ERROR 다발", "dryRun": true}' \
  http://localhost:8080/api/v1/admin/rejudges

# 2. 승인 (REVIEWER, 요청자와 다른 사람)
curl -X POST -H "Authorization: Bearer $REVIEWER_TOKEN" \
  http://localhost:8080/api/v1/admin/rejudges/<id>/approve

# 3. 실행. 승인만으로는 아무것도 돌지 않는다
curl -X POST -H "Authorization: Bearer $JUDGE_OPERATOR_TOKEN" \
  http://localhost:8080/api/v1/admin/rejudges/<id>/dispatch

# 4. 진행과 결과. changes 가 바뀐(또는 바뀔) 판정이다
curl -H "Authorization: Bearer $JUDGE_OPERATOR_TOKEN" \
  http://localhost:8080/api/v1/admin/rejudges/<id>
```

dry-run 결과가 납득되면 `dryRun` 없이 같은 절차를 한 번 더 돌린다. 요청·승인·실행이
세 단계인 것은 번거로우라고 둔 것이다 — 되돌릴 수 없는 일은 한 번의 클릭으로
시작되지 않아야 한다.

**제출은 덮어쓰이지 않는다.** 재채점은 `revision` 을 올리고 이전 판정을 이력에 남긴다
(§4.2 INV-02). 사용자는 `GET /api/v1/submissions/{id}/judgements` 로 무엇에서 무엇으로
바뀌었는지 직접 볼 수 있으므로, "왜 점수가 바뀌었나"에 답이 필요하면 그 링크를 준다.

작업이 `RUNNING` 에서 멈춰 있으면 대상 중 일부가 아직 돌아오지 않은 것이다. 큐를 먼저
보고([queue-lag](#queue-lag)), 큐가 비어 있는데도 멈춰 있으면 [consistency](#consistency)
의 `stuck_in_flight` 를 확인한다.

### restore

DB 복구 목표는 RPO 15분 / RTO 60분이다 (§12.3). **복원이 끝났다는 것은 프로세스가 뜬
것이 아니라 데이터가 맞아떨어진다는 뜻이다.**

```bash
python3 scripts/backup.py list                    # 무엇이 있나
python3 scripts/backup.py verify <파일>           # 이 덤프로 되살릴 수 있나 (운영 무관)
python3 scripts/up.py --down                      # 앱을 먼저 내린다
python3 scripts/backup.py restore <파일> --yes    # 덮어쓴다. 되돌릴 수 없다
python3 scripts/up.py                             # 다시 띄운다
```

**`verify` 를 먼저 돌린다.** 임시 DB 에 복원해 보고 지우므로 운영에 아무 영향이 없고,
받아 둔 덤프가 실제로 쓸 만한지는 그렇게밖에 알 수 없다 — 복원해 본 적 없는 백업은
백업이 아니다.

복원 뒤에는 `GET /api/v1/admin/consistency` 가 깨끗한지 본다 (§12.4). 문제 패키지는
불변 버전이라 이미지에서 그대로 되살아나고, 공개 포인터만 DB 에 있으므로
`published_version_missing` 이 비어 있어야 한다.

**브로커는 복원하지 않는다.** 진행 중이던 작업만 들어 있고, 제출은 아웃박스와 함께
커밋되므로 복원 후 다시 발행된다 (§3.2). 큐까지 되살리면 같은 작업을 두 번 돌린다.

## 장애 주입 훈련

**런북은 훈련하지 않으면 틀린 채로 남는다.** 장애가 났을 때 절차가 틀린 것을
발견하면 이미 늦다.

```bash
python3 scripts/drill.py all
```

| 시나리오 | 주입 | 확인하는 것 |
|---|---|---|
| `broker` | 브로커 정지 후 재기동 | 제출은 받아지고, 복구 후 전부 판정된다 (§12.2) |
| `duplicate` | 같은 멱등 키로 동시 제출 | 제출도 판정도 하나다 (§4.3) |
| `worker-loss` | 채점 중 Runner 강제 종료 | 임대 회수로 판정이 끝난다 (§4.3) |
| `trace-loss` | 판정 직후 Runner 강제 종료 | 판정은 그대로, 트레이스 부재가 오류가 아니다 (§12.2) |

`worker-loss` 는 임대가 만료되기를 기다린다. 기본 120초를 그대로 두면 훈련이 오래
걸리므로 짧게 띄운 오케스트레이터로 돌린다.

```bash
./gradlew :judge:orchestrator:bootRun --args='--codedrill.judge.lease-seconds=15'
```

`broker` 와 `worker-loss` 는 컨테이너와 Runner 프로세스를 실제로 죽인다. **운영
환경에서 돌리지 않는다.**

## capacity

```bash
python3 scripts/loadtest.py -n 30 -c 4
```

§12.1 의 네 가지 SLO 를 재서 통과·실패를 말한다. 목표값은 `alerts.yml` 과 같다.

Runner 는 **동시성 1** 이다. 한 에이전트가 여러 샌드박스를 동시에 띄우면 CPU·wall
측정치가 서로 오염돼, 같은 코드가 부하에 따라 다른 판정을 받는다 (§5.2). 그래서
처리율을 올리는 방법은 동시성이 아니라 **Runner 를 늘리는 것**이다.

큐 대기가 SLO 를 넘는데 Runner 가 놀고 있다면 브로커나 발행을 본다. Runner 가 계속
바쁘다면 용량 부족이며, §17.1 의 용량 모델로 필요한 대수를 산정한다.
