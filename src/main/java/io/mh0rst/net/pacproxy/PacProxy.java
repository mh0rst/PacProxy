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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.script.ScriptException;

import org.littleshoot.proxy.HttpProxyServerBootstrap;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PacProxy main class
 */
public class PacProxy {

    private static final Logger LOG = LoggerFactory.getLogger(PacProxy.class);

    private String bindHost = "127.0.0.1";

    private int bindPort = 3128;

    private String pacLocation;

    /**
     * Starts PacProxy.
     * 
     * @param args call with -h to see options.
     */
    public static void main(String[] args) {
        PacProxy proxy = new PacProxy();
        proxy.parseArguments(args);
        proxy.run();
    }

    private void run() {
        try {
            HttpProxyServerBootstrap proxy = DefaultHttpProxyServer.bootstrap();

            if (pacLocation == null || !Files.exists(Paths.get(pacLocation)) ||
                !Files.isReadable(Paths.get(pacLocation))) {
                LOG.error("PAC file not specified or not readable: " + pacLocation);
                return;
            } else {
                PacProxyManager pcm = new PacProxyManager(pacLocation);
                proxy.withChainProxyManager(pcm);
                LOG.info("Loaded PAC file " + pacLocation);
            }

            proxy.withAddress(new InetSocketAddress(bindHost, bindPort));
            LOG.info("Binding proxy to " + bindHost + ":" + bindPort);

            proxy.start();
            LOG.info("PacProxy started successfully");
        } catch (IOException | ScriptException e) {
            LOG.error("Could not load PAC file " + pacLocation, e);
        }
    }

    private void parseArguments(String[] args) {
        if (args.length == 0) {
            printHelp();
            System.exit(0);
        }
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
            case "-b":
                checkArgumentPresent(args, ++i, "-b");
                String[] bind = checkBinding(args[i]);
                bindHost = bind[0];
                bindPort = Integer.parseInt(bind[1]);
                break;
            case "-h":
                printHelp();
                break;
            case "-p":
                checkArgumentPresent(args, ++i, "-p");
                pacLocation = args[i];
                break;
            default:
                throw new IllegalArgumentException("Unsupported option " + args[i]);
            }
        }
    }

    private String[] checkBinding(String binding) {
        String bind[] = binding.split(":");
        if (bind.length != 2) {
            throw new IllegalArgumentException("Binding parameter must match format HOST/IP:PORT, e.g. localhost:3128");
        }
        return bind;
    }

    private void checkArgumentPresent(String[] args, int atPosition, String option) {
        if (atPosition >= args.length) {
            throw new IllegalArgumentException("Option " + option + " requires a parameter");
        }
    }

    private void printHelp() {
        System.out.println("PacProxy: A HTTP proxy driven by a PAC file.");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -b <HOST/IP:PORT> (Binds the proxy port to given interface address and port, default 127.0.0.1:3128)");
        System.out.println("  -h (Displays this text)");
        System.out.println("  -p <PACFILE> (Location of the PAC file)");
    }
}
