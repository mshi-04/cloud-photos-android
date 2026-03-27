package com.appvoyager.cloudphotos.ui

import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.appvoyager.cloudphotos.domain.auth.valueobject.Email
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaId
import com.appvoyager.cloudphotos.ui.auth.effect.AuthSnackbarMessage
import com.appvoyager.cloudphotos.ui.auth.screen.ForgotPasswordScreen
import com.appvoyager.cloudphotos.ui.auth.screen.LoginScreen
import com.appvoyager.cloudphotos.ui.auth.screen.ResetPasswordScreen
import com.appvoyager.cloudphotos.ui.auth.screen.VerificationCodeScreen
import com.appvoyager.cloudphotos.ui.media.screen.CameraScreen
import com.appvoyager.cloudphotos.ui.media.screen.MediaDetailScreen
import com.appvoyager.cloudphotos.ui.media.screen.MediaScreen
import com.appvoyager.cloudphotos.ui.media.uistate.MediaUiState
import com.appvoyager.cloudphotos.ui.media.viewmodel.MediaViewModel

private const val TRANSITION_DURATION_MS = 300

object MediaRoute {
    internal const val URI_DETAIL = "media_detail/{mediaId}"

    fun detail(mediaId: MediaId): String = "media_detail/${Uri.encode(mediaId.value)}"
}

object AuthRoute {

    const val HOME = "home"
    const val CAMERA = "camera"
    internal const val LOGIN = "login"
    internal const val FORGOT_PASSWORD = "forgot_password"

    internal const val URI_LOGIN = "login?message={message}"
    internal const val URI_VERIFICATION = "verification/{email}"
    internal const val URI_RESET_PASSWORD = "reset_password/{email}"

    fun login(message: AuthSnackbarMessage? = null): String =
        if (message != null) "login?message=${message.key}" else LOGIN

    fun verification(email: Email): String = URI_VERIFICATION.replace("{email}", Uri.encode(email.value))

    fun resetPassword(email: Email): String = URI_RESET_PASSWORD.replace("{email}", Uri.encode(email.value))
}

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String,
    onSignOut: () -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier.fillMaxSize()
    ) {
        composable(
            route = AuthRoute.URI_LOGIN,
            arguments = listOf(
                navArgument("message") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            ),
            enterTransition = { enterForward() },
            exitTransition = { exitForward() },
            popEnterTransition = { enterBack() },
            popExitTransition = { exitBack() }
        ) {
            LoginScreen(
                onNavigateToVerification = { email ->
                    navController.navigate(AuthRoute.verification(email))
                },
                onNavigateToHome = {
                    navController.navigate(AuthRoute.HOME) {
                        popUpTo(AuthRoute.URI_LOGIN) { inclusive = true }
                    }
                },
                onNavigateToForgotPassword = {
                    navController.navigate(AuthRoute.FORGOT_PASSWORD)
                }
            )
        }

        composable(
            route = AuthRoute.URI_VERIFICATION,
            arguments = listOf(navArgument("email") { type = NavType.StringType }),
            enterTransition = { enterForward() },
            exitTransition = { exitForward() },
            popEnterTransition = { enterBack() },
            popExitTransition = { exitBack() }
        ) {
            VerificationCodeScreen(
                onNavigateToHome = {
                    navController.navigate(AuthRoute.HOME) {
                        popUpTo(AuthRoute.URI_LOGIN) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = AuthRoute.FORGOT_PASSWORD,
            enterTransition = { enterForward() },
            exitTransition = { exitForward() },
            popEnterTransition = { enterBack() },
            popExitTransition = { exitBack() }
        ) {
            ForgotPasswordScreen(
                onNavigateToResetPassword = { email ->
                    navController.navigate(AuthRoute.resetPassword(email))
                },
                onNavigateToVerification = { email ->
                    navController.navigate(AuthRoute.verification(email))
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = AuthRoute.URI_RESET_PASSWORD,
            arguments = listOf(navArgument("email") { type = NavType.StringType }),
            enterTransition = { enterForward() },
            exitTransition = { exitForward() },
            popEnterTransition = { enterBack() },
            popExitTransition = { exitBack() }
        ) {
            ResetPasswordScreen(
                onNavigateBackToLogin = { message ->
                    navController.navigate(AuthRoute.login(message)) {
                        popUpTo(AuthRoute.URI_LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = AuthRoute.HOME,
            enterTransition = {
                val fromRoute = initialState.destination.route
                if (fromRoute == AuthRoute.URI_LOGIN) {
                    EnterTransition.None
                } else if (fromRoute == AuthRoute.CAMERA) {
                    enterBack()
                } else {
                    enterForward()
                }
            },
            exitTransition = null,
            popEnterTransition = { enterBack() },
            popExitTransition = { exitBack() }
        ) {
            MediaScreen(
                onNavigateToCamera = {
                    navController.navigate(AuthRoute.CAMERA)
                },
                onSignOut = {
                    onSignOut()
                },
                onMediaClick = { mediaId, _ ->
                    navController.navigate(MediaRoute.detail(mediaId))
                }
            )
        }

        composable(
            route = AuthRoute.CAMERA,
            enterTransition = { enterForward() },
            exitTransition = { exitForward() },
            popEnterTransition = { enterBack() },
            popExitTransition = { exitBack() }
        ) {
            CameraScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = MediaRoute.URI_DETAIL,
            arguments = listOf(navArgument("mediaId") { type = NavType.StringType }),
            enterTransition = { enterForward() },
            exitTransition = { exitForward() },
            popEnterTransition = { enterBack() },
            popExitTransition = { exitBack() }
        ) { backStackEntry ->
            val rawMediaId = backStackEntry.arguments?.getString("mediaId")
            val homeEntry = remember(navController) {
                navController.getBackStackEntry(AuthRoute.HOME)
            }
            val mediaViewModel: MediaViewModel = hiltViewModel(homeEntry)
            val uiState by mediaViewModel.uiState.collectAsStateWithLifecycle()
            if (rawMediaId.isNullOrBlank()) {
                navController.popBackStack()
                return@composable
            }
            val mediaList = (uiState.screenState as? MediaUiState.ScreenState.Success)
                ?.mediaList ?: emptyList()
            MediaDetailScreen(
                mediaList = mediaList,
                initialMediaId = MediaId.of(rawMediaId),
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

private fun AnimatedContentTransitionScope<*>.enterForward() = slideIntoContainer(
    towards = AnimatedContentTransitionScope.SlideDirection.Left,
    animationSpec = tween(TRANSITION_DURATION_MS)
)

private fun AnimatedContentTransitionScope<*>.exitForward() = slideOutOfContainer(
    towards = AnimatedContentTransitionScope.SlideDirection.Left,
    animationSpec = tween(TRANSITION_DURATION_MS)
)

private fun AnimatedContentTransitionScope<*>.enterBack() = slideIntoContainer(
    towards = AnimatedContentTransitionScope.SlideDirection.Right,
    animationSpec = tween(TRANSITION_DURATION_MS)
)

private fun AnimatedContentTransitionScope<*>.exitBack() = slideOutOfContainer(
    towards = AnimatedContentTransitionScope.SlideDirection.Right,
    animationSpec = tween(TRANSITION_DURATION_MS)
)
