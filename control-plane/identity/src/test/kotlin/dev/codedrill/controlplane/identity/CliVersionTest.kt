package dev.codedrill.controlplane.identity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** 낡았다고 확실한 CLI 만 막는다 — 판을 읽을 수 없으면 지나간다. */
class CliVersionTest {

    @Test
    fun `최소보다 낮은 판은 낡았다`() {
        assertEquals("0.1.0", CliVersion.tooOld("codedrill/0.1.0 (darwin; arm64)", "0.2.0"))
        assertEquals("0.9.9", CliVersion.tooOld("codedrill/0.9.9 (linux; amd64)", "0.10.0"))
    }

    @Test
    fun `같거나 높은 판은 지나간다 — 자릿수가 아니라 수로 견준다`() {
        assertNull(CliVersion.tooOld("codedrill/0.2.0 (darwin; arm64)", "0.2.0"))
        assertNull(CliVersion.tooOld("codedrill/0.10.0 (darwin; arm64)", "0.9.0"))
        assertNull(CliVersion.tooOld("codedrill/1.0.0-rc1 (darwin; arm64)", "0.9.0"))
    }

    @Test
    fun `판을 읽을 수 없으면 막지 않는다`() {
        assertNull(CliVersion.tooOld("codedrill/dev (darwin; arm64)", "0.2.0"))
        assertNull(CliVersion.tooOld("Mozilla/5.0", "0.2.0"))
        assertNull(CliVersion.tooOld(null, "0.2.0"))
    }
}
