plugins { alias(libs.plugins.kotlin.spring) }

dependencies {
    implementation(project(":platform:common"))
    // 힌트는 문제가 이미 갖고 있는 것에서 나온다 — 카탈로그의 복잡도·기법, 대표 오답이
    // 무엇을 잘못하는지 적은 줄. 코칭이 그것을 다시 쓰지 않는다.
    implementation(project(":platform:problem-package"))
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.jdbc)
    implementation(libs.jackson.kotlin)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(kotlin("test"))
}
