package com.thoughtcrimes.securesms.util

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.text.TextPaint
import android.text.TextUtils
import io.beldex.bchat.R
import java.math.BigInteger
import java.security.MessageDigest
import java.util.*

object AvatarPlaceholderGenerator {

    private const val EMPTY_LABEL = "0"

    @SuppressLint("ResourceAsColor")
    @JvmStatic
    fun generate(context: Context, pixelSize: Int, hashString: String, displayName: String?): BitmapDrawable {
        val hash: Long
        if (hashString.length >= 12 && hashString.matches(Regex("^[0-9A-Fa-f]+\$"))) {
            hash = getSha512(hashString).substring(0 until 12).toLong(16)
        } else {
            hash = 0
        }

        // Do not cache color array, it may be different depends on the current theme.
        val colorArray = context.resources.getIntArray(R.array.profile_picture_placeholder_colors)
        val colorPrimary = colorArray[(hash % colorArray.size).toInt()]

        val labelText = when {
            !TextUtils.isEmpty(displayName) -> extractLabel(displayName!!.capitalize(Locale.ROOT))
            !TextUtils.isEmpty(hashString) -> extractLabel(hashString)
            else -> EMPTY_LABEL
        }

        val bitmap = Bitmap.createBitmap(pixelSize, pixelSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw background/frame
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = colorPrimary
        val radius = pixelSize.toFloat() / 2
        canvas.drawCircle(radius, radius, radius, paint)
        // Draw text
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textPaint.textSize = pixelSize * 0.5f
        textPaint.color = Color.WHITE
        val textBounds = Rect()
        textPaint.getTextBounds(labelText, 0, labelText.length, textBounds)
        val textX = (canvas.width - textBounds.width()) / 2.0f
        val textY = (canvas.height + textBounds.height()) / 2.0f
        canvas.drawText(labelText, textX, textY, textPaint)
        return BitmapDrawable(context.resources, bitmap)
    }

    fun extractLabel(content: String): String {
        var trimmedContent = content.trim()
        if (trimmedContent.isEmpty()) return EMPTY_LABEL
        return if (trimmedContent.length > 2 && trimmedContent.startsWith("bd")) {
            trimmedContent[2].toString()
        } else {
            val splitWords = trimmedContent.split(Regex("\\W"))
            if (splitWords.size < 2) {
                trimmedContent.take(2)
            } else {
                splitWords.filter { word -> word.isNotEmpty() }.take(2).map { it.first() }.joinToString("")
            }
        }.uppercase()
    }

    private fun getSha512(input: String): String {
        val messageDigest = MessageDigest.getInstance("SHA-512").digest(input.toByteArray())

        // Convert byte array into signum representation
        val no = BigInteger(1, messageDigest)

        // Convert message digest into hex value
        var hashText: String = no.toString(16)

        // Add preceding 0s to make it 32 bytes
        if (hashText.length < 128) {
            val sb = StringBuilder()
            for (i in 0 until 128 - hashText.length) {
                sb.append('0')
            }
            hashText = sb.append(hashText).toString()
        }

        return hashText
    }
}