-- 리플레이 중 다음 상태 예측 (PRD FR-805).
--
-- > 리플레이 중 다음 상태 예측과 최초 분기 진단을 지원합니다. 선택·근거·결과가
-- > 코드·입력·이벤트 시점과 연결됩니다.
--
-- 그 문장이 이 표의 열이다. 선택(predicted)·근거(rationale)·결과(actual, correct)를
-- 이벤트 시점(step)과 함께 남긴다. 셋 중 하나라도 빠지면 나중에 "왜 그렇게 생각했나"에
-- 답할 수 없고, 그러면 이 기록은 정답률 말고 아무것도 말해 주지 못한다.

CREATE TABLE state_prediction (
    id            UUID        PRIMARY KEY,
    user_id       TEXT        NOT NULL,
    submission_id UUID        NOT NULL,

    -- 몇 번째 이벤트를 맞히려 했나. 리플레이의 그 자리로 되돌아갈 수 있어야 한다.
    step          INT         NOT NULL,

    predicted     TEXT        NOT NULL,
    actual        TEXT        NOT NULL,
    correct       BOOLEAN     NOT NULL,

    -- 왜 그렇게 생각했나. 선택이다 — 필수로 하면 예측 자체를 건너뛴다 (FR-803 과 같은 이유).
    -- 개인 데이터이므로 삭제 요청에 비워질 수 있다 (§11.3).
    rationale     TEXT,

    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 같은 자리를 두 번 맞히지 못하게 한다. 틀린 뒤 답을 보고 다시 누르면 그것은 예측이
-- 아니라 받아쓰기이고, 증거가 통째로 거짓이 된다.
CREATE UNIQUE INDEX state_prediction_once_idx
    ON state_prediction (submission_id, step);

CREATE INDEX state_prediction_user_idx ON state_prediction (user_id, created_at DESC);

COMMENT ON TABLE state_prediction IS
    '리플레이 중 다음 이벤트 예측. 선택·근거·결과를 이벤트 시점과 함께 남긴다 (FR-805).';
