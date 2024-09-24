package io.beldex.bchat.webrtc.video

import android.content.Context
import com.beldex.libsignal.utilities.Log
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraEnumerator
import org.webrtc.CameraVideoCapturer

class Camera(context: Context,
             private val cameraEventListener: CameraEventListener): CameraVideoCapturer.CameraSwitchHandler {

    companion object {
        private val TAG = Log.tag(Camera::class.java)
    }

    val capturer: CameraVideoCapturer?
    val cameraCount: Int
    var activeDirection: CameraState.Direction = CameraState.Direction.PENDING
    var enabled: Boolean = false
        set(value) {
            field = value
            capturer ?: return
            try {
                if (value) {
                    capturer.startCapture(1280,720,30)
                } else {
                    capturer.stopCapture()
                }
            } catch (e: InterruptedException) {
                Log.e(TAG,"Interrupted while stopping video capture")
            }
        }

    init {
        val enumerator = Camera2Enumerator(context)
        cameraCount = enumerator.deviceNames.size
        capturer = createVideoCapturer(enumerator, CameraState.Direction.FRONT)?.apply {
            activeDirection = CameraState.Direction.FRONT
        } ?: createVideoCapturer(enumerator, CameraState.Direction.BACK)?.apply {
            activeDirection = CameraState.Direction.BACK
        } ?: run {
            activeDirection = CameraState.Direction.NONE
            null
        }
    }

    fun dispose() {
        capturer?.dispose()
    }

    fun flip() {
        if (capturer == null || cameraCount < 2) {
            Log.w(TAG, "Tried to flip camera without capturer or less than 2 cameras")
            return
        }
        activeDirection = CameraState.Direction.PENDING
        capturer.switchCamera(this)
    }

    override fun onCameraSwitchDone(isFrontFacing: Boolean) {
        activeDirection = if (isFrontFacing) CameraState.Direction.FRONT else CameraState.Direction.BACK
        cameraEventListener.onCameraSwitchCompleted(CameraState(activeDirection, cameraCount))
    }

    override fun onCameraSwitchError(errorMessage: String?) {
        Log.e(TAG,"onCameraSwitchError: $errorMessage")
        cameraEventListener.onCameraSwitchCompleted(CameraState(activeDirection, cameraCount))

    }

    private fun createVideoCapturer(enumerator: CameraEnumerator, direction: CameraState.Direction): CameraVideoCapturer? =
        enumerator.deviceNames.firstOrNull { device ->
            (direction == CameraState.Direction.FRONT && enumerator.isFrontFacing(device)) ||
                    (direction == CameraState.Direction.BACK && enumerator.isBackFacing(device))
        }?.let { enumerator.createCapturer(it, null) }

}