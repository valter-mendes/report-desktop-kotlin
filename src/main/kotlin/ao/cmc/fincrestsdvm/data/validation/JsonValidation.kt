package ao.cmc.fincrestsdvm.data.validation

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull

/**
 * A minimal re-implementation of the field-level checks the web app expresses
 * with Zod in src/schemas/oic, for the same "paste/upload JSON, validate,
 * submit" flow (see JsonImportCard.vue) — not a general-purpose validation
 * framework, just enough to reproduce those specific schemas.
 */
class ValidationContext {
    private val issues = mutableListOf<String>()

    fun issue(path: List<String>, message: String) {
        issues += "${if (path.isEmpty()) "(raiz)" else path.joinToString(".")}: $message"
    }

    fun result(): List<String> = issues
}

fun JsonElement.asObjectOrNull(): JsonObject? = this as? JsonObject
fun JsonElement.asArrayOrNull(): JsonArray? = this as? JsonArray

fun JsonObject.requireString(ctx: ValidationContext, path: List<String>, key: String, minLength: Int = 1): String? {
    val element = this[key]
    val text = (element as? JsonPrimitive)?.takeIf { it.isString }?.content
    if (text == null) {
        ctx.issue(path + key, "Campo obrigatório.")
        return null
    }
    if (text.length < minLength) {
        ctx.issue(path + key, "Deve ter pelo menos $minLength caracter(es).")
    }
    return text
}

fun JsonObject.optionalString(key: String): String? =
    (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

fun JsonObject.requireNumber(ctx: ValidationContext, path: List<String>, key: String, min: Double? = null): Double? {
    val value = (this[key] as? JsonPrimitive)?.doubleOrNull
    if (value == null) {
        ctx.issue(path + key, "Deve ser um número.")
        return null
    }
    if (min != null && value < min) {
        ctx.issue(path + key, "Não pode ser negativo.")
    }
    return value
}

fun JsonObject.optionalNumber(ctx: ValidationContext, path: List<String>, key: String): Double? {
    val element = this[key] ?: return null
    if (element is JsonNull) return null
    val value = (element as? JsonPrimitive)?.doubleOrNull
    if (value == null) ctx.issue(path + key, "Deve ser um número.")
    return value
}

fun JsonObject.requireInt(ctx: ValidationContext, path: List<String>, key: String, min: Int? = null, max: Int? = null): Int? {
    val value = (this[key] as? JsonPrimitive)?.intOrNull
    if (value == null) {
        ctx.issue(path + key, "Deve ser um número inteiro.")
        return null
    }
    if (min != null && value < min) ctx.issue(path + key, "Deve ser igual ou superior a $min.")
    if (max != null && value > max) ctx.issue(path + key, "Deve ser igual ou inferior a $max.")
    return value
}

fun JsonObject.requireArray(ctx: ValidationContext, path: List<String>, key: String, minSize: Int = 0): JsonArray? {
    val array = this[key] as? JsonArray
    if (array == null) {
        if (minSize > 0) ctx.issue(path + key, "Campo obrigatório.")
        return null
    }
    if (array.size < minSize) {
        ctx.issue(path + key, if (minSize == 1) "Inclua pelo menos um registo." else "Inclua pelo menos $minSize registos.")
    }
    return array
}

fun JsonObject.optionalArray(key: String): JsonArray? = this[key] as? JsonArray

fun JsonObject.requireObject(ctx: ValidationContext, path: List<String>, key: String): JsonObject? {
    val obj = this[key] as? JsonObject
    if (obj == null) ctx.issue(path + key, "Campo obrigatório.")
    return obj
}

fun JsonObject.optionalObject(key: String): JsonObject? = this[key] as? JsonObject

/** For a z.strictObject() equivalent: flags any key not in `allowedKeys`. */
fun JsonObject.checkNoExtraKeys(ctx: ValidationContext, path: List<String>, allowedKeys: Set<String>) {
    keys.filter { it !in allowedKeys }.forEach { key ->
        ctx.issue(path + key, "Campo não reconhecido.")
    }
}
