package io.beldex.bchat.onboarding.ui

sealed interface PinCodeSteps {
    data object OldPin: PinCodeSteps
    data object EnterPin: PinCodeSteps
    data object ReEnterPin: PinCodeSteps
    data object VerifyPin: PinCodeSteps
}