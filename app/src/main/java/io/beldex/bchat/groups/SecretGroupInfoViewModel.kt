package io.beldex.bchat.groups

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.beldex.libbchat.messaging.contacts.Contact
import io.beldex.bchat.dependencies.DatabaseComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


class SecretGroupInfoViewModel @Inject constructor(private val groupID: String,
                                                   private val groupRepository: SecretGroupInfoRepository,private val context : Context
) : ViewModel() {

    private val _groupMembers = MutableStateFlow<GroupMembers?>(null)
    val groupMembers: StateFlow<GroupMembers?> = _groupMembers.asStateFlow()

    private val _isEnableNotification = MutableLiveData<String>()
    val isEnableNotification: LiveData<String> get () = _isEnableNotification

    private val _isExpirationItem = MutableLiveData<Int>()
    val isExpirationItem: LiveData<Int> get () = _isExpirationItem

    private val _isShowBottomSheet = MutableLiveData<Boolean>()
    val isShowBottomSheet: LiveData<Boolean> get() = _isShowBottomSheet

    val _searchQuery = MutableStateFlow("")
    var searchQuery: StateFlow<String> = _searchQuery
    init {
        fetchGroupMembers()
    }
     fun fetchGroupMembers() {
        viewModelScope.launch {
            val members = groupRepository.getGroupMembers(groupID)
            _groupMembers.value = members
        }
    }
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun getUserDisplayName(publicKey : String,context : Context) : String {
        val contact=
            DatabaseComponent.get(context).bchatContactDatabase().getContactWithBchatID(publicKey)
        return contact?.displayName(Contact.ContactContext.REGULAR) ?: publicKey
    }

    fun updateNotificationType(selectOption: String) {
        _isEnableNotification.value = selectOption
    }
    fun updateExpirationItem(selectOption: Int) {
        _isExpirationItem.value = selectOption
    }
    fun updateVisibleBottomSheet(isVisible : Boolean){
        _isShowBottomSheet.value = isVisible
    }
}
data class GroupMembers(
    val members: List<String>
)

class SecretGroupViewModelFactory(
    private val groupID: String,
    private val context : Context,
    private val groupRepository: SecretGroupInfoRepository = SecretGroupInfoRepository(DatabaseComponent.get(context).groupDatabase())
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SecretGroupInfoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SecretGroupInfoViewModel(groupID, groupRepository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}