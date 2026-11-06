package com.appvoyager.cloudphotos.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appvoyager.cloudphotos.ui.theme.CloudPhotosTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        mainViewModel.checkSession()
        splashScreen.setKeepOnScreenCondition { mainViewModel.isCheckingSession.value }
        enableEdgeToEdge()
        setContent {
            val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
            val isRetrying by mainViewModel.isRetrying.collectAsStateWithLifecycle()

            LaunchedEffect(uiState) {
                if (uiState is MainUiState.Authenticated) {
                    mainViewModel.registerFcmToken()
                }
            }
            CloudPhotosTheme(
                dynamicColor = true
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    when (uiState) {
                        is MainUiState.None -> {}

                        is MainUiState.Authenticated -> {
                            NavGraph(startDestination = AuthRoute.HOME)
                        }

                        is MainUiState.Unauthenticated -> {
                            NavGraph(startDestination = AuthRoute.login())
                        }

                        is MainUiState.SessionCheckError -> {
                            SessionCheckErrorScreen(
                                isRetrying = isRetrying,
                                onRetry = { mainViewModel.checkSession() }
                            )
                        }
                    }
                }
            }
        }
    }
}
