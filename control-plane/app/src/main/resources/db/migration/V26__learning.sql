-- 학습 루프 (PRD FR-808, FR-205, 기획서 §8.1).
--
-- Learning 모듈이 소유한다 (§3.1 — problem state, collection, activity).

-- 처방 조정. 사용자가 오늘의 처방에서 밀어낸 문제 (FR-808 "사용자가 조정할 수 있다").
--
-- 날짜 단위다. 내일이면 다시 권할 수 있다 — 오늘 안 하겠다는 것과 영영 안 하겠다는 것은
-- 다르고, 후자는 여기서 받지 않는다.
CREATE TABLE prescription_skip (
    user_id    TEXT NOT NULL,
    problem_id TEXT NOT NULL,
    skipped_on DATE NOT NULL,

    PRIMARY KEY (user_id, problem_id, skipped_on)
);

-- 개인 문제집 (FR-205).
CREATE TABLE collection (
    id         UUID        PRIMARY KEY,
    user_id    TEXT        NOT NULL,
    name       TEXT        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX collection_user_idx ON collection (user_id, created_at);

-- 같은 문제를 두 번 담지 않는다 (FR-205 "중복 저장되지 않는다"). 기본 키가 그것을 막는다.
CREATE TABLE collection_item (
    collection_id UUID        NOT NULL REFERENCES collection(id) ON DELETE CASCADE,
    problem_id    TEXT        NOT NULL,
    added_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    PRIMARY KEY (collection_id, problem_id)
);

COMMENT ON TABLE prescription_skip IS '오늘의 처방에서 밀어낸 문제. 날짜가 지나면 다시 권할 수 있다 (FR-808).';
COMMENT ON TABLE collection IS '개인 문제집 (FR-205).';
