-- 프로젝트형 문제의 제출 (feature-roadmap 11단계 — 두 번째 판정기).
--
-- 알고리즘 제출(submission)과 표를 나눈다. 소스 한 문자열이 아니라 파일 여럿이고, 판정은
-- 그룹·케이스가 아니라 테스트 리포트다. 한 표에 열을 더하면 어느 열이 어느 판정기의
-- 것인지 아무도 말할 수 없게 된다.
--
-- 문제 자체는 problem/problem_version 에 등록·공개된다 — 프로젝트형 문제도 문제고, 2인
-- 승인과 보고서 digest 대조라는 규칙은 같다 (§3.2).

CREATE TABLE project_submission (
    id               UUID PRIMARY KEY,
    user_id          TEXT NOT NULL,
    idempotency_key  TEXT NOT NULL,
    project_id       TEXT NOT NULL,
    project_version  INT  NOT NULL,
    language         TEXT NOT NULL,
    -- QUEUED → LEASED → COMPLETED. 종착은 불변이다 (§4.2).
    status           TEXT NOT NULL,
    verdict          TEXT,
    score            INT,
    log              TEXT,
    -- 공개 테스트의 결과만. 숨은 것은 두 수로만 남는다 (§8.3).
    tests            JSONB,
    hidden_passed    INT,
    hidden_total     INT,
    execution_id     TEXT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at     TIMESTAMPTZ,
    version          INT NOT NULL DEFAULT 0,
    UNIQUE (user_id, idempotency_key)
);

CREATE INDEX project_submission_user_idx ON project_submission (user_id, created_at DESC);
CREATE INDEX project_submission_project_idx ON project_submission (project_id, created_at DESC);

-- 제출한 파일. DB 의 것이 화면·반출의 원본이고, 스토어의 워크스페이스는 실행용 복제다 —
-- 알고리즘 제출의 소스와 같은 분담이다 (production-readiness B3).
CREATE TABLE project_submission_file (
    submission_id UUID NOT NULL REFERENCES project_submission (id) ON DELETE CASCADE,
    path          TEXT NOT NULL,
    content       TEXT NOT NULL,
    PRIMARY KEY (submission_id, path)
);
