package io.nekohasekai.sfa.compose.screen.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import io.nekohasekai.sfa.invpn.AuthRepository
import kotlinx.coroutines.launch

/** BRILLIANT-only: pick a recipient name, mint a GOLDEN invite link, copy/share it. */
@Composable
fun CreateInviteDialog(onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var link by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Создать приглашение", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                if (link == null) {
                    Text(
                        "Уровень: GOLDEN (безлимит)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Имя получателя") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    error?.let {
                        Spacer(Modifier.height(10.dp))
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    Text("Ссылка для «$name»:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(link!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Одноразовая, 30 дней. Отправьте получателю — он вставит её в приложение.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            if (link == null) {
                TextButton(
                    enabled = !loading && name.isNotBlank(),
                    onClick = {
                        loading = true
                        error = null
                        scope.launch {
                            try {
                                link = AuthRepository.createInvite(name.trim(), "GOLDEN")
                            } catch (e: Exception) {
                                error = e.message ?: "Не удалось создать"
                            } finally {
                                loading = false
                            }
                        }
                    },
                ) {
                    if (loading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Создать")
                    }
                }
            } else {
                TextButton(onClick = { clipboard.setText(AnnotatedString(link!!)) }) { Text("Копировать") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (link == null) "Отмена" else "Готово") }
        },
    )
}
