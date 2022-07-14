package com.thoughtcrimes.securesms.stablekeyboard

import android.view.inputmethod.InputConnection
import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet

import android.view.LayoutInflater

import android.util.SparseArray
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import io.beldex.bchat.R


class StableAnotherKeyboard(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) :
    GridLayout(context, attrs, defStyleAttr), View.OnClickListener {
    private var button1: Button? = null
    private var button2: Button? = null
    private var button3: Button? = null
    private var button4: Button? = null
    private var button5: Button? = null
    private var button6: Button? = null
    private var button7: Button? = null
    private var button8: Button? = null
    private var button9: Button? = null
    private var button0: Button? = null
    private var buttonDelete: Button? = null
    var buttonEnter: Button? = null
    val keyValues = SparseArray<String>()
    private var inputConnection: InputConnection? = null

    constructor(context: Context?) : this(context, null, 0) {}
    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0) {}

    private fun init(context: Context?, attrs: AttributeSet?) {
        LayoutInflater.from(context).inflate(R.layout.activity_stable_another_keyboard, this, true)
        button1 = findViewById<View>(R.id.buttonNew_1) as Button
        button1!!.setOnClickListener(this)
        button2 = findViewById<View>(R.id.buttonNew_2) as Button
        button2!!.setOnClickListener(this)
        button3 = findViewById<View>(R.id.buttonNew_3) as Button
        button3!!.setOnClickListener(this)
        button4 = findViewById<View>(R.id.buttonNew_4) as Button
        button4!!.setOnClickListener(this)
        button5 = findViewById<View>(R.id.buttonNew_5) as Button
        button5!!.setOnClickListener(this)
        button6 = findViewById<View>(R.id.buttonNew_6) as Button
        button6!!.setOnClickListener(this)
        button7 = findViewById<View>(R.id.buttonNew_7) as Button
        button7!!.setOnClickListener(this)
        button8 = findViewById<View>(R.id.buttonNew_8) as Button
        button8!!.setOnClickListener(this)
        button9 = findViewById<View>(R.id.buttonNew_9) as Button
        button9!!.setOnClickListener(this)
        button0 = findViewById<View>(R.id.buttonNew_0) as Button
        button0!!.setOnClickListener(this)
        buttonDelete = findViewById<View>(R.id.buttonNew_delete) as Button
        buttonDelete!!.setOnClickListener(this)
        buttonEnter = findViewById<View>(R.id.buttonNew_enter) as Button
        buttonEnter!!.setOnClickListener(this)
        keyValues.put(R.id.buttonNew_1, "1")
        keyValues.put(R.id.buttonNew_2, "2")
        keyValues.put(R.id.buttonNew_3, "3")
        keyValues.put(R.id.buttonNew_4, "4")
        keyValues.put(R.id.buttonNew_5, "5")
        keyValues.put(R.id.buttonNew_6, "6")
        keyValues.put(R.id.buttonNew_7, "7")
        keyValues.put(R.id.buttonNew_8, "8")
        keyValues.put(R.id.buttonNew_9, "9")
        keyValues.put(R.id.buttonNew_0, "0")
        keyValues.put(R.id.buttonNew_enter, "✓")
    }

    override fun onClick(view: View) {
        if (inputConnection == null) return
        if (view.id === R.id.buttonNew_delete) {
            val selectedText = inputConnection!!.getSelectedText(0)
            if (TextUtils.isEmpty(selectedText)) {
                inputConnection!!.deleteSurroundingText(1, 0)
            } else {
                inputConnection!!.commitText("", 1)
            }
        }else {
            val value = keyValues[view.getId()]
            inputConnection!!.commitText(value, 1)
        }
    }

    fun setInputConnection(ic: InputConnection?) {
        inputConnection = ic
    }

    init {
        init(context, attrs)
    }
}