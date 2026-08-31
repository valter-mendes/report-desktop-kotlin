package ao.cmc.fincrestsdvm

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import ao.cmc.fincrestsdvm.ui.AppViewModel
import ao.cmc.fincrestsdvm.ui.Screen
import ao.cmc.fincrestsdvm.ui.components.Sidebar
import ao.cmc.fincrestsdvm.ui.screens.ApuramentoTaxasAiScreen
import ao.cmc.fincrestsdvm.ui.screens.ApuramentoTaxasScreen
import ao.cmc.fincrestsdvm.ui.screens.BalancetesScreen
import ao.cmc.fincrestsdvm.ui.screens.HomeScreen
import ao.cmc.fincrestsdvm.ui.screens.LoginScreen
import ao.cmc.fincrestsdvm.ui.screens.MapasAuxiliaresScreen
import ao.cmc.fincrestsdvm.ui.theme.FincrestTheme

fun main() = application {
    val windowState = rememberWindowState(width = 1280.dp, height = 800.dp)
    Window(
        onCloseRequest = ::exitApplication,
        title = "Fincrest — Portal de Reportes SIRA / CMC",
        state = windowState,
        icon = painterResource("icons/icon.png")
    ) {
        FincrestApp()
    }
}

@Composable
fun FincrestApp() {
    val viewModel = remember { AppViewModel() }
    val snackbarHostState = remember { SnackbarHostState() }

    FincrestTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            if (!viewModel.isAuthenticated) {
                LoginScreen(viewModel)
            } else {
                Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
                    Row(modifier = Modifier.fillMaxSize().padding(padding)) {
                        Sidebar(
                            current = viewModel.currentScreen,
                            environment = viewModel.environment,
                            userEmail = viewModel.user?.email,
                            userName = viewModel.user?.name,
                            onNavigate = { viewModel.currentScreen = it },
                            onEnvironmentChange = viewModel::changeEnvironment,
                            onLogout = viewModel::signOut
                        )
                        Surface(modifier = Modifier.fillMaxSize()) {
                            when (viewModel.currentScreen) {
                                Screen.HOME -> HomeScreen(viewModel.user) { viewModel.currentScreen = it }
                                Screen.MAPAS_AUXILIARES -> MapasAuxiliaresScreen(viewModel, snackbarHostState)
                                Screen.BALANCETES -> BalancetesScreen(viewModel, snackbarHostState)
                                Screen.APURAMENTO_TAXAS -> ApuramentoTaxasScreen(viewModel, snackbarHostState)
                                Screen.APURAMENTO_TAXAS_AI -> ApuramentoTaxasAiScreen(viewModel, snackbarHostState)
                            }
                        }
                    }
                }
            }
        }
    }
}
