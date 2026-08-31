package ao.cmc.fincrestsdvm.data.excel

import java.io.InputStream
import java.time.format.DateTimeFormatter
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DateUtil
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory

private val ISO_DATE: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

fun openWorkbook(input: InputStream): Workbook = WorkbookFactory.create(input)

/**
 * Reads a sheet laid out like mapa-auxiliar-modelo.xlsx: a title row, a
 * description row, a header row of schema field names, a row marking each
 * column Obrigatório/Opcional, then one data row per record. Returns one
 * map (header name -> String/Double/Boolean) per non-blank data row.
 */
fun readTemplateRows(sheet: Sheet, headerRowIndex: Int = 2, dataStartRowIndex: Int = 4): List<Map<String, Any?>> {
    val headerRow = sheet.getRow(headerRowIndex) ?: return emptyList()
    val headers = mutableMapOf<Int, String>()
    headerRow.forEach { cell ->
        val name = (cellRawValue(cell) as? String)?.trim()
        if (!name.isNullOrBlank()) headers[cell.columnIndex] = name
    }
    if (headers.isEmpty()) return emptyList()

    val rows = mutableListOf<Map<String, Any?>>()
    for (rowIndex in dataStartRowIndex..sheet.lastRowNum) {
        val row = sheet.getRow(rowIndex) ?: continue
        val values = headers.mapValues { (colIndex, _) -> cellRawValue(row.getCell(colIndex)) }
        val hasData = values.values.any { it != null }
        if (hasData) rows += values.mapKeys { (colIndex, _) -> headers.getValue(colIndex) }
    }
    return rows
}

private fun cellRawValue(cell: Cell?): Any? {
    if (cell == null) return null
    return when (cell.cellType) {
        CellType.STRING -> cell.stringCellValue.trim().ifBlank { null }
        CellType.NUMERIC ->
            if (DateUtil.isCellDateFormatted(cell)) {
                cell.localDateTimeCellValue.toLocalDate().format(ISO_DATE)
            } else {
                cell.numericCellValue
            }
        CellType.BOOLEAN -> cell.booleanCellValue
        CellType.FORMULA -> when (cell.cachedFormulaResultType) {
            CellType.STRING -> cell.richStringCellValue.string.trim().ifBlank { null }
            CellType.NUMERIC -> cell.numericCellValue
            else -> null
        }
        else -> null
    }
}

/** Renders a whole-number Double without a trailing ".0" (e.g. NIFs, quantities). */
fun Double.asJsonNumber(): Number =
    if (!isInfinite() && !isNaN() && this == Math.floor(this)) toLong() else this

fun Map<String, Any?>.textOrNull(key: String): String? = when (val v = this[key]) {
    null -> null
    is String -> v.trim().ifBlank { null }
    is Double -> v.asJsonNumber().toString()
    else -> v.toString()
}

fun Map<String, Any?>.numberOrNull(key: String): Number? = when (val v = this[key]) {
    null -> null
    is Double -> v.asJsonNumber()
    is String -> v.trim().toDoubleOrNull()?.asJsonNumber()
    else -> null
}
