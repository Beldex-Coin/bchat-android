package com.thoughtcrimes.securesms.my_account.ui

import androidx.lifecycle.ViewModel
import com.thoughtcrimes.securesms.util.AssetFileHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class ContentViewModel @Inject constructor(
    private val assetHelper: AssetFileHelper
): ViewModel() {

    private val _content = MutableStateFlow("")
    val content = _content.asStateFlow()

    init {
        _content.value = assetHelper.loadAboutContent() ?: ""
    }

}