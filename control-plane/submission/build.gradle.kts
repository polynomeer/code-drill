plugins { alias(libs.plugins.kotlin.spring) }

dependencies {
    implementation(project(":platform:common"))
    implementation(project(":platform:messaging"))
    implementation(project(":platform:observability"))
    // 판정 결과 계약. 실행 영역의 내부 모델이 아니라 공개 메시지 타입만 쓴다 (§3.1).
    implementation(project(":judge:protocol"))
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.jdbc)
    implementation(libs.spring.boot.starter.amqp)
    implementation(libs.jackson.kotlin)
}
