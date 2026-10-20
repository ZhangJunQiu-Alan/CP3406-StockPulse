package au.edu.jcu.assessment.utilityapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import au.edu.jcu.assessment.utilityapp.ui.StockViewModel
import au.edu.jcu.assessment.utilityapp.ui.screens.SettingsScreen
import au.edu.jcu.assessment.utilityapp.ui.screens.UtilityScreen
import au.edu.jcu.assessment.utilityapp.ui.theme.StockPulseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StockPulseTheme {
                UtilityApp()
            }
        }
    }
}

/** The two bottom-navigation destinations provided by the starter template. */
private enum class AppTab(@StringRes val titleRes: Int, val icon: ImageVector) {
    UTILITY(R.string.tab_watchlist, Icons.Default.Home),
    SETTINGS(R.string.tab_settings, Icons.Default.Settings),
}

/**
 * Main scaffold with bottom navigation. A single [StockViewModel] is shared by both tabs so
 * changes made in Settings are reflected on the Utility screen straight away.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UtilityApp(
    viewModel: StockViewModel = viewModel(factory = StockViewModel.Factory),
) {
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.UTILITY) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (selectedTab == AppTab.UTILITY) R.string.app_name else selectedTab.titleRes
                        )
                    )
                },
                actions = {
                    if (selectedTab == AppTab.UTILITY) {
                        IconButton(onClick = viewModel::refresh) {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.action_refresh))
                        }
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEach { tab ->
                    val title = stringResource(tab.titleRes)
                    NavigationBarItem(
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(title) },
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                    )
                }
            }
        }
    ) { innerPadding ->
        val contentModifier = Modifier.padding(innerPadding)
        Crossfade(
            targetState = selectedTab,
            animationSpec = tween(220),
            modifier = contentModifier,
            label = "Main tab transition",
        ) { tab ->
            when (tab) {
                AppTab.UTILITY -> UtilityScreen(viewModel = viewModel)
                AppTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
            }
        }
    }
}
