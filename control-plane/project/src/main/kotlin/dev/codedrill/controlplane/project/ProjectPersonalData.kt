package dev.codedrill.controlplane.project

import org.springframework.stereotype.Component

/**
 * 프로젝트형 제출의 개인 데이터 (§11.3). 파일은 사용자가 쓴 것이라 반출하고 지운다; 제출
 * 행은 통계의 것이라 남긴다. 스토어의 실행용 복제도 함께 지운다 — DB 를 먼저 비우면 어느
 * 것을 지워야 하는지 다시 알 길이 없다.
 */
@Component
class ProjectPersonalData(private val repository: ProjectRepository, private val workspaces: WorkspaceStore) {

    fun export(userId: String): Map<String, Any> =
        mapOf("submissions" to repository.export(userId), "drafts" to repository.exportDrafts(userId))

    fun erase(userId: String): Map<String, Int> {
        var stored = 0
        for (id in repository.submissionIds(userId)) runCatching { workspaces.delete(id) }.onSuccess { stored += 1 }
        val files = repository.eraseContent(userId)
        val drafts = repository.eraseDrafts(userId)
        return mapOf("files" to files, "storedWorkspaces" to stored, "drafts" to drafts)
    }
}
