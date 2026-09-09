plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.spring) apply false
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "io.spring.dependency-management")

    group = "dev.codedrill"
    version = "0.1.0-SNAPSHOT"

    repositories { mavenCentral() }

    // Boot 가 고정한 Tomcat 10.1.39 에 고칠 수 있는 CRITICAL 이 여섯 있다 (§11.4 이미지
    // 스캔이 잡았다). Boot 판을 통째로 올리는 것보다 이 한 줄이 좁다 — 고쳐야 할 것은
    // 서블릿 컨테이너 하나이고, 나머지를 함께 움직이면 스캔이 잡은 것과 무관한 실패가
    // 섞인다. Boot 를 올릴 때는 이 값이 오히려 낮지 않은지 본다.
    //
    // BOM 의 프로퍼티를 덮어쓰는 방식이다. 셋(core·el·websocket)이 이 한 값을 함께 쓴다.
    extra["tomcat.version"] = rootProject.libs.versions.tomcat.get()

    the<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension>().apply {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:${rootProject.libs.versions.springBoot.get()}")
        }
    }

    extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
        jvmToolchain(21)
        compilerOptions {
            freeCompilerArgs.add("-Xjsr305=strict")
            allWarningsAsErrors.set(true)
        }
    }

    dependencies {
        "implementation"(rootProject.libs.kotlin.reflect)
        "testImplementation"(rootProject.libs.kotlin.test.junit5)
        "testImplementation"(rootProject.libs.spring.boot.starter.test)
    }

    tasks.withType<Test>().configureEach { useJUnitPlatform() }
}

/**
 * 제어 영역 모듈 경계 검사 (기술 설계서 §3.1).
 *
 * 각 도메인 모듈은 자기 데이터만 소유하고, 다른 도메인 모듈을 직접 참조하지 않는다.
 * 모듈 간 협력은 조립 지점인 :control-plane:app 에서 연결한다.
 */
val checkModuleBoundaries by tasks.registering {
    group = "verification"
    description = "제어 영역 도메인 모듈이 서로를 직접 참조하지 않는지 검사한다"

    val violations = provider {
        subprojects
            .filter { it.path.startsWith(":control-plane:") && it.path != ":control-plane:app" }
            .flatMap { module ->
                module.configurations
                    .filter { it.name in setOf("implementation", "api", "compileOnly", "runtimeOnly") }
                    .flatMap { it.dependencies }
                    .filterIsInstance<ProjectDependency>()
                    .map { it.path }
                    .filter { it.startsWith(":control-plane:") }
                    .map { "${module.path} → $it" }
            }
    }

    doLast {
        val found = violations.get()
        if (found.isNotEmpty()) {
            throw GradleException(
                "제어 영역 모듈 경계 위반 (docs/project-context.md 모듈 경계 참조):\n" +
                    found.joinToString("\n") { "  $it" }
            )
        }
    }
}

tasks.register("check") { dependsOn(checkModuleBoundaries) }
