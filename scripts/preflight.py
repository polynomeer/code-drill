#!/usr/bin/env python3
"""첫 배포 전후의 점검 (docs/deploying.md — 앱 호스트 하나 + Runner 노드 하나).

    python3 scripts/preflight.py app       # 앱 호스트에서. 띄우기 전과 후에 모두 돈다
    python3 scripts/preflight.py runner    # Runner 노드에서

배포 절차는 docs/deploying.md 에 있고 이 스크립트는 그 절차의 **전제가 갖춰졌는지**와
**띄운 뒤 실제로 붙었는지**를 한 번에 본다. 문서를 읽고 빠뜨린 한 줄 — 소금이 없다, 인증서가
지난달 것이다, 작업 디렉터리가 없어 모든 제출이 SYSTEM_ERROR 다 — 이 첫 배포에서 가장
흔한 실패고, 그것은 배포한 다음이 아니라 전에 알아야 한다.

세 가지로 답한다. `✓` 는 됐다, `✗` 는 이대로는 안 뜬다(0 이 아닌 값으로 끝난다), `△` 는
뜨긴 하지만 운영이 아니다(백업이 밖으로 안 간다, 런타임이 태그로 떠 있다). 아직 띄우기
전이면 "붙었는가"류는 △ 로 남는다 — 띄운 뒤 다시 돌린다.
"""

from __future__ import annotations

import os
import pathlib
import shutil
import socket
import ssl
import subprocess
import sys
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parents[1]
CERTS = ROOT / "deploy" / "certs"

sys.path.insert(0, str(ROOT / "scripts"))

results: list[tuple[str, str, str]] = []


def ok(label: str, detail: str = "") -> None:
    results.append(("✓", label, detail))


def bad(label: str, detail: str = "") -> None:
    results.append(("✗", label, detail))


def warn(label: str, detail: str = "") -> None:
    results.append(("△", label, detail))


def sh(*args: str) -> subprocess.CompletedProcess:
    return subprocess.run(args, capture_output=True, text=True)


# --- 공통 ---

def check_docker() -> None:
    if shutil.which("docker") is None:
        bad("docker 가 있다", "없다")
        return
    version = sh("docker", "compose", "version")
    if version.returncode != 0:
        bad("docker compose 가 있다", version.stderr.strip()[:120])
        return
    ok("docker 와 compose 가 있다", version.stdout.strip())
    info = sh("docker", "info", "--format", "{{.ServerVersion}}")
    if info.returncode != 0:
        bad("컨테이너 데몬에 붙는다", info.stderr.strip()[:120])
    else:
        ok("컨테이너 데몬에 붙는다", f"server {info.stdout.strip()}")


def check_tag() -> str | None:
    tag = os.environ.get("CODEDRILL_TAG")
    if not tag or tag == "dev":
        bad("CODEDRILL_TAG 가 커밋 SHA 다", "없거나 dev 다 — 롤백의 근거가 되는 것은 SHA 뿐이다 (docs/deploying.md 태그 규칙)")
        return None
    ok("CODEDRILL_TAG 가 정해져 있다", tag)
    return tag


def check_image(name: str, tag: str | None) -> None:
    if tag is None:
        return
    image = f"codedrill/{name}:{tag}"
    if sh("docker", "image", "inspect", image).returncode == 0:
        ok(f"이미지가 있다: {name}", image)
    else:
        bad(f"이미지가 있다: {name}", f"{image} 가 없다 — 레지스트리에서 받거나 deploy/Dockerfile 로 만든다")


def check_env(name: str, why: str, required: bool = True) -> str | None:
    value = os.environ.get(name)
    if value:
        ok(f"{name} 이 있다", why)
        return value
    (bad if required else warn)(f"{name} 이 있다", f"없다 — {why}")
    return None


def check_disk(path: pathlib.Path, min_gb: int) -> None:
    usage = shutil.disk_usage(path)
    free_gb = usage.free // (1024 ** 3)
    (ok if free_gb >= min_gb else warn)(f"{path} 에 {min_gb}GB 이상 남았다", f"{free_gb}GB")


