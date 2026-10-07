/*
 * 기기 승인 — 개인 IDE 의 CLI 가 비밀번호 없이 로그인한다 (feature-roadmap 11단계 이어서 — 2단계, RFC 8628).
 *
 * CLI 가 기기 코드를 받고, 사람은 브라우저에서 사용자 코드를 맞춰 승인한다. 승인되면 CLI 는 **기기 세션**을
 * 받는다 — 웹 세션과 같은 표(user_session)에 kind 만 다르게 둔다. 그래서 refresh 회전·재사용 탐지·로그아웃·
 * 계정 삭제가 기기에도 그대로 걸린다. 기기 세션이 할 수 있는 일은 인증 인터셉터가 한 곳에서 좁힌다.
 */
ALTER TABLE user_session
    ADD COLUMN kind         TEXT NOT NULL DEFAULT 'WEB' CHECK (kind IN ('WEB', 'DEVICE')),
    -- 사람이 알아볼 이름 ("MacBook-Pro (zsh)"). 계정 설정의 "연결된 기기"에 나온다
    ADD COLUMN device_name  TEXT,
    -- 클라이언트와 판 ("codedrill 1.0"). 오래된 CLI 를 알아보려고 둔다
    ADD COLUMN client       TEXT,
    -- 마지막으로 쓴 시각. 기기 목록에서 "언제 썼나"를 보이고, 오래 안 쓴 기기를 끊는 근거다
    ADD COLUMN last_used_at TIMESTAMPTZ;

CREATE INDEX user_session_device_idx ON user_session (user_id, created_at DESC)
    WHERE kind = 'DEVICE' AND revoked_at IS NULL;

CREATE TABLE device_authorization (
    id               UUID        PRIMARY KEY,
    -- 기기 코드는 CLI 만 안다. 세션 토큰처럼 해시만 둔다
    device_code_hash TEXT        NOT NULL UNIQUE,
    -- 사람이 브라우저에 맞춰 보는 짧은 코드 (WQXR-7KDP). 기다리는 동안에만 유일하면 된다
    user_code        TEXT        NOT NULL,
    device_name      TEXT        NOT NULL,
    client           TEXT        NOT NULL,
    -- 요청한 출처의 해시(AbuseGuard — IP 를 그대로 두지 않는다). 한 출처가 코드를 마구 받아 가는 것을 막는 데 쓴다 (§10.2)
    origin           TEXT        NOT NULL,
    status           TEXT        NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'DENIED', 'CONSUMED')),
    -- 승인하거나 거절한 사람. 승인 전에는 없다
    user_id          UUID        REFERENCES app_user (id),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at       TIMESTAMPTZ NOT NULL,
    decided_at       TIMESTAMPTZ,
    last_polled_at   TIMESTAMPTZ
);

CREATE UNIQUE INDEX device_authorization_pending_code ON device_authorization (user_code) WHERE status = 'PENDING';
CREATE INDEX device_authorization_origin_idx ON device_authorization (origin, created_at DESC);
