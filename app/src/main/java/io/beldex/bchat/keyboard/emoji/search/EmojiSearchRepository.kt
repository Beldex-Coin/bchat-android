package io.beldex.bchat.keyboard.emoji.search

import android.content.Context
import android.net.Uri
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import com.beldex.libbchat.utilities.concurrent.SignalExecutors
import io.beldex.bchat.components.emoji.Emoji
import io.beldex.bchat.components.emoji.EmojiPageModel
import io.beldex.bchat.database.EmojiSearchDatabase
import io.beldex.bchat.dependencies.DatabaseComponent
import io.beldex.bchat.emoji.EmojiSource
import java.util.function.Consumer

private const val MINIMUM_QUERY_THRESHOLD = 1
private const val MINIMUM_INLINE_QUERY_THRESHOLD = 2
private const val EMOJI_SEARCH_LIMIT = 20

private val NOT_PUNCTUATION = "[A-Za-z0-9 ]".toRegex()

class EmojiSearchRepository(context: Context) {

  private val emojiSearchDatabase: EmojiSearchDatabase = DatabaseComponent.get(context).emojiSearchDatabase()

  fun submitQuery(query: String, limit: Int = EMOJI_SEARCH_LIMIT): Single<List<String>> {
    val result = if (query.length >= MINIMUM_INLINE_QUERY_THRESHOLD && NOT_PUNCTUATION.matches(query.substring(query.lastIndex))) {
      Single.fromCallable { emojiSearchDatabase.query(query, limit) }
    } else {
      Single.just(emptyList())
    }

    return result.subscribeOn(Schedulers.io())
  }

  fun submitQuery(query: String, limit: Int = EMOJI_SEARCH_LIMIT, consumer: Consumer<EmojiPageModel>) {
    SignalExecutors.SERIAL.execute {
      val emoji: List<String> = emojiSearchDatabase.query(query, limit)

      val displayEmoji: List<Emoji> = emoji
        .mapNotNull { canonical -> EmojiSource.latest.canonicalToVariations[canonical] }
        .map { Emoji(it) }

      consumer.accept(EmojiSearchResultsPageModel(emoji, displayEmoji))
    }
  }

  private class EmojiSearchResultsPageModel(
    private val emoji: List<String>,
    private val displayEmoji: List<Emoji>
  ) : EmojiPageModel {
    override fun getKey(): String = ""

    override fun getIconAttr(): Int = -1

    override fun getEmoji(): List<String> = emoji

    override fun getDisplayEmoji(): List<Emoji> = displayEmoji

    override fun hasSpriteMap(): Boolean = false

    override fun getSpriteUri(): Uri? = null

    override fun isDynamic(): Boolean = false
  }
}
