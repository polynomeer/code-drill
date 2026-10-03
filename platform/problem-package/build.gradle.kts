plugins { alias(libs.plugins.kotlin.spring) }

dependencies {
    implementation(project(":platform:common"))
    implementation(libs.spring.boot.starter)
    api(libs.jackson.kotlin)
    implementation(libs.jackson.yaml)
}

/**
 * 콘텐츠도 테스트의 입력이다 (control-plane/coaching 과 같은 이유). `ProblemNumbersTest` 는
 * 저장소의 모든 문제에 번호가 있는지 보는데, Gradle 이 content 를 입력으로 모르면 문제를
 * 더해도 지난 통과 결과가 그대로 남는다.
 */
tasks.named<Test>("test") {
    inputs.dir(rootProject.file("content")).withPathSensitivity(PathSensitivity.RELATIVE)
}
