-- 아레나에 남의 오답 세우기 (기획서 §8.3 "익명화된 오답", §8.5 신고·검수).
--
-- 지금까지의 과녁은 저작자의 대표 오답뿐이었다. 원본이 말한 "익명화된 오답"은 사용자의
-- 실제 제출인데, 익명화해도 코드는 그 사람의 것이라 두 가지가 먼저 있어야 했다 — 본인이
-- 내놓는 것이어야 하고(기부), 누군가 보고 세워야 한다(검수). 세운 뒤에 문제가 드러나면
-- 내릴 길도 있어야 한다(신고).
--
-- Workspace 모듈이 소유한다 (§3.1). 검수는 Admin 모듈이 하되 표는 여기 있다 — 검수는
-- "무엇을 세울지 결정"하는 일이고, 세우고 내리는 것은 아레나의 것이다.

-- 기부. 본인의 오답 제출 하나가 한 줄이다.
CREATE TABLE arena_donation (
    id             UUID        PRIMARY KEY,
    problem_id     TEXT        NOT NULL,
    -- 어느 제출에서 왔나. 한 제출은 한 번만 세울 수 있다.
    submission_id  UUID        NOT NULL UNIQUE,
    donor_user_id  TEXT        NOT NULL,
    -- 기부 시점의 소스. 제출 소스는 삭제 요청에 사라질 수 있고, 그때 이것도 함께 사라진다 (§11.3).
    source         TEXT,
    -- 기부자가 적은 "무엇을 잘못하는지" 한 줄. 검수자가 다듬어 과녁의 설명이 된다.
    note           TEXT        NOT NULL,
    -- PENDING / APPROVED / REJECTED / RETIRED
    status         TEXT        NOT NULL DEFAULT 'PENDING',
    -- 검수자가 정한 결함군. 승인 전에는 비어 있다.
    kind           TEXT,
    reviewed_by    TEXT,
    reviewed_at    TIMESTAMPTZ,
    -- 반려·내림의 사유. 기부자에게 보인다.
    reason         TEXT,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX arena_donation_problem_idx ON arena_donation (problem_id, status);
CREATE INDEX arena_donation_donor_idx ON arena_donation (donor_user_id, created_at DESC);
CREATE INDEX arena_donation_pending_idx ON arena_donation (created_at) WHERE status = 'PENDING';

-- 신고. 세워진 오답 하나에 대해 맞힌 사람이 올린다.
CREATE TABLE arena_report (
    id            UUID        PRIMARY KEY,
    donation_id   UUID        NOT NULL REFERENCES arena_donation (id),
    reporter_id   TEXT        NOT NULL,
    reason        TEXT        NOT NULL,
    -- OPEN / RETIRED / DISMISSED
    status        TEXT        NOT NULL DEFAULT 'OPEN',
    resolved_by   TEXT,
    resolved_at   TIMESTAMPTZ,
    resolution    TEXT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- 한 사람이 같은 과녁을 두 번 신고하지 못한다.
    UNIQUE (donation_id, reporter_id)
);

CREATE INDEX arena_report_open_idx ON arena_report (created_at) WHERE status = 'OPEN';
