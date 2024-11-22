package io.beldex.bchat.reactions;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import com.beldex.libbchat.utilities.ThemeUtil;
import com.beldex.libsignal.utilities.Log;

import io.beldex.bchat.R;
import io.beldex.bchat.components.emoji.EmojiImageView;
import io.beldex.bchat.database.model.MessageId;
import io.beldex.bchat.reactions.ReactionViewPagerAdapter;
import io.beldex.bchat.util.LifecycleDisposable;
import io.beldex.bchat.util.NumberUtil;

import java.util.Objects;



public final class ReactionsDialogFragment extends BottomSheetDialogFragment implements ReactionViewPagerAdapter.Listener {

  private static final String ARGS_MESSAGE_ID = "reactions.args.message.id";
  private static final String ARGS_IS_MMS     = "reactions.args.is.mms";
  private static final String ARGS_IS_MODERATOR = "reactions.args.is.moderator";
  private static final String ARGS_EMOJI = "reactions.args.emoji";

  private ViewPager2                recipientPagerView;
  private ReactionViewPagerAdapter  recipientsAdapter;
  private Callback                  callback;

  private final LifecycleDisposable disposables = new LifecycleDisposable();

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
    recipientPagerView = view.findViewById(R.id.reactions_bottom_view_recipient_pager);

    disposables.bindTo(getViewLifecycleOwner());

    setUpRecipientsRecyclerView();
    setUpTabMediator(savedInstanceState);

    MessageId messageId = new MessageId(requireArguments().getLong(ARGS_MESSAGE_ID), requireArguments().getBoolean(ARGS_IS_MMS));
    recipientsAdapter.setIsUserModerator(requireArguments().getBoolean(ARGS_IS_MODERATOR));
    recipientsAdapter.setMessageId(messageId);
    setUpViewModel(messageId);
  }

  private void setUpTabMediator(@Nullable Bundle savedInstanceState) {
    if (savedInstanceState == null) {
      FrameLayout    container       = requireDialog().findViewById(R.id.container);
      TabLayout      emojiTabs       = requireDialog().findViewById(R.id.emoji_tabs);

      ViewCompat.setOnApplyWindowInsetsListener(container, (v, insets) -> insets.consumeSystemWindowInsets());

      TabLayoutMediator mediator = new TabLayoutMediator(
              emojiTabs, recipientPagerView, true, false,
              (tab, position) -> {
        tab.setCustomView(R.layout.reactions_pill_large);

        View           customView = Objects.requireNonNull(tab.getCustomView());
        EmojiImageView emoji      = customView.findViewById(R.id.reactions_pill_emoji);
        TextView       text       = customView.findViewById(R.id.reactions_pill_count);
        EmojiCount     emojiCount = recipientsAdapter.getEmojiCount(position);

        customView.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.reaction_pill_dialog_background));
        emoji.setImageEmoji(emojiCount.getDisplayEmoji());
        text.setText(NumberUtil.getFormattedNumber(emojiCount.getCount()));
      });

      emojiTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
        @Override
        public void onTabSelected(TabLayout.Tab tab) {
          View customView = tab.getCustomView();
          customView.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.reaction_pill_background_selected));
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {
          View customView = tab.getCustomView();
          customView.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.reaction_pill_dialog_background));
        }
        @Override
        public void onTabReselected(TabLayout.Tab tab) {}
      });
      mediator.attach();
    }
  }

  private void setUpRecipientsRecyclerView() {
    recipientsAdapter = new ReactionViewPagerAdapter(this);
    recipientPagerView.setAdapter(recipientsAdapter);
  }

  private void setUpViewModel(@NonNull MessageId messageId) {
    ReactionsViewModel.Factory factory = new ReactionsViewModel.Factory(messageId);

    ReactionsViewModel viewModel = new ViewModelProvider(this, factory).get(ReactionsViewModel.class);

    disposables.add(viewModel.getEmojiCounts().subscribe(emojiCounts -> {
      if (emojiCounts.size() < 1) {
        dismiss();
        return;
      }

      recipientsAdapter.submitList(emojiCounts);

      // select the tab based on which emoji the user long pressed on
      TabLayout emojiTabs = requireDialog().findViewById(R.id.emoji_tabs);
      String emoji = requireArguments().getString(ARGS_EMOJI);
      int tabIndex = 0;
      for (int i = 0; i < emojiCounts.size(); i++) {
        if(emojiCounts.get(i).getBaseEmoji().equals(emoji)){
          tabIndex = i;
          break;
        }
      }
      emojiTabs.selectTab(emojiTabs.getTabAt(tabIndex));
    }));
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
