package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.LocalIsCompactDensity
import com.example.ui.viewmodel.AlcanciaViewModel
import kotlinx.coroutines.launch

@Composable
fun AjustesSeguridadScreen(
    viewModel: AlcanciaViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val settings by viewModel.settingsState.collectAsStateWithLifecycle()
    val palette = resolveInicioPalette(settings)
    val isCompact = LocalIsCompactDensity.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var pinConfirmInput by remember { mutableStateOf("") }
    var pinErrorMessage by remember { mutableStateOf<String?>(null) }

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
                title = "Seguridad",
                subtitle = "Bloqueo biométrico y PIN",
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
                AjustesPaletteCard(palette = palette) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AjustesIconBadge(
                            icon = Icons.Default.Security,
                            containerColor = palette.s2,
                            iconTint = palette.accent,
                            size = 46.dp,
                            iconSize = 24.dp
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Bloqueo de la app",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.t1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Protege el acceso a tus finanzas y metas cuando abras la aplicación.",
                                fontSize = 13.sp,
                                color = palette.t2,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            // Bloqueo por huella digital
            item {
                AjustesPaletteCard(palette = palette) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
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
                                    icon = Icons.Default.Fingerprint,
                                    containerColor = palette.s2,
                                    iconTint = palette.ahorro,
                                    size = 42.dp,
                                    iconSize = 22.dp
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = "Bloqueo por huella digital",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.t1
                                    )
                                    Text(
                                        text = "Autenticación biométrica del sistema",
                                        fontSize = 13.sp,
                                        color = palette.t2
                                    )
                                }
                            }

                            Switch(
                                checked = settings.isFingerprintEnabled,
                                colors = ajustesSwitchColors(palette),
                                modifier = Modifier.testTag("switch_fingerprint"),
                                onCheckedChange = { isChecked ->
                                    viewModel.setFingerprintEnabled(isChecked)
                                    scope.launch {
                                        if (isChecked) {
                                            snackbarHostState.showSnackbar("Huella digital activada para abrir la app")
                                        } else {
                                            snackbarHostState.showSnackbar("Bloqueo por huella desactivado")
                                        }
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Solicita tu huella dactilar o reconocimiento biométrico registrado en tu dispositivo cada vez que ingreses a Alcancía.",
                            fontSize = 13.sp,
                            color = palette.t2,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Bloqueo por PIN
            item {
                AjustesPaletteCard(palette = palette) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
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
                                    icon = Icons.Default.Pin,
                                    containerColor = palette.s2,
                                    iconTint = palette.accent,
                                    size = 42.dp,
                                    iconSize = 22.dp
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = "Bloqueo por PIN",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.t1
                                    )
                                    val subtitleText = if (settings.isPinEnabled && settings.pinCode.isNotEmpty()) {
                                        "PIN de 4 dígitos configurado"
                                    } else {
                                        "Código numérico de acceso"
                                    }
                                    Text(
                                        text = subtitleText,
                                        fontSize = 13.sp,
                                        color = palette.t2
                                    )
                                }
                            }

                            Switch(
                                checked = settings.isPinEnabled,
                                colors = ajustesSwitchColors(palette),
                                modifier = Modifier.testTag("switch_pin"),
                                onCheckedChange = { isChecked ->
                                    if (isChecked) {
                                        if (settings.pinCode.length == 4) {
                                            viewModel.setPinSecurity(true, settings.pinCode)
                                            scope.launch { snackbarHostState.showSnackbar("Bloqueo por PIN activado") }
                                        } else {
                                            pinInput = ""
                                            pinConfirmInput = ""
                                            pinErrorMessage = null
                                            showPinDialog = true
                                        }
                                    } else {
                                        viewModel.setPinSecurity(false, settings.pinCode)
                                        scope.launch { snackbarHostState.showSnackbar("Bloqueo por PIN desactivado") }
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Solicita un código PIN numérico de 4 dígitos al iniciar la app.",
                            fontSize = 13.sp,
                            color = palette.t2,
                            lineHeight = 18.sp
                        )

                        if (settings.isPinEnabled && settings.pinCode.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            OutlinedButton(
                                onClick = {
                                    pinInput = ""
                                    pinConfirmInput = ""
                                    pinErrorMessage = null
                                    showPinDialog = true
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = palette.s2,
                                    contentColor = palette.t1
                                ),
                                border = BorderStroke(1.dp, palette.s3),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = palette.accent, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Cambiar código PIN", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            containerColor = palette.s1,
            titleContentColor = palette.t1,
            textContentColor = palette.t2,
            title = {
                Text(
                    text = if (settings.pinCode.isEmpty()) "Configurar PIN de 4 dígitos" else "Cambiar PIN",
                    fontWeight = FontWeight.Bold,
                    color = palette.t1
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Elige un código de 4 dígitos que recuerdes fácilmente.",
                        fontSize = 13.sp,
                        color = palette.t2
                    )

                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) pinInput = it },
                        label = { Text("Nuevo PIN (4 dígitos)") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        colors = ajustesTextFieldColors(palette),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    OutlinedTextField(
                        value = pinConfirmInput,
                        onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) pinConfirmInput = it },
                        label = { Text("Confirmar PIN") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        colors = ajustesTextFieldColors(palette),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    if (pinErrorMessage != null) {
                        Text(
                            text = pinErrorMessage!!,
                            color = palette.gasto,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinInput.length != 4) {
                            pinErrorMessage = "El PIN debe tener exactamente 4 dígitos."
                        } else if (pinInput != pinConfirmInput) {
                            pinErrorMessage = "Los dos códigos PIN no coinciden."
                        } else {
                            viewModel.setPinSecurity(true, pinInput)
                            showPinDialog = false
                            scope.launch { snackbarHostState.showSnackbar("PIN configurado correctamente") }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.brand,
                        contentColor = palette.onBrand
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Guardar PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text("Cancelar", color = palette.t2)
                }
            }
        )
    }
}
