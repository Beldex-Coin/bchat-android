package io.beldex.bchat.reactions.any;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.loader.app.LoaderManager;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.shape.CornerFamily;
import com.google.android.material.shape.ShapeAppearanceModel;

import io.beldex.bchat.components.emoji.EmojiEventListener;
import io.beldex.bchat.components.emoji.EmojiPageView;
import io.beldex.bchat.components.emoji.EmojiPageViewGridAdapter;
import io.beldex.bchat.conversation.v2.ViewUtil;
import io.beldex.bchat.database.model.MessageId;
import io.beldex.bchat.database.model.MessageRecord;
import io.beldex.bchat.keyboard.emoji.KeyboardPageSearchView;
import io.beldex.bchat.util.LifecycleDisposable;

import io.beldex.bchat.R;

public final class ReactWithAnyEmojiDialogFragment extends BottomSheetDialogFragment implements EmojiEventListener,
        EmojiPageViewGridAdapter.VariationSelectorListener,KeyboardPageSearchView.Callbacks
{

  private static final String ARG_MESSAGE_ID = "arg_message_id";
  private static final String ARG_IS_MMS     = "arg_is_mms";
  private static final String ARG_START_PAGE = "arg_start_page";
  private static final String ARG_SHADOWS    = "arg_shadows";

  private ReactWithAnyEmojiViewModel viewModel;
  private Callback                   callback;
  private EmojiPageView              emojiPageView;

  private EditText searchEdit;

  private ImageView backToEmoji;

  private ImageView clearSearch;

  private final LifecycleDisposable disposables = new LifecycleDisposable();

  public static DialogFragment createForMessageRecord(@NonNull MessageRecord messageRecord, int startingPage) {
    DialogFragment fragment = new ReactWithAnyEmojiDialogFragment();
    Bundle         args     = new Bundle();

    args.putLong(ARG_MESSAGE_ID, messageRecord.getId());
    args.putBoolean(ARG_IS_MMS, messageRecord.isMms());
    args.putInt(ARG_START_PAGE, startingPage);
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
  public @NonNull Dialog onCreateDialog(Bundle savedInstanceState) {
    BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
    dialog.getBehavior().setPeekHeight((int) (getResources().getDisplayMetrics().heightPixels * 0.50));

    ShapeAppearanceModel shapeAppearanceModel = ShapeAppearanceModel.builder()
                                                                    .setTopLeftCorner(CornerFamily.ROUNDED, ViewUtil.dpToPx(requireContext(), 18))
                                                                    .setTopRightCorner(CornerFamily.ROUNDED, ViewUtil.dpToPx(requireContext(), 18))
                                                                    .build();

    boolean shadows = requireArguments().getBoolean(ARG_SHADOWS, true);
    if (!shadows) {
      Window window = dialog.getWindow();
      if (window != null) {
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
      }
    }

    return dialog;
  }

  @Override
  public @Nullable View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.react_with_any_emoji_dialog_fragment, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    disposables.bindTo(getViewLifecycleOwner());

    emojiPageView = view.findViewById(R.id.react_with_any_emoji_page_view);
    emojiPageView.initialize(this, this, true);

    searchEdit = view.findViewById(R.id.searchEditText);
    backToEmoji = view.findViewById(R.id.back_to_emoji_icon);
    clearSearch = view.findViewById(R.id.clear_search_icon);

    clearSearch.setOnClickListener(v -> clearQuery());

    searchEdit.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {
      }
      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
      }
      @Override
      public void afterTextChanged(Editable s) {
        if (s.toString().isEmpty()) {
          clearSearch.setImageDrawable(null);
          clearSearch.setClickable(false);
          backToEmoji.setImageResource(R.drawable.ic_search_24);
        } else {
          clearSearch.setImageResource(R.drawable.ic_close);
          clearSearch.setClickable(true);
        }
        if (s.toString().isEmpty()) {
          viewModel.onQueryChanged("");
        } else {
          boolean hasQuery = !TextUtils.isEmpty(s.toString());
          enableBackNavigation(hasQuery);
          viewModel.onQueryChanged(s.toString());
        }
      }
    });

    initializeViewModel();

