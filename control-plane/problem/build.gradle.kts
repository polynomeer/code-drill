plugins { alias(libs.plugins.kotlin.spring) }

dependencies {
    implementation(project(":platform:common"))
    implementation(project(":platform:problem-package"))
    implementation(libs.spring.boot.starter.web)
}
