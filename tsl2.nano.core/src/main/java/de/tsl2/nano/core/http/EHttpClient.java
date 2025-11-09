/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: 08.07.2016
 * 
 * Copyright: (c) Thomas Schneider 2016, all rights reserved
 */
package de.tsl2.nano.core.http;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonStructure;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;

/**
 * Extended Http Client, providing REST param evaluation and multipart form data with files.
 * 
 * usage: see {@link #rest(String, String, String, String, Object...)} and {@link #restJSON(String, Object...)}
 * 
 * @author Thomas Schneider
 * @version $
 */
public class EHttpClient extends HttpClient {
    public static final String AUTHORIZATION = "Authorization";
    private static final Log LOG = LogFactory.getLog(EHttpClient.class);
    public static final char[] SEPARATORS_REST = new char[] { '/', '/', '/' };
    public static final char[] SEPARATORS_QUERY = new char[] { '?', '=', '&' };
    private boolean useRESTSeparators;
    private Map<String, Object> header; // while HttpUrlConnection.getRequestProperties() does not return any added request property, we hold it here

    /**
     * constructor
     * 
     * @param wsUrl base url
     */
    public EHttpClient(String wsUrl) {
        this(wsUrl, true);
    }

    /**
     * constructor
     * @param wsUrl base url
     * @param useRESTSeparators if true, {@link #SEPARATORS_REST} will be used, otherwise {@link #SEPARATORS_QUERY}.
     */
    public EHttpClient(String wsUrl, boolean useRESTSeparators) {
        super(wsUrl);
        this.useRESTSeparators = useRESTSeparators;
    }

    public EHttpClient(String wsUrl, Map<String, Object> header, Authenticator auth) {
        this(wsUrl, true);
        setHeader(header);
        http.setAuthenticator(auth);
    }

    public EHttpClient(String url, Map<String, Object> header, String user, char[] passwd) {
        super(url);
        this.useRESTSeparators = true;
        if (header == null)
            header = new HashMap<>();
        header.putAll(createBasicAuthorization(user, passwd));
        setHeader(header);

        // http.setAuthenticator(new Authenticator() {
        //     // TODO: implement with new PasswordAuthentication(user, passwd));
        // });
    }

    Map<String, Object> header() {
        if (header == null)
            header = new HashMap<>();
        return header;
    }

    public HttpClient setHeader(Map<String, Object> header) {
        this.header = header;
        return this;
    }

    @SuppressWarnings("unchecked")
    public static  Map<String, ?>  createBasicAuthorization(String user, char[] passwd) {
        return MapUtil.asProperties(AUTHORIZATION, "Basic " + StringUtil.toBase64(user + ":" + String.valueOf(passwd)));
    }

    /** extracts basic authorization from header returning user and password in string array */
    public static String[] getBasicAuthorization(Map<String, ?> header) {
        Objects.requireNonNull(header);
        String auth = getEntry(header, AUTHORIZATION);
        if (Util.isEmpty(auth))
            throw new SecurityException("unauthorized");
        auth = StringUtil.substring(auth, "Basic ", null);
        auth = StringUtil.fromBase64(auth);
        // TODO: password should be char[] instead of string
        return auth.split(":");
    }

    private static String getEntry(Map<String,?> header, String key) {
        String value;
        if ((value = Util.asString(header.get(key))) != null)
            return value;
        else if ((value = Util.asString(header.get(key.toLowerCase()))) != null)
            return value;
        else
            return Util.asString(header.get(key.toUpperCase()));
    }

    public HttpClient multipartData(HttpURLConnection http, Object... chunks) {
        try {
            String boundary = UUID.randomUUID().toString();
            byte[] boundaryBytes =
                ("--" + boundary + "\r\n").getBytes(UTF8);
            byte[] finishBoundaryBytes =
                ("--" + boundary + "--").getBytes(UTF8);
            http.setRequestProperty("Content-Type",
                "multipart/form-data; charset=UTF-8; boundary=" + boundary);
            // Enable streaming mode with default settings
            http.setChunkedStreamingMode(0);

            OutputStream out = http.getOutputStream();
            for (int i = 0; i < chunks.length; i++) {
                if (chunks[i + 1] instanceof String) {
                    data(out, (String) chunks[i], (String) chunks[i + 1]);
                } else if (chunks[i + 1] instanceof InputStream) {
                    data(out, (String) chunks[i], (InputStream) chunks[i + 1], (String) chunks[i]);
                } else {
                    throw new IllegalArgumentException("chunks must be of type String or InputStream");
                }
                // Send a separator
                out.write(i >= chunks.length - 1 ? finishBoundaryBytes : boundaryBytes);
            }
            // Finish the request
            out.write(finishBoundaryBytes);
        } catch (Exception e) {
            ManagedException.forward(e);
        }
        return this;
    }

