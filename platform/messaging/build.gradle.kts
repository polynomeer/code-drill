plugins { alias(libs.plugins.kotlin.spring) }

dependencies {
    implementation(project(":platform:common"))
    implementation(libs.spring.boot.starter)
    implementation(libs.spring.boot.starter.amqp)
    implementation(libs.jackson.kotlin)
    implementation(libs.jackson.jsr310)
}
