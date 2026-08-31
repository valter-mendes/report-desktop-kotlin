package ao.cmc.fincrestsdvm.data.excel

import java.io.InputStream
import java.text.Normalizer
import java.time.LocalDate
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

private val PARTICIPANTE_STRINGS = listOf(
    "nif_fundo", "nome_fundo", "categoria_up", "nome", "nif", "dt_subscricao", "dt_resgate", "nivel_risco"
)
private val PARTICIPANTE_NUMBERS = listOf(
    "valor_nominal_up", "quantidade_up_subscritas", "valor_subscrito", "valor_liquidado",
    "valor_nominal_up_resgate", "quantidade_up_resgatadas", "valor_regatados", "valor_actual_up"
)

private val OIC_GERAL_STRINGS = listOf("nif_fundo", "nome_fundo", "conta", "descricao", "moeda", "dt_cambio")
private val OIC_GERAL_NUMBERS = listOf("cambio", "quantidade", "valor_de_mercado", "valias", "valor", "juro", "total")

private val OIC_IMOBILIARIO_STRINGS = listOf(
    "nif_fundo", "nome_fundo", "numero_imovel", "endereco", "localidade",
    "nome_perito_1", "nome_perito_2", "nome_perito_3", "dt_aquisicao", "dt_avaliacao",
    "conta", "descricao", "moeda", "dt_cambio"
)
private val OIC_IMOBILIARIO_NUMBERS = listOf(
    "avaliacao_perito_1", "avaliacao_perito_2", "avaliacao_perito_3", "quantidade", "preco", "taxa_cambio", "valor_final_imovel"
)

private val INDICADOR_NUMBERS = listOf("vlg", "unidades_participacao_circulacao", "valor_unitario_participacao")

private fun record(row: Map<String, Any?>, strings: List<String>, numbers: List<String>): JsonObject = buildJsonObject {
    strings.forEach { key -> row.textOrNull(key)?.let { put(key, it) } }
    numbers.forEach { key -> row.numberOrNull(key)?.let { put(key, it) } }
}

private fun stripAccents(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFD).replace(Regex("\\p{M}"), "")

/**
 * Converts the "mapa auxiliar" reference template (PARTICIPANTES /
 * ACTIVOS FINANCEIROS / ACTIVOS IMOBILIÁRIOS / INDICADORES sheets) into the
 * same JSON shape src/schemas/oic/mapaAuxiliar.schema.ts validates in the
 * web app — the sheet's own INSTRUÇÕES tab is explicit that the Excel is a
 * reference only and the real submission has to be this JSON. The template
 * carries no ano/mes column, so those default to the current year/month —
 * shown at the top of the generated JSON for the user to correct before
 * submitting.
 */
fun parseMapaAuxiliarExcel(input: InputStream): JsonObject {
    val workbook = openWorkbook(input)
    try {
        val participantesSheet = workbook.getSheet("PARTICIPANTES")
            ?: throw IllegalArgumentException("Não foi encontrada a folha \"PARTICIPANTES\" no ficheiro.")
        val financeirosSheet = workbook.getSheet("ACTIVOS FINANCEIROS")
            ?: throw IllegalArgumentException("Não foi encontrada a folha \"ACTIVOS FINANCEIROS\" no ficheiro.")
        val imobiliariosSheet = workbook.getSheet("ACTIVOS IMOBILIÁRIOS")
        val indicadoresSheet = workbook.getSheet("INDICADORES")
            ?: throw IllegalArgumentException("Não foi encontrada a folha \"INDICADORES\" no ficheiro.")

        val participantes = readTemplateRows(participantesSheet)
            .map { record(it, PARTICIPANTE_STRINGS, PARTICIPANTE_NUMBERS) }
            .filter { it.isNotEmpty() }

        val oicGeral = readTemplateRows(financeirosSheet)
            .map { record(it, OIC_GERAL_STRINGS, OIC_GERAL_NUMBERS) }
            .filter { it.isNotEmpty() }

        val oicImobiliarios = imobiliariosSheet
            ?.let { readTemplateRows(it) }
            ?.map { record(it, OIC_IMOBILIARIO_STRINGS, OIC_IMOBILIARIO_NUMBERS) }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()

        var indicadorFinanceiros: JsonObject? = null
        var indicadorImobiliarios: JsonObject? = null
        readTemplateRows(indicadoresSheet).forEach { row ->
            val tipo = (row["tipo_oic"] as? String)?.let { stripAccents(it).uppercase() } ?: return@forEach
            val indicador = record(row, emptyList(), INDICADOR_NUMBERS)
            when {
                "IMOBILI" in tipo -> indicadorImobiliarios = indicador
                "GERAL" in tipo -> indicadorFinanceiros = indicador
            }
        }

        val now = LocalDate.now()
        return buildJsonObject {
            put("ano", now.year)
            put("mes", now.monthValue)
            putJsonArray("participantes") { participantes.forEach(::add) }
            putJsonObject("activosFinanceiros") {
                putJsonArray("oicGeral") { oicGeral.forEach(::add) }
                indicadorFinanceiros?.let { put("indicador", it) }
            }
            if (oicImobiliarios.isNotEmpty() || indicadorImobiliarios != null) {
                putJsonObject("activosImobiliarios") {
                    if (oicImobiliarios.isNotEmpty()) putJsonArray("oicImobiliarios") { oicImobiliarios.forEach(::add) }
                    indicadorImobiliarios?.let { put("indicador", it) }
                }
            }
        }
    } finally {
        workbook.close()
    }
}
