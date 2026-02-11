package io.beldex.bchat.home

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.beldex.bchat.database.ThreadDatabase
import io.beldex.bchat.util.SharedPreferenceUtil
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val threadDb: ThreadDatabase,
    private val sharedPreferenceUtil: SharedPreferenceUtil
): ViewModel() {

}