-- 비밀번호 재설정 (docs/ui-overhaul.md §6.9). Identity 가 소유한다.
--
-- 토큰은 세션처럼 해시만 둔다 — DB 를 읽게 된 공격자가 재설정 링크를 만들 수 없게.
-- 한 번 쓰면 끝이고(used_at), 짧게 산다(expires_at). 새로 요청하면 앞의 것은 쓸 수 없게 된다.
CREATE TABLE password_reset (
    token_hash TEXT        PRIMARY KEY,
    user_id    UUID        NOT NULL REFERENCES app_user (id),
    expires_at TIMESTAMPTZ NOT NULL,
    used_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX password_reset_user_idx ON password_reset (user_id) WHERE used_at IS NULL;

COMMENT ON TABLE password_reset IS '비밀번호 재설정 토큰의 해시. 한 번 쓰면 끝, 짧게 산다.';
