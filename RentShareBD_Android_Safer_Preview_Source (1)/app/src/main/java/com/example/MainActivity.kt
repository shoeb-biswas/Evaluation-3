package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.RentShareContainer
import com.example.data.ai.configureAppCheck
import com.example.ui.RentShareApp
import com.example.ui.theme.RentShareTheme
import com.example.ui.viewmodel.LandlordAdminViewModel
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.TenantViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure Firebase App Check with launch intent before any UI, ViewModels, or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            val container = remember { RentShareContainer(applicationContext) }
            val factory = remember(container) {
                object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return when {
                            modelClass.isAssignableFrom(MainViewModel::class.java) -> MainViewModel(container) as T
                            modelClass.isAssignableFrom(TenantViewModel::class.java) -> TenantViewModel(container) as T
                            modelClass.isAssignableFrom(LandlordAdminViewModel::class.java) -> LandlordAdminViewModel(container) as T
                            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                        }
                    }
                }
            }
            val mainVm: MainViewModel = viewModel(factory = factory)
            val tenantVm: TenantViewModel = viewModel(factory = factory)
            val landlordAdminVm: LandlordAdminViewModel = viewModel(factory = factory)

            RentShareTheme {
                RentShareApp(
                    mainVm = mainVm,
                    tenantVm = tenantVm,
                    landlordAdminVm = landlordAdminVm
                )
            }
        }
    }
}
