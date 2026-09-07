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
    // §13.2 메트릭을 직접 기록한다. 노출은 조립 지점(:control-plane:app)이 맡는다.
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.amqp)
    implementation(libs.jackson.kotlin)
}
