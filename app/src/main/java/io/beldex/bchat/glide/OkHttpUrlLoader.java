package io.beldex.bchat.glide;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import io.beldex.bchat.net.ContentProxySelector;

import io.beldex.bchat.net.ContentProxySelector;

import java.io.InputStream;

import okhttp3.OkHttpClient;

/**
 * A simple model loader for fetching media over http/https using OkHttp.
 */
public class OkHttpUrlLoader implements ModelLoader<GlideUrl, InputStream> {

  private final OkHttpClient client;

  private OkHttpUrlLoader(OkHttpClient client) {
    this.client = client;
  }

  @Override
  public @Nullable LoadData<InputStream> buildLoadData(@NonNull GlideUrl glideUrl, int width, int height, @NonNull Options options) {
    return new LoadData<>(glideUrl, new OkHttpStreamFetcher(client, glideUrl));
  }

  @Override
  public boolean handles(@NonNull GlideUrl glideUrl) {
    return true;
  }

  public static class Factory implements ModelLoaderFactory<GlideUrl, InputStream> {
    private static volatile OkHttpClient internalClient;
    private OkHttpClient client;

    private static OkHttpClient getInternalClient() {
      if (internalClient == null) {
        synchronized (Factory.class) {
          if (internalClient == null) {
            internalClient = new OkHttpClient.Builder()
                                             .proxySelector(new ContentProxySelector())
                                             .build();
          }
        }
      }
      return internalClient;
    }

    public Factory() {
      this(getInternalClient());
    }

    private Factory(OkHttpClient client) {
      this.client = client;
    }

    @Override
    public @NonNull ModelLoader<GlideUrl, InputStream> build(@NonNull MultiModelLoaderFactory multiFactory) {
      return new OkHttpUrlLoader(client);
    }

    @Override
    public void teardown() {
      // Do nothing, this instance doesn't own the client.
    }
  }
}