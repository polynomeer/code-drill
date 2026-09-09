#!/usr/bin/env python3
"""로컬 스택 전체를 띄운다 (기술 설계서 §0.1, docs/running-locally.md).

플랫폼 의존성(Postgres·Redis·RabbitMQ·MinIO) + 앱 세 개 + 웹 개발 서버를 한 번에
올린다. 절차와 함정은 문서가 진실의 원천이고, 이 스크립트는 그 절차를 실행할 뿐이다.

**포트를 비켜 간다.** 개발 머신에는 5432·6379·8080 을 쓰는 스택이 이미 떠 있는 경우가
흔하다. 기본 포트가 차 있으면 빈 포트를 찾아 쓰고, 고른 값을 상태 파일에 적어 다음
실행에서도 같은 포트를 쓴다 — 매번 달라지면 컨테이너가 다시 만들어지고 DB 가 날아간다.

    python3 scripts/up.py            # 띄운다
    python3 scripts/up.py --down     # 내린다 (앱만; 컨테이너는 --all 로)
    python3 scripts/up.py --status   # 지금 무엇이 어디에 떠 있나
    python3 scripts/up.py --no-web   # 웹 없이
    python3 scripts/up.py --seed     # 문제 검증·공개까지 (몇 분 걸린다)
"""

from __future__ import annotations

import argparse
import json
import os
import pathlib
import shutil
import signal
import socket
import subprocess
import sys
import time
import urllib.error
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parents[1]
STATE = ROOT / ".codedrill-stack.json"
LOGS = ROOT / "build" / "local-stack"
COMPOSE = ["docker", "compose", "-f", str(ROOT / "deploy" / "docker-compose.yml")]

BOOTSTRAP_EMAIL = "admin@codedrill.test"

# 이름 → (기본 포트, 환경변수). 차 있으면 위로 훑어 빈 포트를 고른다.
PORTS = {
    "postgres": (5432, "POSTGRES_PORT"),
    "redis": (6379, "REDIS_PORT"),
    "rabbitmq": (5672, "RABBITMQ_PORT"),
    "rabbitmq-ui": (15672, "RABBITMQ_UI_PORT"),
    "minio": (9000, "MINIO_PORT"),
    "minio-ui": (9001, "MINIO_UI_PORT"),
    "control-plane": (8080, None),
    "orchestrator": (8081, None),
    "runner-agent": (8082, None),
    "web": (5173, None),
}

APPS = ("control-plane", "orchestrator", "runner-agent", "web")

# 컨테이너 이름 → (compose 서비스, 컨테이너 안 포트). 이미 우리 컨테이너가 내주고 있는
# 포트를 "차 있다"로 읽으면 실행할 때마다 포트가 밀린다.
INFRA = {
    "postgres": ("postgres", 5432),
    "redis": ("redis", 6379),
    "rabbitmq": ("rabbitmq", 5672),
    "rabbitmq-ui": ("rabbitmq", 15672),
    "minio": ("minio", 9000),
    "minio-ui": ("minio", 9001),
}


# --- 포트 -------------------------------------------------------------------

def free(port: int) -> bool:
    """이 포트에 아무도 없으면 True.

    connect 로 본다. bind 로 보면 SO_REUSEADDR 때문에 이미 듣고 있는 포트도 비어
    보이는 경우가 있다.

    **127.0.0.1 만 보지 않는다.** Vite 는 IPv6 의 ::1 에만 붙는 경우가 있어, IPv4 로만
    두드리면 멀쩡히 떠 있는 서버를 못 찾는다. localhost 를 그대로 풀어 나오는 주소를
    전부 본다 — 브라우저와 프록시가 붙는 방식도 그것이다.
    """
    try:
        candidates = socket.getaddrinfo("localhost", port, type=socket.SOCK_STREAM)
    except socket.gaierror:
        return True

    for family, kind, proto, _, address in candidates:
        with socket.socket(family, kind, proto) as probe:
            probe.settimeout(0.2)
            if probe.connect_ex(address) == 0:
                return False
    return True


def pick(name: str, default: int, taken: set[int]) -> int:
    for candidate in range(default, default + 200):
        if candidate not in taken and free(candidate):
            if candidate != default:
                print(f"  {name}: {default} 이 차 있어 {candidate} 로 비켜 간다")
            return candidate
    raise SystemExit(f"{name}: {default} 부터 200개를 봤는데 빈 포트가 없다")


