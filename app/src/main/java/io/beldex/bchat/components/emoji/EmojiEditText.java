package io.beldex.bchat.components.emoji;

import android.content.Context;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;
import android.text.InputFilter;
import android.util.AttributeSet;

import io.beldex.bchat.R;

import com.beldex.libbchat.utilities.TextSecurePreferences;
import io.beldex.bchat.components.emoji.EmojiProvider.EmojiDrawable;

public class EmojiEditText extends AppCompatEditText {
  private static final String TAG = EmojiEditText.class.getSimpleName();

  public EmojiEditText(Context context) {
    this(context, null);
  }

  public EmojiEditText(Context context, AttributeSet attrs) {
    this(context, attrs, R.attr.editTextStyle);
  }

  public EmojiEditText(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    if (!TextSecurePreferences.isSystemEmojiPreferred(getContext())) {
      setFilters(appendEmojiFilter(this.getFilters()));
    }
  }

  public void insertEmoji(String emoji) {
    final int          start = getSelectionStart();
    final int          end   = getSelectionEnd();

    getText().replace(Math.min(start, end), Math.max(start, end), emoji);
    setSelection(start + emoji.length());
  }

  @Override
  public void invalidateDrawable(@NonNull Drawable drawable) {
    if (drawable instanceof EmojiProvider.EmojiDrawable) invalidate();
    else                                   super.invalidateDrawable(drawable);
  }

  private InputFilter[] appendEmojiFilter(@Nullable InputFilter[] originalFilters) {
    InputFilter[] result;

    if (originalFilters != null) {
      result = new InputFilter[originalFilters.length + 1];
      System.arraycopy(originalFilters, 0, result, 1, originalFilters.length);
    } else {
      result = new InputFilter[1];
    }

    result[0] = new EmojiFilter(this);

    return result;
  }
}
