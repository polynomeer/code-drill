-- 반례 대전 (기획서 §8.4 "풀이 제출보다 타인의 오류를 찾는 경쟁").
--
-- 아레나(§8.3) 위의 대회 종류다. 문제를 맞힌 뒤 아레나의 과녁을 깨뜨리는데, 대회 중에
-- 깨뜨린 **서로 다른 과녁의 수**가 점수다. 같은 과녁을 두 번 깨뜨리는 것은 한 번이다.
-- contest.kind 에 HACK 이 더해지고, 깨뜨린 과녁은 여기 적는다.

CREATE TABLE contest_hack (
    contest_id   UUID        NOT NULL REFERENCES contest (id),
    user_id      TEXT        NOT NULL,
    problem_id   TEXT        NOT NULL,
    target_name  TEXT        NOT NULL,
    at           TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (contest_id, user_id, problem_id, target_name)
);
