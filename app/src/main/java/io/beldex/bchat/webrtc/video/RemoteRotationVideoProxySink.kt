package io.beldex.bchat.webrtc.video

import org.webrtc.VideoFrame
import org.webrtc.VideoSink

class RemoteRotationVideoProxySink: VideoSink {

    private var targetSink: VideoSink? = null

    var rotation: Int = 0

    override fun onFrame(frame: VideoFrame?) {
        val thisSink = targetSink ?: return
        val thisFrame = frame ?: return

        val modifiedRotation = thisFrame.rotation - rotation

        val newFrame = VideoFrame(thisFrame.buffer, modifiedRotation, thisFrame.timestampNs)
        thisSink.onFrame(newFrame)
    }

    fun setSink(videoSink: VideoSink) {
        targetSink = videoSink
    }

    fun release() {
        targetSink = null
    }

}