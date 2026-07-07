package br.com.yson.controle.de.obras

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ObrasViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val navController = rememberNavController()
                    val viewModel: ObrasViewModel = viewModel()

                    NavHost(
                        navController = navController,
                        startDestination = "main"
                    ) {
                        composable("main") {
                            MainScreen(navController = navController, viewModel = viewModel)
                        }
                        composable("csv_backup") {
                            CsvBackupScreen(navController = navController, viewModel = viewModel)
                        }
                        composable("fornecedores") {
                            FornecedoresScreen(navController = navController, viewModel = viewModel)
                        }
                        composable("prestadores") {
                            PrestadoresScreen(navController = navController, viewModel = viewModel)
                        }
                        composable("obras") {
                            ObrasScreen(navController = navController, viewModel = viewModel)
                        }
                        composable(
                            route = "lancamento_obras/{obraId}",
                            arguments = listOf(navArgument("obraId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val obraId = backStackEntry.arguments?.getInt("obraId") ?: 0
                            LancamentoObrasScreen(obraId = obraId, navController = navController, viewModel = viewModel)
                        }
                        composable(
                            route = "lote_obra/{obraId}",
                            arguments = listOf(navArgument("obraId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val obraId = backStackEntry.arguments?.getInt("obraId") ?: 0
                            LoteObraScreen(obraId = obraId, navController = navController, viewModel = viewModel)
                        }
                        composable(
                            route = "burocracia_obra/{obraId}",
                            arguments = listOf(navArgument("obraId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val obraId = backStackEntry.arguments?.getInt("obraId") ?: 0
                            BurocraciaObraScreen(obraId = obraId, navController = navController, viewModel = viewModel)
                        }
                        composable(
                            route = "fornecedores_obra/{obraId}",
                            arguments = listOf(navArgument("obraId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val obraId = backStackEntry.arguments?.getInt("obraId") ?: 0
                            FornecedoresObraScreen(obraId = obraId, navController = navController, viewModel = viewModel)
                        }
                        composable(
                            route = "prestadores_obra/{obraId}",
                            arguments = listOf(navArgument("obraId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val obraId = backStackEntry.arguments?.getInt("obraId") ?: 0
                            PrestadoresObraScreen(obraId = obraId, navController = navController, viewModel = viewModel)
                        }
                        composable(
                            route = "relatorio_obra/{obraId}",
                            arguments = listOf(navArgument("obraId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val obraId = backStackEntry.arguments?.getInt("obraId") ?: 0
                            RelatorioObraScreen(obraId = obraId, navController = navController, viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}
