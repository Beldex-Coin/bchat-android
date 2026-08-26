package io.beldex.bchat.webrtc.video

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.webrtc.VideoFrame
import org.webrtc.VideoSink

class RemoteRotationVideoProxySink: VideoSink {

    private var targetSink: VideoSink? = null

    var rotation: Int = 0

    private val _displayAspect = MutableStateFlow(1f)
    val displayAspect: StateFlow<Float> = _displayAspect.asStateFlow()

    override fun onFrame(frame: VideoFrame?) {
        val thisSink = targetSink ?: return
        val thisFrame = frame ?: return

        val modifiedRotation = thisFrame.rotation - rotation

        val newFrame = VideoFrame(thisFrame.buffer, modifiedRotation, thisFrame.timestampNs)

        val rotatedWidth =
            if (modifiedRotation % 180 == 0) thisFrame.buffer.width else thisFrame.buffer.height
        val rotatedHeight =
            if (modifiedRotation % 180 == 0) thisFrame.buffer.height else thisFrame.buffer.width
        if (rotatedWidth > 0 && rotatedHeight > 0) {
            _displayAspect.value = rotatedWidth.toFloat() / rotatedHeight.toFloat()
        }

        thisSink.onFrame(newFrame)
    }

    fun setSink(videoSink: VideoSink) {
        targetSink = videoSink
    }

    fun release() {
        targetSink = null
    }

}