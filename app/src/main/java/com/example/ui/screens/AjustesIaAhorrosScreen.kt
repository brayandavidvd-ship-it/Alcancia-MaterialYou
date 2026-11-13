package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.service.GeminiService
import com.example.ui.theme.LocalIsCompactDensity
import com.example.ui.viewmodel.AlcanciaViewModel
import kotlinx.coroutines.launch

@Composable
fun AjustesIaAhorrosScreen(
    viewModel: AlcanciaViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val settings by viewModel.settingsState.collectAsStateWithLifecycle()
    val palette = resolveInicioPalette(settings)
    val isCompact = LocalIsCompactDensity.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var cloudModelInput by remember(settings.cloudAiModel) {
        mutableStateOf(settings.cloudAiModel)
    }
    var apiKeyInput by remember {
        mutableStateOf(viewModel.getGeminiApiKey())
    }
    var showApiKey by remember { mutableStateOf(false) }
    var showConsentDialog by remember { mutableStateOf(false) }

    var isTestingCloudConnection by remember { mutableStateOf(false) }
    var cloudSuccessMessage by remember { mutableStateOf<String?>(null) }
    var cloudErrorMessage by remember { mutableStateOf<String?>(null) }

    fun saveCloudConfig(
        enabled: Boolean = settings.isCloudAiEnabled,
        consentAccepted: Boolean = settings.cloudAiConsentAccepted
    ) {
        viewModel.saveGeminiApiKey(apiKeyInput)
        viewModel.setCloudAiConfig(
            enabled = enabled,
            consentAccepted = consentAccepted,
            model = cloudModelInput
        )
    }

    if (showConsentDialog) {
        AlertDialog(
            onDismissRequest = { showConsentDialog = false },
            containerColor = palette.s1,
            titleContentColor = palette.t1,
            textContentColor = palette.t2,
            icon = {
                Icon(
                    imageVector = Icons.Default.Cloud,
                    contentDescription = null,
                    tint = palette.ahorro,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Aviso de IA en la nube",
                    fontWeight = FontWeight.Bold,
                    color = palette.t1
                )
            },
            text = {
                Text(
                    text = "Para responderte, los mensajes que escribas y los datos de la app necesarios para tu pregunta se enviarán a la nube de Google. Activa esto solo si estás de acuerdo.",
                    fontSize = 14.sp,
                    color = palette.t2,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConsentDialog = false
                        saveCloudConfig(enabled = true, consentAccepted = true)
                        scope.launch {
                            snackbarHostState.showSnackbar("IA en la nube activada. Se mostrará el botón ✨ en tus pantallas principales.")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.brand,
                        contentColor = palette.onBrand
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showConsentDialog = false
                        saveCloudConfig(enabled = false, consentAccepted = false)
                    }
                ) {
                    Text("Cancelar", color = palette.t2)
                }
            }
        )
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(palette.bg),
        containerColor = palette.bg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = 92.dp)
            )
        },
        topBar = {
            AjustesSubScreenTopBar(
                title = "IA de ahorros",
                subtitle = "Asistente en la nube (Gemini)",
                palette = palette,
                onBack = onBack
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = if (isCompact) 14.dp else 20.dp,
                end = if (isCompact) 14.dp else 20.dp,
                top = 8.dp,
                bottom = 124.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = null,
                        tint = palette.ahorro,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "IA EN LA NUBE (GEMINI)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.ahorro,
                        letterSpacing = 1.1.sp
                    )
                }
            }

            // Switch "Usar IA en la nube"
            item {
                AjustesPaletteCard(palette = palette) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AjustesIconBadge(
                                icon = Icons.Default.AutoAwesome,
                                containerColor = palette.s2,
                                iconTint = palette.ahorro,
                                size = 44.dp,
                                iconSize = 22.dp
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Usar IA en la nube",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.t1
                                )
                                Text(
                                    text = if (settings.isCloudAiEnabled && settings.cloudAiConsentAccepted) {
                                        "Asistente Gemini activo"
                                    } else {
                                        "Apagado por defecto"
                                    },
                                    fontSize = 13.sp,
                                    color = palette.t2
                                )
                            }
                        }

                        Switch(
                            checked = settings.isCloudAiEnabled && settings.cloudAiConsentAccepted,
                            colors = ajustesSwitchColors(palette),
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
                                    showConsentDialog = true
                                } else {
                                    saveCloudConfig(enabled = false, consentAccepted = false)
                                    scope.launch {
                                        snackbarHostState.showSnackbar("IA en la nube desactivada.")
                                    }
                                }
                            },
                            modifier = Modifier.testTag("switch_cloud_ai")
                        )
                    }
                }
            }

            // Credenciales, modelo y prueba de conexión
            item {
                AjustesPaletteCard(palette = palette) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AjustesIconBadge(
                                icon = Icons.Default.Key,
                                containerColor = palette.s2,
                                iconTint = palette.accent,
                                size = 44.dp,
                                iconSize = 22.dp
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Credenciales y modelo de Gemini",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.t1
                                )
                                Text(
                                    text = "Almacenamiento seguro cifrado en el dispositivo",
                                    fontSize = 13.sp,
                                    color = palette.t2
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        OutlinedTextField(
                            value = apiKeyInput,
                            onValueChange = {
                                apiKeyInput = it
                                viewModel.saveGeminiApiKey(it)
                            },
                            label = { Text("Clave de API de Gemini") },
                            placeholder = { Text("AIzaSy...") },
                            singleLine = true,
                            visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            colors = ajustesTextFieldColors(palette),
                            trailingIcon = {
                                IconButton(onClick = { showApiKey = !showApiKey }) {
                                    Icon(
                                        imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (showApiKey) "Ocultar clave" else "Mostrar clave"
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("gemini_api_key_field"),
                            shape = RoundedCornerShape(14.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    try {
                                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
                                        context.startActivity(browserIntent)
                                    } catch (_: Exception) { }
                                }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = null,
                                tint = palette.accent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Consigue una clave gratis en aistudio.google.com",
                                fontSize = 13.sp,
                                color = palette.accent,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = cloudModelInput,
                            onValueChange = {
                                cloudModelInput = it
                                saveCloudConfig()
                            },
                            label = { Text("Modelo") },
                            placeholder = { Text(GeminiService.DEFAULT_MODEL) },
                            singleLine = true,
                            colors = ajustesTextFieldColors(palette),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("gemini_model_field"),
                            shape = RoundedCornerShape(14.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val chipColors = ajustesChipColors(palette)
                            FilterChip(
                                selected = cloudModelInput == GeminiService.DEFAULT_MODEL,
                                onClick = {
                                    cloudModelInput = GeminiService.DEFAULT_MODEL
                                    saveCloudConfig()
                                },
                                label = { Text("3.5 Flash-Lite", fontSize = 12.sp) },
                                colors = chipColors,
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = cloudModelInput == GeminiService.DEFAULT_MODEL,
                                    borderColor = palette.s3,
                                    selectedBorderColor = palette.accent
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                            FilterChip(
                                selected = cloudModelInput == GeminiService.FLASH_MODEL,
                                onClick = {
                                    cloudModelInput = GeminiService.FLASH_MODEL
                                    saveCloudConfig()
                                },
                                label = { Text("3.5 Flash", fontSize = 12.sp) },
                                colors = chipColors,
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = cloudModelInput == GeminiService.FLASH_MODEL,
                                    borderColor = palette.s3,
                                    selectedBorderColor = palette.accent
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                isTestingCloudConnection = true
                                cloudSuccessMessage = null
                                cloudErrorMessage = null
                                saveCloudConfig()
                                viewModel.testGeminiConnection(apiKeyInput, cloudModelInput) { result ->
                                    isTestingCloudConnection = false
                                    result.onSuccess { msg ->
                                        cloudSuccessMessage = msg
                                        cloudErrorMessage = null
                                    }.onFailure { err ->
                                        cloudErrorMessage = err.message ?: "Error al conectar con Gemini."
                                        cloudSuccessMessage = null
                                    }
                                }
                            },
                            enabled = !isTestingCloudConnection,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.brand,
                                contentColor = palette.onBrand
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("test_gemini_button")
                        ) {
                            if (isTestingCloudConnection) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = palette.onBrand,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Probando conexión...")
                            } else {
                                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Probar conexión", fontWeight = FontWeight.SemiBold)
                            }
                        }

                        if (cloudSuccessMessage != null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = palette.ingreso.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, palette.ingreso.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CloudDone,
                                        contentDescription = null,
                                        tint = palette.ingreso,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = cloudSuccessMessage!!,
                                        fontSize = 13.sp,
                                        color = palette.t1,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        if (cloudErrorMessage != null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = palette.gasto.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, palette.gasto.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Error,
                                        contentDescription = null,
                                        tint = palette.gasto,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = cloudErrorMessage!!,
                                        fontSize = 13.sp,
                                        color = palette.t1,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                AjustesPaletteCard(
                    palette = palette,
                    containerColor = palette.s2,
                    borderColor = palette.s3
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = palette.t2,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Tu clave de API se guarda cifrada localmente en este dispositivo y solo se usa para consultar a Gemini cuando la IA está activada.",
                            fontSize = 12.sp,
                            color = palette.t2,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}
