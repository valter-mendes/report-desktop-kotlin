package ao.cmc.fincrestsdvm.data

import java.text.NumberFormat
import java.util.Locale
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull

private val currencyFormat: NumberFormat = NumberFormat.getNumberInstance(Locale.GERMANY).apply {
    minimumFractionDigits = 2
    maximumFractionDigits = 2
}

fun formatCurrency(value: Double?): String = currencyFormat.format(value ?: 0.0)
fun formatCurrency(value: JsonElement?): String = formatCurrency(value.jsonDoubleOrNull())

fun JsonElement?.jsonObjectOrNull(): JsonObject? = this as? JsonObject
fun JsonElement?.jsonStringOrNull(): String? = (this as? JsonPrimitive)?.takeIf { it.isString }?.content
/** Renders any scalar (string, number, boolean) for display; "—" if absent. */
fun JsonElement?.jsonStringOrDash(): String = (this as? JsonPrimitive)?.content ?: "—"
fun JsonElement?.jsonDoubleOrNull(): Double? = (this as? JsonPrimitive)?.doubleOrNull
fun JsonElement?.jsonIntOrNull(): Int? = (this as? JsonPrimitive)?.intOrNull

fun JsonObject.field(key: String): JsonElement? = this[key]
