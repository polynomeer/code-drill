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
    implementation(libs.aws.s3)
}
