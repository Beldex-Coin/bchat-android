package io.beldex.bchat.keyboard.listeners

import io.beldex.bchat.keyboard.controllers.KeyboardController

interface KeyboardListener {
    fun characterClicked(c: Char)
    fun specialKeyClicked(key: KeyboardController.SpecialKey)
}