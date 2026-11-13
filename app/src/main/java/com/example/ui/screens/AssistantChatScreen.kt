package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.service.GeminiService
import com.example.ui.components.CurrencyUtils
import com.example.ui.model.ActionStatus
import com.example.ui.model.ChatActionProposal
import com.example.ui.model.ChatMessage
import com.example.ui.model.MessageSender
import com.example.ui.theme.ColorblindExpenseColor
import com.example.ui.theme.ColorblindIncomeColor
import com.example.ui.viewmodel.AlcanciaViewModel
import com.example.util.rememberVoiceDictationLauncher
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantChatScreen(
    viewModel: AlcanciaViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onDismiss)

    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isTyping by viewModel.isChatTyping.collectAsStateWithLifecycle()
    val errorMessage by viewModel.chatErrorMessage.collectAsStateWithLifecycle()
    val errorDetail by viewModel.chatErrorDetail.collectAsStateWithLifecycle()
    val settings by viewModel.settingsState.collectAsStateWithLifecycle()

    val systemDark = isSystemInDarkTheme()
    val isDark = when (settings.themeMode) {
        "LIGHT" -> false
        "DARK" -> true
        else -> systemDark
    }
    val rawPalette = if (isDark) InicioPalette.Dark else InicioPalette.Light
    val palette = if (settings.isColorblindPreset) {
        rawPalette.copy(
            ingreso = ColorblindIncomeColor,
            gasto = ColorblindExpenseColor
        )
    } else {
        rawPalette
    }

    var inputValue by remember { mutableStateOf(TextFieldValue("")) }
    val inputFocusRequester = remember { FocusRequester() }
    val pendingVoiceTranscript by viewModel.pendingVoiceTranscript.collectAsStateWithLifecycle()
    val shouldFocusVoiceInputOnOpen by viewModel.shouldFocusVoiceInputOnOpen.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    val launchVoiceInput = rememberVoiceDictationLauncher(
        prompt = "Dicta tu mensaje…",
        onResult = { transcript ->
            val current = inputValue.text
            val updated = if (current.isBlank()) transcript else "${current.trimEnd()} $transcript"
            inputValue = TextFieldValue(
                text = updated,
                selection = TextRange(updated.length)
            )
            try {
                inputFocusRequester.requestFocus()
            } catch (_: Exception) {}
        },
        onUnavailable = {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Dictado por voz no disponible en este dispositivo")
            }
            try {
                inputFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    )

    LaunchedEffect(pendingVoiceTranscript, shouldFocusVoiceInputOnOpen) {
        if (pendingVoiceTranscript != null || shouldFocusVoiceInputOnOpen) {
            val transcript = viewModel.consumePendingVoiceTranscript()
            if (!transcript.isNullOrBlank()) {
                inputValue = TextFieldValue(
                    text = transcript,
                    selection = TextRange(transcript.length)
                )
            }
            try {
                inputFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    val revealedMessageIds = remember { mutableStateListOf<String>() }
    val animationsEnabled = settings.hapticLevel != "OFF"

    LaunchedEffect(messages.size, isTyping) {
        if (messages.isNotEmpty() || isTyping) {
            val target = (messages.size - 1 + if (isTyping) 1 else 0).coerceAtLeast(0)
            listState.animateScrollToItem(target)
        }
    }

    val canScrollForward by remember {
        derivedStateOf { listState.canScrollForward }
    }

    fun copyToClipboard(text: String) {
        if (text.isNotBlank()) {
            clipboardManager.setText(AnnotatedString(text))
            viewModel.vibrateDevice("LOW")
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Copiado al portapapeles")
            }
        }
    }

    val uriHandler = LocalUriHandler.current
    var showModelBottomSheet by remember { mutableStateOf(false) }

    val currentModelLabel = if (settings.cloudAiModel == GeminiService.FLASH_MODEL) {
        "3.5 Flash"
    } else {
        "3.5 Flash-Lite"
    }

    val isEmptyState = messages.isEmpty() && !isTyping && errorMessage == null

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.bg)
    ) {
        // BLOQUE A: Encabezado único con statusBarsPadding()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onDismiss,
                shape = CircleShape,
                color = palette.s2,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("chat_back_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = palette.accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Asistente",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Medium,
                    color = palette.t1,
                    maxLines = 1,
                    softWrap = false
                )

                Spacer(modifier = Modifier.width(10.dp))

                Surface(
                    onClick = { showModelBottomSheet = true },
                    shape = CircleShape,
                    color = palette.s2,
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("model_toggle_chip")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = palette.accent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = currentModelLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = palette.accent,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Seleccionar modelo",
                            tint = palette.t2,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                onClick = { viewModel.resetChatConversation() },
                shape = CircleShape,
                color = palette.s2,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("chat_new_chat_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Nuevo chat",
                        tint = palette.accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Zona central (Estado vacío o Conversación + fade inferior de 28dp)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (isEmptyState) {
                EmptyChatGreeting(
                    palette = palette,
                    onSuggestionClick = { suggestion ->
                        viewModel.sendChatMessage(suggestion)
                    }
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 36.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(
                        items = messages,
                        key = { it.id }
                    ) { msg ->
                        val isLastAssistant = msg == messages.lastOrNull { it.sender == MessageSender.ASSISTANT }
                        val shouldAnimateTypewriter = isLastAssistant &&
                            msg.id !in revealedMessageIds &&
                            animationsEnabled &&
                            msg.sender == MessageSender.ASSISTANT

                        ChatMessageItem(
                            message = msg,
                            currencySymbol = settings.currencySymbol,
                            palette = palette,
                            animateTypewriter = shouldAnimateTypewriter,
                            onTypewriterCompleted = {
                                if (msg.id !in revealedMessageIds) {
                                    revealedMessageIds.add(msg.id)
                                }
                            },
                            onCopyText = { copyToClipboard(it) },
                            onRegenerate = { viewModel.regenerateLastResponse() },
                            onConfirmProposal = { viewModel.confirmActionProposal(it) },
                            onCancelProposal = { viewModel.cancelActionProposal(it) }
                        )
                    }

                    if (isTyping) {
                        item(key = "thinking_indicator") {
                            PulsingThinkingIndicator(palette = palette)
                        }
                    }

                    if (errorMessage != null) {
                        item(key = "error_message_card") {
                            ChatErrorBlock(
                                errorMessage = errorMessage ?: "",
                                errorDetail = errorDetail,
                                palette = palette,
                                onRetry = { viewModel.retryLastTurn() },
                                onNewChat = { viewModel.resetChatConversation() }
                            )
                        }
                    }
                }
            }

            // BLOQUE E1: Difuminado inferior de 28dp sobre el scroll hacia la zona inferior sólida
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                palette.bg.copy(alpha = 0f),
                                palette.bg
                            )
                        )
                    )
            )

            // Botón flotante para bajar al final cuando hay historial por debajo
            if (!isEmptyState && canScrollForward) {
                Surface(
                    onClick = {
                        coroutineScope.launch {
                            val target = (messages.size - 1 + if (isTyping) 1 else 0).coerceAtLeast(0)
                            listState.animateScrollToItem(target)
                        }
                    },
                    shape = CircleShape,
                    color = palette.s3,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                        .size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Ir al final",
                            tint = palette.t1,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
            )
        }

        // BLOQUE E2-E4: Zona inferior sólida con navigationBarsPadding().imePadding() UNA sola vez
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.bg)
                .navigationBarsPadding()
                .imePadding()
                .padding(top = 4.dp, bottom = 12.dp)
        ) {
            if (!isEmptyState) {
                Text(
                    text = "Puedes decir algo como \"aporte de \$50 a ahorros\"",
                    fontSize = 12.sp,
                    color = palette.t2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                val quickTemplates = remember {
                    listOf(
                        "Gasto" to "Gasto de $",
                        "Ingreso" to "Ingreso de $",
                        "Aporte a meta" to "Aporte a meta de $",
                        "Compra" to "Agregar a compras: "
                    )
                }

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(quickTemplates) { (label, template) ->
                        Surface(
                            onClick = {
                                inputValue = TextFieldValue(
                                    text = template,
                                    selection = TextRange(template.length)
                                )
                            },
                            shape = CircleShape,
                            color = palette.s2,
                            modifier = Modifier.height(36.dp)
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = palette.t1,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            Surface(
                shape = RoundedCornerShape(28.dp),
                color = palette.s1,
                border = BorderStroke(
                    width = 1.5.dp,
                    brush = Brush.horizontalGradient(InicioPalette.AiGradient)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .heightIn(min = 56.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = palette.ahorro,
                        modifier = Modifier.size(22.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (inputValue.text.isEmpty()) {
                            Text(
                                text = "Dime qué registrar…",
                                fontSize = 14.sp,
                                color = palette.t2,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        BasicTextField(
                            value = inputValue,
                            onValueChange = { inputValue = it },
                            textStyle = TextStyle(
                                fontSize = 14.sp,
                                color = palette.t1,
                                lineHeight = 20.sp
                            ),
                            cursorBrush = SolidColor(palette.accent),
                            maxLines = 4,
                            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    val trimmed = inputValue.text.trim()
                                    if (trimmed.isNotEmpty() && !isTyping) {
                                        inputValue = TextFieldValue("")
                                        viewModel.sendChatMessage(trimmed)
                                    }
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(inputFocusRequester)
                                .testTag("chat_input_field")
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    AiVoiceMicButton(
                        palette = palette,
                        enabled = !isTyping,
                        onClick = { launchVoiceInput() },
                        modifier = Modifier.testTag("chat_mic_button")
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    if (isTyping) {
                        Surface(
                            onClick = { viewModel.stopGeneration() },
                            shape = CircleShape,
                            color = palette.gasto,
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("chat_stop_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "Detener",
                                    tint = palette.onBrand,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    } else {
                        Surface(
                            onClick = {
                                val trimmed = inputValue.text.trim()
                                if (trimmed.isNotEmpty()) {
                                    inputValue = TextFieldValue("")
                                    viewModel.sendChatMessage(trimmed)
                                }
                            },
                            shape = CircleShape,
                            color = palette.brand,
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("chat_send_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = "Enviar",
                                    tint = palette.onBrand,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // BLOQUE B: Hoja "Modelo del Asistente"
    if (showModelBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showModelBottomSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = palette.s1,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 8.dp)
                        .size(width = 36.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(palette.s3)
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 28.dp)
            ) {
                Text(
                    text = "Modelo del Asistente",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = palette.t1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Elige el modelo según tu tipo de consulta. Al cambiar de modelo se iniciará un nuevo chat.",
                    fontSize = 12.sp,
                    color = palette.t2,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                val isLiteActive = settings.cloudAiModel != GeminiService.FLASH_MODEL
                ModelOptionItem(
                    title = "3.5 Flash-Lite",
                    description = "Rápido y ligero, ideal para el día a día. Permite más usos.",
                    isActive = isLiteActive,
                    badge = "Predeterminado",
                    palette = palette,
                    onClick = {
                        showModelBottomSheet = false
                        viewModel.selectCloudAiModel(GeminiService.DEFAULT_MODEL)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Cambiado a 3.5 Flash-Lite (Nuevo chat)")
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                val isFlashActive = settings.cloudAiModel == GeminiService.FLASH_MODEL
                ModelOptionItem(
                    title = "3.5 Flash",
                    description = "Más capaz con consultas largas, pero gasta más cuota.",
                    isActive = isFlashActive,
                    badge = null,
                    palette = palette,
                    onClick = {
                        showModelBottomSheet = false
                        viewModel.selectCloudAiModel(GeminiService.FLASH_MODEL)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Cambiado a 3.5 Flash (Nuevo chat)")
                        }
                    }
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            try {
                                uriHandler.openUri("https://aistudio.google.com")
                            } catch (_: Exception) {}
                        }
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = null,
                        tint = palette.t2,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Los límites de uso dependen de tu cuenta. Consúltalos en Google AI Studio.",
                        fontSize = 12.sp,
                        color = palette.t2,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyChatGreeting(
    palette: InicioPalette.Colors,
    onSuggestionClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = palette.s2,
            modifier = Modifier.size(88.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "¿En qué te ayudo con tus ahorros?",
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
            color = palette.t1,
            textAlign = TextAlign.Center,
            lineHeight = 28.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        val suggestions = listOf(
            "Resumen de mi mes",
            "Agregar un gasto",
            "Crear una meta",
            "Consejos para ahorrar"
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            suggestions.forEach { suggestion ->
                Surface(
                    onClick = { onSuggestionClick(suggestion) },
                    shape = CircleShape,
                    color = palette.s2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("suggestion_chip_${suggestion.replace(" ", "_")}")
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = suggestion,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = palette.t1,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatMessageItem(
    message: ChatMessage,
    currencySymbol: String,
    palette: InicioPalette.Colors,
    animateTypewriter: Boolean,
    onTypewriterCompleted: () -> Unit,
    onCopyText: (String) -> Unit,
    onRegenerate: () -> Unit,
    onConfirmProposal: (ChatActionProposal) -> Unit,
    onCancelProposal: (ChatActionProposal) -> Unit
) {
    val isUser = message.sender == MessageSender.USER

    if (isUser) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val bubbleMaxWidth = maxWidth * 0.8f
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Surface(
                    shape = RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = 20.dp,
                        bottomEnd = 6.dp
                    ),
                    color = palette.s2,
                    modifier = Modifier
                        .widthIn(max = bubbleMaxWidth)
                        .pointerInput(message.text) {
                            detectTapGestures(
                                onLongPress = { onCopyText(message.text) }
                            )
                        }
                        .testTag("user_chat_bubble")
                ) {
                    Text(
                        text = message.text,
                        fontSize = 15.sp,
                        color = palette.t1,
                        lineHeight = 21.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("assistant_chat_message")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Alcancía",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = palette.t2
                )
            }

            if (message.text.isNotBlank()) {
                if (animateTypewriter) {
                    TypewriterMarkdownText(
                        fullText = message.text,
                        color = palette.t1,
                        onComplete = onTypewriterCompleted,
                        onLongPress = { onCopyText(message.text) },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    MarkdownText(
                        markdown = message.text,
                        color = palette.t1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(message.text) {
                                detectTapGestures(
                                    onLongPress = { onCopyText(message.text) }
                                )
                            }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onCopyText(message.text) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copiar mensaje",
                            tint = palette.t2,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onRegenerate,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Regenerar respuesta",
                            tint = palette.t2,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (message.actionProposal != null) {
                Spacer(modifier = Modifier.height(6.dp))
                ActionProposalCard(
                    proposal = message.actionProposal,
                    currencySymbol = currencySymbol,
                    palette = palette,
                    onConfirm = { onConfirmProposal(message.actionProposal) },
                    onCancel = { onCancelProposal(message.actionProposal) }
                )
            }
        }
    }
}

@Composable
private fun TypewriterMarkdownText(
    fullText: String,
    color: Color,
    onComplete: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    var revealedLength by remember(fullText) { mutableStateOf(0) }

    LaunchedEffect(fullText) {
        if (revealedLength >= fullText.length) return@LaunchedEffect
        while (revealedLength < fullText.length) {
            val step = (fullText.length / 30).coerceIn(2, 8)
            revealedLength = (revealedLength + step).coerceAtMost(fullText.length)
            delay(16)
        }
        onComplete()
    }

    val visibleText = fullText.take(revealedLength)

    MarkdownText(
        markdown = visibleText,
        color = color,
        modifier = modifier.pointerInput(fullText) {
            detectTapGestures(
                onTap = {
                    revealedLength = fullText.length
                    onComplete()
                },
                onLongPress = { onLongPress() }
            )
        }
    )
}

private val SPANISH_MONTHS_SHORT = listOf(
    "ene", "feb", "mar", "abr", "may", "jun",
    "jul", "ago", "sep", "oct", "nov", "dic"
)

private fun formatProposalCardDate(rawDate: String?): String {
    val todayCal = Calendar.getInstance()
    val todayY = todayCal.get(Calendar.YEAR)
    val todayM = todayCal.get(Calendar.MONTH)
    val todayD = todayCal.get(Calendar.DAY_OF_MONTH)

    if (rawDate.isNullOrBlank() || rawDate.equals("hoy", ignoreCase = true)) {
        return "hoy, $todayD ${SPANISH_MONTHS_SHORT.getOrElse(todayM) { "" }}"
    }

    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val parsed = parser.parse(rawDate)
        if (parsed != null) {
            val cal = Calendar.getInstance().apply { time = parsed }
            val y = cal.get(Calendar.YEAR)
            val m = cal.get(Calendar.MONTH)
            val d = cal.get(Calendar.DAY_OF_MONTH)
            val shortStr = "$d ${SPANISH_MONTHS_SHORT.getOrElse(m) { "" }}"
            if (y == todayY && m == todayM && d == todayD) {
                "hoy, $shortStr"
            } else {
                shortStr
            }
        } else {
            rawDate
        }
    } catch (_: Exception) {
        rawDate
    }
}

@Composable
private fun ActionProposalCard(
    proposal: ChatActionProposal,
    currencySymbol: String,
    palette: InicioPalette.Colors,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val isDestructive = proposal.isDestructive
    val isPending = proposal.status == ActionStatus.PENDING
    val isExecuting = proposal.status == ActionStatus.EXECUTING
    val isConfirmed = proposal.status == ActionStatus.CONFIRMED
    val isCancelled = proposal.status == ActionStatus.CANCELLED

    val tipoArg = proposal.arguments["tipo"]?.toString()?.uppercase()
    val isGoalTool = proposal.toolName in listOf(
        "crear_meta", "editar_meta", "aportar_a_meta", "retirar_de_meta"
    )
    val isShoppingTool = proposal.toolName in listOf(
        "crear_lista_compras", "agregar_producto_compra", "editar_producto_compra"
    )

    val (iconVector, iconTint) = when {
        isDestructive -> Icons.Default.Delete to palette.gasto
        proposal.toolName == "agregar_movimiento" || proposal.toolName == "editar_movimiento" -> {
            when (tipoArg) {
                "INCOME" -> Icons.Default.ArrowUpward to palette.ingreso
                "SAVING_DEPOSIT" -> Icons.Default.Savings to palette.ahorro
                else -> Icons.Default.ArrowDownward to palette.gasto
            }
        }
        isGoalTool -> Icons.Default.Savings to palette.ahorro
        isShoppingTool -> Icons.Default.ShoppingCart to palette.accent
        else -> Icons.Default.AutoAwesome to palette.accent
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = palette.s1,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .testTag("action_proposal_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = iconTint.copy(alpha = 0.18f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = iconVector,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    if (proposal.toolName == "agregar_movimiento") {
                        val tipoLabel = when (tipoArg) {
                            "INCOME" -> "Ingreso"
                            "SAVING_DEPOSIT" -> "Ahorro"
                            else -> "Gasto"
                        }
                        val monto = proposal.arguments["monto"]?.toString()?.toDoubleOrNull() ?: 0.0
                        val formattedAmount = CurrencyUtils.formatAmount(monto, currencySymbol)
                        Text(
                            text = "$tipoLabel de $formattedAmount",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = palette.t1,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        val rawNote = proposal.arguments["nota"]?.toString()
                        val concepto = rawNote?.takeIf {
                            it.isNotBlank() &&
                                !it.equals("Sin nota", ignoreCase = true) &&
                                !it.equals("null", ignoreCase = true)
                        }
                        val dateStr = formatProposalCardDate(proposal.arguments["fecha"]?.toString())
                        val line2 = if (concepto != null) "$concepto · $dateStr" else dateStr
                        Text(
                            text = line2,
                            fontSize = 12.sp,
                            color = palette.t2,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        val cleanSummary = proposal.summary
                            .replace(Regex("#[0-9]+"), "")
                            .replace("Sin nota", "")
                            .replace(" en ''", "")
                            .replace("  ", " ")
                            .trim()
                        Text(
                            text = cleanSummary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = palette.t1,
                            lineHeight = 19.sp
                        )
                        if (isPending && isDestructive) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Esta acción no se puede deshacer",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = palette.gasto
                            )
                        }
                    }
                }
            }

            if (isPending) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        onClick = onCancel,
                        shape = CircleShape,
                        color = palette.s3,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("proposal_cancel_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Cancelar",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = palette.t1,
                                maxLines = 1
                            )
                        }
                    }

                    Surface(
                        onClick = onConfirm,
                        shape = CircleShape,
                        color = palette.brand,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("proposal_confirm_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Confirmar",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = palette.onBrand,
                                maxLines = 1
                            )
                        }
                    }
                }
            } else if (isExecuting) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = palette.accent
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ejecutando acción...",
                        fontSize = 12.sp,
                        color = palette.t2
                    )
                }
            } else if (isConfirmed) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = palette.ingreso,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Registrado",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = palette.ingreso
                    )
                }
            } else if (isCancelled) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = proposal.resultText?.takeIf { it.isNotBlank() } ?: "Cancelado",
                    fontSize = 13.sp,
                    color = palette.t2
                )
            }
        }
    }
}

@Composable
private fun ModelOptionItem(
    title: String,
    description: String,
    isActive: Boolean,
    badge: String?,
    palette: InicioPalette.Colors,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = palette.s2,
        border = if (isActive) BorderStroke(1.5.dp, palette.brand) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = palette.t1
                    )
                    if (badge != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = palette.s3
                        ) {
                            Text(
                                text = badge,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = palette.t2,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = palette.t2,
                    lineHeight = 17.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Surface(
                shape = CircleShape,
                color = if (isActive) palette.accent else palette.s3,
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isActive) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Activo",
                            tint = palette.s1,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PulsingThinkingIndicator(
    palette: InicioPalette.Colors
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_dots")

    val dot1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, delayMillis = 0), RepeatMode.Reverse),
        label = "d1"
    )
    val dot2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, delayMillis = 200), RepeatMode.Reverse),
        label = "d2"
    )
    val dot3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, delayMillis = 400), RepeatMode.Reverse),
        label = "d3"
    )
    val iconScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "iconScale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = palette.accent,
            modifier = Modifier
                .size(18.dp)
                .scale(iconScale)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.padding(vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(palette.accent.copy(alpha = dot1Alpha))
            )
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(palette.accent.copy(alpha = dot2Alpha))
            )
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(palette.accent.copy(alpha = dot3Alpha))
            )
        }
    }
}

