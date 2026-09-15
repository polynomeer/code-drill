-- 가입·로그인 남용 방어 (§10.2 남용 방어의 남은 절반, docs/production-readiness.md A6).
--
-- 지금까지 가입에 제한이 없어 계정을 여러 개 만들면 제출 쿼터도 그만큼 늘었고, 로그인은
-- 비밀번호를 무한히 시험할 수 있었다. 둘 다 **어디서 왔는가**로 센다.
--
-- IP 는 저장하지 않는다. 서버 소금과 함께 해시한 값만 두고, 세는 데는 그것으로 충분하다 —
-- 같은 곳에서 온 것인지만 알면 되고 어디인지는 몰라도 된다 (§11.3 최소 수집). 하루 지난
-- 줄은 다음 기록 때 지운다.

CREATE TABLE auth_attempt (
    id           UUID        PRIMARY KEY,
    -- REGISTER / LOGIN_FAILED
    kind         TEXT        NOT NULL,
    origin_hash  TEXT        NOT NULL,
    -- 로그인 실패면 시도한 계정(정규화한 이메일). 가입이면 비어 있다.
    subject      TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX auth_attempt_origin_idx ON auth_attempt (kind, origin_hash, created_at DESC);
CREATE INDEX auth_attempt_subject_idx ON auth_attempt (kind, subject, created_at DESC) WHERE subject IS NOT NULL;
