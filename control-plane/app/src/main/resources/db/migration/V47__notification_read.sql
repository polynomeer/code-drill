-- 알림 읽음 표시 (docs/ui-overhaul.md §4 "알림"). Notification 모듈이 소유한다.
--
-- 알림 자체는 저장하지 않는다 — 대회·레이팅·게시판·전이·제재의 사실에서 그때그때 만든다. 저장하는 것은
-- "어디까지 읽었나" 하나다. 그보다 뒤에 알릴 만해진 것이 안 읽음이다.
CREATE TABLE notification_read (
    user_id    TEXT        PRIMARY KEY,
    read_until TIMESTAMPTZ NOT NULL
);

COMMENT ON TABLE notification_read IS '사용자가 알림을 어디까지 읽었나. 알림 자체는 사실에서 만든다.';
