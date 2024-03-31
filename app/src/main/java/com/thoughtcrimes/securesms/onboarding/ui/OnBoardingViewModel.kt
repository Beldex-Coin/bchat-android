package com.thoughtcrimes.securesms.onboarding.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class OnBoardingViewModel @Inject constructor(): ViewModel() {

    data class State(
        val displayName: String? = null,
        val bChatId: String? = null,
        val beldexAddress: String? = null,
        val generatingKeys: Boolean = false,
        val pinCode: String? = null,
        val reenteredPinCode: String? = null,
        val seedCopied: Boolean = false,
        val currentStep: CreateAccountStep = CreateAccountStep.SetDisplayName
    )

    private val _uiState = MutableStateFlow(State())
    val uiState = _uiState.asStateFlow()


    fun onEvent(event: OnBoardingEvents) {
        when(event) {
            is OnBoardingEvents.CreateAccountEvents.AccountCreationStepChanged -> {
                _uiState.update {
                    it.copy(
                        currentStep = event.step
                    )
                }
            }
            is OnBoardingEvents.CreateAccountEvents.DisplayNameChanged -> {
                _uiState.update {
                    it.copy(
                        displayName = event.name
                    )
                }
            }
            OnBoardingEvents.CreateAccountEvents.SeedCopied -> {
                _uiState.update {
                    it.copy(
                        seedCopied = true
                    )
                }
            }
        }
    }

}