package io.beldex.bchat.scribbles;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.beldex.libsignal.utilities.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import io.beldex.bchat.R;
import io.beldex.bchat.components.emoji.Emoji;
import io.beldex.bchat.components.emoji.EmojiPageModel;
import io.beldex.bchat.components.emoji.RecentEmojiPageModel;
import io.beldex.bchat.emoji.EmojiCategory;
import io.beldex.bchat.emoji.EmojiSource;
import io.beldex.bchat.imageeditor.ImageEditorView;

/**
 * The HUD (heads-up display) that contains all of the tools for interacting with
 * {@link ImageEditorView}
 */
public final class ImageEditorHud extends LinearLayout {

  private static final String TAG = "ImageEditorHud";

  private static final int EMOJI_COLUMNS        = 8;
  private static final int EMOJI_CELL_HEIGHT_DP = 44;

  private static final EmojiCategory[] EMOJI_TAB_CATEGORIES = {
      null, EmojiCategory.PEOPLE, EmojiCategory.NATURE, EmojiCategory.FOODS,
      EmojiCategory.ACTIVITY, EmojiCategory.PLACES, EmojiCategory.SYMBOLS
  };

  private View      cropTitle;
  private View      cropReset;
  private View      toolbar;
  private View      textButton;
  private View      drawButton;
  private View      highlightButton;
  private View      stickerButton;
  private View      cropButton;
  private View      panel;
  private View      history;
  private View      undoButton;
  private View      redoButton;
  private View      drawChip;
  private View      highlightChip;
  private ImageView cropAspectLock;
  private View      cropRotateButton;
  private View      cropFlipButton;
  private View      deleteButton;
  private ViewGroup colors;
  private View      emojiPanel;
  private ViewGroup emojiTabs;
  private EditText  emojiSearch;
  private TextView  emojiSection;
  private int       panelBottomPadding;
  private int       emojiPanelHeight;
  private int       bottomPanelHeight;

  @Nullable
  private RecentEmojiPageModel recentEmojiPageModel;
  @Nullable
  private List<String[]>       emojiSearchIndex;

  private final EmojiAdapter emojiAdapter = new EmojiAdapter();

  @NonNull
  private EventListener eventListener = NULL_EVENT_LISTENER;

  private final Map<Mode, Set<View>> visibilityModeMap = new HashMap<>();
  private final Set<View>            allViews          = new HashSet<>();

  private Mode    currentMode;
  private int     activeColor;
  private boolean highlightColors;

  public ImageEditorHud(@NonNull Context context) {
    super(context);
    initialize();
  }

  public ImageEditorHud(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    initialize();
  }

  public ImageEditorHud(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    initialize();
  }

