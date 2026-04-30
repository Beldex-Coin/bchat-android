package io.beldex.bchat.mediasend;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.LeadingMarginSpan;
import android.text.style.StyleSpan;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;
import com.beldex.libbchat.utilities.MediaTypes;
import com.beldex.libbchat.utilities.Stub;
import com.beldex.libbchat.utilities.TextSecurePreferences;
import com.beldex.libbchat.utilities.Util;
import com.beldex.libbchat.utilities.recipients.Recipient;
import com.beldex.libsignal.utilities.ListenableFuture;
import com.beldex.libsignal.utilities.Log;
import com.beldex.libsignal.utilities.SettableFuture;
import com.beldex.libsignal.utilities.guava.Optional;
import com.bumptech.glide.Glide;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import io.beldex.bchat.R;
import io.beldex.bchat.components.ComposeText;
import io.beldex.bchat.components.ControllableViewPager;
import io.beldex.bchat.components.InputAwareLayout;
import io.beldex.bchat.components.emoji.EmojiEditText;
import io.beldex.bchat.components.emoji.EmojiEventListener;
import io.beldex.bchat.components.emoji.EmojiKeyboardProvider;
import io.beldex.bchat.components.emoji.EmojiToggle;
import io.beldex.bchat.components.emoji.MediaKeyboard;
import io.beldex.bchat.contactshare.SimpleTextWatcher;
import io.beldex.bchat.imageeditor.model.EditorModel;
import io.beldex.bchat.mediapreview.MediaRailAdapter;
import io.beldex.bchat.providers.BlobProvider;
import io.beldex.bchat.scribbles.ImageEditorFragment;
import io.beldex.bchat.textformatter.CustomQuoteSpan;
import io.beldex.bchat.textformatter.QuoteIndentSpan;
import io.beldex.bchat.textformatter.TextFormatter;
import io.beldex.bchat.util.CharacterCalculator;
import io.beldex.bchat.util.PushCharacterCalculator;
import io.beldex.bchat.util.Stopwatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Allows the user to edit and caption a set of media items before choosing to send them.
 */
