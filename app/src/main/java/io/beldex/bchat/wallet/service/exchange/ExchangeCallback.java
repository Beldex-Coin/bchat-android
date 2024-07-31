package io.beldex.bchat.wallet.service.exchange;

public interface ExchangeCallback {

    void onSuccess(ExchangeRate exchangeRate);

    void onError(Exception ex);

}