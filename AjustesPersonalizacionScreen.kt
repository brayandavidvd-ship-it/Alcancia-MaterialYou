package com.example.ui.screens

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CategoryEntity
import com.example.ui.components.AlcanciaCard
import com.example.ui.components.CurrencyUtils
import com.example.ui.theme.LocalExpenseColor
import com.example.ui.theme.LocalIncomeColor
import com.example.ui.theme.LocalIsCompactDensity
import com.example.ui.theme.LocalNumberFontFamily
import com.example.ui.theme.LocalSavingsColor
import com.example.ui.theme.PresetAccents
import com.example.ui.theme.QuickThemes
import com.example.ui.theme.getCategoryColor
import com.example.ui.viewmodel.AlcanciaViewModel

@Composable
fun AjustesPersonalizacionScreen(
    viewModel: AlcanciaViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val settings by viewModel.settingsState.collectAsStateWithLifecycle()
    val categories by viewModel.categoriesState.collectAsStateWithLifecycle()
    val isCategoriesGridView by viewModel.isCategoriesGridView.collectAsStateWithLifecycle()

    val incomeColor = LocalIncomeColor.current
    val expenseColor = LocalExpenseColor.current
    val savingsColor = LocalSavingsColor.current
    val numberFont = LocalNumberFontFamily.current
    val isCompact = LocalIsCompactDensity.current

    var showCategoryBudgetDialog by remember { mutableStateOf<CategoryEntity?>(null) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showColorPickerCustom by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
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
                Text(
                    text = "Personalización",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
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
                bottom = 40.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. LIVE MINI PREVIEW CARD
            item {
                Text(
                    text = "VISTA PREVIA EN VIVO",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                AlcanciaCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Alcancía Demo",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = settings.currencyCode,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "${settings.currencySymbol}14,500.00",
                            style = MaterialTheme.typography.headlineLarge.copy(fontFamily = numberFont),
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Demo income
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = incomeColor.copy(alpha = 0.14f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = incomeColor, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "+${settings.currencySymbol}22,000",
                                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = numberFont),
                                        fontWeight = FontWeight.Bold,
                                        color = incomeColor,
                                        maxLines = 1
                                    )
                                }
                            }

                            // Demo expense
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = expenseColor.copy(alpha = 0.14f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = expenseColor, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "-${settings.currencySymbol}7,500",
                                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = numberFont),
                                        fontWeight = FontWeight.Bold,
                                        color = expenseColor,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. TEMAS RÁPIDOS
            item {
                Text(
                    text = "Temas rápidos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    for (themeOpt in QuickThemes) {
                        val isSelected = !settings.useDynamicColors && settings.quickTheme == themeOpt.id
                        Surface(
                            onClick = { viewModel.applyQuickTheme(themeOpt) },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier.width(110.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = themeOpt.emoji, fontSize = 24.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = themeOpt.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(themeOpt.accentColor)
                                )
                            }
                        }
                    }
                }
            }

            // 3. MODO DE TEMA
            item {
                Text(
                    text = "Modo de tema",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val modes = listOf("SYSTEM" to "Sistema", "LIGHT" to "Claro", "DARK" to "Oscuro")
                    for ((modeKey, modeTitle) in modes) {
                        val isSelected = settings.themeMode == modeKey
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setThemeMode(modeKey) },
                            label = { Text(modeTitle, maxLines = 1) },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 4. COLORES DINÁMICOS
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                item {
                    AlcanciaCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text("Material You (Colores dinámicos)", fontWeight = FontWeight.SemiBold)
                                Text("Adapta los colores del fondo de pantalla", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = settings.useDynamicColors,
                                onCheckedChange = { viewModel.setDynamicColors(it) }
                            )
                        }
                    }
                }
            }

            // 5. COLOR DE ACENTO
            item {
                Text(
                    text = "Color de acento",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for ((index, accent) in PresetAccents.withIndex()) {
                        val isSelected = !settings.useDynamicColors && settings.accentColorIndex == index
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(accent.color)
                                .clickable { viewModel.setAccentPreset(index) }
                                .then(
                                    if (isSelected) {
                                        Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    } else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            }
                        }
                    }

                    // Botón selector libre
                    val isCustomSelected = !settings.useDynamicColors && settings.accentColorIndex == -1 && settings.quickTheme == "CUSTOM"
                    Surface(
                        onClick = { showColorPickerCustom = true },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = if (isCustomSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Palette, contentDescription = "Personalizado", modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            // 6. ESTILO DE TARJETAS
            item {
                Text(
                    text = "Estilo de tarjetas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val styles = listOf("SOLID" to "Sólido", "TINT" to "Tinte", "OUTLINE" to "Contorno")
                    for ((key, label) in styles) {
                        val isSelected = settings.cardStyle == key
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setCardStyle(key) },
                            label = { Text(label, maxLines = 1) },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 7. FONDO (Cuadrícula 2 columnas con etiquetas cortas)
            item {
                Text(
                    text = "Fondo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                val isCurrentDark = settings.themeMode == "DARK" || (settings.themeMode == "SYSTEM")
                val bgOptions = if (isCurrentDark) {
                    listOf("AMOLED" to "AMOLED", "DARK_GRAY" to "Gris", "DARK_TINT" to "Tinte", "DEFAULT" to "Estándar")
                } else {
                    listOf("PURE_WHITE" to "Blanco", "LIGHT_GRAY" to "Gris", "LIGHT_TINT" to "Tinte", "DEFAULT" to "Estándar")
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for ((key, label) in bgOptions.take(2)) {
                            val isSelected = settings.bgStyle == key
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setBgStyle(key) },
                                label = { Text(label, maxLines = 1) },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for ((key, label) in bgOptions.drop(2)) {
                            val isSelected = settings.bgStyle == key
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setBgStyle(key) },
                                label = { Text(label, maxLines = 1) },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 8. ESQUINAS
            item {
                Text(
                    text = "Esquinas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val corners = listOf("ROUNDED" to "Redondas", "MEDIUM" to "Medias", "STRAIGHT" to "Rectas")
                    for ((key, label) in corners) {
                        val isSelected = settings.cornerRadius == key
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setCornerRadius(key) },
                            label = { Text(label, maxLines = 1) },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 9. DENSIDAD DE PANTALLA
            item {
                Text(
                    text = "Densidad",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val densities = listOf("COMFORTABLE" to "Cómoda", "COMPACT" to "Compacta")
                    for ((key, label) in densities) {
                        val isSelected = settings.density == key
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setDensity(key) },
                            label = { Text(label, maxLines = 1) },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 10. TIPOGRAFÍA NUMÉRICA ("Cifras alineadas" en lugar de "Monospace")
            item {
                Text(
                    text = "Tipografía numérica",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val fontsRow1 = listOf(
                            "DEFAULT" to "Estándar",
                            "ROUNDED" to "Redondas"
                        )
                        for ((key, label) in fontsRow1) {
                            val isSelected = settings.numberFont == key
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setNumberFont(key) },
                                label = { Text(label, maxLines = 1) },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    val isTabularSelected = settings.numberFont == "TABULAR" || settings.numberFont == "MONOSPACE"
                    FilterChip(
                        selected = isTabularSelected,
                        onClick = { viewModel.setNumberFont("TABULAR") },
                        label = {
                            Text(
                                text = "Cifras alineadas (números tabulares)",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 11. COLORES FINANCIEROS Y MODO DALTÓNICO
            item {
                Text(
                    text = "Colores financieros",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                AlcanciaCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Modo daltónico accesible", fontWeight = FontWeight.SemiBold)
                                Text("Usa Azul para Ingresos y Naranja para Gastos", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = settings.isColorblindPreset,
                                onCheckedChange = { viewModel.setColorblindPreset(it) }
                            )
                        }

                        if (!settings.isColorblindPreset) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(incomeColor))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Ingreso", style = MaterialTheme.typography.labelSmall)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(expenseColor))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Gasto", style = MaterialTheme.typography.labelSmall)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(savingsColor))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Ahorro", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }



            // 13. CATEGORÍAS Y PRESUPUESTOS (con botón lista/cuadrícula)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Categorías y presupuestos",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val catCount = categories.size
                        Text(
                            text = if (catCount == 1) "1 categoría" else "$catCount categorías",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { showAddCategoryDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Nueva categoría", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { viewModel.toggleCategoriesViewMode() }) {
                            Icon(
                                imageVector = if (isCategoriesGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                                contentDescription = if (isCategoriesGridView) "Ver como lista" else "Ver como cuadrícula",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Lista o cuadrícula de 3 columnas de categorías
            if (isCategoriesGridView) {
                items(
                    items = categories.chunked(3),
                    key = { chunk -> "cat_grid_${chunk.first().id}" }
                ) { chunk ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (cat in chunk) {
                            GridCategoryCard(
                                category = cat,
                                currencySymbol = settings.currencySymbol,
                                modifier = Modifier.weight(1f),
                                onEdit = { showCategoryBudgetDialog = cat }
                            )
                        }
                        for (i in 0 until (3 - chunk.size)) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            } else {
                items(items = categories, key = { it.id }) { cat ->
                    ListCategoryCard(
                        category = cat,
                        currencySymbol = settings.currencySymbol,
                        onEdit = { showCategoryBudgetDialog = cat },
                        onDelete = if (!cat.isDefault) { { viewModel.deleteCategory(cat) } } else null
                    )
                }
            }
        }
    }

    // Modal Selector Libre HSV
    if (showColorPickerCustom) {
        var hVal by remember { mutableFloatStateOf(140f) }
        var sVal by remember { mutableFloatStateOf(0.7f) }
        var vVal by remember { mutableFloatStateOf(0.5f) }

        val pickedColor = remember(hVal, sVal, vVal) {
            Color(ColorUtils.HSLToColor(floatArrayOf(hVal, sVal, vVal)))
        }

        AlertDialog(
            onDismissRequest = { showColorPickerCustom = false },
            title = { Text("Color personalizado") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(pickedColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Muestra de color",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Text("Matiz (Hue): ${hVal.toInt()}°", style = MaterialTheme.typography.bodySmall)
                    Slider(value = hVal, onValueChange = { hVal = it }, valueRange = 0f..360f)

                    Text("Saturación: ${(sVal * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                    Slider(value = sVal, onValueChange = { sVal = it }, valueRange = 0.1f..1f)

                    Text("Brillo: ${(vVal * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                    Slider(value = vVal, onValueChange = { vVal = it }, valueRange = 0.2f..0.85f)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setCustomAccentColor(pickedColor.toArgb().toLong() and 0xFFFFFFFFL)
                        showColorPickerCustom = false
                    }
                ) {
                    Text("Aplicar color")
                }
            },
            dismissButton = {
                TextButton(onClick = { showColorPickerCustom = false }) { Text("Cancelar") }
            }
        )
    }

    // Modal Crear Nueva Categoría
    if (showAddCategoryDialog) {
        var newCatName by remember { mutableStateOf("") }
        var newCatEmoji by remember { mutableStateOf("🏷️") }
        var newCatBudget by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("Nueva categoría") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newCatEmoji,
                        onValueChange = { newCatEmoji = it.take(2) },
                        label = { Text("Emoji") },
                        modifier = Modifier.width(100.dp)
                    )
                    OutlinedTextField(
                        value = newCatName,
                        onValueChange = { newCatName = it },
                        label = { Text("Nombre") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newCatBudget,
                        onValueChange = { newCatBudget = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Límite mensual opcional (${settings.currencySymbol})") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCatName.isNotBlank()) {
                            val parsed = newCatBudget.toDoubleOrNull()
                            viewModel.addCategory(newCatName, newCatEmoji.ifBlank { "🏷️" }, parsed)
                            showAddCategoryDialog = false
                        }
                    }
                ) {
                    Text("Crear categoría")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // Modal Editar Presupuesto de Categoría
    if (showCategoryBudgetDialog != null) {
        val cat = showCategoryBudgetDialog!!
        var budgetInput by remember {
            val b = cat.monthlyBudgetLimit
            mutableStateOf(if (b != null && b > 0) b.toInt().toString() else "")
        }

        AlertDialog(
            onDismissRequest = { showCategoryBudgetDialog = null },
            title = { Text("${cat.emoji} ${cat.name}") },
            text = {
                Column {
                    Text("Define un tope de gasto mensual para recibir avisos.", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = budgetInput,
                        onValueChange = { budgetInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Límite mensual (${settings.currencySymbol})") },
                        placeholder = { Text("Ej. 2500") },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = budgetInput.toDoubleOrNull()
                        viewModel.saveCategory(cat.copy(monthlyBudgetLimit = if (parsed != null && parsed > 0) parsed else null))
                        showCategoryBudgetDialog = null
                    }
                ) {
                    Text("Guardar límite")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCategoryBudgetDialog = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun ListCategoryCard(
    category: CategoryEntity,
    currencySymbol: String,
    onEdit: () -> Unit,
    onDelete: (() -> Unit)?
) {
    val catColor = getCategoryColor(category)

    AlcanciaCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = catColor.copy(alpha = 0.22f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = category.emoji, fontSize = 20.sp)
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val budgetText = if (category.monthlyBudgetLimit != null && category.monthlyBudgetLimit > 0) {
                        "Límite: ${CurrencyUtils.formatCompact(category.monthlyBudgetLimit, currencySymbol)}"
                    } else {
                        "Sin límite asignado"
                    }
                    Text(
                        text = budgetText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar presupuesto", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar categoría", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun GridCategoryCard(
    category: CategoryEntity,
    currencySymbol: String,
    modifier: Modifier = Modifier,
    onEdit: () -> Unit
) {
    val catColor = getCategoryColor(category)

    AlcanciaCard(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable { onEdit() },
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = catColor.copy(alpha = 0.22f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = category.emoji, fontSize = 18.sp)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = category.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            // Only show limit if present (omit "Sin límite asignado")
            if (category.monthlyBudgetLimit != null && category.monthlyBudgetLimit > 0) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = CurrencyUtils.formatCompact(category.monthlyBudgetLimit, currencySymbol),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
