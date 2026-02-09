package io.beldex.bchat.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import io.beldex.bchat.home.HomeActivity
import io.beldex.bchat.R
import io.beldex.bchat.conversation.v2.ConversationActivityV2

open class BaseFragment: Fragment() {

    private var activity: ConversationActivityV2? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = context as ConversationActivityV2
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