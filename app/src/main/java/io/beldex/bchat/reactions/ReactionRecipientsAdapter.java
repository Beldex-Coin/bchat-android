package io.beldex.bchat.reactions;

import static com.beldex.libbchat.utilities.IdUtilKt.truncateIdForDisplay;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.Collections;
import java.util.List;

import io.beldex.bchat.R;
import io.beldex.bchat.components.ProfilePictureView;
import io.beldex.bchat.components.emoji.EmojiImageView;
import io.beldex.bchat.database.model.MessageId;

final class ReactionRecipientsAdapter extends RecyclerView.Adapter<ReactionRecipientsAdapter.ViewHolder> {

  private static final int MAX_REACTORS = 5;
  private static final int HEADER_COUNT = 1;
  private static final int HEADER_POSITION = 0;

  private static final int FOOTER_COUNT = 1;
  private static final int FOOTER_POSITION = 6;

  private static final int HEADER_TYPE = 0;
  private static final int RECIPIENT_TYPE = 1;
  private static final int FOOTER_TYPE = 2;

  private ReactionViewPagerAdapter.Listener callback;
  private List<ReactionDetails> data = Collections.emptyList();
  private MessageId messageId;
  private boolean isUserModerator;
  private EmojiCount emojiData;

  public ReactionRecipientsAdapter(ReactionViewPagerAdapter.Listener callback) {
    this.callback = callback;
  }

  public void updateData(MessageId messageId, EmojiCount newData, boolean isUserModerator) {
    this.messageId = messageId;
    emojiData = newData;
    data = newData.getReactions();
    this.isUserModerator = isUserModerator;
    notifyDataSetChanged();
  }

  @Override
  public int getItemViewType(int position) {
    switch (position) {
      case HEADER_POSITION:
        return HEADER_TYPE;
      case FOOTER_POSITION:
        return FOOTER_TYPE;
      default:
        return RECIPIENT_TYPE;
    }
  }

  @Override
  public @NonNull
  ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    switch (viewType) {
      case HEADER_TYPE:
        return new HeaderViewHolder(callback, LayoutInflater.from(parent.getContext()).inflate(R.layout.reactions_bottom_sheet_dialog_fragment_recycler_header, parent, false));
      case FOOTER_TYPE:
        return new FooterViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.reactions_bottom_sheet_dialog_fragment_recycler_footer, parent, false));
      default:
        return new RecipientViewHolder(callback, LayoutInflater.from(parent.getContext()).inflate(R.layout.reactions_bottom_sheet_dialog_fragment_recipient_item, parent, false));
    }
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    if (holder instanceof RecipientViewHolder) {
      ((RecipientViewHolder) holder).bind(data.get(position - HEADER_COUNT));
    } else if (holder instanceof HeaderViewHolder) {
      ((HeaderViewHolder) holder).bind(emojiData, messageId, isUserModerator);
    } else if (holder instanceof FooterViewHolder) {
      ((FooterViewHolder) holder).bind(emojiData);
    }
  }

  @Override
  public void onViewRecycled(@NonNull ViewHolder holder) {
    if (holder instanceof RecipientViewHolder) {
      ((RecipientViewHolder) holder).unbind();
    }
  }

  @Override
  public int getItemCount() {
    if (data.isEmpty()) {
      return 0;
    } else if (emojiData.getCount() <= MAX_REACTORS) {
      return data.size() + HEADER_COUNT;
    } else {
      return MAX_REACTORS + HEADER_COUNT + FOOTER_COUNT;
    }
  }

  static class ViewHolder extends RecyclerView.ViewHolder {
    public ViewHolder(@NonNull View itemView) {
      super(itemView);
    }
  }

  static class HeaderViewHolder extends ViewHolder {

    private final ReactionViewPagerAdapter.Listener callback;

    public HeaderViewHolder(ReactionViewPagerAdapter.Listener callback, @NonNull View itemView) {
      super(itemView);
      this.callback = callback;
    }

    private void bind(@NonNull final EmojiCount emoji, final MessageId messageId, boolean isUserModerator) {
      View clearAll = itemView.findViewById(R.id.header_view_clear_all);
      clearAll.setVisibility(isUserModerator ? View.VISIBLE : View.GONE);
      clearAll.setOnClickListener(isUserModerator ? (View.OnClickListener) v -> {
        callback.onClearAll(emoji.getBaseEmoji(), messageId);
      } : null);
      EmojiImageView emojiView = itemView.findViewById(R.id.header_view_emoji);
      emojiView.setImageEmoji(emoji.getDisplayEmoji());
      TextView count = itemView.findViewById(R.id.header_view_emoji_count);
      count.setText(String.format(" •  %s", emoji.getCount()));
    }
  }

  static final class RecipientViewHolder extends ViewHolder {

    private ReactionViewPagerAdapter.Listener callback;
    private final ProfilePictureView avatar;
    private final TextView recipient;
    private final ImageView remove;

    public RecipientViewHolder(ReactionViewPagerAdapter.Listener callback, @NonNull View itemView) {
      super(itemView);
      this.callback = callback;
      avatar = itemView.findViewById(R.id.reactions_bottom_view_avatar);
      recipient = itemView.findViewById(R.id.reactions_bottom_view_recipient_name);
      remove = itemView.findViewById(R.id.reactions_bottom_view_recipient_remove);
    }

    void bind(@NonNull ReactionDetails reaction) {
      this.remove.setOnClickListener((v) -> {
        MessageId messageId = new MessageId(reaction.getLocalId(), reaction.isMms());
        callback.onRemoveReaction(reaction.getBaseEmoji(), messageId, reaction.getTimestamp());
      });

      this.avatar.update(reaction.getSender(), false,false);

      if (reaction.getSender().isLocalNumber()) {
        this.recipient.setText(R.string.you);
        this.remove.setVisibility(View.VISIBLE);
      } else {
        String name = reaction.getSender().getName();
        if (name == null){
          name = truncateIdForDisplay(reaction.getSender().getAddress().serialize());
        }
        this.recipient.setText(name);
        this.remove.setVisibility(View.GONE);
      }
    }

    void unbind() {
      avatar.recycle();
    }

  }

  static class FooterViewHolder extends ViewHolder {

    public FooterViewHolder(@NonNull View itemView) {
      super(itemView);
    }

    private void bind(@NonNull final EmojiCount emoji) {
      if (emoji.getCount() > 5) {
        TextView count = itemView.findViewById(R.id.footer_view_emoji_count);

        // We display the first 5 people to react w/ a given emoji so we'll subtract that to get the 'others' count
        int othersCount = emoji.getCount() - 5;
        String s = itemView.getResources().getQuantityString(R.plurals.emojiReactsCountOthers, othersCount, othersCount, emoji.getBaseEmoji());
        count.setText(s);

        itemView.setVisibility(View.VISIBLE);
      } else {
        itemView.setVisibility(View.GONE);
      }
    }
  }

}
