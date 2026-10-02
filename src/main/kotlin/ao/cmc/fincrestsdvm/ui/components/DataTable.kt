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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
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

/**
 * Column widths are minimums: when the available width is larger (e.g. a
 * maximized window) every column grows proportionally to fill it; when it is
 * smaller, the table keeps the minimums and scrolls horizontally. With
 * `fillHeight`, the rows take all the height the caller gives the table
 * (pass a `weight`/`fillMaxHeight` modifier) instead of capping at `maxHeight`.
 */
@Composable
fun SimpleTable(
    columns: List<ColumnSpec>,
    rows: List<JsonObject>,
    modifier: Modifier = Modifier,
    maxHeight: Dp = 360.dp,
    fillHeight: Boolean = false
) {
    val scroll = rememberScrollState()
    val minWidth = columns.sumOf { it.width.value.toDouble() }.toFloat().dp

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val scale = if (maxWidth > minWidth) maxWidth / minWidth else 1f
        val widths = columns.map { it.width * scale }
        val totalWidth = if (scale > 1f) maxWidth else minWidth

        Column(modifier = Modifier.fillMaxWidth().then(if (fillHeight) Modifier.fillMaxHeight() else Modifier).horizontalScroll(scroll)) {
            Row(
                modifier = Modifier
                    .width(totalWidth)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(vertical = 6.dp)
            ) {
                columns.forEachIndexed { index, col ->
                    Text(
                        col.header,
                        modifier = Modifier.width(widths[index]).padding(horizontal = 8.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = col.align
                    )
                }
            }
            HorizontalDivider()
            LazyColumn(
                modifier = Modifier
                    .width(totalWidth)
                    .then(if (fillHeight) Modifier.weight(1f) else Modifier.heightIn(max = maxHeight))
            ) {
                items(rows) { row ->
                    Row(modifier = Modifier.padding(vertical = 6.dp)) {
                        columns.forEachIndexed { index, col ->
                            Box(modifier = Modifier.width(widths[index]).padding(horizontal = 8.dp), contentAlignment = alignmentFor(col.align)) {
                                col.cell(row)
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }
            }
        }

        if (scale <= 1f) {
            HorizontalScrollbar(
                adapter = rememberScrollbarAdapter(scroll),
                modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(horizontal = 4.dp)
            )
        }
    }
}

private fun alignmentFor(align: TextAlign): Alignment = when (align) {
    TextAlign.End -> Alignment.CenterEnd
    TextAlign.Center -> Alignment.Center
    else -> Alignment.CenterStart
}
