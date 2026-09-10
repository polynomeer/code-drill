-- 최소 반례 (기술 설계서 §6.3, PRD §3.4 검증군 증거).
--
-- 떨어진 입력에서 시작해 더 작으면서도 여전히 틀리는 입력을 찾은 결과다. 20만 원소짜리
-- 입력에서 틀렸다는 사실은 아무것도 알려 주지 않지만, 세 원소에서 틀렸다는 사실은 대개
-- 원인을 그대로 가리킨다.

CREATE TABLE counterexample (
    submission_id  UUID        PRIMARY KEY,
    user_id        TEXT        NOT NULL,

    -- PENDING / FOUND / NOT_REPRODUCED / FAILED
    status         TEXT        NOT NULL,
    message        TEXT,

    -- 줄인 입력과 그 입력에서의 두 출력. **이 입력 하나에 대한** 정답이며, 숨은 테스트
    -- 묶음과는 다른 이야기다 — 그것 없이는 "여기서 틀렸다"까지만 말하고 무엇이 옳은지는
    -- 말하지 못한다.
    args           JSONB,
    actual         TEXT,
    expected       TEXT,

    original_size  INT,
    minimal_size   INT,
    rounds         INT,

    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at   TIMESTAMPTZ
);

CREATE INDEX counterexample_user_idx ON counterexample (user_id, created_at DESC);

COMMENT ON TABLE counterexample IS
    '최소 반례 축소 결과. 줄인 입력 하나와 그 입력의 정답만 담는다 (§6.3).';
