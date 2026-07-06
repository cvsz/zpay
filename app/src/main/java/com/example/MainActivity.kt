package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.data.WalletViewModel
import com.example.ui.screens.WalletAppContent
import com.example.ui.theme.ZWalletTheme

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: WalletViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        viewModel = ViewModelProvider(this)[WalletViewModel::class.java]
        
        // Handle deep link from launcher/notification intent
        intent?.dataString?.let {
            viewModel.handleDeepLink(it)
        }

        setContent {
            ZWalletTheme {
                WalletAppContent(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.dataString?.let {
            viewModel.handleDeepLink(it)
        }
    }
}

