package com.qrdrop.phototransfer.ui

import android.Manifest
import android.content.contentValuesOf
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.qrdrop.phototransfer.ui.theme.DarkBackground
import com.qrdrop.phototransfer.ui.theme.DarkSurface
import com.qrdrop.phototransfer.ui.theme.DarkSurfaceVariant
import com.qrdrop.phototransfer.ui.theme.MutedText
import com.qrdrop.phototransfer.ui.theme.OnDarkText
import com.qrdrop.phototransfer.ui.theme.PrimaryCyan
import com.qrdrop.phototransfer.ui.theme.SecondaryGreen
import com.qrdrop.phototransfer.viewmodel.ReceiverViewModel

@Composable
fun ReceiverScreen(
    viewModel: ReceiverViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(uiState.saveResult) {
        uiState.saveResult?.let { resultMsg ->
            Toast.makeText(context, resultMsg, Toast.LENGTH_LONG).show()
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = DarkBackground
    ) {
        if (!hasCameraPermission) {
            CameraPermissionPrompt(
                onRequestPermission = {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
            )
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Bar
                    HeaderSection(uiState.isCompleted)

                    Spacer(modifier = Modifier.height(12.dp))

                    // Live Photo Reconstruction Canvas
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(DarkSurface)
                            .border(1.dp, DarkSurfaceVariant, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        val currentBitmap = uiState.bitmap
                        if (currentBitmap != null) {
                            // Draw the actual bitmap being live reconstructed
                            // Keying on uiState.version guarantees instant Compose updates as frames arrive
                            val imageBitmap = remember(uiState.version) { currentBitmap.asImageBitmap() }
                            Image(
                                bitmap = imageBitmap,
                                contentDescription = "Live Photo Canvas",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            // Blank/Placeholder state before first valid QR packet
                            InitialPlaceholder()
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Progress & Telemetry Panel
                    ProgressTelemetryPanel(
                        receivedFrames = uiState.receivedFramesCount,
                        totalFrames = uiState.totalFramesCount,
                        percentage = uiState.progressPercentage,
                        width = uiState.width,
                        height = uiState.height,
                        statusMessage = uiState.statusMessage,
                        isCompleted = uiState.isCompleted,
                        isSaving = uiState.isSaving,
                        onSaveClicked = { viewModel.saveToGallery(context) },
                        onResetClicked = { viewModel.reset() }
                    )
                }

                // Secondary Small Floating Camera Preview (PIP mode)
                if (uiState.isScanning) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 24.dp, bottom = 120.dp)
                            .size(width = 110.dp, height = 150.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black)
                            .border(2.dp, PrimaryCyan, RoundedCornerShape(12.dp))
                    ) {
                        CameraPreviewView(
                            onBarcodeDetected = { rawQr ->
                                viewModel.processQrText(rawQr)
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Active Scanner Indicator Badge
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(6.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.7f))
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(SecondaryGreen)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "SCANNING",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderSection(isCompleted: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "QR Photo Transfer",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = OnDarkText
            )
            Text(
                text = "Offline Live Optical Receiver",
                fontSize = 12.sp,
                color = MutedText
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(if (isCompleted) SecondaryGreen.copy(alpha = 0.2f) else PrimaryCyan.copy(alpha = 0.2f))
                .border(
                    1.dp,
                    if (isCompleted) SecondaryGreen else PrimaryCyan,
                    RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = if (isCompleted) "COMPLETE" else "LIVE SCAN",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isCompleted) SecondaryGreen else PrimaryCyan
            )
        }
    }
}

@Composable
private fun InitialPlaceholder() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(24.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = PrimaryCyan.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Waiting for QR Photo Stream...",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = OnDarkText,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Point camera at the sender screen. Pixels will reconstruct live in real-time.",
            fontSize = 12.sp,
            color = MutedText,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ProgressTelemetryPanel(
    receivedFrames: Int,
    totalFrames: Int,
    percentage: Float,
    width: Int,
    height: Int,
    statusMessage: String,
    isCompleted: Boolean,
    isSaving: Boolean,
    onSaveClicked: () -> Unit,
    onResetClicked: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            if (isCompleted) {
                // Completion Banner & Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SecondaryGreen,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Transfer Complete",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = SecondaryGreen
                        )
                        Text(
                            text = "$receivedFrames / $totalFrames frames ($width x $height px)",
                            fontSize = 12.sp,
                            color = MutedText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onSaveClicked,
                        enabled = !isSaving,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryGreen, contentColor = Color.Black)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Save to Gallery", fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = onResetClicked,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = OnDarkText)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            } else {
                // Live Transfer Progress
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (totalFrames > 0) "$receivedFrames / $totalFrames frames" else "0 / 0 frames",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = OnDarkText
                    )
                    Text(
                        text = String.format("%.1f%%", percentage),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = PrimaryCyan
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { if (totalFrames > 0) receivedFrames.toFloat() / totalFrames else 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = PrimaryCyan,
                    trackColor = DarkSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Status: $statusMessage",
                        fontSize = 12.sp,
                        color = MutedText
                    )
                    if (width > 0 && height > 0) {
                        Text(
                            text = "${width}x${height}px",
                            fontSize = 12.sp,
                            color = MutedText,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraPermissionPrompt(
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = PrimaryCyan
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Camera Permission Required",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = OnDarkText,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "QR Photo Transfer needs camera access to continuously scan QR photo streams and reconstruct your photos offline.",
            fontSize = 14.sp,
            color = MutedText,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(28.dp))
        Button(
            onClick = onRequestPermission,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan, contentColor = Color.Black)
        ) {
            Text(text = "Grant Camera Permission", fontWeight = FontWeight.Bold)
        }
    }
}
