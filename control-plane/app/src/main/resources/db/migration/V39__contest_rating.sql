-- 레이팅 (기획서 §8.4 "정기 레이팅", §10.2 "레이팅 문제 난이도는 충분한 표본 이후 보정").
--
-- 레이팅 대회는 끝나는 순간 한 번 적용된다. 순위표에서 Elo 를 계산해 참가자마다 전후를
-- 적고, 같은 대회를 두 번 적용하지 않는다 (rated_at). 사용자의 레이팅은 이 변화들의
-- 합이라 언제든 다시 셀 수 있는 projection 이다 — 원본이 잘못됐으면 변화를 지우고 다시
-- 적으면 된다.
--
-- 처음은 1500 이고 첫 몇 대회는 잠정이다 (§10.2). 이름은 참가할 때 이미 나가기로 했다.

ALTER TABLE contest ADD COLUMN rated BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE contest ADD COLUMN rated_at TIMESTAMPTZ;

CREATE TABLE rating_change (
    contest_id  UUID   NOT NULL REFERENCES contest (id),
    user_id     TEXT   NOT NULL,
    rank        INT    NOT NULL,
    before      INT    NOT NULL,
    after       INT    NOT NULL,
    applied_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (contest_id, user_id)
);

CREATE INDEX rating_change_user_idx ON rating_change (user_id, applied_at DESC);

-- 지금의 레이팅. rating_change 의 합과 같아야 한다 — 어긋나면 다시 센다.
CREATE TABLE user_rating (
    user_id   TEXT PRIMARY KEY,
    rating    INT  NOT NULL,
    contests  INT  NOT NULL DEFAULT 0
);
