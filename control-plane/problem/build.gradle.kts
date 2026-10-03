plugins { alias(libs.plugins.kotlin.spring) }

dependencies {
    implementation(project(":platform:common"))
    implementation(project(":platform:problem-package"))
    implementation(libs.spring.boot.starter.web)
    testImplementation(kotlin("test"))
}

/** 목록 시험이 저장소의 실제 문제와 번호표를 읽는다. content 가 바뀌면 다시 돌아야 한다. */
tasks.named<Test>("test") {
    inputs.dir(rootProject.file("content")).withPathSensitivity(PathSensitivity.RELATIVE)
}
