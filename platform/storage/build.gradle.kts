plugins { alias(libs.plugins.kotlin.spring) }

dependencies {
    implementation(project(":platform:common"))
    implementation(libs.spring.boot.starter)
    // S3 호환 API 로 붙는다. MinIO 든 S3 든 같은 클라이언트다 (§8.3).
    implementation(libs.aws.s3)
    // JDK 의 HttpURLConnection 위에서 돈다. 기본 클라이언트(apache, netty)를 끌어오지 않는다.
    implementation(libs.aws.url.connection.client)
}
