package io.beldex.bchat.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.beldex.bchat.BaseActionBarActivity
import io.beldex.bchat.conversation.v2.utilities.BaseDialog
import io.beldex.bchat.R

val defaultBchatRequestCode: Int
    get() = 42

fun BaseActionBarActivity.setUpActionBarBchatLogo(title: String, hideBackButton: Boolean = false) {

    supportActionBar?.let { actionbar ->
        actionbar.setDisplayShowHomeEnabled(false)
        actionbar.setDisplayShowTitleEnabled(false)
        actionbar.setDisplayHomeAsUpEnabled(false)
        actionbar.setHomeButtonEnabled(false)

        actionbar.setCustomView(R.layout.bchat_logo_action_bar_content)
        actionbar.setDisplayShowCustomEnabled(true)

        val rootView: Toolbar = actionbar.customView!!.parent as Toolbar
        rootView.setPadding(0, 0, 0, 0)
        rootView.setContentInsetsAbsolute(0, 0);

        val backButton = actionbar.customView!!.findViewById<View>(R.id.back_button)
        val titleName = actionbar.customView!!.findViewById<TextView>(R.id.title_name)
        titleName.text = title
        if (hideBackButton) {
            backButton.visibility = View.GONE
        } else {
            backButton.visibility = View.VISIBLE
            backButton.setOnClickListener {
                onSupportNavigateUp()
            }
        }
    }
}

fun ComponentActivity.push(intent: Intent, isForResult: Boolean = false) {
    if (isForResult) {
        startActivityForResult(intent, defaultBchatRequestCode)
    } else {
        startActivity(intent)
    }
    overridePendingTransition(R.anim.slide_from_right, R.anim.fade_scale_out)
}

fun AppCompatActivity.show(intent: Intent, isForResult: Boolean = false) {
    if (isForResult) {
        startActivityForResult(intent, defaultBchatRequestCode)
    } else {
        startActivity(intent)
    }
    overridePendingTransition(R.anim.slide_from_bottom, R.anim.fade_scale_out)
}

interface ActivityDispatcher {
    companion object {
        const val SERVICE = "ActivityDispatcher_SERVICE"
        @SuppressLint("WrongConstant")
        fun get(context: Context) = context.getSystemService(SERVICE) as? ActivityDispatcher
    }
    fun dispatchIntent(body: (Context)->Intent?)
    fun showDialog(baseDialog: BaseDialog, tag: String? = null)
    fun showBottomSheetDialog(bottomSheetDialogFragment: BottomSheetDialogFragment,tag: String? = null)
    fun showBottomSheetDialogWithBundle(bottomSheetDialogFragment: BottomSheetDialogFragment,tag: String? = null, bundle: Bundle)
}