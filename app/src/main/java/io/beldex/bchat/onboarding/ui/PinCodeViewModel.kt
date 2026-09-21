package io.beldex.bchat.onboarding.ui

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libbchat.utilities.dynamiclanguage.DynamicLanguageContextWrapper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.beldex.bchat.R
import io.beldex.bchat.util.SharedPreferenceUtil
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class PinCodeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sharedPreferenceUtil: SharedPreferenceUtil,
    savedStateHandle: SavedStateHandle
): ViewModel() {

    private val localizedContext: Context by lazy {
        val language = TextSecurePreferences.getAppSelectedLanguage(context)
            ?: Locale.getDefault().language
        DynamicLanguageContextWrapper.updateContext(context, language)
    }

    private fun stringRes(@StringRes res: Int): String = localizedContext.getString(res)

    private fun stringRes(@StringRes res: Int, vararg formatArgs: Any): String =
        localizedContext.getString(res, *formatArgs)

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
        val savedPinLength = sharedPreferenceUtil.getPinLength()
        _state.update {
            when (action) {
                PinCodeAction.CreatePinCode.action -> {
                    it.copy(
                        pinLength = savedPinLength,
                        step = PinCodeSteps.EnterPin,
                        stepTitle = stringRes(R.string.enter_your_pin)
                    )
                }
                PinCodeAction.VerifyPinCode.action -> {
                    it.copy(
                        pinLength = savedPinLength,
                        step = PinCodeSteps.VerifyPin,
                        stepTitle = stringRes(R.string.enter_your_4_digit_bchat_pin, savedPinLength)
                    )
                }
                PinCodeAction.ChangePinCode.action -> {
                    it.copy(
                        pinLength = savedPinLength,
                        step = PinCodeSteps.OldPin,
                        stepTitle = stringRes(R.string.enter_old_pin)
                    )
                }
                PinCodeAction.VerifyWalletPin.action -> {
                    it.copy(
                        pinLength = savedPinLength,
                        step = PinCodeSteps.VerifyPin,
                        stepTitle = stringRes(R.string.enter_your_4_digit_wallet_pin, savedPinLength)
                    )
                }
                PinCodeAction.CreateWalletPin.action -> {
                    it.copy(
                        pinLength = savedPinLength,
                        step = PinCodeSteps.EnterPin,
                        stepTitle = stringRes(R.string.enter_your_pin)
                    )
                }
                PinCodeAction.ChangeWalletPin.action -> {
                    it.copy(
                        pinLength = savedPinLength,
                        step = PinCodeSteps.OldPin,
                        stepTitle = stringRes(R.string.enter_old_pin)
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
                            if (action != PinCodeAction.VerifyWalletPin.action && event.pinCode.length == state.value.pinLength) {
                                viewModelScope.launch {
                                    if (event.pinCode == savedPassword) {
                                        _successEvent.emit(false)
                                    } else {
                                        _state.update {
                                            it.copy(
                                                pin = ""
                                            )
                                        }
                                        _errorMessage.emit(stringRes(R.string.invalid_password))
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
                                    _errorMessage.emit(stringRes(R.string.incorrect_password_entered))
                                }
                            } else {
                                _state.update {
                                    it.copy(
                                        step = PinCodeSteps.EnterPin,
                                        stepTitle = stringRes(R.string.enter_new_pin)
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
                                    _errorMessage.emit(stringRes(R.string.old_new_password_same))
                                }
                            } else {
                                _state.update {
                                    it.copy(
                                        step = PinCodeSteps.ReEnterPin,
                                        stepTitle = stringRes(R.string.re_enter_your_pin)
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
                                    _errorMessage.emit(stringRes(R.string.password_does_not_match))
                                }
                            } else {
                                sharedPreferenceUtil.setPassword(newPin)
                                // Save selected PIN length here
                                sharedPreferenceUtil.setPinLength(state.value.pinLength)
                                viewModelScope.launch {
                                    val message = if (action == PinCodeAction.CreatePinCode.action) {
                                        stringRes(R.string.pincode_created)
                                    } else {
                                        stringRes(R.string.pincode_changed)

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
            PinCodeEvents.EnableSixDigitPin -> {
                _state.update {
                    it.copy(
                        pinLength = 6,
                        pin = "",
                        newPin = "",
                        reEnteredPin = ""
                    )
                }
            }
            PinCodeEvents.EnableFourDigitPin -> {
                _state.update {
                    it.copy(
                        pinLength = 4,
                        pin = "",
                        newPin = "",
                        reEnteredPin = ""
                    )
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
                            stepTitle = stringRes(R.string.enter_new_pin)
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
                            _errorMessage.emit(stringRes(R.string.old_new_pin_same))
                        }
                    } else {
                        _state.update {
                            it.copy(
                                    step = PinCodeSteps.ReEnterPin,
                                    stepTitle = stringRes(R.string.re_enter_your_pin)
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