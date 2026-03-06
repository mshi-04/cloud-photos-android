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
import com.appvoyager.cloudphotos.domain.auth.valueobject.Email
import com.appvoyager.cloudphotos.ui.auth.screen.ForgotPasswordScreen
import com.appvoyager.cloudphotos.ui.auth.screen.LoginScreen
import com.appvoyager.cloudphotos.ui.auth.screen.ResetPasswordCodeScreen
import com.appvoyager.cloudphotos.ui.auth.screen.VerificationCodeScreen
import com.appvoyager.cloudphotos.ui.screen.HomeScreen

private const val TRANSITION_DURATION_MS = 300

object AuthRoute {

    const val HOME = "home"
    const val LOGIN = "login"
    const val FORGOT_PASSWORD = "forgot_password"

    const val URI_LOGIN = "login?messageResId={messageResId}"
    const val URI_VERIFICATION = "verification/{email}"
    const val URI_RESET_PASSWORD_CODE = "reset_password_code/{email}"

    fun login(messageResId: Int? = null): String =
        if (messageResId != null) URI_LOGIN.replace("{messageResId}", messageResId.toString()) else LOGIN

    fun verification(email: Email): String =
        URI_VERIFICATION.replace("{email}", Uri.encode(email.value))

    fun resetPasswordCode(email: Email): String =
        URI_RESET_PASSWORD_CODE.replace("{email}", Uri.encode(email.value))
}

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String,
    onSignOut: () -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(
            route = AuthRoute.URI_LOGIN,
            arguments = listOf(navArgument("messageResId") {
                type = NavType.IntType
                defaultValue = -1
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
                onNavigateToResetCode = { email ->
                    navController.navigate(AuthRoute.resetPasswordCode(email))
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
            route = AuthRoute.URI_RESET_PASSWORD_CODE,
            arguments = listOf(navArgument("email") { type = NavType.StringType }),
            enterTransition = { enterForward() },
            exitTransition = { exitForward() },
            popEnterTransition = { enterBack() },
            popExitTransition = { exitBack() }
        ) {
            ResetPasswordCodeScreen(
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
            HomeScreen(
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
