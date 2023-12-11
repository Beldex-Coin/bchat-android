package com.thoughtcrimes.securesms.my_account.ui

import androidx.lifecycle.ViewModel
import com.beldex.libbchat.utilities.truncateIdForDisplay
import com.thoughtcrimes.securesms.util.SharedPreferenceUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class MyAccountViewModel @Inject constructor(
    private val preferenceUtil: SharedPreferenceUtil
): ViewModel() {

    data class UIState(
        val profileName: String? = null,
        val publicKey: String = ""
    )

    private val _uiState = MutableStateFlow(UIState())
    val uiState = _uiState.asStateFlow()

    init {
        val publicKey = preferenceUtil.getPublicKey()
        _uiState.update {
            it.copy(
                profileName = preferenceUtil.getProfileName() ?: truncateIdForDisplay(publicKey),
                publicKey = publicKey
            )
        }
    }

}