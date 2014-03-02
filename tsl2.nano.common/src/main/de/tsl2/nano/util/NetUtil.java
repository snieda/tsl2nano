/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 09.10.2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

import de.tsl2.nano.exception.ManagedException;

/**
 * Some net utilities
 * 
 * @author Tom
 * @version $Revision$
 */
public class NetUtil {

    /**
     * getMyIPAdress
     * 
     * @return result of {@link InetAddress#getLocalHost()}. if you have more than one network interface, it may not be
     *         the desired one.
     */
    public static String getInetAdress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * getMyIPAdress
     * 
     * @return
     */
    public static String getMyIP() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) networkInterfaces.nextElement();
                if (!ni.isUp() || ni.isVirtual() || ni.isLoopback())
                    continue;
                Enumeration<InetAddress> inetAdresses = ni.getInetAddresses();
                while (inetAdresses.hasMoreElements()) {
                    InetAddress inetAddress = inetAdresses.nextElement();
                    if (inetAddress.isAnyLocalAddress() || inetAddress.isLinkLocalAddress() || inetAddress.isMulticastAddress())
                        continue;
                        return inetAddress.getHostAddress();
                }
            }
            return getInetAdress();
        } catch (SocketException e) {
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * Any address in the range 127.xxx.xxx.xxx is a "loopback" address. It is only visible to "this" host. Any address
     * in the range 192.168.xxx.xxx is a private IP address. These are reserved for use within an organization. The same
     * applies to 10.xxx.xxx.xxx addresses, and 172.16.xxx.xxx through 172.31.xxx.xxx. Addresses in the range
     * 224.xxx.xxx.xxx through 239.xxx.xxx.xxx are multicast addresses. The address 255.255.255.255 is the broadcast
     * address. Anything else should be a valid public point-to-point IPv4 address.
     */
    public static String getNetInfo() {
//        try {
//            return StringUtil.toFormattedString(InetAddress.getAllByName(null), 100, true);
//        } catch (UnknownHostException e) {
//            ManagedException.forward(e);
//            return null;
//        }
        try {
            StringBuilder str = new StringBuilder();
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) networkInterfaces.nextElement();
                Enumeration<InetAddress> inetAdresses = ni.getInetAddresses();
                if (inetAdresses.hasMoreElements()) {
                    str.append(ni.getDisplayName() + "\n");
                    while (inetAdresses.hasMoreElements()) {
                        str.append("  " + inetAdresses.nextElement().getHostAddress() + "\n");
                    }
                }
            }
            return str.toString();
        } catch (SocketException e) {
            ManagedException.forward(e);
            return null;
        }
    }
}
