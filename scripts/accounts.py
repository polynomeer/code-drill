"""스크립트가 쓸 사용자 계정 (기술 설계서 §11.2).

제출·초안·기록은 전부 인증을 요구한다. 스크립트는 매번 새 계정을 만들어 쓴다 —
고정 계정을 두면 그 비밀번호가 저장소에 남고, 실행할 때마다 남의 기록이 섞인다.
"""

from __future__ import annotations

import json
import urllib.error
import urllib.request
import uuid

BASE = "http://localhost:8080/api/v1"


class Account:
    """가입된 계정 하나. `headers` 를 요청에 그대로 실으면 된다."""

    def __init__(self, email: str, session: dict) -> None:
        self.email = email
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
    # 무작위 비밀번호. 스크립트가 다시 쓸 일이 없으므로 어디에도 남기지 않는다.
    password = uuid.uuid4().hex + uuid.uuid4().hex[:8]
    return Account(email, post("/auth/register", {
        "email": email, "displayName": prefix, "password": password,
    }))
