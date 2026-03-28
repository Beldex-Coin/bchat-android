package io.beldex.bchat.textformatter

import android.os.Parcel
import android.os.Parcelable
import android.text.TextPaint
import android.text.style.CharacterStyle
import android.text.style.UpdateAppearance

class ForegroundColorSpan(
    private val color: Int
) : CharacterStyle(), UpdateAppearance {

    constructor(src: Parcel) : this(src.readInt())

    override fun updateDrawState(textPaint: TextPaint) {
        textPaint.color = color
    }

    fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(color)
    }

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<ForegroundColorSpan> {
            override fun createFromParcel(p: Parcel) = ForegroundColorSpan(p)
            override fun newArray(size: Int) = arrayOfNulls<ForegroundColorSpan>(size)
        }
    }
}