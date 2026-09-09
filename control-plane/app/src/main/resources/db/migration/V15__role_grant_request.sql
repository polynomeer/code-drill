-- 역할 부여에도 2인 승인을 건다 (기술 설계서 §11.2).
--
-- 지금까지 SECURITY_ADMIN 은 혼자서 남에게 역할을 줄 수 있었다. 자기 자신에게 주는
-- 것만 막혀 있었는데, 그 검사는 계정 id 문자열 비교다. 계정 등록은 누구에게나 열려
-- 있으므로 **혼자서 두 번째 계정을 만들어 그것에 PUBLISHER 를 붙이면**, 등록자·승인자
-- 비교는 그 둘을 두 사람으로 센다. 2인 승인이 세는 것이 사람이 아니라 계정이라는
-- 사실이 여기서 드러난다.
--
-- 그래서 권한이 늘어나는 순간 자체를 두 사람이 밟게 한다. 요청과 승인을 나누고,
--
--   * 승인자는 요청자와 달라야 한다 (2인 승인).
--   * 승인자는 역할을 받는 사람과도 달라야 한다. 같아도 되면 SECURITY_ADMIN 둘이
--     서로에게 주면서 각자 권한을 늘릴 수 있고, 그때 필요한 사람 수는 다시 둘이 된다.
--
-- 회수는 즉시다. 권한을 **줄이는** 일까지 승인을 기다리게 하면, 사고가 났을 때 가장
-- 급한 조치가 가장 느려진다.
--
-- 예외는 부트스트랩 구간 하나다. SECURITY_ADMIN 이 한 명뿐이면 승인해 줄 사람이 없어
-- 아무 역할도 만들 수 없다. 그 한 명은 **SECURITY_ADMIN 만** 혼자 줄 수 있다 — 혼자일
-- 때 할 수 있는 유일한 일이 동료를 만드는 것이고, 동료가 생기는 순간 이 길은 닫힌다.

CREATE TABLE role_grant_request (
    id           UUID        PRIMARY KEY,
    -- 역할을 받을 사람.
    user_id      UUID        NOT NULL REFERENCES app_user (id),
    role         TEXT        NOT NULL,
    -- 왜 이 사람에게 이 역할이 필요한가. 승인자가 판단할 근거이며, 없으면 승인은
    -- 형식이 된다 (rejudge_job.reason 과 같은 이유다).
    reason       TEXT        NOT NULL,
    status       TEXT        NOT NULL,
    requested_by TEXT        NOT NULL,
    decided_by   TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    decided_at   TIMESTAMPTZ,

    -- 애플리케이션에서 먼저 걸러 사유를 말해 주지만, 제약으로도 막는다. 버그로도
    -- 우회되지 않아야 하는 규칙이다 (rejudge_two_person 과 같은 자리).
    CONSTRAINT role_grant_two_person CHECK (
        decided_by IS NULL
        OR (decided_by <> requested_by AND decided_by <> user_id::text)
    ),
    -- 요청자가 곧 수혜자일 수 없다. admin_role 쪽 selfGrant 와 짝을 이룬다.
    CONSTRAINT role_grant_not_self CHECK (requested_by <> user_id::text)
);

-- 같은 사람에게 같은 역할을 여러 건 대기시켜 두지 않는다. 두 건이 나란히 있으면
-- 하나를 반려해도 다른 하나가 살아 있어, 반려가 반려로 보이지 않는다.
CREATE UNIQUE INDEX role_grant_request_pending
    ON role_grant_request (user_id, role) WHERE status = 'REQUESTED';

COMMENT ON TABLE role_grant_request IS
    '관리자 역할 부여 요청. 승인자는 요청자와도 수혜자와도 달라야 한다 (§11.2).';
