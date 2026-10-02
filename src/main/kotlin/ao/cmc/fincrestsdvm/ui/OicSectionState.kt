package ao.cmc.fincrestsdvm.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ao.cmc.fincrestsdvm.data.models.ApiResult
import ao.cmc.fincrestsdvm.data.models.SiraRecord
import ao.cmc.fincrestsdvm.data.models.StatusResponse
import kotlinx.serialization.json.JsonObject

/**
 * Mirrors the shape of the three near-identical Pinia stores under
 * src/stores/oic in the web app: a currently loaded record, and independent
 * loading/submitting/updating flags so the three JsonImportCard actions
 * don't fight over one spinner.
 */
class OicSectionState(
    private val getByPeriod: suspend (ano: Int, periodo: Int, nif: String?) -> ApiResult<SiraRecord>,
    private val submitCall: suspend (JsonObject) -> ApiResult<StatusResponse>,
    private val updateCall: suspend (JsonObject) -> ApiResult<StatusResponse>
) {
    var dados by mutableStateOf<SiraRecord?>(null)
        private set
    var loading by mutableStateOf(false)
        private set
    var submitting by mutableStateOf(false)
        private set
    var updating by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    /** Message and per-field details (SIRA's `erros`) of the last failed submit/update. */
    var actionError by mutableStateOf<String?>(null)
        private set
    var errorDetails by mutableStateOf<List<String>>(emptyList())
        private set

    suspend fun fetchByPeriod(ano: Int, periodo: Int, nif: String?): Boolean {
        loading = true
        error = null
        val result = getByPeriod(ano, periodo, nif)
        when (result) {
            is ApiResult.Ok -> dados = result.data
            is ApiResult.Err -> {
                dados = null
                error = result.error.mensagem
            }
        }
        loading = false
        return result is ApiResult.Ok
    }

    suspend fun submit(payload: JsonObject): Boolean {
        submitting = true
        error = null
        actionError = null
        errorDetails = emptyList()
        val result = submitCall(payload)
        if (result is ApiResult.Err) {
            error = result.error.mensagem
            actionError = result.error.mensagem
            errorDetails = result.error.detailLines(payload)
        }
        submitting = false
        return result is ApiResult.Ok
    }

    suspend fun update(payload: JsonObject): Boolean {
        updating = true
        error = null
        actionError = null
        errorDetails = emptyList()
        val result = updateCall(payload)
        if (result is ApiResult.Err) {
            error = result.error.mensagem
            actionError = result.error.mensagem
            errorDetails = result.error.detailLines(payload)
        }
        updating = false
        return result is ApiResult.Ok
    }
}
