package io.beldex.bchat.onboarding.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.beldex.bchat.util.ResourceProvider
import io.beldex.bchat.util.SharedPreferenceUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import io.beldex.bchat.R
import kotlinx.coroutines.channels.BufferOverflow
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
    private val sharedPreferenceUtil: SharedPreferenceUtil,
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

    private val _successContent = MutableSharedFlow<String?>(
            replay = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val successContent = _successContent.asSharedFlow()

    private var savedPassword: String? = null
    private var walletSavedPassword: String? = null
    private var action: Int = -1

    init {
        savedPassword = sharedPreferenceUtil.getSavedPassword()
        walletSavedPassword= sharedPreferenceUtil.getWalletSavePassword()
        action = savedStateHandle.get<Int>(EXTRA_PIN_CODE_ACTION) ?: PinCodeAction.VerifyPinCode.action
        _state.update {
            when (action) {
                PinCodeAction.CreatePinCode.action -> {
                    it.copy(
                        step = PinCodeSteps.EnterPin,
                        stepTitle = resourceProvider.getString(R.string.enter_your_pin)
                    )
                }
                PinCodeAction.VerifyPinCode.action -> {
                    it.copy(
                        step = PinCodeSteps.VerifyPin,
                        stepTitle = resourceProvider.getString(R.string.enter_your_4_digit_bchat_pin)
                    )
                }
                PinCodeAction.ChangePinCode.action -> {
                    it.copy(
                        step = PinCodeSteps.OldPin,
                        stepTitle = resourceProvider.getString(R.string.enter_old_pin)
                    )
                }
                PinCodeAction.VerifyWalletPin.action -> {
                    it.copy(
                        step = PinCodeSteps.VerifyPin,
                        stepTitle = resourceProvider.getString(R.string.enter_your_4_digit_wallet_pin)
                    )
                }
                PinCodeAction.CreateWalletPin.action -> {
                    it.copy(
                        step = PinCodeSteps.EnterPin,
                        stepTitle = resourceProvider.getString(R.string.enter_your_pin)
                    )
                }
                PinCodeAction.ChangeWalletPin.action -> {
                    it.copy(
                        step = PinCodeSteps.OldPin,
                        stepTitle = resourceProvider.getString(R.string.enter_old_pin)
                    )
                }
                else -> it
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
                            if (action != PinCodeAction.VerifyWalletPin.action && event.pinCode.length == 4) {
                                viewModelScope.launch {
                                    if (event.pinCode == savedPassword) {
                                        _successEvent.emit(false)
                                    } else {
                                        _state.update {
                                            it.copy(
                                                pin = ""
                                            )
                                        }
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
                                _state.update {
                                    it.copy(
                                        pin = ""
                                    )
                                }
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
                            println("called wallet pin changed 1")
                            if (action == PinCodeAction.ChangePinCode.action && newPin == savedPassword) {
                                _state.update {
                                    it.copy(
                                        newPin = ""
                                    )
                                }
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
                                _state.update {
                                    it.copy(
                                        reEnteredPin = ""
                                    )
                                }
                                viewModelScope.launch {
                                    _errorMessage.emit(resourceProvider.getString(R.string.password_does_not_match))
                                }
                            } else {
                                sharedPreferenceUtil.setPassword(newPin)
                                viewModelScope.launch {
                                    val message = if (action == PinCodeAction.CreatePinCode.action) {
                                        resourceProvider.getString(R.string.pincode_created)
                                    } else {
                                        resourceProvider.getString(R.string.pincode_changed)

                                    }
                                    _successContent.emit(message)
                                    /*_errorMessage.emit(message)*/
                                    _successEvent.emit(true)
                                }
                            }
                        }
                        PinCodeSteps.VerifyPin -> Unit
                    }
                }
            }
            PinCodeEvents.ResetPinCode -> {
                when (state.value.step) {
                    PinCodeSteps.EnterPin -> {
                        _state.update {
                            it.copy(
                                newPin = ""
                            )
                        }
                    }
                    PinCodeSteps.OldPin -> {
                        _state.update {
                            it.copy(
                                pin = ""
                            )
                        }
                    }
                    PinCodeSteps.ReEnterPin -> {
                        _state.update {
                            it.copy(
                                reEnteredPin = ""
                            )
                        }
                    }
                    PinCodeSteps.VerifyPin -> {
                        _state.update {
                            it.copy(
                                pin = ""
                            )
                        }
                    }
                }
            }
        }
    }

    fun handleWalletPinActions() {
        with(state.value) {
            when (step) {
                PinCodeSteps.OldPin -> {
                    _state.update {
                        it.copy(
                            step = PinCodeSteps.EnterPin,
                            stepTitle = resourceProvider.getString(R.string.enter_new_pin)
                        )
                    }
                }
                PinCodeSteps.EnterPin -> {
                    println("called wallet pin changed $action and $newPin and $walletSavedPassword")
                    if (action == PinCodeAction.ChangeWalletPin.action && newPin == walletSavedPassword) {
                        _state.update {
                            it.copy(
                                    newPin = ""
                            )
                        }
                        viewModelScope.launch {
                            _errorMessage.emit(resourceProvider.getString(R.string.old_new_pin_same))
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
                PinCodeSteps.ReEnterPin -> Unit
                PinCodeSteps.VerifyPin -> Unit
            }
        }
    }

}