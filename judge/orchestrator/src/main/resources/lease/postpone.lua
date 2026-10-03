-- 배정 기한 연장 (LeaseRegistry.postpone). 토큰이 같고 아직 아무도 집어 들지 않았을 때만.
-- KEYS: lease 해시, expiry 정렬 집합 / ARGV: 제출 id, 토큰, expiresAt(ms)
if redis.call('HGET', KEYS[1], 'token') ~= ARGV[2] or redis.call('HGET', KEYS[1], 'started') == '1' then
  return 0
end
redis.call('HSET', KEYS[1], 'expiresAt', ARGV[3])
redis.call('ZADD', KEYS[2], ARGV[3], ARGV[1])
return 1
