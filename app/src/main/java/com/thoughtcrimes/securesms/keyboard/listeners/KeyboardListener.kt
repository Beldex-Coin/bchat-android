package com.thoughtcrimes.securesms.keyboard.listeners

import com.thoughtcrimes.securesms.keyboard.controllers.KeyboardController

interface KeyboardListener {
    fun characterClicked(c: Char)
    fun specialKeyClicked(key: KeyboardController.SpecialKey)
}