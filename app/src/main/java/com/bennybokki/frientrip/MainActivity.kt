package com.bennybokki.frientrip

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bennybokki.frientrip.auth.AuthState
import com.bennybokki.frientrip.auth.AuthViewModel
import com.bennybokki.frientrip.ui.ProfileScreen
import com.bennybokki.frientrip.ui.TripListScreen
import com.bennybokki.frientrip.ui.TripScaffold
import com.bennybokki.frientrip.ui.auth.LoginScreen
import com.bennybokki.frientrip.ui.auth.RegisterScreen
import com.bennybokki.frientrip.ui.theme.FrientripTheme

private const val WEB_CLIENT_ID = "227024772331-11e7iqdfkpvpdoa5c7e1lnr7nbgri50q.apps.googleusercontent.com"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FrientripTheme {
                BrohouseApp()
            }
        }
    }
}

@Composable
fun BrohouseApp() {
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.authState.collectAsState()
    val isAdmin by authViewModel.isAdmin.collectAsState()
    val navController = rememberNavController()

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.LoggedIn -> navController.navigate("trips") {
                popUpTo(0) { inclusive = true }
            }
            is AuthState.LoggedOut -> navController.navigate("login") {
                popUpTo(0) { inclusive = true }
            }
            is AuthState.Loading -> Unit
        }
    }

    if (authState is AuthState.Loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val startDestination = if (authState is AuthState.LoggedIn) "trips" else "login"

    NavHost(navController = navController, startDestination = startDestination) {

        composable("login") {
            LoginScreen(
                viewModel = authViewModel,
                webClientId = WEB_CLIENT_ID,
                onNavigateToRegister = { navController.navigate("register") }
            )
        }

        composable("register") {
            RegisterScreen(
                viewModel = authViewModel,
                webClientId = WEB_CLIENT_ID,
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        composable("trips") {
            val tripListViewModel: TripListViewModel = viewModel()
            TripListScreen(
                viewModel = tripListViewModel,
                authViewModel = authViewModel,
                onNavigateToTrip = { tripId -> navController.navigate("trip/$tripId") }
            )
        }

        composable("trip/{tripId}") {
            val tripViewModel: TripViewModel = viewModel()
            TripScaffold(
                viewModel = tripViewModel,
                isAdmin = isAdmin,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProfile = { navController.navigate("profile") }
            )
        }

        composable("profile") {
            val profileViewModel: ProfileViewModel = viewModel()
            ProfileScreen(
                viewModel = profileViewModel,
                onNavigateBack = { navController.popBackStack() },
                onSignOut = { authViewModel.signOut() }
            )
        }
    }
}