public class MediaSendFragment extends Fragment implements ViewTreeObserver.OnGlobalLayoutListener,
                                                           MediaRailAdapter.RailItemListener,
                                                           InputAwareLayout.OnKeyboardShownListener,
                                                           InputAwareLayout.OnKeyboardHiddenListener
{

  private static final String TAG = MediaSendFragment.class.getSimpleName();

  private static final String KEY_ADDRESS   = "address";

  private InputAwareLayout  hud;
  private View              captionAndRail;
  private ImageButton       sendButton;
  private ComposeText composeText;
  private ViewGroup         composeContainer;
  private EmojiEditText     captionText;
  private EmojiToggle       emojiToggle;
  private Stub<MediaKeyboard> emojiDrawer;
  private ViewGroup         playbackControlsContainer;
  private TextView          charactersLeft;
  private View              closeButton;

  private ControllableViewPager         fragmentPager;
  private MediaSendFragmentPagerAdapter fragmentPagerAdapter;
  private RecyclerView                  mediaRail;
  private MediaRailAdapter              mediaRailAdapter;

  private int                visibleHeight;
  private MediaSendViewModel viewModel;
  private Controller         controller;

  private final Rect visibleBounds = new Rect();

  private final PushCharacterCalculator characterCalculator = new PushCharacterCalculator();

  private final ComposeKeyPressedListener composeKeyPressedListener = new ComposeKeyPressedListener();

  private boolean isFormattingCompose = false;
  private final char bulletChar = '\u2022';
  private char lastBulletTriggerChar = '*';
  private final Runnable formatComposeRunnable = () -> applyFormattingToComposeText();

  public static MediaSendFragment newInstance(@NonNull Recipient recipient) {
    Bundle args = new Bundle();
    args.putParcelable(KEY_ADDRESS, recipient.getAddress());

    MediaSendFragment fragment = new MediaSendFragment();
    fragment.setArguments(args);
    return fragment;
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);

    if (!(requireActivity() instanceof Controller)) {
      throw new IllegalStateException("Parent activity must implement controller interface.");
    }

    controller = (Controller) requireActivity();
  }

  @Override
  public @Nullable View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.mediasend_fragment, container, false);
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    initViewModel();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    hud                       = view.findViewById(R.id.mediasend_hud);
    captionAndRail            = view.findViewById(R.id.mediasend_caption_and_rail);
    sendButton                = view.findViewById(R.id.mediasend_send_button);
    composeText               = view.findViewById(R.id.mediasend_compose_text);
    composeContainer          = view.findViewById(R.id.mediasend_compose_container);
    captionText               = view.findViewById(R.id.mediasend_caption);
    emojiToggle               = view.findViewById(R.id.mediasend_emoji_toggle);
    emojiDrawer               = new Stub<>(view.findViewById(R.id.mediasend_emoji_drawer_stub));
    fragmentPager             = view.findViewById(R.id.mediasend_pager);
    mediaRail                 = view.findViewById(R.id.mediasend_media_rail);
    playbackControlsContainer = view.findViewById(R.id.mediasend_playback_controls_container);
    charactersLeft            = view.findViewById(R.id.mediasend_characters_left);
    closeButton               = view.findViewById(R.id.mediasend_close_button);

    View sendButtonBkg = view.findViewById(R.id.mediasend_send_button_bkg);

    sendButton.setOnClickListener(v -> {
      if (hud.isKeyboardOpen()) {
        hud.hideSoftkey(composeText, null);
      }

      processMedia(fragmentPagerAdapter.getAllMedia(), fragmentPagerAdapter.getSavedState());
    });

    composeText.setOnKeyListener(composeKeyPressedListener);
    composeText.addTextChangedListener(composeKeyPressedListener);
    composeText.setOnClickListener(composeKeyPressedListener);
    composeText.setOnFocusChangeListener(composeKeyPressedListener);

    captionText.clearFocus();
    composeText.requestFocus();

    fragmentPagerAdapter = new MediaSendFragmentPagerAdapter(getChildFragmentManager());
    fragmentPager.setAdapter(fragmentPagerAdapter);

    FragmentPageChangeListener pageChangeListener = new FragmentPageChangeListener();
    fragmentPager.addOnPageChangeListener(pageChangeListener);
    fragmentPager.post(() -> pageChangeListener.onPageSelected(fragmentPager.getCurrentItem()));

    mediaRailAdapter = new MediaRailAdapter(Glide.with(this), this, true);
    mediaRail.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
    mediaRail.setAdapter(mediaRailAdapter);

    hud.addOnKeyboardShownListener(this);
    hud.addOnKeyboardHiddenListener(this);

    captionText.addTextChangedListener(new SimpleTextWatcher() {
      @Override
      public void onTextChanged(String text) {
        viewModel.onCaptionChanged(text);
      }
    });

    composeText.append(viewModel.getBody());

    Recipient recipient   = Recipient.from(requireContext(), getArguments().getParcelable(KEY_ADDRESS), false);
    String    displayName = Optional.fromNullable(recipient.getName())
                                    .or(Optional.fromNullable(recipient.getProfileName())
                                                .or(recipient.getAddress().serialize()));
    composeText.setHint(getString(R.string.MediaSendActivity_message_to_s, displayName));
    composeText.setOnEditorActionListener((v, actionId, event) -> {
      boolean isSend = actionId == EditorInfo.IME_ACTION_SEND;
      if (isSend) sendButton.performClick();
      return isSend;
    });

    if (TextSecurePreferences.isSystemEmojiPreferred(getContext())) {
      emojiToggle.setVisibility(View.GONE);
    } else {
      emojiToggle.setOnClickListener(this::onEmojiToggleClicked);
    }

    closeButton.setOnClickListener(v -> requireActivity().onBackPressed());
  }

  @Override
  public void onResume() {
    super.onResume();
    composeText.setFocusableInTouchMode(true);
  }

  @Override
  public void onPause() {
    super.onPause();
    composeText.setFocusable(false);
  }

  @Override
  public void onStart() {
    super.onStart();

    fragmentPagerAdapter.restoreState(viewModel.getDrawState());
    viewModel.onImageEditorStarted();
    requireActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
  }

  @Override
  public void onHiddenChanged(boolean hidden) {
    super.onHiddenChanged(hidden);
  }

  @Override
  public void onStop() {
    super.onStop();
    fragmentPagerAdapter.saveAllState();
    viewModel.saveDrawState(fragmentPagerAdapter.getSavedState());
  }

  @Override
  public void onGlobalLayout() {
    hud.getRootView().getWindowVisibleDisplayFrame(visibleBounds);

    int currentVisibleHeight = visibleBounds.height();

    if (currentVisibleHeight != visibleHeight) {
      hud.getLayoutParams().height = currentVisibleHeight;
      hud.layout(visibleBounds.left, visibleBounds.top, visibleBounds.right, visibleBounds.bottom);
      hud.requestLayout();

      visibleHeight = currentVisibleHeight;
    }
  }

  @Override
  public void onRailItemClicked(int distanceFromActive) {
    viewModel.onPageChanged(fragmentPager.getCurrentItem() + distanceFromActive);
  }

  @Override
  public void onRailItemDeleteClicked(int distanceFromActive) {
    viewModel.onMediaItemRemoved(requireContext(), fragmentPager.getCurrentItem() + distanceFromActive);
  }

  @Override
  public void onKeyboardShown() {
    if (captionText.hasFocus()) {
      mediaRail.setVisibility(View.VISIBLE);
      composeContainer.setVisibility(View.GONE);
      captionText.setVisibility(View.VISIBLE);
    } else if (composeText.hasFocus()) {
      mediaRail.setVisibility(View.VISIBLE);
      composeContainer.setVisibility(View.VISIBLE);
      captionText.setVisibility(View.GONE);
    } else {
      mediaRail.setVisibility(View.GONE);
      composeContainer.setVisibility(View.VISIBLE);
      captionText.setVisibility(View.GONE);
    }
  }

  @Override
  public void onKeyboardHidden() {
    composeContainer.setVisibility(View.VISIBLE);
    mediaRail.setVisibility(View.VISIBLE);

    if (!Util.isEmpty(viewModel.getSelectedMedia().getValue()) && viewModel.getSelectedMedia().getValue().size() > 1) {
      captionText.setVisibility(View.VISIBLE);
    }
  }

  public void onTouchEventsNeeded(boolean needed) {
    if (fragmentPager != null) {
      fragmentPager.setEnabled(!needed);
    }
  }

  public boolean handleBackPress() {
    if (hud.isInputOpen()) {
      hud.hideCurrentInput(composeText);
      return true;
    }
    return false;
  }

  private void initViewModel() {
    viewModel = new ViewModelProvider(requireActivity(), new MediaSendViewModel.Factory(requireActivity().getApplication(), new MediaRepository())).get(MediaSendViewModel.class);

    viewModel.getSelectedMedia().observe(this, media -> {
      if (Util.isEmpty(media)) {
        controller.onNoMediaAvailable();
        return;
      }

      fragmentPagerAdapter.setMedia(media);

      mediaRail.setVisibility(View.VISIBLE);
      captionText.setVisibility((media.size() > 1 || media.get(0).getCaption().isPresent()) ? View.VISIBLE : View.GONE);
      mediaRailAdapter.setMedia(media);
    });

    viewModel.getPosition().observe(this, position -> {
      if (position == null || position < 0) return;

      fragmentPager.setCurrentItem(position, true);
      mediaRailAdapter.setActivePosition(position);
      mediaRail.smoothScrollToPosition(position);

      if (fragmentPagerAdapter.getAllMedia().size() > position) {
        captionText.setText(fragmentPagerAdapter.getAllMedia().get(position).getCaption().or(""));
      }

      View playbackControls = fragmentPagerAdapter.getPlaybackControls(position);

      if (playbackControls != null) {
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        playbackControls.setLayoutParams(params);
        playbackControlsContainer.removeAllViews();
        playbackControlsContainer.addView(playbackControls);
      } else {
        playbackControlsContainer.removeAllViews();
      }
    });

    viewModel.getBucketId().observe(this, bucketId -> {
      if (bucketId == null) return;

      mediaRailAdapter.setAddButtonListener(() -> controller.onAddMediaClicked(bucketId));
    });
  }

  private EmojiEditText getActiveInputField() {
    if (captionText.hasFocus()) return captionText;
    else                        return composeText;
  }


  private void presentCharactersRemaining() {
    String          messageBody     = composeText.getTextTrimmed();
    CharacterCalculator.CharacterState characterState  = characterCalculator.calculateCharacters(messageBody);

    if (characterState.charactersRemaining <= 15 || characterState.messagesSpent > 1) {
      charactersLeft.setText(String.format(Locale.getDefault(),
                                           "%d/%d (%d)",
                                           characterState.charactersRemaining,
                                           characterState.maxTotalMessageSize,
                                           characterState.messagesSpent));
      charactersLeft.setVisibility(View.VISIBLE);
    } else {
      charactersLeft.setVisibility(View.GONE);
    }
  }

  private void onEmojiToggleClicked(View v) {
    if (!emojiDrawer.resolved()) {
      emojiDrawer.get().setProviders(0, new EmojiKeyboardProvider(requireContext(), new EmojiEventListener() {        @Override
        public void onKeyEvent(KeyEvent keyEvent) {
          getActiveInputField().dispatchKeyEvent(keyEvent);
        }

        @Override
        public void onEmojiSelected(String emoji) {
          getActiveInputField().insertEmoji(emoji);
        }
      }));
      emojiToggle.attach(emojiDrawer.get());
    }

    if (hud.getCurrentInput() == emojiDrawer.get()) {
      hud.showSoftkey(composeText);
    } else {
      hud.hideSoftkey(composeText, () -> hud.post(() -> hud.show(composeText, emojiDrawer.get())));
    }
  }

  @SuppressLint("StaticFieldLeak")
  private void processMedia(@NonNull List<Media> mediaList, @NonNull Map<Uri, Object> savedState) {
    Map<Media, ListenableFuture<Bitmap>> futures = new HashMap<>();

    for (Media media : mediaList) {
      Object state = savedState.get(media.getUri());

      if (state instanceof ImageEditorFragment.Data) {
        EditorModel model = ((ImageEditorFragment.Data) state).readModel();
        if (model != null && model.isChanged()) {
          futures.put(media, render(requireContext(), model));
        }
      }
    }

    new AsyncTask<Void, Void, List<Media>>() {

      private Stopwatch renderTimer;
      private Runnable    progressTimer;
      private AlertDialog dialog;

      @Override
      protected void onPreExecute() {
        renderTimer   = new Stopwatch("ProcessMedia");
        progressTimer = () -> {
          dialog = new AlertDialog.Builder(new ContextThemeWrapper(requireContext(), R.style.Theme_TextSecure_Dialog_MediaSendProgress))
                                  .setView(R.layout.progress_dialog)
                                  .setCancelable(false)
                                  .create();
          dialog.show();
          dialog.getWindow().setLayout(getResources().getDimensionPixelSize(R.dimen.mediasend_progress_dialog_size),
                                       getResources().getDimensionPixelSize(R.dimen.mediasend_progress_dialog_size));
        };
        Util.runOnMainDelayed(progressTimer, 250);
      }

      @Override
      protected List<Media> doInBackground(Void... voids) {
        Context     context      = requireContext();
        List<Media> updatedMedia = new ArrayList<>(mediaList.size());

        for (Media media : mediaList) {
          if (futures.containsKey(media)) {
            try {
              Bitmap                 bitmap   = futures.get(media).get();
              ByteArrayOutputStream  baos     = new ByteArrayOutputStream();
              bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);

              Uri uri = BlobProvider.getInstance()
                                    .forData(baos.toByteArray())
                                    .withMimeType(MediaTypes.IMAGE_JPEG)
                                    .createForSingleBchatOnDisk(context, e -> Log.w(TAG, "Failed to write to disk.", e));

              Media updated = new Media(uri, MediaTypes.IMAGE_JPEG, media.getDate(), bitmap.getWidth(), bitmap.getHeight(), baos.size(), media.getBucketId(), media.getCaption());

              updatedMedia.add(updated);
              renderTimer.split("item");
            } catch (InterruptedException | ExecutionException | IOException e) {
              Log.w(TAG, "Failed to render image. Using base image.");
              updatedMedia.add(media);
            }
          } else {
            updatedMedia.add(media);
          }
        }
        return updatedMedia;
      }

      @Override
      protected void onPostExecute(List<Media> media) {
        controller.onSendClicked(media, composeText.getTextTrimmed());
        Util.cancelRunnableOnMain(progressTimer);
        if (dialog != null) {
          dialog.dismiss();
        }
        renderTimer.stop(TAG);
      }
    }.execute();
  }

  private static ListenableFuture<Bitmap> render(@NonNull Context context, @NonNull EditorModel model) {
    SettableFuture<Bitmap> future = new SettableFuture<>();

    AsyncTask.THREAD_POOL_EXECUTOR.execute(() -> future.set(model.render(context)));

    return future;
  }

  public void onRequestFullScreen(boolean fullScreen) {
    captionAndRail.setVisibility(fullScreen ? View.GONE : View.VISIBLE);
  }

  private class FragmentPageChangeListener extends ViewPager.SimpleOnPageChangeListener {
    @Override
    public void onPageSelected(int position) {
      viewModel.onPageChanged(position);
    }
  }

  private class ComposeKeyPressedListener implements View.OnKeyListener, View.OnClickListener,
          TextWatcher, View.OnFocusChangeListener {

    int beforeLength;

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
      if (event.getAction() == KeyEvent.ACTION_DOWN) {
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
          if (TextSecurePreferences.isEnterSendsEnabled(requireContext())) {
            sendButton.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
            sendButton.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER));
            return true;
          }
        }
      }
      return false;
    }

    @Override
    public void onClick(View v) {
      hud.showSoftkey(composeText);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
      beforeLength = composeText.getTextTrimmed().length();
    }

    @Override
    public void onTextChanged(CharSequence text, int start, int before, int count) {
      if (isFormattingCompose) return;

      Editable editable = composeText.getText();
      if (editable == null) return;

      int cursorPos = composeText.getSelectionStart();
      String rawText = editable.toString();

      if (count > 1 && (text.toString().contains("* ") || text.toString().contains("- "))) {
        isFormattingCompose = true;
        try {
          int pos = 0;
          while (pos < rawText.length() - 1) {
            boolean isMarker = rawText.charAt(pos) == '*' || rawText.charAt(pos) == '-';
            boolean isSingleSpace = rawText.charAt(pos + 1) == ' ';
            boolean isDoubleSpace = (pos + 2 < rawText.length()) && rawText.charAt(pos + 2) == ' ';

            if (isMarker && isSingleSpace && !isDoubleSpace) {
              int lineStart = rawText.lastIndexOf('\n', pos);
              lineStart = (lineStart == -1) ? 0 : lineStart + 1;

              String beforeMarker = rawText.substring(lineStart, pos);

              if (beforeMarker.isEmpty()) {
                lastBulletTriggerChar = rawText.charAt(pos);
                editable.replace(pos, pos + 1, String.valueOf(bulletChar));
                pos++;
              }
            }
            pos++;
          }
        } finally {
          isFormattingCompose = false;
        }

        composeText.setSelection(Math.min(cursorPos, editable.length()));
        composeText.removeCallbacks(formatComposeRunnable);
        composeText.post(formatComposeRunnable);
        return;
      }

      if (count > 1) {
        composeText.removeCallbacks(formatComposeRunnable);
        composeText.post(() -> {
          applyFormattingToComposeText();
          viewModel.onBodyChanged(composeText.getText().toString());
        });
        return;
      }

      if (count == 1) {
        if (cursorPos >= 3) {
          String lastThree = rawText.substring(cursorPos - 3, cursorPos);
          if (lastThree.equals(bulletChar + "  ")) {
            isFormattingCompose = true;
            editable.replace(cursorPos - 3, cursorPos, lastBulletTriggerChar + "  ");
            composeText.setSelection(cursorPos);
            isFormattingCompose = false;
            return;
          }
        }

        if (cursorPos >= 2) {
          int lineStart = rawText.lastIndexOf('\n', Math.max(0, cursorPos - 2));
          lineStart = (lineStart == -1) ? 0 : lineStart + 1;

          String twoChars = rawText.substring(cursorPos - 2, cursorPos);
          int endIndex = cursorPos - 2;
          String beforeMarker =
                  (lineStart >= 0 && endIndex >= lineStart && endIndex <= rawText.length())
                          ? rawText.substring(lineStart, endIndex)
                          : "";

          boolean isNextCharSpace = cursorPos < rawText.length() && rawText.charAt(cursorPos) == ' ';

          if ((twoChars.equals("* ") || twoChars.equals("- "))
                  && beforeMarker.isEmpty()
                  && !isNextCharSpace) {

            if (BaseInputConnection.getComposingSpanStart(editable) != -1) return;

            lastBulletTriggerChar = twoChars.charAt(0);

            isFormattingCompose = true;
            editable.replace(cursorPos - 2, cursorPos, bulletChar + " ");
            composeText.setSelection(cursorPos);
            isFormattingCompose = false;
            return;
          }
        }
      }

      if (before == 1 && count == 0 && cursorPos > 0
              && editable.charAt(cursorPos - 1) == bulletChar) {

        isFormattingCompose = true;
        editable.replace(cursorPos - 1, cursorPos, String.valueOf(lastBulletTriggerChar));
        composeText.setSelection(cursorPos);
        isFormattingCompose = false;
        return;
      }

      if (count > before && text.subSequence(start, start + count).toString().contains("\n")) {
        isFormattingCompose = true;
        try {
          String beforeText = rawText.substring(0, Math.max(0, cursorPos - 1));
          handleAutoList(editable, beforeText);
        } finally {
          isFormattingCompose = false;
        }
        composeText.removeCallbacks(formatComposeRunnable);
        composeText.postDelayed(formatComposeRunnable, 80);
        return;
      }

      if (BaseInputConnection.getComposingSpanStart(editable) != -1) {
        composeText.removeCallbacks(formatComposeRunnable);
        composeText.post(formatComposeRunnable);
        return;
      }

      composeText.removeCallbacks(formatComposeRunnable);
      composeText.post(formatComposeRunnable);
    }

    @Override
    public void afterTextChanged(Editable s) {
      if (isFormattingCompose) return;
      presentCharactersRemaining();
      viewModel.onBodyChanged(s.toString());
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {}
  }

  private void handleAutoList(Editable editable, String beforeText) {
    int cursor = composeText.getSelectionStart();
    if (cursor == 0) return;

    int lineStart = beforeText.lastIndexOf('\n') + 1;
    String currentLine = beforeText.substring(lineStart);

    if (currentLine.startsWith(" ") || currentLine.startsWith("\t")) {
      return;
    }

    currentLine = currentLine.replaceAll("[\\u200B-\\u200D\\uFEFF]", "");

    Pattern numberPattern = Pattern.compile("^(\\d+)\\.\\s(?! )");
    Matcher numberMatch = numberPattern.matcher(currentLine);

    Pattern bulletPattern = Pattern.compile("^([\\-\\u2022\\*])\\s(?! )");
    Matcher bulletMatch = bulletPattern.matcher(currentLine);

    if (numberMatch.find()) {

      String numberStr = numberMatch.group(1);
      Integer number = null;
      try {
        number = Integer.parseInt(numberStr);
      } catch (NumberFormatException e) {
        return;
      }

      if (number < 1 || number > 99) {
        int lineBegin = editable.toString()
                .lastIndexOf('\n', cursor - 1);
        lineBegin = (lineBegin == -1) ? 0 : lineBegin + 1;

        editable.delete(lineBegin, cursor);
        composeText.setSelection(lineBegin);
        composeText.post(formatComposeRunnable);
        return;
      }

      String contentAfterMarker =
              currentLine.substring(numberMatch.group().length()).trim();

      if (contentAfterMarker.isEmpty()) {

        if (cursor > 0 && editable.charAt(cursor - 1) == '\n') {
          editable.delete(cursor - 1, cursor);
          cursor--;
        }

        int lineBegin = editable.toString()
                .lastIndexOf('\n', cursor - 1);
        lineBegin = (lineBegin == -1) ? 0 : lineBegin + 1;

        editable.delete(lineBegin, cursor);
        composeText.setSelection(lineBegin);
        composeText.post(formatComposeRunnable);
        return;
      }

      int nextNumber = number + 1;
      String insertText = nextNumber + ". ";

      if (nextNumber > 99) return;

      editable.insert(cursor, insertText);
      composeText.setSelection(cursor + insertText.length());
      composeText.post(formatComposeRunnable);
    } else if (bulletMatch.find()) {

      String contentAfterMarker =
              currentLine.substring(bulletMatch.group().length()).trim();

      if (contentAfterMarker.isEmpty()) {

        if (cursor > 0 && editable.charAt(cursor - 1) == '\n') {
          editable.delete(cursor - 1, cursor);
          cursor--;
        }

        int lineBegin = editable.toString()
                .lastIndexOf('\n', cursor - 1);
        lineBegin = (lineBegin == -1) ? 0 : lineBegin + 1;

        editable.delete(lineBegin, cursor);
        composeText.setSelection(lineBegin);
        composeText.post(formatComposeRunnable);
        return;
      }

      String bulletCharVal = bulletMatch.group(1);
      String insertText = bulletCharVal + " ";

      editable.insert(cursor, insertText);
      composeText.setSelection(cursor + insertText.length());
      composeText.post(formatComposeRunnable);
    } else {
      if (cursor > 0 && editable.charAt(cursor - 1) == '\n') return;
      editable.insert(cursor, "\n");
      composeText.setSelection(cursor + 1);
    }
  }

  private void applyFormattingToComposeText() {
    if (isFormattingCompose) return;

    Editable editable = composeText.getText();
    if (editable == null || editable.length() == 0) return;

    if (BaseInputConnection.getComposingSpanStart(editable) != -1) return;

    String raw = editable.toString();
    SpannableStringBuilder formatted =
            new SpannableStringBuilder(TextFormatter.formatAppText(raw, requireContext(), true));

    boolean textChanged = !formatted.toString().equals(raw);
    boolean spansChanged =
            formatted.getSpans(0, formatted.length(), Object.class).length > 0
                    || editable.getSpans(0, editable.length(), Object.class).length > 0;

    if (!textChanged && !spansChanged) return;

    isFormattingCompose = true;
    try {
      int cursorPos = Math.min(composeText.getSelectionStart(), raw.length());
      SpannableStringBuilder formattedBeforeCursor = new SpannableStringBuilder(
              TextFormatter.formatAppText(raw.substring(0, cursorPos), requireContext(), true));
      int newCursor = Math.min(formattedBeforeCursor.length(), formatted.length());

      composeText.removeTextChangedListener(composeKeyPressedListener);
      composeText.setText(formatted);

      // get NEW editable after setText()
      Editable newEditable = composeText.getText();

      //Quote formatting
      toUnicodeBlockQuote(requireContext(), newEditable, false);

      // wait until ALL text mutations are done
      applyNumberSpan(newEditable);

      composeText.setSelection(Math.max(0, Math.min(newCursor, composeText.length())));
      composeText.addTextChangedListener(composeKeyPressedListener);

      viewModel.onBodyChanged(composeText.getText().toString());
    } catch (Exception e) {
      Log.w(TAG, "Formatting failed", e);
    } finally {
      isFormattingCompose = false;
    }
  }

  private void applyNumberSpan(Editable editable) {
    int index = 0;
    String text = editable.toString();

    while (index < text.length()) {

      int lineEnd = text.indexOf('\n', index);
      if (lineEnd == -1) lineEnd = text.length();

      String line = text.substring(index, lineEnd);

      Matcher match = Pattern.compile("^(\\d+)\\.\\s").matcher(line);

      if (match.find()) {

        // Remove old margin spans (avoid stacking)
        LeadingMarginSpan[] marginSpans =
                editable.getSpans(index, lineEnd, LeadingMarginSpan.class);
        for (LeadingMarginSpan span : marginSpans) {
          editable.removeSpan(span);
        }

        // Apply indentation
        editable.setSpan(
                new LeadingMarginSpan.Standard(24, 24),
                index,
                lineEnd,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        // Apply bold ONLY to number prefix
        int numberEnd = index + match.group().length();

        editable.setSpan(
                new StyleSpan(android.graphics.Typeface.BOLD),
                index,
                numberEnd,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );
      }

      index = lineEnd + 1;
    }
  }

  public static void toUnicodeBlockQuote(
          Context context,
          Editable builder,
          boolean removeMarker
  ) {

    // Remove old spans
    CustomQuoteSpan[] quoteSpans =
            builder.getSpans(0, builder.length(), CustomQuoteSpan.class);
    for (CustomQuoteSpan span : quoteSpans) {
      builder.removeSpan(span);
    }

    QuoteIndentSpan[] indentSpans =
            builder.getSpans(0, builder.length(), QuoteIndentSpan.class);
    for (QuoteIndentSpan span : indentSpans) {
      builder.removeSpan(span);
    }

    int i = 0;

    while (i < builder.length()) {

      int lineStart = i;
      int lineEnd = builder.toString().indexOf('\n', i);
      if (lineEnd == -1) lineEnd = builder.length();

      if (lineStart >= lineEnd) {
        i = lineEnd + 1;
        continue;
      }

      if (builder.charAt(lineStart) == '>') {

        boolean valid =
                (lineStart + 2 < lineEnd) &&
                        (builder.charAt(lineStart + 1) == ' ') &&
                        (builder.charAt(lineStart + 2) != ' ');

        if (valid) {

          int contentStart;

          if (removeMarker) {
            // Remove "> "
            builder.delete(lineStart, lineStart + 2);
            lineEnd -= 2;
            contentStart = lineStart;

          } else {
            // Keep text, hide "> "
            contentStart = lineStart + 2;

            builder.setSpan(
                    new android.text.style.ScaleXSpan(0f),
                    lineStart,
                    contentStart,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
          }

          // Quote bar
          builder.setSpan(
                  new CustomQuoteSpan(context),
                  lineStart,
                  lineEnd,
                  Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
          );

          // Alignment spacing
          builder.setSpan(
                  new QuoteIndentSpan(20),
                  lineStart,
                  lineEnd,
                  Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
          );

          // Text color
          builder.setSpan(
                  new android.text.style.ForegroundColorSpan(
                          ContextCompat.getColor(context, R.color.quote_gray)
                  ),
                  contentStart,
                  lineEnd,
                  Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
          );

          i = lineEnd + 1;
          continue;
        }
      }

      i = lineEnd + 1;
    }
  }
  public interface Controller {
    void onAddMediaClicked(@NonNull String bucketId);
    void onSendClicked(@NonNull List<Media> media, @NonNull String body);
    void onNoMediaAvailable();
  }
}
