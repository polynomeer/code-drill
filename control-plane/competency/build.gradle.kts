plugins { alias(libs.plugins.kotlin.spring) }

dependencies {
    implementation(project(":platform:common"))
    // 역량 온톨로지와 문제의 역량 태그. 어느 증거가 어느 역량에 속하는지는 카탈로그가 안다.
    implementation(project(":platform:problem-package"))
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.jdbc)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(kotlin("test"))
}
