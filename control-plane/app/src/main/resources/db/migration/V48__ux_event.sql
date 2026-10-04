-- UX 이벤트 (디자인 설계서 §16.1). Analytics 가 소유한다.
--
-- 누가 했는지가 아니라 무엇이 쓰였나를 센다 — 계정 id 가 없다. 탭마다 바뀌는 세션 id 와 로그인 여부뿐이다
-- (§16.2 개인 식별 최소화). 속성은 서버의 스키마(EventSchema)를 통과한 것만 — 소스·입력·출력 전문은 들어올
-- 자리가 없다. 90일이 지나면 지운다.
CREATE TABLE ux_event (
    id         BIGSERIAL   PRIMARY KEY,
    name       TEXT        NOT NULL,
    props      JSONB       NOT NULL,
    session_id TEXT        NOT NULL,
    signed_in  BOOLEAN     NOT NULL,
    ui_version TEXT        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ux_event_name_idx ON ux_event (name, created_at);
CREATE INDEX ux_event_created_idx ON ux_event (created_at);

COMMENT ON TABLE ux_event IS 'UX 이벤트 (§16.1). 계정 id 없음, 스키마를 통과한 속성만, 90일 보관.';
