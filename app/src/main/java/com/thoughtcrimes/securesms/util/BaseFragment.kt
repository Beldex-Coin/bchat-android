package com.thoughtcrimes.securesms.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.thoughtcrimes.securesms.home.HomeActivity
import io.beldex.bchat.R

open class BaseFragment: Fragment() {

    private var activity: HomeActivity? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = context as HomeActivity
    }

    protected fun replaceFragment(newFragment: Fragment, stackName: String?, extras: Bundle?) {
        activity?.replaceFragment(newFragment, stackName, extras)
    }

    protected fun push(intent: Intent, isForResult: Boolean = false) {
        if (isForResult) {
            startActivityForResult(intent, defaultBchatRequestCode)
        } else {
            startActivity(intent)
        }
        activity?.overridePendingTransition(R.anim.slide_from_right, R.anim.fade_scale_out)
    }

    protected fun show(intent: Intent, isForResult: Boolean = false) {
        if (isForResult) {
            startActivityForResult(intent, defaultBchatRequestCode)
        } else {
            startActivity(intent)
        }
        activity?.overridePendingTransition(R.anim.slide_from_bottom, R.anim.fade_scale_out)
    }

}