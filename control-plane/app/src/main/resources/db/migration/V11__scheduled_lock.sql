-- 주기 작업의 단독 실행 (기술 설계서 §12.4, §2.3).
--
-- 제어 영역 인스턴스가 둘이 되면 `@Scheduled` 가 인스턴스마다 돈다. 대부분은 그래도
-- 되지만 — 아웃박스 발행은 FOR UPDATE SKIP LOCKED 라 오히려 병렬로 빨라진다 — 일관성
-- 점검은 아니다. 같은 위반을 인스턴스 수만큼 경고하면 당번은 사고가 몇 건인지 셀 수
-- 없다 (§13.4).
--
-- 임대 방식이다. 잡은 인스턴스가 죽어도 expires_at 이 지나면 다른 인스턴스가 가져간다.

CREATE TABLE scheduled_lock (
    name       TEXT        PRIMARY KEY,
    holder     TEXT        NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL
);

COMMENT ON TABLE scheduled_lock IS
    '주기 작업을 한 인스턴스에서만 돌리기 위한 임대 (§12.4).';
