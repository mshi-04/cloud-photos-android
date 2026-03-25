package com.appvoyager.cloudphotos.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.appvoyager.cloudphotos.ui.theme.CloudPhotosTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        mainViewModel.checkSession()
        splashScreen.setKeepOnScreenCondition { mainViewModel.isCheckingSession }
        enableEdgeToEdge()
        setContent {
            CloudPhotosTheme(
                dynamicColor = true
            ) {
                when (mainViewModel.uiState) {
                    is MainUiState.Authenticated -> {
                        NavGraph(
                            startDestination = AuthRoute.HOME,
                            onSignOut = { mainViewModel.signOut() }
                        )
                    }

                    is MainUiState.Unauthenticated -> {
                        NavGraph(startDestination = AuthRoute.login())
                    }
                }
            }
        }
    }

}
