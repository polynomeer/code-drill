-- 전이 확인 과제 (PRD FR-807).
--
-- > 코칭 후 설명 과제와 변형 문제로 전이를 확인합니다. 힌트·AI 없이 완료한 결과를 가장
-- > 높은 가중치의 증거로 반영합니다.
--
-- 코칭 세션과 나눈다. 세션은 "이 문제를 푸는 동안 도왔다"이고 이것은 "그래서 옮겨졌나"라,
-- 서로 다른 문제를 가리키고 끝나는 시점도 다르다 — 세션은 오늘 닫히지만 전이 확인은
-- 며칠 뒤 다른 문제를 풀 때 끝날 수 있다.

CREATE TABLE transfer_task (
    id                UUID        PRIMARY KEY,
    session_id        UUID        NOT NULL REFERENCES coaching_session(id) ON DELETE CASCADE,
    user_id           TEXT        NOT NULL,

    -- 코칭받은 문제와, 그 배움을 옮겨 볼 문제.
    source_problem_id TEXT        NOT NULL,
    target_problem_id TEXT        NOT NULL,

    -- 설명 과제. 개인 데이터이므로 삭제 요청에 비워질 수 있다 (§11.3).
    explanation       TEXT,

    -- ASSIGNED → EXPLAINED → VERIFIED / UNVERIFIED
    status            TEXT        NOT NULL,

    -- 변형 문제를 풀 때 받은 도움 단계. 0 이 아니면 전이 증거가 되지 않는다 —
    -- 힌트를 보고 푼 것은 "옮겨졌다"의 증거가 아니다 (FR-807 "힌트·AI 없이").
    help_level        INT,

    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at      TIMESTAMPTZ
);

-- 판정이 끝날 때마다 "이 사람에게 이 문제로 걸린 과제가 있나"를 묻는다. 그 질문이
-- 이 표의 뜨거운 경로다.
CREATE INDEX transfer_task_pending_idx
    ON transfer_task (user_id, target_problem_id)
    WHERE completed_at IS NULL;

COMMENT ON TABLE transfer_task IS
    '코칭 후 전이 확인. 힌트 없이 변형 문제를 풀어야 가장 무거운 증거가 된다 (FR-807).';
