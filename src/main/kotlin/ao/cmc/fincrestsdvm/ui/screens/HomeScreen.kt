package ao.cmc.fincrestsdvm.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import ao.cmc.fincrestsdvm.data.models.SiraUser
import ao.cmc.fincrestsdvm.ui.Screen

private data class HomeSection(val screen: Screen, val icon: ImageVector, val description: String)

private val sections = listOf(
    HomeSection(
        Screen.MAPAS_AUXILIARES,
        Icons.Default.Group,
        "Participantes, activos financeiros e activos imobiliários dos OIC."
    ),
    HomeSection(
        Screen.BALANCETES,
        Icons.Default.Balance,
        "Balancetes financeiros mensais das entidades supervisionadas."
    ),
    HomeSection(
        Screen.APURAMENTO_TAXAS,
        Icons.Default.Percent,
        "Apuramento trimestral da taxa de supervisão por tipo de instrumento (OIC)."
    ),
    HomeSection(
        Screen.APURAMENTO_TAXAS_AI,
        Icons.Default.TrendingUp,
        "Apuramento mensal da taxa de supervisão por instrumento (ISIN) dos Agentes de Intermediação."
    )
)

@Composable
fun HomeScreen(user: SiraUser?, onNavigate: (Screen) -> Unit) {
    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("Portal de Reportes SIRA", style = MaterialTheme.typography.headlineSmall)

        Card(modifier = Modifier.fillMaxWidth()) {
            Text(
                buildString {
                    append("Sessão autenticada como ")
                    append(user?.name ?: "")
                    append(" (")
                    append(user?.email ?: "")
                    append(")")
                },
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodySmall
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 240.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(sections) { section ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate(section.screen) }
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(section.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                section.screen.label,
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(0.dp))
                            )
                        }
                        Text(section.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }
}
