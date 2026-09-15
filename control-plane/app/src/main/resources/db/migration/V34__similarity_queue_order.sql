-- 유사도 신호 큐는 새 것부터다 (§11.4).
--
-- 점수순으로 두면 큐가 차는 순간 오래된 1.0 들 뒤에서 새 신호가 영영 보이지 않는다.
-- 스모크를 되풀이하자 그렇게 됐다. 점수는 같은 자리에 실려 있으니 검수자가 보고 고른다.
DROP INDEX similarity_flag_open_idx;
CREATE INDEX similarity_flag_open_idx ON similarity_flag (created_at DESC) WHERE status = 'OPEN';
