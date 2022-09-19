package com.thoughtcrimes.securesms.wallet.service.exchange.krakenEcb;

import androidx.annotation.NonNull;

import com.thoughtcrimes.securesms.util.Helper;
import com.thoughtcrimes.securesms.wallet.service.exchange.ExchangeApi;
import com.thoughtcrimes.securesms.wallet.service.exchange.ExchangeCallback;
import com.thoughtcrimes.securesms.wallet.service.exchange.ExchangeRate;

import okhttp3.OkHttpClient;
import timber.log.Timber;

public class ExchangeApiImpl implements ExchangeApi {
    static public final String BASE_FIAT = "EUR";

    @NonNull
    private final OkHttpClient okHttpClient;

    public ExchangeApiImpl(@NonNull final OkHttpClient okHttpClient) {
        this.okHttpClient = okHttpClient;
    }

    @Override
    public void queryExchangeRate(@NonNull final String baseCurrency, @NonNull final String quoteCurrency,
                                  @NonNull final ExchangeCallback callback) {
        Timber.d("B=%s Q=%s", baseCurrency, quoteCurrency);
        if (baseCurrency.equals(quoteCurrency)) {
            Timber.d("BASE=QUOTE=1");
            callback.onSuccess(new ExchangeRateImpl(baseCurrency, quoteCurrency, 1.0));
            return;
        }

        if (!Helper.BASE_CRYPTO.equals(baseCurrency)
                && !Helper.BASE_CRYPTO.equals(quoteCurrency)) {
            callback.onError(new IllegalArgumentException("no " + Helper.BASE_CRYPTO + " specified"));
            return;
        }

        final String quote = Helper.BASE_CRYPTO.equals(baseCurrency) ? quoteCurrency : baseCurrency;

        final ExchangeApi krakenApi =
                new com.thoughtcrimes.securesms.wallet.service.exchange.kraken.ExchangeApiImpl(okHttpClient);
        krakenApi.queryExchangeRate(Helper.BASE_CRYPTO, BASE_FIAT, new ExchangeCallback() {
            @Override
            public void onSuccess(final ExchangeRate krakenRate) {
                Timber.d("kraken = %f", krakenRate.getRate());
                final ExchangeApi ecbApi =
                        new com.thoughtcrimes.securesms.wallet.service.exchange.ecb.ExchangeApiImpl(okHttpClient);
                ecbApi.queryExchangeRate(BASE_FIAT, quote, new ExchangeCallback() {
                    @Override
                    public void onSuccess(final ExchangeRate ecbRate) {
                        Timber.d("ECB = %f", ecbRate.getRate());
                        double rate = ecbRate.getRate() * krakenRate.getRate();
                        Timber.d("Q=%s QC=%s", quote, quoteCurrency);
                        if (!quote.equals(quoteCurrency)) rate = 1.0d / rate;
                        Timber.d("rate = %f", rate);
                        final ExchangeRate exchangeRate =
                                new ExchangeRateImpl(baseCurrency, quoteCurrency, rate);
                        callback.onSuccess(exchangeRate);
                    }

                    @Override
                    public void onError(Exception ex) {
                        Timber.d(ex);
                        callback.onError(ex);
                    }
                });
            }

            @Override
            public void onError(Exception ex) {
                Timber.d(ex);
                callback.onError(ex);
            }
        });
    }
}
