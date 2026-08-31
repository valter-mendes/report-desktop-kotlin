package ao.cmc.fincrestsdvm.data

enum class SiraEnvironment(val label: String, val baseUrl: String) {
    HOMOLOGACAO("Homologação", "https://reporteshml.cmc.ao"),
    PRODUCAO("Produção", "https://reportes.cmc.ao")
}
