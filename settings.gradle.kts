rootProject.name = "code-drill"

// 제어 영역(Control Plane)과 실행 영역(Judge Data Plane)은 별도 배포 단위다.
// 기술 설계서 §2.2 논리 컴포넌트 / §2.3 배포 토폴로지 참조.

include(
    // 모든 영역이 공유하는 기반
    ":platform:common",
    ":platform:messaging",
    ":platform:observability",
    ":platform:problem-package",

    // 제어 영역: 모듈형 모놀리스 (§3.1)
    ":control-plane:app",
    ":control-plane:identity",
    ":control-plane:problem",
    ":control-plane:workspace",
    ":control-plane:submission",
    ":control-plane:competency",
    ":control-plane:coaching",
    ":control-plane:learning",
    ":control-plane:admin",

    // 실행 영역: 독립 신뢰 경계 (§5.1)
    ":judge:protocol",
    ":judge:orchestrator",
    ":judge:runner-agent",
)
