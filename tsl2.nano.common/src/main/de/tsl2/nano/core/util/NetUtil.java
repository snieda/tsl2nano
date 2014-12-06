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
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

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
    static final String URL_STANDARDFILENAME = "index.html";

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
            String response = String.valueOf(FileUtil.getFileData(url(strUrl).openStream(), null));
            if (LOG.isDebugEnabled())
                LOG.debug("response: " + StringUtil.toString(response, 100));
            return response;
        } catch (Exception e) {
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * getURLStream
     * 
     * @param url
     * @return
     */
    public static InputStream getURLStream(String url) {
        try {
            return URI.create(url).toURL().openStream();
        } catch (Exception e) {
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * delegates ot {@link #download(URL, String, boolean, boolean)}
     */
    public static File download(String strUrl, String destDir, boolean flat, boolean overwrite) {
        return download(url(strUrl), destDir, flat, overwrite);
    }

    public static final URL url(String surl) {
        return url(surl, null);
    }

    /**
     * convenience to get an url object. if no scheme/protocol is given, 'http://' will be used!
     * 
     * @param surl
     * @param parent (optional) parent of surl (like 'http://' or 'http://my.website.com/')
     * @return URL object through surl
     */
    public static final URL url(String surl, String parent) {
        URI uri = URI.create(surl);
        try {
            if (surl.startsWith("//"))
                return new URL("http:" + surl);
            if (uri.getScheme() == null) {
                if (parent == null || uri.getHost() != null)
                    parent = "http://";
                else {
                    if (URI.create(parent).getScheme() == null)
                        parent = "http://" + parent;
                }
                if (parent.endsWith("/") && surl.startsWith("/"))
                    surl = surl.substring(1);
                return new URL(parent + surl);
            } else {
                return uri.toURL();
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * downloads the given strUrl if a network connection is available
     * 
     * @param url network url to load
     * @param destDir local destination directory
     * @param flat if true, the file of that url will be put directly to the environment directory. otherwise the full
     *            path will be stored to the environment.
     * @param overwrite if true, existing files will be overwritten
     * @return downloaded local file
     */
    public static File download(URL url, String destDir, boolean flat, boolean overwrite) {
        try {
            String fileName = flat ? new File(url.getFile()).getName() : getFileName(url);
            fileName = (destDir.endsWith("/") ? destDir : destDir + "/") + fileName;
            File file = new File(fileName);
            if (overwrite || !file.exists()) {
                file.getParentFile().mkdirs();
                LOG.info("downloading " + file.getName() + " from: " + url.toString());
                FileUtil.write(url.openStream(), fileName);
            }
            return file;
        } catch (Exception e) {
            throw ManagedException.toRuntimeEx(e, false, false);
        }
    }

    static String getFileName(URL url) {
        String f = url.getFile();
        return Util.isEmpty(f) ? NetUtil.URL_STANDARDFILENAME : FileUtil.getValidPathName(f);
    }

    public static void wcopy(String url, String dir, String include, String exclude) {
        LOG.info("==> starting downloading site: " + url + " to directory: " + dir);
        new WCopy(url).get(url, dir, include, exclude, false);
        LOG.info("<== finished downloading site: " + url + " to directory: " + dir);
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

    /**
     * NOT IMPLEMENTED YET! getNetworkHosts
     * 
     * @return
     */
    static List<InetAddress> getNetworkHosts() {
        //TODO: implement
        LinkedList<InetAddress> list = new LinkedList<InetAddress>();
        try {
            list.add(InetAddress.getLocalHost());
        } catch (UnknownHostException e) {
            ManagedException.forward(e);
        }
        return list;
    }

    /**
     * delegates to {@link #scan(int, int, InetAddress...)} for one ip and one port. returns true, if given socket is
     * connectable.
     */
    public static boolean isOpen(InetAddress ip, int port) {
        return scan(ip, port, port).get(new InetSocketAddress(ip, port)).equals(Boolean.TRUE);
    }

    /**
     * delegates to {@link #scan(int, int, InetAddress...)} for the given ip.
     */
    public static Map<InetSocketAddress, Boolean> scan(InetAddress ip, int minPort, int maxPort) {
        return scan(minPort, maxPort, ip);
    }

    public static Map<InetSocketAddress, Boolean> scans(int minPort, int maxPort, String... ips) {
        InetAddress[] nips = new InetAddress[ips.length];
        for (int i = 0; i < ips.length; i++) {
            try {
                nips[i] = InetAddress.getByName(ips[i]);
            } catch (UnknownHostException e) {
                ManagedException.forward(e);
            }
        }
        return scan(minPort, maxPort, nips);
    }

    /**
     * port scan
     * 
     * @param minPort minimum port number
     * @param maxPort maximum port number
     * @param ips network hosts to scan
     */
    public static Map<InetSocketAddress, Boolean> scan(int minPort, int maxPort, InetAddress... ips) {
        if (ips.length == 0) {
            ips = getNetworkHosts().toArray(new InetAddress[0]);
        }
        Properties props =
            FileUtil.loadProperties(
                NetUtil.class.getPackage().getName().replace(".", "/") + "/networkports.properties", null);

        int timeout = 200;
        Worker<InetSocketAddress, Boolean> worker =
            ConcurrentUtil
                .createParallelWorker("portscan", Thread.MIN_PRIORITY, InetSocketAddress.class, Boolean.class);
        Map<InetSocketAddress, Boolean> result = worker.getResult();
        for (int i = 0; i < ips.length; i++) {
            for (int p = minPort; p < maxPort; p++) {
                worker.run(new PortScan(new InetSocketAddress(ips[i], p), timeout, result));
            }
        }
        result = worker.waitForJobs(timeout);
        StringBuilder buf = new StringBuilder(result.size() * 30);
        buf.append("========== network-port-scanning finished ================\n");
        buf.append("open ports:\n");
        Set<InetSocketAddress> ns = result.keySet();
        for (InetSocketAddress a : ns) {
            if (Boolean.TRUE.equals(result.get(a)))
                buf.append(a + "\t: " + props.getProperty(String.valueOf(a.getPort())));
        }
        LOG.info(buf);
        return result;
    }
}

class PortScan implements Runnable {
    private static final Log LOG = LogFactory.getLog(PortScan.class);
    InetSocketAddress socketAddress;
    int timeout;
    boolean isOpen;
    Map<InetSocketAddress, Boolean> result;

    public PortScan(InetSocketAddress socketAddress, Map<InetSocketAddress, Boolean> result) {
        this(socketAddress, 200, result);
    }

    /**
     * constructor
     * 
     * @param result
     * @param ip
     * @param port
     */
    public PortScan(InetSocketAddress socketAddress, int timeout, Map<InetSocketAddress, Boolean> result) {
        super();
        this.socketAddress = socketAddress;
        this.timeout = timeout;
        this.result = result;
    }

    @Override
    public void run() {
        try {
            Socket socket = new Socket();
            socket.connect(socketAddress, timeout);
            socket.close();
            result.put(socketAddress, true);
            LOG.info(socketAddress.getHostString() + "(" + socketAddress.getHostName() + "):" + socketAddress.getPort()
                + " is open");
        } catch (Exception ex) {
            //Ok, not open
            result.put(socketAddress, false);
        }
    }
}

class WCopy {
    private static final Log LOG = LogFactory.getLog(WCopy.class);
    URL site;
    List<URL> requestedURLs;

    /**
     * constructor
     * 
     * @param url
     */
    public WCopy(String url) {
        super();
        this.site = NetUtil.url(url);
        requestedURLs = new LinkedList<URL>();
    }

    public void get(String url, String destDir, String include, String exclude, boolean overwrite) {
        get(NetUtil.url(url), destDir, include, exclude, overwrite);
    }

    public void get(URL url, String destDir, String include, String exclude, boolean overwrite) {
        File file;
        try {
            file = NetUtil.download(url, destDir, false, !requestedURLs.contains(url) && overwrite);
        } catch (Exception e) {
            LOG.error(e.toString());
            return;
        }
        if (requestedURLs.contains(url))
            return;
        URL[] urls = evaluateUrls(file, site);
        requestedURLs.add(url);
        for (URL u : urls) {
            if (isLocal(u) && (include == null || u.getPath().matches(include))
                && (exclude == null || !u.getPath().matches(exclude))) {
                get(u.toString(), destDir, include, exclude, overwrite);
            }
        }
    }

    private boolean isLocal(URL u) {
        return site.getHost().equals(u.getHost());
    }

    private static URL[] evaluateUrls(File file, URL site) {
        char[] data = FileUtil.getFileData(file.getPath(), null);
        Collection<URL> urls = new LinkedList<URL>();
        StringBuilder txt = new StringBuilder(String.valueOf(data));
        StringBuilder buf = new StringBuilder(file.getPath() + ":");
        URL url = null;
        String u;
        int index = 0;
        while (index < txt.length() && (!Util.isEmpty(u =
            StringUtil.extract(txt, "(?:(href|src|content)\\s*\\=\\s*\")(.)+(?:\")", null, index)))) {
            u = StringUtil.substring(u, "\"", "\"");
            index = txt.indexOf(u, index) + u.length();
            try {
                url = NetUtil.url(u, site.getHost());
                urls.add(url);
                if (LOG.isDebugEnabled())
                    buf.append("\n\t--> " + url);
                //replace url with a local file name
                StringUtil.replace(txt, u, NetUtil.getFileName(url));
            } catch (Exception e) {
                LOG.debug(e.toString());
                index += 4;
            }
        }
        if (LOG.isDebugEnabled())
            LOG.debug(buf.toString());
        return urls.toArray(new URL[0]);
    }

}