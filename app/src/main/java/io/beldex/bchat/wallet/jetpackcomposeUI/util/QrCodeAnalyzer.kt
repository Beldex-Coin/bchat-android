package io.beldex.bchat.wallet.jetpackcomposeUI.util

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.beldex.libsignal.utilities.Log
import com.google.zxing.BinaryBitmap
import com.google.zxing.ChecksumException
import com.google.zxing.FormatException
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.Result
import com.google.zxing.qrcode.QRCodeReader

private val TAG = QRCodeAnalyzer::class.java.simpleName
class QRCodeAnalyzer(
    private val qrCodeReader: QRCodeReader,
    private val onBarcodeScanned: (Result) -> Unit
): ImageAnalysis.Analyzer {

    // Note: This analyze method is called once per frame of the camera feed.
    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(image: ImageProxy) {
        // Grab the image data as a byte array so we can generate a PlanarYUVLuminanceSource from it
        val buffer = image.planes[0].buffer
        buffer.rewind()
        val imageBytes = ByteArray(buffer.capacity())
        buffer.get(imageBytes) // IMPORTANT: This transfers data from the buffer INTO the imageBytes array, although it looks like it would go the other way around!

        // ZXing requires data as a BinaryBitmap to scan for QR codes, and to generate that we need to feed it a PlanarYUVLuminanceSource
        val luminanceSource = PlanarYUVLuminanceSource(imageBytes, image.width, image.height, 0, 0, image.width, image.height, false)
        val binaryBitmap = BinaryBitmap(HybridBinarizer(luminanceSource))

        // Attempt to extract a QR code from the binary bitmap, and pass it through to our `onBarcodeScanned` method if we find one
        try {
            val result: Result = qrCodeReader.decode(binaryBitmap)
            // No need to close the image here - it'll always make it to the end, and calling `onBarcodeScanned`
            // with a valid contact / recovery phrase / community code will stop calling this `analyze` method.
            onBarcodeScanned(result)
        }
        catch (nfe: NotFoundException) { /* Hits if there is no QR code in the image           */ Log.d(TAG,"${nfe.message}")}
        catch (fe: FormatException)    { /* Hits if we found a QR code but failed to decode it */ Log.d(TAG,"${fe.message}")}
        catch (ce: ChecksumException)  { /* Hits if we found a QR code which is corrupted      */ Log.d(TAG,"${ce.message}")}
        catch (e: Exception)           { /* Hits if there's a genuine problem                  */ Log.e(TAG, "${e.message}")}

        // Remember to close the image when we're done with it!
        // IMPORTANT: It is CLOSING the image that allows this method to run again! If we don't
        // close the image this method runs precisely ONCE and that's it, which is essentially useless.
        image.close()
    }
}