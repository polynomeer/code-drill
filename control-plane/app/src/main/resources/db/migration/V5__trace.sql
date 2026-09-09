-- 실행 트레이스 (기술 설계서 §7.4, §8.1 trace 엔터티).
--
-- manifest 와 청크를 나눠 담는다. 클라이언트는 manifest 와 요약을 먼저 받고 상세 청크는
-- 필요한 위치 주변만 내려받으므로(§7.5), 한 덩어리로 저장하면 그 이점이 사라진다.
--
-- 판정과 독립이다. 트레이스가 없거나 INVALID 여도 제출의 판정은 그대로 유효하다 (§1.2).

CREATE TABLE trace (
    id             TEXT        PRIMARY KEY,
    submission_id  UUID        NOT NULL,
    schema_version TEXT        NOT NULL,
    status         TEXT        NOT NULL,
    manifest       JSONB       NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 제출 하나에 트레이스는 하나다. 재요청이 와도 덮어쓰기로 흡수한다.
CREATE UNIQUE INDEX trace_submission_idx ON trace (submission_id);

CREATE TABLE trace_chunk (
    trace_id  TEXT   NOT NULL REFERENCES trace (id) ON DELETE CASCADE,
    seq       INT    NOT NULL,
    events    JSONB  NOT NULL,

    PRIMARY KEY (trace_id, seq)
);

-- 원본 트레이스 보존은 30일이다 (§7.4). TraceRetention 이 주기적으로 지운다.

-- 이전 슬라이스가 submission 에 직접 담던 컬럼은 더 이상 쓰지 않는다.
ALTER TABLE submission DROP COLUMN IF EXISTS trace;
