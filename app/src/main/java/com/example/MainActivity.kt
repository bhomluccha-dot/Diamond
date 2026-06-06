package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.data.database.AppDatabase
import com.example.data.repository.AppRepository
import com.example.ui.screens.MainDashboard
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.DiamondWalaViewModel
import com.example.ui.viewmodel.DiamondWalaViewModelFactory

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Build local Room persistence stack
    val database = AppDatabase.getInstance(applicationContext)
    val dao = database.appDao()
    val repository = AppRepository(dao)
    
    // Inject components into state management ViewModel
    val viewModelFactory = DiamondWalaViewModelFactory(repository)
    val viewModel = ViewModelProvider(this, viewModelFactory)[DiamondWalaViewModel::class.java]

    setContent {
      MyApplicationTheme {
        MainDashboard(viewModel = viewModel)
      }
    }
  }
}
