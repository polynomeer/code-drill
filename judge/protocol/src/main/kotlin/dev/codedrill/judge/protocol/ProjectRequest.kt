package dev.codedrill.judge.protocol

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import dev.codedrill.platform.problempackage.ProjectLimits
import java.security.MessageDigest
import java.time.Instant

/**
 * 프로젝트형 문제의 봉투 (feature-roadmap 11단계 — 두 번째 판정기).
 *
 * [ExecutionRequest] 를 늘리지 않고 **하나 더 둔다.** 저기에 파일 목록 칸을 붙이면 Runner
 * 의 모든 어댑터가 "파일이 하나인가 여럿인가"를 분기해야 하고, 그 분기는 첫 판정기의
 * 격리·측정을 건드린다. 격리·임대·fencing 은 다시 쓰되 요청과 채점 규칙은 따로다.
 *
 * 파일은 어디에도 전문으로 실리지 않는다. 사용자의 워크스페이스는 제어 영역이 스토어에
 * 올린 [WorkspaceRef] 로, 숨은 테스트는 오케스트레이터가 올린 [BundleRef] 로 온다. Runner
 * 는 둘을 받아 digest 를 대조하고, 워크스페이스 **위에** 숨은 테스트를 덮는다 — 같은 경로가
 * 있으면 숨은 것이 이긴다. 사용자가 테스트 파일을 갈아 끼울 수 없어야 하기 때문이다.
 */
data class ProjectRequest(
    val schemaVersion: String = SCHEMA_VERSION,
    val executionId: String,
    val submissionId: String,
    val attempt: Int,
    val fencingToken: FencingToken,
    val correlationId: String,
    val projectVersionId: String,
    val packageDigest: String,
    val language: Language,
    /** 사용자의 파일. 제어 영역이 올렸다. */
    val workspace: WorkspaceRef,
    /** 숨은 테스트. 오케스트레이터가 올렸다. 키는 패키지 digest 다. */
    val suite: BundleRef,
    val limits: ProjectLimits,
    /**
     * 사용자의 테스트를 시험할 판들 — 참조 구현과 대표 오답 (12단계 실무군 셋째 역량, [Probes]).
     * 오케스트레이터가 올렸다. 없으면 시험하지 않는다. 있어도 Runner 는 사용자가 시작 저장소보다
     * 테스트를 더 썼을 때만 돌린다 — 안 쓴 테스트는 잴 것이 없다.
     */
    val probe: BundleRef? = null,
) {
    companion object {
        const val SCHEMA_VERSION = "1.0"
    }
}

/**
 * "사용자의 테스트가 오답을 잡는가"를 재는 판 (실무군 셋째 역량 — 결함 검출).
 *
 * 숨은 스위트가 사용자의 **구현**을 시험한다면, 이것은 사용자의 **테스트**를 시험한다. 참조 구현
 * 위에서는 통과해야 하고(틀린 것을 맞다고 하는 테스트는 테스트가 아니다), 대표 오답 위에서는
 * 하나라도 떨어져야 그 오답을 잡은 것이다. 오답의 내용은 사용자에게 절대 나가지 않는다 — Runner
 * 가 돌리고 이름과 결과만 돌아온다.
 *
 * tamper 오답은 판에 넣지 않는다. 그것은 테스트가 아니라 가드가 잡는 것이다.
 */
object Probes {

    const val REFERENCE = "reference"

    private val json = ObjectMapper().registerKotlinModule()

    fun probeKey(packageDigest: String) = "probes/$packageDigest.json"

    fun encode(bundle: ProbeBundle): ByteArray = json.writeValueAsBytes(
        bundle.copy(starterTests = bundle.starterTests.toSortedMap(), variants = bundle.variants.toSortedMap().mapValues { it.value.toSortedMap() }),
    )

    fun decode(bytes: ByteArray): ProbeBundle = json.readValue(bytes)
}

/**
 * 시험판 하나의 묶음. [variants] 의 키는 [Probes.REFERENCE] 또는 오답 이름이고 값은 **완성된**
 * 워크스페이스(시작 저장소 위에 그 판을 덮은 것)다. [starterTests] 는 시작 저장소의 테스트 파일 —
 * 사용자가 그보다 더 썼는지를 Runner 가 가린다.
 */
