package io.beldex.bchat.reactions;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import io.beldex.bchat.R;
import io.beldex.bchat.database.model.MessageId;
import io.beldex.bchat.util.LifecycleDisposable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public final class ReactionsDialogFragment extends BottomSheetDialogFragment implements ReactionsViewAdapter.Callback {

  private static final String ARGS_MESSAGE_ID = "reactions.args.message.id";
  private static final String ARGS_IS_MMS     = "reactions.args.is.mms";
  private static final String ARGS_IS_MODERATOR = "reactions.args.is.moderator";
  private static final String ARGS_EMOJI = "reactions.args.emoji";

  private Callback                  callback;

  private RecyclerView allEmojiReactionList;

  private ReactionsViewAdapter reactionRecipientsAdapter;

  private final List<EmojiCount> emojiCounts = new ArrayList<>();

  private final List<ReactionDetails> allEmojiReactions = new ArrayList<>();


  private final LifecycleDisposable disposables = new LifecycleDisposable();

  private TextView reactionsCount;

  private ImageView dismissFragment;

  public static DialogFragment create(MessageId messageId, @Nullable String emoji) {
    Bundle         args     = new Bundle();
    DialogFragment fragment = new ReactionsDialogFragment();

    args.putLong(ARGS_MESSAGE_ID, messageId.getId());
    args.putBoolean(ARGS_IS_MMS, messageId.isMms());
    //need to check
    /*args.putBoolean(ARGS_IS_MODERATOR, isUserModerator);*/
    args.putString(ARGS_EMOJI, emoji);

    fragment.setArguments(args);

    return fragment;
  }

  @Override
  public void onAttach(@NonNull Context context) {
    super.onAttach(context);

    if (getParentFragment() instanceof Callback) {
      callback = (Callback) getParentFragment();
    } else {
      callback = (Callback) context;
    }
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @Override
  public @Nullable View onCreateView(@NonNull LayoutInflater inflater,
                                     @Nullable ViewGroup container,
                                     @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.reactions_bottom_sheet_dialog_fragment, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    allEmojiReactionList = view.findViewById(R.id.reactions_bottom_view_recipient_recycler_all);
    reactionsCount = view.findViewById(R.id.reaction_count);
    dismissFragment = view.findViewById(R.id.dismissImage);
    dismissFragment.setOnClickListener(v -> dismiss());
    disposables.bindTo(getViewLifecycleOwner());
    MessageId messageId = new MessageId(requireArguments().getLong(ARGS_MESSAGE_ID), requireArguments().getBoolean(ARGS_IS_MMS));
    ReactionsViewModel.Factory factory = new ReactionsViewModel.Factory(messageId);
    ReactionsViewModel viewModel = new ViewModelProvider(this, factory).get(ReactionsViewModel.class);
    setUpRecipientsRecyclerView(viewModel);
    if (viewModel.getReactionCount() != null) {
      viewModel.getReactionCount().observe(getViewLifecycleOwner(), new Observer<Integer>() {
        @Override
        public void onChanged(Integer count) {
          reactionsCount.setText(String.valueOf(count));
        }
      });
    }
  }

  private void setUpRecipientsRecyclerView(ReactionsViewModel viewModel) {
    disposables.add(viewModel.getEmojiCounts().subscribe(emojiCounts -> {

      if (emojiCounts.size() < 1) {
        dismiss();
        return;
      }
      this.emojiCounts.clear();
      this.emojiCounts.addAll(emojiCounts);

      Set<ReactionDetails> allEntries = new HashSet<>();
      for (int i = 0; i < emojiCounts.size(); i++) {
        allEntries.addAll(emojiCounts.get(i).getReactions());
        reactionRecipientsAdapter.updateData(emojiCounts.get(i).getReactions(),viewModel);
      }
      this.allEmojiReactions.clear();
      this.allEmojiReactions.addAll(allEntries);
    }));
    reactionRecipientsAdapter = new ReactionsViewAdapter(viewModel,this,requireContext(),allEmojiReactions, Glide.with(this));
    allEmojiReactionList.setAdapter(reactionRecipientsAdapter);

  }

  @Override
  public void onRemoveReaction(@NonNull String emoji, @NonNull MessageId messageId, long timestamp) {
    callback.onRemoveReaction(emoji, messageId);
    dismiss();
  }

  @Override
  public void onClearAll(@NonNull String emoji, @NonNull MessageId messageId) {
    callback.onClearAll(emoji, messageId);
    dismiss();
  }

  public interface Callback {
    void onRemoveReaction(@NonNull String emoji, @NonNull MessageId messageId);

    void onClearAll(@NonNull String emoji, @NonNull MessageId messageId);
  }

}
