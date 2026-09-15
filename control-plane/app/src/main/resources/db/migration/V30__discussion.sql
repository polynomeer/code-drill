-- 문제별 질문 게시판 (기획서 §8.5 "문제별 질문 게시판과 코드 구간 링크", "특정 리플레이
-- 시점을 공유하는 주석").
--
-- 커뮤니티의 두 번째 칸이다. 첫 칸(V29 아레나 기부)이 세운 신고·검수 위에 선다 — 글도
-- "남의 것이 여러 사람에게 보이는" 일이라 같은 바닥이 필요하다.
--
-- 질문과 답은 한 표다. 답은 parent_id 가 있는 글이고, 그 밖에는 다를 것이 없다 — 둘 다
-- 코드 구간이나 리플레이 시점을 붙일 수 있고, 둘 다 신고되고 내려진다.
--
-- Workspace 모듈이 소유한다 (§3.1). 검수는 Admin 이 하되 표는 여기 있다.

CREATE TABLE discussion_post (
    id             UUID        PRIMARY KEY,
    problem_id     TEXT        NOT NULL,
    -- 답이면 질문의 id. 질문이면 NULL.
    parent_id      UUID        REFERENCES discussion_post (id),
    -- 계정을 지우면 NULL 이 된다 (§11.3). 글은 남되 누구의 것인지는 남지 않는다.
    author_id      TEXT,
    -- 질문만 제목이 있다.
    title          TEXT,
    body           TEXT        NOT NULL,
    -- 코드 구간 링크 / 리플레이 시점. 글쓴이 자신의 제출만 붙일 수 있다.
    anchor_submission_id UUID,
    anchor_line_from     INT,
    anchor_line_to       INT,
    anchor_step          INT,
    -- 붙인 시점의 코드 구간. 제출 소스가 삭제 요청에 사라지면 이것도 함께 사라진다.
    anchor_excerpt       TEXT,
    -- 풀이를 드러내는 글. 이 문제를 맞힌 사람에게만 본문이 보인다 (아레나와 같은 잠금).
    spoiler        BOOLEAN     NOT NULL DEFAULT FALSE,
    -- VISIBLE / HIDDEN
    status         TEXT        NOT NULL DEFAULT 'VISIBLE',
    hidden_by      TEXT,
    hidden_at      TIMESTAMPTZ,
    hidden_reason  TEXT,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX discussion_post_problem_idx ON discussion_post (problem_id, created_at DESC) WHERE parent_id IS NULL;
CREATE INDEX discussion_post_parent_idx ON discussion_post (parent_id, created_at);
CREATE INDEX discussion_post_author_idx ON discussion_post (author_id, created_at DESC);
CREATE INDEX discussion_post_anchor_idx ON discussion_post (anchor_submission_id) WHERE anchor_submission_id IS NOT NULL;

-- 신고. 글 하나에 대해 누구나 올린다 — 아레나와 달리 맞힌 사람만이 아니다. 글은 맞히기
-- 전에도 보이므로, 보는 사람이 곧 신고할 수 있는 사람이다.
CREATE TABLE discussion_report (
    id            UUID        PRIMARY KEY,
    post_id       UUID        NOT NULL REFERENCES discussion_post (id),
    reporter_id   TEXT        NOT NULL,
    reason        TEXT        NOT NULL,
    -- OPEN / RETIRED (글을 내렸다) / DISMISSED
    status        TEXT        NOT NULL DEFAULT 'OPEN',
    resolved_by   TEXT,
    resolved_at   TIMESTAMPTZ,
    resolution    TEXT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (post_id, reporter_id)
);

CREATE INDEX discussion_report_open_idx ON discussion_report (created_at) WHERE status = 'OPEN';
