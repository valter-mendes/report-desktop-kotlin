package ao.cmc.fincrestsdvm.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ao.cmc.fincrestsdvm.data.AppSettings
import ao.cmc.fincrestsdvm.data.AuthSession
import ao.cmc.fincrestsdvm.data.SiraEnvironment
import ao.cmc.fincrestsdvm.data.api.ApuramentoTaxasAiApi
import ao.cmc.fincrestsdvm.data.api.ApuramentoTaxasApi
import ao.cmc.fincrestsdvm.data.api.AuthApi
import ao.cmc.fincrestsdvm.data.api.BalanceteApi
import ao.cmc.fincrestsdvm.data.api.MapaAuxiliarApi
import ao.cmc.fincrestsdvm.data.api.SiraApiClient
import ao.cmc.fincrestsdvm.data.models.ApiResult
import ao.cmc.fincrestsdvm.data.models.SiraUser

enum class Screen(val label: String, val icon: String) {
    HOME("Início", "home"),
    MAPAS_AUXILIARES("Mapas Auxiliares", "group"),
    BALANCETES("Balancetes", "balance"),
    APURAMENTO_TAXAS("Apuramento de Taxas (OIC)", "percent"),
    APURAMENTO_TAXAS_AI("Apuramento de Taxas (AI)", "trending_up")
}

class AppViewModel {
    val settings = AppSettings()
    val session = AuthSession()

    private val apiClient = SiraApiClient(settings, session, onSessionExpired = ::handleSessionExpired)
    private val authApi = AuthApi(apiClient)
    private val mapaAuxiliarApi = MapaAuxiliarApi(apiClient)
    private val balanceteApi = BalanceteApi(apiClient)
    private val apuramentoTaxasApi = ApuramentoTaxasApi(apiClient)
    private val apuramentoTaxasAiApi = ApuramentoTaxasAiApi(apiClient)

    val mapaAuxiliar = OicSectionState(
        getByPeriod = mapaAuxiliarApi::getByPeriod,
        submitCall = mapaAuxiliarApi::submit,
        updateCall = mapaAuxiliarApi::update
    )
    val balancete = OicSectionState(
        getByPeriod = balanceteApi::getByPeriod,
        submitCall = balanceteApi::submit,
        updateCall = balanceteApi::update
    )
    val apuramentoTaxas = OicSectionState(
        getByPeriod = apuramentoTaxasApi::getByPeriod,
        submitCall = apuramentoTaxasApi::submit,
        updateCall = apuramentoTaxasApi::update
    )
    val apuramentoTaxasAi = OicSectionState(
        getByPeriod = apuramentoTaxasAiApi::getByPeriod,
        submitCall = apuramentoTaxasAiApi::submit,
        updateCall = apuramentoTaxasAiApi::update
    )

    var isAuthenticated by mutableStateOf(false)
        private set
    var user by mutableStateOf<SiraUser?>(null)
        private set
    var environment by mutableStateOf(settings.environment)
        private set
    var currentScreen by mutableStateOf(Screen.HOME)
    var authenticating by mutableStateOf(false)
        private set
    var authError by mutableStateOf<String?>(null)
        private set
    /** Set when a session is dropped mid-use so the login screen can explain why. */
    var sessionExpiredNotice by mutableStateOf<String?>(null)
        private set

    init {
        session.restore()
        isAuthenticated = session.isAuthenticated
        user = session.user
    }

    /**
     * Called by [SiraApiClient] when the token expires locally or SIRA returns
     * a 401. Clears the session so [FincrestApp] swaps back to the login screen.
     */
    private fun handleSessionExpired() {
        if (!isAuthenticated) return
        session.clear()
        isAuthenticated = false
        user = null
        currentScreen = Screen.HOME
        sessionExpiredNotice = "A sua sessão expirou. Autentique-se novamente."
    }

    suspend fun signIn(email: String, password: String): Boolean {
        authenticating = true
        authError = null
        sessionExpiredNotice = null
        val result = authApi.login(email, password)
        authenticating = false
        return when (result) {
            is ApiResult.Ok -> {
                session.setSession(result.data.token, result.data.user)
                isAuthenticated = true
                user = result.data.user
                currentScreen = Screen.HOME
                true
            }
            is ApiResult.Err -> {
                authError = result.error.mensagem
                false
            }
        }
    }

    fun signOut() {
        session.clear()
        isAuthenticated = false
        user = null
    }

    fun changeEnvironment(env: SiraEnvironment) {
        settings.environment = env
        environment = env
    }
}
