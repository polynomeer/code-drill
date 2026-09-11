plugins { alias(libs.plugins.kotlin.spring) }

dependencies {
    implementation(project(":platform:common"))
    // 추천은 카탈로그(역량·선수 관계·난이도) 위에서 선다.
    implementation(project(":platform:problem-package"))
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.jdbc)
    implementation(libs.jackson.kotlin)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(kotlin("test"))
}
