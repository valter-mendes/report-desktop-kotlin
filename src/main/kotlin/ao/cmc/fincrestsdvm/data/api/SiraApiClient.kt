package ao.cmc.fincrestsdvm.data.api

import ao.cmc.fincrestsdvm.data.AppSettings
import ao.cmc.fincrestsdvm.data.AuthSession
import ao.cmc.fincrestsdvm.data.models.ApiResult
import ao.cmc.fincrestsdvm.data.models.SiraErrorBody
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Native desktop equivalent of the web app's src/plugins/01.api.ts +
 * server/api/sira/[...path].ts pair. There is no browser here, so there is
 * no CORS restriction to route around — this calls reporteshml.cmc.ao /
 * reportes.cmc.ao directly, same as the deleted Nitro proxy did
 * server-to-server.
 */
class SiraApiClient(
    @PublishedApi internal val settings: AppSettings,
    @PublishedApi internal val session: AuthSession,
    /**
     * Invoked whenever the session is found to be invalid — either the stored
     * JWT has expired locally, or SIRA answered a request with 401. The app
     * wires this to clearing the session and routing back to the login screen.
     */
    @PublishedApi internal val onSessionExpired: () -> Unit = {}
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    @PublishedApi
    internal val client = HttpClient(CIO) {
        engine {
            https {
                trustManager = buildSiraTrustManager()
            }
        }
        install(ContentNegotiation) {
            json(json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 30_000
        }
        expectSuccess = false
    }

    suspend inline fun <reified T> get(path: String, query: Map<String, String?> = emptyMap()): ApiResult<T> =
        execute(HttpMethod.Get, path, query)

    // `body` is reified too (not just `Any?`) so Ktor's ContentNegotiation
    // plugin knows its exact compile-time type when picking a serializer —
    // erasing it to Any made it fall back to reflection, which chokes on
    // JsonElement's internal JsonLiteral class ("Serializer for class
    // 'JsonLiteral' is not found").
    suspend inline fun <reified T, reified B> post(path: String, body: B): ApiResult<T> =
        execute(HttpMethod.Post, path) {
            contentType(ContentType.Application.Json)
            setBody(body)
        }

    // Network/TLS/timeout failures and response-shape mismatches used to
    // propagate as uncaught exceptions all the way to a raw AWT error
    // dialog instead of the app's own snackbar/error UI. Caught here and
    // turned into an ApiResult.Err instead, same as the deleted Nitro
    // proxy's fallback body for an unreachable SIRA.
    suspend inline fun <reified T> execute(
        method: HttpMethod,
        path: String,
        query: Map<String, String?> = emptyMap(),
        block: HttpRequestBuilder.() -> Unit = {}
    ): ApiResult<T> {
        // Proactively bail out if the stored JWT is already past its `exp`,
        // so an expired session routes back to login without a round-trip.
        if (session.isExpired) {
            onSessionExpired()
            return ApiResult.Err(
                401,
                SiraErrorBody(mensagem = "A sua sessão expirou. Autentique-se novamente.")
            )
        }
        return try {
            val response = client.request(settings.environment.baseUrl + path) {
                this.method = method
                session.token?.let { header(HttpHeaders.Authorization, "Bearer $it") }
                query.forEach { (key, value) -> if (value != null) parameter(key, value) }
                block()
            }
            if (response.status.isSuccess()) {
                ApiResult.Ok(response.status.value, response.body<T>())
            } else {
                val error = try {
                    response.body<SiraErrorBody>()
                } catch (_: Exception) {
                    SiraErrorBody()
                }
                // SIRA rejected the token (expired or revoked server-side) —
                // drop the session and send the user back to login.
                if (response.status.value == 401) {
                    onSessionExpired()
                    return ApiResult.Err(
                        401,
                        SiraErrorBody(
                            mensagem = error.mensagem.takeIf { it.isNotBlank() && it != SiraErrorBody().mensagem }
                                ?: "A sua sessão expirou. Autentique-se novamente."
                        )
                    )
                }
                ApiResult.Err(response.status.value, error)
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            ApiResult.Err(
                0,
                SiraErrorBody(mensagem = "Não foi possível contactar o SIRA (${e.message ?: e::class.simpleName}). Tente novamente mais tarde.")
            )
        }
    }
}
