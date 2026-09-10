-- 문제 풀이 전 질문과 응답 (PRD FR-803, 기획서 §4.3 역량 중심 학습 루프).
--
-- > 문제 전 조건·알고리즘·복잡도 질문을 문제와 사용자 상태에 따라 제공합니다.
-- > 응답은 정답 여부뿐 아니라 **근거와 오개념 분류**로 저장됩니다.
--
-- 정답 여부만 남기면 3단계 Competency 가 "맞았다/틀렸다"밖에 읽을 수 없다. 무엇을 골랐고
-- 왜 골랐는지가 있어야 **어떤 오개념인지**를 말할 수 있고, 그것이 코칭이 겨눌 지점이다.
--
-- Workspace 모듈이 소유한다 (§3.1 — draft, custom test, **response**, preference).

CREATE TABLE prequestion_response (
    id           UUID        PRIMARY KEY,
    user_id      TEXT        NOT NULL,
    problem_id   TEXT        NOT NULL,

    -- ALGORITHM_CHOICE / TIME_COMPLEXITY / SPACE_COMPLEXITY
    kind         TEXT        NOT NULL,
    answer       TEXT        NOT NULL,
    correct      BOOLEAN     NOT NULL,

    -- 왜 그렇게 골랐는가. 사용자가 적지 않을 수 있어 NULL 을 허용한다 — 근거를 필수로
    -- 하면 질문 자체를 건너뛰게 되고, 그러면 아무 증거도 남지 않는다.
    rationale    TEXT,

    -- 틀렸을 때의 분류. 맞았으면 NULL 이다.
    misconception TEXT,

    answered_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 같은 문제를 다시 풀 때 또 답할 수 있다. 마지막 답만 남기지 않는 이유는 **바뀌었다는
-- 사실 자체가 증거**이기 때문이다 — 처음엔 O(n²)라 했다가 나중에 O(n log n)이라 하면
-- 그 사이에 배운 것이 있다.
CREATE INDEX prequestion_user_problem_idx
    ON prequestion_response (user_id, problem_id, answered_at DESC);

COMMENT ON TABLE prequestion_response IS
    '풀이 전 질문 응답. 정답 여부·근거·오개념을 함께 남긴다 (FR-803).';
