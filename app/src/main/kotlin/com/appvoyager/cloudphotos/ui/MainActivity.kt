package com.appvoyager.cloudphotos.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
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
            val context = LocalContext.current
            LaunchedEffect(Unit) {
                mainViewModel.uiEvent.collect { event ->
                    when (event) {
                        is MainUiEvent.SignOutFailed -> Toast.makeText(
                            context,
                            context.getString(R.string.error_sign_out),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            CloudPhotosTheme(
                dynamicColor = true
            ) {
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
            }
        }
    }

}
