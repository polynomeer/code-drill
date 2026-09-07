package dev.codedrill.judge.runner.execution.sandbox

import dev.codedrill.judge.protocol.Language
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

/**
 * 언어별 seccomp allowlist (기술 설계서 §5.2 시스템 호출, §11.4 Sandbox regression).
 *
 * **허용 목록이다.** 여기 없는 시스템 호출은 전부 막힌다. 금지 목록으로 만들면 새로
 * 위험해진 호출이 생길 때마다 우리가 먼저 알아야 하는데, 커널은 우리를 기다려 주지
 * 않는다.
 *
 * 프로파일이 소스에 있는 이유는 **왜 허용하는지**를 함께 두기 위해서다. JSON 파일만
 * 있으면 몇 달 뒤에 어느 줄을 지워도 되는지 아무도 모르고, 그래서 아무도 지우지 않아
 * 목록이 자라기만 한다. 실행 직전에 JSON 으로 내보내 컨테이너 런타임에 넘긴다.
 *
 * 막힌 호출은 죽이지 않고 `EPERM` 을 돌려준다 (`SCMP_ACT_ERRNO`). 목록이 조금 모자랄
 * 때 죽이면 멀쩡한 제출이 통째로 실패하는데, 그 실패는 사용자 코드 탓처럼 보인다.
 * 오탐의 대가를 사용자가 치르게 두지 않는다.
 *
 * 검증: [SeccompProfileTest] 가 금지 호출이 목록에 없는지 보고, [SandboxRegressionTest]
 * 가 컨테이너에서 실제로 막히는지 본다.
 */
object SeccompProfile {

    /**
     * 컨테이너 런타임이 사용자 코드를 띄우기 **전에** 쓰는 것.
     *
     * seccomp 필터는 컨테이너 init 프로세스에도 걸린다. 그래서 이것들을 막으면 사용자
     * 코드가 시작하기도 전에 컨테이너가 뜨지 못한다 — 처음에 `capset` 을 금지했다가
     * "unable to apply bounding set" 으로 전부 실패했다.
     *
     * 허용해도 안전한 이유는 **다른 통제가 이미 그 힘을 없앴기** 때문이다.
     *
     * - `capset`: `--cap-drop ALL` 로 bounding set 이 비어 있어 되찾을 capability 가 없다.
     * - `setpgid`/`getpgid`: 프로세스 그룹은 컨테이너 PID 네임스페이스 안에서만 의미가
     *   있고, `--pids-limit` 이 그 안의 프로세스 수를 이미 묶어 둔다.
     *
     * `no-new-privileges` 가 위 둘 모두의 마지막 빗장이다. 세 통제 중 하나라도 빠지면
     * 이 목록을 다시 검토해야 한다.
     */
    private val CONTAINER_INIT = listOf("capset", "setpgid", "getpgid")

