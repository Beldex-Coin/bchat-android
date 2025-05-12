package io.beldex.bchat.components.emoji;

import android.view.KeyEvent;
public interface EmojiEventListener {
    void onEmojiSelected(String emoji);
    void onKeyEvent(KeyEvent keyEvent);
}