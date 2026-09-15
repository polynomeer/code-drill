-- 제재와 이의 (기획서 §8.5 "단계적 제재", §10.4 "이의 절차 제공").
--
-- 대회의 전제다. 유사도 신호와 신고는 사람이 본 뒤 기록으로 남았고, 그 기록이 계정에
-- 닿는 길이 여기다. 세 단계 — 경고, 글쓰기 정지, 제출 정지 — 이고 기간이 있다. 영구
-- 정지는 없다: 그것은 계정 삭제이고, 삭제는 본인만 한다 (§11.3).
--
-- 제재는 근거를 가리킨다 (유사도 신호나 신고의 id). 근거 없는 제재는 없다. 이의는 제재
-- 하나에 한 번이고, 보안 관리자가 유지하거나 푼다. 발부한 사람이 이의를 판단하지 않는다.
--
-- Identity 모듈이 소유한다 (§3.1). 계정의 상태라서다.

CREATE TABLE sanction (
    id           UUID        PRIMARY KEY,
    user_id      TEXT        NOT NULL,
    -- WARNING / MUTE (글쓰기 정지) / SUSPEND (제출 정지 — 글쓰기도 함께)
    kind         TEXT        NOT NULL,
    reason       TEXT        NOT NULL,
    -- 근거. "similarity:<id>" 나 "report:<id>" 처럼 종류와 id 를 적는다.
    evidence     TEXT        NOT NULL,
    issued_by    TEXT        NOT NULL,
    starts_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- 경고는 끝이 없다 (기록이다). 정지는 끝이 있다.
    ends_at      TIMESTAMPTZ,
    lifted_by    TEXT,
    lifted_at    TIMESTAMPTZ,
    -- 이의 (§10.4). 한 번이다.
    appeal       TEXT,
    appealed_at  TIMESTAMPTZ,
    -- UPHELD / LIFTED. 비어 있으면 아직 안 봤다.
    appeal_resolution TEXT,
    appeal_note  TEXT,
    resolved_by  TEXT,
    resolved_at  TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX sanction_user_idx ON sanction (user_id, created_at DESC);
CREATE INDEX sanction_appeal_open_idx ON sanction (appealed_at) WHERE appeal IS NOT NULL AND appeal_resolution IS NULL;