    /**
     * 어떤 프로그램이든 필요한 것.
     *
     * 파일·메모리·시그널·스레드·시간. 이 목록에서 하나를 빼면 대부분의 런타임이 기동조차
     * 하지 못하므로, 여기서 줄일 여지는 거의 없다.
     */
    private val BASELINE = listOf(
        // 파일 입출력
        "read", "write", "readv", "writev", "pread64", "pwrite64", "preadv", "pwritev",
        "open", "openat", "openat2", "close", "close_range", "lseek", "dup", "dup2", "dup3",
        "pipe", "pipe2", "fcntl", "flock", "ftruncate", "fsync", "fdatasync", "sync_file_range",
        "readlink", "readlinkat", "getcwd", "chdir", "fchdir", "umask",
        "mkdir", "mkdirat", "rmdir", "unlink", "unlinkat", "rename", "renameat", "renameat2",
        "link", "linkat", "symlink", "symlinkat",
        "chmod", "fchmod", "fchmodat", "utimensat", "futimesat",
        "getdents", "getdents64",

        // 파일 메타데이터
        "stat", "fstat", "lstat", "newfstatat", "statx", "statfs", "fstatfs",
        "access", "faccessat", "faccessat2",

        // 다중화. JVM 과 CPython 모두 자기 이벤트 루프에서 쓴다.
        "poll", "ppoll", "select", "pselect6",
        "epoll_create", "epoll_create1", "epoll_ctl", "epoll_wait", "epoll_pwait", "epoll_pwait2",
        "eventfd", "eventfd2",

        // 메모리
        "mmap", "mmap2", "munmap", "mprotect", "mremap", "madvise", "brk", "mlock", "munlock",
        "memfd_create",

        // 시그널
        "rt_sigaction", "rt_sigprocmask", "rt_sigreturn", "rt_sigpending", "rt_sigsuspend",
        "rt_sigtimedwait", "rt_sigqueueinfo", "rt_tgsigqueueinfo", "sigaltstack", "signalfd4",
        "restart_syscall",

        // 프로세스·스레드. clone 은 플래그를 걸러서 따로 허용한다 (RESTRICTED 참조).
        "gettid", "getpid", "getppid", "getpgrp", "getsid", "set_tid_address",
        "set_robust_list", "get_robust_list", "exit", "exit_group", "wait4", "waitid",
        "execve", "execveat", "tgkill", "kill",

        // 동기화
        "futex", "futex_waitv", "membarrier", "rseq",

        // 스케줄링. 런타임이 코어 수를 보고 스레드 수를 정한다.
        "sched_yield", "sched_getaffinity", "sched_setaffinity", "sched_getparam",
        "sched_getscheduler", "sched_get_priority_max", "sched_get_priority_min",
        "getcpu", "getpriority", "setpriority",

        // 시간. TIME_LIMIT 판정의 근거이므로 막으면 안 된다.
        "clock_gettime", "clock_getres", "clock_nanosleep", "gettimeofday", "time", "nanosleep",
        "timer_create", "timer_settime", "timer_gettime", "timer_delete",
        "timerfd_create", "timerfd_settime", "timerfd_gettime",

        // 신원·한도 조회. 바꾸는 쪽(setuid 등)은 허용하지 않는다.
        "getuid", "geteuid", "getgid", "getegid", "getgroups", "getresuid", "getresgid",
        "getrlimit", "prlimit64", "getrusage", "sysinfo", "uname",

        // 기타
        "arch_prctl", "prctl", "ioctl", "getrandom", "sched_rr_get_interval",
    ) + CONTAINER_INIT

    /**
     * JVM 이 더 필요로 하는 것.
     *
     * 대부분 이미 baseline 에 있다. 여기 남은 것은 JVM 이 **자기 프로세스 안에서** 쓰는
     * 것들이며, 바깥에 영향을 주지 않는다.
     */
    private val JVM_EXTRA = listOf(
        // 힙 크기를 정하려고 자기 메모리 정책을 본다.
        "get_mempolicy",
        // Unsafe.park 구현.
        "clock_settime64", "clock_gettime64",
        // 스레드 스택 보호 페이지.
        "mlockall", "munlockall",
    )

    /** CPython 이 더 필요로 하는 것. */
    private val PYTHON_EXTRA = listOf(
        // 재귀 한도와 스택 크기.
        "setrlimit",
        // 임시 파일 정리.
        "truncate",
    )

    /**
     * 절대로 허용하지 않는 것 (§11.1 위협 모델).
     *
     * 목록 자체는 필터에 들어가지 않는다 — allowlist 라서 적지 않으면 이미 막힌다.
     * 여기 적어 두는 것은 **테스트가 검사하기 위해서**다. 누군가 baseline 에 무심코
     * 추가하면 [SeccompProfileTest] 가 잡는다.
     */
    val FORBIDDEN = setOf(
        // 샌드박스 탈출
        "unshare", "setns", "mount", "umount", "umount2", "pivot_root", "chroot",
        "ptrace", "process_vm_readv", "process_vm_writev", "kcmp",
        // 커널 조작
        "init_module", "finit_module", "delete_module", "kexec_load", "kexec_file_load",
        "bpf", "perf_event_open", "iopl", "ioperm",
        // 권한 상승. capset 은 예외이며 이유는 CONTAINER_INIT 에 적었다.
        "setuid", "setgid", "setreuid", "setregid", "setresuid", "setresgid", "setfsuid",
        "setfsgid", "setgroups",
        // 비밀 취급
        "keyctl", "add_key", "request_key",
        // 네트워크. `--network none` 이 이미 막지만, 소켓을 만드는 것부터 막아
        // 격리가 한 겹 뚫려도 통신 경로가 서지 않게 한다.
        "socket", "socketpair", "connect", "bind", "listen", "accept", "accept4",
        "sendto", "recvfrom", "sendmsg", "recvmsg", "getsockopt", "setsockopt",
        // 시스템 상태 변경
        "reboot", "swapon", "swapoff", "settimeofday", "clock_settime", "sethostname",
        "setdomainname", "acct", "quotactl",
    )

