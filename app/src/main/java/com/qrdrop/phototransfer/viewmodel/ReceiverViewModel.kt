package com.qrdrop.phototransfer.viewmodel

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qrdrop.phototransfer.engine.ImageReconstructionEngine
import com.qrdrop.phototransfer.parser.PacketParser
import com.qrdrop.phototransfer.saver.GallerySaver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReceiverUiState(
    val isScanning: Boolean = true,
    val width: Int = 0,
    val height: Int = 0,
    val receivedFramesCount: Int = 0,
    val totalFramesCount: Int = 0,
    val progressPercentage: Float = 0f,
    val statusMessage: String = "Scanning for QR code...",
    val bitmap: Bitmap? = null,
    val isCompleted: Boolean = false,
    val isSaving: Boolean = false,
    val saveResult: String? = null,
    val lastScannedFrame: Int? = null,
    val version: Long = 0L
)

class ReceiverViewModel : ViewModel() {

    private val engine = ImageReconstructionEngine()

    private val _uiState = MutableStateFlow(ReceiverUiState())
    val uiState: StateFlow<ReceiverUiState> = _uiState.asStateFlow()

    fun processQrText(rawText: String) {
        if (_uiState.value.isCompleted) return

        viewModelScope.launch(Dispatchers.Default) {
            val packet = PacketParser.parse(rawText) ?: return@launch

            val isNewFrame = engine.processPacket(packet)
            if (isNewFrame) {
                val isDone = engine.isComplete
                val status = if (isDone) {
                    "Transfer Complete!"
                } else {
                    "Receiving frame ${packet.frameIndex + 1}"
                }

                _uiState.update { state ->
                    state.copy(
                        width = engine.width,
                        height = engine.height,
                        receivedFramesCount = engine.receivedFrameCount,
                        totalFramesCount = engine.totalFrames,
                        progressPercentage = engine.progressPercentage,
                        statusMessage = status,
                        bitmap = engine.bitmap,
                        isCompleted = isDone,
                        isScanning = !isDone,
                        lastScannedFrame = packet.frameIndex,
                        version = state.version + 1
                    )
                }
            }
        }
    }

    fun saveToGallery(context: Context) {
        val currentBitmap = engine.bitmap ?: return
        if (_uiState.value.isSaving) return

        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            val result = GallerySaver.saveBitmapToGallery(context, currentBitmap)
            result.onSuccess { filename ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        saveResult = "Saved: $filename"
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        saveResult = "Save error: ${error.localizedMessage ?: "Unknown"}"
                    )
                }
            }
        }
    }

    fun reset() {
        engine.reset()
        _uiState.update { ReceiverUiState() }
    }
}
