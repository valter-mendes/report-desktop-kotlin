package ao.cmc.fincrestsdvm.data.validation

import java.time.LocalDate
import kotlinx.serialization.json.JsonObject

private val DATE_REGEX = Regex("""^\d{4}-\d{2}-\d{2}$""")

private fun JsonObject.requireDate(ctx: ValidationContext, path: List<String>, key: String) {
    val text = requireString(ctx, path, key) ?: return
    if (!DATE_REGEX.matches(text)) ctx.issue(path + key, "Data inválida (AAAA-MM-DD).")
}

private fun JsonObject.optionalDate(ctx: ValidationContext, path: List<String>, key: String) {
    val text = optionalString(key) ?: return
    if (!DATE_REGEX.matches(text)) ctx.issue(path + key, "Data inválida (AAAA-MM-DD).")
}

fun ValidationContext.checkAnoMes(ano: Int?, mes: Int?) {
    val now = LocalDate.now()
    if (ano != null && ano > now.year) issue(listOf("ano"), "O ano não pode ser superior ao ano actual.")
    if (ano != null && mes != null && ano == now.year && mes > now.monthValue) {
        issue(listOf("mes"), "O mês não pode ser superior ao mês actual.")
    }
}

fun ValidationContext.checkAnoTrimestre(ano: Int?, trimestre: Int?) {
    val now = LocalDate.now()
    val currentQuarter = (now.monthValue - 1) / 3 + 1
    if (ano != null && ano > now.year) issue(listOf("ano"), "O ano não pode ser superior ao ano actual.")
    if (ano != null && trimestre != null && ano == now.year && trimestre > currentQuarter) {
        issue(listOf("trimestre"), "O trimestre não pode ser superior ao trimestre actual.")
    }
}

// ---- Apuramento de Taxas (OIC) ----
// submit/update share the exact same shape in the web schemas.
fun validateApuramentoTaxas(json: JsonObject): List<String> {
    val ctx = ValidationContext()
    val ano = json.requireInt(ctx, emptyList(), "ano", min = 2000)
    val trimestre = json.requireInt(ctx, emptyList(), "trimestre", min = 1, max = 4)
    ctx.checkAnoTrimestre(ano, trimestre)

    val itens = json.requireArray(ctx, emptyList(), "itens", minSize = 1)
    itens?.forEachIndexed { index, element ->
        val path = listOf("itens", index.toString())
        val item = element.asObjectOrNull()
        if (item == null) {
            ctx.issue(path, "Deve ser um objecto.")
        } else {
            item.requireString(ctx, path, "tipo_instrumento")
            item.requireNumber(ctx, path, "total_activos", min = 0.0)
            item.requireNumber(ctx, path, "taxa", min = 0.0)
            item.requireNumber(ctx, path, "valor_arrecadado", min = 0.0)
            item.checkNoExtraKeys(ctx, path, setOf("tipo_instrumento", "total_activos", "taxa", "valor_arrecadado"))
        }
    }
    return ctx.result()
}

// ---- Mapa Auxiliar (OIC) ----
private fun validateParticipante(ctx: ValidationContext, path: List<String>, item: JsonObject) {
    item.requireString(ctx, path, "nif_fundo")
    item.requireString(ctx, path, "nome_fundo")
    item.requireString(ctx, path, "categoria_up")
    item.requireString(ctx, path, "nome", minLength = 5)
    item.requireString(ctx, path, "nif")
    item.requireDate(ctx, path, "dt_subscricao")
    item.requireNumber(ctx, path, "valor_nominal_up")
    item.requireNumber(ctx, path, "quantidade_up_subscritas")
    item.requireNumber(ctx, path, "valor_subscrito")
    item.requireNumber(ctx, path, "valor_liquidado")
    item.optionalDate(ctx, path, "dt_resgate")
    item.requireNumber(ctx, path, "valor_actual_up")
    item.requireString(ctx, path, "nivel_risco")
}

private fun validateOicGeralItem(ctx: ValidationContext, path: List<String>, item: JsonObject) {
    item.requireString(ctx, path, "nif_fundo")
    item.requireString(ctx, path, "nome_fundo")
    item.requireString(ctx, path, "conta")
    item.requireString(ctx, path, "descricao")
    item.requireString(ctx, path, "moeda")
    item.optionalDate(ctx, path, "dt_cambio")
}

private fun validateIndicador(ctx: ValidationContext, path: List<String>, item: JsonObject) {
    item.requireNumber(ctx, path, "vlg")
    item.requireInt(ctx, path, "unidades_participacao_circulacao")
    item.requireNumber(ctx, path, "valor_unitario_participacao")
}

