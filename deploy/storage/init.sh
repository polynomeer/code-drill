#!/bin/sh
# 버킷 하나를 만든다 (§8.3). 스토어가 뜬 뒤 한 번 돌고 끝난다.
#
# 앱은 버킷을 만들지 않는다 — 만들 수 있는 자격증명은 너무 넓다. 신원과 권한은 스토어가
# 시작할 때 storage/start.sh 가 넣는다; 여기서는 버킷만 본다. 다시 돌려도 해가 없다.
set -eu

BUCKET="${STORAGE_BUCKET:-codedrill}"
MASTER="${STORAGE_MASTER:-storage:9333}"

echo "s3.bucket.create -name $BUCKET" | weed shell -master="$MASTER" >/dev/null 2>&1 || true
echo "s3.bucket.list" | weed shell -master="$MASTER" | grep -q "$BUCKET"
echo "스토어 준비: 버킷 $BUCKET (신원은 스토어가 뜰 때 설정된다 — storage/start.sh)"
