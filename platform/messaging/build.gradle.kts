plugins { alias(libs.plugins.kotlin.spring) }

dependencies {
    implementation(project(":platform:common"))
    implementation(libs.spring.boot.starter)
    implementation(libs.spring.boot.starter.amqp)
    implementation(libs.jackson.kotlin)
    implementation(libs.jackson.jsr310)
}

// 배포용 브로커 정의를 코드에서 만든다. 손으로 고치지 않는다 (BrokerDefinitions).
tasks.register<JavaExec>("writeBrokerDefinitions") {
    group = "build"
    description = "deploy/broker/definitions.json 을 JudgeTopology 에서 다시 만든다"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("dev.codedrill.platform.messaging.BrokerDefinitionsKt")
    args(rootProject.file("deploy/broker/definitions.json").absolutePath)
}
