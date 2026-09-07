"""로컬·CI 에서 관리자 API 를 부를 때 쓰는 운영자 토큰 (기술 설계서 §11.2).

제어 영역과 스크립트가 같은 `ADMIN_OPERATORS` 값을 읽는다. 토큰을 스크립트에 따로
적어 두면 둘이 갈라져, 서비스는 뜨는데 스크립트만 401 을 받는 상태가 된다.

형식: `<이름>:<토큰>:<역할,역할>` 을 `;` 로 이은 것.
설정 방법은 docs/running-locally.md 에 있다.
"""

from __future__ import annotations

import os
import sys

ENV_VAR = "ADMIN_OPERATORS"


class Operator:
    def __init__(self, name: str, token: str, roles: set[str]) -> None:
        self.name = name
        self.token = token
        self.roles = roles

    @property
    def headers(self) -> dict[str, str]:
        return {"Authorization": f"Bearer {self.token}"}


def load() -> list[Operator]:
    raw = os.environ.get(ENV_VAR, "").strip()
    if not raw:
        print(f"{ENV_VAR} 가 비어 있다. 관리자 API 는 운영자가 없으면 닫혀 있다.")
        print("docs/running-locally.md 의 '관리자 토큰' 절을 따른다.")
        sys.exit(2)

    operators = []
    for entry in raw.split(";"):
        entry = entry.strip()
        if not entry:
            continue
        parts = entry.split(":")
        if len(parts) != 3:
            print(f"{ENV_VAR} 형식이 어긋났다: {entry}")
            sys.exit(2)
        name, token, roles = parts
        operators.append(Operator(name, token, {r.strip().upper() for r in roles.split(",")}))
    return operators


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
    print(f"{need} 역할을 가진 운영자{suffix}가 {ENV_VAR} 에 없다.")
    print("docs/running-locally.md 의 '관리자 토큰' 절이 필요한 운영자 구성을 정한다.")
    sys.exit(2)
