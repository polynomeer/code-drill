package dev.codedrill.platform.storage

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.readBytes

/**
 * 디렉터리 하나를 스토어로 쓴다. **테스트와 오브젝트 스토어 없는 개발용이다.**
 *
 * 같은 머신에서만 통한다. 오케스트레이터가 여기 쓴 번들을 다른 노드의 Runner 는 볼 수
 * 없다 — 그래서 공개 환경에서는 [S3BlobStore] 다.
 */
class DirectoryBlobStore(private val root: Path) : BlobStore {

    override fun put(key: String, bytes: ByteArray, contentType: String, digest: String?) {
        val target = resolve(key).also { it.parent.createDirectories() }
        // 통째로 바꾼다. 쓰다 만 파일을 읽는 쪽이 집어 들면 digest 가 틀려 시스템 오류가 된다.
        val staging = Files.createTempFile(target.parent, ".${target.fileName}", ".part")
        Files.write(staging, bytes)
        Files.move(staging, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    override fun get(key: String): ByteArray? = resolve(key).takeIf { it.exists() }?.readBytes()

    /** 메타데이터가 없으니 내용으로 센다. 개발용이라 그 값이면 된다. */
    override fun digestOf(key: String): String? = get(key)?.let { bytes ->
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    }

    override fun delete(key: String) { resolve(key).deleteIfExists() }

    private fun resolve(key: String): Path {
        val path = root.resolve(key).normalize()
        require(path.startsWith(root)) { "키가 스토어 밖을 가리킨다: $key" }
        return path
    }
}
