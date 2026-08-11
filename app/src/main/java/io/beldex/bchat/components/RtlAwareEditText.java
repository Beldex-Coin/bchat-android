package io.beldex.bchat.components;

import android.content.Context;
import android.graphics.Rect;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;

import java.util.Locale;
import java.util.regex.Pattern;

public class RtlAwareEditText extends AppCompatEditText {

  private static final Pattern ARABIC_PATTERN =
      Pattern.compile("[\\u0600-\\u06FF\\u0750-\\u077F\\u08A0-\\u08FF\\uFB50-\\uFDFF\\uFE70-\\uFEFF]");

  public static void applyDirectionHandling(android.widget.EditText editText) {
    if (editText == null) return;
    editText.addTextChangedListener(new TextWatcher() {
      @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
      @Override public void afterTextChanged(Editable s) {}
      @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
        applyDirection(editText, s);
      }
    });
    applyDirection(editText, editText.getText());
  }

  private static void applyDirection(android.widget.EditText editText, @Nullable CharSequence text) {
    int direction = getDirection(editText, text);
    int alignment = direction == View.TEXT_DIRECTION_INHERIT
        ? View.TEXT_ALIGNMENT_INHERIT
        : View.TEXT_ALIGNMENT_TEXT_START;

    boolean changed = false;
    if (editText.getTextDirection() != direction) {
      editText.setTextDirection(direction);
      changed = true;
    }
    if (editText.getTextAlignment() != alignment) {
      editText.setTextAlignment(alignment);
      changed = true;
    }

    if (changed) refreshCursorPosition(editText);
  }

  private static void refreshCursorPosition(final android.widget.EditText editText) {
    editText.post(new Runnable() {
      @Override public void run() {
        int length = editText.length();
        int start = Math.min(editText.getSelectionStart(), length);
        int end = Math.min(editText.getSelectionEnd(), length);
        if (start < 0 || end < 0) return;
        editText.setSelection(start, end);
      }
    });
  }

  private static int getDirection(android.widget.EditText editText, @Nullable CharSequence text) {
    if (text != null && text.length() > 0) {
      return ARABIC_PATTERN.matcher(text).find() ? View.TEXT_DIRECTION_RTL : View.TEXT_DIRECTION_LTR;
    }
    Boolean arabic = isArabicKeyboard(editText);
    if (arabic == null) return View.TEXT_DIRECTION_INHERIT;
    return arabic ? View.TEXT_DIRECTION_RTL : View.TEXT_DIRECTION_LTR;
  }

  @Nullable
  private static Boolean isArabicKeyboard(android.widget.EditText editText) {
    try {
      InputMethodManager imm =
          (InputMethodManager) editText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
      if (imm == null) return null;
      InputMethodSubtype subtype = imm.getCurrentInputMethodSubtype();
      if (subtype == null) return null;
      String locale = subtype.getLanguageTag();
      if (locale == null || locale.isEmpty()) locale = subtype.getLocale();
      if (locale == null || locale.isEmpty()) return null;
      return locale.toLowerCase(Locale.ROOT).startsWith("ar");
    } catch (RuntimeException e) {
      return null;
    }
  }

  public RtlAwareEditText(Context context) {
    super(context);
    init();
  }

  public RtlAwareEditText(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  public RtlAwareEditText(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init();
  }

  @Override
  protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
    super.onFocusChanged(focused, direction, previouslyFocusedRect);
    if (focused) applyDirection(this, getText());
  }

  private void init() {
    if (isInEditMode()) return;

    addTextChangedListener(new TextWatcher() {
      @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
      @Override public void afterTextChanged(Editable s) {}
      @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
        applyDirection(RtlAwareEditText.this, s);
      }
    });

    applyDirection(this, getText());
  }
}
