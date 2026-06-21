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
import androidx.compose.runtime.LaunchedEffect
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

/** Carries an invite code captured from an `invpn://i/<code>` deep link into the auth screen. */
object InviteLinkBus {
    var pendingCode by mutableStateOf<String?>(null)
}

/** Accept a full link (`https://…/i/<code>` or `invpn://i/<code>`) or a bare code. */
fun extractInviteCode(input: String): String {
    val s = input.trim()
    val i = s.lastIndexOf("/i/")
    val raw = if (i >= 0) s.substring(i + 3) else s
    return raw.substringBefore('?').substringBefore('#').trim()
}

class AuthViewModel : ViewModel() {
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)

    fun redeem(codeOrLink: String, onDone: () -> Unit) =
        perform(onDone) { AuthRepository.redeemInvite(extractInviteCode(codeOrLink)) }

    fun login(username: String, password: String, onDone: () -> Unit) =
        perform(onDone) { AuthRepository.login(username.trim(), password) }

    private fun perform(onDone: () -> Unit, block: suspend () -> Unit) {
        if (loading) return
        loading = true
        error = null
        viewModelScope.launch {
            try {
                block()
                // Pull + install the per-user config; best-effort (retry from Connect if it fails).
                runCatching { ConfigInstaller.refreshAndInstall() }
                onDone()
            } catch (e: ApiException) {
                error = e.message
            } catch (e: Exception) {
                error = e.message ?: "Ошибка сети"
            } finally {
                loading = false
            }
        }
    }
}

@Composable
fun AuthGate(content: @Composable () -> Unit) {
    var authenticated by remember { mutableStateOf(AuthRepository.isAuthenticated()) }
    if (authenticated) content() else LoginScreen(onAuthenticated = { authenticated = true })
}

@Composable
fun LoginScreen(
    onAuthenticated: () -> Unit,
    vm: AuthViewModel = viewModel(),
) {
    var useLogin by remember { mutableStateOf(false) }
    var invite by remember { mutableStateOf(InviteLinkBus.pendingCode ?: "") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // A deep link arriving while this screen is open prefills the invite field.
    LaunchedEffect(InviteLinkBus.pendingCode) {
        InviteLinkBus.pendingCode?.let { invite = it; useLogin = false }
    }

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
                text = if (useLogin) "Вход по логину и паролю" else "Активация по ссылке-приглашению",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))

            if (useLogin) {
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
            } else {
                OutlinedTextField(
                    value = invite,
                    onValueChange = { invite = it },
                    label = { Text("Ссылка или код приглашения") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

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
                onClick = {
                    if (useLogin) {
                        vm.login(username, password) { InviteLinkBus.pendingCode = null; onAuthenticated() }
                    } else {
                        vm.redeem(invite) { InviteLinkBus.pendingCode = null; onAuthenticated() }
                    }
                },
                enabled = !vm.loading &&
                    if (useLogin) username.isNotBlank() && password.length >= 8 else invite.isNotBlank(),
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
                    Text(if (useLogin) "Войти" else "Активировать")
                }
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { useLogin = !useLogin; vm.error = null }) {
                Text(if (useLogin) "У меня есть ссылка-приглашение" else "Войти по логину и паролю")
            }
        }
    }
}
