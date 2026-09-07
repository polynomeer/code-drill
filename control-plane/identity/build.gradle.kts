plugins { alias(libs.plugins.kotlin.spring) }

dependencies {
    implementation(project(":platform:common"))
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.jdbc)
    // 비밀번호 해싱(BCrypt)만 쓴다. 인증 경로는 우리 인터셉터가 들고 있으므로
    // spring-boot-starter-security 의 필터 체인은 끌어오지 않는다.
    implementation(libs.spring.security.crypto)
}
