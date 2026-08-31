package ao.cmc.fincrestsdvm.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import ao.cmc.fincrestsdvm.data.SiraEnvironment
import ao.cmc.fincrestsdvm.ui.Screen
import ao.cmc.fincrestsdvm.ui.theme.WarningColor
import ao.cmc.fincrestsdvm.ui.util.SvgLogo

private fun iconFor(screen: Screen): ImageVector = when (screen) {
    Screen.HOME -> Icons.Default.Home
    Screen.MAPAS_AUXILIARES -> Icons.Default.Group
    Screen.BALANCETES -> Icons.Default.Balance
    Screen.APURAMENTO_TAXAS -> Icons.Default.Percent
    Screen.APURAMENTO_TAXAS_AI -> Icons.Default.TrendingUp
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Sidebar(
    current: Screen,
    environment: SiraEnvironment,
    userEmail: String?,
    userName: String?,
    onNavigate: (Screen) -> Unit,
    onEnvironmentChange: (SiraEnvironment) -> Unit,
    onLogout: () -> Unit
) {
    Surface(
        modifier = Modifier.width(260.dp).fillMaxHeight(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
    ) {
        Column(modifier = Modifier.fillMaxHeight().padding(12.dp)) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 8.dp), contentAlignment = Alignment.CenterStart) {
                SvgLogo(
                    resourcePath = "/branding/fincrest-logo-horizontal.svg",
                    width = 180.dp,
                    height = 40.dp,
                    contentDescription = "Fincrest"
                )
            }

            Spacer(Modifier.height(12.dp))

            Screen.entries.forEach { screen ->
                val selected = screen == current
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate(screen) }
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else androidx.compose.ui.graphics.Color.Transparent,
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 10.dp)
                ) {
                    Icon(
                        iconFor(screen),
                        contentDescription = null,
                        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.height(14.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        screen.label,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(Modifier.weight(1f))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            EnvironmentSwitcher(environment, onEnvironmentChange)

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ){
                Text(
                    userName ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    userEmail ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1
                )
                
                IconButton(onClick = onLogout) {
                    Icon(Icons.Default.Logout, contentDescription = "Sair")
                   
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnvironmentSwitcher(environment: SiraEnvironment, onChange: (SiraEnvironment) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val isProd = environment == SiraEnvironment.PRODUCAO

    Surface(
        color = if (isProd) WarningColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.fillMaxWidth().clickable { expanded = true }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (isProd) Icons.Default.Shield else Icons.Default.Science,
                    contentDescription = null,
                    tint = if (isProd) WarningColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.height(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(environment.label, style = MaterialTheme.typography.labelMedium)
            }
            Icon(Icons.Default.ExpandMore, contentDescription = null, modifier = Modifier.height(16.dp))
        }
    }

    androidx.compose.material3.DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        SiraEnvironment.entries.forEach { env ->
            DropdownMenuItem(text = { Text(env.label) }, onClick = {
                onChange(env)
                expanded = false
            })
        }
    }
}
