package com.thoughtcrimes.securesms.onboarding.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thoughtcrimes.securesms.util.ResourceProvider
import com.thoughtcrimes.securesms.util.SharedPreferenceUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import io.beldex.bchat.R
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PinCodeViewModel @Inject constructor(
    private val resourceProvider: ResourceProvider,
    sharedPreferenceUtil: SharedPreferenceUtil,
    savedStateHandle: SavedStateHandle
): ViewModel() {

    private val _state = MutableStateFlow(PinCodeState())
    val state = _state.asStateFlow()

    private val _errorMessage = MutableSharedFlow<String?>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val errorMessage = _errorMessage.asSharedFlow()

    private val _successEvent = MutableSharedFlow<Boolean>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val successEvent = _successEvent.asSharedFlow()

    private var savedPassword: String? = null

    init {
        savedPassword = sharedPreferenceUtil.getSavedPassword()
        val action = savedStateHandle.get<Int>("action") ?: PinCodeAction.VerifyPinCode.action
        _state.update {
            if (action == PinCodeAction.VerifyPinCode.action) {
                it.copy(
                    step = PinCodeSteps.VerifyPin,
                    stepTitle = resourceProvider.getString(R.string.enter_your_4_digit_bchat_pin)
                )
            } else {
                it.copy(
                    step = PinCodeSteps.OldPin,
                    stepTitle = resourceProvider.getString(R.string.enter_old_pin)
                )
            }
        }
    }

    fun onEvent(event: PinCodeEvents) {
        when (event) {
            is PinCodeEvents.PinCodeChanged -> {
                with(state.value) {
                    when (step) {
                        PinCodeSteps.EnterPin -> {
                            _state.update {
                                it.copy(
                                    newPin = event.pinCode
                                )
                            }
                        }
                        PinCodeSteps.OldPin -> {
                            _state.update {
                                it.copy(
                                   pin  = event.pinCode
                                )
                            }
                        }
                        PinCodeSteps.ReEnterPin -> {
                            _state.update {
                                it.copy(
                                    reEnteredPin = event.pinCode
                                )
                            }
                        }
                        PinCodeSteps.VerifyPin -> {
                            _state.update {
                                it.copy(
                                    pin  = event.pinCode
                                )
                            }
                            if (event.pinCode.length == 4) {
                                viewModelScope.launch {
                                    if (event.pinCode == savedPassword) {
                                        _successEvent.emit(true)
                                    } else {
                                        _errorMessage.emit(resourceProvider.getString(R.string.invalid_password))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            PinCodeEvents.Submit -> {
                with(state.value) {
                    when (step) {
                        PinCodeSteps.OldPin -> {
                            if (pin != savedPassword) {
                                viewModelScope.launch {
                                    _errorMessage.emit(resourceProvider.getString(R.string.incorrect_password_entered))
                                }
                            } else {
                                _state.update {
                                    it.copy(
                                        step = PinCodeSteps.EnterPin,
                                        stepTitle = resourceProvider.getString(R.string.enter_new_pin)
                                    )
                                }
                            }
                        }
                        PinCodeSteps.EnterPin -> {
                            if (newPin == savedPassword) {
                                viewModelScope.launch {
                                    _errorMessage.emit(resourceProvider.getString(R.string.old_new_password_same))
                                }
                            } else {
                                _state.update {
                                    it.copy(
                                        step = PinCodeSteps.ReEnterPin,
                                        stepTitle = resourceProvider.getString(R.string.re_enter_your_pin)
                                    )
                                }
                            }
                        }
                        PinCodeSteps.ReEnterPin -> {
                            if (reEnteredPin != newPin) {
                                viewModelScope.launch {
                                    _errorMessage.emit(resourceProvider.getString(R.string.password_does_not_match))
                                }
                            } else {
                                viewModelScope.launch {
                                    _errorMessage.emit("Password changed successfully.")
                                    _successEvent.emit(true)
                                }
                            }
                        }
                        PinCodeSteps.VerifyPin -> Unit
                    }
                }
            }
        }
    }

}