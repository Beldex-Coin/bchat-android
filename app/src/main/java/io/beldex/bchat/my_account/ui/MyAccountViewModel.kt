package io.beldex.bchat.my_account.ui

import android.content.Context
import android.content.res.Configuration
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.beldex.libbchat.mnode.OnionRequestAPI
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libbchat.utilities.dynamiclanguage.DynamicLanguageContextWrapper
import com.beldex.libbchat.utilities.truncateIdForDisplay
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.beldex.bchat.R
import io.beldex.bchat.my_account.domain.PathNodeModel
import io.beldex.bchat.util.AppLanguageEvent
import io.beldex.bchat.util.IP2Country
import io.beldex.bchat.util.ResourceProvider
import io.beldex.bchat.util.SharedPreferenceUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class MyAccountViewModel @Inject constructor(
    @ApplicationContext context : Context,
    private val preferenceUtil: SharedPreferenceUtil,
    private val resourceProvider: ResourceProvider
): ViewModel() {

    data class UIState(
        val profileName: String? = null,
        val publicKey: String = "",
        val additionalDisplayName: String? = null,
        val additionalPublicKey: String? = ""
    )

    private val _uiState = MutableStateFlow(UIState())
    val uiState = _uiState.asStateFlow()

    private val _pathState = MutableStateFlow(listOf<PathNodeModel>())
    val pathState = _pathState.asStateFlow()

    private val _isProfileChanged = MutableLiveData<Boolean>()
    val isProfileChanged: LiveData<Boolean> get () = _isProfileChanged

    private val _showLoader = MutableLiveData<Boolean>()
    val showLoader: LiveData<Boolean> get () = _showLoader

    private val _selectedLanguageCode = MutableStateFlow(TextSecurePreferences.getAppSelectedLanguage(context) ?: Locale.getDefault().language)

    val selectedLanguageCode: StateFlow<String> =
        _selectedLanguageCode.asStateFlow()

    fun selectLanguage(code: String, context: Context) {
        _selectedLanguageCode.value = code
        val localizedContext = getLocalizedContext(context, code)
        DynamicLanguageContextWrapper.updateContext(
            context, code)
        resourceProvider.updateContext(localizedContext)
    }

    private fun getLocalizedContext(context: Context, code: String): Context {
        val locale = Locale(code)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    init {
        val publicKey = preferenceUtil.getPublicKey()
        _uiState.update {
            it.copy(
                profileName = preferenceUtil.getProfileName() ?: truncateIdForDisplay(publicKey),
                publicKey = publicKey
            )
        }
        AppLanguageEvent.deviceLanguage.value = TextSecurePreferences.getDeviceLanguage(context) ?: Locale.getDefault().language
    }

    fun getPathNodes() {
        if (OnionRequestAPI.paths.isNotEmpty()) {
            val path = OnionRequestAPI.paths.firstOrNull() ?: return
            _pathState.value = path.mapIndexed { _, mNode ->
                val isGuardNode = (OnionRequestAPI.guardMnodes.contains(mNode))
                PathNodeModel(
                    title = if (isGuardNode) {
                        resourceProvider.getString(R.string.activity_path_guard_node_row_title)
                    } else {
                        resourceProvider.getString(R.string.activity_path_service_node_row_title)
                    },
                    subTitle = if (IP2Country.isInitialized) {
                        IP2Country.shared.countryNamesCache[mNode.ip] ?: resourceProvider.getString(R.string.activity_path_resolving_progress)
                    } else {
                        resourceProvider.getString(R.string.activity_path_resolving_progress)
                    }
                )
            }
        }
    }

    fun refreshProfileName() {
        _uiState.update {
            it.copy(
                profileName = preferenceUtil.getProfileName() ?: truncateIdForDisplay(uiState.value.publicKey),
            )
        }
    }

    fun updateProfile(updated: Boolean){
        _isProfileChanged.value = updated
    }

    fun updateLoaderStatus(status: Boolean){
        _showLoader.value = status
    }

}