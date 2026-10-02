package ao.cmc.fincrestsdvm.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import ao.cmc.fincrestsdvm.data.jsonDoubleOrNull
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ao.cmc.fincrestsdvm.data.excel.parseBalanceteExcel
import ao.cmc.fincrestsdvm.data.formatCurrency
import ao.cmc.fincrestsdvm.data.jsonObjectOrNull
import ao.cmc.fincrestsdvm.data.jsonStringOrDash
import ao.cmc.fincrestsdvm.data.jsonStringOrNull
import ao.cmc.fincrestsdvm.data.validation.BALANCETE_SDVM_GROUPS
import ao.cmc.fincrestsdvm.data.validation.validateBalanceteSubmit
import ao.cmc.fincrestsdvm.data.validation.validateBalanceteUpdate
import ao.cmc.fincrestsdvm.ui.AppViewModel
import ao.cmc.fincrestsdvm.ui.components.ColumnSpec
import ao.cmc.fincrestsdvm.ui.components.JsonImportCard
import ao.cmc.fincrestsdvm.ui.components.OicTab
import ao.cmc.fincrestsdvm.ui.components.OicTabRow
import ao.cmc.fincrestsdvm.ui.components.PeriodFilterBar
import ao.cmc.fincrestsdvm.ui.components.PeriodType
import ao.cmc.fincrestsdvm.ui.components.SimpleTable
import ao.cmc.fincrestsdvm.ui.components.StatusBadge
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

@Composable
fun BalancetesScreen(viewModel: AppViewModel, snackbarHostState: SnackbarHostState) {
    val store = viewModel.balancete
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf(OicTab.CONSULTAR) }

    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Balancetes (${viewModel.tipoSociedade.label})", style = MaterialTheme.typography.headlineSmall)
        OicTabRow(tab) { tab = it }
        HorizontalDivider()

        when (tab) {
            OicTab.CONSULTAR -> {
                PeriodFilterBar(periodType = PeriodType.MES, loading = store.loading) { ano, mes, nif ->
                    scope.launch { store.fetchByPeriod(ano, mes, nif) }
                }

                // dados is a list with at most one balancete per period for the authenticated entity.
                val resultado = store.dados as? JsonArray
                val dados = resultado?.firstOrNull()?.jsonObjectOrNull()
                if (dados == null && !store.loading) {
                    Text(
                        if (resultado != null) "Não existem balancetes registados para o período indicado."
                        else "Escolha um ano e mês e clique em “Consultar” para ver o balancete já submetido.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                store.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                if (dados != null) BalanceteSheetViewer(dados, Modifier.weight(1f))
            }

            OicTab.SUBMETER -> JsonImportCard(
                title = "Submeter balancete",
                description = "Envie o balancete completo do período, com as 253 contas do plano de contas nos cinco grupos (activos, passivos, fundos próprios, resultados e contas extrapatrimoniais).",
                submitLabel = "Submeter",
                submitting = store.submitting,
                serverError = store.actionError,
                serverErrorDetails = store.errorDetails,
                validate = ::validateBalanceteSubmit,
                excelParser = ::parseBalanceteExcel,
                onSubmit = { payload ->
                    scope.launch {
                        if (store.submit(payload)) {
                            tab = OicTab.CONSULTAR
                            snackbarHostState.showSnackbar("Balancete submetido com sucesso.")
                        } else {
                            snackbarHostState.showSnackbar(store.error ?: "Não foi possível submeter o balancete.")
                        }
                    }
                }
            )

            OicTab.ACTUALIZAR -> JsonImportCard(
                title = "Actualizar balancete",
                description = "Envie os grupos a corrigir para um período já submetido, cada um com todas as contas do plano. O grupo fundos próprios não é actualizado por este serviço.",
                submitLabel = "Actualizar",
                submitting = store.updating,
                serverError = store.actionError,
                serverErrorDetails = store.errorDetails,
                validate = ::validateBalanceteUpdate,
                excelParser = ::parseBalanceteExcel,
                onSubmit = { payload ->
                    scope.launch {
                        if (store.update(payload)) {
                            tab = OicTab.CONSULTAR
                            snackbarHostState.showSnackbar("Balancete actualizado com sucesso.")
                        } else {
                            snackbarHostState.showSnackbar(store.error ?: "Não foi possível actualizar o balancete.")
                        }
                    }
                }
            )
        }
    }
}