@Composable
private fun ChatErrorBlock(
    errorMessage: String,
    errorDetail: String?,
    palette: InicioPalette.Colors,
    onRetry: () -> Unit,
    onNewChat: () -> Unit
) {
    var isDetailExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag("chat_error_card")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = palette.gasto,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Alcancía",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = palette.t2
            )
        }

        val headline = when {
            errorMessage.contains("Clave", ignoreCase = true) -> "Clave de API inválida"
            errorMessage.contains("conexión", ignoreCase = true) -> "Problema de conexión"
            else -> "Hubo un problema al responder"
        }

        Text(
            text = headline,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = palette.gasto
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = errorMessage,
            fontSize = 15.sp,
            color = palette.gasto,
            lineHeight = 22.sp
        )

        if (!errorDetail.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isDetailExpanded) "Ocultar detalle técnico ▴" else "Ver detalle técnico ▾",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = palette.t2,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { isDetailExpanded = !isDetailExpanded }
                    .padding(vertical = 4.dp)
            )

            if (isDetailExpanded) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = palette.s2,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorDetail,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = palette.t1,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                onClick = onNewChat,
                shape = CircleShape,
                color = palette.s3,
                modifier = Modifier
                    .height(40.dp)
                    .testTag("error_new_chat_btn")
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nuevo chat",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = palette.t1
                    )
                }
            }

            Surface(
                onClick = onRetry,
                shape = CircleShape,
                color = palette.gasto,
                modifier = Modifier
                    .height(40.dp)
                    .testTag("error_retry_btn")
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Reintentar",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = palette.onBrand
                    )
                }
            }
        }
    }
}

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified
) {
    val styledText = remember(markdown) {
        parseMarkdownToAnnotatedString(markdown)
    }
    Text(
        text = styledText,
        fontSize = 15.sp,
        color = color,
        modifier = modifier,
        lineHeight = 22.sp
    )
}

