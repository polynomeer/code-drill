-- 제출 (기술 설계서 §8.1, §8.2).
--
-- 슬라이스는 소스를 이 테이블에 직접 담는다. 운영에서는 오브젝트 스토어에 올리고
-- 참조와 digest 만 보관해야 한다 (§8.3).

CREATE TABLE submission (
    id               UUID        PRIMARY KEY,
    user_id          TEXT        NOT NULL,
    idempotency_key  TEXT        NOT NULL,
    problem_id       TEXT        NOT NULL,
    problem_version  INT         NOT NULL,
    language         TEXT        NOT NULL,
    source           TEXT        NOT NULL,
    status           TEXT        NOT NULL,
    verdict          TEXT,
    score            INT,
    compile_log      TEXT,
    groups           JSONB,
    -- 상태 전이를 낙관적 갱신으로 적용하기 위한 컬럼 (§3.2).
    version          BIGINT      NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- 제출 버튼 중복 클릭을 흡수하는 유일한 장치 (§4.3).
    CONSTRAINT submission_idempotency UNIQUE (user_id, idempotency_key)
);

CREATE INDEX submission_user_recent_idx ON submission (user_id, created_at DESC);
CREATE INDEX submission_problem_status_idx ON submission (problem_id, status);
