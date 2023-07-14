package com.thoughtcrimes.securesms.components.emoji.parsing

import com.thoughtcrimes.securesms.emoji.EmojiPage


data class EmojiDrawInfo(val page: EmojiPage, val index: Int, private val emoji: String, val rawEmoji: String?, val jumboSheet: String?)
