package ao.cmc.fincrestsdvm.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ao.cmc.fincrestsdvm.ui.theme.SuccessColor
import ao.cmc.fincrestsdvm.ui.theme.WarningColor

@Composable
fun StatusBadge(status: String?) {
    if (status.isNullOrBlank()) {
        Text("—", color = MaterialTheme.colorScheme.outline, fontSize = 12.sp)
        return
    }
    val color = when {
        status.contains("PENALIDADE") -> WarningColor
        status == "SUBMETIDO" || status == "ACTUALIZADO" -> SuccessColor
        else -> MaterialTheme.colorScheme.outline
    }
    Text(
        text = status,
        color = color,
        fontSize = 11.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}
