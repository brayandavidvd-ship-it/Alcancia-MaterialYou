package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RadioButtonUnchecked
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ShoppingItemEntity
import com.example.data.model.ShoppingListEntity
import com.example.ui.components.AlcanciaCard
import com.example.ui.components.CurrencyUtils
import com.example.ui.components.RegisterShoppingExpenseSheet
import com.example.ui.theme.LocalExpenseColor
import com.example.ui.theme.LocalIncomeColor
import com.example.ui.theme.LocalIsCompactDensity
import com.example.ui.theme.LocalNumberFontFamily
import com.example.ui.viewmodel.AlcanciaViewModel
import kotlinx.coroutines.launch

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
    val productSuggestions by viewModel.productSuggestions.collectAsStateWithLifecycle()
    val categories by viewModel.categoriesState.collectAsStateWithLifecycle()
    val settings by viewModel.settingsState.collectAsStateWithLifecycle()

    val incomeColor = LocalIncomeColor.current
    val expenseColor = LocalExpenseColor.current
    val numberFont = LocalNumberFontFamily.current
    val isCompact = LocalIsCompactDensity.current

    val currentList = remember(shoppingLists, selectedListId) {
        shoppingLists.firstOrNull { it.id == selectedListId } ?: shoppingLists.firstOrNull()
    }

    // Split items into "Por comprar" and "Comprado"
    val pendingItems = remember(items) { items.filter { !it.isPurchased } }
    val purchasedItems = remember(items) { items.filter { it.isPurchased } }

    val totalAmount = remember(items) { items.sumOf { it.quantity * it.unitPrice } }
    val purchasedAmount = remember(items) { purchasedItems.sumOf { it.quantity * it.unitPrice } }
    val pendingAmount = remember(items) { pendingItems.sumOf { it.quantity * it.unitPrice } }

    // Dialog states
    var showNewListDialog by remember { mutableStateOf(false) }
    var showRenameListDialog by remember { mutableStateOf(false) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var showRegisterExpenseSheet by remember { mutableStateOf(false) }
    var showListsDropdown by remember { mutableStateOf(false) }
    var showListOptionsMenu by remember { mutableStateOf(false) }

    // Item Context Menu states
    var itemContextMenu by remember { mutableStateOf<ShoppingItemEntity?>(null) }
    var itemToEdit by remember { mutableStateOf<ShoppingItemEntity?>(null) }
    var itemToDelete by remember { mutableStateOf<ShoppingItemEntity?>(null) }

    // Quick add product fields
    var newProductName by remember { mutableStateOf("") }
    var newProductQty by remember { mutableStateOf("1") }
    var newProductPrice by remember { mutableStateOf("") }

    // Matching suggestions
    val matchingSuggestions = remember(newProductName, productSuggestions) {
        if (newProductName.isBlank()) {
            productSuggestions.take(5)
        } else {
            productSuggestions.filter { it.contains(newProductName, ignoreCase = true) }.take(5)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            // Live Total and Register Button fixed at bottom
            Surface(
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Total en vivo",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val formattedTotal = CurrencyUtils.formatAmount(totalAmount, settings.currencySymbol)
                            Text(
                                text = formattedTotal,
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = numberFont
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.testTag("shopping_total_text")
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Comprado: ${CurrencyUtils.formatAmount(purchasedAmount, settings.currencySymbol)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = numberFont),
                                color = incomeColor,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Por comprar: ${CurrencyUtils.formatAmount(pendingAmount, settings.currencySymbol)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = numberFont),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            if (totalAmount > 0) {
                                showRegisterExpenseSheet = true
                            }
                        },
                        enabled = totalAmount > 0,
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("register_as_expense_btn")
                    ) {
                        Icon(imageVector = Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Registrar como gasto",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
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
                top = 16.dp,
                bottom = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(if (isCompact) 12.dp else 16.dp)
        ) {
            // Header: List Selector & Actions (Share, Duplicate list, Rename, New)
            item(key = "list_header") {
                AlcanciaCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable { showListsDropdown = true }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.List,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = currentList?.name ?: "Mi lista",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                DropdownMenu(
                                    expanded = showListsDropdown,
                                    onDismissRequest = { showListsDropdown = false },
                                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                ) {
                                    for (list in shoppingLists) {
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = list.name,
                                                    fontWeight = if (list.id == currentList?.id) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            onClick = {
                                                viewModel.selectShoppingList(list.id)
                                                showListsDropdown = false
                                            }
                                        )
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        if (currentList != null) {
                                            shareListAsText(context, currentList, items, totalAmount, settings.currencySymbol)
                                        }
                                    }
                                ) {
                                    Icon(imageVector = Icons.Default.Share, contentDescription = "Compartir lista como texto")
                                }

                                Box {
                                    IconButton(onClick = { showListOptionsMenu = true }) {
                                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Opciones de lista")
                                    }

                                    DropdownMenu(
                                        expanded = showListOptionsMenu,
                                        onDismissRequest = { showListOptionsMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                                            text = { Text("Duplicar lista") },
                                            onClick = {
                                                if (currentList != null) {
                                                    viewModel.duplicateShoppingList(currentList.id)
                                                    scope.launch { snackbarHostState.showSnackbar("Lista duplicada") }
                                                }
                                                showListOptionsMenu = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                            text = { Text("Renombrar") },
                                            onClick = {
                                                showRenameListDialog = true
                                                showListOptionsMenu = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            leadingIcon = { Icon(Icons.Default.ShoppingCart, contentDescription = null) },
                                            text = { Text("Fijar presupuesto") },
                                            onClick = {
                                                showBudgetDialog = true
                                                showListOptionsMenu = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                                            text = { Text("Nueva lista") },
                                            onClick = {
                                                showNewListDialog = true
                                                showListOptionsMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Presupuesto opcional con barra que cambia de color al 80% y >100%
                        if (currentList?.budget != null && currentList.budget > 0.0) {
                            Spacer(modifier = Modifier.height(10.dp))
                            val budget = currentList.budget
                            val ratio = (totalAmount / budget).toFloat()
                            val barColor = when {
                                ratio >= 1.0f -> expenseColor
                                ratio >= 0.8f -> Color(0xFFF57C00) // Amber / warning
                                else -> MaterialTheme.colorScheme.primary
                            }

                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Presupuesto: ${CurrencyUtils.formatAmount(budget, settings.currencySymbol)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    val percentInt = (ratio * 100).toInt()
                                    Text(
                                        text = "$percentInt%",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = barColor
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(ratio.coerceIn(0.02f, 1f))
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(barColor)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Quick Add Product Card with Price Memory & Suggestions
            item(key = "add_product_card") {
                AlcanciaCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Añadir producto",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = newProductName,
                            onValueChange = { input ->
                                newProductName = input
                                scope.launch {
                                    val lastPrice = viewModel.getLastUnitPriceForProduct(input)
                                    if (lastPrice != null && lastPrice > 0.0 && newProductPrice.isBlank()) {
                                        newProductPrice = if (lastPrice % 1.0 == 0.0) lastPrice.toInt().toString() else lastPrice.toString()
                                    }
                                }
                            },
                            label = { Text("Producto (ej. Leche, Manzanas)") },
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("product_name_input")
                        )

                        // Suggestions Chips (Price memory)
                        if (matchingSuggestions.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                for (suggestion in matchingSuggestions) {
                                    FilterChip(
                                        selected = false,
                                        onClick = {
                                            newProductName = suggestion
                                            scope.launch {
                                                val lastPrice = viewModel.getLastUnitPriceForProduct(suggestion)
                                                if (lastPrice != null && lastPrice > 0.0) {
                                                    newProductPrice = if (lastPrice % 1.0 == 0.0) lastPrice.toInt().toString() else lastPrice.toString()
                                                }
                                            }
                                        },
                                        label = { Text(suggestion, style = MaterialTheme.typography.labelSmall) },
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

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
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("product_qty_input")
                            )

                            OutlinedTextField(
                                value = newProductPrice,
                                onValueChange = { newProductPrice = it },
                                label = { Text("Precio U.") },
                                placeholder = { Text("0.00") },
                                prefix = { Text(settings.currencySymbol) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = RoundedCornerShape(16.dp),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1.3f)
                                    .testTag("product_price_input")
                            )

                            Button(
                                onClick = {
                                    val qty = newProductQty.toDoubleOrNull() ?: 1.0
                                    val price = newProductPrice.toDoubleOrNull() ?: 0.0
                                    if (newProductName.isNotBlank()) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.addShoppingItem(newProductName, qty, price)
                                        newProductName = ""
                                        newProductQty = "1"
                                        newProductPrice = ""
                                    }
                                },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .height(54.dp)
                                    .testTag("add_item_btn")
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "Agregar producto")
                            }
                        }
                    }
                }
            }

            // SECTION 1: Por comprar
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
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (pendingItems.isNotEmpty()) {
                        Text(
                            text = CurrencyUtils.formatAmount(pendingAmount, settings.currencySymbol),
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = numberFont),
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (pendingItems.isEmpty()) {
                item(key = "empty_pending") {
                    Text(
                        text = if (items.isEmpty()) "Lista vacía. Agrega productos arriba." else "¡Todo comprado en esta lista! 🎉",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            } else {
                items(items = pendingItems, key = { it.id }) { item ->
                    ShoppingItemRow(
                        item = item,
                        currencySymbol = settings.currencySymbol,
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

            // SECTION 2: Comprado (moved to bottom, dimmed)
            if (purchasedItems.isNotEmpty()) {
                item(key = "section_purchased_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Comprado (${purchasedItems.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = incomeColor
                        )
                        Text(
                            text = CurrencyUtils.formatAmount(purchasedAmount, settings.currencySymbol),
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = numberFont),
                            fontWeight = FontWeight.SemiBold,
                            color = incomeColor
                        )
                    }
                }

                items(items = purchasedItems, key = { it.id }) { item ->
                    ShoppingItemRow(
                        item = item,
                        currencySymbol = settings.currencySymbol,
                        isDimmed = true,
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

            item(key = "bottom_space_shopping") {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // Long-press Context Menu for Product: Editar, Duplicar, Borrar
    if (itemContextMenu != null) {
        val item = itemContextMenu!!
        AlertDialog(
            onDismissRequest = { itemContextMenu = null },
            title = { Text(item.name, fontWeight = FontWeight.Bold) },
            text = {
                val sub = CurrencyUtils.formatAmount(item.subtotal, settings.currencySymbol)
                Text("${item.quantity} × ${CurrencyUtils.formatAmount(item.unitPrice, settings.currencySymbol)} = $sub")
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        val target = item
                        itemContextMenu = null
                        itemToEdit = target
                    }) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Editar")
                    }

                    TextButton(onClick = {
                        viewModel.duplicateShoppingItem(item)
                        itemContextMenu = null
                        scope.launch { snackbarHostState.showSnackbar("Producto duplicado") }
                    }) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Duplicar")
                    }

                    Button(
                        onClick = {
                            val target = item
                            itemContextMenu = null
                            itemToDelete = target
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = expenseColor)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Borrar", color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { itemContextMenu = null }) { Text("Cancelar") }
            }
        )
    }

    // Delete item confirmation dialog
    if (itemToDelete != null) {
        val target = itemToDelete!!
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("¿Borrar ${target.name}?") },
            text = { Text("Se quitará de la lista de compras.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteShoppingItem(target)
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = expenseColor)
                ) {
                    Text("Borrar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { itemToDelete = null }) { Text("Cancelar") } }
        )
    }

    // Edit item dialog
    if (itemToEdit != null) {
        val target = itemToEdit!!
        EditShoppingItemDialog(
            item = target,
            currencySymbol = settings.currencySymbol,
            onDismiss = { itemToEdit = null },
            onSave = { newName, newQty, newPrice ->
                viewModel.updateShoppingItem(
                    target.copy(name = newName, quantity = newQty, unitPrice = newPrice)
                )
                itemToEdit = null
            }
        )
    }

    // Budget Dialog
    if (showBudgetDialog && currentList != null) {
        var budgetInput by remember {
            val b = currentList.budget
            mutableStateOf(if (b != null && b > 0) b.toInt().toString() else "")
        }
        AlertDialog(
            onDismissRequest = { showBudgetDialog = false },
            title = { Text("Presupuesto de la lista") },
            text = {
                Column {
                    Text("Define un tope de gasto para ver el progreso y advertencias de color.", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = budgetInput,
                        onValueChange = { budgetInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Presupuesto límite (${settings.currencySymbol})") },
                        placeholder = { Text("Ej. 1500") },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = budgetInput.toDoubleOrNull()
                        viewModel.updateShoppingList(currentList.copy(budget = if (parsed != null && parsed > 0) parsed else null))
                        showBudgetDialog = false
                    },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Guardar presupuesto")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBudgetDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // Dialog: Crear nueva lista
    if (showNewListDialog) {
        var listNameInput by remember { mutableStateOf("") }
        var listBudgetInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewListDialog = false },
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
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = listBudgetInput,
                        onValueChange = { listBudgetInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Presupuesto opcional (${settings.currencySymbol})") },
                        placeholder = { Text("Ej. 1000") },
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (listNameInput.isNotBlank()) {
                            val budget = listBudgetInput.toDoubleOrNull()
                            viewModel.createShoppingList(listNameInput, budget)
                            showNewListDialog = false
                        }
                    },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Crear lista")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewListDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // Dialog: Renombrar lista
    if (showRenameListDialog && currentList != null) {
        var renameInput by remember { mutableStateOf(currentList.name) }
        AlertDialog(
            onDismissRequest = { showRenameListDialog = false },
            title = { Text("Renombrar lista") },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text("Nombre") },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
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
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameListDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // Section C: Full design modal for registering shopping as expense
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ShoppingItemRow(
    item: ShoppingItemEntity,
    currencySymbol: String,
    modifier: Modifier = Modifier,
    isDimmed: Boolean = false,
    onTogglePurchased: () -> Unit,
    onQuantityChanged: (Double) -> Unit,
    onLongPress: () -> Unit
) {
    val incomeColor = LocalIncomeColor.current
    val numberFont = LocalNumberFontFamily.current

    val qtyFormatted = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()
    val priceFormatted = CurrencyUtils.formatAmount(item.unitPrice, currencySymbol)

    AlcanciaCard(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .combinedClickable(
                onClick = onTogglePurchased,
                onLongClick = onLongPress
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = onTogglePurchased,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (item.isPurchased) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = "Marcar comprado",
                        tint = if (item.isPurchased) incomeColor else MaterialTheme.colorScheme.outline
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Column {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDimmed) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (item.isPurchased) TextDecoration.LineThrough else null,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Formula: Cantidad × Precio Unitario
                    Text(
                        text = "$qtyFormatted × $priceFormatted",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = numberFont),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isDimmed) 0.5f else 0.8f)
                    )
                }
            }

            // Stepper and Subtotal
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onQuantityChanged(item.quantity - 1.0) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(imageVector = Icons.Default.Remove, contentDescription = "Menos", modifier = Modifier.size(16.dp))
                }

                IconButton(
                    onClick = { onQuantityChanged(item.quantity + 1.0) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Más", modifier = Modifier.size(16.dp))
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Subtotal
                Text(
                    text = CurrencyUtils.formatAmount(item.subtotal, currencySymbol),
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = numberFont),
                    fontWeight = FontWeight.Bold,
                    color = if (item.isPurchased) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun EditShoppingItemDialog(
    item: ShoppingItemEntity,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (name: String, qty: Double, price: Double) -> Unit
) {
    var name by remember { mutableStateOf(item.name) }
    var qtyStr by remember { mutableStateOf(if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()) }
    var priceStr by remember { mutableStateOf(if (item.unitPrice % 1.0 == 0.0) item.unitPrice.toInt().toString() else item.unitPrice.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar producto") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Producto") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = qtyStr,
                    onValueChange = { qtyStr = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Cantidad") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = priceStr,
                    onValueChange = { priceStr = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Precio Unitario ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(14.dp),
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
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Guardar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
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
            sb.append("  [ ] ${item.name} (${qty} × ${price} = ${sub})\n")
        }
        sb.append("\n")
    }

    val purchased = items.filter { it.isPurchased }
    if (purchased.isNotEmpty()) {
        sb.append("✅ Comprado:\n")
        for (item in purchased) {
            val qty = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()
            val sub = CurrencyUtils.formatAmount(item.subtotal, currencySymbol)
            sb.append("  [x] ${item.name} (${qty} = ${sub})\n")
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
