package com.thoughtcrimes.securesms.my_account.ui

import androidx.lifecycle.ViewModel
import com.beldex.libbchat.mnode.OnionRequestAPI
import com.beldex.libbchat.utilities.truncateIdForDisplay
import com.thoughtcrimes.securesms.my_account.domain.PathNodeModel
import com.thoughtcrimes.securesms.util.IP2Country
import com.thoughtcrimes.securesms.util.ResourceProvider
import com.thoughtcrimes.securesms.util.SharedPreferenceUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import io.beldex.bchat.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class MyAccountViewModel @Inject constructor(
    private val preferenceUtil: SharedPreferenceUtil,
    private val resourceProvider: ResourceProvider
): ViewModel() {

    data class UIState(
        val profileName: String? = null,
        val publicKey: String = ""
    )

    private val _uiState = MutableStateFlow(UIState())
    val uiState = _uiState.asStateFlow()

    private val _pathState = MutableStateFlow(listOf<PathNodeModel>())
    val pathState = _pathState.asStateFlow()

    init {
        val publicKey = preferenceUtil.getPublicKey()
        _uiState.update {
            it.copy(
                profileName = preferenceUtil.getProfileName() ?: truncateIdForDisplay(publicKey),
                publicKey = publicKey
            )
        }
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

}