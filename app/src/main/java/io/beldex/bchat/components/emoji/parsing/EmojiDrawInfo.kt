package io.beldex.bchat.components.emoji.parsing

import io.beldex.bchat.emoji.EmojiPage
data class EmojiDrawInfo(val page: EmojiPage, val index: Int, private val emoji: String, val rawEmoji: String?, val jumboSheet: String?)