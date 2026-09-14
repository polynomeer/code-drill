package dev.codedrill.platform.storage

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import java.net.URI
import java.nio.file.Path

/**
 * 스토어 조립.
 *
 * `endpoint` 가 있으면 S3 호환 스토어다. 없으면 디렉터리로 내려가되 **경고를 남긴다** —
 * 조용히 내려가면 노드가 둘이 되는 날 Runner 가 번들을 못 찾는데, 그 실패는 "채점이
 * 시스템 오류로 끝난다"로만 보인다.
 */
@AutoConfiguration
@EnableConfigurationProperties(StorageProperties::class)
class StorageConfig {

    @Bean
    fun blobStore(properties: StorageProperties): BlobStore {
        if (properties.endpoint.isBlank()) {
            val directory = Path.of(properties.directory.ifBlank { System.getProperty("java.io.tmpdir") + "/codedrill-blobs" })
            LoggerFactory.getLogger(javaClass).warn(
                "오브젝트 스토어 없이 디렉터리에 둔다: {}. 같은 머신에서만 통한다 — 공개 환경에서는 codedrill.storage.endpoint 를 준다",
                directory,
            )
            return DirectoryBlobStore(directory)
        }
        val client = S3Client.builder()
            .endpointOverride(URI(properties.endpoint))
            .region(Region.of(properties.region))
            .credentialsProvider(
                StaticCredentialsProvider.create(AwsBasicCredentials.create(properties.accessKey, properties.secretKey)),
            )
            // MinIO 는 버킷을 경로로 받는다. 가상 호스트 방식은 DNS 가 필요하다.
            .forcePathStyle(true)
            .httpClientBuilder(UrlConnectionHttpClient.builder())
            .build()
        return S3BlobStore(client, properties.bucket)
    }
}

@ConfigurationProperties(prefix = "codedrill.storage")
data class StorageProperties(
    /** S3 호환 엔드포인트. 비우면 디렉터리 스토어다. */
    val endpoint: String = "",
    val bucket: String = "codedrill",
    val accessKey: String = "",
    val secretKey: String = "",
    /** MinIO 는 아무 값이나 받지만 클라이언트는 하나를 요구한다. */
    val region: String = "us-east-1",
    /** 엔드포인트가 없을 때 쓰는 디렉터리. 비우면 임시 디렉터리 아래다. */
    val directory: String = "",
)
