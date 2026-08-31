package ao.cmc.fincrestsdvm.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.JsonObject

data class ColumnSpec(
    val header: String,
    val width: Dp = 140.dp,
    val align: TextAlign = TextAlign.Start,
    val cell: @Composable (JsonObject) -> Unit
)

@Composable
fun SimpleTable(columns: List<ColumnSpec>, rows: List<JsonObject>, maxHeight: Dp = 360.dp) {
    val scroll = rememberScrollState()
    val totalWidth = columns.sumOf { it.width.value.toInt() }.dp

    Box(modifier = Modifier.fillMaxWidth()){

        Column(modifier = Modifier.fillMaxWidth().horizontalScroll(scroll)) {
            Row(
                modifier = Modifier
                    .width(totalWidth)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(vertical = 6.dp)
            ) {
                columns.forEach { col ->
                    Text(
                        col.header,
                        modifier = Modifier.width(col.width).padding(horizontal = 8.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = col.align
                    )
                }
            }
            HorizontalDivider()
            LazyColumn(modifier = Modifier.width(totalWidth).heightIn(max = maxHeight)) {
                items(rows) { row ->
                    Row(modifier = Modifier.padding(vertical = 6.dp)) {
                        columns.forEach { col ->
                            Box(modifier = Modifier.width(col.width).padding(horizontal = 8.dp), contentAlignment = alignmentFor(col.align)) {
                                col.cell(row)
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }
            }
        }

        HorizontalScrollbar(
        adapter = rememberScrollbarAdapter(scroll),
        modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(horizontal = 4.dp)
        )
    }

}

private fun alignmentFor(align: TextAlign): Alignment = when (align) {
    TextAlign.End -> Alignment.CenterEnd
    TextAlign.Center -> Alignment.Center
    else -> Alignment.CenterStart
}
