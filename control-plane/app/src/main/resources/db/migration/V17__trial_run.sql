-- 사용자가 자기 입력으로 돌려 보는 실행 (기획서 부록 A 실행 도메인, PRD §5.2 테스트 패널).
--
-- 제출과 다른 표에 둔다. 같은 표에 두면 "제출 기록"에 시험 실행이 섞이고, 정답률·역량
-- 증거·재채점 대상이 전부 그것을 세게 된다. **판정이 아닌 실행은 판정 옆에 두지 않는다.**
--
-- Workspace 모듈이 소유한다 (§3.1 — draft, custom test, response, preference).

CREATE TABLE trial_run (
    id              UUID        PRIMARY KEY,
    user_id         TEXT        NOT NULL,
    problem_id      TEXT        NOT NULL,
    problem_version INT         NOT NULL,
    language        TEXT        NOT NULL,

    -- 개인 데이터다. 계정 삭제 요청에 지워져야 하므로 NULL 을 허용한다 (§11.3).
    -- submission.source 가 NOT NULL 이라 삭제가 500 으로 터졌던 일이 있었다.
    source          TEXT,
    cases           JSONB,

    status          TEXT        NOT NULL,
    compile_log     TEXT,
    -- 케이스별 실제 출력과 기대 출력의 대조. 실행 전에는 NULL 이다.
    results         JSONB,

    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at    TIMESTAMPTZ
);

-- "내 최근 시험 실행"과 쿼터 계산이 같은 인덱스를 쓴다.
CREATE INDEX trial_run_user_recent_idx ON trial_run (user_id, created_at DESC);

COMMENT ON TABLE trial_run IS
    '사용자 커스텀 테스트 실행. 판정이 아니며 제출 이력·역량 증거에 들어가지 않는다.';
