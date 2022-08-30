package com.yugabyte.app.messenger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.Proxy.Type;
import java.util.Collections;
import java.util.List;

public class DatabaseProxySelector extends ProxySelector {

    private String proxyHost;
    private int proxyPort;

    public DatabaseProxySelector(String proxyHost, int proxyPort) {
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
    }

    @Override
    public List<Proxy> select(URI uri) {
        // YugabyteDB Managed host always ends with `ybdb.io`
        if (uri.toString().contains("ybdb.io")) {
            System.out.println("Using the proxy for YugabyteDB Managed: " + uri);

            final InetSocketAddress proxyAddress = InetSocketAddress
                    .createUnresolved(proxyHost, proxyPort);
            return Collections.singletonList(new Proxy(Type.SOCKS, proxyAddress));
        }

        return Collections.singletonList(Proxy.NO_PROXY);
    }

    @Override
    public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
        new IOException("Failed to connect to the proxy", ioe).printStackTrace();
    }

}
