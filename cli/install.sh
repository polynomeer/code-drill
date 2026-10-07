#!/bin/sh
# codedrill 설치 — macOS·Linux. Windows 는 Scoop 이나 Releases 의 zip.
#
#   curl -fsSL https://github.com/polynomeer/code-drill/releases/latest/download/install.sh | sh
#
# 받은 묶음을 checksums.txt 와 맞춰 본다. cosign 이 있으면 checksums.txt 의 서명도 확인한다.
# CODEDRILL_VERSION=1.0.0 으로 판을, CODEDRILL_INSTALL_DIR 로 놓을 곳을 정한다 (기본 ~/.local/bin).
set -eu

repo="polynomeer/code-drill"
dir="${CODEDRILL_INSTALL_DIR:-$HOME/.local/bin}"

case "$(uname -s)" in
  Darwin) os=darwin ;;
  Linux) os=linux ;;
  *) echo "이 스크립트는 macOS·Linux 용입니다 — Windows 는 scoop install codedrill" >&2; exit 1 ;;
esac
case "$(uname -m)" in
  x86_64 | amd64) arch=amd64 ;;
  arm64 | aarch64) arch=arm64 ;;
  *) echo "지원하지 않는 CPU 입니다: $(uname -m)" >&2; exit 1 ;;
esac

if [ -n "${CODEDRILL_VERSION:-}" ]; then
  version="${CODEDRILL_VERSION#v}"
else
  version="$(curl -fsSL -o /dev/null -w '%{url_effective}' "https://github.com/$repo/releases/latest" | sed 's|.*/tag/v||')"
fi
base="https://github.com/$repo/releases/download/v$version"
archive="codedrill_${version}_${os}_${arch}.tar.gz"

tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
echo "codedrill $version ($os/$arch) 받는 중…"
curl -fsSL -o "$tmp/$archive" "$base/$archive"
curl -fsSL -o "$tmp/checksums.txt" "$base/checksums.txt"

if command -v cosign >/dev/null 2>&1; then
  curl -fsSL -o "$tmp/checksums.txt.sig" "$base/checksums.txt.sig"
  curl -fsSL -o "$tmp/checksums.txt.pem" "$base/checksums.txt.pem"
  cosign verify-blob \
    --certificate "$tmp/checksums.txt.pem" --signature "$tmp/checksums.txt.sig" \
    --certificate-identity-regexp "^https://github.com/$repo/.github/workflows/cli.yml@refs/tags/v" \
    --certificate-oidc-issuer https://token.actions.githubusercontent.com \
    "$tmp/checksums.txt" >/dev/null
  echo "서명 확인"
fi

expected="$(grep " $archive\$" "$tmp/checksums.txt" | cut -d' ' -f1)"
if command -v sha256sum >/dev/null 2>&1; then
  actual="$(sha256sum "$tmp/$archive" | cut -d' ' -f1)"
else
  actual="$(shasum -a 256 "$tmp/$archive" | cut -d' ' -f1)"
fi
if [ -z "$expected" ] || [ "$expected" != "$actual" ]; then
  echo "체크섬이 맞지 않습니다 — 설치하지 않습니다" >&2
  exit 1
fi

tar -xzf "$tmp/$archive" -C "$tmp" codedrill
mkdir -p "$dir"
install -m 0755 "$tmp/codedrill" "$dir/codedrill"
echo "설치했습니다: $dir/codedrill"
case ":$PATH:" in
  *":$dir:"*) ;;
  *) echo "PATH 에 $dir 를 더하세요" ;;
esac