def check_tls(host: str, port: int, identity: pathlib.Path, label: str) -> None:
    """브로커에 mTLS 로 붙어 본다. 인증서가 맞으면 핸드셰이크가 되고, 아니면 여기서 끊긴다."""
    try:
        context = ssl.create_default_context(cafile=str(identity / "ca.pem"))
        context.load_cert_chain(str(identity / "cert.pem"), str(identity / "key.pem"))
        with socket.create_connection((host, port), timeout=5) as raw:
            with context.wrap_socket(raw, server_hostname=host) as tls:
                ok(label, f"{host}:{port} {tls.version()} — 서버 CN {dict(x[0] for x in tls.getpeercert()['subject'])['commonName']}")
    except FileNotFoundError as error:
        bad(label, f"신원 파일이 없다: {error.filename}")
    except (OSError, ssl.SSLError) as error:
        warn(label, f"{host}:{port} 에 붙지 못했다 — 아직 안 띄웠거나 인증서·SAN 이 어긋난다: {error}")


def check_http(url: str, label: str) -> bool:
    try:
        with urllib.request.urlopen(url, timeout=5) as response:
            body = response.read(200).decode(errors="replace")
        ok(label, f"{url} → {body.strip()[:60]}")
        return True
    except Exception as error:  # noqa: BLE001 — 붙지 못한 이유가 무엇이든 한 줄로 보인다
        warn(label, f"{url} — {error}")
        return False


def check_cron(needle: str, label: str) -> None:
    crontab = sh("crontab", "-l")
    if crontab.returncode == 0 and needle in crontab.stdout:
        ok(label)
    else:
        warn(label, f"crontab 에 `{needle}` 줄이 없다 (docs/deploying.md)")


# --- 앱 호스트 ---

def app() -> None:
    check_docker()
    tag = check_tag()
    for name in ("control-plane", "orchestrator", "web"):
        check_image(name, tag)

    import certs  # scripts/certs.py — 같은 자리의 인증서를 본다
    if not (CERTS / "ca.pem").exists():
        bad("인증서가 있다", "deploy/certs/ 가 비어 있다 — python3 scripts/certs.py --broker-host <이름>")
    else:
        leaves = [CERTS / "ca.pem", *certs.leaf_certs()]
        missing = [c for c in leaves if not c.exists()]
        if missing:
            bad("인증서가 있다", "없다: " + ", ".join(str(m.relative_to(ROOT)) for m in missing))
        else:
            worst = min(certs.days_left(c) for c in leaves)
            (ok if worst >= certs.WARN_DAYS else bad)("인증서가 살아 있다", f"가장 짧은 것이 {worst}일 남음")
            if certs.days_left(CERTS / "ca.pem") < 365 * 2:
                warn("CA 가 넉넉하다", "2년 안에 만료된다 — 새 CA(10년)로 다시 만들면 모든 노드가 같이 바뀐다")

    check_env("AUTH_ORIGIN_SALT", "출처 해시의 소금. 배포마다 달라야 한다 (§10.2)")
    check_env("STORAGE_CONTROL_PLANE_SECRET", "제어 영역의 스토어 비밀")
    check_env("STORAGE_ORCHESTRATOR_SECRET", "오케스트레이터의 스토어 비밀")
    check_env("STORAGE_RUNNER_SECRET", "Runner 노드에 줄 스토어 비밀 — 여기서 만들고 저쪽에 옮긴다")
    check_env("ADMIN_BOOTSTRAP_EMAIL", "첫 관리자. 없으면 아무도 문제를 공개하지 못한다", required=False)
    if os.environ.get("AUTH_TRUSTED_PROXY") == "true":
        warn("AUTH_TRUSTED_PROXY", "켜져 있다 — 앞에 프록시가 정말 있어야 한다. 없으면 아무나 X-Forwarded-For 로 출처를 꾸민다")

    check_disk(ROOT, 20)

    if shutil.which("mc") is None:
        warn("mc 가 있다", "없다 — 백업이 밖으로 가지 못한다 (docs/deploying.md 백업 절)")
    elif sh("mc", "alias", "ls", "offsite").returncode != 0:
        warn("오프사이트 별칭 offsite 가 있다", "mc alias set offsite <끝점> <키> <비밀>")
    else:
        ok("오프사이트 별칭 offsite 가 있다")
    check_cron("backup.py create", "백업 cron 이 걸려 있다")
    check_cron("backup.py verify", "복원 리허설 cron 이 걸려 있다")
    check_cron("certs.py --check", "인증서 만료 감시 cron 이 걸려 있다")

    # 띄운 뒤에 뜻이 있는 것. 아직이면 △ 로 남는다.
    broker_port = int(os.environ.get("RABBITMQ_TLS_PORT", "5671"))
    check_tls("localhost", broker_port, CERTS / "control-plane", "브로커에 제어 영역 신원으로 붙는다")
    control = f"http://localhost:{os.environ.get('CONTROL_PLANE_PORT', '8080')}"
    if check_http(f"{control}/actuator/health", "제어 영역이 떠 있다"):
        check_http(f"{control}/api/v1/problems?size=1", "문제 목록이 나온다 (이미지에 구운 패키지)")
    check_http(f"http://localhost:{os.environ.get('ORCHESTRATOR_PORT', '8081')}/actuator/health", "오케스트레이터가 떠 있다")
    check_http(f"http://localhost:{os.environ.get('WEB_PORT', '8090')}/", "웹이 떠 있다")


