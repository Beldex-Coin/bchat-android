package io.beldex.bchat.wallet.service.exchange;

import androidx.annotation.NonNull;


public interface ExchangeApi {

    /**
     * Queries the exchnage rate
     *
     * @param baseCurrency  base currency
     * @param quoteCurrency quote currency
     * @param callback      the callback with the exchange rate
     */
    void queryExchangeRate(@NonNull final String baseCurrency, @NonNull final String quoteCurrency,
                           @NonNull final ExchangeCallback callback);

}
