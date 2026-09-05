코칭 Workspace: Monaco 에디터, 자동 저장(CAS), 샘플 실행, 커스텀 테스트.

초안 저장은 `PUT /workspaces/{problem}/{language}` 의 낙관적 잠금을 쓴다.
충돌 시 DRAFT_VERSION_CONFLICT 를 받고 사용자에게 선택을 묻는다 (§9.4).
