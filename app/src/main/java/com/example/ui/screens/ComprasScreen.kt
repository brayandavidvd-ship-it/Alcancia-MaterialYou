package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ShoppingItemEntity
import com.example.data.model.ShoppingListEntity
import com.example.ui.components.CurrencyUtils
import com.example.ui.components.RegisterShoppingExpenseSheet
import com.example.ui.theme.ColorblindExpenseColor
import com.example.ui.theme.ColorblindIncomeColor
import com.example.ui.theme.LocalNumberFontFamily
import com.example.ui.viewmodel.AlcanciaViewModel
import com.example.util.rememberVoiceDictationLauncher
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComprasScreen(
    viewModel: AlcanciaViewModel,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val shoppingLists by viewModel.shoppingListsState.collectAsStateWithLifecycle()
    val selectedListId by viewModel.selectedListId.collectAsStateWithLifecycle()
    val items by viewModel.currentShoppingItemsState.collectAsStateWithLifecycle()
    val listItemCounts by viewModel.shoppingListItemCountsState.collectAsStateWithLifecycle()
    val productSuggestions by viewModel.productSuggestions.collectAsStateWithLifecycle()
    val categories by viewModel.categoriesState.collectAsStateWithLifecycle()
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

    val numberFont = LocalNumberFontFamily.current
    val launchVoiceInput = rememberVoiceDictationLauncher(
        prompt = "Dime qué comprar…",
        onResult = { transcript -> viewModel.openAssistantChatWithVoice(transcript) },
        onUnavailable = { viewModel.openAssistantChatWithVoice(null) }
    )

    val currentList = remember(shoppingLists, selectedListId) {
        shoppingLists.firstOrNull { it.id == selectedListId } ?: shoppingLists.firstOrNull()
    }

    // Keep selectedListId synced if active list was deleted or missing
    LaunchedEffect(shoppingLists, selectedListId) {
        if (shoppingLists.isNotEmpty() && shoppingLists.none { it.id == selectedListId }) {
            viewModel.selectShoppingList(shoppingLists.first().id)
        }
    }

    val pendingItems = remember(items) { items.filter { !it.isPurchased } }
    val purchasedItems = remember(items) { items.filter { it.isPurchased } }

    val totalAmount = remember(items) { items.sumOf { it.quantity * it.unitPrice } }
    val purchasedAmount = remember(purchasedItems) { purchasedItems.sumOf { it.quantity * it.unitPrice } }
    val pendingAmount = remember(pendingItems) { pendingItems.sumOf { it.quantity * it.unitPrice } }

    // Modal & Dialog states
    var showNewListDialog by remember { mutableStateOf(false) }
    var showRenameListDialog by remember { mutableStateOf(false) }
    var showBudgetSheet by remember { mutableStateOf(false) }
    var showAddProductSheet by remember { mutableStateOf(false) }
    var showListSelectorSheet by remember { mutableStateOf(false) }
    var showRegisterExpenseSheet by remember { mutableStateOf(false) }
    var showListOptionsMenu by remember { mutableStateOf(false) }
    var listToDelete by remember { mutableStateOf<ShoppingListEntity?>(null) }

    // Item Context Menu states
    var itemContextMenu by remember { mutableStateOf<ShoppingItemEntity?>(null) }
    var itemToEdit by remember { mutableStateOf<ShoppingItemEntity?>(null) }
    var itemToDelete by remember { mutableStateOf<ShoppingItemEntity?>(null) }

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.bg)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = statusBarTop + 8.dp,
                bottom = navBarBottom + 162.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // B1. Encabezado
            item(key = "compras_header") {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Compras",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Medium,
                            color = palette.t1,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                onClick = {
                                    if (currentList != null) {
                                        shareListAsText(context, currentList, items, totalAmount, settings.currencySymbol)
                                    }
                                },
                                shape = CircleShape,
                                color = palette.s2,
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("compras_share_btn")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Compartir lista como texto",
                                        tint = palette.accent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Box {
                                Surface(
                                    onClick = { showListOptionsMenu = true },
                                    shape = CircleShape,
                                    color = palette.s2,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .testTag("compras_menu_btn")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "Opciones de lista",
                                            tint = palette.accent,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = showListOptionsMenu,
                                    onDismissRequest = { showListOptionsMenu = false },
                                    modifier = Modifier.background(palette.s2)
                                ) {
                                    DropdownMenuItem(
                                        leadingIcon = {
                                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = palette.t1)
                                        },
                                        text = { Text("Duplicar lista", color = palette.t1) },
                                        onClick = {
                                            if (currentList != null) {
                                                viewModel.duplicateShoppingList(currentList.id)
                                                scope.launch { snackbarHostState.showSnackbar("Lista duplicada") }
                                            }
                                            showListOptionsMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        leadingIcon = {
                                            Icon(Icons.Default.Edit, contentDescription = null, tint = palette.t1)
                                        },
                                        text = { Text("Renombrar", color = palette.t1) },
                                        onClick = {
                                            showRenameListDialog = true
                                            showListOptionsMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        leadingIcon = {
                                            Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = palette.t1)
                                        },
                                        text = { Text("Presupuesto de la lista", color = palette.t1) },
                                        onClick = {
                                            showBudgetSheet = true
                                            showListOptionsMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        leadingIcon = {
                                            Icon(Icons.Default.Add, contentDescription = null, tint = palette.t1)
                                        },
                                        text = { Text("Nueva lista", color = palette.t1) },
                                        onClick = {
                                            showNewListDialog = true
                                            showListOptionsMenu = false
                                        }
                                    )
                                    if (shoppingLists.size > 1 && currentList != null) {
                                        DropdownMenuItem(
                                            leadingIcon = {
                                                Icon(Icons.Default.Delete, contentDescription = null, tint = palette.gasto)
                                            },
                                            text = { Text("Eliminar lista", color = palette.gasto) },
                                            onClick = {
                                                listToDelete = currentList
                                                showListOptionsMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Selector de lista activa (cápsula 40.dp)
                    Surface(
                        onClick = { showListSelectorSheet = true },
                        shape = CircleShape,
                        color = palette.s2,
                        modifier = Modifier
                            .height(40.dp)
                            .testTag("list_selector_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = currentList?.name ?: "Mi lista",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = palette.accent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Seleccionar lista",
                                tint = palette.accent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // B2, B3, B4. Tarjeta de Total, Presupuesto y Botón Registrar como gasto
            item(key = "shopping_live_total_card") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = palette.s1
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Total en vivo",
                                    fontSize = 12.sp,
                                    color = palette.t2
                                )
                                Text(
                                    text = CurrencyUtils.formatAmount(totalAmount, settings.currencySymbol),
                                    fontSize = 34.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = numberFont,
                                    color = palette.t1,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.testTag("shopping_total_text")
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Comprado: ${CurrencyUtils.formatAmount(purchasedAmount, settings.currencySymbol)}",
                                    fontSize = 12.sp,
                                    fontFamily = numberFont,
                                    color = palette.ingreso,
                                    maxLines = 1
                                )
                                Text(
                                    text = "Por comprar: ${CurrencyUtils.formatAmount(pendingAmount, settings.currencySymbol)}",
                                    fontSize = 12.sp,
                                    fontFamily = numberFont,
                                    color = palette.t2,
                                    maxLines = 1
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Barra de progreso comprado vs total
                        val purchasedFraction = if (totalAmount > 0.0) {
                            (purchasedAmount / totalAmount).toFloat().coerceIn(0f, 1f)
                        } else {
                            0f
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(palette.s3)
                        ) {
                            if (purchasedFraction > 0f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(purchasedFraction)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(palette.brand)
                                )
                            }
                        }

                        // B3. Presupuesto (solo si la lista tiene presupuesto > 0)
                        val budget = currentList?.budget
                        if (budget != null && budget > 0.0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            val isOverBudget = totalAmount > budget
                            val diff = abs(budget - totalAmount)
                            val budgetFraction = (totalAmount / budget).toFloat().coerceIn(0f, 1f)
                            val budgetBarColor = if (isOverBudget) palette.gasto else palette.brand

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Presupuesto ${CurrencyUtils.formatAmount(budget, settings.currencySymbol)}",
                                    fontSize = 12.sp,
                                    fontFamily = numberFont,
                                    color = palette.t2
                                )
                                Text(
                                    text = if (isOverBudget) {
                                        "Te pasaste por ${CurrencyUtils.formatAmount(diff, settings.currencySymbol)}"
                                    } else {
                                        "Te quedan ${CurrencyUtils.formatAmount(diff, settings.currencySymbol)}"
                                    },
                                    fontSize = 12.sp,
                                    fontFamily = numberFont,
                                    color = if (isOverBudget) palette.gasto else palette.ingreso
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(palette.s3)
                            ) {
                                if (budgetFraction > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(budgetFraction)
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(budgetBarColor)
                                    )
                                }
                            }
                        }

                        // B4. Botón "Registrar como gasto" (solo si Comprado > 0)
                        if (purchasedAmount > 0.0) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                onClick = { showRegisterExpenseSheet = true },
                                shape = CircleShape,
                                color = palette.brand,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("register_as_expense_btn")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingCart,
                                        contentDescription = null,
                                        tint = palette.onBrand,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Registrar como gasto",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = palette.onBrand
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // B6. Sección "Por comprar ({n})"
            item(key = "section_pending_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Por comprar (${pendingItems.size})",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = palette.t1
                    )
                    if (pendingItems.isNotEmpty()) {
                        Text(
                            text = CurrencyUtils.formatAmount(pendingAmount, settings.currencySymbol),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = numberFont,
                            color = palette.t1
                        )
                    }
                }
            }

            if (pendingItems.isEmpty()) {
                item(key = "empty_pending") {
                    Text(
                        text = "Lista vacía. Agrega productos desde la barra de abajo.",
                        fontSize = 14.sp,
                        color = palette.t2,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            } else {
                item(key = "pending_items_group") {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        pendingItems.forEachIndexed { index, item ->
                            val shape = shoppingRowShape(index, pendingItems.size)
                            ShoppingItemRow(
                                item = item,
                                currencySymbol = settings.currencySymbol,
                                palette = palette,
                                shape = shape,
                                isPurchased = false,
                                onTogglePurchased = { viewModel.toggleShoppingItemPurchased(item) },
                                onQuantityChanged = { newQty ->
                                    if (newQty > 0) viewModel.updateShoppingItem(item.copy(quantity = newQty))
                                    else viewModel.deleteShoppingItem(item)
                                },
                                onLongPress = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    itemContextMenu = item
                                }
                            )
                        }
                    }
                }
            }

            // B6. Sección "Comprado ({n})"
            if (purchasedItems.isNotEmpty()) {
                item(key = "section_purchased_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Comprado (${purchasedItems.size})",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = palette.ingreso
                        )
                        Text(
                            text = CurrencyUtils.formatAmount(purchasedAmount, settings.currencySymbol),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = numberFont,
                            color = palette.ingreso
                        )
                    }
                }

                item(key = "purchased_items_group") {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        purchasedItems.forEachIndexed { index, item ->
                            val shape = shoppingRowShape(index, purchasedItems.size)
                            ShoppingItemRow(
                                item = item,
                                currencySymbol = settings.currencySymbol,
                                palette = palette,
                                shape = shape,
                                isPurchased = true,
                                onTogglePurchased = { viewModel.toggleShoppingItemPurchased(item) },
                                onQuantityChanged = { newQty ->
                                    if (newQty > 0) viewModel.updateShoppingItem(item.copy(quantity = newQty))
                                    else viewModel.deleteShoppingItem(item)
                                },
                                onLongPress = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    itemContextMenu = item
                                }
                            )
                        }
                    }
                }
            }
        }

        // SnackbarHost sobre la barra de IA
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 150.dp)
        )

        // B5. Barra de IA flotante sobre la cápsula de navegación (separación 12.dp)
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 86.dp)
                .fillMaxWidth()
                .height(56.dp)
                .testTag("ai_bar_compras"),
            shape = CircleShape,
            color = palette.s1,
            border = BorderStroke(1.5.dp, Brush.horizontalGradient(InicioPalette.AiGradient)),
            onClick = { viewModel.openAssistantChat() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Asistente de IA",
                    tint = palette.ahorro,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Dime qué comprar…",
                    fontSize = 14.sp,
                    color = palette.t2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                AiVoiceMicButton(
                    palette = palette,
                    onClick = { launchVoiceInput() },
                    modifier = Modifier.testTag("mic_bar_compras_btn")
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(palette.brand)
                        .clickable { showAddProductSheet = true }
                        .testTag("add_item_bar_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Añadir producto",
                        tint = palette.onBrand,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }

    // B7. Hoja "Añadir producto"
    if (showAddProductSheet) {
        AddProductBottomSheet(
            currencySymbol = settings.currencySymbol,
            productSuggestions = productSuggestions,
            palette = palette,
            onLookupLastPrice = { name -> viewModel.getLastUnitPriceForProduct(name) },
            onDismiss = { showAddProductSheet = false },
            onAdd = { name, qty, price ->
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                viewModel.addShoppingItem(name, qty, price)
                showAddProductSheet = false
            }
        )
    }

    // B8. Hoja "Presupuesto de la lista"
    if (showBudgetSheet && currentList != null) {
        ListBudgetBottomSheet(
            currentBudget = currentList.budget,
            currencySymbol = settings.currencySymbol,
            palette = palette,
            onDismiss = { showBudgetSheet = false },
            onSave = { newBudget ->
                viewModel.updateShoppingList(currentList.copy(budget = newBudget))
                showBudgetSheet = false
            }
        )
    }

    // B9. Selector de listas (hoja inferior)
    if (showListSelectorSheet) {
        ListSelectorBottomSheet(
            shoppingLists = shoppingLists,
            currentListId = currentList?.id,
            itemCounts = listItemCounts,
            palette = palette,
            onDismiss = { showListSelectorSheet = false },
            onSelectList = { listId ->
                viewModel.selectShoppingList(listId)
                showListSelectorSheet = false
            },
            onNewList = {
                showListSelectorSheet = false
                showNewListDialog = true
            }
        )
    }

    // B9. Diálogo de confirmación para eliminar lista
    if (listToDelete != null) {
        val target = listToDelete!!
        AlertDialog(
            onDismissRequest = { listToDelete = null },
            containerColor = palette.s1,
            titleContentColor = palette.t1,
            textContentColor = palette.t2,
            title = { Text("¿Eliminar lista?") },
            text = { Text("Se eliminará \"${target.name}\" y todos sus productos.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteShoppingList(target)
                        listToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.gasto),
                    shape = CircleShape
                ) {
                    Text("Eliminar", color = palette.onBrand, fontWeight = FontWeight.Medium)
                }
            },
            dismissButton = {
                TextButton(onClick = { listToDelete = null }) {
                    Text("Cancelar", color = palette.t2)
                }
            }
        )
    }

    // Long-press Context Menu for Product: Editar, Duplicar, Eliminar (BLOQUE G)
    if (itemContextMenu != null) {
        val item = itemContextMenu!!
        Dialog(onDismissRequest = { itemContextMenu = null }) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = palette.s1,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = item.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = palette.t1,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    val sub = CurrencyUtils.formatAmount(item.subtotal, settings.currencySymbol)
                    Text(
                        text = "${item.quantity} × ${CurrencyUtils.formatAmount(item.unitPrice, settings.currencySymbol)} = $sub",
                        fontSize = 14.sp,
                        fontFamily = numberFont,
                        color = palette.t2
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    BoxWithConstraints(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val singleButtonWidth = (maxWidth - 16.dp) / 3f
                        val showIcons = singleButtonWidth >= 96.dp

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                onClick = {
                                    val target = item
                                    itemContextMenu = null
                                    itemToEdit = target
                                },
                                shape = CircleShape,
                                color = palette.s3,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    if (showIcons) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = null,
                                            tint = palette.accent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = "Editar",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = palette.t1,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Surface(
                                onClick = {
                                    viewModel.duplicateShoppingItem(item)
                                    itemContextMenu = null
                                    scope.launch { snackbarHostState.showSnackbar("Producto duplicado") }
                                },
                                shape = CircleShape,
                                color = palette.s3,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    if (showIcons) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = null,
                                            tint = palette.accent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = "Duplicar",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = palette.t1,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Surface(
                                onClick = {
                                    val target = item
                                    itemContextMenu = null
                                    itemToDelete = target
                                },
                                shape = CircleShape,
                                color = palette.gasto.copy(alpha = 0.18f),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    if (showIcons) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = palette.gasto,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = "Eliminar",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = palette.gasto,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { itemContextMenu = null },
                            modifier = Modifier.heightIn(min = 40.dp)
                        ) {
                            Text(
                                text = "Cancelar",
                                fontSize = 14.sp,
                                color = palette.t2
                            )
                        }
                    }
                }
            }
        }
    }

    // Delete item confirmation dialog
    if (itemToDelete != null) {
        val target = itemToDelete!!
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            containerColor = palette.s1,
            titleContentColor = palette.t1,
            textContentColor = palette.t2,
            title = { Text("¿Borrar ${target.name}?") },
            text = { Text("Se quitará de la lista de compras.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteShoppingItem(target)
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.gasto)
                ) {
                    Text("Borrar", color = palette.onBrand, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancelar", color = palette.t2)
                }
            }
        )
    }

    // Edit item dialog
    if (itemToEdit != null) {
        val target = itemToEdit!!
        EditShoppingItemDialog(
            item = target,
            currencySymbol = settings.currencySymbol,
            palette = palette,
            onDismiss = { itemToEdit = null },
            onSave = { newName, newQty, newPrice ->
                viewModel.updateShoppingItem(
                    target.copy(name = newName, quantity = newQty, unitPrice = newPrice)
                )
                itemToEdit = null
            }
        )
    }

    // Dialog: Crear nueva lista
    if (showNewListDialog) {
        var listNameInput by remember { mutableStateOf("") }
        var listBudgetInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewListDialog = false },
            containerColor = palette.s1,
            titleContentColor = palette.t1,
            textContentColor = palette.t2,
            title = { Text("Nueva lista de compras") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = listNameInput,
                        onValueChange = { listNameInput = it },
                        label = { Text("Nombre de la lista") },
                        placeholder = { Text("Ej. Despensa semanal, Farmacia") },
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = palette.t1,
                            unfocusedTextColor = palette.t1,
                            focusedBorderColor = palette.brand,
                            unfocusedBorderColor = palette.s3,
                            focusedLabelColor = palette.accent,
                            unfocusedLabelColor = palette.t2
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = listBudgetInput,
                        onValueChange = { listBudgetInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Presupuesto opcional (${settings.currencySymbol})") },
                        placeholder = { Text("Ej. 1000") },
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = palette.t1,
                            unfocusedTextColor = palette.t1,
                            focusedBorderColor = palette.brand,
                            unfocusedBorderColor = palette.s3,
                            focusedLabelColor = palette.accent,
                            unfocusedLabelColor = palette.t2
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (listNameInput.isNotBlank()) {
                            val budget = listBudgetInput.toDoubleOrNull()?.takeIf { it > 0.0 }
                            viewModel.createShoppingList(listNameInput, budget)
                            showNewListDialog = false
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = palette.brand, contentColor = palette.onBrand)
                ) {
                    Text("Crear lista")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewListDialog = false }) {
                    Text("Cancelar", color = palette.t2)
                }
            }
        )
    }

    // Dialog: Renombrar lista
    if (showRenameListDialog && currentList != null) {
        var renameInput by remember { mutableStateOf(currentList.name) }
        AlertDialog(
            onDismissRequest = { showRenameListDialog = false },
            containerColor = palette.s1,
            titleContentColor = palette.t1,
            textContentColor = palette.t2,
            title = { Text("Renombrar lista") },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text("Nombre") },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = palette.t1,
                        unfocusedTextColor = palette.t1,
                        focusedBorderColor = palette.brand,
                        unfocusedBorderColor = palette.s3,
                        focusedLabelColor = palette.accent,
                        unfocusedLabelColor = palette.t2
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameInput.isNotBlank()) {
                            viewModel.renameShoppingList(currentList, renameInput)
                            showRenameListDialog = false
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = palette.brand, contentColor = palette.onBrand)
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameListDialog = false }) {
                    Text("Cancelar", color = palette.t2)
                }
            }
        )
    }

    // Register shopping as expense modal (existing logic unchanged)
    if (showRegisterExpenseSheet && currentList != null) {
        RegisterShoppingExpenseSheet(
            categories = categories,
            currencySymbol = settings.currencySymbol,
            listName = currentList.name,
            purchasedTotal = purchasedAmount,
            fullTotal = totalAmount,
            onDismiss = { showRegisterExpenseSheet = false },
            onSave = { amount, category, note, dateMillis, repeat, timeString ->
                viewModel.registerShoppingListAsExpense(
                    category = category,
                    totalAmount = amount,
                    note = note,
                    dateMillis = dateMillis,
                    repeatOption = repeat,
                    timeString = timeString,
                    onSuccess = {
                        showRegisterExpenseSheet = false
                        scope.launch { snackbarHostState.showSnackbar("Gasto registrado en Inicio correctamente") }
                    }
                )
            }
        )
    }
}

