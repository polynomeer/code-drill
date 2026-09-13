#!/usr/bin/env python3
"""브로커 mTLS 인증서를 만든다 (기술 설계서 §11.2 워크로드 신원).

    python3 scripts/certs.py                       # deploy/certs/ 에 CA·브로커·신원 셋
    python3 scripts/certs.py --broker-host judge.example.com   # 브로커 SAN 을 더한다

만드는 것:

    ca.pem, ca-key.pem                 이 배포만의 CA. 키는 여기 말고 어디에도 두지 않는다.
    broker/broker.pem, broker-key.pem  브로커 서버 인증서. SAN 은 rabbitmq·localhost·--broker-host.
    <신원>/cert.pem, key.pem, ca.pem   control-plane, orchestrator, runner. CN 이 곧 브로커 사용자.

**신원 디렉터리 하나가 곧 그 노드가 들고 가는 전부다.** Runner 노드에는 runner/ 만 준다 —
거기서 유출되어도 얻는 것은 Runner 의 출구뿐이다 (JudgeIdentity.exchange).

이미 있는 파일은 건드리지 않는다. 다시 만들려면 지우고 돌린다. 인증서는 1년이고, 만료 전에
바꾸는 것은 지금은 사람의 일이다.
"""
from __future__ import annotations

import argparse
import pathlib
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
OUT = ROOT / "deploy" / "certs"
IDENTITIES = ("control-plane", "orchestrator", "runner")
DAYS = "365"


def sh(*args: str) -> None:
    subprocess.run(args, check=True, capture_output=True)


def ensure_ca() -> None:
    if (OUT / "ca.pem").exists():
        return
    sh("openssl", "req", "-x509", "-newkey", "ec", "-pkeyopt", "ec_paramgen_curve:prime256v1",
       "-nodes", "-days", DAYS, "-subj", "/CN=codedrill-broker-ca",
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
    print(f"  {directory.relative_to(ROOT)}/")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--broker-host", action="append", default=[],
                        help="브로커 서버 인증서에 더할 호스트 이름 (Runner 노드가 부르는 이름)")
    args = parser.parse_args()

    OUT.mkdir(parents=True, exist_ok=True)
    print(f"인증서 → {OUT.relative_to(ROOT)}/")
    ensure_ca()
    issue(OUT / "broker", "rabbitmq", ["rabbitmq", "localhost", *args.broker_host], "serverAuth")
    for identity in IDENTITIES:
        issue(OUT / identity, identity, None, "clientAuth")
    return 0


if __name__ == "__main__":
    sys.exit(main())
