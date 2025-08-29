package io.beldex.bchat.emoji

import android.net.Uri
import androidx.annotation.WorkerThread
import com.beldex.libbchat.messaging.MessagingModuleConfiguration
import io.beldex.bchat.ApplicationContext
import io.beldex.bchat.components.emoji.Emoji
import io.beldex.bchat.components.emoji.EmojiPageModel
import io.beldex.bchat.components.emoji.parsing.EmojiDrawInfo
import io.beldex.bchat.components.emoji.parsing.EmojiTree
import io.beldex.bchat.util.ScreenDensity
import java.io.InputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

/**
 * The entry point for the application to request Emoji data for custom emojis.
 */
class EmojiSource(
  val decodeScale: Float,
  private val emojiData: EmojiData,
  private val emojiPageFactory: EmojiPageFactory
) : EmojiData by emojiData {

  val variationsToCanonical: Map<String, String> by lazy {
    val map = mutableMapOf<String, String>()

    for (page: EmojiPageModel in dataPages) {
      for (emoji: Emoji in page.displayEmoji) {
        for (variation: String in emoji.variations) {
          map[variation] = emoji.value
        }
      }
    }

    map
  }

  val canonicalToVariations: Map<String, List<String>> by lazy {
    val map = mutableMapOf<String, List<String>>()

    for (page: EmojiPageModel in dataPages) {
      for (emoji: Emoji in page.displayEmoji) {
        map[emoji.value] = emoji.variations
      }
    }

    map
  }

  val maxEmojiLength: Int by lazy {
    dataPages.map { it.emoji.map(String::length) }
      .flatten()
      .maxOrZero()
  }

  val emojiTree: EmojiTree by lazy {
    val tree = EmojiTree()

    dataPages
      .filter { it.spriteUri != null }
      .forEach { page ->
        val emojiPage = emojiPageFactory(page.spriteUri!!)

        var overallIndex = 0
        page.displayEmoji.forEach { emoji: Emoji ->
          emoji.variations.forEachIndexed { variationIndex, variation ->
            val raw = emoji.getRawVariation(variationIndex)
            tree.add(variation, EmojiDrawInfo(emojiPage, overallIndex++, variation, raw, jumboPages[raw]))
          }
        }
      }

    obsolete.forEach {
      tree.add(it.obsolete, tree.getEmoji(it.replaceWith, 0, it.replaceWith.length))
    }

    tree
  }

  companion object {

    private val emojiSource = AtomicReference<EmojiSource>()
    private val emojiLatch = CountDownLatch(1)

    @JvmStatic
    val latest: EmojiSource
      get() {
        emojiLatch.await()
        return emojiSource.get()
      }

    @JvmStatic
    @WorkerThread
    fun refresh() {
      emojiSource.set(getEmojiSource())
      emojiLatch.countDown()
    }

    private fun getEmojiSource(): EmojiSource {
      return loadAssetBasedEmojis()
    }

    private fun loadAssetBasedEmojis(): EmojiSource {
      val context = MessagingModuleConfiguration.shared.context
      val emojiData: InputStream = ApplicationContext.getInstance(context).assets.open("emoji/emoji_data.json")

      emojiData.use {
        val parsedData: ParsedEmojiData= EmojiJsonParser.parse(it, ::getAssetsUri).getOrThrow()
        return EmojiSource(
          ScreenDensity.xhdpiRelativeDensityScaleFactor("xhdpi"),

          parsedData.copy(
            displayPages = parsedData.displayPages,
            dataPages = parsedData.dataPages
          )

        ) { uri: Uri -> EmojiPage.Asset(uri) }
      }
    }
  }
}

private fun List<Int>.maxOrZero(): Int = maxOrNull() ?: 0

interface EmojiData {
  val metrics: EmojiMetrics
  val densities: List<String>
  val format: String
  val displayPages: List<EmojiPageModel>
  val dataPages: List<EmojiPageModel>
  val jumboPages: Map<String, String>
  val obsolete: List<ObsoleteEmoji>
}

data class ObsoleteEmoji(val obsolete: String, val replaceWith: String)

data class EmojiMetrics(val rawHeight: Int, val rawWidth: Int, val perRow: Int)

private fun getAssetsUri(name: String, format: String): Uri = Uri.parse("file:///android_asset/emoji/$name.$format")
