package com.thoughtcrimes.securesms.wallet.jetpackcomposeUI.walletscanqr

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import com.thoughtcrimes.securesms.wallet.jetpackcomposeUI.util.QrCodeAnalyzer
import com.google.zxing.Result
import com.thoughtcrimes.securesms.compose_utils.appColors
import io.beldex.bchat.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScannerScreen(
    hasCamPermission: Boolean,
    cameraProviderFuture: ListenableFuture<ProcessCameraProvider>,
    lifecycleOwner: LifecycleOwner,
    onQrCodeScanned: (Result) -> Unit,
    intent : (Intent) -> Unit,
    onBackPress : () -> Unit
) {
    val time = (1 * 1000).toLong()

    var uploadFromGalleryButtonIsEnabled by remember {
        mutableStateOf(true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                title = {
                    Text(
                        text = stringResource(id = R.string.scan_qrcode_text),
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = MaterialTheme.appColors.editTextColor,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        onBackPress()
                    }) {
                        Icon(Icons.Filled.ArrowBack, "backIcon")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = MaterialTheme.colorScheme.primary),
                actions = {
                    IconButton(enabled = false, onClick = {
                    }) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            "Hide",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(it)
            ) {
                if (hasCamPermission) {
                    AndroidView(
                        factory = { context ->
                            val previewView = PreviewView(context)
                            val preview = Preview.Builder().build()
                            val selector = CameraSelector.Builder()
                                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                                .build()
                            preview.setSurfaceProvider(previewView.surfaceProvider)
                            val imageAnalysis = ImageAnalysis.Builder()
                                .setTargetResolution(
                                    Size(
                                        previewView.width,
                                        previewView.height
                                    )
                                )
                                .setBackpressureStrategy(STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                            imageAnalysis.setAnalyzer(
                                ContextCompat.getMainExecutor(context),
                                QrCodeAnalyzer { result ->
                                    onQrCodeScanned(result)
                                }
                            )
                            try {
                                cameraProviderFuture.get().bindToLifecycle(
                                    lifecycleOwner,
                                    selector,
                                    preview,
                                    imageAnalysis
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            previewView
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 20.dp, end = 20.dp, top = 25.dp, bottom = 10.dp)
                    )

                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedButton(
                        contentPadding = PaddingValues(start = 25.dp,bottom = 20.dp,end = 25.dp, top = 20.dp),
                        modifier = Modifier.padding(15.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.appColors.walletDashboardMainMenuCardBackground,
                            contentColor = MaterialTheme.appColors.textColor
                        ),
                        border = BorderStroke(2.dp, MaterialTheme.appColors.primaryButtonColor),
                        enabled = uploadFromGalleryButtonIsEnabled,
                        onClick = {
                            uploadFromGalleryButtonIsEnabled = false
                            val pickIntent = Intent(Intent.ACTION_PICK)
                            pickIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
                            intent(pickIntent)
                            Handler(Looper.getMainLooper()).postDelayed({
                                uploadFromGalleryButtonIsEnabled = true
                            }, time)
                        }
                    ) {
                        Icon(
                            painterResource(id = R.drawable.gallery_upload),
                            stringResource(R.string.upload_from_gallery),
                            tint = MaterialTheme.appColors.textColor
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = stringResource(R.string.upload_from_gallery),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            style = TextStyle(color = MaterialTheme.appColors.textColor),
                            modifier = Modifier.padding()
                        )
                    }
                    Text(
                        stringResource(R.string.wallet_scan_qr_content),
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.CenterHorizontally).padding(start = 20.dp,end = 20.dp, top = 10.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    )
}