# --- Runner 노드 ---

def runner() -> None:
    check_docker()
    tag = check_tag()
    check_image("runner-agent", tag)

    work = pathlib.Path("/var/lib/codedrill/work")
    if not work.is_dir():
        bad("샌드박스 작업 디렉터리가 있다", f"{work} 가 없다 — 없으면 데몬이 빈 디렉터리를 붙여 모든 제출이 SYSTEM_ERROR 다")
    elif not os.access(work, os.W_OK):
        bad("샌드박스 작업 디렉터리에 쓸 수 있다", str(work))
    else:
        ok("샌드박스 작업 디렉터리가 있다", str(work))
    if not pathlib.Path("/var/run/docker.sock").exists():
        bad("컨테이너 소켓이 있다", "/var/run/docker.sock — 샌드박스 컨테이너를 띄울 길이다")

    broker = check_env("BROKER_URL", "amqps://<브로커 호스트>:5671")
    if broker and not broker.startswith("amqps://"):
        bad("BROKER_URL 이 amqps 다", f"{broker} — 배포용 브로커에는 평문 리스너가 없다")
    tls_dir = check_env("BROKER_TLS", "runner 신원 디렉터리 (cert.pem·key.pem·ca.pem)")
    check_env("STORAGE_ENDPOINT", "http://<스토어 호스트>:9000")
    check_env("STORAGE_RUNNER_SECRET", "앱 호스트에서 만든 Runner 의 스토어 비밀")

    for name, default in (("KOTLIN_IMAGE", "eclipse-temurin:21-jre"), ("JAVA_IMAGE", "eclipse-temurin:21-jdk"), ("PYTHON_IMAGE", "python:3.12-alpine")):
        image = os.environ.get(name, default)
        if "@sha256:" not in image:
            warn(f"{name} 이 digest 로 고정돼 있다", f"{image} — 태그는 같은 이름이 다른 내용을 가리킬 수 있다 (§5.5)")
        else:
            ok(f"{name} 이 digest 로 고정돼 있다")
        if sh("docker", "image", "inspect", image).returncode == 0:
            ok(f"샌드박스 런타임이 받아져 있다: {name}")
        else:
            warn(f"샌드박스 런타임이 받아져 있다: {name}", f"{image} — 첫 제출이 이미지를 받느라 시간 초과가 된다. 미리 docker pull")

    check_disk(pathlib.Path("/var/lib"), 10)

    if broker and tls_dir:
        host = broker.removeprefix("amqps://").split("/")[0]
        hostname, _, port = host.partition(":")
        check_tls(hostname, int(port or 5671), pathlib.Path(tls_dir), "브로커에 runner 신원으로 붙는다")
    endpoint = os.environ.get("STORAGE_ENDPOINT")
    if endpoint:
        check_http(f"{endpoint.rstrip('/')}/minio/health/live", "스토어에 닿는다")
    check_http(f"http://localhost:{os.environ.get('RUNNER_PORT', '8082')}/actuator/health", "Runner 가 떠 있다")


def main() -> int:
    role = sys.argv[1] if len(sys.argv) > 1 else ""
    if role not in ("app", "runner"):
        print(__doc__)
        return 2
    (app if role == "app" else runner)()
    for mark, label, detail in results:
        print(f"  {mark} {label}" + (f": {detail}" if detail else ""))
    failed = sum(1 for mark, _, _ in results if mark == "✗")
    warned = sum(1 for mark, _, _ in results if mark == "△")
    print(f"\n✗ {failed}, △ {warned}, ✓ {len(results) - failed - warned}")
    if failed:
        print("✗ 가 있으면 이대로는 뜨지 않는다. docs/deploying.md 의 해당 절을 본다.")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
