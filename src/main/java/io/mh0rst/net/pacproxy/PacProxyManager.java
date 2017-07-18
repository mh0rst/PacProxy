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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.handler.codec.http.HttpRequest;

/**
 * Handles the proxy resolving using a PAC file for each request.
 */
public class PacProxyManager implements ChainedProxyManager {

    private static final String DIRECT = "DIRECT";

    private static final String PROXY = "PROXY ";

    private static final Logger LOG = LoggerFactory.getLogger(PacProxyManager.class);

    private ScriptEngine pacScript;

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
        }
    }

    /**
     * This will call the standard JavaScript method FindProxyForURL for each request to be handled by the proxy.
     */
    @Override
    public void lookupChainedProxies(HttpRequest httpRequest, Queue<ChainedProxy> chainedProxies) {
        Invocable invocable = (Invocable) pacScript;
        try {
            String uri = httpRequest.getUri();
            String parsedURIHost = getHostFromURI(uri);
            String host = parsedURIHost == null ? uri.split(":")[0] : parsedURIHost;
            String result = (String) invocable.invokeFunction("FindProxyForURL", uri, host);
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
        if (input.length() > PROXY.length() && input.startsWith(PROXY)) {
            // TODO IPv6
            String[] proxy = input.substring(PROXY.length()).split(":");
            if (proxy.length != 2) {
                LOG.error("Malformed PROXY value returned: " + input);
                return null;
            }
            return new PacProxy(new InetSocketAddress(proxy[0], Integer.parseInt(proxy[1])));
        }
        // TODO support SOCKS
        LOG.error("Illegal value returned by FindProxyForURL: " + input);
        return null;
    }

    private static class PacProxy extends ChainedProxyAdapter {

        private InetSocketAddress proxyAddress;

        public PacProxy(InetSocketAddress proxyAddress) {
            this.proxyAddress = proxyAddress;
        }

        @Override
        public InetSocketAddress getChainedProxyAddress() {
            return proxyAddress;
        }
    }

}
