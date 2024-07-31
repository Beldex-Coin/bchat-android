package io.beldex.bchat.wallet.service.exchange.krakenEcb;

import androidx.annotation.NonNull;

import io.beldex.bchat.wallet.service.exchange.ExchangeRate;

class ExchangeRateImpl implements ExchangeRate {
    private final String baseCurrency;
    private final String quoteCurrency;
    private final double rate;

    @Override
    public String getServiceName() {
        return "kraken+ecb";
    }

    @Override
    public String getBaseCurrency() {
        return baseCurrency;
    }

    @Override
    public String getQuoteCurrency() {
        return quoteCurrency;
    }

    @Override
    public double getRate() {
        return rate;
    }

    ExchangeRateImpl(@NonNull final String baseCurrency, @NonNull final String quoteCurrency, double rate) {
        super();
        this.baseCurrency = baseCurrency;
        this.quoteCurrency = quoteCurrency;
        this.rate = rate;
    }
}
