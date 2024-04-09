package com.thoughtcrimes.securesms.calls

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import io.beldex.bchat.R

class SpinnerIconAdapter(
        context: Context,
        private val defaultIcons: List<Int>,
) : ArrayAdapter<Int>(context, 0, defaultIcons) {
    private var selectedIcons: List<Int> = emptyList()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView
                ?: LayoutInflater.from(context).inflate(R.layout.spinner_item_with_icon, parent, false)
        val item = defaultIcons[position]
        val imageView = view.findViewById<ImageView>(R.id.image_view)
        imageView.setImageResource(item)
        return view
    }

    private fun createView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView
                ?: LayoutInflater.from(context).inflate(R.layout.spinner_item_with_icon, parent, false)
        val imageView = view.findViewById<ImageView>(R.id.image_view)
        println("earPieceIconUpdate updated value $isEarPieceIconUpdate")

        selectedIcons = if (WebRtcCallActivity.isBlueToothConnect) {
            listOf(if (isEarPieceIconUpdate) {
                R.drawable.speaker_with_dropdown_gray
            } else {
                R.drawable.speaker_with_dropdown
            }, R.drawable.bluetooth_with_dropdown)
        } else {
            listOf(R.drawable.speaker_with_dropdown)
        }
        imageView.setImageResource(selectedIcons[position])
        return view

    }

    fun earPieceIconUpdate(isEarPiece: Boolean) {
        isEarPieceIconUpdate = isEarPiece
    }

    companion object {
        var isEarPieceIconUpdate: Boolean = false
    }

}