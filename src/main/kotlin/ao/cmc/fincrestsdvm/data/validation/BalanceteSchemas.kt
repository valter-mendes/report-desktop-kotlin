package ao.cmc.fincrestsdvm.data.validation

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

// ---- Balancete (SDVM / SCVM) ----
// Distinct from the OIC balancete: five groups only, no total_up, strict schema.
// Group -> number of accounts in the official chart of accounts (253 in total).
val BALANCETE_SDVM_GROUPS = linkedMapOf(
    "activos" to 88,
    "passivos" to 37,
    "fundosProprios" to 24,
    "resultados" to 75,
    "contasExtrapatrimoniais" to 29
)

private val BALANCETE_ITEM_KEYS = setOf(
    "num_conta", "conta_principal", "num_subconta", "subconta",
    "num_conta_movimento", "nome_conta", "debito", "credito", "saldo"
)

private val BALANCETE_ROOT_KEYS = setOf("ano", "mes") + BALANCETE_SDVM_GROUPS.keys

private fun validateBalanceteItem(ctx: ValidationContext, path: List<String>, item: JsonObject) {
    item.requireString(ctx, path, "num_conta")
    item.requireString(ctx, path, "conta_principal")
    item.requireString(ctx, path, "num_subconta")
    item.requireString(ctx, path, "subconta")
    item.requireString(ctx, path, "num_conta_movimento")
    item.requireString(ctx, path, "nome_conta")
    item.requireNumber(ctx, path, "debito", min = 0.0)
    item.requireNumber(ctx, path, "credito", min = 0.0)
    item.requireNumber(ctx, path, "saldo")
    item.checkNoExtraKeys(ctx, path, BALANCETE_ITEM_KEYS)
}

/** A group that is sent must carry every account of the chart, not just the ones being changed. */
private fun validateBalanceteGroup(ctx: ValidationContext, group: String, array: JsonArray) {
    val expected = BALANCETE_SDVM_GROUPS.getValue(group)
    if (array.size != expected) {
        ctx.issue(listOf(group), "Deve conter as $expected contas do plano de contas (recebidas ${array.size}).")
    }
    array.forEachIndexed { index, element ->
        val path = listOf(group, index.toString())
        val item = element.asObjectOrNull()
        if (item == null) ctx.issue(path, "Deve ser um objecto.") else validateBalanceteItem(ctx, path, item)
    }
}

private fun ValidationContext.checkPeriodo(json: JsonObject) {
    val ano = json.requireInt(this, emptyList(), "ano", min = 2000)
    val mes = json.requireInt(this, emptyList(), "mes", min = 1, max = 12)
    checkAnoMes(ano, mes)
    json.checkNoExtraKeys(this, emptyList(), BALANCETE_ROOT_KEYS)
}

fun validateBalanceteSubmit(json: JsonObject): List<String> {
    val ctx = ValidationContext()
    ctx.checkPeriodo(json)
    BALANCETE_SDVM_GROUPS.keys.forEach { group ->
        json.requireArray(ctx, emptyList(), group, minSize = 1)?.let { validateBalanceteGroup(ctx, group, it) }
    }
    return ctx.result()
}

fun validateBalanceteUpdate(json: JsonObject): List<String> {
    val ctx = ValidationContext()
    ctx.checkPeriodo(json)

    var hasAny = false
    BALANCETE_SDVM_GROUPS.keys.forEach { group ->
        val array = json.optionalArray(group) ?: return@forEach
        if (array.isNotEmpty()) {
            hasAny = true
            validateBalanceteGroup(ctx, group, array)
        }
    }
    if (!hasAny) ctx.issue(listOf("activos"), "Inclua pelo menos um dos grupos contabilísticos com dados.")
    return ctx.result()
}
