package com.thoughtcrimes.securesms.util;



import com.thoughtcrimes.securesms.model.NetworkType;
import com.thoughtcrimes.securesms.model.WalletManager;

import okhttp3.HttpUrl;

public class ServiceHelper {
    public static String ASSET = null;

    static public HttpUrl getXmrToBaseUrl() {
        if ((WalletManager.getInstance() == null)
                || (WalletManager.getInstance().getNetworkType() != NetworkType.NetworkType_Mainnet)) {
            throw new IllegalStateException("Only mainnet not supported");
        } else {
            return HttpUrl.parse("https://sideshift.ai/api/v1/");
        }
    }

    //by hales
   /* static public ExchangeApi getExchangeApi() {
        return new com.m2049r.xmrwallet.service.exchange.krakenEcb.ExchangeApiImpl(OkHttpHelper.getOkHttpClient());
    }*/
}
