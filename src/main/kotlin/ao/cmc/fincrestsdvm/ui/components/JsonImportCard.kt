package ao.cmc.fincrestsdvm.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import ao.cmc.fincrestsdvm.ui.theme.SuccessColor
import java.awt.Dimension
import java.awt.Toolkit
import java.io.File
import java.io.InputStream
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * Kotlin counterpart of components/oic/JsonImportCard.vue: paste or load a
 * JSON payload, validate it client-side against the same rules as the web
 * app's Zod schemas (data/validation/OicSchemas.kt), and submit the raw,
 * validated JSON — no field-by-field form, matching the original UX. When
 * `excelParser` is supplied, an .xlsx (the reference report template) can
 * also be loaded and is converted to the same JSON before validation.
 */
@Composable
fun JsonImportCard(
    title: String,
    description: String,
    submitLabel: String,
    submitting: Boolean,
    validate: (JsonObject) -> List<String>,
    onSubmit: (JsonObject) -> Unit,
    excelParser: ((InputStream) -> JsonObject)? = null
) {
    var raw by remember { mutableStateOf("") }
    var loadError by remember { mutableStateOf<String?>(null) }

    val parsed: JsonObject? = remember(raw) {
        if (raw.isBlank()) null else runCatching { Json.parseToJsonElement(raw) as? JsonObject }.getOrNull()
    }
    val parseFailed = raw.isNotBlank() && parsed == null
    val errors = remember(parsed) { parsed?.let(validate) ?: emptyList() }
    val isValid = parsed != null && errors.isEmpty()

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }

            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                OutlinedButton(onClick = {
                    loadError = null
                    val outcome = pickReportFile(excelParser != null)
                    when (outcome) {
                        is LoadOutcome.Json -> raw = outcome.text
                        is LoadOutcome.Excel -> {
                            val result = runCatching { excelParser!!(outcome.bytes.inputStream()) }
                            result.onSuccess { raw = PRETTY_JSON.encodeToString(JsonObject.serializer(), it) }
                                .onFailure { loadError = it.message ?: "Não foi possível ler o ficheiro Excel." }
                        }
                        LoadOutcome.Cancelled -> {}
                    }
                }) {
                    Text(if (excelParser != null) "Carregar ficheiro .json ou .xlsx" else "Carregar ficheiro .json")
                }
                Spacer(Modifier.width(8.dp))
                Text("ou cole o JSON abaixo", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }

            OutlinedTextField(
                value = raw,
                onValueChange = { raw = it; loadError = null },
                modifier = Modifier.fillMaxWidth().height(280.dp),
                placeholder = { Text("Cole aqui o JSON do reporte…") },
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                keyboardOptions = KeyboardOptions.Default
            )

            if (loadError != null) {
                ValidationAlert(title = "Não foi possível carregar o ficheiro", messages = listOf(loadError!!), isError = true)
            } else if (parseFailed) {
                ValidationAlert(title = "Foram encontrados problemas no JSON", messages = listOf("O conteúdo não é um JSON válido."), isError = true)
            } else if (errors.isNotEmpty()) {
                ValidationAlert(title = "Foram encontrados problemas no JSON", messages = errors, isError = true)
            } else if (isValid) {
                ValidationAlert(title = "JSON válido e pronto a enviar.", messages = emptyList(), isError = false)
            }

            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { parsed?.let(onSubmit) },
                    enabled = isValid && !submitting
                ) {
                    if (submitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.5.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(submitLabel)
                    }
                }
            }
        }
    }
}

@Composable
private fun ValidationAlert(title: String, messages: List<String>, isError: Boolean) {
    val color = if (isError) MaterialTheme.colorScheme.error else SuccessColor
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, color = color, style = MaterialTheme.typography.labelMedium)
        messages.forEach { Text("• $it", color = color, style = MaterialTheme.typography.bodySmall) }
    }
}

private val PRETTY_JSON = Json { prettyPrint = true }

private sealed class LoadOutcome {
    data class Json(val text: String) : LoadOutcome()
    data class Excel(val bytes: ByteArray) : LoadOutcome()
    data object Cancelled : LoadOutcome()
}

private fun pickReportFile(allowExcel: Boolean): LoadOutcome {
    val chooser = JFileChooser()
    chooser.fileFilter = if (allowExcel) {
        FileNameExtensionFilter("JSON ou Excel (.json, .xlsx)", "json", "xlsx")
    } else {
        FileNameExtensionFilter("JSON", "json")
    }
    // Swing's default file chooser size (~500x300) is cramped for browsing
    // real folders — size it to most of the screen; a null parent centers
    // the resulting dialog on screen.
    val screen = Toolkit.getDefaultToolkit().screenSize
    chooser.preferredSize = Dimension((screen.width * 0.85).toInt(), (screen.height * 0.85).toInt())
    val result = chooser.showOpenDialog(null)
    if (result != JFileChooser.APPROVE_OPTION) return LoadOutcome.Cancelled
    val file: File = chooser.selectedFile ?: return LoadOutcome.Cancelled
    return if (file.extension.equals("xlsx", ignoreCase = true)) {
        runCatching { LoadOutcome.Excel(file.readBytes()) }.getOrElse { LoadOutcome.Cancelled }
    } else {
        runCatching { LoadOutcome.Json(file.readText()) }.getOrElse { LoadOutcome.Cancelled }
    }
}
