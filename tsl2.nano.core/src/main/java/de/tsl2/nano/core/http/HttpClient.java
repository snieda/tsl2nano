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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.ByteUtil;
import de.tsl2.nano.core.util.MapUtil;

/**
 * simple http client
 * <p/>
 * Use: get() or send(...)
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class HttpClient implements Runnable {
    Log LOG = LogFactory.getLog(HttpClient.class);

    protected String url;

    protected HttpURLConnection http;
    
    static final String UTF8 = "UTF-8";

    public HttpClient(String url) {
        this.url = url;
    }

    protected HttpURLConnection openHttpConnection(String wsUrl) {
        try {
            URL url = new URL(wsUrl);
            return (HttpURLConnection) url.openConnection();
        } catch (Exception e) {
            ManagedException.forward(e);
            return null;
        }
    }

    public String getString() {
        return read(get(url, null), String.class);
    }
    public InputStream get() {
        return get(url, null);
    }

    /**
     * delegates to {@link #http(String, String, String, byte[])} with method GET
     * 
     * @param wsUrl service url
     * @param contenttype
     * @return url inputstream
     */
    public InputStream get(String url, String contenttype) {
        return send(url, "GET", contenttype, null);
    }

    /**
     * does an http request to the given url. if method is POST or PUT, the given data will be sent to the given web
     * service.
     * 
     * @param wsUrl web service
     * @param method HttpMethod like GET, PUT, POST etc.
     * @param contenttype content type like 'application/xml', 'application/json; charset=UTF-8' etc.
     * @param data (optional) data to post or put (only if method is POST or PUT)
     * @return the http response after sending the request. error handling is included using unchecked exceptions
     */
    public InputStream send(String url, String method, String contenttype, byte[] data) {
        return send(url, method, MapUtil.asProperties("Content-Type", contenttype), data);
    }

    public InputStream send(String url, String method, Map<String, Object> header, byte[] data) {

        try {
            http = openHttpConnection(url);
            http.setRequestMethod(method);
            if (header != null) {
                header.forEach( (k, v) -> http.addRequestProperty(k, String.valueOf(v) ) );
            }
            if ((method.equals("POST") || method.equals("PUT")) && data != null) {
                http.setDoOutput(true);
                http.setFixedLengthStreamingMode(data.length);
                OutputStream os = http.getOutputStream();
                os.write(data);
                os.close();
            }
            run();
            return response(http);
        } catch (Exception e) {
            ManagedException.forward(e);
            return null;
        } finally {
            http = null;
        }
    }

    public InputStream response(URLConnection http) {
        try {
            return http.getInputStream();
        } catch (IOException e) {
            ManagedException.forward(e);
            return null;
        }
    }

    public byte[] read(InputStream in) {
        try {
            try (in) {
                return ByteUtil.toByteArray(in);
            }
        } catch (IOException e) {
            ManagedException.forward(e);
            return null;
        }
    }
    
    public <T> T read(InputStream in, Class<T> type) {
        return ByteUtil.toByteStream(read(in), type);
    }
    
    /**
     * does the request...
     */
    @Override
    public void run() {
        try {
            if (http.getDoOutput())
                http.getOutputStream().flush();
            LOG.debug("sending request " + http.getURL());
            http.connect();
            int code = http.getResponseCode();
            LOG.debug("--> http-code:" + code);
            if (code >= 300)
                throw new IllegalStateException("Http " + code + (http.getErrorStream() != null
                    ? ": " + new String(ByteUtil.toByteArray(http.getErrorStream()), "UTF-8") : ""));
        } catch (IOException e) {
            ManagedException.forward(e);
        }
    }

    @Override
    public String toString() {
        return url;
    }
}