    protected HttpClient data(OutputStream out, String name, String value) {
        try {
            String o = "Content-Disposition: form-data; name=\""
                + URLEncoder.encode(name, UTF8) + "\"\r\n\r\n";
            out.write(o.getBytes(UTF8));
        } catch (Exception e) {
            ManagedException.forward(e);
        }
        return this;
    }

    protected HttpClient data(OutputStream out, String name, InputStream in, String fileName) {
        try {
            String o = "Content-Disposition: form-data; name=\"" + URLEncoder.encode(name, "UTF-8")
                + "\"; filename=\"" + URLEncoder.encode(fileName, UTF8) + "\"\r\n\r\n";
            out.write(o.getBytes(UTF8));
            FileUtil.write(in, out, false);
            byte[] buffer = new byte[2048];
            for (int n = 0; n >= 0; n = in.read(buffer))
                out.write(buffer, 0, n);
            out.write("\r\n".getBytes(UTF8));
        } catch (Exception e) {
            ManagedException.forward(e);
        }
        return this;
    }

    public JsonStructure restJSON(String url, Object... args) {
        String jsonStr = get(url, args);
        return Json.createReader(new StringReader(jsonStr)).read();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public /*<T> T*/String get(String url/*, Class<T> responseType*/, Map args) {
        return get(url, MapUtil.asArray(args));
    }

    public String get(String url, Object... args) {
        return get(url, getParameterSeparators(), args);
    }

    public String rest_(String url, Object... args) {
        return get(url, getParameterSeparators(), args);
    }
    /**
     * getParameterSeparators
     * @return
     */
    char[] getParameterSeparators() {
        return useRESTSeparators ? SEPARATORS_REST : SEPARATORS_QUERY;
    }

    public String get(String url/*, Class<T> responseType*/, char[] separators, Object... args) {
        return rest(url, "GET", null, null, separators, args);
    }
    public String rest(String url, String method, String contenttype, String data, Object... args) {
        return rest(url, method, contenttype, data, getParameterSeparators(), args);
    }
    /**
     * simply returns the response of the given url + args restful request
     * 
     * @param url
     * @param responseType type of response object
     * @param separators 0: start of parameters (e.g.: '?'), 2: separator between parameters (e.g.: '&'), 1: separator
     *            between key and value (e.g.: '=').
     * @param args key/value pairs to be appended as rest-ful call to the url
     * @return content as object of type responseType
     */
    public String rest(String url, String method, String contenttype, String data /*, Class<T> responseType*/, char[] separators, Object... args) {
        String restUrl = parameter( url.contains("://") ? url : this.url + url, separators, args);
        header().put("Content-Type", contenttype);
        LOG.debug("reading response from" + restUrl);
        // if (true /* TODO: how to check if already connected or disconnected */)
        //     http = NetUtil.copyHeader(http, openHttpConnection(restUrl));
        return read(send(url, method, header, data != null ? data.getBytes() : null), String.class);
    }

    /**
     * inserts given args into the given url.
     * 
     * @param url base url
     * @param args key-/value pairs
     * @return new url path
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static String path(String url, Object... args) {
        //prepare encoded values...
        Map map = MapUtil.asMap(args);
        Map margs = new LinkedHashMap(map.size());
        for (Object k : map.keySet()) {
            Object v = map.get(k);
            if (v != null)
                margs.put(k, URLEncoder.encode(v.toString()));
        }
        return StringUtil.insertProperties(url, margs, "{", "}");
    }

    public static String parameter(String url, boolean rest, Object... args) {
        return parameter(url, rest ? SEPARATORS_REST : SEPARATORS_QUERY, args);
    }
    
    /**
     * path
     * 
     * @param url baseurl where the args are appended - or a rest url with e.g.: /mybaseurl/{mypathparam1}/{mypathparam2}
     * @param separators should be one of {@link #SEPARATORS_REST} or {@link #SEPARATORS_QUERY}
     * @param args arguments to insert or append
     * @return complete url
     */
    protected static String parameter(String url, char[] separators, Object... args) {
        StringBuilder buf = new StringBuilder(path(url, args));
        if (args != null) {
            char c = url.endsWith(String.valueOf(separators[0])) ? 0 : separators[0];
            for (int i = 0; i < args.length; i++) {
                try {
                    //WORKAROUND: if (parameter was already inserted (perhaps through path()) ignore key and value
                    if (i < args.length -1 && (args[i+1] == null || buf.indexOf(separators[2] + URLEncoder.encode(args[i+1].toString())) != -1)) {
                        ++i;
                        continue;
                    }
                    buf.append((c != 0 ? c : "") + URLEncoder.encode(StringUtil.toString(args[i]), UTF8));
                } catch (UnsupportedEncodingException e) {
                    ManagedException.forward(e);
                }
                c = i % 2 == 0 ? separators[1] : separators[2];
            }
        }
        url = buf.toString();
        return url;
    }

    @Override
    public void run() {
        if (http.getURL().toString().contains("{"))
            throw new IllegalStateException("variables not filled: " + http.getURL().toString());
        super.run();
    }
}
