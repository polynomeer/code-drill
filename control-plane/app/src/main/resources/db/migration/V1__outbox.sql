-- 트랜잭셔널 아웃박스 (기술 설계서 §3.2, §8.1, §8.2).
--
-- 도메인 상태 변경과 이 테이블의 삽입이 한 트랜잭션에서 커밋되어야, 브로커가 죽어도
-- 이벤트가 유실되지 않는다. 퍼블리셔는 published_at IS NULL 인 행만 훑는다.
--
-- 다른 엔터티(problem, submission, evidence_event 등)는 해당 기능을 구현하는
-- 마이그레이션에서 함께 추가한다. 이 baseline 은 P0 산출물인 outbox 만 세운다.

CREATE TABLE outbox_event (
    id            UUID        PRIMARY KEY,
    aggregate     TEXT        NOT NULL,
    aggregate_id  TEXT        NOT NULL,
    type          TEXT        NOT NULL,
    payload       JSONB       NOT NULL,
    occurred_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at  TIMESTAMPTZ,
    -- 같은 aggregate 안에서는 발행 순서를 보존해야 한다.
    sequence_no   BIGSERIAL   NOT NULL
);

-- 퍼블리셔의 주 조회 경로. 미발행 행만 인덱싱해 테이블이 커져도 스캔 비용을 묶어둔다.
CREATE INDEX outbox_event_unpublished_idx
    ON outbox_event (sequence_no)
    WHERE published_at IS NULL;

CREATE INDEX outbox_event_aggregate_idx
    ON outbox_event (aggregate, aggregate_id, sequence_no);
