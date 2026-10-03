-- 연장 (LeaseRegistry.renew). 토큰이 같을 때만.
-- KEYS: lease 해시, expiry 정렬 집합, 큐별 생존 해시 / ARGV: 제출 id, 토큰, expiresAt(ms)
--
-- 연장한 시각을 그 임대의 큐(lane)에도 적는다. 같은 큐의 워커가 살아 있는지를
-- LeaseRegistry.working 이 여기서 읽는다. lane 이 없는 것은 이 필드 이전의 임대다.
if redis.call('HGET', KEYS[1], 'token') ~= ARGV[2] then
  return 0
end
redis.call('HSET', KEYS[1], 'expiresAt', ARGV[3], 'started', '1')
redis.call('ZADD', KEYS[2], ARGV[3], ARGV[1])
local lane = redis.call('HGET', KEYS[1], 'lane')
if lane then
  redis.call('HSET', KEYS[3], lane, ARGV[3])
end
return 1