    /**
     * 인자를 걸러 허용하는 것.
     *
     * `clone` 은 스레드를 만드는 데 필요하지만, 같은 호출로 새 네임스페이스도 만든다.
     * 그래서 네임스페이스 플래그가 하나도 켜져 있지 않을 때만 허용한다.
     */
    private const val NAMESPACE_FLAGS = 0x7E020000L

    fun forLanguage(language: Language): List<String> = (
        BASELINE + when (language) {
            Language.KOTLIN, Language.JAVA -> JVM_EXTRA
            Language.PYTHON -> PYTHON_EXTRA
        }
        ).distinct().sorted()

    /**
     * 컨테이너 런타임이 읽을 JSON.
     *
     * 손으로 다듬지 않는다. 문자열을 조립하는 것이 마음에 걸리지만, JSON 라이브러리를
     * 끌어오는 것보다 이 파일 하나가 자기 완결적인 편이 낫다 — 이 프로파일은 실행
     * 영역의 가장 바깥 방어선이라 의존성이 적을수록 좋다.
     */
    fun toJson(language: Language): String {
        val allowed = forLanguage(language).joinToString(",\n") { "        \"$it\"" }
        return """
            {
              "defaultAction": "SCMP_ACT_ERRNO",
              "defaultErrnoRet": 1,
              "architectures": [
                "SCMP_ARCH_X86_64", "SCMP_ARCH_X86", "SCMP_ARCH_X32",
                "SCMP_ARCH_AARCH64", "SCMP_ARCH_ARM"
              ],
              "syscalls": [
                {
                  "names": [
            $allowed
                  ],
                  "action": "SCMP_ACT_ALLOW"
                },
                {
                  "names": ["clone"],
                  "action": "SCMP_ACT_ALLOW",
                  "args": [
                    {
                      "index": 0,
                      "value": $NAMESPACE_FLAGS,
                      "valueTwo": 0,
                      "op": "SCMP_CMP_MASKED_EQ"
                    }
                  ]
                },
                {
                  "names": ["clone3"],
                  "action": "SCMP_ACT_ERRNO",
                  "errnoRet": 38
                }
              ]
            }
        """.trimIndent()
    }

    /**
     * 프로파일을 파일로 내보내고 경로를 돌려준다.
     *
     * 컨테이너 런타임은 파일 경로만 받는다. 기동할 때 한 번 쓰고, 그 뒤로는 같은 파일을
     * 계속 쓴다.
     */
    fun materialize(directory: Path): Map<Language, Path> {
        directory.createDirectories()
        return Language.entries.associateWith { language ->
            val path = directory.resolve("${language.name.lowercase()}.json")
            path.writeText(toJson(language))
            path
        }
    }

    /**
     * 프로파일 digest.
     *
     * §5.5 의 runtime_manifest 는 이미지 digest 와 seccomp 프로파일을 함께 기록한다.
     * 과거 제출을 재현하려면 "어떤 규칙으로 돌렸는가"까지 같아야 한다.
     */
    fun digest(language: Language): String = MessageDigest.getInstance("SHA-256")
        .digest(toJson(language).toByteArray())
        .take(6)
        .joinToString("") { "%02x".format(it) }
}
