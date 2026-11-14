package io.beldex.bchat.onboarding

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import io.beldex.bchat.databinding.ViewSeedReminderBinding

class SeedReminderView : FrameLayout {
    private lateinit var binding: ViewSeedReminderBinding
    
    var title: CharSequence
        get() = binding.titleTextView.text
        set(value) { binding.titleTextView.text = value }
    var subtitle: CharSequence
        get() = binding.subtitleTextView.text
        set(value) { binding.subtitleTextView.text = value }
    var delegate: SeedReminderViewDelegate? = null

    constructor(context: Context) : super(context) {
        setUpViewHierarchy()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        setUpViewHierarchy()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        setUpViewHierarchy()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        setUpViewHierarchy()
    }

    private fun setUpViewHierarchy() {
        binding = ViewSeedReminderBinding.inflate(LayoutInflater.from(context), this, true)
        binding.button.setOnClickListener { delegate?.handleSeedReminderViewContinueButtonTapped() }
    }

    fun setProgress(progress: Int, isAnimated: Boolean) {
        binding.progressBar.setProgress(progress, isAnimated)
    }

    fun hideContinueButton() {
        binding.button.visibility = View.GONE
    }
}

interface SeedReminderViewDelegate {

    fun handleSeedReminderViewContinueButtonTapped()
}