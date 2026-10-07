package com.xiaojidiu.philosophy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import com.xiaojidiu.philosophy.ui.theme.LittleThoughtsTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.xiaojidiu.philosophy.data.local.PreferencesRepository
import com.xiaojidiu.philosophy.ui.AppViewModel
import com.xiaojidiu.philosophy.ui.onboarding.*
import com.xiaojidiu.philosophy.navigation.MainNavigation
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.compose.foundation.isSystemInDarkTheme

class MainActivity : ComponentActivity() {
    override fun onResume() {super.onResume();(application as ThoughtsApplication).graph.bgm.setForeground(true)}
    override fun onStop() {if(!isChangingConfigurations) (application as ThoughtsApplication).graph.bgm.setForeground(false);super.onStop()}
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: AppViewModel = viewModel(factory = viewModelFactory { initializer { AppViewModel((application as ThoughtsApplication).graph) } })
            val settings by vm.settings.collectAsStateWithLifecycle()
            val ready by vm.ready.collectAsStateWithLifecycle()
            val error by vm.error.collectAsStateWithLifecycle()
            var splashDone by rememberSaveable { mutableStateOf(false) }
            val dark=settings?.theme=="dark" || settings?.theme!="light" && isSystemInDarkTheme()
            SideEffect {WindowCompat.getInsetsController(window,window.decorView).apply {isAppearanceLightStatusBars=!dark;isAppearanceLightNavigationBars=!dark}}
            LittleThoughtsTheme(settings?.theme ?: "system") {
                Surface {
                    when { !splashDone || settings == null -> SplashScreen { splashDone = true }
                        settings?.onboarding != true -> OnboardingScreen(vm::completeOnboarding)
                        error != null -> com.xiaojidiu.philosophy.ui.components.ErrorState(error!!, vm::retry)
                        !ready -> com.xiaojidiu.philosophy.ui.components.LoadingState()
                        else -> MainNavigation(vm)
                    }
                }
            }
        }
    }
}
