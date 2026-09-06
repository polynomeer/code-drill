package dev.codedrill.controlplane.submission.trace

import com.fasterxml.jackson.databind.ObjectMapper
import dev.codedrill.judge.protocol.TraceChunk
import dev.codedrill.judge.protocol.TraceManifest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * 트레이스 저장소 (기술 설계서 §7.4).
 *
 * manifest 와 청크를 함께 커밋한다. 목차만 있고 청크가 없는 상태를 클라이언트가 보면
 * 요청한 청크마다 404 를 받게 되고, 그것을 "손상된 트레이스"와 구분할 수 없다.
 */
@Repository
class TraceRepository(private val jdbc: JdbcTemplate, private val json: ObjectMapper) {

    @Transactional
    fun save(manifest: TraceManifest, chunks: List<TraceChunk>) {
        jdbc.update(
            """
            INSERT INTO trace (id, submission_id, schema_version, status, manifest)
            VALUES (?, ?::uuid, ?, ?, ?::jsonb)
            ON CONFLICT (submission_id) DO UPDATE
               SET id = EXCLUDED.id,
                   schema_version = EXCLUDED.schema_version,
                   status = EXCLUDED.status,
                   manifest = EXCLUDED.manifest,
                   created_at = now()
            """.trimIndent(),
            manifest.traceId,
            manifest.submissionId,
            manifest.schemaVersion,
            manifest.status.name,
            json.writeValueAsString(manifest),
        )

        // 재처리로 청크 수가 줄 수 있다. 남은 옛 청크가 섞이면 목차와 어긋난다.
        jdbc.update("DELETE FROM trace_chunk WHERE trace_id = ?", manifest.traceId)
        for (chunk in chunks) {
            jdbc.update(
                "INSERT INTO trace_chunk (trace_id, seq, events) VALUES (?, ?, ?::jsonb)",
                manifest.traceId, chunk.index, json.writeValueAsString(chunk.events),
            )
        }
    }

    fun findManifest(submissionId: UUID): TraceManifest? =
        jdbc.query(
            "SELECT manifest FROM trace WHERE submission_id = ?",
            { rs, _ -> json.readValue(rs.getString(1), TraceManifest::class.java) },
            submissionId,
        ).firstOrNull()

    fun findChunk(submissionId: UUID, index: Int): TraceChunk? =
        jdbc.query(
            """
            SELECT c.trace_id, c.seq, c.events
              FROM trace_chunk c
              JOIN trace t ON t.id = c.trace_id
             WHERE t.submission_id = ? AND c.seq = ?
            """.trimIndent(),
            { rs, _ ->
                TraceChunk(
                    traceId = rs.getString("trace_id"),
                    index = rs.getInt("seq"),
                    events = json.readValue(
                        rs.getString("events"),
                        json.typeFactory.constructCollectionType(
                            List::class.java,
                            dev.codedrill.judge.protocol.TraceEvent::class.java,
                        ),
                    ),
                )
            },
            submissionId, index,
        ).firstOrNull()
}
