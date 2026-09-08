"""로컬·CI 에서 관리자 API 를 부를 때 쓰는 운영자 계정 (기술 설계서 §11.2).

**공유 토큰이 없다.** 예전에는 `ADMIN_OPERATORS` 에 `<이름>:<토큰>:<역할>` 이 들어 있었고,
그 토큰은 서버와 스크립트 양쪽의 프로세스 목록에 평문으로 보였다. 이제 운영자도 사람과
같은 방식으로 로그인하고, 역할은 DB 의 부여 기록이 정한다.

매듭을 푸는 것은 부트스트랩 하나다. **역할 표가 완전히 비어 있을 때만**, 서버 설정
`codedrill.admin.bootstrap-email` 의 주인이 SECURITY_ADMIN 을 받는다. 그 다음부터는
그 계정이 다른 계정에 역할을 준다.

    export ADMIN_BOOTSTRAP_EMAIL=admin@example.test   # 앱과 같은 값이어야 한다

비밀번호는 이 스크립트가 만들어 `~/.codedrill/seed-operators.json` 에 둔다. 환경변수로
받지 않는 이유는 그러면 다시 프로세스 목록에 남기 때문이다. 저장소 밖에 두는 이유는
실수로 커밋될 자리를 만들지 않기 위해서다.
"""

from __future__ import annotations

import json
import os
import pathlib
import secrets
import sys
import urllib.error
import urllib.request

BASE = "http://localhost:8080/api/v1"
BOOTSTRAP_ENV = "ADMIN_BOOTSTRAP_EMAIL"
STORE = pathlib.Path.home() / ".codedrill" / "seed-operators.json"

# 로컬 시딩이 필요로 하는 구성. 역할마다 둘씩 있어야 2인 승인을 확인할 수 있고,
# 두 단계를 모두 할 수 있는 계정이 하나씩 있어야 권한이 과한 계정에서도 등록자·승인자
# 분리가 남아 있는지 볼 수 있다 (§11.2).
SEED_OPERATORS = {
    "content-editor": ["CONTENT_EDITOR"],
    "release-manager": ["CONTENT_EDITOR", "PUBLISHER"],
    "release-approver": ["PUBLISHER"],
    "judge-operator": ["JUDGE_OPERATOR", "REVIEWER"],
    "judge-reviewer": ["REVIEWER"],
    "security-admin": ["SECURITY_ADMIN"],
}


class Operator:
    def __init__(self, name: str, user_id: str, token: str, roles: set[str]) -> None:
        self.name = name
        self.user_id = user_id
        self.token = token
        self.roles = roles

    @property
    def headers(self) -> dict[str, str]:
        return {"Authorization": f"Bearer {self.token}"}


def _call(method: str, path: str, body=None, headers=None):
    data = json.dumps(body).encode() if body is not None else None
    request = urllib.request.Request(f"{BASE}{path}", data=data, method=method)
    request.add_header("Content-Type", "application/json")
    for key, value in (headers or {}).items():
        request.add_header(key, value)
    try:
        with urllib.request.urlopen(request, timeout=10) as response:
            payload = response.read()
            return response.status, (json.loads(payload) if payload else None)
    except urllib.error.HTTPError as error:
        payload = error.read()
        try:
            return error.code, json.loads(payload) if payload else None
        except json.JSONDecodeError:
            return error.code, None
    except urllib.error.URLError as error:
        print(f"제어 영역에 연결하지 못했다: {error.reason}")
        sys.exit(2)


def _store_read() -> dict:
    if not STORE.exists():
        return {}
    return json.loads(STORE.read_text())


def _store_write(data: dict) -> None:
    STORE.parent.mkdir(parents=True, exist_ok=True)
    STORE.write_text(json.dumps(data, indent=2) + "\n")
    # 비밀번호가 들어 있다. 같은 머신의 다른 계정이 읽을 이유가 없다.
    STORE.chmod(0o600)


