package br.com.studyou.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.studyou.ui.components.PrimaryButton
import br.com.studyou.ui.di.LocalAppContainer
import androidx.compose.runtime.collectAsState

@Composable
fun LoginRoute(onAuthenticated: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: LoginViewModel = viewModel(
        factory = viewModelFactory {
            initializer { LoginViewModel(container.authRepository) }
        }
    )
    val uiState by viewModel.uiState.collectAsState()

    LoginScreen(
        uiState = uiState,
        onModeSelected = viewModel::onModeSelected,
        onNameChange = viewModel::onNameChange,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onSubmit = { viewModel.submit(onAuthenticated) }
    )
}

@Composable
fun LoginScreen(
    uiState: LoginUiState,
    onModeSelected: (AuthMode) -> Unit,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .padding(vertical = 28.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            Text(
                text = "StudYou",
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                text = "Transforme seus estudos em jogo",
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Takes all the space left after the header above, instead of a fixed weight ratio
        // against it — so the extra "Nome" field in REGISTER mode (and the keyboard, once a
        // field is focused) don't squeeze the header and form into competing fixed shares.
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                AuthModeTabs(mode = uiState.mode, onModeSelected = onModeSelected)

                Spacer(20.dp)

                if (uiState.mode == AuthMode.REGISTER) {
                    Text("Nome", style = MaterialTheme.typography.labelLarge)
                    Spacer(4.dp)
                    OutlinedTextField(
                        value = uiState.name,
                        onValueChange = onNameChange,
                        placeholder = { Text("Seu nome") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(16.dp)
                }

                Text("E-mail", style = MaterialTheme.typography.labelLarge)
                Spacer(4.dp)
                OutlinedTextField(
                    value = uiState.email,
                    onValueChange = onEmailChange,
                    placeholder = { Text("voce@email.com") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(16.dp)

                Text("Senha", style = MaterialTheme.typography.labelLarge)
                Spacer(4.dp)
                OutlinedTextField(
                    value = uiState.password,
                    onValueChange = onPasswordChange,
                    placeholder = {
                        Text(if (uiState.mode == AuthMode.REGISTER) "Mínimo de 6 caracteres" else "Sua senha")
                    },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )

                uiState.errorMessage?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(20.dp)

                PrimaryButton(
                    text = if (uiState.mode == AuthMode.LOGIN) "Entrar" else "Criar conta",
                    onClick = onSubmit,
                    enabled = !uiState.isLoading
                )

                Spacer(16.dp)

                val (question, action, targetMode) = if (uiState.mode == AuthMode.LOGIN) {
                    Triple("Não tem conta?", "Criar conta", AuthMode.REGISTER)
                } else {
                    Triple("Já tem conta?", "Entrar", AuthMode.LOGIN)
                }
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(question, style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = { onModeSelected(targetMode) }, contentPadding = PaddingValues(horizontal = 6.dp)) {
                        Text(action, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthModeTabs(mode: AuthMode, onModeSelected: (AuthMode) -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            AuthModeTab(
                text = "Entrar",
                selected = mode == AuthMode.LOGIN,
                onClick = { onModeSelected(AuthMode.LOGIN) },
                modifier = Modifier.weight(1f)
            )
            AuthModeTab(
                text = "Criar conta",
                selected = mode == AuthMode.REGISTER,
                onClick = { onModeSelected(AuthMode.REGISTER) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AuthModeTab(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val containerColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentColor),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
        modifier = modifier
    ) {
        Text(text, textAlign = TextAlign.Center, maxLines = 1, softWrap = false, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Spacer(height: androidx.compose.ui.unit.Dp) {
    Box(modifier = Modifier.height(height))
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    br.com.studyou.ui.theme.StudyouTheme {
        LoginScreen(
            uiState = LoginUiState(email = "voce@email.com", password = "123456"),
            onModeSelected = {},
            onNameChange = {},
            onEmailChange = {},
            onPasswordChange = {},
            onSubmit = {}
        )
    }
}
