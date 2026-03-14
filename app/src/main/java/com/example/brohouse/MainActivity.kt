package com.example.brohouse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.brohouse.ui.HouseDetailsScreen
import com.example.brohouse.ui.MainScreen
import com.example.brohouse.ui.theme.BrohouseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BrohouseTheme {
                val navController = rememberNavController()
                val vm: MainViewModel = viewModel()

                NavHost(navController = navController, startDestination = "main") {
                    composable("main") {
                        MainScreen(
                            viewModel = vm,
                            onNavigateToHouseDetails = { navController.navigate("house_details") }
                        )
                    }
                    composable("house_details") {
                        HouseDetailsScreen(
                            viewModel = vm,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
