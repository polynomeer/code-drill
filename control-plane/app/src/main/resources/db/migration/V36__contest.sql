-- 대회 (기획서 §8.4). 비레이팅 연습 대회와 미니 대결부터.
--
-- 전제(§11.4 부정행위 방어 — 신원·격리·유사도 신호·제재·남용 한도)가 다 선 뒤에 연다.
-- 레이팅은 아직 없다 — 레이팅은 순위가 믿을 만해진 뒤의 일이고, 순위가 믿을 만한지는
-- 비레이팅으로 먼저 본다.
--
-- **참가가 곧 이름 공개 동의다.** 이 제품은 이름을 어디에도 내지 않았다 (§8.5 평판까지
-- 등급뿐이었다). 순위표는 이름 없이는 뜻이 없으므로, 참가할 때 그때의 표시 이름을 여기
-- 적고 그 대회의 순위표에만 낸다. 지우면 이름이 빠진다.
--
-- Contest 모듈이 소유한다 (§3.1). 제출과 판정은 그대로 제출 도메인의 것이고, 대회는
-- "이 판정이 이 대회의 점수인가"만 안다.

CREATE TABLE contest (
    id          UUID        PRIMARY KEY,
    -- CONTEST (운영자가 여는 비레이팅 대회) / DUEL (사용자가 여는 미니 대결)
    kind        TEXT        NOT NULL,
    title       TEXT        NOT NULL,
    created_by  TEXT        NOT NULL,
    -- 대결은 둘째가 붙을 때 시작한다. 그 전에는 비어 있다.
    starts_at   TIMESTAMPTZ,
    ends_at     TIMESTAMPTZ,
    -- 대결의 길이(분). 대회는 starts/ends 로 정해져 비어 있다.
    minutes     INT,
    -- 대회는 공개해야 보인다. 대결은 만들면 곧 보인다(코드로만).
    published   BOOLEAN     NOT NULL DEFAULT FALSE,
    -- 대결에 붙는 코드. 대회는 비어 있다.
    join_code   TEXT        UNIQUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX contest_time_idx ON contest (published, starts_at DESC);

CREATE TABLE contest_problem (
    contest_id  UUID   NOT NULL REFERENCES contest (id),
    problem_id  TEXT   NOT NULL,
    ord         INT    NOT NULL,
    PRIMARY KEY (contest_id, problem_id)
);

CREATE TABLE contest_entry (
    contest_id    UUID        NOT NULL REFERENCES contest (id),
    user_id       TEXT        NOT NULL,
    -- 참가 시점의 표시 이름. 이 대회의 순위표에만 나간다.
    display_name  TEXT        NOT NULL,
    joined_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (contest_id, user_id)
);

CREATE INDEX contest_entry_user_idx ON contest_entry (user_id);

-- 문제별 최고 점수. 대회 중의 판정만 반영한다.
CREATE TABLE contest_score (
    contest_id  UUID        NOT NULL REFERENCES contest (id),
    user_id     TEXT        NOT NULL,
    problem_id  TEXT        NOT NULL,
    best_score  INT         NOT NULL DEFAULT 0,
    attempts    INT         NOT NULL DEFAULT 0,
    -- 처음 만점을 받은 시각. 동점의 순서다.
    solved_at   TIMESTAMPTZ,
    PRIMARY KEY (contest_id, user_id, problem_id)
);