  private void initialize() {
    inflate(getContext(), R.layout.image_editor_hud, this);
    setOrientation(VERTICAL);

    cropTitle        = findViewById(R.id.scribble_crop_title);
    cropReset        = findViewById(R.id.scribble_crop_reset);
    toolbar          = findViewById(R.id.scribble_toolbar);
    textButton       = findViewById(R.id.scribble_text_button);
    drawButton       = findViewById(R.id.scribble_draw_button);
    highlightButton  = findViewById(R.id.scribble_highlight_button);
    stickerButton    = findViewById(R.id.scribble_sticker_button);
    cropButton       = findViewById(R.id.scribble_crop_button);
    panel            = findViewById(R.id.scribble_panel);
    history          = findViewById(R.id.scribble_history);
    undoButton       = findViewById(R.id.scribble_undo_button);
    redoButton       = findViewById(R.id.scribble_redo_button);
    drawChip         = findViewById(R.id.scribble_draw_chip);
    highlightChip    = findViewById(R.id.scribble_highlight_chip);
    cropAspectLock   = findViewById(R.id.scribble_crop_aspect_lock);
    cropRotateButton = findViewById(R.id.scribble_crop_rotate);
    cropFlipButton   = findViewById(R.id.scribble_crop_flip);
    deleteButton     = findViewById(R.id.scribble_delete_button);
    colors           = findViewById(R.id.scribble_colors);
    emojiPanel       = findViewById(R.id.scribble_emoji_panel);
    emojiTabs        = findViewById(R.id.scribble_emoji_tabs);
    emojiSearch      = findViewById(R.id.scribble_emoji_search);
    emojiSection     = findViewById(R.id.scribble_emoji_section);

    panelBottomPadding = panel.getPaddingBottom();
    emojiPanelHeight   = emojiPanel.getLayoutParams().height;

    OnLayoutChangeListener panelLayoutListener = (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> updateBottomPanelHeight();
    panel.addOnLayoutChangeListener(panelLayoutListener);
    emojiPanel.addOnLayoutChangeListener(panelLayoutListener);

    cropAspectLock.setOnClickListener(v -> {
      eventListener.onCropAspectLock(!eventListener.isCropAspectLocked());
      updateCropAspectLockImage(eventListener.isCropAspectLocked());
    });

    initializeViews();
    initializeVisibilityMap();
    setUndoAvailability(false);
    setRedoAvailability(false);
    setMode(Mode.NONE);
  }

  private void updateCropAspectLockImage(boolean cropAspectLocked) {
    cropAspectLock.setImageResource(cropAspectLocked ? R.drawable.ic_crop_lock_32 : R.drawable.ic_crop_unlock_32);
  }

  private void initializeVisibilityMap() {
    setVisibleViewsWhenInMode(Mode.NONE, toolbar);

    setVisibleViewsWhenInMode(Mode.DRAW, toolbar, panel, history, drawChip, highlightChip, colors);

    setVisibleViewsWhenInMode(Mode.HIGHLIGHT, toolbar, panel, history, drawChip, highlightChip, colors);

    setVisibleViewsWhenInMode(Mode.TEXT, toolbar, panel, deleteButton, colors);

    setVisibleViewsWhenInMode(Mode.MOVE_DELETE, toolbar, panel, deleteButton);

    setVisibleViewsWhenInMode(Mode.EMOJI, toolbar, emojiPanel);

    setVisibleViewsWhenInMode(Mode.CROP, cropTitle, cropReset, panel, cropAspectLock, cropRotateButton, cropFlipButton);

    for (Set<View> views : visibilityModeMap.values()) {
      allViews.addAll(views);
    }
  }

  private void setVisibleViewsWhenInMode(Mode mode, View... views) {
    visibilityModeMap.put(mode, new HashSet<>(Arrays.asList(views)));
  }

  private void initializeViews() {
    undoButton.setOnClickListener(v -> eventListener.onUndo());
    redoButton.setOnClickListener(v -> eventListener.onRedo());

    deleteButton.setOnClickListener(v -> {
      eventListener.onDelete();
      setMode(Mode.NONE);
    });

    cropButton.setOnClickListener(v -> setMode(Mode.CROP));
    cropFlipButton.setOnClickListener(v -> eventListener.onFlipHorizontal());
    cropRotateButton.setOnClickListener(v -> eventListener.onRotate90AntiClockwise());
    cropReset.setOnClickListener(v -> eventListener.onCropReset());

    findViewById(R.id.scribble_done_button).setOnClickListener(v -> setMode(Mode.NONE));
    findViewById(R.id.scribble_cancel_button).setOnClickListener(v -> {
      eventListener.onCancel();
      setMode(Mode.NONE);
    });

    for (int i = 0; i < colors.getChildCount(); i++) {
      View swatch = colors.getChildAt(i);
      swatch.setOnClickListener(v -> selectColor(swatchColor(swatch), true));
    }

    drawButton.setOnClickListener(v -> setMode(Mode.DRAW));
    highlightButton.setOnClickListener(v -> setMode(Mode.HIGHLIGHT));
    drawChip.setOnClickListener(v -> setMode(Mode.DRAW));
    highlightChip.setOnClickListener(v -> setMode(Mode.HIGHLIGHT));
    textButton.setOnClickListener(v -> setMode(Mode.TEXT));
    stickerButton.setOnClickListener(v -> setMode(currentMode == Mode.EMOJI ? Mode.NONE : Mode.EMOJI));

    initializeEmojiPanel();
  }

  private void initializeEmojiPanel() {
    RecyclerView grid = findViewById(R.id.scribble_emoji_grid);
    grid.setLayoutManager(new GridLayoutManager(getContext(), EMOJI_COLUMNS));
    grid.setAdapter(emojiAdapter);

    for (int i = 0; i < emojiTabs.getChildCount(); i++) {
      int index = i;
      emojiTabs.getChildAt(i).setOnClickListener(v -> {
        emojiSearch.setText("");
        showEmojiTab(index);
      });
    }

    emojiSearch.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {
      }

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
      }

      @Override
      public void afterTextChanged(Editable s) {
        String query = s.toString().trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
          showEmojiTab(1);
        } else {
          showEmojiSearch(query);
        }
      }
    });
  }

  private void showEmojiTab(int index) {
    for (int i = 0; i < emojiTabs.getChildCount(); i++) {
      emojiTabs.getChildAt(i).setSelected(i == index);
    }

    EmojiCategory category = EMOJI_TAB_CATEGORIES[index];
    if (category == null) {
      if (recentEmojiPageModel == null) {
        recentEmojiPageModel = new RecentEmojiPageModel(getContext());
      }
      setEmojiSection(R.string.ReactWithAnyEmojiBottomSheetDialogFragment__recently_used);
      emojiAdapter.setEmoji(recentEmojiPageModel.getEmoji());
      return;
    }

    setEmojiSection(category.getCategoryLabel());
    List<String> emoji = new ArrayList<>();
    for (EmojiPageModel page : EmojiSource.getLatest().getDisplayPages()) {
      if (category.getKey().equals(page.getKey())) {
        for (Emoji item : page.getDisplayEmoji()) {
          emoji.add(item.getValue());
        }
      }
    }
    emojiAdapter.setEmoji(emoji);
  }

  private void setEmojiSection(@StringRes int label) {
    emojiSection.setText(label);
  }

  private void showEmojiSearch(@NonNull String query) {
    for (int i = 0; i < emojiTabs.getChildCount(); i++) {
      emojiTabs.getChildAt(i).setSelected(false);
    }
    emojiSection.setText(query);

    List<String> results = new ArrayList<>();
    for (String[] entry : getEmojiSearchIndex()) {
      for (int i = 1; i < entry.length; i++) {
        if (entry[i].contains(query)) {
          results.add(entry[0]);
          break;
        }
      }
    }
    emojiAdapter.setEmoji(results);
  }

  private @NonNull List<String[]> getEmojiSearchIndex() {
    if (emojiSearchIndex != null) return emojiSearchIndex;

    List<String[]> index = new ArrayList<>();
    try (InputStream stream = getContext().getAssets().open("emoji/emoji_search_index.json")) {
      byte[] bytes = new byte[stream.available()];
      int    read  = 0;
      while (read < bytes.length) {
        int count = stream.read(bytes, read, bytes.length - read);
        if (count < 0) break;
        read += count;
      }
      JSONArray entries = new JSONArray(new String(bytes, 0, read, StandardCharsets.UTF_8));
      for (int i = 0; i < entries.length(); i++) {
        JSONObject entry = entries.getJSONObject(i);
        JSONArray  tags  = entry.getJSONArray("tags");
        String[]   row   = new String[tags.length() + 1];
        row[0] = entry.getString("emoji");
        for (int t = 0; t < tags.length(); t++) {
          row[t + 1] = tags.getString(t).toLowerCase(Locale.ROOT);
        }
        index.add(row);
      }
    } catch (Exception e) {
      Log.w(TAG, "Failed to load the emoji search index.", e);
    }
    emojiSearchIndex = index;
    return index;
  }

  private void onEmojiClicked(@NonNull String emoji) {
    if (recentEmojiPageModel == null) {
      recentEmojiPageModel = new RecentEmojiPageModel(getContext());
    }
    recentEmojiPageModel.getEmoji();
    RecentEmojiPageModel.onCodePointSelected(emoji);
    eventListener.onEmojiSelected(emoji);
  }

  private static int swatchColor(@NonNull View swatch) {
    Drawable background = ((ViewGroup) swatch).getChildAt(0).getBackground();
    return background instanceof ColorDrawable ? ((ColorDrawable) background).getColor() : 0xFFFFFFFF;
  }

  private void selectColor(int color, boolean notify) {
    activeColor = color;
    for (int i = 0; i < colors.getChildCount(); i++) {
      View swatch = colors.getChildAt(i);
      swatch.setSelected((swatchColor(swatch) & 0xFFFFFF) == (color & 0xFFFFFF));
    }
    if (notify) {
      eventListener.onColorChange(highlightColors ? replaceAlphaWith128(color) : color);
    }
  }

  public int getActiveColor() {
    return activeColor;
  }

  public void setActiveColor(int color) {
    selectColor(color, true);
  }

  public void setEventListener(@Nullable EventListener eventListener) {
    this.eventListener = eventListener != null ? eventListener : NULL_EVENT_LISTENER;
  }

  public void setBottomInset(int bottomInset) {
    panel.setPadding(panel.getPaddingLeft(), panel.getPaddingTop(), panel.getPaddingRight(), panelBottomPadding + bottomInset);

    ViewGroup.LayoutParams params = emojiPanel.getLayoutParams();
    params.height = emojiPanelHeight + bottomInset;
    emojiPanel.setLayoutParams(params);
    emojiPanel.setPadding(0, 0, 0, bottomInset);
  }

  public void enterMode(@NonNull Mode mode) {
    setMode(mode, false);
  }

  private void setMode(@NonNull Mode mode) {
    setMode(mode, true);
  }

  private void setMode(@NonNull Mode mode, boolean notify) {
    this.currentMode = mode;
    Set<View> visibleViews = visibilityModeMap.get(mode);
    for (View view : allViews) {
      view.setVisibility(visibleViews != null && visibleViews.contains(view) ? VISIBLE : GONE);
    }

    textButton.setSelected(mode == Mode.TEXT);
    drawButton.setSelected(mode == Mode.DRAW);
    highlightButton.setSelected(mode == Mode.HIGHLIGHT);
    stickerButton.setSelected(mode == Mode.EMOJI || mode == Mode.MOVE_DELETE);
    drawChip.setSelected(mode == Mode.DRAW);
    highlightChip.setSelected(mode == Mode.HIGHLIGHT);

    switch (mode) {
      case CROP:      presentModeCrop();      break;
      case DRAW:      presentModeDraw();      break;
      case HIGHLIGHT: presentModeHighlight(); break;
      case TEXT:      presentModeText();      break;
      case EMOJI:     presentModeEmoji();     break;
    }

    if (notify) {
      eventListener.onModeStarted(mode);
    }
    eventListener.onRequestFullScreen(mode != Mode.NONE);
    post(this::updateBottomPanelHeight);
  }

  private void updateBottomPanelHeight() {
    int height = 0;
    if (panel.getVisibility() == VISIBLE) {
      height = panel.getHeight();
    } else if (emojiPanel.getVisibility() == VISIBLE) {
      height = emojiPanel.getHeight();
    }
    if (height != bottomPanelHeight) {
      bottomPanelHeight = height;
      eventListener.onBottomPanelHeightChanged(height);
    }
  }

  private void presentModeCrop() {
    updateCropAspectLockImage(eventListener.isCropAspectLocked());
  }

  private void presentModeDraw() {
    highlightColors = false;
    selectColor(ContextCompat.getColor(getContext(), R.color.rounded_indicator), true);
  }

  private void presentModeHighlight() {
    highlightColors = true;
    selectColor(ContextCompat.getColor(getContext(), R.color.editor_swatch_yellow), true);
  }

  private void presentModeEmoji() {
    emojiSearch.setText("");
    showEmojiTab(1);
  }

  private void presentModeText() {
    highlightColors = false;
    selectColor(ContextCompat.getColor(getContext(), R.color.text_white), true);
  }

  private static int replaceAlphaWith128(int color) {
    return color & ~0xff000000 | 0x80000000;
  }

  public void setUndoAvailability(boolean undoAvailable) {
    undoButton.setEnabled(undoAvailable);
  }

  public void setRedoAvailability(boolean redoAvailable) {
    redoButton.setEnabled(redoAvailable);
  }

  public enum Mode {
    NONE, DRAW, HIGHLIGHT, TEXT, MOVE_DELETE, CROP, EMOJI
  }

  public interface EventListener {
    void onModeStarted(@NonNull Mode mode);
    void onColorChange(int color);
    void onUndo();
    void onRedo();
    void onDelete();
    void onCancel();
    void onCropReset();
    void onEmojiSelected(@NonNull String emoji);
    void onBottomPanelHeightChanged(int height);
    void onFlipHorizontal();
    void onRotate90AntiClockwise();
    void onCropAspectLock(boolean locked);
    boolean isCropAspectLocked();
    void onRequestFullScreen(boolean fullScreen);
  }

  private static final EventListener NULL_EVENT_LISTENER = new EventListener() {

    @Override
    public void onModeStarted(@NonNull Mode mode) {
    }

    @Override
    public void onColorChange(int color) {
    }

    @Override
    public void onUndo() {
    }

    @Override
    public void onRedo() {
    }

    @Override
    public void onDelete() {
    }

    @Override
    public void onCancel() {
    }

    @Override
    public void onCropReset() {
    }

    @Override
    public void onEmojiSelected(@NonNull String emoji) {
    }

    @Override
    public void onBottomPanelHeightChanged(int height) {
    }

    @Override
    public void onFlipHorizontal() {
    }

    @Override
    public void onRotate90AntiClockwise() {
    }

    @Override
    public void onCropAspectLock(boolean locked) {
    }

    @Override
    public boolean isCropAspectLocked() {
      return false;
    }

    @Override
    public void onRequestFullScreen(boolean fullScreen) {
    }
  };

  private final class EmojiAdapter extends RecyclerView.Adapter<EmojiAdapter.EmojiViewHolder> {

    private final List<String> emoji = new ArrayList<>();

    void setEmoji(@NonNull List<String> emoji) {
      this.emoji.clear();
      this.emoji.addAll(emoji);
      notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EmojiViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
      TextView view = new TextView(parent.getContext());
      int      height = Math.round(EMOJI_CELL_HEIGHT_DP * getResources().getDisplayMetrics().density);
      view.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));
      view.setGravity(Gravity.CENTER);
      view.setTextSize(22);
      return new EmojiViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EmojiViewHolder holder, int position) {
      String value = emoji.get(position);
      holder.text.setText(value);
      holder.text.setOnClickListener(v -> onEmojiClicked(value));
    }

    @Override
    public int getItemCount() {
      return emoji.size();
    }

    final class EmojiViewHolder extends RecyclerView.ViewHolder {
      final TextView text;

      EmojiViewHolder(@NonNull TextView text) {
        super(text);
        this.text = text;
      }
    }
  }
}
