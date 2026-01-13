package io.beldex.bchat.wallet.utils.keyboardview.interfaces;

import io.beldex.bchat.wallet.utils.keyboardview.enums.KeyboardButtonEnum;

public interface KeyboardButtonClickedListener {
    void onKeyboardClick(KeyboardButtonEnum keyboardButtonEnum);

    void onRippleAnimationEnd();
}