private fun parseMarkdownToAnnotatedString(input: String): AnnotatedString {
    val lines = input.lines().map { line ->
        val trimmed = line.trimStart()
        when {
            trimmed.startsWith("### ") -> {
                val indent = line.substring(0, line.length - trimmed.length)
                indent + "**" + trimmed.substring(4) + "**"
            }
            trimmed.startsWith("## ") -> {
                val indent = line.substring(0, line.length - trimmed.length)
                indent + "**" + trimmed.substring(3) + "**"
            }
            trimmed.startsWith("# ") -> {
                val indent = line.substring(0, line.length - trimmed.length)
                indent + "**" + trimmed.substring(2) + "**"
            }
            trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                val indent = line.substring(0, line.length - trimmed.length)
                indent + "• " + trimmed.substring(2)
            }
            else -> line
        }
    }
    val processedText = lines.joinToString("\n")

    return buildAnnotatedString {
        var cursor = 0
        val pattern = Pattern.compile("(\\*\\*(.*?)\\*\\*)|((?<!\\*)\\*([^*]+)\\*(?!\\*))|(`(.*?)`)")
        val matcher = pattern.matcher(processedText)

        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()

            if (start > cursor) {
                append(processedText.substring(cursor, start))
            }

            val boldGroup = matcher.group(2)
            val italicGroup = matcher.group(4)
            val codeGroup = matcher.group(6)

            when {
                boldGroup != null -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(boldGroup)
                    }
                }
                italicGroup != null -> {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(italicGroup)
                    }
                }
                codeGroup != null -> {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium
                        )
                    ) {
                        append(codeGroup)
                    }
                }
            }
            cursor = end
        }

        if (cursor < processedText.length) {
            append(processedText.substring(cursor))
        }
    }
}
