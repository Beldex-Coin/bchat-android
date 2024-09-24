package io.beldex.bchat.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.core.net.toUri
import androidx.fragment.app.DialogFragment
import com.beldex.libbchat.utilities.TextSecurePreferences
import io.beldex.bchat.compose_utils.PromotionDialog
import io.beldex.bchat.mms.GlideRequests
import io.beldex.bchat.util.FirebaseRemoteConfigUtil
import io.beldex.bchat.util.UiMode
import io.beldex.bchat.util.UiModeUtilities
import dagger.hilt.android.AndroidEntryPoint
import io.beldex.bchat.R
import org.json.JSONObject
import javax.inject.Inject

@AndroidEntryPoint
class PromotionOfferDialog: DialogFragment() {

    @Inject
    lateinit var remoteConfig: FirebaseRemoteConfigUtil
    @Inject
    lateinit var preferences: TextSecurePreferences
    lateinit var glide: GlideRequests

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.PromotionDialogStyle);
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val isDarkTheme = UiModeUtilities.getUserSelectedUiMode(requireContext()) == UiMode.NIGHT
        val promotionObject = JSONObject(remoteConfig.getPromotionData())
        val bannerUrl = if (isDarkTheme)
            promotionObject.optString("banner_dark")
        else
            promotionObject.optString("banner_light")
        val redirectUrl = promotionObject.optString("landing_url")
        val redirectToLandingPage: (String) -> Unit = {
            val intent = Intent()
            intent.action = Intent.ACTION_VIEW
            intent.data = it.toUri()
            requireContext().startActivity(intent)
            dismiss()
        }
        return ComposeView(requireContext()).apply {
            setContent {
                PromotionDialog(
                    bannerUrl = bannerUrl,
                    redirectToLandingPage = {
                        redirectToLandingPage(redirectUrl)
                        preferences.setPromotionDialogClicked()
                    },
                    isDarkTheme = isDarkTheme,
                    dismiss = {
                        preferences.setPromotionDialogIgnoreCount(preferences.getPromotionDialogIgnoreCount() + 1)
                        dismiss()
                    }
                )
            }
        }
    }

    companion object {
        const val TAG = "PromotionOfferDialog"
        fun newInstance(): PromotionOfferDialog{
            val args = Bundle()
            val fragment = PromotionOfferDialog()
            fragment.arguments = args
            return fragment
        }
    }

}