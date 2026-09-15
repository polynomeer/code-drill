-- 풀이 공유와 기여자 평판 (기획서 §8.5 "정적 풀이", "좋은 반례·해설·시각화에 대한 기여자 평판").
--
-- 커뮤니티의 셋째 칸이다. 풀이는 새 표가 아니라 **질문 게시판의 글 한 종류**다 — 맞힌
-- 제출 하나에 접근을 적어 올리는 글이고, 보이는 것·신고되는 것·내려지는 것·지워지는 것이
-- 전부 글과 같다. 다른 것은 셋뿐이다: 붙이는 제출이 맞힌 것이어야 하고, 코드 구간이 아니라
-- 소스 전체이며, 항상 풀이 노출이다.
--
-- 평판은 "도움됐다"로 잰다. 남의 글에 맞힌 사람이 한 번씩 남기고, 받은 수가 그 사람의
-- 기여다. 이름은 여전히 나가지 않는다 — 글에 실리는 것은 등급뿐이다.

-- QUESTION / ANSWER / SOLUTION. 기존 글은 parent_id 로 갈린다.
ALTER TABLE discussion_post ADD COLUMN kind TEXT;
UPDATE discussion_post SET kind = CASE WHEN parent_id IS NULL THEN 'QUESTION' ELSE 'ANSWER' END;
ALTER TABLE discussion_post ALTER COLUMN kind SET NOT NULL;

CREATE INDEX discussion_post_solution_idx ON discussion_post (problem_id, created_at DESC) WHERE kind = 'SOLUTION';

-- 도움됐다. 한 사람이 한 글에 한 번.
CREATE TABLE discussion_helpful (
    post_id     UUID        NOT NULL REFERENCES discussion_post (id),
    user_id     TEXT        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (post_id, user_id)
);

CREATE INDEX discussion_helpful_user_idx ON discussion_helpful (user_id);
