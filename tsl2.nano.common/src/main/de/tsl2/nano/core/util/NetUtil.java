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

import static de.tsl2.nano.core.util.ByteUtil.serialize;
import static de.tsl2.nano.core.util.ByteUtil.toByteArray;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
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

import de.tsl2.nano.bean.BeanUtil;
import de.tsl2.nano.collection.MapUtil;
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
    public static InetAddress getInetAddress() {
        try {
            return InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * getMyIPAdress
     * 
     * @return result of {@link InetAddress#getLocalHost()}. if you have more than one network interface, it may not be
     *         the desired one.
     */
    public static String getLocalhost() {
        return getInetAddress().getHostAddress();
    }

    public static String getMyIP() {
        return getMyAddress().getHostAddress();
    }

    /**
     * getMyIPAdress
     * 
     * @return
     */
    public static InetAddress getMyAddress() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface ni = networkInterfaces.nextElement();
                if (!ni.isUp() || ni.isVirtual() || ni.isLoopback()) {
                    continue;
                }
                Enumeration<InetAddress> inetAdresses = ni.getInetAddresses();
                while (inetAdresses.hasMoreElements()) {
                    InetAddress inetAddress = inetAdresses.nextElement();
                    if (inetAddress.isAnyLocalAddress()
                        || inetAddress.isLinkLocalAddress()
                        //                        || inetAddress.isSiteLocalAddress()
                        || inetAddress.isMulticastAddress()) {
                        continue;
                    }
                    //TODO: how to check for VPN connections?
                    if (inetAddress.isReachable(2000)) {
                        return inetAddress;
                    }
                }
            }
            return InetAddress.getLoopbackAddress();//getInetAdress();
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
                NetworkInterface ni = networkInterfaces.nextElement();
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
            if (LOG.isDebugEnabled()) {
                LOG.debug("response: " + StringUtil.toString(response, 100));
            }
            return response;
        } catch (Exception e) {
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * gets the URL content through a call to {@link #get(String)} and removes all xml-tags to have pure text.
     * 
     * @param strUrl url to load
     * @param out prints out pure text of url
     */
    public static void browse(String strUrl, PrintStream out) {
        out.println(StringUtil.removeXMLTags(get(strUrl)));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static/*<T> T*/String getRestful(String url/*, Class<T> responseType*/, Map args) {
        return getRestful(url, MapUtil.asArray(args));
    }

    /**
     * simply returns the response of the given url + args restful request
     * 
     * @param url
     * @param responseType type of response object
     * @param args key/value pairs to be appended as rest-ful call to the url
     * @return content as object of type responseType
     */
    public static/*<T> T*/String getRestful(String url/*, Class<T> responseType*/, Object... args) {
        try {
            //create the rest-ful call
            StringBuilder buf = new StringBuilder(url);
            for (int i = 0; i < args.length; i++) {
                buf.append("/" + args[i]);
            }
            url = buf.toString();

            LOG.debug("starting request: " + url);
            char[] response = FileUtil.getFileData(url(url).openStream(), null);
            if (LOG.isDebugEnabled()) {
                LOG.debug("response: " + StringUtil.toString(response, 100));
            }
            return /*(T)*/String.valueOf(response);
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
    public static File download(String strUrl, String destDir) {
        return download(url(strUrl), destDir, false, true);
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
            if (surl.startsWith("//")) {
                return new URL("http:" + surl);
            }
            if (uri.getScheme() == null) {
                if (parent == null || uri.getHost() != null) {
                    parent = "http://";
                } else {
                    if (URI.create(parent).getScheme() == null) {
                        parent = "http://" + parent;
                    }
                }
                if (parent.endsWith("/") && surl.startsWith("/")) {
                    surl = surl.substring(1);
                }
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
     * @param destDir local destination directory. if null, user.dir will be used
     * @param flat if true, the file of that url will be put directly to the environment directory. otherwise the full
     *            path will be stored to the environment.
     * @param overwrite if true, existing files will be overwritten
     * @return downloaded local file
     */
    public static File download(URL url, String destDir, boolean flat, boolean overwrite) {
        try {
            String fileName = flat ? new File(FileUtil.getValidFileName(url.getFile())).getName() : getFileName(url);
            destDir = destDir == null ? System.getProperty("user.dir") : destDir;
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
        return Util.isEmpty(f) || f.equals("/") ? NetUtil.URL_STANDARDFILENAME : FileUtil.getValidPathName(f);
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
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    ManagedException.forward(e);
                }
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
            if (isonline) {//check, whether most important site is available
                try {//isReachable(2000) fails...
                    isonline = InetAddress.getByName("www.google.com") != null;//.isReachable(2000);
                } catch (Exception e) {
                    isonline = false;
                }
            }
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

//    public static String gateway() {
//        try (DatagramSocket s = new DatagramSocket()) {
//          s.connect(InetAddress.getByAddress(new byte[] { 1, 1, 1, 1 }), 0);
//          return new String(NetworkInterface.getByInetAddress(s.getLocalAddress()).getHardwareAddress());
//        } catch (Exception e) {
//          e.printStackTrace();
//          return null;
//        }
//      }

    public static Proxy proxy(String testURI) {
        return proxy(testURI, null, null, null);
    }

    public static Proxy proxy(String testURI, String newProxy) {
        return proxy(testURI, newProxy, null, null);
    }

    /**
     * @param testURI uri to evaluate the protocol from
     * @param newProxy (optional) new proxy configuration to be set.
     * @param user (optional) user for new proxy configuration
     * @param passwd (optional) password for new proxy configuration
     * @return last proxy for the given uri protocol
     */
    public static Proxy proxy(String testURI, String newProxy, String user, String passwd) {
        String host = null;
        int port = -1;
        if (newProxy != null) {
            String proxy[] = newProxy.split("\\:");
            host = proxy[0];
            if (proxy.length > 1)
                port = Integer.valueOf(proxy[1]);
        }
        return proxy(testURI, host, port, user, passwd);
    }

    /**
     * logs some proxy informations and let you define a proxy to be set to the system properties and to be usable on
     * connections.
     * 
     * @see "Web Proxy Autodiscovery Protocol" and "Proxy-Auto-Config-(PAC)-Standard". Try to look at
     *      http://wpad/wpad.dat or http://wpad.com/wpad.dat.
     * 
     * @param testURI uri to evaluate the protocol from. may be http, https, ftp or socket. e.g.: http://foo.bar.
     * @param newProxy (optional) new proxy configuration to be set.
     * @param user (optional) user for new proxy configuration
     * @param passwd (optional) password for new proxy configuration
     * @return last or new proxy for the given uri protocol, or null on errors or not available
     */
    public static Proxy proxy(String testURI, String newProxyHost, int port, String user, String passwd) {
        String protocol = testURI.split("\\:\\/\\/")[0] + ".";
        LOG.info("to see the organisations automatic proxy definitions, open the 'Proxy-Auto-Config-(PAC)-Standard' file, mostly http://wpad/wpad.dat or http://wpad.com/wpad.dat (-->'Web Proxy Autodiscovery Protocol') in your browser!");
        LOG.info("current system properties: {" + getSystem(protocol + "proxyHost") + getSystem(protocol + "proxyPort")
            + getSystem(protocol + "proxyUser") + getSystem(protocol + "proxyPassword"));
        LOG.info("}");

        LOG.info("detecting current proxies on " + testURI + ":");
        try {
            StringBuilder buf = new StringBuilder();
            List<Proxy> protProxies = ProxySelector.getDefault().select(new URI(testURI));
            Proxy currentProxy = null;
            if (protProxies != null) {
                for (Proxy proxy : protProxies) {
                    buf.append("\t - PROXY type: " + proxy.type());
                    InetSocketAddress addr = (InetSocketAddress) proxy.address();

                    if (addr == null) {
                        buf.append(" (No Proxy)");
                    } else {
                        buf.append("hostName: " + addr.getHostName() + "\n\t http.proxyPort = " + addr.getPort());
                        //last proxy wins
                        currentProxy = proxy;
                    }
                    buf.append("\n");
                }
            }

            if (newProxyHost != null && port != -1) {
                buf.append("\nsetting new system properties:");
                setSystem("java.net.useSystemProxies", "true", buf);
                setSystem(protocol + "proxySet", "true", buf);
                setSystem(protocol + "proxyHost", newProxyHost, buf);
                setSystem(protocol + "proxyPort", String.valueOf(port), buf);
                if (user != null)
                    setSystem(protocol + "proxyUser", user, buf);
                if (passwd != null)
                    setSystem(protocol + "proxyPassword", passwd, buf);
                LOG.info(buf);

                SocketAddress addr = new InetSocketAddress(newProxyHost, port);
                Proxy.Type type =
                    protocol.startsWith("http") ? Proxy.Type.HTTP : protocol.startsWith("sock") ? Proxy.Type.SOCKS
                        : Proxy.Type.DIRECT;
                return type.equals(Proxy.Type.DIRECT) ? Proxy.NO_PROXY : new Proxy(type, addr);
            } else
                // last configuration
                LOG.info(buf);
            return currentProxy;
        } catch (URISyntaxException e) {
            e.printStackTrace();
            new RuntimeException(e);
        }
        return null;
    }

    static String getSystem(String k) {
        return System.getProperty(k) != null ? "\n\t" + k + "=" + System.getProperty(k) : "";
    }

    static void setSystem(String k, String v, StringBuilder log) {
        log.append("\n\t key=" + k + ", value=" + v);
        System.setProperty(k, v);
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
        return Boolean.TRUE.equals(scan(ip, port, port).get(new InetSocketAddress(ip, port)));
    }

    /**
     * creates a new socket and connects it to the given server (host + port)
     * 
     * @param host server
     * @param port communication port
     * @return client socket
     */
    public static Socket connect(String host, int port) {
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), 3000);
            return socket;
        } catch (Exception e) {
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * sends the given args to the given server, creating and closing a socket to the server
     * 
     * @param host server ip
     * @param port server socket port
     * @param resultType result type
     * @param args data (key/values) to be sent packed into a map (key1, value1, key2, value2,...)
     * @return servers result
     */
    public static <T> T send(String host, int port, Class<T> resultType, Object... args) {
        Socket socket = NetUtil.connect(host, port);
        try {
            return send(connect(host, port), resultType, args);
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    ManagedException.forward(e);
                }
            }
        }
    }

    /**
     * sends the given args to the given server, using the given socket - without closing it
     * 
     * @param socket socket
     * @param resultType result type
     * @param args data (key/values) to be sent packed into a map (key1, value1, key2, value2,...)
     * @return servers result
     */
    @SuppressWarnings("unchecked")
    public static <T> T send(Socket socket, Class<T> resultType, Object... args) {
        try {
            socket.getOutputStream().write(BeanUtil.serialize(MapUtil.asMap(args)));
            return (T) serialize(toByteArray(socket.getInputStream()));
        } catch (IOException e) {
            ManagedException.forward(e);
            return null;
        }
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
            for (int p = minPort; p <= maxPort; p++) {
                worker.run(new PortScan(new InetSocketAddress(ips[i], p), timeout, result));
            }
        }
        result = worker.waitForJobs(timeout * 4);
        StringBuilder buf = new StringBuilder(result.size() * 30);
        buf.append("========== network-port-check finished ================\n");
        buf.append("open ports:\n");
        Set<InetSocketAddress> ns = result.keySet();
        for (InetSocketAddress a : ns) {
            if (Boolean.TRUE.equals(result.get(a))) {
                buf.append(a + "\t: " + props.getProperty(String.valueOf(a.getPort())));
            }
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
        if (requestedURLs.contains(url)) {
            return;
        }
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
        String u, f;
        int index = 0;
        while (index < txt.length() && (!Util.isEmpty(u =
            StringUtil.extract(txt, "(?:(href|src)\\s*\\=\\s*\")(.)+(?:\")", null, index)))) {
            u = StringUtil.substring(u, "\"", "\"");
            try {
                url = NetUtil.url(u, site.getHost());
                if (site.getHost().equals(url.getHost())) {
                    f = NetUtil.getFileName(url);
                } else {
                    f = FileUtil.getValidFileName(url.toString());
                }
                index = txt.indexOf(u, index) + f.length();
                urls.add(url);
                if (LOG.isDebugEnabled()) {
                    buf.append("\n\t--> " + url);
                }
                //replace url with a local file name
                StringUtil.replace(txt, u, f);
            } catch (Exception e) {
                LOG.debug(e.toString());
                index += 4;
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug(buf.toString());
        }
        return urls.toArray(new URL[0]);
    }

}
