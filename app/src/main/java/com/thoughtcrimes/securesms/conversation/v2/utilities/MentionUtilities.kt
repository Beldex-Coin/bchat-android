package com.thoughtcrimes.securesms.conversation.v2.utilities

import android.content.Context
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Range
import androidx.core.content.res.ResourcesCompat
import io.beldex.bchat.R
import nl.komponents.kovenant.combine.Tuple2
import com.beldex.libbchat.messaging.contacts.Contact
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.thoughtcrimes.securesms.dependencies.DatabaseComponent
import com.thoughtcrimes.securesms.util.UiModeUtilities
import java.util.regex.Pattern

object MentionUtilities {

    @JvmStatic
    fun highlightMentions(text: CharSequence, threadID: Long, context: Context): String {
        return highlightMentions(text, false, threadID, context).toString() // isOutgoingMessage is irrelevant
    }

    @JvmStatic
    fun highlightMentions(text: CharSequence, isOutgoingMessage: Boolean, threadID: Long, context: Context): SpannableString {
        @Suppress("NAME_SHADOWING") var text = text
        val pattern = Pattern.compile("@[0-9a-fA-F]*")
        var matcher = pattern.matcher(text)
        val mentions = mutableListOf<Tuple2<Range<Int>, String>>()
        var startIndex = 0
        val userPublicKey = TextSecurePreferences.getLocalNumber(context)!!
        val openGroup = DatabaseComponent.get(context).storage().getV2OpenGroup(threadID)
        if (matcher.find(startIndex)) {
            while (true) {
                val publicKey = text.subSequence(matcher.start() + 1, matcher.end()).toString() // +1 to get rid of the @
                val userDisplayName: String? = if (publicKey.equals(userPublicKey, ignoreCase = true)) {
                    TextSecurePreferences.getProfileName(context)
                } else {
                    val contact = DatabaseComponent.get(context).bchatContactDatabase().getContactWithBchatID(publicKey)
                    @Suppress("NAME_SHADOWING") val context = if (openGroup!=null) Contact.ContactContext.OPEN_GROUP else Contact.ContactContext.REGULAR
                    contact?.displayName(context)
                }
                if (userDisplayName != null) {
                    text = text.subSequence(0, matcher.start()).toString() + "@" + userDisplayName + text.subSequence(matcher.end(), text.length)
                    val endIndex = matcher.start() + 1 + userDisplayName.length
                    startIndex = endIndex
                    mentions.add(Tuple2(Range.create(matcher.start(), endIndex), publicKey))
                } else {
                    startIndex = matcher.end()
                }
                matcher = pattern.matcher(text)
                if (!matcher.find(startIndex)) { break }
            }
        }
        val result = SpannableString(text)
        val isLightMode = UiModeUtilities.isDayUiMode(context)
        for (mention in mentions) {
            val colorID = if (isOutgoingMessage) {
                if (isLightMode) R.color.white else R.color.black
            } else {
                R.color.accent
            }
            val color = ResourcesCompat.getColor(context.resources, colorID, context.theme)
            result.setSpan(ForegroundColorSpan(color), mention.first.lower, mention.first.upper, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            result.setSpan(StyleSpan(Typeface.BOLD), mention.first.lower, mention.first.upper, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return result
    }
}