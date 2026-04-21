package io.beldex.bchat.conversation.v2.utilities

import android.content.Context
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import androidx.core.content.res.ResourcesCompat
import io.beldex.bchat.R
import com.beldex.libbchat.messaging.contacts.Contact
import com.beldex.libbchat.utilities.TextSecurePreferences
import io.beldex.bchat.dependencies.DatabaseComponent
import io.beldex.bchat.util.UiModeUtilities

object MentionUtilities {

    @JvmStatic
    fun highlightMentions(text: CharSequence, threadID: Long, context: Context): String {
        return highlightMentions(text, false, threadID, context).toString() // isOutgoingMessage is irrelevant
    }

    @JvmStatic
    fun highlightMentionsSpannableString(text: CharSequence, threadID: Long, context: Context): SpannableStringBuilder {
        return highlightMentions(text, false, threadID, context) // isOutgoingMessage is irrelevant
    }

    @JvmStatic
    fun highlightMentions(
        text: CharSequence,
        isOutgoingMessage: Boolean,
        threadID: Long,
        context: Context
    ): SpannableStringBuilder {

        val builder = if (text is SpannableStringBuilder) {
            text
        } else {
            SpannableStringBuilder(text)
        }

        val regex = Regex("@([0-9a-fA-F]+)")
        val matches = regex.findAll(builder).toList().asReversed()

        val userPublicKey = TextSecurePreferences.getLocalNumber(context)!!
        val openGroup = DatabaseComponent.get(context).storage().getV2OpenGroup(threadID)
        val isLightMode = UiModeUtilities.isDayUiMode(context)

        for (match in matches) {

            val start = match.range.first
            val end = match.range.last + 1
            val publicKey = match.groupValues[1]

            val userDisplayName: String? = if (publicKey.equals(userPublicKey, true)) {
                TextSecurePreferences.getProfileName(context)
            } else {
                val contact = DatabaseComponent.get(context)
                    .bchatContactDatabase()
                    .getContactWithBchatID(publicKey)

                val ctx = if (openGroup != null)
                    Contact.ContactContext.OPEN_GROUP
                else
                    Contact.ContactContext.REGULAR

                contact?.displayName(ctx)
            }

            if (userDisplayName != null) {

                val displayText = "@$userDisplayName"

                builder.replace(start, end, displayText)

                val newEnd = start + displayText.length

                val colorID = if (isOutgoingMessage) {
                    if (isLightMode) R.color.white else R.color.black
                } else {
                    R.color.accent
                }

                val color = ResourcesCompat.getColor(context.resources, colorID, context.theme)

                builder.setSpan(
                    ForegroundColorSpan(color),
                    start,
                    newEnd,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                builder.setSpan(
                    StyleSpan(Typeface.BOLD),
                    start,
                    newEnd,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }

        return builder
    }
}