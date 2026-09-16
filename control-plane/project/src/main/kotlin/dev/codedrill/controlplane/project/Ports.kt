package dev.codedrill.controlplane.project

import dev.codedrill.judge.protocol.WorkspaceRef

/**
 * 공개된 콘텐츠 id. Admin 이 소유하는 상태이고, 프로젝트형 문제도 같은 표에 등록·공개된다 —
 * 프로젝트형 문제도 문제다 (§3.2). 조립 지점이 잇는다.
 */
fun interface PublishedProjects {
    fun ids(): Set<String>
}

/**
 * 워크스페이스가 실행 영역으로 가는 길 (§8.3, production-readiness B3).
 *
 * 제출의 파일들은 DB 에 남고(화면·반출의 원본), 스토어의 것은 실행용 복제다. 메시지에는
 * 참조와 digest 만 실린다. Submission 모듈의 SourceStore 와 같은 분담이다.
 */
interface WorkspaceStore {
    fun store(submissionId: String, files: Map<String, String>): WorkspaceRef
    fun delete(submissionId: String)
}
