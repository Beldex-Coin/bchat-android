package io.beldex.bchat.components.emoji;

import android.content.Context;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.TextViewCompat;
import androidx.appcompat.widget.AppCompatTextView;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import io.beldex.bchat.R;

import io.beldex.bchat.components.emoji.parsing.EmojiParser;
import com.beldex.libbchat.utilities.TextSecurePreferences;
import com.beldex.libbchat.utilities.Util;
import io.beldex.bchat.components.emoji.EmojiProvider.EmojiDrawable;
import io.beldex.bchat.components.emoji.parsing.EmojiParser;
import com.beldex.libsignal.utilities.guava.Optional;

public class EmojiTextView extends AppCompatTextView {
  private final boolean scaleEmojis;

  private static final char ELLIPSIS = '…';

  private CharSequence previousText;
  private BufferType   previousBufferType;
  private float        originalFontSize;
  private boolean      useSystemEmoji;
  private boolean      sizeChangeInProgress;
  private int          maxLength;
  private CharSequence overflowText;
  private CharSequence previousOverflowText;

  public EmojiTextView(Context context) {
    this(context, null);
  }

  public EmojiTextView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public EmojiTextView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);

    scaleEmojis = true;
    maxLength   = 1000;
    originalFontSize = getResources().getDimension(R.dimen.small_font_size);
  }

  @Override public void setText(@Nullable CharSequence text, BufferType type) {
    EmojiParser.CandidateList candidates = EmojiProvider.getCandidates(text);

    if (scaleEmojis && candidates != null && candidates.allEmojis) {
      int   emojis = candidates.size();
      float scale  = 1.0f;

      if (emojis <= 8) scale += 0.25f;
      if (emojis <= 6) scale += 0.25f;
      if (emojis <= 4) scale += 0.25f;
      if (emojis <= 2) scale += 0.25f;

      super.setTextSize(TypedValue.COMPLEX_UNIT_PX, originalFontSize * scale);
    } else if (scaleEmojis) {
      super.setTextSize(TypedValue.COMPLEX_UNIT_PX, originalFontSize);
    }

    if (unchanged(text, overflowText, type)) {
      return;
    }

    previousText         = text;
    previousOverflowText = overflowText;
    previousBufferType   = type;
    useSystemEmoji       = useSystemEmoji();

    if (useSystemEmoji || candidates == null || candidates.size() == 0) {
      super.setText(new SpannableStringBuilder(Optional.fromNullable(text).or("")).append(Optional.fromNullable(overflowText).or("")), BufferType.NORMAL);

      if (getEllipsize() == TextUtils.TruncateAt.END && maxLength > 0) {
        ellipsizeAnyTextForMaxLength();
      }
    } else {
      CharSequence emojified = EmojiProvider.emojify(candidates, text, this, false);
      super.setText(new SpannableStringBuilder(emojified).append(Optional.fromNullable(overflowText).or("")), BufferType.SPANNABLE);

      // Android fails to ellipsize spannable strings. (https://issuetracker.google.com/issues/36991688)
      // We ellipsize them ourselves by manually truncating the appropriate section.
      if (getEllipsize() == TextUtils.TruncateAt.END) {
        if (maxLength > 0) {
          ellipsizeAnyTextForMaxLength();
        } else {
          ellipsizeEmojiTextForMaxLines();
        }
      }
    }
  }

  public void setOverflowText(@Nullable CharSequence overflowText) {
    this.overflowText = overflowText;
    setText(previousText, BufferType.SPANNABLE);
  }

  private void ellipsizeAnyTextForMaxLength() {
    if (maxLength > 0 && getText().length() > maxLength + 1) {
      SpannableStringBuilder newContent = new SpannableStringBuilder();
      newContent.append(getText().subSequence(0, maxLength)).append(ELLIPSIS).append(Optional.fromNullable(overflowText).or(""));

      EmojiParser.CandidateList newCandidates = EmojiProvider.getCandidates(newContent);

      if (useSystemEmoji || newCandidates == null || newCandidates.size() == 0) {
        super.setText(newContent, BufferType.NORMAL);
      } else {
        CharSequence emojified = EmojiProvider.emojify(newCandidates, newContent, this, false);
        super.setText(emojified, BufferType.SPANNABLE);
      }
    }
  }

  private void ellipsizeEmojiTextForMaxLines() {
    post(() -> {
      if (getLayout() == null) {
        ellipsizeEmojiTextForMaxLines();
        return;
      }

      int maxLines = TextViewCompat.getMaxLines(EmojiTextView.this);
      if (maxLines <= 0 && maxLength < 0) {
        return;
      }

      int lineCount = getLineCount();
      if (lineCount > maxLines) {
        int overflowStart = getLayout().getLineStart(maxLines - 1);
        CharSequence overflow = getText().subSequence(overflowStart, getText().length());
        CharSequence ellipsized = TextUtils.ellipsize(overflow, getPaint(), getWidth(), TextUtils.TruncateAt.END);

        SpannableStringBuilder newContent = new SpannableStringBuilder();
        newContent.append(getText().subSequence(0, overflowStart))
                  .append(ellipsized.subSequence(0, ellipsized.length()))
                  .append(Optional.fromNullable(overflowText).or(""));

        EmojiParser.CandidateList newCandidates = EmojiProvider.getCandidates(newContent);
        CharSequence              emojified     = EmojiProvider.emojify(newCandidates, newContent, this, false);

        super.setText(emojified, BufferType.SPANNABLE);
      }
    });
  }

  private boolean unchanged(CharSequence text, CharSequence overflowText, BufferType bufferType) {
    return Util.equals(previousText, text)                 &&
           Util.equals(previousOverflowText, overflowText) &&
           Util.equals(previousBufferType, bufferType)     &&
           useSystemEmoji == useSystemEmoji()              &&
           !sizeChangeInProgress;
  }

  private boolean useSystemEmoji() {
   return TextSecurePreferences.isSystemEmojiPreferred(getContext());
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);

    if (!sizeChangeInProgress) {
      sizeChangeInProgress = true;
      setText(previousText, previousBufferType);
      sizeChangeInProgress = false;
    }
  }

  @Override
  public void invalidateDrawable(@NonNull Drawable drawable) {
    if (drawable instanceof EmojiProvider.EmojiDrawable) {
      invalidate();
    } else {
      super.invalidateDrawable(drawable);
    }
  }

  @Override
  public void setTextSize(float size) {
    setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
  }

  @Override
  public void setTextSize(int unit, float size) {
    this.originalFontSize = TypedValue.applyDimension(unit, size, getResources().getDisplayMetrics());
    super.setTextSize(unit, size);
  }
}
