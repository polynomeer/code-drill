-- 재채점 실행 (기술 설계서 §4.2 INV-02, §8.1 revision, §13.3).
--
-- 재채점은 이미 사용자에게 보여 준 판정을 바꾸는 행위다. 그래서 덮어쓰지 않고 쌓는다 —
-- "왜 점수가 바뀌었나"에 답하려면 이전 판정이 남아 있어야 한다.

/*
 * 판정 이력.
 *
 * 최초 판정도 여기에 들어간다. 재채점 결과만 이력에 남기면 "무엇에서 무엇으로 바뀌었나"의
 * 앞쪽 절반이 비어, 정작 필요한 비교를 할 수 없다.
 *
 * execution_id 가 유일하다. 같은 실행 결과가 두 번 도착해도 이력이 두 번 쌓이지 않고,
 * revision 도 헛되이 오르지 않는다 (§4.3 결과 중복).
 */
CREATE TABLE submission_judgement (
    id             BIGSERIAL   PRIMARY KEY,
    submission_id  UUID        NOT NULL REFERENCES submission (id),
    -- 이 판정이 만든 revision. dry-run 이면 "만들었을" revision 이다.
    revision       INT         NOT NULL,
    execution_id   TEXT        NOT NULL,
    verdict        TEXT        NOT NULL,
    score          INT         NOT NULL,
    compile_log    TEXT,
    groups         JSONB       NOT NULL,
    -- 어느 재채점이 만든 판정인지. NULL 이면 사용자가 직접 제출한 최초 판정이다.
    rejudge_job_id UUID        REFERENCES rejudge_job (id),
    -- 현재 판정으로 반영했는지. dry-run 은 이력에만 남고 제출 행을 건드리지 않는다.
    applied        BOOLEAN     NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT submission_judgement_execution_unique UNIQUE (execution_id)
);

CREATE INDEX submission_judgement_submission_idx
    ON submission_judgement (submission_id, created_at DESC);

/*
 * 재채점 대상.
 *
 * 어떤 제출을 언제 다시 걸었고 언제 돌아왔는지. 이 표가 없으면 "재채점이 끝났는가"에
 * 답할 수 없고, 절반만 돌린 채 끝난 작업과 전부 끝난 작업을 구분하지 못한다.
 *
 * 실행 영역은 재채점을 모른다. 채점 메시지에 작업 ID 를 싣는 대신 제어 영역이 스스로
 * 짝을 기억한다 — 재채점은 Admin 의 관심사이지 판정의 관심사가 아니다 (§3.1).
 */
CREATE TABLE rejudge_target (
    job_id        UUID        NOT NULL REFERENCES rejudge_job (id),
    submission_id UUID        NOT NULL REFERENCES submission (id),
    dispatched_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at  TIMESTAMPTZ,

    PRIMARY KEY (job_id, submission_id)
);

-- 아직 결과를 기다리는 대상을 제출 하나로 빨리 찾기 위한 인덱스.
CREATE INDEX rejudge_target_pending_idx
    ON rejudge_target (submission_id) WHERE completed_at IS NULL;

ALTER TABLE rejudge_job
    -- 판정을 바꾸지 않고 무엇이 바뀔지만 본다 (§19.2 FR-721 dry-run).
    ADD COLUMN dry_run       BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN dispatched_at TIMESTAMPTZ,
    ADD COLUMN target_count  INT     NOT NULL DEFAULT 0;
