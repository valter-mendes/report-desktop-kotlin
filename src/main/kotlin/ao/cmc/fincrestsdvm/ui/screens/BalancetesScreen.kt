package ao.cmc.fincrestsdvm.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import ao.cmc.fincrestsdvm.data.formatCurrency
import ao.cmc.fincrestsdvm.data.jsonObjectOrNull
import ao.cmc.fincrestsdvm.data.jsonStringOrDash
import ao.cmc.fincrestsdvm.data.jsonStringOrNull
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
        Text("Balancetes (OIC)", style = MaterialTheme.typography.headlineSmall)
        OicTabRow(tab) { tab = it }
        HorizontalDivider()

        when (tab) {
            OicTab.CONSULTAR -> {
                PeriodFilterBar(periodType = PeriodType.MES, loading = store.loading) { ano, mes, nif ->
                    scope.launch { store.fetchByPeriod(ano, mes, nif) }
                }

                val dados = store.dados?.jsonObjectOrNull()
                if (dados == null && !store.loading) {
                    Text(
                        "Escolha um ano e mês e clique em “Consultar” para ver o balancete já submetido.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                store.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                dados?.entries?.forEach { (grupo, value) ->
                    val itens = (value as? JsonArray)?.mapNotNull { it as? JsonObject } ?: return@forEach
                    Card {
                        Column(modifier = Modifier.padding(bottom = if (itens.isEmpty()) 12.dp else 0.dp)) {
                            Text(
                                "$grupo (${itens.size})",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(12.dp)
                            )
                            if (itens.isEmpty()) {
                                Text(
                                    "Sem movimentos submetidos neste grupo.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            } else {
                                SimpleTable(
                                    columns = listOf(
                                        ColumnSpec("Conta", width = 220.dp) {
                                            Text("${it["num_conta"].jsonStringOrDash()} — ${it["conta_principal"].jsonStringOrDash()}")
                                        },
                                        ColumnSpec("Subconta de movimento", width = 220.dp) {
                                            Text("${it["num_conta_movimento"].jsonStringOrDash()} — ${it["nome_conta"].jsonStringOrDash()}")
                                        },
                                        ColumnSpec("Débito", align = TextAlign.End) { Text(formatCurrency(it["debito"]), textAlign = TextAlign.End) },
                                        ColumnSpec("Crédito", align = TextAlign.End) { Text(formatCurrency(it["credito"]), textAlign = TextAlign.End) },
                                        ColumnSpec("Saldo", align = TextAlign.End) { Text(formatCurrency(it["saldo"]), textAlign = TextAlign.End) },
                                        ColumnSpec("Estado", width = 130.dp) { StatusBadge(it["status"].jsonStringOrNull()) }
                                    ),
                                    rows = itens
                                )
                            }
                        }
                    }
                }
            }

            OicTab.SUBMETER -> JsonImportCard(
                title = "Submeter balancete",
                description = "Envie o balancete completo do período, com todos os grupos contabilísticos (activos, passivos, compensações, fundos próprios, resultados e fluxos de caixa).",
                submitLabel = "Submeter",
                submitting = store.submitting,
                validate = ::validateBalanceteSubmit,
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
                description = "Envie apenas os grupos contabilísticos que necessitam de correcção para um período já submetido.",
                submitLabel = "Actualizar",
                submitting = store.updating,
                validate = ::validateBalanceteUpdate,
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
