package com.thoughtcrimes.securesms.onboarding.ui

enum class CreateAccountStep {
    SetDisplayName,
    GenerateKeys,
    SetPinCode,
    CopySeed
}
sealed interface OnBoardingEvents {

    sealed interface CreateAccountEvents: OnBoardingEvents {
        data class DisplayNameChanged(val name: String): CreateAccountEvents
        data class AccountCreationStepChanged(val step: CreateAccountStep): CreateAccountEvents
        data object SeedCopied: CreateAccountEvents
    }

    sealed interface RestoreAccountEvents: OnBoardingEvents {

    }
}