package com.thoughtcrimes.securesms.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivityPathBinding
import com.beldex.libbchat.mnode.OnionRequestAPI
import com.beldex.libsignal.utilities.Mnode
import com.thoughtcrimes.securesms.PassphraseRequiredActionBarActivity
import com.thoughtcrimes.securesms.util.*

class PathActivity : PassphraseRequiredActionBarActivity() {
    private lateinit var binding: ActivityPathBinding
    private val broadcastReceivers = mutableListOf<BroadcastReceiver>()

    // region Lifecycle
    override fun onCreate(savedInstanceState: Bundle?, isReady: Boolean) {
        super.onCreate(savedInstanceState, isReady)
        binding = ActivityPathBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar!!.title = resources.getString(R.string.activity_path_title)
        binding.pathRowsContainer.disableClipping()
        update(false)
        registerObservers()
    }

    private fun registerObservers() {
        val buildingPathsReceiver: BroadcastReceiver = object : BroadcastReceiver() {

            override fun onReceive(context: Context, intent: Intent) {
                Log.d("Beldex","ip (1)")
                handleBuildingPathsEvent()
            }
        }
        broadcastReceivers.add(buildingPathsReceiver)
        LocalBroadcastManager.getInstance(this).registerReceiver(buildingPathsReceiver, IntentFilter("buildingPaths"))
        val pathsBuiltReceiver: BroadcastReceiver = object : BroadcastReceiver() {

            override fun onReceive(context: Context, intent: Intent) {
                Log.d("Beldex","ip (2)")
                handlePathsBuiltEvent()
            }
        }
        broadcastReceivers.add(pathsBuiltReceiver)
        LocalBroadcastManager.getInstance(this).registerReceiver(pathsBuiltReceiver, IntentFilter("pathsBuilt"))
        val onionRequestPathCountriesLoadedReceiver: BroadcastReceiver = object : BroadcastReceiver() {

            override fun onReceive(context: Context, intent: Intent) {
                Log.d("Beldex","ip (3)")
                handleOnionRequestPathCountriesLoaded()
            }
        }
        broadcastReceivers.add(onionRequestPathCountriesLoadedReceiver)
        LocalBroadcastManager.getInstance(this).registerReceiver(onionRequestPathCountriesLoadedReceiver, IntentFilter("onionRequestPathCountriesLoaded"))
    }

