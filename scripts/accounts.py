"""스크립트가 쓸 사용자 계정 (기술 설계서 §11.2).

제출·초안·기록은 전부 인증을 요구한다. 스크립트는 매번 새 계정을 만들어 쓴다 —
고정 계정을 두면 그 비밀번호가 저장소에 남고, 실행할 때마다 남의 기록이 섞인다.
"""

from __future__ import annotations

import json
import os
import urllib.error
import urllib.request
import uuid

# 제어 영역 주소. scripts/up.py 가 포트를 비켜 갔으면 그 값을 넘겨 준다.
BASE = os.environ.get("CODEDRILL_BASE", "http://localhost:8080").rstrip("/") + "/api/v1"


class Account:
    """가입된 계정 하나. `headers` 를 요청에 그대로 실으면 된다."""

    def __init__(self, email: str, password: str, session: dict) -> None:
        self.email = email
        # 계정 삭제가 비밀번호를 다시 묻는다 (§11.3). 되돌릴 수 없는 요청이라 세션만으로는
        # 부족하고, 그래서 스크립트도 들고 있어야 한다.
        self.password = password
        self.user_id = session["userId"]
        self.display_name = session["displayName"]
        self.access_token = session["accessToken"]
        self.refresh_token = session["refreshToken"]

    @property
    def headers(self) -> dict[str, str]:
        return {"Authorization": f"Bearer {self.access_token}"}


def post(path: str, body: dict) -> dict:
    request = urllib.request.Request(
        f"{BASE}{path}", data=json.dumps(body).encode(), method="POST",
    )
    request.add_header("Content-Type", "application/json")
    with urllib.request.urlopen(request, timeout=10) as response:
        return json.loads(response.read())


def create(prefix: str = "drill") -> Account:
    """새 계정을 만들고 로그인된 상태로 돌려준다."""
    email = f"{prefix}-{uuid.uuid4().hex[:10]}@example.test"
    # 무작위 비밀번호. 저장소에도 환경변수에도 남기지 않는다 — 이 프로세스 안에만 있다.
    password = uuid.uuid4().hex + uuid.uuid4().hex[:8]
    return Account(email, password, post("/auth/register", {
        "email": email, "displayName": prefix, "password": password,
    }))
