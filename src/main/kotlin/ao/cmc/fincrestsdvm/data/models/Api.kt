package ao.cmc.fincrestsdvm.data.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * `detalhe` is usually a string, but some services (e.g. the SDVM/SCVM
 * balancete) return an object such as { message, naoEnviados: [...] } —
 * typing it as String made the whole error body fail to decode.
 */
@Serializable
data class SiraErrorDetail(
    val detalhe: JsonElement? = null,
    val campo: String? = null
)

@Serializable
data class SiraErrorBody(
    val sucesso: Boolean = false,
    val mensagem: String = "Ocorreu um erro não identificado. Tente novamente.",
    val dados: JsonElement? = null,
    val erros: List<SiraErrorDetail>? = null
) {
    /**
     * Human-readable lines for each entry of `erros`. For accounts listed in
     * `naoEnviados`, `sent` (the submitted payload) is used to point at the
     * identifier field that differs from the chart of accounts — usually a
     * single character, which is otherwise very hard to spot.
     */
    fun detailLines(sent: JsonObject? = null): List<String> = erros.orEmpty().flatMap { erro ->
        val prefix = erro.campo?.let { "$it: " } ?: ""
        when (val detalhe = erro.detalhe) {
            null, is JsonNull -> listOfNotNull(erro.campo)
            is JsonPrimitive -> listOf(prefix + detalhe.content)
            is JsonObject -> {
                val message = (detalhe["message"] as? JsonPrimitive)?.content
                val sentGroup = (erro.campo?.let { sent?.get(it) } as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }
                val naoEnviados = (detalhe["naoEnviados"] as? JsonArray).orEmpty().flatMap { conta ->
                    val expected = conta as? JsonObject ?: return@flatMap emptyList()
                    describeMissing(expected, sentGroup)
                }
                listOf(prefix + (message ?: detalhe.toString())) + naoEnviados
            }
            else -> listOf(prefix + detalhe.toString())
        }
    }
}

private fun JsonObject.text(key: String): String? = (this[key] as? JsonPrimitive)?.content

private fun describeMissing(expected: JsonObject, sentGroup: List<JsonObject>): List<String> {
    val num = expected.text("num_conta_movimento") ?: "?"
    val header = "    em falta: $num — ${expected.text("nome_conta").orEmpty()}"
    // num_conta_movimento is not unique in the chart, so match on it plus the name when possible.
    val candidates = sentGroup.filter { it.text("num_conta_movimento") == num }
    val match = candidates.firstOrNull { it.text("nome_conta") == expected.text("nome_conta") } ?: candidates.firstOrNull()
        ?: return listOf("$header (não enviada)")
    val diffs = expected.keys.filter { it in IDENTIFIER_KEYS && match.text(it) != expected.text(it) }.map { key ->
        "        $key: enviado \"${match.text(key)}\", esperado \"${expected.text(key)}\""
    }
    return listOf(header) + diffs
}

private val IDENTIFIER_KEYS = setOf(
    "num_conta", "conta_principal", "num_subconta", "subconta", "num_conta_movimento", "nome_conta"
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
