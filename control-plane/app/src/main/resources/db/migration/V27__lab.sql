-- 해설·비교·실험실 (기획서 §6.4~6.6, PRD FR-214).

-- 해설 열람 (FR-214).
--
-- > 정답 전에는 해설을 잠그되 사용자가 명시적으로 열 수 있습니다. 열람 이력이 학습 기록에
-- > 남으며 대회 정책이 우선합니다.
--
-- 맞히기 전에 열었다는 사실은 그 문제의 도움 단계다 — 방법을 본 것이니 힌트 3단계와
-- 같다. 그 뒤의 제출 증거가 그만큼 가벼워진다.
CREATE TABLE editorial_unlock (
    user_id     TEXT        NOT NULL,
    problem_id  TEXT        NOT NULL,
    unlocked_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    PRIMARY KEY (user_id, problem_id)
);

-- 실험실 실행.
--
-- 결과에 참조 풀이의 이벤트가 통째로 실린다. 이것이 허용되는 이유는 이 행이 **맞힌 사람
-- 또는 명시적으로 해설을 연 사람**의 것만 만들어지기 때문이다 (FR-214). 그 조건을
-- 확인하는 곳은 LabService 하나다.
CREATE TABLE lab_run (
    id          UUID        PRIMARY KEY,
    user_id     TEXT        NOT NULL,
    problem_id  TEXT        NOT NULL,
    args        JSONB       NOT NULL,
    labels      JSONB       NOT NULL,

    -- PENDING / COMPLETED
    status      TEXT        NOT NULL,
    -- 풀이별 결과 [{label, verdict, actual, measurements, eventCounts, events, truncated}]
    results     JSONB,

    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ
);

CREATE INDEX lab_run_user_recent_idx ON lab_run (user_id, created_at DESC);

COMMENT ON TABLE editorial_unlock IS '정답 전에 해설을 연 기록. 그 문제의 도움 단계가 된다 (FR-214).';
COMMENT ON TABLE lab_run IS '한 입력에 여러 풀이를 나란히 돌린 결과 (§6.4~6.6).';
