#!/bin/sh
# 오브젝트 스토어 초기화 (기술 설계서 §8.3, §11.2 최소 권한).
#
# docker-compose.apps.yml 의 storage-init 이 스토어가 뜬 뒤 한 번 돌린다. 버킷 하나와
# 신원 둘을 만든다 — 앱은 버킷을 만들지 않는다. 만들 수 있는 자격증명은 너무 넓다.
#
#   control-plane 제출 소스와 워크스페이스를 올리고 지운다 — sources/·workspaces/ 아래만
#   orchestrator  번들과 숨은 스위트를 올린다. 이 버킷에 읽고 쓴다
#   runner        번들·스위트·소스·워크스페이스를 받는다. 이 버킷을 읽기만 한다
#
# Runner 노드를 잃어도 잃는 것은 읽기뿐이다. 번들을 바꿔치기해 다른 테스트로 채점하게
# 만들 수는 없고, 그 시도는 어차피 Runner 자신의 digest 대조에서 걸린다.
#
# 다시 돌려도 해가 없다. 있는 것은 그대로 두고 비밀번호만 새로 적는다.
set -eu

: "${MINIO_ROOT_USER:?}" "${MINIO_ROOT_PASSWORD:?}"
: "${STORAGE_ORCHESTRATOR_SECRET:?오케스트레이터의 스토어 비밀이 필요하다}"
: "${STORAGE_RUNNER_SECRET:?Runner 의 스토어 비밀이 필요하다}"
: "${STORAGE_CONTROL_PLANE_SECRET:?제어 영역의 스토어 비밀이 필요하다}"
BUCKET="${STORAGE_BUCKET:-codedrill}"

mc alias set store http://minio:9000 "$MINIO_ROOT_USER" "$MINIO_ROOT_PASSWORD" >/dev/null
mc mb --ignore-existing "store/$BUCKET"

policy() {
  name=$1; shift
  cat > "/tmp/$name.json" <<JSON
{"Version": "2012-10-17", "Statement": [{"Effect": "Allow", "Action": [$1], "Resource": ["arn:aws:s3:::$BUCKET", "arn:aws:s3:::$BUCKET/*"]}]}
JSON
  mc admin policy create store "$name" "/tmp/$name.json" >/dev/null 2>&1 || true
}
policy codedrill-read '"s3:GetObject", "s3:GetBucketLocation", "s3:ListBucket"'
policy codedrill-write '"s3:GetObject", "s3:PutObject", "s3:GetBucketLocation", "s3:ListBucket"'

# 제어 영역은 sources/·workspaces/ 아래만 — 올리고, 재채점 때 다시 묻고, 삭제 요청에 지운다.
# 번들과 숨은 스위트는 못 건드린다.
cat > /tmp/codedrill-sources.json <<JSON
{"Version": "2012-10-17", "Statement": [
  {"Effect": "Allow", "Action": ["s3:GetBucketLocation", "s3:ListBucket"], "Resource": ["arn:aws:s3:::$BUCKET"]},
  {"Effect": "Allow", "Action": ["s3:GetObject", "s3:PutObject", "s3:DeleteObject"], "Resource": ["arn:aws:s3:::$BUCKET/sources/*", "arn:aws:s3:::$BUCKET/workspaces/*"]}
]}
JSON
mc admin policy create store codedrill-sources /tmp/codedrill-sources.json >/dev/null 2>&1 || true

# add 는 있으면 비밀번호를 바꾼다. attach 는 이미 붙어 있으면 실패하므로 무시한다.
mc admin user add store orchestrator "$STORAGE_ORCHESTRATOR_SECRET"
mc admin policy attach store codedrill-write --user orchestrator >/dev/null 2>&1 || true
mc admin user add store runner "$STORAGE_RUNNER_SECRET"
mc admin policy attach store codedrill-read --user runner >/dev/null 2>&1 || true
mc admin user add store control-plane "$STORAGE_CONTROL_PLANE_SECRET"
mc admin policy attach store codedrill-sources --user control-plane >/dev/null 2>&1 || true

echo "스토어 준비: 버킷 $BUCKET, 신원 control-plane(sources/·workspaces/ 읽기·쓰기·삭제) orchestrator(읽기·쓰기) runner(읽기)"
