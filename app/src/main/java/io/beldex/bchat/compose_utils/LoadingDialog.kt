package io.beldex.bchat.compose_utils

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import io.beldex.bchat.util.UiMode
import io.beldex.bchat.util.UiModeUtilities

/**
 * Revamp_2026 loading popup ("Saving to media.."): the shared [DialogPanel] with the green
 * [BChatLoader], title and message. Replaces the old Android ProgressDialog.
 */
object LoadingDialog {
    @JvmStatic
    fun create(context: Context, title: String, message: String): Dialog {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        val view = ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
            (context as? ComponentActivity)?.let { activity ->
                setViewTreeLifecycleOwner(activity as LifecycleOwner)
                setViewTreeSavedStateRegistryOwner(activity)
                setViewTreeViewModelStoreOwner(activity)
            }
            setContent {
                BChatTheme(darkTheme = UiModeUtilities.getUserSelectedUiMode(context) == UiMode.NIGHT) {
                    DialogPanel(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 24.dp)
                        ) {
                            BChatLoader(size = 40.dp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = title,
                                color = MaterialTheme.appColors.editTextColor,
                                fontFamily = OpenSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = message,
                                color = MaterialTheme.appColors.onboardingBodyColor,
                                fontFamily = RobotoMono,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
        dialog.setContentView(view)
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        return dialog
    }
}
