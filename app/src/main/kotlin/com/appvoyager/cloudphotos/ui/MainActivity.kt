package com.appvoyager.cloudphotos.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.appvoyager.cloudphotos.R
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
            val snackbarHostState = remember { SnackbarHostState() }
            val signOutErrorMessage = stringResource(R.string.error_sign_out)
            LaunchedEffect(Unit) {
                mainViewModel.uiEvent.collect { event ->
                    when (event) {
                        is MainUiEvent.SignOutFailed -> snackbarHostState.showSnackbar(
                            signOutErrorMessage
                        )
                    }
                }
            }
            LaunchedEffect(mainViewModel.uiState) {
                if (mainViewModel.uiState is MainUiState.Authenticated) {
                    mainViewModel.registerFcmToken()
                }
            }
            CloudPhotosTheme(
                dynamicColor = true
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    when (mainViewModel.uiState) {
                        is MainUiState.None -> {}

                        is MainUiState.Authenticated -> {
                            NavGraph(
                                startDestination = AuthRoute.HOME,
                                onSignOut = { mainViewModel.signOut() }
                            )
                        }

                        is MainUiState.Unauthenticated -> {
                            NavGraph(startDestination = AuthRoute.login())
                        }

                        is MainUiState.SessionCheckError -> {
                            SessionCheckErrorScreen(
                                isRetrying = mainViewModel.isRetrying,
                                onRetry = { mainViewModel.checkSession() }
                            )
                        }
                    }
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }
}
