-- 콘텐츠 수명주기와 운영 명령 (기술 설계서 §3.1 Admin, §8.1).
--
-- 공개는 검증 완료 버전을 가리키는 포인터 교체로 원자화한다 (§3.2). 버전 행은 공개 후
-- 불변이며, 무엇을 공개할지는 problem.published_version_id 하나만 움직인다.

CREATE TABLE problem (
    id                   TEXT        PRIMARY KEY,
    -- 공개 중인 버전. NULL 이면 아직 공개된 적이 없다.
    published_version_id TEXT,
    -- 논리 삭제. 이미 채점된 제출이 참조하므로 행을 지우지 않는다 (§8.1).
    archived             BOOLEAN     NOT NULL DEFAULT false,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE problem_version (
    id             TEXT        PRIMARY KEY,
    problem_id     TEXT        NOT NULL REFERENCES problem (id),
    version        INT         NOT NULL,
    package_digest TEXT        NOT NULL,
    -- §6.3 검증 보고서의 digest. 공개할 때 대조한다.
    report_digest  TEXT        NOT NULL,
    registered_by  TEXT        NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT problem_version_unique UNIQUE (problem_id, version),
    -- 같은 내용의 패키지가 두 버전으로 등록되는 것을 막는다 (§8.2).
    CONSTRAINT problem_version_digest_unique UNIQUE (package_digest)
);

ALTER TABLE problem
    ADD CONSTRAINT problem_published_version_fk
    FOREIGN KEY (published_version_id) REFERENCES problem_version (id);

/*
 * 감사 로그 (§13.3).
 *
 * 판정 변경·콘텐츠 공개·재채점·권한 변경을 남긴다. append-only 다 — 트리거로 UPDATE 와
 * DELETE 를 막아, 애플리케이션 버그나 운영자 실수로도 지워지지 않게 한다 (§3.1 감사 로그
 * 우회 금지).
 */
CREATE TABLE audit_log (
    id         BIGSERIAL   PRIMARY KEY,
    action     TEXT        NOT NULL,
    subject    TEXT        NOT NULL,
    actor      TEXT        NOT NULL,
    detail     JSONB       NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX audit_log_subject_idx ON audit_log (subject, created_at DESC);

CREATE OR REPLACE FUNCTION audit_log_is_append_only() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION '감사 로그는 append-only 다 (§13.3)';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER audit_log_no_update
    BEFORE UPDATE OR DELETE ON audit_log
    FOR EACH ROW EXECUTE FUNCTION audit_log_is_append_only();

/*
 * 재채점 작업 (§8.1 rejudge_job).
 *
 * 작성자와 승인자를 분리한다 (§11.2). 대량 재채점은 판정을 바꾸는 행위이므로 한 사람이
 * 혼자 실행할 수 없어야 한다.
 */
CREATE TABLE rejudge_job (
    id           UUID        PRIMARY KEY,
    scope        TEXT        NOT NULL,
    reason       TEXT        NOT NULL,
    status       TEXT        NOT NULL,
    requested_by TEXT        NOT NULL,
    approved_by  TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- 자기가 요청한 작업을 자기가 승인할 수 없다.
    CONSTRAINT rejudge_two_person CHECK (approved_by IS NULL OR approved_by <> requested_by)
);

-- 종료된 제출은 재채점해도 덮어쓰지 않고 revision 을 올린다 (§4.2 INV-02).
ALTER TABLE submission ADD COLUMN revision INT NOT NULL DEFAULT 1;
