#!/usr/bin/env python3
"""검증 보고서를 등록·공개로 잇는다 (기술 설계서 §6.3 → §3.2).

`./gradlew :judge:runner-agent:validateContent` 가 남긴 보고서를 읽어, 통과한 버전만
등록하고 공개한다. 보고서 digest 를 그대로 넘기므로 **검증하지 않은 패키지는 공개할 수
없다** — 패키지를 고치면 digest 가 달라져 등록이 거부된다.

§11.2 는 등록자와 승인자가 달라야 한다고 요구한다. 이 스크립트는 로컬·CI 시딩용이라
설정된 운영자 중 서로 다른 둘을 쓴다. **사람이 하는 공개에는 쓰지 않는다** — 그 경우
두 사람이 각자 등록과 승인을 해야 2인 승인이 의미를 갖는다.

사용법:
    ./gradlew :judge:runner-agent:validateContent
    python3 scripts/publish-content.py
"""

from __future__ import annotations

import json
import pathlib
import sys
import urllib.error
import urllib.request

import operators

BASE = "http://localhost:8080/api/v1/admin"
REPORT_DIR = pathlib.Path("content/reports")


def post(path: str, body: dict | None, actor) -> tuple[int, dict | None]:
    data = json.dumps(body).encode() if body is not None else None
    request = urllib.request.Request(f"{BASE}{path}", data=data, method="POST")
    request.add_header("Content-Type", "application/json")
    for key, value in actor.headers.items():
        request.add_header(key, value)
    try:
        with urllib.request.urlopen(request, timeout=10) as response:
            payload = response.read()
            return response.status, (json.loads(payload) if payload else None)
    except urllib.error.HTTPError as error:
        payload = error.read()
        return error.code, (json.loads(payload) if payload else None)


def main() -> int:
    registrar = operators.with_roles("CONTENT_EDITOR")
    publisher = operators.with_roles("PUBLISHER", other_than=registrar)

    reports = sorted(REPORT_DIR.glob("*.json"))
    if not reports:
        print(f"보고서가 없다: {REPORT_DIR}")
        print("먼저 ./gradlew :judge:runner-agent:validateContent 를 돌린다.")
        return 1

    failures = 0
    for path in reports:
        report = json.loads(path.read_text())
        problem_id, _, version = report["problemVersionId"].partition("@")

        if not report["passed"]:
            failed = [c["stage"] for c in report["checks"] if not c["passed"]]
            print(f"SKIP  {report['problemVersionId']}: 검증 실패 ({', '.join(failed)})")
            failures += 1
            continue

        status, body = post(
            f"/problems/{problem_id}/versions",
            {
                "version": int(version),
                "packageDigest": report["packageDigest"],
                "reportDigest": report["reportDigest"],
                "validatorVersion": report["validatorVersion"],
            },
            registrar,
        )
        # 이미 등록된 같은 버전은 정상이다. 시딩은 여러 번 돌아도 같은 결과여야 한다.
        # 파이프라인만 바뀐 경우는 200 으로 보고서가 갈린다 (§15.3).
        if status not in (200, 409):
            print(f"FAIL  {report['problemVersionId']}: 등록 실패 {status} {body}")
            failures += 1
            continue

        status, body = post(
            f"/problems/{problem_id}/publish",
            {
                "version": int(version),
                "reportDigest": report["reportDigest"],
                "validatorVersion": report["validatorVersion"],
            },
            publisher,
        )
        if status == 200:
            print(f"OK    {report['problemVersionId']} 공개됨")
        else:
            print(f"FAIL  {report['problemVersionId']}: 공개 실패 {status} {body}")
            failures += 1

    print()
    print(f"{len(reports) - failures}/{len(reports)} 공개")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
