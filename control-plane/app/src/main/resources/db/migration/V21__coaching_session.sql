-- 코칭 세션 (PRD FR-802).
--
-- 세션과 도움을 한 표에 둔다. 도움은 세션 없이 존재할 수 없고, 세션 하나에 붙는 도움은
-- 많아야 역량 2개 × 몇 단계라 따로 표를 둘 만큼 크지 않다. 나누면 "몇 단계까지 봤나"를
-- 물을 때마다 조인이 붙는데, 그 질문이 이 표의 유일한 질문이다.
--
-- Coaching 모듈이 소유한다 (§3.1 — session, prescription, assistance).

CREATE TABLE coaching_session (
    id          UUID        PRIMARY KEY,
    user_id     TEXT        NOT NULL,
    problem_id  TEXT        NOT NULL,

    -- 이번 세션에 개입할 역량. 빈 배열일 수 있다 — 약한 것이 없으면 도울 것도 없다.
    focus       JSONB       NOT NULL,

    -- 펼친 도움 [{competency, level, revealedAt}]. 사용자가 "몇 단계까지 봤는지"를
    -- 확인할 수 있어야 하고(FR-802), 증거 가중치가 이 값을 본다 (FR-806).
    revealed    JSONB       NOT NULL DEFAULT '[]'::jsonb,

    started_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    ended_at    TIMESTAMPTZ
);

-- "이 문제에서 내가 연 세션"을 찾는다. 한 사용자가 한 문제에 여러 번 열 수 있으므로
-- 유일하지 않다 — 어제 코칭받고 오늘 다시 푸는 것은 막을 일이 아니다.
CREATE INDEX coaching_session_user_problem_idx
    ON coaching_session (user_id, problem_id, started_at DESC);

COMMENT ON TABLE coaching_session IS
    '코칭 세션과 펼친 도움. 한 세션에 약한 역량 1~2개만 개입한다 (FR-802).';
