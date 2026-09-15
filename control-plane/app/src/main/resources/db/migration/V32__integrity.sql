-- 제출 사이의 유사도 신호 (기술 설계서 §11.4 부정행위 방어, 기획서 §10.4 정책).
--
-- 대회의 전제다. 맞힌 제출의 구조 지문을 두고, 같은 문제·같은 언어의 다른 사람 제출과
-- 겹치는 쌍을 검수 큐에 올린다. 신호는 판정을 바꾸지 않는다 — "탐지 신호를 단독 유죄
-- 근거로 사용하지 않고 이의 절차를 제공한다"(§10.4). 확인된 것도 지금은 기록이고, 제재는
-- 대회와 함께 온다.
--
-- Integrity 모듈이 소유한다 (§3.1). 소스는 여기 없다 — 지문만 있고, 소스는 검수자가 볼
-- 때 제출 도메인에 묻는다.

CREATE TABLE submission_fingerprint (
    submission_id  UUID        PRIMARY KEY,
    -- 계정을 지우면 지문도 지운다 (소스와 함께 간다).
    user_id        TEXT,
    problem_id     TEXT        NOT NULL,
    language       TEXT        NOT NULL,
    token_count    INT         NOT NULL,
    hashes         INTEGER[]   NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX submission_fingerprint_problem_idx ON submission_fingerprint (problem_id, language, created_at DESC);
CREATE INDEX submission_fingerprint_user_idx ON submission_fingerprint (user_id);

CREATE TABLE similarity_flag (
    id                   UUID        PRIMARY KEY,
    problem_id           TEXT        NOT NULL,
    language             TEXT        NOT NULL,
    -- 순서 없는 쌍. 작은 id 가 앞이라 같은 쌍이 두 번 서지 않는다.
    submission_id        UUID        NOT NULL,
    other_submission_id  UUID        NOT NULL,
    -- 계정을 지우면 NULL. 신호는 상대방의 기록이기도 해서 남는다.
    user_id              TEXT,
    other_user_id        TEXT,
    score                DOUBLE PRECISION NOT NULL,
    -- OPEN / CONFIRMED / DISMISSED
    status               TEXT        NOT NULL DEFAULT 'OPEN',
    reviewed_by          TEXT,
    reviewed_at          TIMESTAMPTZ,
    note                 TEXT,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (submission_id, other_submission_id)
);

CREATE INDEX similarity_flag_open_idx ON similarity_flag (score DESC, created_at) WHERE status = 'OPEN';
CREATE INDEX similarity_flag_user_idx ON similarity_flag (user_id);
CREATE INDEX similarity_flag_other_user_idx ON similarity_flag (other_user_id);
