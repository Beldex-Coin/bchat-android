package io.beldex.bchat.reactions;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.annimon.stream.Stream;

import io.beldex.bchat.database.model.MessageId;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class ReactionsViewModel extends ViewModel {

  private final MessageId           messageId;
  private final ReactionsRepository repository;

  public ReactionsViewModel(@NonNull MessageId messageId) {
    this.messageId  = messageId;
    this.repository = new ReactionsRepository();
  }

  public @NonNull
  Observable<List<EmojiCount>> getEmojiCounts() {
    return repository.getReactions(messageId)
                     .map(reactionList -> Stream.of(reactionList)
                                                          .groupBy(ReactionDetails::getBaseEmoji)
                                                          .sorted(this::compareReactions)
                                                          .map(entry -> new EmojiCount(entry.getKey(),
                                                                                       getCountDisplayEmoji(entry.getValue()),
                                                                                       entry.getValue()))
                                                          .toList())
                     .observeOn(AndroidSchedulers.mainThread());
  }

  private int compareReactions(@NonNull Map.Entry<String, List<ReactionDetails>> lhs, @NonNull Map.Entry<String, List<ReactionDetails>> rhs) {
    int lengthComparison = -Integer.compare(lhs.getValue().size(), rhs.getValue().size());
    if (lengthComparison != 0) return lengthComparison;

    long latestTimestampLhs = getLatestTimestamp(lhs.getValue());
    long latestTimestampRhs = getLatestTimestamp(rhs.getValue());

    return -Long.compare(latestTimestampLhs, latestTimestampRhs);
  }

  private long getLatestTimestamp(List<ReactionDetails> reactions) {
    return Stream.of(reactions)
                 .max(Comparator.comparingLong(ReactionDetails::getTimestamp))
                 .map(ReactionDetails::getTimestamp)
                 .orElse(-1L);
  }

  private @NonNull String getCountDisplayEmoji(@NonNull List<ReactionDetails> reactions) {
    for (ReactionDetails reaction : reactions) {
      if (reaction.getSender().isLocalNumber()) {
        return reaction.getDisplayEmoji();
      }
    }

    return reactions.get(reactions.size() - 1).getDisplayEmoji();
  }

  static final class Factory implements ViewModelProvider.Factory {

    private final MessageId messageId;

    Factory(@NonNull MessageId messageId) {
      this.messageId = messageId;
    }

    @Override
    public @NonNull <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
      return modelClass.cast(new ReactionsViewModel(messageId));
    }
  }
}
