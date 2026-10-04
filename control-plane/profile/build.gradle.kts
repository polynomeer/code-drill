plugins { alias(libs.plugins.kotlin.spring) }

dependencies {
    implementation(project(":platform:common"))
    implementation(libs.spring.boot.starter.web)
    testImplementation(kotlin("test"))
}
