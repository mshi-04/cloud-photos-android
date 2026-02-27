package com.appvoyager.cloudphotos.ui.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.appvoyager.cloudphotos.ui.auth.AuthRoute
import com.appvoyager.cloudphotos.ui.auth.NavGraph
import com.appvoyager.cloudphotos.ui.theme.CloudPhotosTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LaunchedEffect(Unit) {
                mainViewModel.checkSession()
            }

            CloudPhotosTheme(
                dynamicColor = true
            ) {
                when (mainViewModel.uiState) {
                    is MainUiState.Loading -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    is MainUiState.Authenticated -> {
                        NavGraph(startDestination = AuthRoute.HOME)
                    }

                    is MainUiState.Unauthenticated -> {
                        NavGraph(startDestination = AuthRoute.LOGIN)
                    }
                }
            }
        }
    }

}
