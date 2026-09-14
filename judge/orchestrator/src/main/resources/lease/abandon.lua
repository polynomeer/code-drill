-- 포기 (LeaseRegistry.abandon). 종료로 표시해 뒤늦은 결과가 덮어쓰지 못하게 한다.
-- KEYS: lease 해시, expiry 정렬 집합, done 키 / ARGV: 제출 id, 자리표, done TTL(초)
redis.call('SET', KEYS[3], ARGV[2], 'EX', ARGV[3])
redis.call('DEL', KEYS[1])
redis.call('ZREM', KEYS[2], ARGV[1])
return 1