//    EmojiKeyboardPageCategoriesAdapter categoriesAdapter = new EmojiKeyboardPageCategoriesAdapter(key -> {
//      scrollTo(key);
//      viewModel.selectPage(key);
//    });

    disposables.add(viewModel.getEmojiList().subscribe(pages -> emojiPageView.setList(pages, null)));
//    disposables.add(viewModel.getCategories().subscribe(categoriesAdapter::submitList));
//    disposables.add(viewModel.getSelectedKey().subscribe(key -> categoriesRecycler.post(() -> {
//      int index = categoriesAdapter.indexOfFirst(EmojiKeyboardPageCategoryMappingModel.class, m -> m.getKey().equals(key));
//
//      if (index != -1) {
//        categoriesRecycler.smoothScrollToPosition(index);
//      }
//    })));
  }


  public void enableBackNavigation(Boolean enable) {
    backToEmoji.setImageResource(enable ? R.drawable.ic_arrow_left : R.drawable.ic_search_24);
    if (enable) {
      backToEmoji.setImageResource(R.drawable.ic_arrow_left);
      backToEmoji.setOnClickListener(v -> backToFragment());
    } else {
      backToEmoji.setImageResource(R.drawable.ic_search_24);
      backToEmoji.setOnClickListener(null);
    }
  }

  public void clearQuery() {
    searchEdit.getText().clear();
  }

  public void backToFragment(){
    clearQuery();
    searchEdit.clearFocus();
    ViewUtil.hideKeyboard(requireContext(), requireView());
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    LoaderManager.getInstance(requireActivity()).destroyLoader((int) requireArguments().getLong(ARG_MESSAGE_ID));
  }

  @Override
  public void onDismiss(@NonNull DialogInterface dialog) {
    super.onDismiss(dialog);

    callback.onReactWithAnyEmojiDialogDismissed();
  }

  private void initializeViewModel() {
    Bundle                             args       = requireArguments();
    ReactWithAnyEmojiRepository        repository = new ReactWithAnyEmojiRepository(requireContext());
    ReactWithAnyEmojiViewModel.Factory factory    = new ReactWithAnyEmojiViewModel.Factory(repository, args.getLong(ARG_MESSAGE_ID), args.getBoolean(ARG_IS_MMS));

    viewModel = new ViewModelProvider(this, factory).get(ReactWithAnyEmojiViewModel.class);
  }

  @Override
  public void onEmojiSelected(String emoji) {
    viewModel.onEmojiSelected(emoji);
    Bundle    args      = requireArguments();
    MessageId messageId = new MessageId(args.getLong(ARG_MESSAGE_ID), args.getBoolean(ARG_IS_MMS));
    callback.onReactWithAnyEmojiSelected(emoji, messageId);
    dismiss();
  }

  @Override
  public void onKeyEvent(KeyEvent keyEvent) {
  }

  @Override
  public void onVariationSelectorStateChanged(boolean open) { }

  public interface Callback {
    void onReactWithAnyEmojiDialogDismissed();

    void onReactWithAnyEmojiSelected(@NonNull String emoji, MessageId messageId);
  }

  private class SearchCallbacks implements KeyboardPageSearchView.Callbacks {
    @Override
    public void onQueryChanged(@NonNull String query) {
      viewModel.onQueryChanged(query);
    }

    @Override
    public void onNavigationClicked() {
      clearQuery();
      searchEdit.clearFocus();
      ViewUtil.hideKeyboard(requireContext(), requireView());
      dismiss();
    }

    @Override
    public void onFocusGained() {
      ((BottomSheetDialog) requireDialog()).getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    @Override
    public void onClicked() { }

    @Override
    public void onFocusLost() { }
  }
}
