/*
 * PacProxy - A HTTP proxy driven by a PAC file.
 * Copyright (C) 2017 Moritz Horstmann
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.mh0rst.net.pacproxy;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Queue;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.littleshoot.proxy.ChainedProxy;
import org.littleshoot.proxy.ChainedProxyAdapter;
import org.littleshoot.proxy.ChainedProxyManager;
import org.littleshoot.proxy.ChainedSocksProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.handler.codec.http.HttpRequest;

/**
 * Handles the proxy resolving using a PAC file for each request.
 */
public class PacProxyManager implements ChainedProxyManager {

    private static final String DIRECT = "DIRECT";

    private static final String PROXY = "PROXY ";

    private static final String SOCKS = "SOCKS ";

    private static final String SOCKS4 = "SOCKS4 ";

    private static final String SOCKS5 = "SOCKS5 ";

    private static final Logger LOG = LoggerFactory.getLogger(PacProxyManager.class);

    private ScriptEngine pacScript;

    private boolean usePACExtensions;

    /**
     * Construct a new proxy manager using the given PAC location. This will initialize the Java Nashorn scripting
     * engine with the PAC shim (javaPacShim.js) and the PAC file.
     *
     * @param pacLocation
     * @throws IOException
     * @throws ScriptException
     */
    public PacProxyManager(String pacLocation) throws IOException, ScriptException {
        pacScript = SecureScriptEngine.newNashornEngine();
        try (Reader shim = new InputStreamReader(PacProxyManager.class.getResourceAsStream("/javaPacShim.js"),
                                                 StandardCharsets.UTF_8);
             Reader pacReader = new FileReader(pacLocation)) {
            pacScript.eval(shim);
            pacScript.eval(pacReader);
            usePACExtensions = pacScript.get("FindProxyForURLEx") != null;
        }
    }

    /**
     * This will call the standard JavaScript method FindProxyForURL for each request to be handled by the proxy.
     */
    @Override
    public void lookupChainedProxies(HttpRequest httpRequest, Queue<ChainedProxy> chainedProxies) {
        Invocable invocable = (Invocable) pacScript;
        try {
            String uri = httpRequest.uri();
            String parsedURIHost = getHostFromURI(uri);
            String host = parsedURIHost == null ? uri.split(":")[0] : parsedURIHost;
            String result = (String) invocable.invokeFunction(usePACExtensions ? "FindProxyForURLEx"
                                                                               : "FindProxyForURL",
                                                              uri, host);
            for (String proxyEntry : result.split(";")) {
                ChainedProxy proxy = fromPACString(proxyEntry.trim());
                if (proxy != null) {
                    chainedProxies.add(proxy);
                }
            }
        } catch (NoSuchMethodException | ScriptException e) {
            LOG.error("Error while executing FindProxyForURL", e);
        }
    }

    private String getHostFromURI(String uri) {
        try {
            return new URI(uri).getHost();
        } catch (URISyntaxException e) {
            LOG.trace("Not an URI", e);
        }
        return null;
    }

    private ChainedProxy fromPACString(String input) {
        if (input.isEmpty()) {
            return null;
        }
        if (DIRECT.equals(input)) {
            return ChainedProxyAdapter.FALLBACK_TO_DIRECT_CONNECTION;
        }
        boolean socks = false;
        boolean socks5 = false;
        socks5 = largerAndStartsWith(input, SOCKS5);
        socks = socks5 || largerAndStartsWith(input, SOCKS) || largerAndStartsWith(input, SOCKS4);
        if (socks || largerAndStartsWith(input, PROXY)) {
            // TODO IPv6
            String[] proxy = input.substring(input.indexOf(' ') + 1).split(":");
            if (proxy.length != 2) {
                LOG.error("Malformed proxy address value returned: " + input);
                return null;
            }
            PacProxy proxyDescription = new PacProxy(new InetSocketAddress(proxy[0], Integer.parseInt(proxy[1])));
            proxyDescription.setSocks(socks);
            proxyDescription.setSocks5(socks5);
            return proxyDescription;
        }
        LOG.error("Illegal value returned by FindProxyForURL: " + input);
        return null;
    }

    private static boolean largerAndStartsWith(String string, String prefix) {
        return string.length() > prefix.length() && string.startsWith(prefix);
    }

    private static class PacProxy extends ChainedProxyAdapter implements ChainedSocksProxy {

        private InetSocketAddress proxyAddress;

        private boolean socks;

        private boolean socks5;

        PacProxy(InetSocketAddress proxyAddress) {
            this.proxyAddress = proxyAddress;
        }

        void setSocks(boolean socks) {
            this.socks = socks;
        }

        void setSocks5(boolean socks5) {
            this.socks5 = socks5;
        }

        @Override
        public InetSocketAddress getChainedProxyAddress() {
            return proxyAddress;
        }

        @Override
        public boolean isSocksProxy() {
            return socks;
        }

        @Override
        public boolean isSocks5Proxy() {
            return socks5;
        }

        @Override
        public boolean useSocks5Resolver() {
            return true;
        }
    }

}
