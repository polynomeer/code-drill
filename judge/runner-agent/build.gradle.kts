plugins {
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
    application
}

dependencies {
    implementation(project(":judge:protocol"))
    implementation(project(":platform:common"))
    implementation(project(":platform:messaging"))
    implementation(project(":platform:observability"))
    implementation(libs.spring.boot.starter)
    implementation(libs.spring.boot.starter.actuator)
    // health·prometheus 엔드포인트를 노출하려면 web 이 필요하다 (§13.2 기본 대시보드).
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.amqp)
    // 로컬 kotlinc 설치에 기대지 않고 in-process 로 컴파일한다.
    implementation(libs.kotlin.compiler.embeddable)
}

application {
    mainClass = "dev.codedrill.judge.runner.RunnerAgentApplicationKt"
}

/*
 * Runner 는 fat jar 로 실행하지 않는다.
 *
 * kotlin-compiler-embeddable 은 자기 jar 안의 `extensions/compiler.xml` 을 클래스패스에서
 * 직접 찾는데, Spring Boot fat jar 의 중첩 jar 안에서는 찾지 못하고 컴파일이 통째로
 * 실패한다. 그래서 평범한 클래스패스 배포(`installDist`)로 내보낸다. 실행 영역을 고정
 * digest 의 런타임 이미지로 굽는 §5.5 방향과도 맞다.
 *
 *   ./gradlew :judge:runner-agent:installDist
 *   judge/runner-agent/build/install/runner-agent/bin/runner-agent
 */
/**
 * 콘텐츠 검증 (§6.3). 문제를 추가·수정한 사람이 돌리고 CI 도 같은 명령을 돌린다.
 * 저장소 루트를 기준으로 동작하도록 workingDir 를 맞춘다.
 */
tasks.register<JavaExec>("validateContent") {
    group = "verification"
    description = "content/problems 의 모든 문제를 §6.3 파이프라인으로 검증한다"
    mainClass = "dev.codedrill.judge.runner.content.ValidateContent"
    classpath = sourceSets["main"].runtimeClasspath
    workingDir = rootProject.projectDir
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") { enabled = false }
tasks.named<Jar>("jar") { enabled = true }
