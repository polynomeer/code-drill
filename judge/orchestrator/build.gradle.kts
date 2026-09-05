plugins {
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
}

dependencies {
    implementation(project(":judge:protocol"))
    implementation(project(":platform:common"))
    implementation(project(":platform:messaging"))
    implementation(project(":platform:observability"))
    implementation(libs.spring.boot.starter)
    implementation(libs.spring.boot.starter.amqp)
    implementation(libs.spring.boot.starter.actuator)
    // health·prometheus 엔드포인트를 노출하려면 web 이 필요하다 (§13.2 기본 대시보드).
    implementation(libs.spring.boot.starter.web)
}
