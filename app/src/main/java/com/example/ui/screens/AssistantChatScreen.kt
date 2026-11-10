package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.platform.LocalUriHandler
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.service.GeminiService
import com.example.ui.model.ActionStatus
import com.example.ui.model.ChatActionProposal
import com.example.ui.model.ChatMessage
import com.example.ui.model.MessageSender
import com.example.ui.viewmodel.AlcanciaViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.regex.Pattern

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

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Track which message IDs have already completed their progressive typewriter reveal
    val revealedMessageIds = remember { mutableStateListOf<String>() }
    val animationsEnabled = settings.hapticLevel != "OFF"

    // Auto-scroll to bottom when new messages arrive or when typing state changes
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

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Asistente",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Model Selector Chip (Tap opens bottom sheet)
                        FilterChip(
                            selected = true,
                            onClick = {
                                showModelBottomSheet = true
                            },
                            label = {
                                Text(
                                    text = currentModelLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Seleccionar modelo",
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("model_toggle_chip")
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("chat_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.resetChatConversation() },
                        modifier = Modifier.testTag("chat_new_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Nuevo chat",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                "Pregunta o pide algo...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_field"),
                        shape = RoundedCornerShape(26.dp),
                        maxLines = 5,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            unfocusedBorderColor = Color.Transparent
                        ),
                        keyboardOptions = KeyboardOptions.Default.copy(
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (inputText.isNotBlank() && !isTyping) {
                                    val textToSend = inputText
                                    inputText = ""
                                    viewModel.sendChatMessage(textToSend)
                                }
                            }
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Smooth transition between Send and Stop
                    AnimatedContent(
                        targetState = isTyping,
                        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                        label = "send_stop_button"
                    ) { typing ->
                        if (typing) {
                            IconButton(
                                onClick = { viewModel.stopGeneration() },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.errorContainer)
                                    .testTag("chat_stop_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "Detener",
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        } else {
                            val canSend = inputText.isNotBlank()
                            IconButton(
                                onClick = {
                                    if (canSend) {
                                        val textToSend = inputText
                                        inputText = ""
                                        viewModel.sendChatMessage(textToSend)
                                    }
                                },
                                enabled = canSend,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (canSend) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceContainerHigh
                                    )
                                    .testTag("chat_send_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Enviar",
                                    tint = if (canSend) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (messages.isEmpty() && !isTyping && errorMessage == null) {
                // Empty state (Gemini / ChatGPT style)
                EmptyChatGreeting(
                    onSuggestionClick = { suggestion ->
                        viewModel.sendChatMessage(suggestion)
                    }
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
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
                            PulsingThinkingIndicator()
                        }
                    }

                    if (errorMessage != null) {
                        item(key = "error_message_card") {
                            ChatErrorCard(
                                errorMessage = errorMessage ?: "",
                                errorDetail = errorDetail,
                                onRetry = { viewModel.retryLastTurn() },
                                onNewChat = { viewModel.resetChatConversation() }
                            )
                        }
                    }
                }
            }

            // Floating "↓" button when user scrolls up
            AnimatedVisibility(
                visible = canScrollForward,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable {
                            coroutineScope.launch {
                                val target = (messages.size - 1 + if (isTyping) 1 else 0).coerceAtLeast(0)
                                listState.animateScrollToItem(target)
                            }
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Ir al final",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }

    if (showModelBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showModelBottomSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 6.dp,
            dragHandle = {
                Surface(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 8.dp)
                        .size(width = 36.dp, height = 4.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                ) {}
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 28.dp)
            ) {
                Text(
                    text = "Modelo del Asistente",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Elige el modelo según tu tipo de consulta. Al cambiar de modelo se iniciará un nuevo chat.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Option 1: 3.5 Flash-Lite
                val isLiteActive = settings.cloudAiModel == GeminiService.DEFAULT_MODEL
                ModelOptionItem(
                    title = "3.5 Flash-Lite",
                    description = "Rápido y ligero, ideal para el día a día. Permite más usos.",
                    isActive = isLiteActive,
                    badge = "Predeterminado",
                    onClick = {
                        showModelBottomSheet = false
                        viewModel.selectCloudAiModel(GeminiService.DEFAULT_MODEL)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Cambiado a 3.5 Flash-Lite (Nuevo chat)")
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Option 2: 3.5 Flash
                val isFlashActive = settings.cloudAiModel == GeminiService.FLASH_MODEL
                ModelOptionItem(
                    title = "3.5 Flash",
                    description = "Más capaz con consultas largas, pero gasta más cuota.",
                    isActive = isFlashActive,
                    badge = null,
                    onClick = {
                        showModelBottomSheet = false
                        viewModel.selectCloudAiModel(GeminiService.FLASH_MODEL)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Cambiado a 3.5 Flash (Nuevo chat)")
                        }
                    }
                )

                Spacer(modifier = Modifier.height(22.dp))

                // Subtle footer
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
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Los límites de uso dependen de tu cuenta. Consúltalos en Google AI Studio.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyChatGreeting(
    onSuggestionClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "¿En qué te ayudo con tus ahorros?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
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
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            suggestions.forEach { suggestion ->
                SuggestionChip(
                    onClick = { onSuggestionClick(suggestion) },
                    label = {
                        Text(
                            text = suggestion,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .testTag("suggestion_chip_${suggestion.replace(" ", "_")}")
                )
            }
        }
    }
}

@Composable
private fun ChatMessageItem(
    message: ChatMessage,
    currencySymbol: String,
    animateTypewriter: Boolean,
    onTypewriterCompleted: () -> Unit,
    onCopyText: (String) -> Unit,
    onRegenerate: () -> Unit,
    onConfirmProposal: (ChatActionProposal) -> Unit,
    onCancelProposal: (ChatActionProposal) -> Unit
) {
    val isUser = message.sender == MessageSender.USER

    if (isUser) {
        // User Message: rounded bubble on the right
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 20.dp,
                    topEnd = 20.dp,
                    bottomStart = 20.dp,
                    bottomEnd = 4.dp
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .widthIn(max = 310.dp)
                    .pointerInput(message.text) {
                        detectTapGestures(
                            onLongPress = { onCopyText(message.text) }
                        )
                    }
                    .testTag("user_chat_bubble")
            ) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    lineHeight = 22.sp
                )
            }
        }
    } else {
        // Assistant Message: NO bubble, full width with small avatar on top (Gemini style)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("assistant_chat_message")
        ) {
            // Header with small avatar ✨
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Alcancía",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Message text with Markdown + progressive reveal
            if (message.text.isNotBlank()) {
                if (animateTypewriter) {
                    TypewriterMarkdownText(
                        fullText = message.text,
                        onComplete = onTypewriterCompleted,
                        onLongPress = { onCopyText(message.text) },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    MarkdownText(
                        markdown = message.text,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(message.text) {
                                detectTapGestures(
                                    onLongPress = { onCopyText(message.text) }
                                )
                            }
                    )
                }

                // Actions row under assistant message: Copy, Regenerate
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onCopyText(message.text) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copiar mensaje",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = onRegenerate,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Regenerar respuesta",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Action Proposal Card integrated into the chat
            if (message.actionProposal != null) {
                Spacer(modifier = Modifier.height(8.dp))
                ActionProposalCard(
                    proposal = message.actionProposal,
                    currencySymbol = currencySymbol,
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
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.pointerInput(fullText) {
            detectTapGestures(
                onTap = {
                    // Tap immediately displays the full text
                    revealedLength = fullText.length
                    onComplete()
                },
                onLongPress = { onLongPress() }
            )
        }
    )
}

private fun formatCardShortDate(rawDate: String?): String {
    if (rawDate.isNullOrBlank() || rawDate.equals("hoy", ignoreCase = true)) {
        val sdf = SimpleDateFormat("d 'de' MMM", Locale("es", "MX"))
        return sdf.format(Date()).replace(".", "")
    }
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val parsed = parser.parse(rawDate)
        if (parsed != null) {
            SimpleDateFormat("d 'de' MMM", Locale("es", "MX")).format(parsed).replace(".", "")
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
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val isDestructive = proposal.isDestructive
    val isPending = proposal.status == ActionStatus.PENDING
    val isExecuting = proposal.status == ActionStatus.EXECUTING
    val isConfirmed = proposal.status == ActionStatus.CONFIRMED
    val isCancelled = proposal.status == ActionStatus.CANCELLED

    // High contrast container & content colors
    val containerColor = if (isPending && isDestructive) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }

    val contentColor = if (isPending && isDestructive) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag("action_proposal_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isDestructive && isPending) Icons.Default.Warning else Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = if (isDestructive && isPending) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val dynamicTitle = when {
                        isPending -> "Confirmar acción"
                        isConfirmed -> "Acción realizada"
                        isCancelled -> "Acción cancelada"
                        else -> "Acción realizada"
                    }
                    Text(
                        text = dynamicTitle,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                }

                // Status Badge when resolved
                if (isConfirmed) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Confirmado ✓",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                } else if (isCancelled) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "Cancelado",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Summary / Description (cleaned of any technical IDs or placeholder artifacts)
            val cleanSummary = proposal.summary
                .replace(Regex("#[0-9]+"), "")
                .replace("Categoría actual", "Sin categoría")
                .replace("Sin nota", "")
                .replace(" en ''", "")
                .replace("  ", " ")
                .trim()

            Text(
                text = cleanSummary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = contentColor,
                lineHeight = 20.sp
            )

            // Extracted key details (Amount, Category, Date/Time, Note) - NEVER technical IDs!
            val amountVal = proposal.arguments["monto"] ?: proposal.arguments["montoObjetivo"] ?: proposal.arguments["precioEstimado"]
            val amountNum = amountVal?.toString()?.toDoubleOrNull()
            val rawCategory = proposal.arguments["categoria"]?.toString()
            val categoryVal = when {
                proposal.toolName in listOf("crear_meta", "editar_meta", "eliminar_meta", "aportar_a_meta", "retirar_de_meta") -> null
                rawCategory.isNullOrBlank() || rawCategory.contains("actual", ignoreCase = true) || rawCategory.equals("null", ignoreCase = true) -> "Sin categoría"
                else -> rawCategory
            }
            val rawDate = proposal.arguments["fecha"]?.toString() ?: proposal.arguments["fechaLimite"]?.toString()
            val dateVal = if (!rawDate.isNullOrBlank()) formatCardShortDate(rawDate) else null
            val rawNote = proposal.arguments["nota"]?.toString()
            val noteVal = rawNote?.takeIf {
                it.isNotBlank() && !it.equals("Sin nota", ignoreCase = true) && !it.equals("null", ignoreCase = true)
            }

            if (amountNum != null || categoryVal != null || dateVal != null || noteVal != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isPending && isDestructive) {
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (amountNum != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Monto:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "$currencySymbol${String.format(Locale.getDefault(), "%,.2f", amountNum)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        if (categoryVal != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Categoría:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = categoryVal,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        if (dateVal != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Fecha:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = dateVal,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        if (noteVal != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Nota:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = noteVal,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Single notice: "Esta acción no se puede deshacer" - only once and only while pending and destructive
            if (isPending && isDestructive) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Esta acción no se puede deshacer",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Result text if completed
            if (!proposal.resultText.isNullOrBlank() && !isPending) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = proposal.resultText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isConfirmed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Buttons while pending
            if (isPending) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("proposal_cancel_button")
                    ) {
                        Text("Cancelar")
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = onConfirm,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            contentColor = if (isDestructive) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("proposal_confirm_button")
                    ) {
                        Text("Confirmar")
                    }
                }
            } else if (isExecuting) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ejecutando acción...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        border = if (isActive) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
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
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                    if (badge != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = badge,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Surface(
                shape = CircleShape,
                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(24.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isActive) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Activo",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PulsingThinkingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_dots")

    val dot1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, delayMillis = 0), RepeatMode.Reverse),
        label = "d1"
    )
    val dot2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, delayMillis = 200), RepeatMode.Reverse),
        label = "d2"
    )
    val dot3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, delayMillis = 400), RepeatMode.Reverse),
        label = "d3"
    )
    val iconScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "iconScale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier
                .size(26.dp)
                .scale(iconScale)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = dot1Alpha))
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = dot2Alpha))
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = dot3Alpha))
            )
        }
    }
}

