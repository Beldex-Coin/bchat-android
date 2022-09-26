package com.thoughtcrimes.securesms.data;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.thoughtcrimes.securesms.model.Wallet;
import com.thoughtcrimes.securesms.wallet.utils.validator.BitcoinAddressType;
import com.thoughtcrimes.securesms.wallet.utils.validator.BitcoinAddressValidator;

import io.beldex.bchat.R;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Crypto {
    XMR("XMR", true, "Beldex:tx_amount:recipient_name:tx_description", R.id.ibBDX, R.id.ibBDX, R.id.ibBDX, Wallet::isAddressValid),
    BTC("BTC", true, "bitcoin:amount:label:message", R.id.ibBDX, R.id.ibBDX, R.id.ibBDX, address -> {
        Log.d("BTC ->%s", address);
        return BitcoinAddressValidator.validate(address, BitcoinAddressType.BTC);
    }),
    DASH("DASH", true, "dash:amount:label:message", R.id.ibBDX, R.id.ibBDX, R.id.ibBDX, address -> {
        return BitcoinAddressValidator.validate(address, BitcoinAddressType.DASH);
    }),
    DOGE("DOGE", true, "dogecoin:amount:label:message", R.id.ibBDX, R.id.ibBDX, R.id.ibBDX, address -> {
        return BitcoinAddressValidator.validate(address, BitcoinAddressType.DOGE);
    }),
    //ETH("ETH", false, "ethereum:amount:label:message", R.id.ibBDX, R.id.ibBDX, R.id.ibBDX, EthAddressValidator::validate),
    LTC("LTC", true, "litecoin:amount:label:message", R.id.ibBDX, R.id.ibBDX, R.id.ibBDX, address -> {
        return BitcoinAddressValidator.validate(address, BitcoinAddressType.LTC);
    });

    @Getter
    @NonNull
    private final String symbol;
    @Getter
    private final boolean casefull;
    @NonNull
    private final String uriSpec;
    @Getter
    private final int buttonId;
    @Getter
    private final int iconEnabledId;
    @Getter
    private final int iconDisabledId;
    @NonNull
    private final Validator validator;

    Crypto(@NonNull String symbol, boolean casefull, @NonNull String uriSpec, int buttonId, int iconEnabledId, int iconDisabledId, @NonNull Validator validator) {
        this.symbol = symbol;
        this.casefull = casefull;
        this.uriSpec = uriSpec;
        this.buttonId = buttonId;
        this.iconEnabledId = iconEnabledId;
        this.iconDisabledId = iconDisabledId;
        this.validator = validator;
    }

    public String getSymbol() {
        return symbol;
    }

    public boolean isCasefull() { return casefull;}


    @Nullable
    public static Crypto withScheme(@NonNull String scheme) {
        for (Crypto crypto : values()) {
            if (crypto.getUriScheme().equals(scheme)) return crypto;
        }
        return null;
    }

    @Nullable
    public static Crypto withSymbol(@NonNull String symbol) {
        final String upperSymbol = symbol.toUpperCase();
        for (Crypto crypto : values()) {
            if (crypto.symbol.equals(upperSymbol)) return crypto;
        }
        return null;
    }

    interface Validator {
        boolean validate(String address);
    }

    // TODO maybe cache these segments
    String getUriScheme() {
        return uriSpec.split(":")[0];
    }

    String getUriAmount() {
        return uriSpec.split(":")[1];
    }

    String getUriLabel() {
        return uriSpec.split(":")[2];
    }

    String getUriMessage() {
        return uriSpec.split(":")[3];
    }

    boolean validate(String address) {
        return validator.validate(address);
    }
}
/*
* package com.thoughtcrimes.securesms.data;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.thoughtcrimes.securesms.model.Wallet;
import com.thoughtcrimes.securesms.wallet.utils.validator.BitcoinAddressType;
import com.thoughtcrimes.securesms.wallet.utils.validator.BitcoinAddressValidator;

import io.beldex.bchat.R;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Crypto {
    XMR("XMR", true, "Beldex:tx_amount:recipient_name:tx_description", R.id.ibBDX, R.id.ibBDX, R.id.ibBDX, Wallet::isAddressValid),
    BTC("BTC", true, "bitcoin:amount:label:message", R.id.ibBDX, R.id.ibBDX, R.id.ibBDX, address -> {
        Log.d("BTC ->%s", address);
        return BitcoinAddressValidator.validate(address, BitcoinAddressType.BTC);
    }),
    DASH("DASH", true, "dash:amount:label:message", R.id.ibBDX, R.id.ibBDX, R.id.ibBDX, address -> {
        return BitcoinAddressValidator.validate(address, BitcoinAddressType.DASH);
    }),
    DOGE("DOGE", true, "dogecoin:amount:label:message", R.id.ibBDX, R.id.ibBDX, R.id.ibBDX, address -> {
        return BitcoinAddressValidator.validate(address, BitcoinAddressType.DOGE);
    }),
    //ETH("ETH", false, "ethereum:amount:label:message", R.id.ibBDX, R.id.ibBDX, R.id.ibBDX, EthAddressValidator::validate),
    LTC("LTC", true, "litecoin:amount:label:message", R.id.ibBDX, R.id.ibBDX, R.id.ibBDX, address -> {
        return BitcoinAddressValidator.validate(address, BitcoinAddressType.LTC);
    });

    @Getter
    @NonNull
    private final String symbol;
    @Getter
    private final boolean casefull;
    @NonNull
    private final String uriSpec;
    @Getter
    private final int buttonId;
    @Getter
    private final int iconEnabledId;
    @Getter
    private final int iconDisabledId;
    @NonNull
    private final Validator validator;

    public String getSymbol() {
        return symbol;
    }

    public boolean isCasefull() { return casefull;}


    @Nullable
    public static Crypto withScheme(@NonNull String scheme) {
        for (Crypto crypto : values()) {
            if (crypto.getUriScheme().equals(scheme)) return crypto;
        }
        return null;
    }

    @Nullable
    public static Crypto withSymbol(@NonNull String symbol) {
        final String upperSymbol = symbol.toUpperCase();
        for (Crypto crypto : values()) {
            if (crypto.symbol.equals(upperSymbol)) return crypto;
        }
        return null;
    }

    interface Validator {
        boolean validate(String address);
    }

    // TODO maybe cache these segments
    String getUriScheme() {
        return uriSpec.split(":")[0];
    }

    String getUriAmount() {
        return uriSpec.split(":")[1];
    }

    String getUriLabel() {
        return uriSpec.split(":")[2];
    }

    String getUriMessage() {
        return uriSpec.split(":")[3];
    }

    boolean validate(String address) {
        return validator.validate(address);
    }
}

*/