def published() -> dict:
    """우리 compose 프로젝트가 지금 내주고 있는 호스트 포트.

    이 값이 없으면 실행할 때마다 포트가 밀린다. **우리 컨테이너가 잡은 포트도 그냥
    보면 "차 있다"로 읽히기 때문**이다. 그러면 다음 실행이 옆 포트로 비켜 가고,
    컨테이너가 다시 만들어지고, 그 다음 실행이 또 비켜 간다.
    """
    found = {}
    for name, (service, inside) in INFRA.items():
        result = subprocess.run(
            [*COMPOSE, "port", service, str(inside)],
            cwd=ROOT, capture_output=True, text=True,
        )
        mapped = result.stdout.strip()
        if result.returncode == 0 and ":" in mapped:
            found[name] = int(mapped.rsplit(":", 1)[1])
    return found


def resolve_ports(previous: dict) -> dict:
    """이번 실행이 쓸 포트.

    **이미 떠 있는 우리 컨테이너의 포트를 그대로 쓴다.** 바꾸면 컨테이너가 다시
    만들어지고, 그때마다 앱이 붙을 주소가 달라진다.

    남은 것은 지난 실행이 쓰던 포트를 재사용하되, 비어 있을 때만 그렇게 한다 — 그
    사이 남이 차지했으면 비켜 간다.
    """
    print("포트 확인", flush=True)
    mine = published()
    ports, taken = {}, set()
    for name, (default, _) in PORTS.items():
        keep = mine.get(name) or previous.get(name)
        reusable = keep and keep not in taken and (keep == mine.get(name) or free(keep))
        chosen = keep if reusable else pick(name, default, taken)
        ports[name] = chosen
        taken.add(chosen)
    return ports


def alive(pid: int) -> bool:
    try:
        os.kill(pid, 0)
        return True
    except OSError:
        return False


# --- 실행 -------------------------------------------------------------------

def env_for(ports: dict) -> dict:
    env = dict(os.environ)
    env.update(
        DB_URL=f"jdbc:postgresql://localhost:{ports['postgres']}/codedrill",
        REDIS_URL=f"redis://localhost:{ports['redis']}",
        BROKER_URL=f"amqp://codedrill:codedrill@localhost:{ports['rabbitmq']}",
        CONTENT_ROOT=str(ROOT / "content" / "problems"),
        ADMIN_BOOTSTRAP_EMAIL=BOOTSTRAP_EMAIL,
    )
    return env


def remember(ports: dict, pids: dict) -> None:
    """무엇을 띄웠는지 그때그때 적는다.

    마지막에 한 번 적으면 중간에 실패했을 때 기록이 없고, 이미 뜬 앱은 아무도 내릴 수
    없는 고아가 된다. 실제로 한 번 그렇게 만들었다.
    """
    STATE.write_text(json.dumps({"ports": ports, "pids": pids}, indent=2) + "\n")


def spawn(name: str, command: list[str], env: dict, cwd: pathlib.Path = ROOT) -> int:
    LOGS.mkdir(parents=True, exist_ok=True)
    log = open(LOGS / f"{name}.log", "w")
    # 자기 세션으로 띄운다. 이 스크립트가 Ctrl-C 로 죽어도 앱이 함께 죽지 않아야,
    # --down 이 유일한 종료 경로가 된다.
    process = subprocess.Popen(
        command, cwd=cwd, env=env, stdout=log, stderr=subprocess.STDOUT,
        start_new_session=True,
    )
    return process.pid


def wait_http(url: str, name: str, timeout: int = 180) -> None:
    deadline = time.time() + timeout
    while time.time() < deadline:
        try:
            with urllib.request.urlopen(url, timeout=3):
                print(f"  {name} 기동")
                return
        except urllib.error.HTTPError:
            print(f"  {name} 기동")
            return
        except Exception:
            time.sleep(1)
    raise SystemExit(f"{name} 이(가) {timeout}초 안에 뜨지 않았다. {LOGS / (name + '.log')} 를 본다")


def wait_port(port: int, name: str, timeout: int = 180) -> None:
    deadline = time.time() + timeout
    while time.time() < deadline:
        if not free(port):
            print(f"  {name} 기동")
            return
        time.sleep(1)
    raise SystemExit(f"{name} 이(가) {timeout}초 안에 뜨지 않았다. {LOGS / (name + '.log')} 를 본다")


