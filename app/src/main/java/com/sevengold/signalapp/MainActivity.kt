package com.sevengold.signalapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.sevengold.signalapp.ui.navigation.AppNav
import com.sevengold.signalapp.ui.theme.SignalAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SignalAppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNav()
                }
            }
        }
    }
}
