#!/usr/bin/env python3
"""브로커 mTLS 인증서를 만든다 (기술 설계서 §11.2 워크로드 신원).

    python3 scripts/certs.py                       # deploy/certs/ 에 CA·브로커·신원 셋
    python3 scripts/certs.py --broker-host judge.example.com   # 브로커 SAN 을 더한다
    python3 scripts/certs.py --check               # 남은 날수. 30일 안이면 0 이 아닌 값
    python3 scripts/certs.py --renew               # CA 는 두고 브로커·신원 인증서를 새로 발급

만드는 것:

    ca.pem, ca-key.pem                 이 배포만의 CA. 키는 여기 말고 어디에도 두지 않는다.
    broker/broker.pem, broker-key.pem  브로커 서버 인증서. SAN 은 rabbitmq·localhost·--broker-host.
    <신원>/cert.pem, key.pem, ca.pem   control-plane, orchestrator, runner. CN 이 곧 브로커 사용자.

**신원 디렉터리 하나가 곧 그 노드가 들고 가는 전부다.** Runner 노드에는 runner/ 만 준다 —
거기서 유출되어도 얻는 것은 Runner 의 출구뿐이다 (JudgeIdentity.exchange).

이미 있는 파일은 건드리지 않는다. CA 는 10년, 그 아래 인증서는 1년이다 — `--renew` 가 CA 는
그대로 두고 아래 것만 다시 발급하므로 갱신에 신뢰(ca.pem)는 바뀌지 않고, 바뀐 디렉터리를
그 노드에 옮겨 컨테이너를 다시 띄우면 된다. 갱신 시점은 `--check` 를 cron 에 걸어 안다
(docs/deploying.md 인증서 절). CA 자체를 바꾸는 것은 전부 지우고 다시 만드는 것이고, 그때는
모든 노드가 한 번에 바뀐다.
"""
from __future__ import annotations

import argparse
import datetime
import os
import pathlib
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
# 시험에서만 다른 곳에 만든다. 운영의 자리는 하나다.
OUT = pathlib.Path(os.environ.get("CERTS_OUT", str(ROOT / "deploy" / "certs")))
IDENTITIES = ("control-plane", "orchestrator", "runner")
# CA 는 길게, 아래 것은 짧게. 아래 것은 --renew 로 바꾸고, CA 는 바꾸는 날 모든 노드가 같이 바뀐다.
CA_DAYS = "3650"
DAYS = "365"
# 이보다 적게 남았으면 --check 가 0 이 아닌 값으로 끝난다. 한 달이면 갱신본을 노드에 옮길 시간이다.
WARN_DAYS = 30


def sh(*args: str) -> None:
    subprocess.run(args, check=True, capture_output=True)


def show(path: pathlib.Path) -> str:
    return str(path.relative_to(ROOT)) if path.is_relative_to(ROOT) else str(path)


def ensure_ca() -> None:
    if (OUT / "ca.pem").exists():
        return
    sh("openssl", "req", "-x509", "-newkey", "ec", "-pkeyopt", "ec_paramgen_curve:prime256v1",
       "-nodes", "-days", CA_DAYS, "-subj", "/CN=codedrill-broker-ca",
       "-addext", "basicConstraints=critical,CA:TRUE", "-addext", "keyUsage=critical,keyCertSign,cRLSign",
       "-keyout", str(OUT / "ca-key.pem"), "-out", str(OUT / "ca.pem"))
    print("  ca.pem")


def issue(directory: pathlib.Path, cn: str, sans: list[str] | None, usage: str) -> None:
    if (directory / "cert.pem").exists() or (directory / f"{directory.name}.pem").exists():
        return
    directory.mkdir(parents=True, exist_ok=True)
    key = directory / ("key.pem" if sans is None else "broker-key.pem")
    cert = directory / ("cert.pem" if sans is None else "broker.pem")
    csr = directory / "request.csr"
    ext = directory / "extensions.cnf"
    lines = ["basicConstraints=CA:FALSE", "keyUsage=critical,digitalSignature,keyEncipherment",
             f"extendedKeyUsage={usage}"]
    if sans:
        lines.append("subjectAltName=" + ",".join(f"DNS:{s}" for s in sans))
    ext.write_text("\n".join(lines) + "\n")
    sh("openssl", "req", "-new", "-newkey", "ec", "-pkeyopt", "ec_paramgen_curve:prime256v1",
       "-nodes", "-subj", f"/CN={cn}", "-keyout", str(key), "-out", str(csr))
    sh("openssl", "x509", "-req", "-in", str(csr), "-CA", str(OUT / "ca.pem"), "-CAkey", str(OUT / "ca-key.pem"),
       "-CAcreateserial", "-days", DAYS, "-extfile", str(ext), "-out", str(cert))
    csr.unlink()
    ext.unlink()
    # 디렉터리 하나가 그 쪽이 들고 가는 전부다: 자기 인증서, 자기 키, 그리고 상대를 믿을 CA.
    (directory / "ca.pem").write_bytes((OUT / "ca.pem").read_bytes())
    print(f"  {show(directory)}/")