data class ProbeBundle(
    val starterTests: Map<String, String>,
    val variants: Map<String, Map<String, String>>,
)

/** 시험판의 결과. 오답의 내용은 없고 이름과 잡았는지만 있다. */
data class ProjectProbeOutcome(
    /** 사용자의 테스트가 참조 구현 위에서 전부 통과했는가. 아니면 아래 둘은 뜻이 없다. */
    val referencePassed: Boolean,
    /** 사용자의 테스트가 떨어뜨린 오답. */
    val killed: List<String>,
    /** 사용자의 테스트가 통과시킨 오답. */
    val survived: List<String>,
    /** 참조 위에서 떨어졌을 때 그 사유 — 사용자의 테스트가 무엇을 잘못 기대했는지. */
    val log: String? = null,
)

/** 오브젝트 스토어의 워크스페이스 하나. [Workspaces] 가 모양을 정한다. */
data class WorkspaceRef(val key: String, val digest: String)

/**
 * 워크스페이스·스위트의 저장 모양. 경로 → 내용을 JSON 으로 적은 것이다.
 *
 * tar 가 아니다. 프로젝트형의 제출은 텍스트 소스 파일 수십 개이고, 그것을 셀 수 있는
 * 한계([MAX_FILES], [MAX_TOTAL_BYTES]) 안에서 받는다. 이진 파일과 권한 비트를 실을 이유가
 * 없고, 없으면 받는 쪽이 검사할 것도 적다.
 *
 * 경로는 [normalize] 를 지난 것만 담는다. `..` 과 절대 경로는 스토어에 올라가기 전에
 * 거절되고, Runner 도 풀 때 한 번 더 본다 — 스토어를 손댄 사람이 샌드박스 밖에 파일을
 * 쓰게 두면 안 된다.
 */
object Workspaces {

    const val CONTENT_TYPE = "application/json"
    const val MAX_FILES = 200
    const val MAX_FILE_BYTES = 256 * 1024
    const val MAX_TOTAL_BYTES = 2 * 1024 * 1024
    const val MAX_PATH_LENGTH = 200

    private val json = ObjectMapper().registerKotlinModule()

    fun workspaceKey(submissionId: String) = "workspaces/$submissionId.json"
    fun suiteKey(packageDigest: String) = "suites/$packageDigest.json"

    /** 경로를 정렬해 적는다. 같은 파일 묶음은 같은 바이트, 같은 digest 여야 한다. */
    fun encode(files: Map<String, String>): ByteArray =
        json.writeValueAsBytes(Archive(files.toSortedMap()))

    fun decode(bytes: ByteArray): Map<String, String> = json.readValue<Archive>(bytes).files