private val GROUP_LABELS = mapOf(
    "activos" to "Activos",
    "passivos" to "Passivos",
    "fundosProprios" to "Fundos Próprios",
    "resultados" to "Resultados",
    "contasExtrapatrimoniais" to "Contas Extrapatrimoniais"
)

private fun JsonObject.hasMovimento(): Boolean =
    listOf("debito", "credito", "saldo").any { (this[it].jsonDoubleOrNull() ?: 0.0) != 0.0 }

/**
 * Shows the balancete the way the reference Excel template lays it out: one
 * group at a time, the template's columns, and a sheet tab per group on top.
 */
@Composable
private fun BalanceteSheetViewer(dados: JsonObject, modifier: Modifier = Modifier) {
    var grupo by remember(dados) { mutableStateOf(BALANCETE_SDVM_GROUPS.keys.first()) }
    var soComMovimento by remember { mutableStateOf(false) }

    val todas = (dados[grupo] as? JsonArray)?.mapNotNull { it as? JsonObject }.orEmpty()
    val itens = if (soComMovimento) todas.filter { it.hasMovimento() } else todas

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxSize()) {
            SheetTabs(selected = grupo, dados = dados, onSelect = { grupo = it })
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Balancete ${dados["mes"].jsonStringOrDash()}/${dados["ano"].jsonStringOrDash()} — ${GROUP_LABELS[grupo] ?: grupo}",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        "${todas.size} contas · ${todas.count { it.hasMovimento() }} com movimento",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Text("Só contas com movimento", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.width(8.dp))
                Switch(checked = soComMovimento, onCheckedChange = { soComMovimento = it })
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                TotalLabel("Total débito", todas.sumOf { it["debito"].jsonDoubleOrNull() ?: 0.0 })
                TotalLabel("Total crédito", todas.sumOf { it["credito"].jsonDoubleOrNull() ?: 0.0 })
                TotalLabel("Total saldo", todas.sumOf { it["saldo"].jsonDoubleOrNull() ?: 0.0 })
            }
            HorizontalDivider()

            if (itens.isEmpty()) {
                Text(
                    if (todas.isEmpty()) "Sem contas submetidas neste grupo." else "Nenhuma conta com movimento neste grupo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                // key() resets the table's scroll position when switching sheets.
                key(grupo, soComMovimento) {
                    SimpleTable(
                        columns = listOf(
                            ColumnSpec("num_conta", width = 90.dp) { Text(it["num_conta"].jsonStringOrDash()) },
                            ColumnSpec("conta_principal", width = 260.dp) { Text(it["conta_principal"].jsonStringOrDash()) },
                            ColumnSpec("num_subconta", width = 110.dp) { Text(it["num_subconta"].jsonStringOrDash()) },
                            ColumnSpec("subconta", width = 260.dp) { Text(it["subconta"].jsonStringOrDash()) },
                            ColumnSpec("num_conta_movimento", width = 150.dp) { Text(it["num_conta_movimento"].jsonStringOrDash()) },
                            ColumnSpec("nome_conta", width = 280.dp) { Text(it["nome_conta"].jsonStringOrDash()) },
                            ColumnSpec("debito", align = TextAlign.End) { Text(formatCurrency(it["debito"]), textAlign = TextAlign.End) },
                            ColumnSpec("credito", align = TextAlign.End) { Text(formatCurrency(it["credito"]), textAlign = TextAlign.End) },
                            ColumnSpec("saldo", align = TextAlign.End) { Text(formatCurrency(it["saldo"]), textAlign = TextAlign.End) },
                            ColumnSpec("Estado", width = 130.dp) { StatusBadge(it["status"].jsonStringOrNull()) }
                        ),
                        rows = itens,
                        modifier = Modifier.weight(1f),
                        fillHeight = true
                    )
                }
            }

        }
    }
}

@Composable
private fun TotalLabel(label: String, value: Double) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        Text(formatCurrency(value), style = MaterialTheme.typography.bodyMedium)
    }
}

/** Excel-style sheet tabs: the active one looks attached to the sheet below it. */
@Composable
private fun SheetTabs(selected: String, dados: JsonObject, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .horizontalScroll(rememberScrollState())
            .padding(start = 8.dp, top = 6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        BALANCETE_SDVM_GROUPS.keys.forEach { grupo ->
            val isSelected = grupo == selected
            val count = (dados[grupo] as? JsonArray)?.size ?: 0
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .then(
                        if (isSelected) Modifier.border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                        ) else Modifier
                    )
                    .clickable { onSelect(grupo) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    "${GROUP_LABELS[grupo] ?: grupo} ($count)",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
