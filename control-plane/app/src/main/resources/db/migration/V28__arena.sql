-- 반례 아레나 (기획서 §8.3).
--
-- > 익명화된 오답을 실패시키는 입력을 작성합니다. 성공한 반례를 자동 축소하고 깨뜨린
-- > 가정을 분류합니다. 작은 반례, 새로운 버그 유형, 많은 제출을 깨는 반례를 별도 평가합니다.
--
-- 지금의 "오답"은 저작자가 만든 대표 오답이다. 남의 실제 제출을 익명화해 세우는 것은
-- 검수 절차(§8.3 마지막 줄)가 서기 전에는 하지 않는다 — 익명화해도 코드는 그 사람의 것이다.
--
-- Workspace 모듈이 소유한다 (§3.1 — custom test).

CREATE TABLE arena_attempt (
    id          UUID        PRIMARY KEY,
    user_id     TEXT        NOT NULL,
    problem_id  TEXT        NOT NULL,
    -- 사용자가 적은 입력. 개인 데이터라 삭제 요청에 비워질 수 있다 (§11.3).
    args        JSONB,
    -- PENDING / COMPLETED / INVALID_INPUT / FAILED
    status      TEXT        NOT NULL,
    message     TEXT,
    -- 오답별 결과 [{name, kind, broken, actual, minimalArgs, minimalSize}]
    results     JSONB,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ
);

CREATE INDEX arena_attempt_user_idx ON arena_attempt (user_id, created_at DESC);

-- 기록판. (문제, 오답) 마다 가장 작은 반례와 처음 깨뜨린 사람.
--
-- 사용자 id 를 담지만 화면에는 "나인가 아닌가"만 나간다. 순위표에 이름을 올리는 것은
-- 커뮤니티 정책(§8.5 평판)이 서고 나서다.
CREATE TABLE arena_record (
    problem_id     TEXT        NOT NULL,
    mutant_name    TEXT        NOT NULL,
    -- 몇 명이 깨뜨렸나.
    breakers       INT         NOT NULL DEFAULT 0,
    smallest_size  INT,
    smallest_by    TEXT,
    first_broken_by TEXT,
    first_broken_at TIMESTAMPTZ,
    PRIMARY KEY (problem_id, mutant_name)
);

-- 한 사람이 같은 오답을 여러 번 깨뜨려도 한 번으로 센다.
CREATE TABLE arena_breaker (
    problem_id  TEXT NOT NULL,
    mutant_name TEXT NOT NULL,
    user_id     TEXT NOT NULL,
    best_size   INT  NOT NULL,
    PRIMARY KEY (problem_id, mutant_name, user_id)
);

COMMENT ON TABLE arena_attempt IS '반례 아레나 시도. 저작자의 대표 오답을 깨뜨리는 입력 (§8.3).';
COMMENT ON TABLE arena_record IS '(문제, 오답) 별 가장 작은 반례와 처음 깨뜨린 사람.';
