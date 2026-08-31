package ao.cmc.fincrestsdvm.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import ao.cmc.fincrestsdvm.data.formatCurrency
import ao.cmc.fincrestsdvm.data.jsonDoubleOrNull
import ao.cmc.fincrestsdvm.data.jsonObjectOrNull
import ao.cmc.fincrestsdvm.data.jsonStringOrDash
import ao.cmc.fincrestsdvm.data.jsonStringOrNull
import ao.cmc.fincrestsdvm.data.validation.validateApuramentoTaxas
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
import org.apache.poi.hpsf.Date

@Composable
fun ApuramentoTaxasScreen(viewModel: AppViewModel, snackbarHostState: SnackbarHostState) {
    val store = viewModel.apuramentoTaxas
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf(OicTab.CONSULTAR) }

    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Mapa de Apuramento de Taxas (OIC)", style = MaterialTheme.typography.headlineSmall)
        OicTabRow(tab) { tab = it }
        HorizontalDivider()

        when (tab) {
            OicTab.CONSULTAR -> {
                PeriodFilterBar(periodType = PeriodType.TRIMESTRE, loading = store.loading) {
                        ano,
                        trimestre,
                        nif ->
                    scope.launch { store.fetchByPeriod(ano, trimestre, nif) }
                }

                val dados = store.dados?.jsonObjectOrNull()
                val itens =
                        (dados?.get("itens") as? JsonArray)?.mapNotNull { it as? JsonObject }
                                ?: emptyList()

                if (dados == null && !store.loading) {
                    Text(
                            "Escolha um ano e trimestre e clique em “Consultar” para ver o apuramento já submetido.",
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
                    val total = itens.sumOf { it["valor_arrecadado"].jsonDoubleOrNull() ?: 0.0 }
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
                                "Total arrecadado: ${formatCurrency(total)} AOA",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.outline
                        )
                    }
                    fun JsonElement?.jsonStringorDashFormatted(): String {
                        val raw = this.jsonStringOrDash()

                        // println(raw)
                        // println(DateTimeFormatterUtil.formatIsoToLongDate(raw))

                        return if (raw != "-") DateTimeFormatterUtil.formatIsoToDisplay(raw)
                        else raw
                    }

                    SimpleTable(
                            columns =
                                    listOf(
                                            ColumnSpec("Tipo de instrumento", width = 200.dp) {
                                                Text(it["tipo_instrumento"].jsonStringOrDash())
                                            },
                                            ColumnSpec("Total de activos", align = TextAlign.End) {
                                                Text(
                                                        it["total_activos"].jsonStringOrDash(),
                                                        textAlign = TextAlign.End
                                                )
                                            },
                                            ColumnSpec("Taxa", align = TextAlign.End) {
                                                Text(
                                                        formatCurrency(it["taxa"]),
                                                        textAlign = TextAlign.End
                                                )
                                            },
                                            ColumnSpec(
                                                    "Valor arrecadado",
                                                    width = 160.dp,
                                                    align = TextAlign.End
                                            ) {
                                                Text(
                                                        formatCurrency(it["valor_arrecadado"]),
                                                        textAlign = TextAlign.End
                                                )
                                            },
                                            ColumnSpec("Estado", width = 130.dp) {
                                                StatusBadge(it["status"].jsonStringOrNull())
                                            },
                                            ColumnSpec("Criado em", width = 160.dp, align=TextAlign.End){
                                                Text(it["createdAt"].jsonStringorDashFormatted(), textAlign = TextAlign.End)
                                            },
                                            ColumnSpec("Atualizado em", width = 160.dp, align=TextAlign.End){
                                                Text(it["updatedAt"].jsonStringorDashFormatted(), textAlign = TextAlign.End)
                                            }
                                    ),
                            rows = itens
                    )
                }
            }
            OicTab.SUBMETER ->
                    JsonImportCard(
                            title = "Submeter mapa de apuramento de taxas",
                            description =
                                    "Envie, agregado por tipo de instrumento, o apuramento trimestral da taxa de supervisão. Após o registo, a CMC emite automaticamente uma RUPE enviada por e-mail.",
                            submitLabel = "Submeter",
                            submitting = store.submitting,
                            validate = ::validateApuramentoTaxas,
                            onSubmit = { payload ->
                                scope.launch {
                                    if (store.submit(payload)) {
                                        tab = OicTab.CONSULTAR
                                        snackbarHostState.showSnackbar(
                                                "Mapa de apuramento de taxas submetido com sucesso. A RUPE será enviada por e-mail."
                                        )
                                    } else {
                                        snackbarHostState.showSnackbar(
                                                store.error
                                                        ?: "Não foi possível submeter o mapa de apuramento de taxas."
                                        )
                                    }
                                }
                            }
                    )
            OicTab.ACTUALIZAR ->
                    JsonImportCard(
                            title = "Actualizar mapa de apuramento de taxas",
                            description =
                                    "Envie apenas as linhas (por tipo de instrumento) que necessitam de correcção para um trimestre já submetido, dentro do prazo regulamentar.",
                            submitLabel = "Actualizar",
                            submitting = store.updating,
                            validate = ::validateApuramentoTaxas,
                            onSubmit = { payload ->
                                scope.launch {
                                    if (store.update(payload)) {
                                        tab = OicTab.CONSULTAR
                                        snackbarHostState.showSnackbar(
                                                "Mapa de apuramento de taxas actualizado com sucesso."
                                        )
                                    } else {
                                        snackbarHostState.showSnackbar(
                                                store.error
                                                        ?: "Não foi possível actualizar o mapa de apuramento de taxas."
                                        )
                                    }
                                }
                            }
                    )
        }
    }
}
