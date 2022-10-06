package com.thoughtcrimes.securesms.wallet.utils.helper;

import com.thoughtcrimes.securesms.model.NetworkType;
import com.thoughtcrimes.securesms.model.WalletManager;
import com.thoughtcrimes.securesms.util.OkHttpHelper;
import com.thoughtcrimes.securesms.wallet.service.exchange.ExchangeApi;

import okhttp3.HttpUrl;

public class ServiceHelper {
    public static String ASSET = null;

    static public HttpUrl getBdxToBaseUrl() {
        if ((WalletManager.getInstance() == null)
                || (WalletManager.getInstance().getNetworkType() != NetworkType.NetworkType_Mainnet)) {
            throw new IllegalStateException("Only mainnet not supported");
        } else {
            return HttpUrl.parse("https://sideshift.ai/api/v1/");
        }
    }

    static public ExchangeApi getExchangeApi() {
        return new com.thoughtcrimes.securesms.wallet.service.exchange.krakenEcb.ExchangeApiImpl(OkHttpHelper.getOkHttpClient());
    }
}