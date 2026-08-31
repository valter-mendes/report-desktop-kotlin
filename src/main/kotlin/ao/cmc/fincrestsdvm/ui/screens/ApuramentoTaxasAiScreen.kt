package ao.cmc.fincrestsdvm.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import ao.cmc.fincrestsdvm.data.DateTimeFormatterUtil
import ao.cmc.fincrestsdvm.data.excel.parseApuramentoTaxasAiExcel
import ao.cmc.fincrestsdvm.data.formatCurrency
import ao.cmc.fincrestsdvm.data.jsonDoubleOrNull
import ao.cmc.fincrestsdvm.data.jsonObjectOrNull
import ao.cmc.fincrestsdvm.data.jsonStringOrDash
import ao.cmc.fincrestsdvm.data.jsonStringOrNull
import ao.cmc.fincrestsdvm.data.validation.validateApuramentoTaxasAi
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
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Composable
fun ApuramentoTaxasAiScreen(viewModel: AppViewModel, snackbarHostState: SnackbarHostState) {
    val store = viewModel.apuramentoTaxasAi
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf(OicTab.CONSULTAR) }

    Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
                "Mapa de Apuramento de Taxas (Agentes de Intermediação)",
                style = MaterialTheme.typography.headlineSmall
        )
        OicTabRow(tab) { tab = it }
        HorizontalDivider()

        when (tab) {
            OicTab.CONSULTAR -> {
                PeriodFilterBar(periodType = PeriodType.MES, loading = store.loading) {
                        ano,
                        mes,
                        nif ->
                    scope.launch { store.fetchByPeriod(ano, mes, nif) }
                }

                val dados = store.dados?.jsonObjectOrNull()
                val itens =
                        (dados?.get("itens") as? JsonArray)?.mapNotNull { it as? JsonObject }
                                ?: emptyList()

                if (dados == null && !store.loading) {
                    Text(
                            "Escolha um ano e mês e clique em “Consultar” para ver o apuramento já submetido.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                    )
                }
                store.error?.let {
                    Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                    )
                }

                if (dados != null) {
                    val total = itens.sumOf { it["taxa_supervisao"].jsonDoubleOrNull() ?: 0.0 }
                    Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                                "Linhas de apuramento (${itens.size})",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                                "Total taxa de supervisão: ${formatCurrency(total)} AOA",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.outline
                        )
                    }

                    fun JsonElement?.jsonStringOrDashFormatted(): String {
                        val raw = this.jsonStringOrDash()

                        return if (raw != "-") DateTimeFormatterUtil.formatIsoToDisplay(raw)
                        else raw
                    }
                    SimpleTable(
                            columns =
                                    listOf(
                                            ColumnSpec("ISIN", width = 150.dp) {
                                                Text(it["isin"].jsonStringOrDash())
                                            },
                                            ColumnSpec("Tipo de instrumento", width = 200.dp) {
                                                Text(it["tipo_instrumento"].jsonStringOrDash())
                                            },
                                            ColumnSpec(
                                                    "Preço de mercado",
                                                    width = 120.dp,
                                                    align = TextAlign.End
                                            ) {
                                                Text(
                                                        formatCurrency(it["preco_mercado"]),
                                                        textAlign = TextAlign.End
                                                )
                                            },
                                            ColumnSpec("Quantidade", align = TextAlign.End) {
                                                Text(
                                                        it["quantidade"].jsonStringOrDash(),
                                                        textAlign = TextAlign.End
                                                )
                                            },
                                            ColumnSpec(
                                                    "Montante apurado",
                                                    width = 160.dp,
                                                    align = TextAlign.End
                                            ) {
                                                Text(
                                                        formatCurrency(it["montante_apurado"]),
                                                        textAlign = TextAlign.End
                                                )
                                            },
                                            ColumnSpec(
                                                    "Factor de rateio",
                                                    width = 160.dp,
                                                    align = TextAlign.End
                                            ) {
                                                Text(
                                                        it["fator_rateio"].jsonStringOrDash(),
                                                        textAlign = TextAlign.End
                                                )
                                            },
                                            ColumnSpec(
                                                    "Taxa de supervisão",
                                                    width = 160.dp,
                                                    align = TextAlign.End
                                            ) {
                                                Text(
                                                        formatCurrency(it["taxa_supervisao"]),
                                                        textAlign = TextAlign.End
                                                )
                                            },
                                            ColumnSpec("Estado", width = 170.dp) {
                                                StatusBadge(it["status"].jsonStringOrNull())
                                            },
                                            ColumnSpec(
                                                    "Criado em",
                                                    width = 170.dp,
                                                    align = TextAlign.End
                                            ) {
                                                Text(
                                                        it["createdAt"].jsonStringOrDashFormatted(),
                                                        textAlign = TextAlign.End
                                                )
                                            },
                                            ColumnSpec(
                                                    "Atualizado em",
                                                    width = 170.dp,
                                                    align = TextAlign.End
                                            ) {
                                                Text(
                                                        it["updatedAt"].jsonStringOrDashFormatted(),
                                                        textAlign = TextAlign.End
                                                )
                                            }
                                    ),
                            rows = itens
                    )
                }
            }
            OicTab.SUBMETER ->
                    JsonImportCard(
                            title = "Submeter mapa de apuramento de taxas (AI)",
                            description =
                                    "Envie, por ISIN, todos os instrumentos financeiros detidos ou transaccionados no período. Após o registo, a CMC emite automaticamente uma RUPE enviada por e-mail.",
                            submitLabel = "Submeter",
                            submitting = store.submitting,
                            validate = ::validateApuramentoTaxasAi,
                            excelParser = ::parseApuramentoTaxasAiExcel,
                            onSubmit = { payload ->
                                scope.launch {
                                    if (store.submit(payload)) {
                                        tab = OicTab.CONSULTAR
                                        snackbarHostState.showSnackbar(
                                                "Reporte de apuramento de taxas dos agentes de intermediação submetido com sucesso."
                                        )
                                    } else {
                                        snackbarHostState.showSnackbar(
                                                store.error
                                                        ?: "Não foi possível submeter o reporte de apuramento de taxas."
                                        )
                                    }
                                }
                            }
                    )
            OicTab.ACTUALIZAR ->
                    JsonImportCard(
                            title = "Actualizar mapa de apuramento de taxas (AI)",
                            description =
                                    "Envie apenas os itens (por ISIN) que necessitam de correcção para um período já submetido, dentro do prazo regulamentar.",
                            submitLabel = "Actualizar",
                            submitting = store.updating,
                            validate = ::validateApuramentoTaxasAi,
                            excelParser = ::parseApuramentoTaxasAiExcel,
                            onSubmit = { payload ->
                                scope.launch {
                                    if (store.update(payload)) {
                                        tab = OicTab.CONSULTAR
                                        snackbarHostState.showSnackbar(
                                                "Reporte de apuramento de taxas dos agentes de intermediação actualizado com sucesso."
                                        )
                                    } else {
                                        snackbarHostState.showSnackbar(
                                                store.error
                                                        ?: "Não foi possível actualizar o reporte de apuramento de taxas."
                                        )
                                    }
                                }
                            }
                    )
        }
    }
}