def leaf_certs() -> list[pathlib.Path]:
    """CA 아래의 인증서 파일. --renew 가 지우고 --check 가 본다."""
    return [OUT / "broker" / "broker.pem"] + [OUT / identity / "cert.pem" for identity in IDENTITIES]


def days_left(cert: pathlib.Path) -> int:
    out = subprocess.run(["openssl", "x509", "-in", str(cert), "-noout", "-enddate"],
                         check=True, capture_output=True, text=True).stdout.strip()
    # notAfter=Sep 13 11:56:47 2027 GMT
    not_after = datetime.datetime.strptime(out.removeprefix("notAfter="), "%b %d %H:%M:%S %Y %Z").replace(tzinfo=datetime.timezone.utc)
    return (not_after - datetime.datetime.now(datetime.timezone.utc)).days


def check() -> int:
    """남은 날수를 찍고, 하나라도 WARN_DAYS 안이거나 없으면 1. cron 이 그 값으로 알린다."""
    worst = None
    for cert in [OUT / "ca.pem", *leaf_certs()]:
        if not cert.exists():
            print(f"  ✗ {show(cert)}: 없다")
            worst = -1
            continue
        left = days_left(cert)
        mark = "✗" if left < WARN_DAYS else "·"
        print(f"  {mark} {show(cert)}: {left}일 남음")
        worst = left if worst is None else min(worst, left)
    if worst is None or worst < WARN_DAYS:
        print(f"\n{WARN_DAYS}일 안에 만료되는 인증서가 있다. python3 scripts/certs.py --renew 로 새로 발급하고 노드에 옮긴다.")
        return 1
    return 0


def renew() -> None:
    """CA 는 두고 아래 인증서를 지운다. 그 뒤 main 의 발급이 빈자리를 채운다."""
    if not (OUT / "ca.pem").exists():
        raise SystemExit("CA 가 없다. --renew 는 있는 CA 로 아래 것만 다시 발급한다 — 처음이면 그냥 돌린다.")
    if days_left(OUT / "ca.pem") < int(DAYS):
        raise SystemExit("CA 가 새 인증서보다 먼저 만료된다. deploy/certs/ 를 지우고 전부 다시 만든다 — 모든 노드가 같이 바뀐다.")
    for cert in leaf_certs():
        key = cert.with_name("broker-key.pem" if cert.name == "broker.pem" else "key.pem")
        cert.unlink(missing_ok=True)
        key.unlink(missing_ok=True)
    print("브로커·신원 인증서를 지웠다. 같은 CA 로 다시 발급한다.")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--broker-host", action="append", default=[],
                        help="브로커 서버 인증서에 더할 호스트 이름 (Runner 노드가 부르는 이름)")
    parser.add_argument("--check", action="store_true", help="남은 날수만 본다. 만료가 가까우면 0 이 아닌 값")
    parser.add_argument("--renew", action="store_true", help="CA 는 두고 브로커·신원 인증서를 새로 발급한다")
    args = parser.parse_args()

    if args.check:
        return check()

    OUT.mkdir(parents=True, exist_ok=True)
    if args.renew:
        renew()
    print(f"인증서 → {show(OUT)}/")
    ensure_ca()
    issue(OUT / "broker", "rabbitmq", ["rabbitmq", "localhost", *args.broker_host], "serverAuth")
    for identity in IDENTITIES:
        issue(OUT / identity, identity, None, "clientAuth")
    return 0


if __name__ == "__main__":
    sys.exit(main())
