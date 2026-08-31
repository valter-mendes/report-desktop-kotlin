package ao.cmc.fincrestsdvm.data.validation

import kotlinx.serialization.json.JsonObject

private val AI_ITEM_KEYS = setOf(
    "isin", "tipo_instrumento", "preco_mercado", "quantidade", "montante_apurado", "fator_rateio", "taxa_supervisao"
)

// Mirrors the SIRA docs for POST /api/reportes/mapa-apuramento-taxas-ai: ano/mes
// (not future), itens with >=1 element, each strictly isin + tipo_instrumento
// (required) plus optional numeric fields (fator_rateio nullable). Submit and
// update accept the same item shape.
fun validateApuramentoTaxasAi(json: JsonObject): List<String> {
    val ctx = ValidationContext()
    val ano = json.requireInt(ctx, emptyList(), "ano", min = 2000)
    val mes = json.requireInt(ctx, emptyList(), "mes", min = 1, max = 12)
    ctx.checkAnoMes(ano, mes)

    val itens = json.requireArray(ctx, emptyList(), "itens", minSize = 1)
    itens?.forEachIndexed { index, element ->
        val path = listOf("itens", index.toString())
        val item = element.asObjectOrNull()
        if (item == null) {
            ctx.issue(path, "Deve ser um objecto.")
        } else {
            item.requireString(ctx, path, "isin")
            item.requireString(ctx, path, "tipo_instrumento")
            item.optionalNumber(ctx, path, "preco_mercado")
            item.optionalNumber(ctx, path, "quantidade")
            item.optionalNumber(ctx, path, "montante_apurado")
            item.optionalNumber(ctx, path, "fator_rateio")
            item.optionalNumber(ctx, path, "taxa_supervisao")
            item.checkNoExtraKeys(ctx, path, AI_ITEM_KEYS)
        }
    }
    return ctx.result()
}
