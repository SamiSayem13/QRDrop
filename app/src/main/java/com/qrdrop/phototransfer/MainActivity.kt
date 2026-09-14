package com.qrdrop.phototransfer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.qrdrop.phototransfer.ui.ReceiverScreen
import com.qrdrop.phototransfer.ui.theme.QRPhotoTransferTheme
import com.qrdrop.phototransfer.viewmodel.ReceiverViewModel

class MainActivity : ComponentActivity() {

    private val receiverViewModel: ReceiverViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QRPhotoTransferTheme {
                ReceiverScreen(viewModel = receiverViewModel)
            }
        }
    }
}
