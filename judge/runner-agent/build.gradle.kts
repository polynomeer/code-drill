plugins {
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
}

dependencies {
    implementation(project(":judge:protocol"))
    implementation(libs.spring.boot.starter)
    implementation(libs.spring.boot.starter.actuator)
}
