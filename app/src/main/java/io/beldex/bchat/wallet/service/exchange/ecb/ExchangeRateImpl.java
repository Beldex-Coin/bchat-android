package io.beldex.bchat.wallet.service.exchange.ecb;

import androidx.annotation.NonNull;

import io.beldex.bchat.wallet.service.exchange.ExchangeRate;

import java.util.Date;

class ExchangeRateImpl implements ExchangeRate {
    private final Date date;
    private final String baseCurrency = "EUR";
    private final String quoteCurrency;
    private final double rate;

    @Override
    public String getServiceName() {
        return "ecb.europa.eu";
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

    ExchangeRateImpl(@NonNull final String quoteCurrency, double rate, @NonNull final Date date) {
        super();
        this.quoteCurrency = quoteCurrency;
        this.rate = rate;
        this.date = date;
    }
}

