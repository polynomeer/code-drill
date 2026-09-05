plugins {
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
}

dependencies {
    implementation(project(":judge:protocol"))
    implementation(project(":platform:common"))
    implementation(libs.spring.boot.starter)
    implementation(libs.spring.boot.starter.actuator)
    // 로컬 kotlinc 설치에 기대지 않고 in-process 로 컴파일한다.
    implementation(libs.kotlin.compiler.embeddable)
}
