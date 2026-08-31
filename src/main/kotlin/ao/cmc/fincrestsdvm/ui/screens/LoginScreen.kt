package ao.cmc.fincrestsdvm.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import ao.cmc.fincrestsdvm.ui.AppViewModel
import ao.cmc.fincrestsdvm.ui.util.SvgLogo
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(viewModel: AppViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.widthIn(max = 380.dp).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SvgLogo(
                resourcePath = "/branding/fincrest-logo-vertical.svg",
                width = 220.dp,
                height = 113.dp,
                contentDescription = "Fincrest"
            )
            Spacer(Modifier.height(16.dp))
            Text("Entrar", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Autentique-se com as credenciais da sua entidade supervisionada para aceder ao SIRA.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("E-mail") },
                placeholder = { Text("utilizador@exemplo.com") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.width(340.dp)
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Palavra-passe") },
                placeholder = { Text("Introduza a sua palavra-passe") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.width(340.dp)
            )

            if (viewModel.authError != null) {
                Spacer(Modifier.height(8.dp))
                Text(viewModel.authError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            } else if (viewModel.sessionExpiredNotice != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    viewModel.sessionExpiredNotice!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { scope.launch { viewModel.signIn(email.trim(), password) } },
                enabled = !viewModel.authenticating && email.isNotBlank() && password.isNotBlank(),
                modifier = Modifier.width(340.dp)
            ) {
                if (viewModel.authenticating) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp).width(20.dp),
                        strokeWidth = 2.5.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Entrar")
                }
            }
        }
    }
}