    override fun onDestroy() {
        for (receiver in broadcastReceivers) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
        }
        super.onDestroy()
    }
    // endregion

    // region Updating
    private fun handleBuildingPathsEvent() { update(false) }
    private fun handlePathsBuiltEvent() { update(false) }
    private fun handleOnionRequestPathCountriesLoaded() { update(false) }
   /* private fun showToast(status:Boolean){
        if(status)
        Toast.makeText(this,"Please check your internet connection",Toast.LENGTH_SHORT).show()
        else
            Toast.makeText(this,"Please check your internet connection",Toast.LENGTH_SHORT).cancel()
    }*/

    private fun update(isAnimated: Boolean) {
        binding.pathRowsContainer.removeAllViews()
        if (OnionRequestAPI.paths.isNotEmpty()) {
            val path = OnionRequestAPI.paths.firstOrNull() ?: return finish()
            val dotAnimationRepeatInterval = path.count().toLong() * 1000 + 1000
            val pathRows = path.mapIndexed { index, mnode ->
                val isGuardMnode = (OnionRequestAPI.guardMnodes.contains(mnode))
                getPathRow(mnode, LineView.Location.Middle, index.toLong() * 1000 + 2000, dotAnimationRepeatInterval, isGuardMnode)
            }
            val youRow = getPathRow(resources.getString(R.string.activity_path_device_row_title), null, LineView.Location.Top, 1000, dotAnimationRepeatInterval)
            val destinationRow = getPathRow(resources.getString(R.string.activity_path_destination_row_title), null, LineView.Location.Bottom, path.count().toLong() * 1000 + 2000, dotAnimationRepeatInterval)
            val rows = listOf( youRow ) + pathRows + listOf( destinationRow )
            for (row in rows) {
                binding.pathRowsContainer.addView(row)
            }
            if (isAnimated) {
                binding.spinner.fadeOut()
            } else {
                binding.spinner.alpha = 0.0f
                binding.spinnerText.visibility=View.GONE
            }
            //showToast(false)
        } else {
            //showToast(true)
            if (isAnimated) {
                binding.spinner.fadeIn()
            } else {
                binding.spinner.alpha = 1.0f
                binding.spinnerText.visibility=View.VISIBLE
            }
        }
    }
    // endregion

    // region General
    private fun getPathRow(title: String, subtitle: String?, location: LineView.Location, dotAnimationStartDelay: Long, dotAnimationRepeatInterval: Long): LinearLayout {
        val mainContainer = LinearLayout(this)
        mainContainer.orientation = LinearLayout.HORIZONTAL
        mainContainer.gravity = Gravity.CENTER_VERTICAL
        mainContainer.disableClipping()
        val mainContainerLayoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        mainContainer.layoutParams = mainContainerLayoutParams
        val lineView = LineView(this, location, dotAnimationStartDelay, dotAnimationRepeatInterval)
        val lineViewLayoutParams = LinearLayout.LayoutParams(resources.getDimensionPixelSize(R.dimen.path_row_expanded_dot_size), resources.getDimensionPixelSize(R.dimen.path_row_height))
        lineView.layoutParams = lineViewLayoutParams
        mainContainer.addView(lineView)
        val titleTextView = TextView(this)
        titleTextView.setTextColor(resources.getColorWithID(R.color.text, theme))
        titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.medium_font_size))
        titleTextView.text = title

        //New Line
        val face = Typeface.createFromAsset(assets, "fonts/poppins_medium.ttf")
        titleTextView.typeface = face

        titleTextView.textAlignment = TextView.TEXT_ALIGNMENT_VIEW_START
        val titleContainer = LinearLayout(this)
        titleContainer.orientation = LinearLayout.VERTICAL
        titleContainer.addView(titleTextView)
        val titleContainerLayoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        titleContainerLayoutParams.marginStart = resources.getDimensionPixelSize(R.dimen.large_spacing)
        titleContainer.layoutParams = titleContainerLayoutParams
        mainContainer.addView(titleContainer)
        if (subtitle != null) {
            val subtitleTextView = TextView(this)
            subtitleTextView.setTextColor(resources.getColorWithID(R.color.text, theme))
            subtitleTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.small_font_size))
            subtitleTextView.text = subtitle

            //New Line
            val face1 = Typeface.createFromAsset(assets, "fonts/poppins_regular.ttf")
            subtitleTextView.typeface = face1

            subtitleTextView.textAlignment = TextView.TEXT_ALIGNMENT_VIEW_START
            titleContainer.addView(subtitleTextView)
        }
        return mainContainer
    }

    private fun getPathRow(mnode: Mnode, location: LineView.Location, dotAnimationStartDelay: Long, dotAnimationRepeatInterval: Long, isGuardMnode: Boolean): LinearLayout {
        val title = if (isGuardMnode) resources.getString(R.string.activity_path_guard_node_row_title) else resources.getString(R.string.activity_path_service_node_row_title)
        val subtitle = if (IP2Country.isInitialized) {
            Log.d("Beldex","ip ${IP2Country.shared.countryNamesCache[mnode.ip]}, ${mnode.ip}")
            IP2Country.shared.countryNamesCache[mnode.ip] ?: resources.getString(R.string.activity_path_resolving_progress)
        } else {
            Log.d("Beldex","ip1 ${IP2Country.shared.countryNamesCache[mnode.ip]}")
            resources.getString(R.string.activity_path_resolving_progress)
        }
        return getPathRow(title, subtitle, location, dotAnimationStartDelay, dotAnimationRepeatInterval)
    }
    // endregion

    // region Line View
    private class LineView : RelativeLayout {
        private lateinit var location: Location
        private var dotAnimationStartDelay: Long = 0
        private var dotAnimationRepeatInterval: Long = 0

        private val dotView by lazy {
            val result = PathDotView(context)
            result.setBackgroundResource(R.drawable.accent_dot)
            result.mainColor = resources.getColorWithID(R.color.accent, context.theme)
            result
        }

        enum class Location {
            Top, Middle, Bottom
        }

        constructor(context: Context, location: Location, dotAnimationStartDelay: Long, dotAnimationRepeatInterval: Long) : super(context) {
            this.location = location
            this.dotAnimationStartDelay = dotAnimationStartDelay
            this.dotAnimationRepeatInterval = dotAnimationRepeatInterval
            setUpViewHierarchy()
        }

        constructor(context: Context) : super(context) {
            throw Exception("Use LineView(context:location:dotAnimationStartDelay:dotAnimationRepeatInterval:) instead.")
        }

        constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
            throw Exception("Use LineView(context:location:dotAnimationStartDelay:dotAnimationRepeatInterval:) instead.")
        }

        constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
            throw Exception("Use LineView(context:location:dotAnimationStartDelay:dotAnimationRepeatInterval:) instead.")
        }

        constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
            throw Exception("Use LineView(context:location:dotAnimationStartDelay:dotAnimationRepeatInterval:) instead.")
        }

        private fun setUpViewHierarchy() {
            disableClipping()
            val lineView = View(context)
            lineView.setBackgroundColor(resources.getColorWithID(R.color.text, context.theme))
            val lineViewHeight = when (location) {
                Location.Top, Location.Bottom -> resources.getDimensionPixelSize(R.dimen.path_row_height) / 2
                Location.Middle -> resources.getDimensionPixelSize(R.dimen.path_row_height)
            }
            val lineViewLayoutParams = LayoutParams(1, lineViewHeight)
            when (location) {
                Location.Top -> lineViewLayoutParams.addRule(ALIGN_PARENT_BOTTOM)
                Location.Middle, Location.Bottom -> lineViewLayoutParams.addRule(ALIGN_PARENT_TOP)
            }
            lineViewLayoutParams.addRule(CENTER_HORIZONTAL)
            lineView.layoutParams = lineViewLayoutParams
            addView(lineView)
            val dotViewSize = resources.getDimensionPixelSize(R.dimen.path_row_dot_size)
            val dotViewLayoutParams = LayoutParams(dotViewSize, dotViewSize)
            dotViewLayoutParams.addRule(CENTER_IN_PARENT)
            dotView.layoutParams = dotViewLayoutParams
            addView(dotView)
            Handler().postDelayed({
                performAnimation()
            }, dotAnimationStartDelay)
        }

        private fun performAnimation() {
            expand()
            Handler().postDelayed({
                collapse()
                Handler().postDelayed({
                    performAnimation()
                }, dotAnimationRepeatInterval)
            }, 1000)
        }

        private fun expand() {
            dotView.animateSizeChange(R.dimen.path_row_dot_size, R.dimen.path_row_expanded_dot_size)
            @ColorRes val startColorID = if (UiModeUtilities.isDayUiMode(context)) R.color.transparent_black_30 else R.color.black
            GlowViewUtilities.animateShadowColorChange(context, dotView, startColorID, R.color.accent)
        }

        private fun collapse() {
            dotView.animateSizeChange(R.dimen.path_row_expanded_dot_size, R.dimen.path_row_dot_size)
            @ColorRes val endColorID = if (UiModeUtilities.isDayUiMode(context)) R.color.transparent_black_30 else R.color.black
            GlowViewUtilities.animateShadowColorChange(context, dotView, R.color.accent, endColorID)
        }
    }
    // endregion
}