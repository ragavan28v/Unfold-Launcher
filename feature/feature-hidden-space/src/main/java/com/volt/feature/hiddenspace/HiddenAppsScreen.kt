package com.volt.feature.hiddenspace

import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.volt.core.ui.theme.LocalVoltTheme
import java.util.concurrent.Executors

@Composable
fun HiddenAppsScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    val theme = LocalVoltTheme.current
    val context = LocalContext.current
    var isAuthenticated by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val executor = Executors.newSingleThreadExecutor()
        val activity = context as? FragmentActivity
        if (activity != null) {
            val biometricPrompt = BiometricPrompt(activity, executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        isAuthenticated = true
                    }
                })

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("UNFOLD SECURE STORAGE")
                .setSubtitle("Authenticate to access hidden systems")
                .setNegativeButtonText("CANCEL")
                .build()

            biometricPrompt.authenticate(promptInfo)
        } else {
            // Fallback for previews/non-activity contexts
            isAuthenticated = true
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(theme.bgVoid),
        contentAlignment = Alignment.Center
    ) {
        if (isAuthenticated) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "SECURE CABINET ACTIVE",
                    color = theme.accentPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onBack) {
                    Text("CLOSE CABINET")
                }
            }
        } else {
            Text(
                text = "AUTHENTICATION REQUIRED",
                color = theme.accentDanger,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