def _session(email: str, password: str, display_name: str) -> tuple[str, str]:
    """계정을 만들거나 로그인해 (계정 id, access token) 을 돌려준다."""
    status, body = _call(
        "POST", "/auth/register",
        {"email": email, "displayName": display_name, "password": password},
    )
    if status in (200, 201):
        return body["userId"], body["accessToken"]

    status, body = _call("POST", "/auth/login", {"email": email, "password": password})
    if status == 200:
        return body["userId"], body["accessToken"]

    print(f"운영자 계정 로그인 실패 ({email}): {status} {body}")
    print(f"{STORE} 를 지우고 다시 돌리면 계정을 새로 만든다.")
    sys.exit(2)


def _ensure_seeded() -> dict:
    """운영자 계정과 역할을 갖춰 두고, 계정별 세션을 돌려준다.

    여러 번 돌아도 같은 결과여야 한다 — 계정은 이미 있으면 로그인하고, 역할은 이미
    있으면 아무 일도 하지 않는다.
    """
    bootstrap_email = os.environ.get(BOOTSTRAP_ENV, "").strip()
    if not bootstrap_email:
        print(f"{BOOTSTRAP_ENV} 가 비어 있다. 앱의 codedrill.admin.bootstrap-email 과 같은 값이어야 한다.")
        print("docs/running-locally.md 의 '관리자 계정' 절을 따른다.")
        sys.exit(2)

    store = _store_read()
    passwords = store.setdefault("passwords", {})

    def password_for(key: str) -> str:
        if key not in passwords:
            passwords[key] = secrets.token_urlsafe(24)
        return passwords[key]

    # 1. 부트스트랩 계정. 역할 표가 비어 있으면 첫 호출에서 SECURITY_ADMIN 을 받는다.
    root_id, root_token = _session(
        bootstrap_email, password_for("bootstrap"), "bootstrap admin",
    )
    _store_write(store)

    status, _ = _call("GET", "/admin/operators", headers={"Authorization": f"Bearer {root_token}"})
    if status != 200:
        print(f"부트스트랩 계정이 SECURITY_ADMIN 을 받지 못했다: {status}")
        print(f"앱의 codedrill.admin.bootstrap-email 이 {bootstrap_email} 인지,")
        print("그리고 이미 다른 계정에 역할이 부여돼 있지 않은지 본다 (부트스트랩은 표가 빌 때만 열린다).")
        sys.exit(2)

    # 2. 나머지 운영자 계정과 역할.
    sessions = {}
    for name, roles in SEED_OPERATORS.items():
        email = f"{name}@{bootstrap_email.split('@', 1)[1]}"
        user_id, token = _session(email, password_for(name), name)
        for role in roles:
            _call(
                "POST", f"/admin/operators/{user_id}/roles", {"role": role},
                headers={"Authorization": f"Bearer {root_token}"},
            )
        sessions[name] = Operator(name, user_id, token, set(roles))

    _store_write(store)
    return sessions


_CACHE: dict | None = None


def load() -> list[Operator]:
    global _CACHE
    if _CACHE is None:
        _CACHE = _ensure_seeded()
    return list(_CACHE.values())


def with_roles(*roles: str, other_than: Operator | None = None) -> Operator:
    """요구한 역할을 모두 가진 운영자 하나.

    `other_than` 을 주면 그 사람이 아닌 다른 사람을 찾는다 — 2인 승인(§11.2)을 부르는
    쪽에서 같은 사람을 두 번 쓰지 않게 하기 위해서다.

    없으면 무엇이 빠졌는지 말하고 끝낸다. 조용히 None 을 돌려주면 호출부가 인증
    오류로 착각한다.
    """
    wanted = set(roles)
    for operator in load():
        if wanted <= operator.roles and (other_than is None or operator.name != other_than.name):
            return operator

    need = ",".join(sorted(wanted))
    suffix = f" ({other_than.name} 과 다른 사람)" if other_than else ""
    print(f"{need} 역할을 가진 운영자{suffix}가 없다.")
    print("SEED_OPERATORS 가 필요한 운영자 구성을 정한다 (scripts/operators.py).")
    sys.exit(2)
