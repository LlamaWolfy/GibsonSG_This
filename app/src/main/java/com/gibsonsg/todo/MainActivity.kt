package com.gibsonsg.todo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.gibsonsg.todo.core.designsystem.ScribbleTodoTheme
import com.gibsonsg.todo.feature.navigation.TodoNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScribbleTodoTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TodoNavHost()
                }
            }
        }
    }
}
