-- 연장 (LeaseRegistry.renew). 토큰이 같을 때만.
-- KEYS: lease 해시, expiry 정렬 집합 / ARGV: 제출 id, 토큰, expiresAt(ms)
if redis.call('HGET', KEYS[1], 'token') ~= ARGV[2] then
  return 0
end
redis.call('HSET', KEYS[1], 'expiresAt', ARGV[3], 'started', '1')
redis.call('ZADD', KEYS[2], ARGV[3], ARGV[1])
return 1
