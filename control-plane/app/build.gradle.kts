plugins {
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
}

/**
 * 조립 지점. 도메인 모듈은 서로를 참조하지 않고 여기에서만 함께 묶인다 (§3.1).
 */
dependencies {
    implementation(project(":platform:common"))
    implementation(project(":platform:messaging"))
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
    implementation(project(":control-plane:admin"))

    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.jdbc)
    implementation(libs.spring.boot.starter.amqp)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.jackson.kotlin)
    implementation(libs.flyway.core)
    runtimeOnly(libs.flyway.postgresql)
    runtimeOnly(libs.postgresql)

    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.rabbitmq)
}
