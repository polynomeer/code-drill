-- 임대 (LeaseRegistry.lease / reclaim).
-- KEYS: lease 해시, expiry 정렬 집합, done 키, fencing 카운터
-- ARGV: 제출 id, executionId, origin JSON, expiresAt(ms), 기대 토큰("" 이면 무조건), 해시 TTL(초)
--
-- 기대 토큰이 있으면 지금 토큰과 같을 때만 바꾼다. 회수 인스턴스가 여럿일 때 한 쪽만 이긴다.
local token = redis.call('HGET', KEYS[1], 'token')
if ARGV[5] ~= '' and token ~= ARGV[5] then
  return nil
end
local attempt = tonumber(redis.call('HGET', KEYS[1], 'attempt') or '0') + 1
local next = redis.call('INCR', KEYS[4])
redis.call('HSET', KEYS[1],
  'attempt', attempt, 'token', next, 'expiresAt', ARGV[4], 'started', '0',
  'executionId', ARGV[2], 'origin', ARGV[3])
redis.call('EXPIRE', KEYS[1], ARGV[6])
redis.call('ZADD', KEYS[2], ARGV[4], ARGV[1])
-- 지난 완료 기록을 지운다. 재채점 결과가 지난 판정과 같아도 중복으로 오해받지 않게.
redis.call('DEL', KEYS[3])
return {attempt, next}