# --- 단계 -------------------------------------------------------------------

def check_tools() -> None:
    missing = [t for t in ("docker", "java", "pnpm") if shutil.which(t) is None]
    if missing:
        raise SystemExit(f"필요한 도구가 없다: {', '.join(missing)} (docs/running-locally.md)")


def start_infra(ports: dict, env: dict) -> None:
    print("플랫폼 의존성", flush=True)
    compose_env = dict(env)
    for name, (_, var) in PORTS.items():
        if var:
            compose_env[var] = str(ports[name])
    # --wait 는 healthcheck 가 통과할 때까지 기다린다. 기다리지 않으면 앱이 먼저 떠서
    # 커넥션 오류를 내고 죽는다.
    subprocess.run([*COMPOSE, "up", "-d", "--wait"], cwd=ROOT, env=compose_env, check=True)


def build() -> None:
    print("빌드", flush=True)
    subprocess.run(
        ["./gradlew", "--quiet", ":control-plane:app:bootJar", ":judge:orchestrator:bootJar",
         ":judge:runner-agent:installDist"],
        cwd=ROOT, check=True,
    )


def start_apps(ports: dict, env: dict, with_web: bool) -> dict:
    print("앱", flush=True)
    pids: dict = {}

    control = ROOT / "control-plane/app/build/libs/app-0.1.0-SNAPSHOT.jar"
    pids["control-plane"] = spawn(
        "control-plane", ["java", "-jar", str(control)],
        {**env, "PORT": str(ports["control-plane"])},
    )
    remember(ports, pids)
    wait_http(f"http://localhost:{ports['control-plane']}/api/v1/problems?limit=1", "control-plane")

    orchestrator = ROOT / "judge/orchestrator/build/libs/orchestrator-0.1.0-SNAPSHOT.jar"
    pids["orchestrator"] = spawn(
        "orchestrator", ["java", "-jar", str(orchestrator)],
        {**env, "PORT": str(ports["orchestrator"])},
    )
    remember(ports, pids)
    wait_port(ports["orchestrator"], "orchestrator")

    pids["runner-agent"] = spawn(
        "runner-agent", [str(ROOT / "judge/runner-agent/build/install/runner-agent/bin/runner-agent")],
        {**env, "PORT": str(ports["runner-agent"])},
    )
    remember(ports, pids)
    wait_port(ports["runner-agent"], "runner-agent")

    if with_web:
        pids["web"] = spawn(
            "web", ["pnpm", "dev", "--port", str(ports["web"]), "--strictPort"],
            {**env, "CONTROL_PLANE_URL": f"http://localhost:{ports['control-plane']}"},
            cwd=ROOT / "web",
        )
        remember(ports, pids)
        wait_port(ports["web"], "web")

    return pids


def seed(ports: dict, env: dict) -> None:
    print("문제 검증·공개 (몇 분 걸린다)", flush=True)
    subprocess.run(["./gradlew", ":judge:runner-agent:validateContent"], cwd=ROOT, env=env, check=True)
    subprocess.run(
        [sys.executable, "scripts/publish-content.py"], cwd=ROOT,
        env={**env, "CODEDRILL_BASE": f"http://localhost:{ports['control-plane']}"}, check=True,
    )


def release(ports: dict, timeout: int = 20) -> None:
    """방금 죽인 앱이 포트를 놓을 때까지 잠깐 기다린다.

    기다리지 않으면 아직 닫히지 않은 포트를 '차 있다'로 읽고 옆 포트로 비켜 간다.
    실행할 때마다 한 칸씩 밀리는 것이 그래서 생긴다.
    """
    watch = [ports[name] for name in APPS if name in ports]
    deadline = time.time() + timeout
    while time.time() < deadline and not all(free(p) for p in watch):
        time.sleep(0.5)


