package io.nekohasekai.sfa.compose.screen.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import io.nekohasekai.sfa.invpn.ApiException
import io.nekohasekai.sfa.invpn.AuthRepository
import io.nekohasekai.sfa.invpn.ConfigInstaller
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)

    fun submit(register: Boolean, invite: String, username: String, password: String, onDone: () -> Unit) {
        if (loading) return
        loading = true
        error = null
        viewModelScope.launch {
            try {
                if (register) {
                    AuthRepository.register(invite.trim(), username.trim(), password)
                } else {
                    AuthRepository.login(username.trim(), password)
                }
                // Pull + install the per-user config. Best-effort: if the backend is
                // unreachable the user is still authenticated and can retry from Connect.
                runCatching { ConfigInstaller.refreshAndInstall() }
                onDone()
            } catch (e: ApiException) {
                error = e.message
            } catch (e: Exception) {
                error = e.message ?: "Не удалось подключиться"
            } finally {
                loading = false
            }
        }
    }
}

/** Shows the auth screen until credentials exist, then the real app [content]. */
@Composable
fun AuthGate(content: @Composable () -> Unit) {
    var authenticated by remember { mutableStateOf(AuthRepository.isAuthenticated()) }
    if (authenticated) {
        content()
    } else {
        LoginRegisterScreen(onAuthenticated = { authenticated = true })
    }
}

@Composable
fun LoginRegisterScreen(
    onAuthenticated: () -> Unit,
    vm: AuthViewModel = viewModel(),
) {
    var register by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var invite by remember { mutableStateOf("") }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 28.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "INVPN",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (register) "Регистрация по коду приглашения" else "Вход",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))

            if (register) {
                OutlinedTextField(
                    value = invite,
                    onValueChange = { invite = it },
                    label = { Text("Код приглашения") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
            }
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Логин") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Пароль") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )

            vm.error?.let {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(28.dp))
            Button(
                onClick = { vm.submit(register, invite, username, password, onAuthenticated) },
                enabled = !vm.loading &&
                    username.isNotBlank() &&
                    password.length >= 8 &&
                    (!register || invite.isNotBlank()),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                if (vm.loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(if (register) "Зарегистрироваться" else "Войти")
                }
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { register = !register; vm.error = null }) {
                Text(if (register) "У меня уже есть аккаунт" else "Есть код приглашения — зарегистрироваться")
            }
        }
    }
}
