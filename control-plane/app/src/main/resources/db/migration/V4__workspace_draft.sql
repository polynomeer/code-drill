-- 작업 중인 초안 (기술 설계서 §8.1, §8.2).
--
-- 사용자·문제·언어마다 초안 하나다. version 은 낙관적 잠금(CAS)용이며, 두 탭에서 같은
-- 초안을 고칠 때 나중 저장이 앞선 저장을 조용히 덮어쓰지 않게 한다 (§9.4 DRAFT_VERSION_CONFLICT).
--
-- 초안은 판정과 무관하다. Workspace 모듈은 제출 상태를 건드리지 않는다 (§3.1).

CREATE TABLE workspace_draft (
    user_id    TEXT        NOT NULL,
    problem_id TEXT        NOT NULL,
    language   TEXT        NOT NULL,
    code       TEXT        NOT NULL,
    version    BIGINT      NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    PRIMARY KEY (user_id, problem_id, language)
);

-- "내가 최근에 손댄 문제" 조회 경로.
CREATE INDEX workspace_draft_recent_idx ON workspace_draft (user_id, updated_at DESC);
