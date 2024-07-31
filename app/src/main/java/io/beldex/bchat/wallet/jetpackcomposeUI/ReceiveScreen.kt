package io.beldex.bchat.wallet.jetpackcomposeUI


import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Environment
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import io.beldex.bchat.compose_utils.BChatOutlinedTextField
import io.beldex.bchat.compose_utils.PrimaryButton
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.crypto.IdentityKeyUtil
import io.beldex.bchat.data.BarcodeData
import io.beldex.bchat.data.Crypto
import io.beldex.bchat.util.FileProviderUtil
import io.beldex.bchat.util.Helper
import io.beldex.bchat.util.copyToClipBoard
import io.beldex.bchat.util.toPx
import io.beldex.bchat.wallet.receive.ReceiveFragment
import io.beldex.bchat.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.util.HashMap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiveScreen(listenerCallback: ReceiveFragment.Listener?, modifier: Modifier) {

    val context = LocalContext.current
    val numberRegex = remember { "[\\d]*[.]?[\\d]*".toRegex() }

    var beldexAmount by remember {
        mutableStateOf("")
    }
    var errorAction by remember {
        mutableStateOf(false)
    }

    val beldexAddress by remember {
        mutableStateOf(IdentityKeyUtil.retrieve(context, IdentityKeyUtil.IDENTITY_W_ADDRESS_PREF))
    }
    var isButtonEnabled by remember {
        mutableStateOf(true)
    }
    val scope = rememberCoroutineScope()

    var bcData: BarcodeData?
    var logo: Bitmap? = null
    val resources = LocalContext.current.resources
    var qrValid = false
    var qrCodeAsBitmap: Bitmap? = null


    fun getBchatLogo(): Bitmap? {
        if (logo == null) {
            logo = Helper.getBitmap(context, R.drawable.ic_launcher_foreground)
        }
        return logo
    }

    fun addLogo(qrBitmap: Bitmap): Bitmap? {
        // addume logo & qrcode are both square
        val logo: Bitmap? = getBchatLogo()
        val qrSize = qrBitmap.width
        val logoSize = logo!!.width
        val logoBitmap = Bitmap.createBitmap(qrSize, qrSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(logoBitmap)
        canvas.drawBitmap(qrBitmap, 0f, 0f, null)
        canvas.save()
        val sx = 0.2f * qrSize / logoSize
        canvas.scale(sx, sx, qrSize / 2f, qrSize / 2f)
        canvas.restore()
        return logoBitmap
    }


    fun generate(text: String?, width: Int, height: Int): Bitmap? {
        println("value changed listern 6 $beldexAmount")
        if (width <= 0 || height <= 0) return null
        val hints: MutableMap<EncodeHintType, Any?> = HashMap()
        hints[EncodeHintType.CHARACTER_SET] = "utf-8"
        hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.M
        try {
            val bitMatrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, width, height, hints)
            val pixels = IntArray(width * height)
            for (i in 0 until height) {
                for (j in 0 until width) {
                    if (bitMatrix[j, i]) {
                        pixels[i * width + j] = 0x00000000
                    } else {
                        pixels[i * height + j] = -0x1
                    }
                }
            }
            var bitmap = Bitmap.createBitmap(pixels, 0, width, width, height, Bitmap.Config.RGB_565)
            bitmap = addLogo(bitmap)!!
            return bitmap
        } catch (ex: WriterException) {
            Timber.e(ex)
        }
        return null
    }

    fun generateQr(address: String, amount: String, notes: String): Bitmap? {
        bcData = BarcodeData(Crypto.BDX, address, notes, amount)
        val size = toPx(280, resources)
        val qr = generate(bcData!!.uriString, size, size)
        if (qr != null) {
            return qr
        }
        return null
    }

    fun clearQR() {
        if (qrValid) {/*binding.qrCodeReceive.setImageBitmap(null)*/
            qrValid = false/* if (isLoaded) binding.qrCodeReceive.visibility = View.VISIBLE*/
        }
    }


    fun validateBELDEXAmount(amount: String): Boolean {
        val maxValue = 150000000.00000
        val value = amount.replace(',', '.')
        val regExp = "^(([0-9]{0,9})?|[.][0-9]{0,5})?|([0-9]{0,9}+([.][0-9]{0,5}))\$"
        var isValid = false

        isValid = if (value.matches(Regex(regExp))) {
            if (value == ".") {
                false
            } else {
                try {
                    val dValue = value.toDouble()
                    (dValue <= maxValue && dValue > 0)
                } catch (e: Exception) {
                    false
                }
            }
        } else {
            false
        }
        return isValid
    }


    fun shareQrCode() {
        val directory = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val fileName = "MyWalletQr.png"
        val file = File(directory, fileName)
        file.createNewFile()
        val fos = FileOutputStream(file)
        val qrCode = qrCodeAsBitmap
        qrCode?.compress(Bitmap.CompressFormat.PNG, 100, fos)
        fos.flush()
        fos.close()
        val intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(Intent.EXTRA_STREAM, FileProviderUtil.getUriFor(context, file))
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.type = "image/png"
        context.startActivity(Intent.createChooser(intent, resources.getString(R.string.fragment_view_my_qr_code_share_title)))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.appColors.cardBackground
                ),
                title = {
                    Text(
                        text = context.getString(R.string.activity_receive_page_title), style = MaterialTheme.typography.titleLarge.copy(
                            color = MaterialTheme.appColors.editTextColor,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                        ), textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        listenerCallback!!.walletOnBackPressed()
                    }) {
                        Icon(
                            painterResource(id = R.drawable.ic_back_arrow),
                            contentDescription = stringResource(
                                id = R.string.back
                            ),
                            tint = MaterialTheme.appColors.editTextColor,
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = MaterialTheme.appColors.cardBackground),
                actions = {
                    IconButton(enabled = false,onClick = {
                    }) {
                        Icon(Icons.Filled.ArrowBack, "Hide", tint = MaterialTheme.appColors.cardBackground)
                    }
                }
            )
        },
        content = {
            Column(modifier = modifier
                .fillMaxSize()
                .background(color = MaterialTheme.appColors.cardBackground)
                .padding(it)
            ) {

                Box(modifier = Modifier.padding(10.dp)) {
                    Box(modifier = Modifier.padding(top = 80.dp, bottom = 20.dp)) {

                        Column(modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.appColors.receiveCardBackground,
                                shape = RoundedCornerShape(18.dp)
                            )

                        ) {

                            Text(text = context.getString(R.string.enter_bdx_amount), modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 150.dp, bottom = 5.dp, start = 20.dp), style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, fontWeight = FontWeight(600), color = MaterialTheme.appColors.textColor))

                            BChatOutlinedTextField(
                                value = beldexAmount,
                                placeHolder = stringResource(id = R.string.hint),
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Done,
                                onValueChange = {
                                    if(numberRegex.matches(it)) {
                                        beldexAmount = it
                                        if (beldexAmount.isNotEmpty()) {
                                            if (validateBELDEXAmount(it)) {
                                                errorAction = false
                                                generateQr(beldexAddress, beldexAmount, "")
                                            } else {
                                                errorAction = true
                                            }
                                        } else {
                                            errorAction = false
                                            generateQr(beldexAddress, beldexAmount, "")
                                        }
                                    }
                                },
                                focusedBorderColor = MaterialTheme.appColors.textFiledBorderColor,
                                focusedLabelColor = MaterialTheme.appColors.textColor,
                                textColor = MaterialTheme.appColors.textColor,
                                maxLen = 16,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        start = 20.dp,
                                        top = 10.dp,
                                        end = 20.dp,
                                        bottom = 20.dp
                                    ),
                                shape = RoundedCornerShape(8.dp),
                            )

                            if (errorAction) {
                                Text(text = stringResource(id = R.string.beldex_amount_valid_error_message), modifier = Modifier.padding(start = 20.dp, bottom = 20.dp), style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.appColors.errorMessageColor, fontSize = 13.sp, fontWeight = FontWeight(400)))
                            }

                            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 20.dp, end = 20.dp)

                            ) {
                                Text(text = "Beldex Address", style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, fontWeight = FontWeight(600), color = MaterialTheme.appColors.textColor))

                                Box(modifier = Modifier
                                    .width(32.dp)
                                    .height(32.dp)
                                    .background(
                                        color = MaterialTheme.appColors.primaryButtonColor,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        context.copyToClipBoard("Beldex Address", beldexAddress)
                                    }, contentAlignment = Alignment.Center) {
                                    Image(painter = painterResource(id = R.drawable.copy_icon_small), contentDescription = "")
                                }
                            }

                            Card(modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 20.dp, top = 10.dp, end = 20.dp, bottom = 30.dp), colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.appColors.beldexAddressBackground,
                            )) {
                                Text(text = beldexAddress, modifier = Modifier.padding(20.dp), style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.appColors.primaryButtonColor, fontSize = 13.sp, fontWeight = FontWeight(400)))
                            }

                        }
                    }
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {

                        Column(modifier = Modifier
                            .size(190.dp)
                            .clip(RectangleShape)
                            .background(
                                color = MaterialTheme.appColors.qrCodeBackground,
                                shape = RoundedCornerShape(16.dp)
                            )

                        ) {
                            Card(modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp), colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.appColors.beldexAddressBackground,
                            )) {

                                generateQr(beldexAddress, beldexAmount, "")?.let {
                                    Image(bitmap = it.asImageBitmap(), contentDescription = "", modifier = Modifier.fillMaxSize()

                                    )
                                    qrCodeAsBitmap = it
                                }

                            }
                        }
                    }

                }


                PrimaryButton(onClick = {
                    if(isButtonEnabled) {
                        isButtonEnabled = false
                        scope.launch(Dispatchers.Main) {
                            shareQrCode()
                            delay(2000)
                            isButtonEnabled = true
                        }
                    }

                }, modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp), shape = RoundedCornerShape(16.dp)) {
                    Image(painter = painterResource(id = R.drawable.share), contentDescription = "")
                    Text(text = stringResource(id = R.string.share), modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight(700), color = Color.White))
                }
            }
        }
    )
}


/*
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ReceiveScreenPreview() {
    BChatTheme() {
        ReceiveScreen(
            listenerCallback = listenerCallback, modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun ReceiveScreenLightPreview() {
    BChatTheme() {
        ReceiveScreen(
            listenerCallback = listenerCallback, modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
        )
    }
}*/