@Composable
private fun ChatErrorCard(
    errorMessage: String,
    errorDetail: String?,
    onRetry: () -> Unit,
    onNewChat: () -> Unit
) {
    var isDetailExpanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag("chat_error_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (errorMessage.contains("Clave", ignoreCase = true)) {
                        "Clave de API inválida"
                    } else if (errorMessage.contains("conexión", ignoreCase = true)) {
                        "Problema de conexión"
                    } else {
                        "Hubo un problema al responder"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.95f)
            )

            if (!errorDetail.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { isDetailExpanded = !isDetailExpanded }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isDetailExpanded) "Ocultar detalle técnico ▴" else "Ver detalle técnico ▾",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }

                if (isDetailExpanded) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorDetail,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onNewChat,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("error_new_chat_btn")
                ) {
                    Text("Nuevo chat")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onRetry,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("error_retry_btn")
                ) {
                    Text("Reintentar")
                }
            }
        }
    }
}

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    val styledText = remember(markdown) {
        parseMarkdownToAnnotatedString(markdown)
    }
    Text(
        text = styledText,
        style = MaterialTheme.typography.bodyLarge,
        color = color,
        modifier = modifier,
        lineHeight = 24.sp
    )
}

private fun parseMarkdownToAnnotatedString(input: String): AnnotatedString {
    // 1. Process headers and bullet lists line by line
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

    // 2. Parse inline **bold**, *italic*, and `code`
    return buildAnnotatedString {
        var cursor = 0
        // Group 2: bold, Group 4: italic, Group 6: inline code
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
