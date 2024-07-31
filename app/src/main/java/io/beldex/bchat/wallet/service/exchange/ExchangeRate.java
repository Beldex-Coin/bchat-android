package io.beldex.bchat.wallet.service.exchange;

public interface ExchangeRate {

    String getServiceName();

    String getBaseCurrency();

    String getQuoteCurrency();

    double getRate();

}
