package io.nekohasekai.sfa.compose.screen.auth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
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
    val authenticated by AuthRepository.authState.collectAsState()
    if (authenticated) content() else LoginScreen(onAuthenticated = { AuthRepository.refreshAuthState() })
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        LogoMark()

        Spacer(Modifier.height(28.dp))
        Text(
            text = "INVPN",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = if (useLogin) {
                "Вход по логину и паролю."
            } else {
                "Тихий и быстрый VPN. Войдите, чтобы начать."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(40.dp))

        if (useLogin) {
            MinimalField(
                value = username,
                onValueChange = { username = it },
                label = "Логин",
            )
            Spacer(Modifier.height(18.dp))
            MinimalField(
                value = password,
                onValueChange = { password = it },
                label = "Пароль",
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            )
        } else {
            MinimalField(
                value = invite,
                onValueChange = { invite = it },
                label = "Ссылка или код приглашения",
            )
        }

        vm.error?.let {
            Spacer(Modifier.height(18.dp))
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(36.dp))
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
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
        ) {
            if (vm.loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text(
                    text = if (useLogin) "Войти" else "Активировать",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        TextButton(onClick = { useLogin = !useLogin; vm.error = null }) {
            Text(
                text = if (useLogin) "У меня есть ссылка-приглашение" else "Войти по логину и паролю",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** A simple outlined circle with a short vertical tick — the InVPN mark. */
@Composable
private fun LogoMark() {
    val ink = MaterialTheme.colorScheme.onBackground
    Box(modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(56.dp)) {
            val sw = 2.dp.toPx()
            val d = size.minDimension - sw
            drawCircle(color = ink, radius = d / 2f, style = Stroke(sw))
            val cx = size.width / 2f
            drawLine(
                color = ink,
                start = Offset(cx, size.height * 0.26f),
                end = Offset(cx, size.height * 0.62f),
                strokeWidth = sw,
                cap = StrokeCap.Round,
            )
        }
    }
}

/** Borderless, underline-style input: transparent fill, a single hairline at the bottom. */
@Composable
private fun MinimalField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation =
        androidx.compose.ui.text.input.VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        textStyle = MaterialTheme.typography.bodyLarge,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            errorContainerColor = Color.Transparent,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
            unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedTextColor = MaterialTheme.colorScheme.onBackground,
            unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}