    fun digest(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    fun workspaceRef(submissionId: String, files: Map<String, String>): WorkspaceRef {
        val bytes = encode(files)
        return WorkspaceRef(workspaceKey(submissionId), digest(bytes))
    }

    /**
     * 경로를 받을 수 있는 모양으로 다듬는다. 안 되면 예외다.
     *
     * 루트 상대, `/` 구분, 빈 조각·`.`·`..` 없음, 숨은 파일 없음. 제어 영역이 제출을 받을
     * 때와 Runner 가 풀 때 같은 함수를 쓴다.
     */
    fun normalize(path: String): String {
        val trimmed = path.trim().replace('\\', '/')
        require(trimmed.isNotEmpty() && trimmed.length <= MAX_PATH_LENGTH) { "경로가 비었거나 너무 길다: '$path'" }
        require(!trimmed.startsWith("/")) { "절대 경로는 받지 않는다: $path" }
        val parts = trimmed.split('/')
        for (part in parts) {
            require(part.isNotEmpty()) { "빈 경로 조각이 있다: $path" }
            require(part != "." && part != "..") { "상위·현재 디렉터리 참조는 받지 않는다: $path" }
            require(!part.startsWith(".")) { "숨은 파일은 받지 않는다: $path" }
            require(part.all { it.isLetterOrDigit() || it in "-_." }) { "경로에 허용되지 않는 문자가 있다: $path" }
        }
        return parts.joinToString("/")
    }

    /** 파일 묶음 전체를 검사하고 경로를 다듬어 돌려준다. 한계를 넘거나 경로가 나쁘면 예외다. */
    fun validate(files: Map<String, String>): Map<String, String> {
        require(files.isNotEmpty()) { "파일이 하나도 없다" }
        require(files.size <= MAX_FILES) { "파일이 너무 많다: ${files.size} > $MAX_FILES" }
        var total = 0
        val normalized = linkedMapOf<String, String>()
        for ((path, content) in files) {
            val clean = normalize(path)
            require(normalized.put(clean, content) == null) { "같은 경로가 둘이다: $clean" }
            val bytes = content.toByteArray(Charsets.UTF_8).size
            require(bytes <= MAX_FILE_BYTES) { "$clean: 파일이 너무 크다 ($bytes B > $MAX_FILE_BYTES B)" }
            total += bytes
        }
        require(total <= MAX_TOTAL_BYTES) { "전체가 너무 크다: $total B > $MAX_TOTAL_BYTES B" }
        return normalized
    }

    private data class Archive(val files: Map<String, String>)
}

/**
 * 프로젝트형 결과 봉투. [ExecutionResult] 의 짝이다.
 *
 * 테스트 하나가 곧 케이스 하나다. 어느 것이 숨은 것인지는 Runner 가 모른다 — 모듈 이름을
 * 실어 보내고, 패키지를 아는 오케스트레이터가 숨은 모듈의 내역을 잘라낸다 (§8.3).
 */
data class ProjectResult(
    val schemaVersion: String = ProjectRequest.SCHEMA_VERSION,
    override val executionId: String,
    override val submissionId: String,
    override val attempt: Int,
    override val fencingToken: FencingToken,
    val projectVersionId: String,
    val verdict: Verdict,
    /**
     * 사람이 읽을 로그. 빌드가 실패했을 때는 빌드 출력, 테스트 단계가 리포트 없이 끝났을
     * 때는 그 출력의 끝, 테스트 기반이 손댄 것으로 판정됐을 때는 그 사유다.
     */
    val log: String?,
    val tests: List<ProjectTestOutcome>,
    val buildMillis: Long,
    val testMillis: Long,
    override val resultDigest: String,
    /** 사용자의 테스트를 시험한 결과. 시험할 판이 없거나 더 쓴 테스트가 없으면 null. */
    val probe: ProjectProbeOutcome? = null,
) : LeasedResult

/** 테스트 하나의 결과. [module] 로 공개·숨은 것을 가른다. */
data class ProjectTestOutcome(
    val module: String,
    val name: String,
    val passed: Boolean,
    /** 실패 사유. 숨은 테스트의 것은 오케스트레이터가 지운다 — 기대값이 그대로 들어 있다. */
    val message: String? = null,
)

/**
 * 제어 영역 → 오케스트레이터: 프로젝트형 제출이 큐에 올랐다. [SubmissionQueued] 의 짝이다.
 */
data class ProjectQueued(
    val schemaVersion: String = ProjectRequest.SCHEMA_VERSION,
    override val submissionId: String,
    override val correlationId: String,
    val projectId: String,
    val projectVersion: Int,
    val language: Language,
    val workspace: WorkspaceRef,
    val queuedAt: Instant? = null,
) : JudgeOrigin

/**
 * 오케스트레이터 → 제어 영역: 프로젝트형 채점이 끝났다. 멱등 키는 `executionId` 다.
 *
 * [tests] 에는 **공개 테스트만** 있다. 숨은 것은 [hiddenPassed]/[hiddenTotal] 두 수로만 나간다 —
 * 이름도 사유도 없다. 이름은 무엇을 시험하는지의 힌트고, 사유는 기대값 그 자체다.
 */
data class ProjectCompleted(
    val schemaVersion: String = ProjectRequest.SCHEMA_VERSION,
    val submissionId: String,
    val executionId: String,
    val correlationId: String,
    val verdict: Verdict,
    val score: Int,
    val log: String?,
    val tests: List<ProjectTestOutcome>,
    val hiddenPassed: Int,
    val hiddenTotal: Int,
    /** 사용자의 테스트가 오답을 잡았는가 (실무군 셋째 역량). 시험하지 않았으면 null. */
    val probe: ProjectProbeOutcome? = null,
)
