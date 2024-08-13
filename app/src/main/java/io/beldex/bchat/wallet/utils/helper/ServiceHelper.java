package io.beldex.bchat.wallet.utils.helper;

import io.beldex.bchat.wallet.service.exchange.ExchangeApi;
import io.beldex.bchat.wallet.service.exchange.krakenEcb.ExchangeApiImpl;
import io.beldex.bchat.model.NetworkType;
import io.beldex.bchat.model.WalletManager;
import io.beldex.bchat.util.OkHttpHelper;
import io.beldex.bchat.wallet.service.exchange.ExchangeApi;

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
        return new ExchangeApiImpl(OkHttpHelper.getOkHttpClient());
    }
}