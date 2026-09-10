plugins { alias(libs.plugins.kotlin.spring) }

dependencies {
    implementation(project(":platform:common"))
    implementation(project(":platform:messaging"))
    // 실행 요청 계약. 실행 영역의 내부 모델이 아니라 공개 메시지 타입만 쓴다 (§3.1).
    implementation(project(":judge:protocol"))
    // 시험 실행에도 문제의 시그니처와 제한이 필요하다. 판정과 다른 제한으로 돌리면
    // 사용자가 본 결과와 채점 결과가 갈린다.
    implementation(project(":platform:problem-package"))
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.jdbc)
    implementation(libs.spring.boot.starter.amqp)
    implementation(libs.jackson.kotlin)
}
