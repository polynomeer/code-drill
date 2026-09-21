-- 사용자의 테스트가 오답을 잡았는가 (12단계 실무군 셋째 역량 — 결함 검출).
--
-- 판정 결과 옆에 한 칸이다. 시험하지 않았으면(더 쓴 테스트가 없거나 시험판이 없으면) NULL 이고,
-- 시험했으면 참조 위에서 통과했는지와 잡은·놓친 오답의 **이름**이다. 오답의 내용은 어디에도
-- 실리지 않는다 — Runner 가 돌리고 이름만 돌아온다. 재채점 이력에도 같은 칸을 둔다: 오답이
-- 늘어 다시 재면 잡은 수가 달라지고, 그것도 "왜 바뀌었나"의 일부다.

ALTER TABLE project_submission ADD COLUMN probe JSONB;
ALTER TABLE project_judgement ADD COLUMN probe JSONB;
