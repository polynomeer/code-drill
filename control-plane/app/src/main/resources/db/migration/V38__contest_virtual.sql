-- 가상 참가 (기획서 §8.4 "과거 대회를 동일 시간 조건으로 재현").
--
-- 끝난 대회를 혼자 다시 돈다. 문제와 길이가 같고 지금 시작하며, 순위표는 원래 참가자들
-- 사이에 자기 자리를 보인다 — "그때 참가했다면 몇 등이었나". 원래 대회의 순위표는 건드리지
-- 않는다: 가상 참가는 자기 것이고 남에게 보이지 않는다.
ALTER TABLE contest ADD COLUMN parent_id UUID REFERENCES contest (id);
CREATE INDEX contest_parent_idx ON contest (parent_id, created_by);
