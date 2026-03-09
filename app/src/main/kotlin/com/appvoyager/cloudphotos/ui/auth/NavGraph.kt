package com.appvoyager.cloudphotos.ui.auth

import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.appvoyager.cloudphotos.domain.auth.valueobject.Email
import com.appvoyager.cloudphotos.ui.auth.screen.ForgotPasswordScreen
import com.appvoyager.cloudphotos.ui.auth.screen.LoginScreen
import com.appvoyager.cloudphotos.ui.auth.screen.ResetPasswordScreen
import com.appvoyager.cloudphotos.ui.auth.screen.VerificationCodeScreen
import com.appvoyager.cloudphotos.ui.media.screen.MediaScreen

private const val TRANSITION_DURATION_MS = 300

object AuthRoute {

    const val NO_MESSAGE_RES_ID = -1

    const val HOME = "home"
    internal const val LOGIN = "login"
    internal const val FORGOT_PASSWORD = "forgot_password"

    internal const val URI_LOGIN = "login?messageResId={messageResId}"
    internal const val URI_VERIFICATION = "verification/{email}"
    internal const val URI_RESET_PASSWORD = "reset_password/{email}"

    fun login(messageResId: Int? = null): String =
        if (messageResId != null) URI_LOGIN.replace(
            "{messageResId}",
            messageResId.toString()
        ) else LOGIN

    fun verification(email: Email): String =
        URI_VERIFICATION.replace("{email}", Uri.encode(email.value))

    fun resetPassword(email: Email): String =
        URI_RESET_PASSWORD.replace("{email}", Uri.encode(email.value))

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
            arguments = listOf(navArgument("messageResId") {
                type = NavType.IntType
                defaultValue = AuthRoute.NO_MESSAGE_RES_ID
            }),
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
                onNavigateBackToLogin = { messageResId ->
                    navController.navigate(AuthRoute.login(messageResId)) {
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
                } else {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(TRANSITION_DURATION_MS)
                    )
                }
            },
            exitTransition = null,
            popEnterTransition = { enterBack() },
            popExitTransition = { exitBack() }
        ) {
            MediaScreen(
                onSignOut = {
                    onSignOut()
                }
            )
        }
    }
}

private fun AnimatedContentTransitionScope<*>.enterForward() =
    slideIntoContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Left,
        animationSpec = tween(TRANSITION_DURATION_MS)
    )

private fun AnimatedContentTransitionScope<*>.exitForward() =
    slideOutOfContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Left,
        animationSpec = tween(TRANSITION_DURATION_MS)
    )

private fun AnimatedContentTransitionScope<*>.enterBack() =
    slideIntoContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Right,
        animationSpec = tween(TRANSITION_DURATION_MS)
    )

private fun AnimatedContentTransitionScope<*>.exitBack() =
    slideOutOfContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Right,
        animationSpec = tween(TRANSITION_DURATION_MS)
    )
