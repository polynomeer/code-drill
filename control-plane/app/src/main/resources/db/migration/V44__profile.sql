-- 공개 프로필 (docs/ui-overhaul.md §6.7).
--
-- Identity 가 소유한다. 핸들은 주소(`/u/{handle}`)에 쓰는 이름이고, 표시 이름과 따로 둔다 — 표시
-- 이름은 겹쳐도 되지만 주소는 겹치면 안 된다.
--
-- **공개는 고르는 것이다.** 기본은 비공개다. 이 제품에서 이름이 남에게 보이는 곳은 대회 순위표뿐이었고
-- 그것도 참가가 곧 동의였다. 활동 기록을 내거는 것도 같은 방식으로 본인이 켠다.
ALTER TABLE app_user ADD COLUMN handle TEXT;
ALTER TABLE app_user ADD COLUMN profile_public BOOLEAN NOT NULL DEFAULT false;

-- 핸들은 소문자로 정규화해 저장한다. 그래도 인덱스는 lower() 로 둔다 — 정규화를 잊은 쓰기가 있어도
-- 대소문자만 다른 두 핸들이 생기지 않게.
CREATE UNIQUE INDEX app_user_handle_unique ON app_user (lower(handle)) WHERE handle IS NOT NULL;

COMMENT ON COLUMN app_user.handle IS '공개 프로필 주소의 이름. 고르기 전에는 null.';
COMMENT ON COLUMN app_user.profile_public IS '공개 프로필을 남에게 보이는가. 기본은 비공개.';
