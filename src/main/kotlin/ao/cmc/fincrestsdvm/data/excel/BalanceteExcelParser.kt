package ao.cmc.fincrestsdvm.data.excel

import ao.cmc.fincrestsdvm.data.validation.BALANCETE_SDVM_GROUPS
import java.io.InputStream
import java.time.LocalDate
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.FormulaEvaluator
import org.apache.poi.ss.usermodel.Sheet

private val BALANCETE_IDENTIFIERS = listOf(
    "num_conta", "conta_principal", "num_subconta", "subconta", "num_conta_movimento", "nome_conta"
)
private val BALANCETE_VALUES = listOf("debito", "credito", "saldo")

/**
 * Converts balancete-sdvm-scvm-modelo-excel.xlsx (one sheet per group, named
 * after the JSON key, header on row 1, one account per row) into the JSON
 * validated by data/validation/BalanceteSchemas.kt.
 *
 * Unlike readTemplateRows, identifier cells are not trimmed: SIRA matches
 * them character-for-character against the chart of accounts and the
 * template itself carries trailing spaces in some names. `saldo` is a
 * formula (debito - credito) in the template, so formulas are evaluated
 * rather than trusting a possibly missing cached result. Empty value cells
 * are sent as 0, as required for accounts without movement. The template
 * has no ano/mes, so those default to the current year/month for the user
 * to correct before submitting.
 */
fun parseBalanceteExcel(input: InputStream): JsonObject {
    val workbook = openWorkbook(input)
    try {
        val evaluator = workbook.creationHelper.createFormulaEvaluator()
        val groups = BALANCETE_SDVM_GROUPS.keys.associateWith { group ->
            val sheet = workbook.getSheet(group)
                ?: throw IllegalArgumentException("Não foi encontrada a folha \"$group\" no ficheiro.")
            readBalanceteSheet(sheet, evaluator)
        }

        val now = LocalDate.now()
        return buildJsonObject {
            put("ano", now.year)
            put("mes", now.monthValue)
            groups.forEach { (group, contas) -> putJsonArray(group) { contas.forEach(::add) } }
        }
    } finally {
        workbook.close()
    }
}

private fun readBalanceteSheet(sheet: Sheet, evaluator: FormulaEvaluator): List<JsonObject> {
    val headerRow = sheet.getRow(0)
        ?: throw IllegalArgumentException("A folha \"${sheet.sheetName}\" não tem cabeçalho.")
    val columns = headerRow.mapNotNull { cell ->
        cell.takeIf { it.cellType == CellType.STRING }?.stringCellValue?.trim()?.let { it to cell.columnIndex }
    }.toMap()
    (BALANCETE_IDENTIFIERS + BALANCETE_VALUES).firstOrNull { it !in columns }?.let {
        throw IllegalArgumentException("Falta a coluna \"$it\" na folha \"${sheet.sheetName}\".")
    }

    return (1..sheet.lastRowNum).mapNotNull { rowIndex ->
        val row = sheet.getRow(rowIndex) ?: return@mapNotNull null
        val identifiers = BALANCETE_IDENTIFIERS.associateWith { key ->
            textCell(row.getCell(columns.getValue(key)), evaluator)
        }
        if (identifiers.values.all { it == null }) return@mapNotNull null
        buildJsonObject {
            identifiers.forEach { (key, value) -> value?.let { put(key, it) } }
            BALANCETE_VALUES.forEach { key ->
                put(key, numberCell(row.getCell(columns.getValue(key)), evaluator, sheet.sheetName, rowIndex, key))
            }
        }
    }
}

private fun textCell(cell: Cell?, evaluator: FormulaEvaluator): String? {
    if (cell == null) return null
    val value = evaluator.evaluate(cell) ?: return null
    return when (value.cellType) {
        CellType.STRING -> value.stringValue.takeIf { it.isNotBlank() }
        CellType.NUMERIC -> value.numberValue.asJsonNumber().toString()
        else -> null
    }
}

private fun numberCell(cell: Cell?, evaluator: FormulaEvaluator, sheet: String, rowIndex: Int, key: String): Number {
    if (cell == null) return 0
    val value = evaluator.evaluate(cell) ?: return 0
    return when (value.cellType) {
        CellType.NUMERIC -> value.numberValue.asJsonNumber()
        CellType.STRING -> value.stringValue.trim().let { text ->
            if (text.isEmpty()) 0
            else text.replace(",", ".").toDoubleOrNull()?.asJsonNumber()
                ?: throw IllegalArgumentException("Valor inválido em \"$sheet\", linha ${rowIndex + 1}, coluna $key: \"$text\".")
        }
        else -> 0
    }
}
