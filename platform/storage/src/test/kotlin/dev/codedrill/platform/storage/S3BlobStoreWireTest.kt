package dev.codedrill.platform.storage

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * 스토어로 나가는 업로드의 모양.
 *
 * SeaweedFS 는 꼬리 체크섬(x-amz-trailer)이 붙은 서명 청크 업로드를 TCP 조각에 따라
 * 틀리게 읽는다 — StorageConfig 의 주석. 그래서 SDK 의 기본값을 꺼 두었는데, SDK 를 올리거나
 * 조립을 바꾸다 그 설정이 빠지면 macOS 에서는 여전히 통과하고 CI 의 Linux 에서만 제출이 500
 * 이 된다. 그 회귀를 여기서 잡는다.
 */
class S3BlobStoreWireTest {

    private val headers = mutableListOf<Map<String, String>>()
    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
        createContext("/") { exchange ->
            headers += exchange.requestHeaders.mapKeys { it.key.lowercase() }.mapValues { it.value.joinToString(",") }
            exchange.requestBody.readAllBytes()
            exchange.sendResponseHeaders(200, -1)
            exchange.close()
        }
        start()
    }

    @AfterTest
    fun stop() = server.stop(0)

    @Test
    fun `업로드에 꼬리 체크섬을 달지 않는다`() {
        val store = StorageConfig().blobStore(
            StorageProperties(
                endpoint = "http://127.0.0.1:${server.address.port}",
                accessKey = "test", secretKey = "test",
            ),
        )

        store.put("sources/s.txt", "fun main() {}\n".toByteArray(), "text/plain; charset=utf-8", "digest")

        val put = headers.single()
        assertNull(put["x-amz-trailer"], "꼬리 체크섬을 달았다: $put")
        assertNull(put["x-amz-sdk-checksum-algorithm"])
        // 우리 digest 는 여전히 메타데이터로 간다 — 받는 쪽이 대조하는 것은 이것이다.
        assertEquals("digest", put["x-amz-meta-sha256"])
    }
}
