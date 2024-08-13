package io.beldex.bchat.wallet.utils.keyboardview.interfaces;

import io.beldex.bchat.wallet.utils.keyboardview.enums.KeyboardButtonEnum;

public interface KeyboardButtonClickedListener {
    public void onKeyboardClick(KeyboardButtonEnum keyboardButtonEnum);

    public void onRippleAnimationEnd();
}
