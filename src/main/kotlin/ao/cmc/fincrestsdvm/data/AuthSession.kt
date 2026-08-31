package ao.cmc.fincrestsdvm.data

import ao.cmc.fincrestsdvm.data.models.SiraUser
import java.util.Base64
import java.util.prefs.Preferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * The SIRA login endpoint does not return an explicit expiry field, but the
 * token itself is a JWT whose `exp` claim reflects the server-configured
 * lifetime (~1h per the docs). Decoding it locally lets callers know when to
 * treat a stored session as stale instead of waiting for the next 401.
 */
fun decodeJwtExpiryMs(token: String): Long? = try {
    val payload = token.split(".").getOrNull(1)
    if (payload == null) {
        null
    } else {
        val normalized = payload.replace('-', '+').replace('_', '/')
        val padded = normalized + "=".repeat((4 - normalized.length % 4) % 4)
        val json = Json.parseToJsonElement(String(Base64.getDecoder().decode(padded)))
        (json as? JsonObject)?.get("exp")?.jsonPrimitive?.longOrNull?.times(1000)
    }
} catch (_: Exception) {
    null
}

class AuthSession {
    private val prefs = Preferences.userRoot().node("ao/cmc/fincrestsdvm")

    var token: String?
        get() = prefs.get(KEY_TOKEN, null)
        private set(value) {
            if (value == null) prefs.remove(KEY_TOKEN) else prefs.put(KEY_TOKEN, value)
        }

    var user: SiraUser?
        get() = prefs.get(KEY_USER, null)?.let {
            try {
                Json.decodeFromString(SiraUser.serializer(), it)
            } catch (_: Exception) {
                null
            }
        }
        private set(value) {
            if (value == null) prefs.remove(KEY_USER) else prefs.put(KEY_USER, Json.encodeToString(value))
        }

    val isAuthenticated: Boolean
        get() = token != null && user != null

    /**
     * True when a token is stored but its JWT `exp` claim is in the past.
     * A token that cannot be decoded is treated as not-expired — the next
     * 401 from SIRA is then the authority.
     */
    val isExpired: Boolean
        get() {
            val currentToken = token ?: return false
            val expiresAtMs = decodeJwtExpiryMs(currentToken) ?: return false
            return expiresAtMs <= System.currentTimeMillis()
        }

    /** Restores + validates any session persisted from a previous run. */
    fun restore() {
        if (isExpired) clear()
    }

    fun setSession(accessToken: String, sessionUser: SiraUser) {
        token = accessToken
        user = sessionUser
    }

    fun clear() {
        token = null
        user = null
    }

    companion object {
        private const val KEY_TOKEN = "auth.token"
        private const val KEY_USER = "auth.user"
    }
}
