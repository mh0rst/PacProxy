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

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Enumeration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;

/**
 * Static implementations of common PAC JavaScript functions, to be called from Nashorn scripting engine.
 */
public class PacFunctions {

    private static final Logger LOG = LoggerFactory.getLogger(PacFunctions.class);

    /**
     * Prints given object to the log.
     * 
     * @param object
     */
    public static void log(Object object) {
        LOG.info(object.toString());
    }

    /**
     * Returns true if given host's domain equals the given domain.
     * 
     * @param host
     * @param domain
     */
    public static boolean dnsDomainIs(String host, String domain) {
        return host != null && domain != null && host.length() >= domain.length() && host.endsWith(domain);
    }

    /**
     * Returns the number of domain levels in the given host (number of dots).
     * 
     * @param host
     */
    public static int dnsDomainLevels(String host) {
        if (host == null) {
            return 0;
        }
        int count = 0;
        int i = -1;
        do {
            i = host.indexOf('.', i + 1);
            if (i != -1) {
                count++;
            }
        } while (i != -1);
        return count;
    }

    /**
     * Resolves the given host name and returns the IP address. An empty string is returned if the resolution fails.
     * 
     * @param host
     */
    public static String dnsResolve(String host) {
        if (host == null) {
            return "";
        }
        try {
            return InetAddress.getByName(host).getHostAddress();
        } catch (UnknownHostException e) {
            LOG.debug("Could not resolve " + host, e);
            return "";
        }
    }

    /**
     * Returns true if the given host name is a plain host name (does not contain any dots).
     * 
     * @param host
     */
    public static boolean isPlainHostName(String host) {
        return host != null && host.indexOf('.') == -1;
    }

    /**
     * Returns true if the given host name is resolvable.
     * 
     * @param host
     */
    public static boolean isResolvable(String host) {
        if (nullOrEmpty(host)) {
            return false;
        }
        try {
            InetAddress.getByName(host);
            return true;
        } catch (UnknownHostException e) {
            LOG.debug("Could not resolve " + host, e);
            return false;
        }
    }

    /**
     * Returns true if the resolved IPv4 address of the given host name is part of the subnet denoted by the pattern and
     * the mask.
     * 
     * @param host A host name or IP address
     * @param pattern A subnet pattern, e.g. 10.1.2.0
     * @param mask A mask for the subnet, e.g. 255.255.255.0
     */
    public static boolean isInNet(String host, String pattern, String mask) {
        if (nullOrEmpty(host) || nullOrEmpty(pattern) || nullOrEmpty(mask)) {
            return false;
        }
        try {
            SubnetPattern subnet = new SubnetPattern(InetAddress.getByName(pattern).getAddress(),
                                                     InetAddress.getByName(mask).getAddress());
            return subnet.isInMask(InetAddress.getByName(host).getAddress());
        } catch (UnknownHostException e) {
            LOG.debug("Could not resolve " + host, e);
            return false;
        }
    }

    /**
     * Returns true if the given host is equal to the given host domain, or if the host is the first part of the given
     * host domain.
     * 
     * @param host
     * @param hostdom
     */
    public static boolean localHostOrDomainIs(String host, String hostdom) {
        return Objects.equal(host, hostdom) || hostdom != null && hostdom.startsWith(host + ".");
    }

    /**
     * Attempts to detect the IPv4 address of the machine running this proxy.
     */
    public static String myIpAddress() {
        String address = null;
        try {
            address = InetAddress.getLocalHost().getHostAddress();
            if (!address.startsWith("127")) {
                return address;
            }
        } catch (UnknownHostException e) {
            LOG.debug("Could not resolve local hostname", e);
        }
        try {
            for (NetworkInterface iface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (iface.isLoopback()) {
                    continue;
                }
                Enumeration<InetAddress> inetAddresses = iface.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress ifaceAddress = inetAddresses.nextElement();
                    if (!ifaceAddress.isLinkLocalAddress() && !(ifaceAddress instanceof Inet6Address)) {
                        return ifaceAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            LOG.warn("Could not resolve own IP address", e);
        }
        return address == null ? "" : address;
    }

    /**
     * Attempts to detect all IP addresses of the machine running this proxy.
     */
    public static String myIpAddressEx() {
        StringBuilder addresses = new StringBuilder();
        try {
            for (NetworkInterface iface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (iface.isLoopback()) {
                    continue;
                }
                Enumeration<InetAddress> inetAddresses = iface.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress ifaceAddress = inetAddresses.nextElement();
                    if (!ifaceAddress.isLinkLocalAddress()) {
                        if (addresses.length() > 0) {
                            addresses.append(';');
                        }
                        addresses.append(ifaceAddress.getHostAddress());
                    }
                }
            }
        } catch (SocketException e) {
            LOG.warn("Could not resolve own IP address", e);
        }
        return addresses.toString();
    }

    /**
     * Returns true if given string matches the shell expression.
     * 
     * @param str
     * @param shexp An expression with the wildcards ? (one arbitrary character) and * (any amount of arbitrary
     *            characters).
     */
    public static boolean shExpMatch(String str, String shexp) {
        return str != null && shexp != null &&
               str.matches(shexp.replace(".", "\\.").replace("?", ".?").replace("*", ".*"));
    }

    private static boolean nullOrEmpty(String host) {
        return host == null || host.isEmpty();
    }

}
