package ao.cmc.fincrestsdvm.data

import java.util.prefs.Preferences

/**
 * Mirrors the "sira.environment" cookie the web app persists via its
 * Settings store: which of the two SIRA hosts (Homologação/Produção)
 * requests are sent to. Stored in the OS user-preferences store so it
 * survives app restarts without needing a config file.
 */
class AppSettings {
    private val prefs = Preferences.userRoot().node("ao/cmc/fincrestsdvm")

    var environment: SiraEnvironment
        get() = SiraEnvironment.entries.find { it.name == prefs.get(KEY_ENVIRONMENT, null) }
            ?: SiraEnvironment.HOMOLOGACAO
        set(value) = prefs.put(KEY_ENVIRONMENT, value.name)

    companion object {
        private const val KEY_ENVIRONMENT = "environment"
    }
}
