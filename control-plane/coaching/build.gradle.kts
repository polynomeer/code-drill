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

/**
 * 콘텐츠도 테스트의 입력이다 — `:judge:runner-agent` 와 같은 이유, 같은 한 줄이다.
 *
 * `HintCoverageTest` 는 `content/problems` 의 힌트와 카탈로그를 읽는데, Gradle 은 그것을
 * 모른다. 선언하지 않으면 문제를 더해도 테스트 작업이 UP-TO-DATE 로 건너뛰고 지난 결과가
 * 통과로 남는다 — 힌트가 선언하지 않은 역량에 붙은 문제를 로컬에서는 통과로 보고 CI 에서
 * 잡았다. `--no-build-cache` 로도 막지 못한다; 캐시가 아니라 최신성 판정이기 때문이다.
 */
tasks.named<Test>("test") {
    inputs.dir(rootProject.file("content")).withPathSensitivity(PathSensitivity.RELATIVE)
}
