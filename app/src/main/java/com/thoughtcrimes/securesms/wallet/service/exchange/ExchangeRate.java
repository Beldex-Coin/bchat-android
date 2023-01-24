package com.thoughtcrimes.securesms.wallet.service.exchange;

public interface ExchangeRate {

    String getServiceName();

    String getBaseCurrency();

    String getQuoteCurrency();

    double getRate();

}
