package ao.cmc.fincrestsdvm.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

enum class OicTab(val label: String, val icon: ImageVector) {
    CONSULTAR("Consultar", Icons.Default.Search),
    SUBMETER("Submeter", Icons.Default.Upload),
    ACTUALIZAR("Actualizar", Icons.Default.Edit)
}

@Composable
fun OicTabRow(current: OicTab, onSelect: (OicTab) -> Unit) {
    Row {
        OicTab.entries.forEach { tab ->
            val selected = tab == current
            TextButton(onClick = { onSelect(tab) }) {
                Icon(
                    tab.icon,
                    contentDescription = null,
                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    tab.label,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