private fun validateOicImobiliarioItem(ctx: ValidationContext, path: List<String>, item: JsonObject) {
    item.requireString(ctx, path, "nif_fundo")
    item.requireString(ctx, path, "nome_fundo")
    item.requireString(ctx, path, "numero_imovel")
    item.requireString(ctx, path, "endereco")
    item.requireString(ctx, path, "localidade")
    item.requireString(ctx, path, "nome_perito_1")
    item.requireString(ctx, path, "nome_perito_2")
    item.requireString(ctx, path, "nome_perito_3")
    item.optionalDate(ctx, path, "dt_aquisicao")
    item.requireDate(ctx, path, "dt_avaliacao")
    item.requireString(ctx, path, "conta")
    item.requireString(ctx, path, "descricao")
    item.requireString(ctx, path, "moeda")
    item.optionalDate(ctx, path, "dt_cambio")
}

fun validateMapaAuxiliarSubmit(json: JsonObject): List<String> {
    val ctx = ValidationContext()
    val ano = json.requireInt(ctx, emptyList(), "ano", min = 2000)
    val mes = json.requireInt(ctx, emptyList(), "mes", min = 1, max = 12)
    ctx.checkAnoMes(ano, mes)

    val participantes = json.requireArray(ctx, emptyList(), "participantes", minSize = 1)
    participantes?.forEachIndexed { index, element ->
        val path = listOf("participantes", index.toString())
        val item = element.asObjectOrNull()
        if (item == null) ctx.issue(path, "Deve ser um objecto.") else validateParticipante(ctx, path, item)
    }

    val financeiros = json.requireObject(ctx, emptyList(), "activosFinanceiros")
    if (financeiros != null) {
        val oicGeral = financeiros.requireArray(ctx, listOf("activosFinanceiros"), "oicGeral", minSize = 1)
        oicGeral?.forEachIndexed { index, element ->
            val path = listOf("activosFinanceiros", "oicGeral", index.toString())
            val item = element.asObjectOrNull()
            if (item == null) ctx.issue(path, "Deve ser um objecto.") else validateOicGeralItem(ctx, path, item)
        }
        val indicador = financeiros.requireObject(ctx, listOf("activosFinanceiros"), "indicador")
        indicador?.let { validateIndicador(ctx, listOf("activosFinanceiros", "indicador"), it) }
    }

    val imobiliarios = json.requireObject(ctx, emptyList(), "activosImobiliarios")
    if (imobiliarios != null) {
        imobiliarios.optionalArray("oicImobiliarios")?.forEachIndexed { index, element ->
            val path = listOf("activosImobiliarios", "oicImobiliarios", index.toString())
            val item = element.asObjectOrNull()
            if (item == null) ctx.issue(path, "Deve ser um objecto.") else validateOicImobiliarioItem(ctx, path, item)
        }
        val indicador = imobiliarios.requireObject(ctx, listOf("activosImobiliarios"), "indicador")
        indicador?.let { validateIndicador(ctx, listOf("activosImobiliarios", "indicador"), it) }
    }
    return ctx.result()
}

fun validateMapaAuxiliarUpdate(json: JsonObject): List<String> {
    val ctx = ValidationContext()
    val ano = json.requireInt(ctx, emptyList(), "ano", min = 2000)
    val mes = json.requireInt(ctx, emptyList(), "mes", min = 1, max = 12)
    ctx.checkAnoMes(ano, mes)

    val participantes = json.optionalArray("participantes")
    participantes?.forEachIndexed { index, element ->
        val path = listOf("participantes", index.toString())
        val item = element.asObjectOrNull()
        if (item == null) ctx.issue(path, "Deve ser um objecto.") else validateParticipante(ctx, path, item)
    }

    val financeiros = json.optionalObject("activosFinanceiros")
    val oicGeral = financeiros?.optionalArray("oicGeral")
    oicGeral?.forEachIndexed { index, element ->
        val path = listOf("activosFinanceiros", "oicGeral", index.toString())
        val item = element.asObjectOrNull()
        if (item == null) ctx.issue(path, "Deve ser um objecto.") else validateOicGeralItem(ctx, path, item)
    }
    financeiros?.optionalObject("indicador")?.let {
        validateIndicador(ctx, listOf("activosFinanceiros", "indicador"), it)
    }

    val imobiliarios = json.optionalObject("activosImobiliarios")
    val oicImobiliarios = imobiliarios?.optionalArray("oicImobiliarios")
    oicImobiliarios?.forEachIndexed { index, element ->
        val path = listOf("activosImobiliarios", "oicImobiliarios", index.toString())
        val item = element.asObjectOrNull()
        if (item == null) ctx.issue(path, "Deve ser um objecto.") else validateOicImobiliarioItem(ctx, path, item)
    }
    imobiliarios?.optionalObject("indicador")?.let {
        validateIndicador(ctx, listOf("activosImobiliarios", "indicador"), it)
    }

    val hasParticipantes = (participantes?.size ?: 0) > 0
    val hasFinanceiros = (oicGeral?.size ?: 0) > 0
    val hasImobiliarios = (oicImobiliarios?.size ?: 0) > 0
    if (!hasParticipantes && !hasFinanceiros && !hasImobiliarios) {
        ctx.issue(
            listOf("participantes"),
            "Inclua pelo menos um registo em participantes, activos financeiros ou activos imobiliários."
        )
    }
    return ctx.result()
}
