package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.repository.SettingsRepository
import com.example.ui.screens.AjustesScreen
import com.example.ui.screens.AppLockScreen
import com.example.ui.screens.AssistantChatScreen
import com.example.ui.screens.ComprasScreen
import com.example.ui.screens.InicioPalette
import com.example.ui.screens.InicioScreen
import com.example.ui.screens.MetasScreen
import com.example.ui.screens.resolveInicioPalette
import com.example.ui.theme.AlcanciaTheme
import com.example.ui.viewmodel.AlcanciaViewModel
import com.example.ui.viewmodel.AppTab

class MainActivity : ComponentActivity() {
    private val viewModel: AlcanciaViewModel by viewModels()

    @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        setContent {
            val initialSettings = remember { SettingsRepository.getCachedSettings(applicationContext) }
            val settings by viewModel.settingsState.collectAsStateWithLifecycle(initialValue = initialSettings)
            val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
            val isAssistantChatOpen by viewModel.isAssistantChatOpen.collectAsStateWithLifecycle()

            // Lock state: require authentication if fingerprint or PIN is enabled
            val isSecurityEnabled = settings.isFingerprintEnabled || (settings.isPinEnabled && settings.pinCode.isNotEmpty())
            var isUnlocked by remember { mutableStateOf(!isSecurityEnabled) }

            AlcanciaTheme(settings = settings) {
                val palette = resolveInicioPalette(settings)

                if (isSecurityEnabled && !isUnlocked) {
                    AppLockScreen(
                        settings = settings,
                        viewModel = viewModel,
                        onUnlock = { isUnlocked = true }
                    )
                } else if (isAssistantChatOpen) {
                    AssistantChatScreen(
                        viewModel = viewModel,
                        onDismiss = { viewModel.closeAssistantChat() }
                    )
                } else {
                    // If on secondary tab, back press returns to Inicio
                    BackHandler(enabled = currentTab != AppTab.INICIO) {
                        viewModel.selectTab(AppTab.INICIO)
                    }

                    // Floating layout: screen content extends completely to the bottom edge-to-edge
                    // and the capsule floats cleanly on top with NO background behind it
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(palette.bg)
                    ) {
                        when (currentTab) {
                            AppTab.INICIO -> InicioScreen(viewModel = viewModel)
                            AppTab.METAS -> MetasScreen(viewModel = viewModel)
                            AppTab.COMPRAS -> ComprasScreen(viewModel = viewModel)
                            AppTab.AJUSTES -> AjustesScreen(viewModel = viewModel)
                        }

                        // Floating bottom navigation capsule overlay
                        AlcanciaBottomNavigation(
                            currentTab = currentTab,
                            palette = palette,
                            onTabSelected = { viewModel.selectTab(it) },
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AlcanciaBottomNavigation(
    currentTab: AppTab,
    palette: InicioPalette.Colors,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            color = palette.s1,
            border = BorderStroke(1.dp, palette.s3),
            tonalElevation = 0.dp,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavPillItem(
                    label = "Inicio",
                    selected = currentTab == AppTab.INICIO,
                    selectedIcon = Icons.Filled.Savings,
                    unselectedIcon = Icons.Outlined.Savings,
                    palette = palette,
                    onClick = { onTabSelected(AppTab.INICIO) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tab_inicio")
                )
                BottomNavPillItem(
                    label = "Metas",
                    selected = currentTab == AppTab.METAS,
                    selectedIcon = Icons.Filled.Flag,
                    unselectedIcon = Icons.Outlined.Flag,
                    palette = palette,
                    onClick = { onTabSelected(AppTab.METAS) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tab_metas")
                )
                BottomNavPillItem(
                    label = "Compras",
                    selected = currentTab == AppTab.COMPRAS,
                    selectedIcon = Icons.Filled.ShoppingCart,
                    unselectedIcon = Icons.Outlined.ShoppingCart,
                    palette = palette,
                    onClick = { onTabSelected(AppTab.COMPRAS) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tab_compras")
                )
                BottomNavPillItem(
                    label = "Ajustes",
                    selected = currentTab == AppTab.AJUSTES,
                    selectedIcon = Icons.Filled.Settings,
                    unselectedIcon = Icons.Outlined.Settings,
                    palette = palette,
                    onClick = { onTabSelected(AppTab.AJUSTES) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tab_ajustes")
                )
            }
        }
    }
}

@Composable
private fun BottomNavPillItem(
    label: String,
    selected: Boolean,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    palette: InicioPalette.Colors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(26.dp),
        color = if (selected) palette.s3 else Color.Transparent,
        border = if (selected) BorderStroke(1.dp, palette.ahorro.copy(alpha = 0.30f)) else null,
        modifier = modifier.defaultMinSize(minHeight = 52.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 7.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (selected) selectedIcon else unselectedIcon,
                contentDescription = label,
                tint = if (selected) palette.accent else palette.t2,
                modifier = Modifier.size(21.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) palette.t1 else palette.t2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

