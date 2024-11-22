package io.beldex.bchat.reactions

import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.schedulers.Schedulers
import com.beldex.libbchat.messaging.MessagingModuleConfiguration
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.recipients.Recipient
import io.beldex.bchat.components.emoji.EmojiUtil
import io.beldex.bchat.database.model.MessageId
import io.beldex.bchat.database.model.ReactionRecord
import io.beldex.bchat.dependencies.DatabaseComponent

class ReactionsRepository {

    fun getReactions(messageId: MessageId): Observable<List<ReactionDetails>> {
        return Observable.create { emitter: ObservableEmitter<List<ReactionDetails>> ->
            emitter.onNext(fetchReactionDetails(messageId))
        }.subscribeOn(Schedulers.io())
    }

    private fun fetchReactionDetails(messageId: MessageId): List<ReactionDetails> {
        val context = MessagingModuleConfiguration.shared.context
        val reactions: List<ReactionRecord> = DatabaseComponent.get(context).reactionDatabase().getReactions(messageId)

        return reactions.map { reaction ->
            ReactionDetails(
                sender = Recipient.from(context, Address.fromSerialized(reaction.author), false),
                baseEmoji = EmojiUtil.getCanonicalRepresentation(reaction.emoji),
                displayEmoji = reaction.emoji,
                timestamp = reaction.dateReceived,
                serverId = reaction.serverId,
                localId = reaction.messageId,
                isMms = reaction.isMms,
                count = reaction.count.toInt()
            )
        }
    }
}
