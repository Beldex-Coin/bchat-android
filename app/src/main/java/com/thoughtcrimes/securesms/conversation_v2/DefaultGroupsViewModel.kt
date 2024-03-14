package com.thoughtcrimes.securesms.conversation_v2

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.beldex.libbchat.messaging.open_groups.OpenGroupAPIV2
import com.thoughtcrimes.securesms.util.State
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update

typealias DefaultGroups = List<OpenGroupAPIV2.DefaultGroup>
typealias GroupState = State<DefaultGroups>

class DefaultGroupsViewModel : ViewModel() {

    data class UIState(
        val groupUrl: String = ""
    )

    private val _uiState = MutableStateFlow(UIState())
    val uiState = _uiState.asStateFlow()

    init {
        OpenGroupAPIV2.getDefaultRoomsIfNeeded()
    }

    val defaultRooms = OpenGroupAPIV2.defaultRooms.map<DefaultGroups, GroupState> {
        State.Success(it)
    }.onStart {
        emit(State.Loading)
    }.asLiveData()

    fun onEvent(event: OpenGroupEvents) {
        when (event) {
            is OpenGroupEvents.GroupUrlChanged -> {
                _uiState.update {
                    it.copy(
                        groupUrl = event.url
                    )
                }
            }
        }
    }
}