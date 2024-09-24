package io.beldex.bchat.my_account.ui

import androidx.lifecycle.ViewModel
import io.beldex.bchat.my_account.domain.ChangeLogModel
import io.beldex.bchat.util.AssetFileHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONException
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class ChangeLogViewModel @Inject constructor(
    private val assetHelper: AssetFileHelper
): ViewModel() {

    private val _changeLogs = MutableStateFlow(listOf<ChangeLogModel>())
    val changeLogs = _changeLogs.asStateFlow()

    init {
        populateChangeLogs()
    }

    private fun populateChangeLogs() {
        val versionsList = mutableListOf<ChangeLogModel>()
        try {
            assetHelper.loadChangeLogsFromAsset()?.let { jsonLogs ->
                val obj = JSONObject(jsonLogs)
                val changeLogArray = obj.getJSONArray("change_log")
                for (i in 0 until changeLogArray.length()) {
                    val changeLogObject = changeLogArray.getJSONObject(i)
                    val changeLogTitleValue = changeLogObject.getString("title")
                    val descriptionsArray = changeLogObject.getJSONArray("descriptions")
                    val versionLogs = mutableListOf<String>()
                    for (j in 0 until descriptionsArray.length()) {
                        val descriptionsObject = descriptionsArray.getJSONObject(j)
                        val descriptionsObjectValue = descriptionsObject.getString("description")
                        versionLogs.add(descriptionsObjectValue)
                    }
                    versionsList.add(
                        ChangeLogModel(
                            version = changeLogTitleValue,
                            logs = versionLogs
                        )
                    )
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        _changeLogs.value = versionsList
    }

}