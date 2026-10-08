plugins {
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
}

/**
 * 조립 지점. 도메인 모듈은 서로를 참조하지 않고 여기에서만 함께 묶인다 (§3.1).
 */
dependencies {
    // HTTP 요청·응답은 Boot 4 의 Jackson 3 이 읽고 쓴다 — Kotlin 데이터 클래스를 위해 그 Kotlin 모듈
    runtimeOnly(libs.jackson3.kotlin)
    implementation(project(":platform:common"))
    implementation(project(":platform:messaging"))
    implementation(project(":platform:storage"))
    implementation(project(":platform:observability"))
    implementation(project(":platform:problem-package"))
    implementation(project(":judge:protocol"))

    implementation(project(":control-plane:identity"))
    implementation(project(":control-plane:problem"))
    implementation(project(":control-plane:workspace"))
    implementation(project(":control-plane:submission"))
    implementation(project(":control-plane:competency"))
    implementation(project(":control-plane:coaching"))
    implementation(project(":control-plane:learning"))
    implementation(project(":control-plane:integrity"))
    implementation(project(":control-plane:contest"))
    implementation(project(":control-plane:project"))
    implementation(project(":control-plane:admin"))
    implementation(project(":control-plane:profile"))
    implementation(project(":control-plane:notification"))
    implementation(project(":control-plane:analytics"))

    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.jdbc)
    implementation(libs.spring.boot.starter.amqp)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.jackson.kotlin)
    // Boot 3 의 ObjectMapper 가 끌고 오던 모듈 — 이제 우리가 만드는 Jackson 2 매퍼(ControlPlaneConfig)가 찾아 쓴다
    runtimeOnly(libs.jackson.jsr310)
    runtimeOnly(libs.jackson.jdk8)
    implementation(libs.spring.boot.starter.flyway)
    runtimeOnly(libs.flyway.postgresql)
    runtimeOnly(libs.postgresql)
}
