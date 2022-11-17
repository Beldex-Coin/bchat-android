package com.thoughtcrimes.securesms.wallet.service.exchange.kraken;

import androidx.annotation.NonNull;

import com.thoughtcrimes.securesms.wallet.service.exchange.ExchangeException;
import com.thoughtcrimes.securesms.wallet.service.exchange.ExchangeRate;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class ExchangeRateImpl implements ExchangeRate {

    private final String baseCurrency;
    private final String quoteCurrency;
    private final double rate;

    @Override
    public String getServiceName() {
        return "kraken.com";
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

    ExchangeRateImpl(final JSONObject jsonObject, final boolean swapAssets) throws JSONException, ExchangeException {
        try {
            final String key = jsonObject.keys().next(); // we expect only one
            Pattern pattern = Pattern.compile("^X(.*?)Z(.*?)$");
            Matcher matcher = pattern.matcher(key);
            if (matcher.find()) {
                baseCurrency = swapAssets ? matcher.group(2) : matcher.group(1);
                quoteCurrency = swapAssets ? matcher.group(1) : matcher.group(2);
            } else {
                throw new ExchangeException("no pair returned!");
            }

            JSONObject pair = jsonObject.getJSONObject(key);
            JSONArray close = pair.getJSONArray("c");
            String closePrice = close.getString(0);
            if (closePrice != null) {
                try {
                    double rate = Double.parseDouble(closePrice);
                    this.rate = swapAssets ? (1 / rate) : rate;
                } catch (NumberFormatException ex) {
                    throw new ExchangeException(ex.getLocalizedMessage());
                }
            } else {
                throw new ExchangeException("no close price returned!");
            }
        } catch (NoSuchElementException ex) {
            throw new ExchangeException(ex.getLocalizedMessage());
        }
    }
}
