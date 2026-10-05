"""떠 있는 스택의 주소 — `scripts/up.py` 가 비켜 간 포트를 그대로 쓴다.

`up.py` 는 기본 포트가 차 있으면 **비켜 간다** (docs/running-locally.md). 고른 값은
`.codedrill-stack.json` 에 남는데, 스택을 부르는 스크립트들이 그 파일을 보지 않고 8080 을
가정하고 있었다. 그래서 `CODEDRILL_BASE` 를 손으로 export 하는 것을 잊으면 스크립트가
**남의 스택** 또는 아무도 없는 포트를 두드렸고, 화면에는 "연결 거부" 나 엉뚱한 401 로만
보였다. 비켜 간 쪽이 스택이므로, 비켜 간 값을 찾는 책임도 한곳에 둔다.

순서는 셋이다.

1. 환경 변수(`CODEDRILL_BASE`) — 명시한 것이 가장 세다. CI 와 원격 스택이 이 길을 쓴다.
2. `.codedrill-stack.json` — 이 머신에서 `up.py` 가 띄운 스택.
3. 기본 포트 — 아무 기록도 없을 때.

호스트는 1번에서만 온다. 상태 파일은 이 머신의 것이므로 언제나 `localhost` 다.
"""

from __future__ import annotations

import json
import os
import pathlib
from urllib.parse import urlsplit

ROOT = pathlib.Path(__file__).resolve().parents[1]
STATE = ROOT / ".codedrill-stack.json"

# 기본 포트. up.py 의 PORTS 와 같은 값이다 — 저기는 "무엇을 띄울까"이고 여기는 "아무
# 기록도 없으면 어디를 두드릴까"다.
DEFAULTS = {
    "control-plane": 8080,
    "orchestrator": 8081,
    "runner-agent": 8082,
    "web": 5173,
}


def ports() -> dict[str, int]:
    """상태 파일에 적힌 포트. 파일이 없거나 깨졌으면 빈 사전이다."""
    try:
        recorded = json.loads(STATE.read_text()).get("ports") or {}
    except (OSError, ValueError):
        return {}
    return {name: int(port) for name, port in recorded.items() if isinstance(port, int) or str(port).isdigit()}


def port_of(app: str, default: int | None = None) -> int:
    return ports().get(app) or default or DEFAULTS.get(app) or 8080


def url_of(app: str, default: int | None = None) -> str:
    """앱 하나의 주소 (`/api/v1` 같은 경로는 부르는 쪽이 붙인다).

    `CODEDRILL_BASE` 가 있으면 그 호스트를 따른다 — 원격 스택을 가리킬 때 제어 영역만
    원격이고 오케스트레이터는 내 머신이라고 볼 이유가 없다. 포트는 제어 영역이면 그
    주소의 것이고, 다른 앱이면 상태 파일이나 기본값이다.
    """
    configured = os.environ.get("CODEDRILL_BASE", "").strip().rstrip("/")
    if not configured:
        return f"http://localhost:{port_of(app, default)}"
    if app == "control-plane":
        return configured
    split = urlsplit(configured)
    return f"{split.scheme or 'http'}://{split.hostname or 'localhost'}:{port_of(app, default)}"


def base_url() -> str:
    """제어 영역의 주소. 스크립트가 `/api/v1` 을 붙여 쓴다."""
    return url_of("control-plane")
