package ao.cmc.fincrestsdvm.data.excel

import java.io.InputStream
import java.time.LocalDate
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

private val AI_STRINGS = listOf("isin", "tipo_instrumento")
private val AI_NUMBERS = listOf("preco_mercado", "quantidade", "montante_apurado", "fator_rateio", "taxa_supervisao")

/**
 * Converts mapa-apuramento-taxas-ai.xlsx into the JSON shape validated by
 * data/validation/AiSchemas.kt. Unlike the mapa-auxiliar template, this one
 * is a single flat sheet ("titulos", the field's pre-v4.0.0 name) with the
 * header on row 1 and data starting row 2 — no title/description rows. The
 * template carries no ano/mes column, so those default to the current
 * year/month, shown at the top of the generated JSON for correction before
 * submitting.
 */
fun parseApuramentoTaxasAiExcel(input: InputStream): JsonObject {
    val workbook = openWorkbook(input)
    try {
        val sheet = workbook.getSheetAt(0)
        val itens = readTemplateRows(sheet, headerRowIndex = 0, dataStartRowIndex = 1)
            .map { row ->
                buildJsonObject {
                    AI_STRINGS.forEach { key -> row.textOrNull(key)?.let { put(key, it) } }
                    AI_NUMBERS.forEach { key -> row.numberOrNull(key)?.let { put(key, it) } }
                }
            }
            .filter { it.isNotEmpty() }

        if (itens.isEmpty()) {
            throw IllegalArgumentException("Não foram encontradas linhas de dados na folha \"${sheet.sheetName}\".")
        }

        val now = LocalDate.now()
        return buildJsonObject {
            put("ano", now.year)
            put("mes", now.monthValue)
            putJsonArray("itens") { itens.forEach(::add) }
        }
    } finally {
        workbook.close()
    }
}
