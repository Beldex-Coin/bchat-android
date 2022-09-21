package com.thoughtcrimes.securesms.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


import io.beldex.bchat.R;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import timber.log.Timber;

@RequiredArgsConstructor
public enum Crypto {
    //by hales
    /*XMR("XMR", true, "Beldex:tx_amount:recipient_name:tx_description", R.id.ibXMR, R.drawable.ic_monero, R.drawable.ic_monero_bw, Wallet::isAddressValid),
    BTC("BTC", true, "bitcoin:amount:label:message", R.id.ibBTC, R.drawable.ic_xmrto_btc, R.drawable.ic_xmrto_btc_off, address -> {
        Timber.d("BTC ->%s", address);
        return BitcoinAddressValidator.validate(address, BitcoinAddressType.BTC);
    }),
    DASH("DASH", true, "dash:amount:label:message", R.id.ibDASH, R.drawable.ic_xmrto_dash, R.drawable.ic_xmrto_dash_off, address -> {
        return BitcoinAddressValidator.validate(address, BitcoinAddressType.DASH);
    }),
    DOGE("DOGE", true, "dogecoin:amount:label:message", R.id.ibDOGE, R.drawable.ic_xmrto_doge, R.drawable.ic_xmrto_doge_off, address -> {
        return BitcoinAddressValidator.validate(address, BitcoinAddressType.DOGE);
    }),
    ETH("ETH", false, "ethereum:amount:label:message", R.id.ibETH, R.drawable.ic_xmrto_eth, R.drawable.ic_xmrto_eth_off, EthAddressValidator::validate),
    LTC("LTC", true, "litecoin:amount:label:message", R.id.ibLTC, R.drawable.ic_xmrto_ltc, R.drawable.ic_xmrto_ltc_off, address -> {
        return BitcoinAddressValidator.validate(address, BitcoinAddressType.LTC);
    });*/
    ;

   /* @Getter
    @NonNull
    private final String symbol;*/
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
        /*this.symbol = symbol;*/
        this.casefull = casefull;
        this.uriSpec = uriSpec;
        this.buttonId = buttonId;
        this.iconEnabledId = iconEnabledId;
        this.iconDisabledId = iconDisabledId;
        this.validator = validator;
    }

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
           /* if (crypto.symbol.equals(upperSymbol)) return crypto;*/
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

    public int getButtonId() {
        return buttonId;
    }

    boolean validate(String address) {
        return validator.validate(address);
    }
}
