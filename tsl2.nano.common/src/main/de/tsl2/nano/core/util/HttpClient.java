/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: 08.07.2016
 * 
 * Copyright: (c) Thomas Schneider 2016, all rights reserved
 */
package de.tsl2.nano.core.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.log.LogFactory;

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
    
    HttpURLConnection http;

    Object result;
    static final String UTF8 = "UTF-8";

    /**
     * constructor
     * 
     * @param http
     */
    public HttpClient(String wsUrl) {
        createHttpConnection(wsUrl);
    }

    /**
     * createHttpConnection
     * @param wsUrl
     */
    protected HttpClient createHttpConnection(String wsUrl) {
        try {
            URL url = new URL(wsUrl);
            http = (HttpURLConnection) url.openConnection();
        } catch (Exception e) {
            ManagedException.forward(e);
        }
        return this;
    }

    public String getString() {
        return read(get(null), String.class);
    }
    public InputStream get() {
        return get(null);
    }

    public HttpClient setRequestProperty(String key, String value) {
        http.setRequestProperty(key, value);
        return this;
    }

    /**
     * delegates to {@link #http(String, String, String, byte[])} with method GET
     * 
     * @param wsUrl service url
     * @param contenttype
     * @return url inputstream
     */
    public InputStream get(String contenttype) {
        return send("GET", contenttype, null);
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
    public InputStream send(String method, String contenttype, byte[] data) {
        try {
            http.setRequestMethod(method);
            if (contenttype != null)
                http.setRequestProperty("Content-Type", contenttype);
            if ((method.equals("POST") || method.equals("PUT")) && data != null) {
                http.setDoOutput(true);
                http.setFixedLengthStreamingMode(data.length);
                OutputStream os = http.getOutputStream();
                os.write(data);
            }
            run();
            return response();
        } catch (Exception e) {
            ManagedException.forward(e);
            return null;
        }
    }

    public InputStream response() {
        try {
            return http.getInputStream();
        } catch (IOException e) {
            ManagedException.forward(e);
            return null;
        }
    }

    public byte[] read(InputStream in) {
        return ByteUtil.toByteArray(in);
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
        return http.toString();
    }
}
