-- 사용자와 세션 (기술 설계서 §11.2, §8.1).
--
-- 토큰은 **불투명 문자열**이다. 서명된 토큰(JWT)이 아니라 저장소에 둔 임의 값이라,
-- 하나를 즉시 무효로 만들 수 있다. 서명 토큰은 발급 후 만료까지 취소할 방법이 없어,
-- 유출을 알아차려도 기다리는 것 말고 할 수 있는 일이 없다.
--
-- 대신 요청마다 조회가 한 번 든다. 모듈형 모놀리스에서 이 비용은 감당할 만하고,
-- 필요해지면 Redis 로 짧게 캐시한다 (§10.1).

CREATE TABLE app_user (
    id            UUID        PRIMARY KEY,
    -- 로그인 식별자. 대소문자를 구분하지 않도록 정규화해 저장한다.
    email         TEXT        NOT NULL,
    display_name  TEXT        NOT NULL,
    -- BCrypt 해시. 평문도, 되돌릴 수 있는 형태도 저장하지 않는다 (§11.3).
    password_hash TEXT        NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT app_user_email_unique UNIQUE (email)
);

/*
 * 세션 (§11.2 짧은 access token + 회전 가능한 refresh token).
 *
 * 토큰 자체는 저장하지 않고 SHA-256 해시만 둔다. DB 를 읽을 수 있게 된 공격자가 그대로
 * 로그인할 수 있으면, 해시를 쓰는 비밀번호 컬럼 옆에서 토큰만 평문인 셈이 된다.
 *
 * 갱신은 회전이다. refresh 를 쓰면 그 자리에서 무효가 되고 새 것이 나온다. 이미 쓴
 * refresh 가 다시 오면 토큰이 복제됐다는 뜻이므로, 그 세션 전체를 끊는다.
 */
CREATE TABLE user_session (
    id                  UUID        PRIMARY KEY,
    user_id             UUID        NOT NULL REFERENCES app_user (id),
    access_token_hash   TEXT        NOT NULL,
    refresh_token_hash  TEXT        NOT NULL,
    access_expires_at   TIMESTAMPTZ NOT NULL,
    refresh_expires_at  TIMESTAMPTZ NOT NULL,
    -- 끊긴 시각. 로그아웃·회전·재사용 탐지가 채운다. 행을 지우지 않아 이력이 남는다.
    revoked_at          TIMESTAMPTZ,
    revoked_reason      TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT user_session_access_unique UNIQUE (access_token_hash),
    CONSTRAINT user_session_refresh_unique UNIQUE (refresh_token_hash)
);

CREATE INDEX user_session_user_idx ON user_session (user_id, created_at DESC);

/*
 * 기존 데이터의 사용자 식별자를 옮긴다 (§15.2 expand → backfill).
 *
 * 이전 슬라이스는 `X-User-Id` 헤더의 문자열을 그대로 user_id 로 썼다. 그 값들은 이제
 * 어떤 계정에도 속하지 않으므로, 새 사용자로 만들지 않고 그대로 둔다 — 소유자가 없는
 * 행은 어떤 로그인으로도 열리지 않는다. 이것이 의도한 결과다.
 *
 * 컬럼 타입은 TEXT 로 남긴다. UUID 로 좁히면 이전 행이 전부 마이그레이션에서 터진다.
 */
