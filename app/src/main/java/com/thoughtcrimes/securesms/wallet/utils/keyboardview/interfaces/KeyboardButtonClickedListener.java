package com.thoughtcrimes.securesms.wallet.utils.keyboardview.interfaces;

import com.thoughtcrimes.securesms.wallet.utils.keyboardview.enums.KeyboardButtonEnum;

public interface KeyboardButtonClickedListener {
    public void onKeyboardClick(KeyboardButtonEnum keyboardButtonEnum);

    public void onRippleAnimationEnd();
}