def stop(state: dict, everything: bool) -> None:
    for name, pid in (state.get("pids") or {}).items():
        if alive(pid):
            # 프로세스 그룹째 보낸다. pnpm 은 vite 를 자식으로 띄우므로 부모만
            # 죽이면 포트를 잡은 자식이 남는다.
            try:
                os.killpg(os.getpgid(pid), signal.SIGTERM)
            except OSError:
                os.kill(pid, signal.SIGTERM)
            print(f"  {name} 종료 (pid {pid})")
    # 포트를 실제로 놓을 때까지 기다린다. Spring 은 우아하게 내려가느라 잠깐 더 잡고
    # 있는데, 여기서 바로 돌아가면 --status 가 "떠 있음"으로 보이고 다음 --up 이
    # 옆 포트로 비켜 간다.
    release(state.get("ports") or {})

    if everything:
        print("플랫폼 의존성 정지")
        subprocess.run([*COMPOSE, "stop"], cwd=ROOT, check=False)
    state["pids"] = {}
    STATE.write_text(json.dumps(state, indent=2) + "\n")


def emit_env(state: dict) -> None:
    """고른 포트를 셸에 넘긴다.

    compose 를 손으로 부를 때 이게 없으면 기본 포트로 뜨려다 실패한다 — 비켜 간 이유가
    남이 그 포트를 쓰고 있기 때문이므로, 두 번째 시도도 같은 곳에서 막힌다.
    """
    ports = state.get("ports") or {}
    if not ports:
        print("# 띄운 기록이 없다. python3 scripts/up.py 로 시작한다.")
        return
    for name, (_, var) in PORTS.items():
        if var and name in ports:
            print(f"export {var}={ports[name]}")
    print(f"export CODEDRILL_BASE=http://localhost:{ports['control-plane']}")
    print(f"export ADMIN_BOOTSTRAP_EMAIL={BOOTSTRAP_EMAIL}")


def status(state: dict) -> None:
    ports = state.get("ports") or {}
    if not ports:
        print("띄운 기록이 없다. python3 scripts/up.py 로 시작한다.")
        return
    pids = state.get("pids") or {}
    for name in PORTS:
        mark = "떠 있음" if not free(ports[name]) else "없음"
        owner = f" pid {pids[name]}" if name in pids and alive(pids[name]) else ""
        print(f"  {name:<14} :{ports[name]:<6} {mark}{owner}")


def summary(ports: dict, with_web: bool) -> None:
    print()
    print("떴다.")
    if with_web:
        print(f"  웹            http://localhost:{ports['web']}")
    print(f"  제어 영역      http://localhost:{ports['control-plane']}/api/v1")
    print(f"  RabbitMQ UI   http://localhost:{ports['rabbitmq-ui']}  (codedrill/codedrill)")
    print(f"  MinIO UI      http://localhost:{ports['minio-ui']}     (codedrill/codedrill)")
    print(f"  로그           {LOGS}")
    print()
    print("관리자 API 를 부르려면 스크립트 터미널에서:")
    print(f"  export ADMIN_BOOTSTRAP_EMAIL={BOOTSTRAP_EMAIL}")
    print("  python3 scripts/smoke.py")
    print()
    print("내릴 때는 python3 scripts/up.py --down (컨테이너까지면 --down --all)")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--down", action="store_true", help="앱을 내린다")
    parser.add_argument("--all", action="store_true", help="--down 과 함께: 컨테이너도 정지")
    parser.add_argument("--status", action="store_true", help="지금 상태")
    parser.add_argument("--no-web", action="store_true", help="웹 개발 서버 없이")
    parser.add_argument("--seed", action="store_true", help="문제 검증·공개까지")
    parser.add_argument(
        "--env", action="store_true",
        help="고른 포트를 export 구문으로 찍는다 (eval \"$(python3 scripts/up.py --env)\")",
    )
    args = parser.parse_args()

    state = json.loads(STATE.read_text()) if STATE.exists() else {}

    if args.env:
        emit_env(state)
        return 0
    if args.status:
        status(state)
        return 0
    if args.down:
        stop(state, everything=args.all)
        return 0

    check_tools()
    # 지난 실행이 남아 있으면 먼저 정리한다. 두 번 띄우면 포트가 밀리고, 밀린 쪽이
    # 무엇을 보고 있는지 아무도 모르게 된다.
    stop(state, everything=False)

    ports = resolve_ports(state.get("ports") or {})
    env = env_for(ports)

    start_infra(ports, env)
    build()
    pids = start_apps(ports, env, with_web=not args.no_web)

    if args.seed:
        seed(ports, env)

    summary(ports, with_web=not args.no_web)
    return 0


if __name__ == "__main__":
    sys.exit(main())
