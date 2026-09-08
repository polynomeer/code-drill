-- 검증 파이프라인 버전 (기술 설계서 §15.3 스키마·파이프라인 호환).
--
-- 보고서 digest 가 등록된 것과 다를 때, 원인은 둘이다. 패키지를 고쳤거나, **검증
-- 파이프라인 자체가 바뀌었거나**. digest 만으로는 둘을 가를 수 없어서 "패키지가
-- 바뀌었다"고만 말하게 되고, 그러면 바꾼 적 없는 패키지를 뒤지게 된다.
--
-- 파이프라인이 어느 버전이었는지를 등록 행에 함께 남겨 원인을 이름으로 구분한다.

ALTER TABLE problem_version
    -- '0' 은 이 열이 생기기 전에 등록된 행이다. 어느 파이프라인이 통과시켰는지 알 수
    -- 없다는 뜻이고, 그 사실 자체가 재검증이 필요하다는 신호다.
    ADD COLUMN validator_version TEXT NOT NULL DEFAULT '0';

COMMENT ON COLUMN problem_version.validator_version IS
    '이 보고서를 만든 §6.3 파이프라인의 버전. 0 은 기록 이전.';
