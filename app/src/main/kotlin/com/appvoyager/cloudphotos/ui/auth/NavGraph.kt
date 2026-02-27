package com.appvoyager.cloudphotos.ui.auth

import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.appvoyager.cloudphotos.ui.auth.screen.ForgotPasswordScreen
import com.appvoyager.cloudphotos.ui.auth.screen.LoginScreen
import com.appvoyager.cloudphotos.ui.auth.screen.ResetPasswordCodeScreen
import com.appvoyager.cloudphotos.ui.auth.screen.VerificationCodeScreen
import com.appvoyager.cloudphotos.ui.screen.HomeScreen

private const val TRANSITION_DURATION_MS = 300

object AuthRoute {
    const val LOGIN = "login"
    const val VERIFICATION = "verification/{email}"
    const val FORGOT_PASSWORD = "forgot_password"
    const val RESET_PASSWORD_CODE = "reset_password_code/{email}"
    const val HOME = "home"

    fun verification(email: String): String = "verification/${Uri.encode(email)}"
    fun resetPasswordCode(email: String): String = "reset_password_code/${Uri.encode(email)}"
}

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(
            route = AuthRoute.LOGIN,
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
                        popUpTo(AuthRoute.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToForgotPassword = {
                    navController.navigate(AuthRoute.FORGOT_PASSWORD)
                }
            )
        }

        composable(
            route = AuthRoute.VERIFICATION,
            arguments = listOf(navArgument("email") { type = NavType.StringType }),
            enterTransition = { enterForward() },
            exitTransition = { exitForward() },
            popEnterTransition = { enterBack() },
            popExitTransition = { exitBack() }
        ) {
            VerificationCodeScreen(
                onNavigateToHome = {
                    navController.navigate(AuthRoute.HOME) {
                        popUpTo(AuthRoute.LOGIN) { inclusive = true }
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
                onNavigateToResetCode = { email ->
                    navController.navigate(AuthRoute.resetPasswordCode(email))
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = AuthRoute.RESET_PASSWORD_CODE,
            arguments = listOf(navArgument("email") { type = NavType.StringType }),
            enterTransition = { enterForward() },
            exitTransition = { exitForward() },
            popEnterTransition = { enterBack() },
            popExitTransition = { exitBack() }
        ) {
            ResetPasswordCodeScreen(
                onNavigateBackToLogin = {
                    navController.navigate(AuthRoute.LOGIN) {
                        popUpTo(AuthRoute.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = AuthRoute.HOME,
            enterTransition = { enterForward() },
            exitTransition = null,
            popEnterTransition = { enterBack() },
            popExitTransition = { exitBack() }
        ) {
            HomeScreen({})
        }
    }
}

private fun AnimatedContentTransitionScope<*>.isInitial(): Boolean = initialState == null

private fun AnimatedContentTransitionScope<*>.enterForward() =
    if (isInitial()) EnterTransition.None
    else slideIntoContainer(
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
