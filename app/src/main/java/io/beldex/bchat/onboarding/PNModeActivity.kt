package io.beldex.bchat.onboarding

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.app.AlertDialog
import android.content.Intent
import android.graphics.drawable.TransitionDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivityPnModeBinding
import com.beldex.libbchat.utilities.TextSecurePreferences
import io.beldex.bchat.ApplicationContext
import io.beldex.bchat.BaseActionBarActivity
import io.beldex.bchat.home.HomeActivity
import io.beldex.bchat.util.*
import dagger.hilt.android.AndroidEntryPoint
import io.beldex.bchat.notifications.PushManager
import io.beldex.bchat.notifications.PushRegistry
import javax.inject.Inject

class PNModeActivity : BaseActionBarActivity() {

    @Inject lateinit var pushRegistry: PushRegistry
    private lateinit var binding: ActivityPnModeBinding
    private var selectedOptionView: PNModeView? = null

    // region Lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TextSecurePreferences.setHasSeenWelcomeScreen(this, true)
        binding = ActivityPnModeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpActionBarBchatLogo("",true)
        with(binding) {
            contentView.disableClipping()
            fcmOptionView.setOnClickListener { toggleFCM() }
            fcmOptionView.mainColor = resources.getColorWithID(R.color.pn_option_background, theme)
            fcmOptionView.strokeColor = resources.getColorWithID(R.color.pn_option_border, theme)
            backgroundPollingOptionView.setOnClickListener { toggleBackgroundPolling() }
            backgroundPollingOptionView.mainColor = resources.getColorWithID(R.color.pn_option_background, theme)
            backgroundPollingOptionView.strokeColor = resources.getColorWithID(R.color.pn_option_border, theme)
            registerButton.setOnClickListener { register() }
        }
        toggleFCM()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_pn_mode, menu)
        return true
    }
    // endregion

    // region Animation
    private fun performTransition(@DrawableRes transitionID: Int, subject: View) {
        val drawable = resources.getDrawable(transitionID, theme) as TransitionDrawable
        subject.background = drawable
        drawable.startTransition(250)
    }
    // endregion

    // region Interaction
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.learnMoreButton -> learnMore()
            else -> { /* Do nothing */ }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun learnMore() {
        try {
            val url = "https://www.beldex.io/"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, R.string.invalid_url, Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleFCM() = with(binding) {
        when (selectedOptionView) {
            null -> {
                performTransition(R.drawable.pn_option_background_select_transition, fcmOptionView)
                GlowViewUtilities.animateShadowColorChange(this@PNModeActivity, fcmOptionView, R.color.transparent, R.color.accent)
                animateStrokeColorChange(fcmOptionView, R.color.pn_option_border, R.color.accent)
                selectedOptionView = fcmOptionView
            }
            fcmOptionView -> {
                performTransition(R.drawable.pn_option_background_deselect_transition, fcmOptionView)
                GlowViewUtilities.animateShadowColorChange(this@PNModeActivity, fcmOptionView, R.color.accent, R.color.transparent)
                animateStrokeColorChange(fcmOptionView, R.color.accent, R.color.pn_option_border)
                selectedOptionView = null
            }
            backgroundPollingOptionView -> {
                performTransition(R.drawable.pn_option_background_select_transition, fcmOptionView)
                GlowViewUtilities.animateShadowColorChange(this@PNModeActivity, fcmOptionView, R.color.transparent, R.color.accent)
                animateStrokeColorChange(fcmOptionView, R.color.pn_option_border, R.color.accent)
                performTransition(R.drawable.pn_option_background_deselect_transition, backgroundPollingOptionView)
                GlowViewUtilities.animateShadowColorChange(this@PNModeActivity, backgroundPollingOptionView, R.color.accent, R.color.transparent)
                animateStrokeColorChange(backgroundPollingOptionView, R.color.accent, R.color.pn_option_border)
                selectedOptionView = fcmOptionView
            }
        }
    }

    private fun toggleBackgroundPolling() = with(binding) {
        when (selectedOptionView) {
            null -> {
                performTransition(R.drawable.pn_option_background_select_transition, backgroundPollingOptionView)
                GlowViewUtilities.animateShadowColorChange(this@PNModeActivity, backgroundPollingOptionView, R.color.transparent, R.color.accent)
                animateStrokeColorChange(backgroundPollingOptionView, R.color.pn_option_border, R.color.accent)
                selectedOptionView = backgroundPollingOptionView
            }
            backgroundPollingOptionView -> {
                performTransition(R.drawable.pn_option_background_deselect_transition, backgroundPollingOptionView)
                GlowViewUtilities.animateShadowColorChange(this@PNModeActivity, backgroundPollingOptionView, R.color.accent, R.color.transparent)
                animateStrokeColorChange(backgroundPollingOptionView, R.color.accent, R.color.pn_option_border)
                selectedOptionView = null
            }
            fcmOptionView -> {
                performTransition(R.drawable.pn_option_background_select_transition, backgroundPollingOptionView)
                GlowViewUtilities.animateShadowColorChange(this@PNModeActivity, backgroundPollingOptionView, R.color.transparent, R.color.accent)
                animateStrokeColorChange(backgroundPollingOptionView, R.color.pn_option_border, R.color.accent)
                performTransition(R.drawable.pn_option_background_deselect_transition, fcmOptionView)
                GlowViewUtilities.animateShadowColorChange(this@PNModeActivity, fcmOptionView, R.color.accent, R.color.transparent)
                animateStrokeColorChange(fcmOptionView, R.color.accent, R.color.pn_option_border)
                selectedOptionView = backgroundPollingOptionView
            }
        }
    }

    private fun animateStrokeColorChange(bubble: PNModeView, @ColorRes startColorID: Int, @ColorRes endColorID: Int) {
        val startColor = resources.getColorWithID(startColorID, theme)
        val endColor = resources.getColorWithID(endColorID, theme)
        val animation = ValueAnimator.ofObject(ArgbEvaluator(), startColor, endColor)
        animation.duration = 250
        animation.addUpdateListener { animator ->
            val color = animator.animatedValue as Int
            bubble.strokeColor = color
        }
        animation.start()
    }

    private fun register() {
        if (selectedOptionView == null) {
            val dialog = AlertDialog.Builder(this)
            dialog.setTitle(R.string.activity_pn_mode_no_option_picked_dialog_title)
            dialog.setPositiveButton(R.string.ok) { _, _ -> }
            dialog.create().show()
            return
        }
        TextSecurePreferences.setPushEnabled(this, (selectedOptionView == binding.fcmOptionView))
        val application =ApplicationContext.getInstance(this)
        application.startPollingIfNeeded()
        pushRegistry.refresh(true)
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        show(intent)
    }
    // endregion
}