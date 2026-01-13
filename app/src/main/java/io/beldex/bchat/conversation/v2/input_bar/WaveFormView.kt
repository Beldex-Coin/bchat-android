package io.beldex.bchat.conversation.v2.input_bar

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

class WaveFormView(context: Context?, attr: AttributeSet?): View(context, attr) {

    private var paint = Paint()
    private var rect: RectF
    private val amplitudes = ArrayList<Float>()
    private val spikes = ArrayList<RectF>()

    private val radius = 6f
    private val spikeWidth = 9f
    private val d = 6f

    private var sw = 0f
    private val sh = 400f

    private var maxSpikes = 0

    init {
        paint.color = Color.rgb(244, 81, 30)
        rect = RectF(20f, 30f, 20+30f, 30f+60f)
        sw = resources.displayMetrics.widthPixels.toFloat()
        maxSpikes = (sw / (spikeWidth + d)).toInt()
    }

    fun addAmplitude(amp: Float) {
        val norm = min(amp.toInt() / 7, 400).toFloat()
        amplitudes.add(norm)

        spikes.clear()
        val amps = amplitudes.takeLast(maxSpikes)
        for (i in amps.indices) {
            val left = sw - i * (spikeWidth + d)
            val top = sh / 2 - amps[i] / 2
            val right = left + spikeWidth
            val bottom = top + amps[i]
            spikes.add(RectF(left, top, right, bottom))
        }

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        spikes.forEach {
            canvas.drawRoundRect(it, radius, radius, paint)
        }
    }

}