private fun shoppingRowShape(index: Int, totalCount: Int): Shape {
    return when {
        totalCount <= 1 -> RoundedCornerShape(20.dp)
        index == 0 -> RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 6.dp, bottomEnd = 6.dp)
        index == totalCount - 1 -> RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
        else -> RoundedCornerShape(6.dp)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ShoppingItemRow(
    item: ShoppingItemEntity,
    currencySymbol: String,
    palette: InicioPalette.Colors,
    shape: Shape,
    isPurchased: Boolean,
    onTogglePurchased: () -> Unit,
    onQuantityChanged: (Double) -> Unit,
    onLongPress: () -> Unit
) {
    val numberFont = LocalNumberFontFamily.current
    val qtyFormatted = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()
    val priceFormatted = CurrencyUtils.formatAmount(item.unitPrice, currencySymbol)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(palette.s1)
            .combinedClickable(
                onClick = onTogglePurchased,
                onLongClick = onLongPress
            )
            .padding(12.dp)
            .testTag("shopping_item_${item.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Zona táctil mínima 40.dp alrededor del círculo check visual de 28.dp
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { onTogglePurchased() },
                    contentAlignment = Alignment.Center
                ) {
                    if (isPurchased) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(palette.ingreso),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Marcar pendiente",
                                tint = palette.onBrand,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, palette.t2, CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isPurchased) palette.t2 else palette.t1,
                        textDecoration = if (isPurchased) TextDecoration.LineThrough else null,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$qtyFormatted × $priceFormatted",
                        fontSize = 11.sp,
                        fontFamily = numberFont,
                        color = palette.t2,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { onQuantityChanged(item.quantity - 1.0) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Menos",
                        tint = palette.t2,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { onQuantityChanged(item.quantity + 1.0) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Más",
                        tint = palette.t2,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = CurrencyUtils.formatAmount(item.subtotal, currencySymbol),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = numberFont,
                    color = if (isPurchased) palette.t2 else palette.t1
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddProductBottomSheet(
    currencySymbol: String,
    productSuggestions: List<String>,
    palette: InicioPalette.Colors,
    onLookupLastPrice: suspend (String) -> Double?,
    onDismiss: () -> Unit,
    onAdd: (name: String, qty: Double, price: Double) -> Unit
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var newProductName by remember { mutableStateOf("") }
    var newProductQty by remember { mutableStateOf("1") }
    var newProductPrice by remember { mutableStateOf("") }

    val matchingSuggestions = remember(newProductName, productSuggestions) {
        if (newProductName.isBlank()) {
            productSuggestions.take(4)
        } else {
            productSuggestions.filter { it.contains(newProductName, ignoreCase = true) }.take(4)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = palette.s1,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(palette.s3)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Añadir producto",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = palette.t1
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = newProductName,
                onValueChange = { input ->
                    newProductName = input
                    scope.launch {
                        val lastPrice = onLookupLastPrice(input)
                        if (lastPrice != null && lastPrice > 0.0 && newProductPrice.isBlank()) {
                            newProductPrice = if (lastPrice % 1.0 == 0.0) lastPrice.toInt().toString() else lastPrice.toString()
                        }
                    }
                },
                label = { Text("Producto") },
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = palette.t1,
                    unfocusedTextColor = palette.t1,
                    focusedContainerColor = palette.s2,
                    unfocusedContainerColor = palette.s2,
                    focusedBorderColor = palette.brand,
                    unfocusedBorderColor = palette.s3,
                    focusedLabelColor = palette.accent,
                    unfocusedLabelColor = palette.t2
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("product_name_input")
            )

            if (matchingSuggestions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (suggestion in matchingSuggestions) {
                        FilterChip(
                            selected = false,
                            onClick = {
                                newProductName = suggestion
                                scope.launch {
                                    val lastPrice = onLookupLastPrice(suggestion)
                                    if (lastPrice != null && lastPrice > 0.0) {
                                        newProductPrice = if (lastPrice % 1.0 == 0.0) lastPrice.toInt().toString() else lastPrice.toString()
                                    }
                                }
                            },
                            label = {
                                Text(
                                    text = suggestion,
                                    fontSize = 12.sp,
                                    color = palette.t1
                                )
                            },
                            shape = CircleShape,
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = palette.s2,
                                labelColor = palette.t1
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = false,
                                borderColor = palette.s3
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newProductQty,
                    onValueChange = { newProductQty = it },
                    label = { Text("Cant.") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = palette.t1,
                        unfocusedTextColor = palette.t1,
                        focusedContainerColor = palette.s2,
                        unfocusedContainerColor = palette.s2,
                        focusedBorderColor = palette.brand,
                        unfocusedBorderColor = palette.s3,
                        focusedLabelColor = palette.accent,
                        unfocusedLabelColor = palette.t2
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("product_qty_input")
                )

                OutlinedTextField(
                    value = newProductPrice,
                    onValueChange = { newProductPrice = it },
                    label = { Text("Precio U.") },
                    placeholder = { Text("0.00", color = palette.t2) },
                    prefix = { Text(currencySymbol, color = palette.t2) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = palette.t1,
                        unfocusedTextColor = palette.t1,
                        focusedContainerColor = palette.s2,
                        unfocusedContainerColor = palette.s2,
                        focusedBorderColor = palette.brand,
                        unfocusedBorderColor = palette.s3,
                        focusedLabelColor = palette.accent,
                        unfocusedLabelColor = palette.t2
                    ),
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("product_price_input")
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = {
                    if (newProductName.isNotBlank()) {
                        val qty = newProductQty.toDoubleOrNull() ?: 1.0
                        val price = newProductPrice.toDoubleOrNull() ?: 0.0
                        onAdd(newProductName, qty, price)
                    }
                },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.brand,
                    contentColor = palette.onBrand
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("add_item_btn")
            ) {
                Text(
                    text = "Agregar",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = palette.onBrand
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListBudgetBottomSheet(
    currentBudget: Double?,
    currencySymbol: String,
    palette: InicioPalette.Colors,
    onDismiss: () -> Unit,
    onSave: (Double?) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val numberFont = LocalNumberFontFamily.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val hasExistingBudget = currentBudget != null && currentBudget > 0.0
    var amountString by remember {
        mutableStateOf(
            if (hasExistingBudget) {
                val b = currentBudget!!
                if (b % 1.0 == 0.0) b.toLong().toString() else b.toString()
            } else {
                "0"
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = palette.s1,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(palette.s3)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Presupuesto de la lista",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = palette.t1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Dinero exacto que quieres gastar en esta lista",
                fontSize = 12.sp,
                color = palette.t2,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "$currencySymbol$amountString",
                fontSize = 40.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = numberFont,
                color = palette.t1,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            val keyRows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf(".", "0", "DEL")
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (row in keyRows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (key in row) {
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        when (key) {
                                            "DEL" -> {
                                                amountString = if (amountString.length > 1) {
                                                    amountString.dropLast(1)
                                                } else {
                                                    "0"
                                                }
                                            }
                                            "." -> {
                                                if (!amountString.contains(".")) {
                                                    amountString += "."
                                                }
                                            }
                                            else -> {
                                                if (amountString == "0") {
                                                    amountString = key
                                                } else {
                                                    val dotIdx = amountString.indexOf('.')
                                                    if (dotIdx == -1 || amountString.length - dotIdx <= 2) {
                                                        if (amountString.length < 9) {
                                                            amountString += key
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    },
                                shape = RoundedCornerShape(16.dp),
                                color = palette.s2
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (key == "DEL") {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                                            contentDescription = "Borrar",
                                            tint = palette.t1,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    } else {
                                        Text(
                                            text = key,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = palette.t1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val parsed = amountString.toDoubleOrNull() ?: 0.0
                    onSave(if (parsed > 0.0) parsed else null)
                },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.brand,
                    contentColor = palette.onBrand
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = "Guardar",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = palette.onBrand
                )
            }

            if (hasExistingBudget) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    onClick = { onSave(null) },
                    shape = CircleShape,
                    color = palette.s3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Quitar presupuesto",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = palette.t1
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListSelectorBottomSheet(
    shoppingLists: List<ShoppingListEntity>,
    currentListId: Long?,
    itemCounts: Map<Long, Int>,
    palette: InicioPalette.Colors,
    onDismiss: () -> Unit,
    onSelectList: (Long) -> Unit,
    onNewList: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = palette.s1,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(palette.s3)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (list in shoppingLists) {
                val isSelected = list.id == currentListId
                val count = itemCounts[list.id] ?: 0
                Surface(
                    onClick = { onSelectList(list.id) },
                    shape = RoundedCornerShape(16.dp),
                    color = palette.s2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = list.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = palette.t1,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "$count productos",
                                fontSize = 11.sp,
                                color = palette.t2
                            )
                        }

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Lista activa",
                                tint = palette.accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Button(
                onClick = onNewList,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.brand,
                    contentColor = palette.onBrand
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = palette.onBrand,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Nueva lista",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = palette.onBrand
                )
            }
        }
    }
}

@Composable
private fun EditShoppingItemDialog(
    item: ShoppingItemEntity,
    currencySymbol: String,
    palette: InicioPalette.Colors,
    onDismiss: () -> Unit,
    onSave: (name: String, qty: Double, price: Double) -> Unit
) {
    var name by remember { mutableStateOf(item.name) }
    var qtyStr by remember { mutableStateOf(if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()) }
    var priceStr by remember { mutableStateOf(if (item.unitPrice % 1.0 == 0.0) item.unitPrice.toInt().toString() else item.unitPrice.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.s1,
        titleContentColor = palette.t1,
        textContentColor = palette.t2,
        title = { Text("Editar producto") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Producto") },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = palette.t1,
                        unfocusedTextColor = palette.t1,
                        focusedBorderColor = palette.brand,
                        unfocusedBorderColor = palette.s3,
                        focusedLabelColor = palette.accent,
                        unfocusedLabelColor = palette.t2
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = qtyStr,
                    onValueChange = { qtyStr = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Cantidad") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = palette.t1,
                        unfocusedTextColor = palette.t1,
                        focusedBorderColor = palette.brand,
                        unfocusedBorderColor = palette.s3,
                        focusedLabelColor = palette.accent,
                        unfocusedLabelColor = palette.t2
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = priceStr,
                    onValueChange = { priceStr = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Precio Unitario ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = palette.t1,
                        unfocusedTextColor = palette.t1,
                        focusedBorderColor = palette.brand,
                        unfocusedBorderColor = palette.s3,
                        focusedLabelColor = palette.accent,
                        unfocusedLabelColor = palette.t2
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val q = qtyStr.toDoubleOrNull() ?: item.quantity
                    val p = priceStr.toDoubleOrNull() ?: item.unitPrice
                    if (name.isNotBlank()) {
                        onSave(name.trim(), q, p)
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = palette.brand, contentColor = palette.onBrand)
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = palette.t2)
            }
        }
    )
}

private fun shareListAsText(
    context: android.content.Context,
    list: ShoppingListEntity,
    items: List<ShoppingItemEntity>,
    total: Double,
    currencySymbol: String
) {
    val sb = StringBuilder()
    sb.append("🛒 Lista de compras: ${list.name}\n\n")

    val pending = items.filter { !it.isPurchased }
    if (pending.isNotEmpty()) {
        sb.append("📋 Por comprar:\n")
        for (item in pending) {
            val qty = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()
            val price = CurrencyUtils.formatAmount(item.unitPrice, currencySymbol)
            val sub = CurrencyUtils.formatAmount(item.subtotal, currencySymbol)
            sb.append("  [ ] ${item.name} ($qty × $price = $sub)\n")
        }
        sb.append("\n")
    }

    val purchased = items.filter { it.isPurchased }
    if (purchased.isNotEmpty()) {
        sb.append("✅ Comprado:\n")
        for (item in purchased) {
            val qty = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()
            val sub = CurrencyUtils.formatAmount(item.subtotal, currencySymbol)
            sb.append("  [x] ${item.name} ($qty = $sub)\n")
        }
        sb.append("\n")
    }

    sb.append("Total: ${CurrencyUtils.formatAmount(total, currencySymbol)}\n")
    if (list.budget != null && list.budget > 0) {
        sb.append("Presupuesto: ${CurrencyUtils.formatAmount(list.budget, currencySymbol)}\n")
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Lista: ${list.name}")
        putExtra(Intent.EXTRA_TEXT, sb.toString())
    }
    val chooser = Intent.createChooser(intent, "Compartir lista de compras")
    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(chooser)
}
