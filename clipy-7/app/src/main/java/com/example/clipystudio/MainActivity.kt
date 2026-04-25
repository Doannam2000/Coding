package com.example.clipystudio

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.clipystudio.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()
    val repository = ClipyApplication.repository(applicationContext)
    setContent {
      MyApplicationTheme { Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { MainNavigation(repository) } }
    }
  }
}

object ClipyApplication {
  @Volatile private var repository: com.example.clipystudio.data.DefaultDataRepository? = null

  fun repository(context: Context): com.example.clipystudio.data.DefaultDataRepository = repository ?: synchronized(this) {
    repository ?: com.example.clipystudio.data.DefaultDataRepository(context.applicationContext).also { repository = it }
  }
}
