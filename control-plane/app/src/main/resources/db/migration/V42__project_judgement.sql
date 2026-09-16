-- 프로젝트형 제출의 판정 이력과 재채점 (로드맵 11단계, §8.1 rejudge_job, §4.2 INV-02).
--
-- submission_judgement 의 프로젝트 판. 종료된 제출을 덮어쓰지 않고 revision 을 올려 새 판정으로
-- 기록한다 — 이전 판정이 사라지면 "왜 점수가 바뀌었나"에 답할 수 없다. 재채점 작업과 대상
-- 표(rejudge_job·rejudge_target)는 Admin 의 것을 그대로 쓴다: 대상은 UUID 이고 어느 판정기의
-- 제출인지는 scope 가 말한다.

ALTER TABLE project_submission ADD COLUMN revision INT NOT NULL DEFAULT 1;

CREATE TABLE project_judgement (
    id             BIGSERIAL   PRIMARY KEY,
    submission_id  UUID        NOT NULL REFERENCES project_submission (id) ON DELETE CASCADE,
    -- 이 판정이 만든 revision. dry-run 이면 "만들었을" revision 이다.
    revision       INT         NOT NULL,
    execution_id   TEXT        NOT NULL,
    verdict        TEXT        NOT NULL,
    score          INT         NOT NULL,
    log            TEXT,
    tests          JSONB       NOT NULL,
    hidden_passed  INT         NOT NULL,
    hidden_total   INT         NOT NULL,
    -- 어느 재채점이 만든 판정인지. NULL 이면 사용자가 직접 제출한 최초 판정이다.
    rejudge_job_id UUID        REFERENCES rejudge_job (id),
    -- 현재 판정으로 반영했는지. dry-run 은 이력에만 남고 제출 행을 건드리지 않는다.
    applied        BOOLEAN     NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT project_judgement_execution_unique UNIQUE (execution_id)
);

CREATE INDEX project_judgement_submission_idx ON project_judgement (submission_id, created_at DESC);

-- 재채점 대상은 두 판정기의 제출이다. 한 표만 가리키는 외래키로는 프로젝트 제출을 대상에 넣을
-- 수 없다. 대상의 존재는 requeue 가 확인한다 — 없는 제출은 조용히 빠진다.
ALTER TABLE rejudge_target DROP CONSTRAINT rejudge_target_submission_id_fkey;
