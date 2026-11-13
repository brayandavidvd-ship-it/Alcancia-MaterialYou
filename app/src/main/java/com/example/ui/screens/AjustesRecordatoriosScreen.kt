package com.example.ui.screens

import android.Manifest
import android.app.TimePickerDialog
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.LocalIsCompactDensity
import com.example.ui.viewmodel.AlcanciaViewModel
import com.example.util.ReminderHelper
import kotlinx.coroutines.launch

@Composable
fun AjustesRecordatoriosScreen(
    viewModel: AlcanciaViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val settings by viewModel.settingsState.collectAsStateWithLifecycle()
    val customMessage by viewModel.reminderCustomMessage.collectAsStateWithLifecycle()
    val palette = resolveInicioPalette(settings)
    val isCompact = LocalIsCompactDensity.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var hasNotificationPermission by remember {
        mutableStateOf(ReminderHelper.areNotificationsEnabled(context))
    }
    var isBatteryOptimized by remember {
        mutableStateOf(!ReminderHelper.isBatteryOptimizationIgnored(context))
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotificationPermission = ReminderHelper.areNotificationsEnabled(context)
                isBatteryOptimized = !ReminderHelper.isBatteryOptimizationIgnored(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        scope.launch {
            if (isGranted) {
                snackbarHostState.showSnackbar("Permiso de notificaciones concedido")
            } else {
                snackbarHostState.showSnackbar("Permiso denegado. Puedes habilitarlo desde los ajustes del sistema.")
            }
        }
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
                title = "Recordatorios",
                subtitle = "Notificaciones locales de ahorro",
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
            if (!hasNotificationPermission) {
                item {
                    AjustesPaletteCard(
                        palette = palette,
                        borderColor = palette.gasto.copy(alpha = 0.45f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AjustesIconBadge(
                                    icon = Icons.Default.NotificationsOff,
                                    containerColor = palette.gasto.copy(alpha = 0.16f),
                                    iconTint = palette.gasto,
                                    size = 46.dp,
                                    iconSize = 24.dp
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = "Permiso de notificaciones requerido",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.t1
                                    )
                                    Text(
                                        text = "Avisos desactivados por el sistema",
                                        fontSize = 13.sp,
                                        color = palette.gasto
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Para que Alcancía pueda enviarte avisos a la hora indicada, es necesario otorgar el permiso de notificaciones en tu celular (requerido en Android 13 o superior).",
                                fontSize = 13.sp,
                                color = palette.t2,
                                lineHeight = 19.sp
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            ReminderHelper.openNotificationSettings(context)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = palette.brand,
                                        contentColor = palette.onBrand
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Conceder permiso", fontWeight = FontWeight.SemiBold)
                                }

                                OutlinedButton(
                                    onClick = { ReminderHelper.openNotificationSettings(context) },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = palette.s2,
                                        contentColor = palette.t1
                                    ),
                                    border = BorderStroke(1.dp, palette.s3),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Ajustes")
                                }
                            }
                        }
                    }
                }
            }

            item {
                AjustesPaletteCard(palette = palette) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AjustesIconBadge(
                            icon = Icons.Default.NotificationsActive,
                            containerColor = palette.s2,
                            iconTint = palette.accent,
                            size = 46.dp,
                            iconSize = 24.dp
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Hábito de ahorro",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.t1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Recibe notificaciones locales programadas para mantener tus registros al día y alcanzar tus metas.",
                                fontSize = 13.sp,
                                color = palette.t2,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

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
                                icon = Icons.Default.Notifications,
                                containerColor = palette.s2,
                                iconTint = palette.ahorro,
                                size = 42.dp,
                                iconSize = 22.dp
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Activar recordatorios",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.t1
                                )
                                val statusText = if (settings.remindersEnabled) {
                                    if (hasNotificationPermission) "Notificaciones activadas" else "Activado (esperando permiso)"
                                } else {
                                    "Desactivadas"
                                }
                                Text(
                                    text = statusText,
                                    fontSize = 13.sp,
                                    color = if (settings.remindersEnabled && !hasNotificationPermission) palette.gasto else palette.t2
                                )
                            }
                        }

                        Switch(
                            checked = settings.remindersEnabled,
                            colors = ajustesSwitchColors(palette),
                            modifier = Modifier.testTag("switch_reminders"),
                            onCheckedChange = { isChecked ->
                                if (isChecked && !hasNotificationPermission) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        ReminderHelper.openNotificationSettings(context)
                                    }
                                }
                                viewModel.setRemindersConfig(isChecked, settings.reminderTime, settings.reminderFrequency)
                                scope.launch {
                                    if (isChecked) {
                                        snackbarHostState.showSnackbar("Recordatorios programados para las ${settings.reminderTime}")
                                    } else {
                                        snackbarHostState.showSnackbar("Recordatorios desactivados")
                                    }
                                }
                            }
                        )
                    }
                }
            }

            if (settings.remindersEnabled) {
                item {
                    val parts = settings.reminderTime.split(":")
                    val currentHour = parts.getOrNull(0)?.toIntOrNull() ?: 20
                    val currentMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0

                    AjustesPaletteCard(
                        palette = palette,
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    val formattedTime = String.format(java.util.Locale.getDefault(), "%02d:%02d", hour, minute)
                                    viewModel.setRemindersConfig(true, formattedTime, settings.reminderFrequency)
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Hora actualizada a las $formattedTime")
                                    }
                                },
                                currentHour,
                                currentMinute,
                                true
                            ).show()
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                AjustesIconBadge(
                                    icon = Icons.Default.AccessTime,
                                    containerColor = palette.s2,
                                    iconTint = palette.accent,
                                    size = 42.dp,
                                    iconSize = 22.dp
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = "Hora de notificación",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.t1
                                    )
                                    Text(
                                        text = "Toca para cambiar la hora",
                                        fontSize = 13.sp,
                                        color = palette.t2
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = palette.s3,
                                border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.35f)),
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Text(
                                    text = settings.reminderTime,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.accent,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                item {
                    AjustesPaletteCard(palette = palette) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AjustesIconBadge(
                                    icon = Icons.Default.DateRange,
                                    containerColor = palette.s2,
                                    iconTint = palette.accent,
                                    size = 42.dp,
                                    iconSize = 22.dp
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = "Frecuencia de aviso",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.t1
                                    )
                                    Text(
                                        text = "Elige cuándo recibir el aviso",
                                        fontSize = 13.sp,
                                        color = palette.t2
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            val frequencies = listOf(
                                "DAILY" to "Diario (Todos los días)",
                                "WEEKDAYS" to "Días hábiles (Lun - Vie)",
                                "WEEKENDS" to "Fines de semana (Sáb - Dom)"
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                for ((key, label) in frequencies) {
                                    val isSelected = settings.reminderFrequency == key
                                    Surface(
                                        onClick = {
                                            viewModel.setRemindersConfig(true, settings.reminderTime, key)
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isSelected) palette.s2 else Color.Transparent,
                                        border = if (isSelected) BorderStroke(1.dp, palette.accent.copy(alpha = 0.45f)) else null,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .defaultMinSize(minHeight = 48.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = {
                                                    viewModel.setRemindersConfig(true, settings.reminderTime, key)
                                                },
                                                colors = ajustesRadioColors(palette)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = label,
                                                fontSize = 14.sp,
                                                color = palette.t1,
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                            )
                                        }
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
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AjustesIconBadge(
                                    icon = Icons.Default.Edit,
                                    containerColor = palette.s1,
                                    iconTint = palette.accent,
                                    size = 42.dp,
                                    iconSize = 22.dp
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = "Mensaje de notificación",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.t1
                                    )
                                    Text(
                                        text = "Personaliza el aviso local diario o hábil",
                                        fontSize = 13.sp,
                                        color = palette.t2
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedTextField(
                                value = customMessage,
                                onValueChange = { viewModel.updateReminderCustomMessage(it) },
                                label = { Text("Mensaje personalizado de notificación") },
                                placeholder = { Text(ReminderHelper.DEFAULT_REMINDER_MESSAGE) },
                                minLines = 2,
                                maxLines = 4,
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = palette.t1,
                                    unfocusedTextColor = palette.t1,
                                    focusedContainerColor = palette.s1,
                                    unfocusedContainerColor = palette.s1,
                                    focusedBorderColor = palette.accent,
                                    unfocusedBorderColor = palette.s3,
                                    focusedLabelColor = palette.accent,
                                    unfocusedLabelColor = palette.t2,
                                    focusedPlaceholderColor = palette.t2.copy(alpha = 0.7f),
                                    unfocusedPlaceholderColor = palette.t2.copy(alpha = 0.7f),
                                    cursorColor = palette.accent
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reminder_custom_message_field")
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Este texto se mostrará en tus avisos programados y pruebas.",
                                fontSize = 12.sp,
                                color = palette.t2,
                                lineHeight = 16.sp
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedButton(
                                onClick = {
                                    viewModel.resetReminderCustomMessage()
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Mensaje predeterminado restaurado")
                                    }
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = palette.s1,
                                    contentColor = palette.t1
                                ),
                                border = BorderStroke(1.dp, palette.s3),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("reset_reminder_message_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RestartAlt,
                                    contentDescription = null,
                                    tint = palette.accent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Restaurar mensaje predeterminado",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                item {
                    AjustesPaletteCard(palette = palette) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AjustesIconBadge(
                                    icon = if (isBatteryOptimized) Icons.Default.BatteryAlert else Icons.Default.BatteryChargingFull,
                                    containerColor = palette.s2,
                                    iconTint = if (isBatteryOptimized) palette.accent else palette.ingreso,
                                    size = 42.dp,
                                    iconSize = 22.dp
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = "Segundo plano y batería",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.t1
                                    )
                                    Text(
                                        text = if (isBatteryOptimized) "Optimización del sistema activa" else "Sin restricciones (Óptimo)",
                                        fontSize = 13.sp,
                                        color = if (isBatteryOptimized) palette.accent else palette.ingreso
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = if (isBatteryOptimized) {
                                    "Los sistemas de ahorro de batería agresivos pueden cerrar los procesos en segundo plano y cancelar las alarmas exactas. Permite que Alcancía se ejecute sin restricciones para garantizar que los recordatorios se disparen aunque la app esté cerrada."
                                } else {
                                    "¡Excelente! La app tiene permiso para ejecutarse en segundo plano. Tus alarmas programadas se activarán puntualmente incluso si el dispositivo entra en modo reposo."
                                },
                                fontSize = 13.sp,
                                color = palette.t2,
                                lineHeight = 18.sp
                            )

                            if (isBatteryOptimized) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = {
                                        ReminderHelper.requestIgnoreBatteryOptimization(context)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = palette.s2,
                                        contentColor = palette.t1
                                    ),
                                    border = BorderStroke(1.dp, palette.s3),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = palette.accent, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Permitir ejecución sin interrupciones", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                item {
                    AjustesPaletteCard(palette = palette) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Text(
                                text = "Prueba de notificación",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.t1
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Comprueba al instante cómo se visualiza el aviso de Alcancía en la barra de estado de tu celular.",
                                fontSize = 13.sp,
                                color = palette.t2,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedButton(
                                onClick = {
                                    if (!hasNotificationPermission) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            ReminderHelper.openNotificationSettings(context)
                                        }
                                    } else {
                                        ReminderHelper.sendSavingsNotification(context, isTest = true)
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Notificación de prueba enviada")
                                        }
                                    }
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = palette.s2,
                                    contentColor = palette.t1
                                ),
                                border = BorderStroke(1.dp, palette.s3),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, tint = palette.accent, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Enviar notificación de prueba", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}
