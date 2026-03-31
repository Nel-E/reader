package com.nele.reader.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nele.reader.ui.screens.HomeScreen
import com.nele.reader.ui.screens.ViewerScreen

@Composable
fun ReaderApp() {
    val navController = rememberNavController()
    val vm: ReaderViewModel = viewModel()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                vm = vm,
                onOpenFile = { file ->
                    vm.openFile(file)
                    navController.navigate("viewer")
                }
            )
        }
        composable("viewer") {
            ViewerScreen(
                vm = vm,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
