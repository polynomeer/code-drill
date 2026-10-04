#!/usr/bin/env bash
# 시각 회귀를 CI 와 같은 리눅스 컨테이너에서 돌린다 (playwright.visual.config.ts).
#
#   pnpm e2e:visual                      # 기준과 견준다
#   pnpm e2e:visual --update-snapshots   # 화면이 일부러 바뀌었으면 기준을 다시 찍는다
#
# node_modules 와 pnpm 저장소는 컨테이너 전용 볼륨에 둔다 — 호스트(macOS)의 것에는 리눅스용 네이티브 바이너리가
# 없고, 저장소를 저장소 안에 두면 작업 트리에 .pnpm-store 가 생긴다.
set -euo pipefail
IMAGE="mcr.microsoft.com/playwright:v1.63.0-noble"
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
exec docker run --rm --ipc=host \
  -v "$ROOT":/work \
  -v codedrill-visual-node-modules:/work/web/node_modules \
  -v codedrill-visual-pnpm-store:/pnpm-store \
  -w /work/web \
  -e CI="${CI:-}" \
  "$IMAGE" \
  bash -lc "corepack enable >/dev/null 2>&1 && pnpm install --frozen-lockfile --store-dir /pnpm-store --config.confirmModulesPurge=false >/dev/null && pnpm exec playwright test -c playwright.visual.config.ts $*"
