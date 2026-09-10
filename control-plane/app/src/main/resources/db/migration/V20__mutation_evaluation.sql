-- 사용자 테스트를 대표 오답에 겨눈 결과 (PRD FR-804).
--
-- trial_run 옆에 따로 둔다. 둘 다 "사용자가 적은 케이스를 돌린 것"이지만 재는 것이
-- 다르다 — 시험 실행은 **내 코드가** 맞는지를 보고, 이것은 **내 테스트가** 쓸모 있는지를
-- 본다. 한 표에 섞으면 상태 값이 두 뜻을 갖게 되고, 화면이 그것을 다시 갈라야 한다.
--
-- Workspace 모듈이 소유한다 (§3.1 — draft, custom test, response, preference).

CREATE TABLE mutation_evaluation (
    id              UUID        PRIMARY KEY,
    user_id         TEXT        NOT NULL,
    problem_id      TEXT        NOT NULL,
    problem_version INT         NOT NULL,

    -- 사용자가 적은 케이스. 개인 데이터이므로 삭제 요청에 비워질 수 있다 (§11.3).
    cases           JSONB,

    status          TEXT        NOT NULL,
    message         TEXT,

    -- 기대 출력이 실제 정답과 어긋난 케이스 번호. 정답 값은 담지 않는다.
    mistaken_cases  JSONB,

    -- 오답별 (kind, killed, killedBy). **이름도 소스도 담지 않는다** — 저장하면
    -- 언젠가 응답에 실리고, 오답 코드는 정답의 골격을 그대로 보여 준다 (§8.3).
    outcomes        JSONB,

    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at    TIMESTAMPTZ
);

-- 쿼터가 이 인덱스를 쓴다. 한 건이 정답 한 번 + 오답 N 번을 돌리므로 시험 실행보다
-- 훨씬 비싸고, 한도도 따로 둔다 (§10.2).
CREATE INDEX mutation_evaluation_user_recent_idx
    ON mutation_evaluation (user_id, created_at DESC);

COMMENT ON TABLE mutation_evaluation IS
    '사용자 테스트의 결함 탐지력 평가. 판정이 아니며 정답·오답 소스를 담지 않는다.';
