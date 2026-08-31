package ao.cmc.fincrestsdvm.data.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
data class SiraErrorDetail(
    val detalhe: String? = null,
    val campo: String? = null
)

@Serializable
data class SiraErrorBody(
    val sucesso: Boolean = false,
    val mensagem: String = "Ocorreu um erro não identificado. Tente novamente.",
    val dados: JsonObject? = null,
    val erros: List<SiraErrorDetail>? = null
)

/**
 * "Consultar" endpoints return response shapes that mirror the submit/update
 * payloads but add server-assigned fields (id, status, payload_hash,
 * timestamps). Used only for read-only display, so it is intentionally left
 * unmodelled as raw JSON rather than duplicating every payload as a response
 * DTO — mirrors `SiraRecord = Record<string, any>` on the web frontend.
 */
typealias SiraRecord = JsonElement

sealed class ApiResult<out T> {
    data class Ok<T>(val status: Int, val data: T) : ApiResult<T>()
    data class Err(val status: Int, val error: SiraErrorBody) : ApiResult<Nothing>()

    val isOk get() = this is Ok<T>
}

@Serializable
data class StatusResponse(val status: String)

/**
 * Every SIRA reportes endpoint (submit/update/consultar) wraps its payload
 * as { sucesso, mensagem, dados }. auth/login is the one exception — it
 * returns SiraLoginResponse unwrapped — so this is only used for the
 * reportes endpoints, not AuthApi.
 */
@Serializable
data class SiraEnvelope<T>(
    val sucesso: Boolean = true,
    val mensagem: String = "",
    val dados: T
)

fun <T> ApiResult<SiraEnvelope<T>>.unwrap(): ApiResult<T> = when (this) {
    is ApiResult.Ok -> ApiResult.Ok(status, data.dados)
    is ApiResult.Err -> this
}
