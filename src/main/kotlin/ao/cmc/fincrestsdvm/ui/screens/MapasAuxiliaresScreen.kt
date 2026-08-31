package ao.cmc.fincrestsdvm.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import ao.cmc.fincrestsdvm.data.excel.parseMapaAuxiliarExcel
import ao.cmc.fincrestsdvm.data.formatCurrency
import ao.cmc.fincrestsdvm.data.jsonObjectOrNull
import ao.cmc.fincrestsdvm.data.jsonStringOrDash
import ao.cmc.fincrestsdvm.data.jsonStringOrNull
import ao.cmc.fincrestsdvm.data.validation.validateMapaAuxiliarSubmit
import ao.cmc.fincrestsdvm.data.validation.validateMapaAuxiliarUpdate
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
fun MapasAuxiliaresScreen(viewModel: AppViewModel, snackbarHostState: SnackbarHostState) {
    val store = viewModel.mapaAuxiliar
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf(OicTab.CONSULTAR) }

    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Mapas Auxiliares (OIC)", style = MaterialTheme.typography.headlineSmall)
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
                        "Escolha um ano e mês e clique em “Consultar” para ver os dados já submetidos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                store.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                if (dados != null) {
                    val participantes = (dados["participantes"] as? JsonArray)?.mapNotNull { it as? JsonObject } ?: emptyList()
                    Card {
                        Column {
                            Text(
                                "Participantes (${participantes.size})",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(12.dp)
                            )
                            SimpleTable(
                                columns = listOf(
                                    ColumnSpec("Nome", width = 180.dp) { Text(it["nome"].jsonStringOrDash()) },
                                    ColumnSpec("NIF", width = 120.dp) { Text(it["nif"].jsonStringOrDash()) },
                                    ColumnSpec("Fundo", width = 160.dp) { Text(it["nome_fundo"].jsonStringOrDash()) },
                                    ColumnSpec("Valor subscrito", width = 150.dp, align = TextAlign.End) {
                                        Text(formatCurrency(it["valor_subscrito"]), textAlign = TextAlign.End)
                                    },
                                    ColumnSpec("Valor actual UP", width = 150.dp, align = TextAlign.End) {
                                        Text(formatCurrency(it["valor_actual_up"]), textAlign = TextAlign.End)
                                    },
                                    ColumnSpec("Estado", width = 130.dp) { StatusBadge(it["status"].jsonStringOrNull()) }
                                ),
                                rows = participantes
                            )
                        }
                    }

                    val financeiros = dados["activosFinanceiros"]?.jsonObjectOrNull()
                    val oicGeral = (financeiros?.get("oicGeral") as? JsonArray)?.mapNotNull { it as? JsonObject } ?: emptyList()
                    val indicadorFin = financeiros?.get("indicador")?.jsonObjectOrNull()
                    Card {
                        Column {
                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    "Activos financeiros — OIC Geral (${oicGeral.size})",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                if (indicadorFin != null) IndicadorSummary(indicadorFin)
                            }
                            SimpleTable(
                                columns = listOf(
                                    ColumnSpec("Conta", width = 140.dp) { Text(it["conta"].jsonStringOrDash()) },
                                    ColumnSpec("Descrição", width = 200.dp) { Text(it["descricao"].jsonStringOrDash()) },
                                    ColumnSpec("Moeda", width = 90.dp) { Text(it["moeda"].jsonStringOrDash()) },
                                    ColumnSpec("Valor de mercado", width = 150.dp, align = TextAlign.End) {
                                        Text(formatCurrency(it["valor_de_mercado"]), textAlign = TextAlign.End)
                                    },
                                    ColumnSpec("Total", width = 130.dp, align = TextAlign.End) {
                                        Text(formatCurrency(it["total"]), textAlign = TextAlign.End)
                                    },
                                    ColumnSpec("Estado", width = 130.dp) { StatusBadge(it["status"].jsonStringOrNull()) }
                                ),
                                rows = oicGeral
                            )
                        }
                    }

                    val imobiliarios = dados["activosImobiliarios"]?.jsonObjectOrNull()
                    val oicImobiliarios = (imobiliarios?.get("oicImobiliarios") as? JsonArray)?.mapNotNull { it as? JsonObject } ?: emptyList()
                    val indicadorImob = imobiliarios?.get("indicador")?.jsonObjectOrNull()
                    Card {
                        Column {
                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    "Activos imobiliários (${oicImobiliarios.size})",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                if (indicadorImob != null) IndicadorSummary(indicadorImob)
                            }
                            SimpleTable(
                                columns = listOf(
                                    ColumnSpec("Imóvel", width = 140.dp) { Text(it["numero_imovel"].jsonStringOrDash()) },
                                    ColumnSpec("Endereço", width = 200.dp) { Text(it["endereco"].jsonStringOrDash()) },
                                    ColumnSpec("Localidade", width = 140.dp) { Text(it["localidade"].jsonStringOrDash()) },
                                    ColumnSpec("Valor final", width = 140.dp, align = TextAlign.End) {
                                        Text(formatCurrency(it["valor_final_imovel"]), textAlign = TextAlign.End)
                                    },
                                    ColumnSpec("Estado", width = 130.dp) { StatusBadge(it["status"].jsonStringOrNull()) }
                                ),
                                rows = oicImobiliarios
                            )
                        }
                    }
                }
            }

            OicTab.SUBMETER -> JsonImportCard(
                title = "Submeter mapa auxiliar",
                description = "Envie o reporte agregado (participantes, activos financeiros e activos imobiliários) para um novo período. Só é permitida uma submissão inicial por mês. Pode carregar o modelo .xlsx preenchido ou colar o JSON directamente — confirme sempre o ano/mês antes de submeter.",
                submitLabel = "Submeter",
                submitting = store.submitting,
                validate = ::validateMapaAuxiliarSubmit,
                excelParser = ::parseMapaAuxiliarExcel,
                onSubmit = { payload ->
                    scope.launch {
                        if (store.submit(payload)) {
                            tab = OicTab.CONSULTAR
                            snackbarHostState.showSnackbar("Mapa auxiliar submetido com sucesso.")
                        } else {
                            snackbarHostState.showSnackbar(store.error ?: "Não foi possível submeter o mapa auxiliar.")
                        }
                    }
                }
            )

            OicTab.ACTUALIZAR -> JsonImportCard(
                title = "Actualizar mapa auxiliar",
                description = "Envie apenas os blocos (participantes, activos financeiros e/ou activos imobiliários) que necessitam de correcção para um período já submetido.",
                submitLabel = "Actualizar",
                submitting = store.updating,
                validate = ::validateMapaAuxiliarUpdate,
                excelParser = ::parseMapaAuxiliarExcel,
                onSubmit = { payload ->
                    scope.launch {
                        if (store.update(payload)) {
                            tab = OicTab.CONSULTAR
                            snackbarHostState.showSnackbar("Mapa auxiliar actualizado com sucesso.")
                        } else {
                            snackbarHostState.showSnackbar(store.error ?: "Não foi possível actualizar o mapa auxiliar.")
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun IndicadorSummary(indicador: JsonObject) {
    Text(
        "VLG: ${formatCurrency(indicador["vlg"])} · " +
            "UP em circulação: ${indicador["unidades_participacao_circulacao"].jsonStringOrDash()} · " +
            "Valor unitário: ${formatCurrency(indicador["valor_unitario_participacao"])}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.outline
    )
}
