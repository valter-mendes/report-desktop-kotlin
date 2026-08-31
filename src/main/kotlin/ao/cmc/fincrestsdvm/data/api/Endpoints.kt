package ao.cmc.fincrestsdvm.data.api

import ao.cmc.fincrestsdvm.data.models.ApiResult
import ao.cmc.fincrestsdvm.data.models.SiraEnvelope
import ao.cmc.fincrestsdvm.data.models.SiraLoginRequest
import ao.cmc.fincrestsdvm.data.models.SiraLoginResponse
import ao.cmc.fincrestsdvm.data.models.SiraRecord
import ao.cmc.fincrestsdvm.data.models.StatusResponse
import ao.cmc.fincrestsdvm.data.models.unwrap
import kotlinx.serialization.json.JsonObject

class AuthApi(private val client: SiraApiClient) {
    suspend fun login(email: String, password: String): ApiResult<SiraLoginResponse> =
        client.post("/auth/login", SiraLoginRequest(email, password))
}

class ApuramentoTaxasApi(private val client: SiraApiClient) {
    private val prefix = "/api/reportes/mapa-apuramento-taxas-oic"

    suspend fun submit(payload: JsonObject): ApiResult<StatusResponse> =
        client.post<SiraEnvelope<StatusResponse>, JsonObject>(prefix, payload).unwrap()

    suspend fun update(payload: JsonObject): ApiResult<StatusResponse> =
        client.post<SiraEnvelope<StatusResponse>, JsonObject>("$prefix/update", payload).unwrap()

    suspend fun getByPeriod(ano: Int, trimestre: Int, nif: String?): ApiResult<SiraRecord> {
        val path = if (!nif.isNullOrBlank()) "$prefix/$ano/$trimestre/$nif" else "$prefix/$ano/$trimestre"
        return client.get<SiraEnvelope<SiraRecord>>(path).unwrap()
    }
}

class BalanceteApi(private val client: SiraApiClient) {
    private val prefix = "/api/reportes/balancete"

    suspend fun submit(payload: JsonObject): ApiResult<StatusResponse> =
        client.post<SiraEnvelope<StatusResponse>, JsonObject>(prefix, payload).unwrap()

    suspend fun update(payload: JsonObject): ApiResult<StatusResponse> =
        client.post<SiraEnvelope<StatusResponse>, JsonObject>("$prefix/updateBalancete", payload).unwrap()

    suspend fun getByPeriod(ano: Int, mes: Int, nifFundo: String?): ApiResult<SiraRecord> {
        val path = if (!nifFundo.isNullOrBlank()) "$prefix/$ano/$mes/$nifFundo" else "$prefix/$ano/$mes"
        return client.get<SiraEnvelope<SiraRecord>>(path).unwrap()
    }
}

class MapaAuxiliarApi(private val client: SiraApiClient) {
    private val prefix = "/api/reportes/mapa-auxiliar"

    suspend fun submit(payload: JsonObject): ApiResult<StatusResponse> =
        client.post<SiraEnvelope<StatusResponse>, JsonObject>(prefix, payload).unwrap()

    suspend fun update(payload: JsonObject): ApiResult<StatusResponse> =
        client.post<SiraEnvelope<StatusResponse>, JsonObject>("$prefix/update", payload).unwrap()

    suspend fun getByPeriod(ano: Int, mes: Int, nifFundo: String?): ApiResult<SiraRecord> =
        client.get<SiraEnvelope<SiraRecord>>(
            "$prefix/$ano/$mes",
            query = mapOf("nif_fundo" to nifFundo?.ifBlank { null })
        ).unwrap()
}

class ApuramentoTaxasAiApi(private val client: SiraApiClient) {
    private val prefix = "/api/reportes/mapa-apuramento-taxas-ai"

    suspend fun submit(payload: JsonObject): ApiResult<StatusResponse> =
        client.post<SiraEnvelope<StatusResponse>, JsonObject>(prefix, payload).unwrap()

    suspend fun update(payload: JsonObject): ApiResult<StatusResponse> =
        client.post<SiraEnvelope<StatusResponse>, JsonObject>("$prefix/update", payload).unwrap()

    suspend fun getByPeriod(ano: Int, mes: Int, nif: String?): ApiResult<SiraRecord> {
        val path = if (!nif.isNullOrBlank()) "$prefix/$ano/$mes/$nif" else "$prefix/$ano/$mes"
        return client.get<SiraEnvelope<SiraRecord>>(path).unwrap()
    }
}
