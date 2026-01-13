package io.beldex.bchat.reactions;

import static com.beldex.libbchat.utilities.IdUtilKt.truncateIdForDisplay;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import java.util.List;

import io.beldex.bchat.R;
import io.beldex.bchat.components.FromTextView;
import io.beldex.bchat.components.ProfilePictureView;
import io.beldex.bchat.components.emoji.EmojiImageView;
import io.beldex.bchat.database.model.MessageId;
import com.bumptech.glide.RequestManager;

public class ReactionsViewAdapter extends RecyclerView.Adapter<ReactionsViewAdapter.ViewHolder> {

    private final List<ReactionDetails> reactions;
    private final Context context;

    private final Callback callback;

    private final RequestManager glideRequests;

    private final ReactionsViewModel reactionsViewModel;

    public ReactionsViewAdapter(ReactionsViewModel reactionsViewModel, Callback callback, Context context, List<ReactionDetails> reactions, RequestManager glideRequests) {
        this.glideRequests = glideRequests;
        this.context = context;
        this.reactions = reactions;
        this.callback = callback;
        this.reactionsViewModel = reactionsViewModel;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.reactions_bottom_sheet_dialog_fragment_recipient_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ReactionDetails reaction = reactions.get(position);
        holder.bind(this.reactionsViewModel, reaction, this.glideRequests);
    }

    @Override
    public int getItemCount() {
        return reactions.size();
    }

    public void updateData(List<ReactionDetails> newReactions, ReactionsViewModel viewModel) {
        reactions.addAll(newReactions);
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final ProfilePictureView avatar;
        private final FromTextView recipientName;
        private final FromTextView tapToRemove;
        private final EmojiImageView selectedEmoji;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.reactions_bottom_view_avatar);
            recipientName = itemView.findViewById(R.id.reactions_bottom_view_recipient_name);
            selectedEmoji = itemView.findViewById(R.id.reactions_bottom_view_selected_emoji);
            tapToRemove = itemView.findViewById(R.id.reactions_bottom_view_tab_to_remove);
        }

        public void bind(ReactionsViewModel reactionsViewModel, ReactionDetails reaction, RequestManager glideRequests) {
            reactionsViewModel.updateReactionCount(getItemCount());
            this.avatar.glide = glideRequests;
            this.avatar.update(reaction.getSender(), false, false);

            if (reaction.getSender().isLocalNumber()) {
                recipientName.setText(R.string.you);
                this.tapToRemove.setVisibility(View.VISIBLE);
                this.tapToRemove.setText(R.string.tap_to_remove);
            } else {
                String name = reaction.getSender().getName();
                if (name == null) {
                    name = truncateIdForDisplay(reaction.getSender().getAddress().serialize());
                }
                this.recipientName.setText(name);
                this.tapToRemove.setVisibility(View.GONE);
            }
            selectedEmoji.setImageEmoji(reaction.getDisplayEmoji());


            this.itemView.setOnClickListener((v) -> {
                if (reaction.getSender().isLocalNumber()) {
                    MessageId messageId = new MessageId(reaction.getLocalId(), reaction.isMms());
                    callback.onRemoveReaction(reaction.getBaseEmoji(), messageId, reaction.getTimestamp());
                    notifyDataSetChanged();
                }
            });
        }
    }

    public interface Callback {
        void onRemoveReaction(@NonNull String emoji, @NonNull MessageId messageId, long timestamp);

        void onClearAll(@NonNull String emoji, @NonNull MessageId messageId);
    }
}
