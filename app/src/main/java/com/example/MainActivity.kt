package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.repository.SettingsRepository
import com.example.ui.screens.AjustesScreen
import com.example.ui.screens.AppLockScreen
import com.example.ui.screens.AssistantChatScreen
import com.example.ui.screens.ComprasScreen
import com.example.ui.screens.InicioScreen
import com.example.ui.screens.MetasScreen
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
                            .background(MaterialTheme.colorScheme.background)
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
                            onTabSelected = { viewModel.selectTab(it) },
                            modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter)
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
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 22.dp, vertical = 10.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 4.dp,
            shadowElevation = 6.dp
        ) {
            NavigationBar(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 0.dp,
                windowInsets = WindowInsets(0, 0, 0, 0),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            ) {
                // Tab 1: Inicio
                val isInicio = currentTab == AppTab.INICIO
                NavigationBarItem(
                    selected = isInicio,
                    onClick = { onTabSelected(AppTab.INICIO) },
                    icon = {
                        Icon(
                            imageVector = if (isInicio) Icons.Filled.Savings else Icons.Outlined.Savings,
                            contentDescription = "Inicio"
                        )
                    },
                    label = {
                        Text(
                            text = "Inicio",
                            fontWeight = if (isInicio) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("tab_inicio")
                )

                // Tab 2: Metas (Section E)
                val isMetas = currentTab == AppTab.METAS
                NavigationBarItem(
                    selected = isMetas,
                    onClick = { onTabSelected(AppTab.METAS) },
                    icon = {
                        Icon(
                            imageVector = if (isMetas) Icons.Filled.Flag else Icons.Outlined.Flag,
                            contentDescription = "Metas"
                        )
                    },
                    label = {
                        Text(
                            text = "Metas",
                            fontWeight = if (isMetas) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("tab_metas")
                )

                // Tab 3: Compras
                val isCompras = currentTab == AppTab.COMPRAS
                NavigationBarItem(
                    selected = isCompras,
                    onClick = { onTabSelected(AppTab.COMPRAS) },
                    icon = {
                        Icon(
                            imageVector = if (isCompras) Icons.Filled.ShoppingCart else Icons.Outlined.ShoppingCart,
                            contentDescription = "Compras"
                        )
                    },
                    label = {
                        Text(
                            text = "Compras",
                            fontWeight = if (isCompras) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("tab_compras")
                )

                // Tab 4: Ajustes
                val isAjustes = currentTab == AppTab.AJUSTES
                NavigationBarItem(
                    selected = isAjustes,
                    onClick = { onTabSelected(AppTab.AJUSTES) },
                    icon = {
                        Icon(
                            imageVector = if (isAjustes) Icons.Filled.Settings else Icons.Outlined.Settings,
                            contentDescription = "Ajustes"
                        )
                    },
                    label = {
                        Text(
                            text = "Ajustes",
                            fontWeight = if (isAjustes) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("tab_ajustes")
                )
            }
        }
    }
}
