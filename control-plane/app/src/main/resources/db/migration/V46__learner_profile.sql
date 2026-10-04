-- 가입 직후 세 문항 (docs/ui-overhaul.md §6.9). Learning 이 소유한다.
--
-- 하루 목표는 처방의 칸 수, 주 언어는 풀이 화면의 기본 언어, 수준은 진단의 난이도 띠가 된다.
-- 수준은 자기 보고라 역량 지도에는 들어가지 않는다 — 거기는 실행 증거만 간다.
CREATE TABLE learner_profile (
    user_id    TEXT        PRIMARY KEY,
    daily_goal INT         NOT NULL CHECK (daily_goal BETWEEN 1 AND 3),
    language   TEXT        NOT NULL,
    level      TEXT        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON TABLE learner_profile IS '온보딩 세 문항의 답 — 하루 목표·주 언어·자기 보고 수준.';
