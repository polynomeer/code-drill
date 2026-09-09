-- 제출 쿼터가 보는 질의를 받친다 (기술 설계서 §10.2).
--
-- 제출마다 두 번 센다. "지금 채점 중인 내 제출"과 "최근 1분 동안 낸 내 제출"이다.
-- 뒤의 것은 submission_user_recent_idx 가 이미 받치지만, 앞의 것은 상태로 걸러야 한다.
--
-- 부분 인덱스인 이유는 **진행 중인 제출이 전체의 아주 작은 일부**이기 때문이다. 끝난
-- 제출까지 담으면 인덱스가 테이블만큼 커지는데, 쿼터는 그 부분을 한 번도 보지 않는다.

CREATE INDEX submission_in_flight_idx
    ON submission (user_id)
 WHERE status IN ('CREATED', 'QUEUED', 'LEASED', 'RUNNING');
