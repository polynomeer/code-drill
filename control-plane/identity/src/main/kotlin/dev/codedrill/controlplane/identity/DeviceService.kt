package dev.codedrill.controlplane.identity

import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.util.UUID

/**
 * 기기 승인 (RFC 8628) — CLI 가 비밀번호 없이 로그인한다 (feature-roadmap 11단계 이어서 — 2단계).
 *
 * 1. CLI 가 [start] — 기기 코드(CLI 만 안다)와 사용자 코드(사람이 본다)를 받는다.
 * 2. 사람이 브라우저에서 로그인한 채로 사용자 코드를 맞춰 [decide] — 승인하거나 거절한다.
 * 3. CLI 는 간격을 두고 [poll] — 승인됐으면 **기기 세션**을 받는다 ([IdentityService.issueDevice]).
 *
 * 기기 코드는 해시만 남기고, 한 번만 세션으로 바뀐다. 기기 세션이 할 수 있는 일은 [DeviceAuthorization.allows].
 */
@Service
class DeviceService(
    private val devices: DeviceRepository,
    private val sessions: IdentityRepository,
    private val identity: IdentityService,
    private val properties: IdentityProperties,
    private val clock: Clock = Clock.systemUTC(),
) {

    data class Started(val deviceCode: String, val userCode: String, val expiresAt: Instant, val intervalSeconds: Long)

    sealed interface StartOutcome {
        data class Ok(val started: Started) : StartOutcome
        data object Throttled : StartOutcome
    }

    @Transactional
    fun start(deviceName: String, client: String, origin: AbuseGuard.Origin): StartOutcome {
        val now = clock.instant()
        if (!origin.exempt && devices.recentFromOrigin(origin.hash, now.minusSeconds(3600)) >= properties.deviceCodesPerHour) {
            return StartOutcome.Throttled
        }
        val deviceCode = Tokens.issue()
        val expiresAt = now.plus(properties.deviceCodeTtl)
        // 사용자 코드는 기다리는 요청끼리만 유일하면 된다. 겹치면(2.6×10^10 분의 일) 새로 뽑는다
        repeat(5) {
            val userCode = DeviceAuthorization.userCode()
            if (devices.insert(UUID.randomUUID(), Tokens.hash(deviceCode), userCode, deviceName.take(80), client.take(80), origin.hash, expiresAt)) {
                return StartOutcome.Ok(Started(deviceCode, userCode, expiresAt, properties.devicePollInterval.seconds))
            }
        }
        error("사용자 코드를 다섯 번 뽑아도 겹쳤다")
    }

    sealed interface PollOutcome {
        data class Issued(val session: IssuedSession) : PollOutcome
        data class Wait(val answer: DeviceAuthorization.Answer) : PollOutcome
    }

    @Transactional
    fun poll(deviceCode: String): PollOutcome {
        val request = devices.findByDeviceCodeHash(Tokens.hash(deviceCode)) ?: return PollOutcome.Wait(DeviceAuthorization.Answer.Expired)
        val now = clock.instant()
        val answer = DeviceAuthorization.answer(request.status, request.expiresAt, request.lastPolledAt, now, properties.devicePollInterval)
        if (answer != DeviceAuthorization.Answer.Issue) {
            if (answer == DeviceAuthorization.Answer.Pending || answer == DeviceAuthorization.Answer.SlowDown) devices.touchPoll(request.id, now)
            return PollOutcome.Wait(answer)
        }
        // 한 번만 — 동시에 두 번 물어도 세션은 하나다
        if (devices.consume(request.id) == 0) return PollOutcome.Wait(DeviceAuthorization.Answer.Expired)
        val session = identity.issueDevice(request.userId!!, IdentityRepository.Device(request.deviceName, request.client))
            ?: return PollOutcome.Wait(DeviceAuthorization.Answer.Denied)
        return PollOutcome.Issued(session)
    }

    /** 승인 화면이 보여 줄 것 — 기다리는 중이고 만료 전인 요청만 */
    fun describe(rawUserCode: String): DeviceRepository.Request? {
        val code = DeviceAuthorization.normalize(rawUserCode) ?: return null
        return devices.findPending(code)?.takeIf { it.expiresAt.isAfter(clock.instant()) }
    }

    @Transactional
    fun decide(rawUserCode: String, userId: String, approve: Boolean): Boolean {
        val code = DeviceAuthorization.normalize(rawUserCode) ?: return false
        return devices.decide(code, UUID.fromString(userId), approve, clock.instant()) == 1
    }

    fun connected(userId: String): List<IdentityRepository.Session> = sessions.devices(UUID.fromString(userId))

    @Transactional
    fun disconnect(userId: String, sessionId: UUID): Boolean = sessions.revokeDevice(UUID.fromString(userId), sessionId) == 1
}

/**
 * 기기 승인의 개인 데이터 (§11.3). 기기 세션 자체는 계정 삭제가 세션을 전부 끊을 때 함께 끊기고, 여기서는 그 사람이
 * 결정한 승인 요청(기기 이름이 남아 있다)을 지운다. 반출에는 연결된 기기 목록을 싣는다.
 */
@Component
class DevicePersonalData(private val devices: DeviceRepository, private val sessions: IdentityRepository) : PersonalData {
    override val area = "devices"

    override fun export(userId: String): Any = sessions.devices(UUID.fromString(userId)).map {
        mapOf("device" to it.device?.name, "client" to it.device?.client, "connectedAt" to it.createdAt, "lastUsedAt" to it.lastUsedAt)
    }

    override fun erase(userId: String): Map<String, Int> = mapOf("authorizations" to devices.eraseFor(UUID.fromString(userId)))
}
