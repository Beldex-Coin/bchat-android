package io.beldex.bchat.util;



import io.beldex.bchat.model.NetworkType;
import io.beldex.bchat.model.WalletManager;

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
}
