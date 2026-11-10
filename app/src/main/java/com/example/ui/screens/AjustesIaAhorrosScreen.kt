package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.ui.components.AlcanciaCard
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
    val isCompact = LocalIsCompactDensity.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Cloud AI (Gemini) states
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

    // Local AI (Ollama) states
    var localServerUrlInput by remember(settings.localAiServerUrl) {
        mutableStateOf(settings.localAiServerUrl)
    }
    var localModelInput by remember(settings.localAiModel) {
        mutableStateOf(settings.localAiModel)
    }

    var isTestingLocalConnection by remember { mutableStateOf(false) }
    var localSuccessMessage by remember { mutableStateOf<String?>(null) }
    var localErrorMessage by remember { mutableStateOf<String?>(null) }

    fun saveLocalConfig(enabled: Boolean = settings.isLocalAiEnabled) {
        viewModel.setLocalAiConfig(
            enabled = enabled,
            serverUrl = localServerUrlInput,
            model = localModelInput
        )
    }

    fun saveCloudConfig(enabled: Boolean = settings.isCloudAiEnabled, consentAccepted: Boolean = settings.cloudAiConsentAccepted) {
        viewModel.saveGeminiApiKey(apiKeyInput)
        viewModel.setCloudAiConfig(
            enabled = enabled,
            consentAccepted = consentAccepted,
            model = cloudModelInput
        )
    }

    // Consent Dialog (Rule 2)
    if (showConsentDialog) {
        AlertDialog(
            onDismissRequest = {
                showConsentDialog = false
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Cloud,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Aviso de IA en la nube",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Para responderte, los mensajes que escribas y los datos de la app necesarios para tu pregunta se enviarán a la nube de Google. Activa esto solo si estás de acuerdo.",
                    style = MaterialTheme.typography.bodyMedium,
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
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showConsentDialog = false
                        // User cancelled: switch stays off (Rule 2)
                        saveCloudConfig(enabled = false, consentAccepted = false)
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Regresar",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "IA de ahorros",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Asistente en la nube e IA local",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
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
            // ==============================================================
            // BLOQUE 1: IA EN LA NUBE (GEMINI) - PRIMERO (Regla 1)
            // ==============================================================
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "IA en la nube (Gemini)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Card A1: Switch "Usar IA en la nube"
            item {
                AlcanciaCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = "Usar IA en la nube",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (settings.isCloudAiEnabled && settings.cloudAiConsentAccepted) "Asistente Gemini activo" else "Apagado por defecto",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Switch(
                                checked = settings.isCloudAiEnabled && settings.cloudAiConsentAccepted,
                                onCheckedChange = { isChecked ->
                                    if (isChecked) {
                                        showConsentDialog = true
                                    } else {
                                        // When turning off, acceptance is forgotten (Rule 2)
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
            }

            // Card A2: Cloud API Key, Model, Test & Link
            item {
                AlcanciaCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Key,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Credenciales y modelo de Gemini",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Almacenamiento seguro cifrado en el dispositivo",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Campo "Clave de API" (oculta con botón Mostrar)
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
                            trailingIcon = {
                                IconButton(onClick = { showApiKey = !showApiKey }) {
                                    Icon(
                                        imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (showApiKey) "Ocultar clave" else "Mostrar clave"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("gemini_api_key_field"),
                            shape = RoundedCornerShape(14.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Enlace clickable "Consigue una clave gratis en aistudio.google.com"
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    try {
                                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
                                        context.startActivity(browserIntent)
                                    } catch (_: Exception) {}
                                }
                                .padding(vertical = 4.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Consigue una clave gratis en aistudio.google.com",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Campo "Modelo" (editable, por defecto Flash-Lite)
                        OutlinedTextField(
                            value = cloudModelInput,
                            onValueChange = {
                                cloudModelInput = it
                                saveCloudConfig()
                            },
                            label = { Text("Modelo") },
                            placeholder = { Text("gemini-3.5-flash-lite") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("gemini_model_field"),
                            shape = RoundedCornerShape(14.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Model suggestion chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = cloudModelInput == "gemini-3.5-flash-lite",
                                onClick = {
                                    cloudModelInput = "gemini-3.5-flash-lite"
                                    saveCloudConfig()
                                },
                                label = { Text("3.5 Flash-Lite", fontSize = 12.sp) },
                                shape = RoundedCornerShape(12.dp)
                            )
                            FilterChip(
                                selected = cloudModelInput == "gemini-3.5-flash",
                                onClick = {
                                cloudModelInput = "gemini-3.5-flash"
                                saveCloudConfig()
                            },
                            label = { Text("3.5 Flash", fontSize = 12.sp) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Botón "Probar conexión"
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
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("test_gemini_button")
                        ) {
                            if (isTestingCloudConnection) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
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

                        // Status feedback
                        if (cloudSuccessMessage != null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CloudDone,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = cloudSuccessMessage!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        if (cloudErrorMessage != null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Error,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = cloudErrorMessage!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ==============================================================
            // BLOQUE 2: IA LOCAL (OLLAMA) - ABAJO (Regla 1b)
            // ==============================================================
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "IA local (Ollama)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Card B1: Switch "Usar IA local"
            item {
                AlcanciaCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Memory,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = "Usar IA local",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (settings.isLocalAiEnabled) "Función activada" else "Apagada por defecto",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Switch(
                                checked = settings.isLocalAiEnabled,
                                onCheckedChange = { isChecked ->
                                    saveLocalConfig(enabled = isChecked)
                                    scope.launch {
                                        if (isChecked) {
                                            snackbarHostState.showSnackbar("IA local activada. Verás el botón ✨ Sugerir en nuevos movimientos.")
                                        } else {
                                            snackbarHostState.showSnackbar("IA local desactivada.")
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Card B2: Local Server configuration & Model
            item {
                AlcanciaCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Lan,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Configuración del servidor local",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Ollama API en tu computadora o red",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Campo "Dirección del servidor"
                        OutlinedTextField(
                            value = localServerUrlInput,
                            onValueChange = {
                                localServerUrlInput = it
                                saveLocalConfig()
                            },
                            label = { Text("Dirección del servidor") },
                            placeholder = { Text("http://localhost:11434") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Quick URL suggestion chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = localServerUrlInput == "http://localhost:11434",
                                onClick = {
                                    localServerUrlInput = "http://localhost:11434"
                                    saveLocalConfig()
                                },
                                label = { Text("localhost:11434", fontSize = 12.sp) },
                                shape = RoundedCornerShape(12.dp)
                            )
                            FilterChip(
                                selected = localServerUrlInput == "http://10.0.2.2:11434",
                                onClick = {
                                    localServerUrlInput = "http://10.0.2.2:11434"
                                    saveLocalConfig()
                                },
                                label = { Text("10.0.2.2 (Emulador)", fontSize = 12.sp) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Campo "Modelo"
                        OutlinedTextField(
                            value = localModelInput,
                            onValueChange = {
                                localModelInput = it
                                saveLocalConfig()
                            },
                            label = { Text("Modelo") },
                            placeholder = { Text("llama3.2:1b") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Botón "Probar conexión"
                        Button(
                            onClick = {
                                isTestingLocalConnection = true
                                localSuccessMessage = null
                                localErrorMessage = null
                                saveLocalConfig()
                                viewModel.testOllamaConnection(localServerUrlInput) { result ->
                                    isTestingLocalConnection = false
                                    result.onSuccess { models ->
                                        localSuccessMessage = if (models.isEmpty()) {
                                            "Conectado con éxito a Ollama (sin modelos descargados aún)"
                                        } else {
                                            "Conectado. Modelos encontrados: ${models.joinToString(", ")}"
                                        }
                                        localErrorMessage = null
                                    }.onFailure { error ->
                                        localErrorMessage = error.message ?: "Error al intentar conectar con Ollama."
                                        localSuccessMessage = null
                                    }
                                }
                            },
                            enabled = !isTestingLocalConnection,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            if (isTestingLocalConnection) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
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

                        // Status feedback
                        if (localSuccessMessage != null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CloudDone,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = localSuccessMessage!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        if (localErrorMessage != null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Error,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = localErrorMessage!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Discrete note for local IA (Allowed to retain "sin enviar datos a la nube" per Rule 3)
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Requiere tener Ollama instalado y encendido. Funciona en tu red local sin enviar datos a la nube. La app funciona completa sin esto.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}
