package ao.cmc.fincrestsdvm.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate

enum class PeriodType { MES, TRIMESTRE }

@Composable
fun PeriodFilterBar(
    periodType: PeriodType,
    loading: Boolean,
    showNif: Boolean = true,
    onConsultar: (ano: Int, periodo: Int, nif: String?) -> Unit
) {
    val now = LocalDate.now()
    var ano by remember { mutableStateOf(now.year.toString()) }
    var periodo by remember {
        mutableStateOf(if (periodType == PeriodType.MES) now.monthValue else (now.monthValue - 1) / 3 + 1)
    }
    var nif by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val periodOptions = if (periodType == PeriodType.MES) {
        (1..12).map { it to it.toString().padStart(2, '0') }
    } else {
        (1..4).map { it to "${it}º trimestre" }
    }

    Row(verticalAlignment = Alignment.Bottom) {
        OutlinedTextField(
            value = ano,
            onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) ano = it },
            label = { Text("Ano") },
            modifier = Modifier.width(110.dp)
        )
        Spacer(Modifier.width(12.dp))

        Box {
            OutlinedTextField(
                value = periodOptions.first { it.first == periodo }.second,
                onValueChange = {},
                readOnly = true,
                label = { Text(if (periodType == PeriodType.MES) "Mês" else "Trimestre") },
                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                modifier = Modifier.width(165.dp)
            )
            // A read-only OutlinedTextField swallows tap events for its own
            // focus handling, so an outer Modifier.clickable only fired on
            // keyboard activation (Enter). This transparent overlay sits on
            // top of the whole field and captures the click instead.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { expanded = true }
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                periodOptions.forEach { (value, label) ->
                    DropdownMenuItem(text = { Text(label) }, onClick = {
                        periodo = value
                        expanded = false
                    })
                }
            }
        }
        Spacer(Modifier.width(12.dp))

        if (showNif) {
            OutlinedTextField(
                value = nif,
                onValueChange = { nif = it },
                label = { Text("NIF (opcional)") },
                placeholder = { Text("Filtrar por NIF") },
                modifier = Modifier.width(180.dp)
            )
            Spacer(Modifier.width(12.dp))
        }

        Button(
            onClick = { ano.toIntOrNull()?.let { onConsultar(it, periodo, nif.ifBlank { null }) } },
            enabled = !loading
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.5.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Consultar")
            }
        }
    }
}
