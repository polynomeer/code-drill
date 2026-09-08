-- 관리자 역할을 데이터로 옮긴다 (기술 설계서 §11.2).
--
-- 지금까지 운영자는 `ADMIN_OPERATORS` 환경변수에 `<이름>:<토큰>:<역할>` 로 들어 있었다.
-- 세 가지가 나빴다.
--
--   1. 토큰이 **평문 장기 비밀**이다. 프로세스 목록(`ps eww`)에 그대로 보인다.
--   2. 사람이 아니라 토큰이 신원이라, 감사 로그의 actor 가 계정과 이어지지 않는다.
--   3. 역할을 바꾸려면 재기동해야 하고, 누가 언제 줬는지 아무 데도 안 남는다.
--
-- 이제 신원은 Identity 모듈의 계정이 정하고(§11.2 짧은 수명 토큰), 이 표는 그 계정에
-- 무엇을 허용하는지만 담는다. 부여와 회수는 감사 로그에 남는다 (§13.3).

CREATE TABLE admin_role (
    user_id    UUID        NOT NULL REFERENCES app_user (id),
    role       TEXT        NOT NULL,
    -- 누가 줬는지. 부트스트랩으로 생긴 첫 역할은 'bootstrap' 이다.
    granted_by TEXT        NOT NULL,
    granted_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    PRIMARY KEY (user_id, role)
);

-- 요청마다 "이 사람의 역할"을 묻는다. 기본 키가 (user_id, role) 이라 접두사로 커버된다.
COMMENT ON TABLE admin_role IS
    '관리자 역할 부여. 신원은 app_user 가 정하고 이 표는 인가만 담는다 (§11.2).';
