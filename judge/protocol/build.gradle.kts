plugins { alias(libs.plugins.kotlin.spring) }

dependencies {
    api(project(":platform:problem-package"))
    implementation(project(":platform:common"))
    implementation(libs.spring.boot.starter)
}
