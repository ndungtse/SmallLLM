package com.smallllm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.smallllm.ui.navigation.SmallLlmNavGraph
import com.smallllm.ui.theme.SmallLLMTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmallLLMTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SmallLlmNavGraph()
                }
            }
        }
    }
}
