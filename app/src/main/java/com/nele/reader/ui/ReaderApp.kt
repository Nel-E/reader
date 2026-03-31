package com.nele.reader.ui

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nele.reader.model.MdFile
import com.nele.reader.ui.screens.HomeScreen
import com.nele.reader.ui.screens.SettingsScreen
import com.nele.reader.ui.screens.ViewerScreen
import com.nele.reader.ui.theme.ReaderTheme

@Composable
fun ReaderApp() {
    val navController = rememberNavController()
    val vm: ReaderViewModel = viewModel()
    val themeMode by vm.themeMode.collectAsStateWithLifecycle()

    // Handle VIEW/EDIT intent when launched from a file manager
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    LaunchedEffect(Unit) {
        val intent = activity?.intent ?: return@LaunchedEffect
        if (intent.action == Intent.ACTION_VIEW || intent.action == Intent.ACTION_EDIT) {
            val uri = intent.data ?: return@LaunchedEffect
            val displayName = uri.lastPathSegment
                ?.substringAfterLast("/")
                ?.substringAfterLast("%2F")
                ?: uri.toString()
            val mdFile = MdFile(
                id = uri.toString(),
                displayName = displayName,
                isRemote = false,
                uri = uri.toString(),
                isReadOnly = intent.action == Intent.ACTION_VIEW
            )
            vm.openFile(mdFile)
            navController.navigate("viewer")
            // Clear the intent so back-navigation doesn't re-open
            activity.intent = Intent()
        }
    }

    ReaderTheme(themeMode = themeMode) {
        NavHost(navController = navController, startDestination = "home") {
            composable("home") {
                HomeScreen(
                    vm = vm,
                    onOpenFile = { file ->
                        vm.openFile(file)
                        navController.navigate("viewer")
                    },
                    onOpenSettings = { navController.navigate("settings") }
                )
            }
            composable("viewer") {
                ViewerScreen(
                    vm = vm,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("settings") {
                SettingsScreen(
                    vm = vm,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
