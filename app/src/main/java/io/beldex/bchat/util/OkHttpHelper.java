package io.beldex.bchat.util;

import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class OkHttpHelper {
    static private OkHttpClient Singleton;

    static public OkHttpClient getOkHttpClient() {
        if (Singleton == null) {
            synchronized (OkHttpHelper.class) {
                if (Singleton == null) {
                    Singleton = new OkHttpClient();
                }
            }
        }
        return Singleton;
    }

    public static final int HTTP_TIMEOUT = 1000; //ms

    static private OkHttpClient EagerSingleton;

    static public OkHttpClient getEagerClient() {
        if (EagerSingleton == null) {
            synchronized (OkHttpHelper.class) {
                if (EagerSingleton == null) {
                    EagerSingleton = new OkHttpClient.Builder()
                            .connectTimeout(HTTP_TIMEOUT, TimeUnit.MILLISECONDS)
                            .writeTimeout(HTTP_TIMEOUT, TimeUnit.MILLISECONDS)
                            .readTimeout(HTTP_TIMEOUT, TimeUnit.MILLISECONDS)
                            .build();
                }
                /*if (EagerSingleton == null) {
                    EagerSingleton = new OkHttpClient.Builder()
                            .connectTimeout(30, TimeUnit.SECONDS)
                            .writeTimeout(30, TimeUnit.SECONDS)
                            .readTimeout(30, TimeUnit.SECONDS)
                            .build();
                }*/
            }
        }
        return EagerSingleton;
    }

    static final public String USER_AGENT = "Monerujo/1.0";

    static public Request getPostRequest(HttpUrl url, RequestBody requestBody) {
        return new Request.Builder().url(url).post(requestBody)
                .header("User-Agent", USER_AGENT)
                .build();
    }

    static public Request getGetRequest(HttpUrl url) {
        return new Request.Builder().url(url).get()
                .header("User-Agent", USER_AGENT)
                .build();
    }
}
