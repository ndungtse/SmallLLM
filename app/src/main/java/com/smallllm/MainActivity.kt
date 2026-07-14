package com.smallllm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.smallllm.ui.navigation.SmallLlmNavGraph
import com.smallllm.ui.theme.SmallLLMTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Brand scheme by default; Material You dynamic color is an opt-in the user
            // can flip (toggle affordance added with the Gallery toolbar in a later pass).
            var dynamicColor by rememberSaveable { mutableStateOf(false) }
            SmallLLMTheme(dynamicColor = dynamicColor) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SmallLlmNavGraph(
                        dynamicColor = dynamicColor,
                        onToggleDynamicColor = { dynamicColor = !dynamicColor },
                    )
                }
            }
        }
    }
}
