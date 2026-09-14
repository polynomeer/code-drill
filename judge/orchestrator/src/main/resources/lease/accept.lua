-- 결과 수락 (LeaseRegistry.accept).
-- KEYS: lease 해시, expiry 정렬 집합, done 키
-- ARGV: 제출 id, 결과의 토큰, 결과의 attempt, result digest, done TTL(초)
--
-- 순서가 중요하다. 중복 판정을 fencing 검사보다 먼저 해야 같은 결과의 재전달이 스테일로
-- 잘못 기록되지 않는다.
local done = redis.call('GET', KEYS[3])
if done then
  if done == ARGV[4] then return {'DUPLICATE'} else return {'COMPLETED'} end
end
if redis.call('EXISTS', KEYS[1]) == 0 then
  return {'UNLEASED'}
end
local token = tonumber(redis.call('HGET', KEYS[1], 'token'))
local attempt = tonumber(redis.call('HGET', KEYS[1], 'attempt'))
if tonumber(ARGV[2]) < token then
  return {'STALE', 'fencing 토큰이 낮다: 받은 ' .. ARGV[2] .. ', 현재 ' .. token}
end
if tonumber(ARGV[3]) ~= attempt then
  return {'STALE', 'attempt 가 다르다: 받은 ' .. ARGV[3] .. ', 현재 ' .. attempt}
end
local origin = redis.call('HGET', KEYS[1], 'origin')
redis.call('SET', KEYS[3], ARGV[4], 'EX', ARGV[5])
redis.call('DEL', KEYS[1])
redis.call('ZREM', KEYS[2], ARGV[1])
return {'ACCEPTED', origin}
