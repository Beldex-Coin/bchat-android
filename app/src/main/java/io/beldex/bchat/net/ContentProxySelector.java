package io.beldex.bchat.net;


import com.beldex.libsignal.utilities.Log;

import io.beldex.bchat.BuildConfig;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ContentProxySelector extends ProxySelector {

  private static final String TAG = ContentProxySelector.class.getSimpleName();

  private static final Set<String> WHITELISTED_DOMAINS = new HashSet<>();
  static {
    WHITELISTED_DOMAINS.add("giphy.com");
  }

  private final List<Proxy> CONTENT = new ArrayList<Proxy>(1) {{
    add(new Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved(BuildConfig.CONTENT_PROXY_HOST,
                                                                      BuildConfig.CONTENT_PROXY_PORT)));
  }};

  @Override
  public List<Proxy> select(URI uri) {
    for (String domain : WHITELISTED_DOMAINS) {
      if (uri.getHost().endsWith(domain)) {
        return CONTENT;
      }
    }
    throw new IllegalArgumentException("Tried to proxy a non-whitelisted domain.");
  }

  @Override
  public void connectFailed(URI uri, SocketAddress address, IOException failure) {
    if (failure instanceof SocketException) {
      Log.d(TAG, "Socket exception. Likely a cancellation.");
    } else {
      Log.w(TAG, "Connection failed.", failure);
    }
  }
}
