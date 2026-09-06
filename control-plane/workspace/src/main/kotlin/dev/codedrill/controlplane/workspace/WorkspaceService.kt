package dev.codedrill.controlplane.workspace

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 초안 자동 저장 (기술 설계서 §9.2 `PUT /workspaces/{problem}/{language}`).
 *
 * 저장 결과는 셋 중 하나다: 새로 만들었거나, 기대 버전과 맞아 덮어썼거나, 그 사이에
 * 누군가 저장해 충돌했거나. 충돌을 조용히 이기지 않는 것이 이 서비스의 존재 이유다 —
 * 두 탭에서 같은 문제를 열어 둔 사용자가 작성 중인 코드를 잃으면 안 된다.
 */
@Service
class WorkspaceService(private val repository: WorkspaceRepository) {

    fun find(userId: String, problemId: String, language: String): WorkspaceDraft? =
        repository.find(userId, problemId, language)

    fun recent(userId: String, limit: Int = 10): List<WorkspaceDraft> =
        repository.recent(userId, limit)

    /**
     * [expectedVersion] 이 null 이면 "아직 초안이 없다"는 주장이다. 그 주장이 틀렸다면
     * (이미 초안이 있다면) 새로 만들지 않고 충돌로 알린다.
     */
    @Transactional
    fun save(
        userId: String,
        problemId: String,
        language: String,
        code: String,
        expectedVersion: Long?,
    ): SaveOutcome {
        if (expectedVersion == null) {
            if (repository.insert(userId, problemId, language, code) > 0) {
                return SaveOutcome.Saved(version = 1)
            }
            return conflict(userId, problemId, language)
        }

        if (repository.compareAndSet(userId, problemId, language, code, expectedVersion) > 0) {
            return SaveOutcome.Saved(version = expectedVersion + 1)
        }
        return conflict(userId, problemId, language)
    }

    /**
     * 충돌 응답에는 **현재 서버 상태를 함께 실어 보낸다.**
     *
     * 클라이언트가 다시 조회하러 오게 만들면, 그 사이에 또 바뀔 수 있고 왕복도 는다.
     * 무엇과 충돌했는지 그 자리에서 보여줄 수 있어야 사용자가 고를 수 있다.
     */
    private fun conflict(userId: String, problemId: String, language: String): SaveOutcome {
        val current = repository.find(userId, problemId, language)
            ?: error("충돌인데 현재 초안을 찾지 못했다")
        return SaveOutcome.Conflict(current)
    }

    sealed interface SaveOutcome {
        data class Saved(val version: Long) : SaveOutcome
        data class Conflict(val current: WorkspaceDraft) : SaveOutcome
    }
}
