package dev.codedrill.platform.storage

import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.NoSuchBucketException
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.S3Exception

/**
 * S3 호환 스토어. MinIO 와 S3 에 같은 코드로 붙는다.
 *
 * 버킷은 만들지 않는다. 배포에서 버킷과 그 권한은 스토어 쪽 설정이고(deploy/), 여기서
 * 만들 수 있다는 것은 곧 이 자격증명이 너무 넓다는 뜻이다.
 *
 * digest 는 오브젝트 메타데이터에 둔다. ETag 는 쓸 수 없다 — 단일 업로드에서는 MD5 지만
 * 분할 업로드에서는 아니고, 우리가 대조하는 것은 SHA-256 이다.
 */
class S3BlobStore(private val client: S3Client, private val bucket: String) : BlobStore {

    override fun put(key: String, bytes: ByteArray, contentType: String, digest: String?) {
        client.putObject(
            {
                it.bucket(bucket).key(key).contentType(contentType).contentLength(bytes.size.toLong())
                if (digest != null) it.metadata(mapOf(DIGEST_METADATA to digest))
            },
            RequestBody.fromBytes(bytes),
        )
    }

    override fun digestOf(key: String): String? = try {
        client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build()).metadata()[DIGEST_METADATA] ?: ""
    } catch (e: NoSuchKeyException) {
        null
    } catch (e: NoSuchBucketException) {
        throw IllegalStateException("버킷이 없다: $bucket", e)
    } catch (e: S3Exception) {
        // HEAD 는 본문이 없어 404 가 NoSuchKey 로 풀리지 않을 때가 있다.
        if (e.statusCode() == 404) null else throw e
    }

    override fun get(key: String): ByteArray? = try {
        client.getObjectAsBytes { it.bucket(bucket).key(key) }.asByteArray()
    } catch (e: NoSuchKeyException) {
        null
    }

    private companion object {
        /** S3 는 사용자 메타데이터 키를 소문자로 돌려준다. */
        const val DIGEST_METADATA = "sha256"
    }
}
