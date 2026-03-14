package com.thiccbokki.brohouse

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
import com.thiccbokki.brohouse.auth.AuthState
import com.thiccbokki.brohouse.auth.AuthViewModel
import com.thiccbokki.brohouse.ui.HouseDetailsScreen
import com.thiccbokki.brohouse.ui.InviteScreen
import com.thiccbokki.brohouse.ui.MainScreen
import com.thiccbokki.brohouse.ui.SuppliesScreen
import com.thiccbokki.brohouse.ui.TripListScreen
import com.thiccbokki.brohouse.ui.auth.LoginScreen
import com.thiccbokki.brohouse.ui.auth.RegisterScreen
import com.thiccbokki.brohouse.ui.theme.BrohouseTheme

private const val WEB_CLIENT_ID = "316196631711-rpghvrkvonsvdvfslet521v2potvudae.apps.googleusercontent.com"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BrohouseTheme {
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

        composable("trip/{tripId}") { backStackEntry ->
            val tripId = backStackEntry.arguments?.getString("tripId") ?: return@composable
            val tripViewModel: TripViewModel = viewModel()
            MainScreen(
                viewModel = tripViewModel,
                isAdmin = isAdmin,
                onNavigateToHouseDetails = { navController.navigate("trip/$tripId/house_details") },
                onNavigateToSupplies = { navController.navigate("trip/$tripId/supplies") },
                onNavigateToInvite = { navController.navigate("trip/$tripId/invite") },
                onNavigateBack = { navController.popBackStack() },
                onSignOut = { authViewModel.signOut() }
            )
        }

        composable("trip/{tripId}/supplies") {
            val tripViewModel: TripViewModel = viewModel()
            SuppliesScreen(
                viewModel = tripViewModel,
                isAdmin = isAdmin,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("trip/{tripId}/house_details") {
            val tripViewModel: TripViewModel = viewModel()
            HouseDetailsScreen(
                viewModel = tripViewModel,
                isAdmin = isAdmin,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("trip/{tripId}/invite") {
            val tripViewModel: TripViewModel = viewModel()
            InviteScreen(
                viewModel = tripViewModel,
                isAdmin = isAdmin,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
