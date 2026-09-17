plugins { alias(libs.plugins.kotlin.spring) }

dependencies {
    implementation(project(":platform:common"))
    implementation(libs.spring.boot.starter)
    // S3 호환 API 로 붙는다. MinIO 든 S3 든 같은 클라이언트다 (§8.3).
    implementation(libs.aws.s3) {
        // s3 아티팩트는 기본 HTTP 클라이언트 둘(apache, netty)을 런타임에 끌고 온다. 우리는 아래
        // url-connection-client 를 명시적으로 쓰므로 둘 다 쓰이지 않는데, netty 는 이미지 스캔에서
        // 고칠 수 있는 CRITICAL 로 잡혔다 — 쓰지 않는 코드로 게이트를 막느니 싣지 않는다.
        exclude(group = "software.amazon.awssdk", module = "netty-nio-client")
        exclude(group = "software.amazon.awssdk", module = "apache-client")
    }
    // JDK 의 HttpURLConnection 위에서 돈다. StorageConfig 가 이 클라이언트를 명시한다.
    implementation(libs.aws.url.connection.client)
}
