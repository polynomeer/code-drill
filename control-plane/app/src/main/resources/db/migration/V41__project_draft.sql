-- 프로젝트형 문제의 초안 (로드맵 11단계, §8.1 초안 자동 저장의 프로젝트 판).
--
-- workspace_draft 와 표를 나눈다. 저것은 소스 한 문자열이고 이것은 파일 여럿이다 — 한 표의
-- code 열에 JSON 을 넣으면 어느 행이 어느 편집기의 것인지 아무도 말할 수 없다. CAS 의
-- 규칙은 같다: 저장할 때마다 version 이 1 오르고, 클라이언트가 본 버전일 때만 덮어쓴다.

CREATE TABLE project_draft (
    user_id    TEXT        NOT NULL,
    project_id TEXT        NOT NULL,
    -- 경로 → 내용. 받을 때 Workspaces.validate 를 지난 것만 들어온다.
    files      JSONB       NOT NULL,
    version    BIGINT      NOT NULL DEFAULT 1,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, project_id)
);
