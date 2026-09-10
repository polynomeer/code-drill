-- 역량 증거 (기획서 §4.2 역량 체계와 증거 모델, PRD FR-801·FR-806).
--
-- 숙련도를 저장하지 않는다. **증거만 쌓고 숙련도는 그때그때 계산한다.**
--
-- 저장하면 두 가지가 생긴다. 계산 규칙을 고쳤을 때 옛 값이 남아 새 규칙과 섞이고,
-- "이 숫자가 어디서 나왔나"에 답하려면 결국 증거를 다시 봐야 한다. 그럴 바에는 증거가
-- 유일한 진실이고 숙련도는 그것의 함수인 편이 낫다 (§3.1 — Competency 는 원본 증거를
-- 수정하지 않는다).
--
-- 표가 커지면 투영 테이블을 옆에 두면 된다. 그때도 이 표가 원본이다.

CREATE TABLE competency_evidence (
    id           UUID        PRIMARY KEY,
    user_id      TEXT        NOT NULL,

    -- platform/problem-package 의 Competency enum. 16개 세부 역량 중 하나다.
    competency   TEXT        NOT NULL,

    -- 무엇이 이 증거를 만들었나: SUBMISSION / PREQUESTION / TRIAL
    source       TEXT        NOT NULL,

    -- 그 행동이 성공이었나. 판정 ACCEPTED, 사전 질문 정답 같은 것이다.
    success      BOOLEAN     NOT NULL,

    /*
     * 증거의 무게 (§3.4 — 힌트·AI 도움 수준에 따라 가중치를 낮춘다).
     *
     * 지금은 도움 기능이 없어 전부 1.0 이다. 그래도 칸을 두는 이유는, 4단계에서 힌트가
     * 붙을 때 **지난 증거를 다시 해석하지 않기 위해서**다. 그때 칸을 만들면 그 전의
     * 증거는 무게를 모르는 채로 남는다.
     */
    weight       REAL        NOT NULL DEFAULT 1.0,

    -- 무엇을 풀다 나온 증거인가. 근거 상세가 여기서 문제로 내려간다 (FR-806).
    problem_id   TEXT        NOT NULL,

    -- 그 근거를 가리키는 값 (제출 id 등). 화면이 이것으로 원본을 연다.
    reference    TEXT,

    -- 사람이 읽을 한 줄. "O(n²) 라고 답했다 — 실제보다 비싸게 봄" 같은 것.
    detail       TEXT,

    occurred_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 역량 지도는 사용자별로 역량마다 최근 것을 본다.
CREATE INDEX competency_evidence_user_idx
    ON competency_evidence (user_id, competency, occurred_at DESC);

-- 같은 사건이 두 번 들어오지 않게 한다. 아웃박스도 재채점도 at-least-once 라,
-- 막지 않으면 한 번의 판정이 증거 두 개가 되어 숙련도가 실제보다 단단해 보인다.
CREATE UNIQUE INDEX competency_evidence_unique
    ON competency_evidence (user_id, competency, source, reference)
    WHERE reference IS NOT NULL;

COMMENT ON TABLE competency_evidence IS
    '역량 증거. 숙련도는 저장하지 않고 이 표에서 계산한다 (§4.2).';
