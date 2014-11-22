/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 09.10.2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.core.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Enumeration;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.log.LogFactory;

/**
 * Some network utilities
 * 
 * @author Tom
 * @version $Revision$
 */
public class NetUtil {
    private static final Log LOG = LogFactory.getLog(NetUtil.class);

    private static boolean isonline;
    private static final long deltaOnlineCheck = Integer.valueOf(System
        .getProperty("netutil.delta.onlinecheck", "5000"));
    private static long lastOnlineCheck = Long.MAX_VALUE;

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
                    if (inetAddress.isAnyLocalAddress()
                        || inetAddress.isLinkLocalAddress()
                        //                        || inetAddress.isSiteLocalAddress()
                        || inetAddress.isMulticastAddress())
                        continue;
                    //TODO: how to check for VPN connections?
                    if (inetAddress.isReachable(2000))
                        return inetAddress.getHostAddress();
                }
            }
            return InetAddress.getLoopbackAddress().getHostAddress();//getInetAdress();
        } catch (Exception e) {
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

    /**
     * simply returns the response of the given url request
     * 
     * @param strUrl
     * @return content as string
     */
    public static String get(String strUrl) {
        try {
            LOG.debug("starting request: " + strUrl);
            String response = String.valueOf(FileUtil.getFileData(new URL(strUrl).openStream(), null));
            if (LOG.isDebugEnabled())
                LOG.debug("response: " + StringUtil.toString(response, 100));
            return response;
        } catch (Exception e) {
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * downloads the given strUrl if a network connection is available
     * 
     * @param strUrl network url to load
     * @param destDir local destination directory
     * @param flat if true, the file of that url will be put directly to the environment directory. otherwise the full
     *            path will be stored to the environment.
     * @param overwrite if true, existing files will be overwritten
     * @return downloaded local file
     */
    public static File download(String strUrl, String destDir, boolean flat, boolean overwrite) {
        try {
            URL url = new URL(strUrl);
            String fileName = destDir + (flat ? new File(url.getFile()).getName() : url.getFile());
            File file = new File(fileName);
            if (overwrite || !file.exists()) {
                file.getParentFile().mkdirs();
                LOG.info("downloading " + file.getName() + " from: " + url.toString());
                FileUtil.write(url.openStream(), fileName);
            }
            return file;
        } catch (Exception e) {
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * UNTESTED YET!
     * 
     * @param socket connection to send the data to
     * @param url data location
     */
    public static void upload(Socket socket, String name, String strUrl) {
        InputStream stream = null;
        try {
            URL url = new URL(strUrl);
            LOG.info("uploading " + strUrl + " to socket " + socket);
            stream = url.openStream();
            FileUtil.write(stream, socket.getOutputStream(), false);
            url.openStream().close();
        } catch (Exception e) {
            ManagedException.forward(e);
        } finally {
            if (stream != null)
                try {
                    stream.close();
                } catch (IOException e) {
                    ManagedException.forward(e);
                }
        }
    }

    /**
     * hasNetworkConnection
     * 
     * @return true, if this system is connected to a network
     */
    public static final boolean isOnline() {
        if (lastOnlineCheck - System.currentTimeMillis() > deltaOnlineCheck) {
            lastOnlineCheck = System.currentTimeMillis();
            isonline = !getMyIP().equals(InetAddress.getLoopbackAddress().getHostAddress());
        }
        return isonline;
    }

    /**
     * this methods provide a free port - but no guarantee is given that this port will be opened by another task right
     * now. if you need a server-socket, call new ServerSocket(0) instead (see {@link ServerSocket#ServerSocket(int)}).
     * 
     * @return serversocket instance
     */
    public static int getFreePort() {
        try {
            ServerSocket s = new ServerSocket(0);
            int port = s.getLocalPort();
            s.close();
            return port;
        } catch (IOException e) {
            ManagedException.forward(e);
            return -1;
        }

    }
}
