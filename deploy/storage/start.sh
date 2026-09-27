#!/bin/sh
# 오브젝트 스토어 (기술 설계서 §8.3, §11.2 최소 권한).
#
# SeaweedFS 의 S3 게이트웨이는 신원과 권한을 **시작할 때 설정 파일에서** 읽는다. MinIO 처럼
# 뜬 뒤에 관리 명령으로 넣는 것이 아니라서, 이 스크립트가 환경에서 비밀을 받아 파일을 쓰고
# 서버를 띄운다. 비밀은 이미지에도 설정에도 박히지 않는다.
#
#   control-plane 제출 소스와 워크스페이스를 올리고 지운다 — sources/·workspaces/ 아래만
#   orchestrator  번들과 숨은 스위트를 올린다. 이 버킷에 읽고 쓴다
#   runner        번들·스위트·소스·워크스페이스를 받는다. 이 버킷을 읽기만 한다
#
# Runner 노드를 잃어도 잃는 것은 읽기뿐이다. 번들을 바꿔치기해 다른 테스트로 채점하게
# 만들 수는 없고, 그 시도는 어차피 Runner 자신의 digest 대조에서 걸린다.
#
# SeaweedFS 에는 DeleteObject 라는 따로 떨어진 권한이 없다 — 지우기는 Write 에 들어간다.
# 그래서 제어 영역의 쓰기는 두 접두사로 좁혀 둔다. MinIO 정책에서 옮겨 올 때 유일하게
# 모양이 달라진 곳이다.
set -eu

BUCKET="${STORAGE_BUCKET:-codedrill}"
ROOT_KEY="${STORAGE_ROOT_KEY:-codedrill}"
ROOT_SECRET="${STORAGE_ROOT_SECRET:-codedrill}"
ORCHESTRATOR_SECRET="${STORAGE_ORCHESTRATOR_SECRET:-codedrill-orchestrator}"
RUNNER_SECRET="${STORAGE_RUNNER_SECRET:-codedrill-runner}"
CONTROL_PLANE_SECRET="${STORAGE_CONTROL_PLANE_SECRET:-codedrill-control-plane}"

mkdir -p /etc/seaweedfs
cat > /etc/seaweedfs/s3.json <<JSON
{
  "identities": [
    {
      "name": "root",
      "credentials": [{"accessKey": "$ROOT_KEY", "secretKey": "$ROOT_SECRET"}],
      "actions": ["Admin", "Read", "Write", "List", "Tagging"]
    },
    {
      "name": "orchestrator",
      "credentials": [{"accessKey": "orchestrator", "secretKey": "$ORCHESTRATOR_SECRET"}],
      "actions": ["Read:$BUCKET", "Write:$BUCKET", "List:$BUCKET", "Tagging:$BUCKET"]
    },
    {
      "name": "runner",
      "credentials": [{"accessKey": "runner", "secretKey": "$RUNNER_SECRET"}],
      "actions": ["Read:$BUCKET", "List:$BUCKET"]
    },
    {
      "name": "control-plane",
      "credentials": [{"accessKey": "control-plane", "secretKey": "$CONTROL_PLANE_SECRET"}],
      "actions": [
        "List:$BUCKET",
        "Read:$BUCKET/sources", "Write:$BUCKET/sources",
        "Read:$BUCKET/workspaces", "Write:$BUCKET/workspaces"
      ]
    }
  ]
}
JSON
chmod 600 /etc/seaweedfs/s3.json

exec weed server -dir=/data -ip=storage -s3 -s3.port=8333 -s3.config=/etc/seaweedfs/s3.json "$@"
