/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 15.07.2016
 * 
 * Copyright: (c) Thomas Schneider 2016, all rights reserved
 */
package de.tsl2.nano.h5.expression;

import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Default;
import org.simpleframework.xml.DefaultType;
import org.simpleframework.xml.Element;

import de.tsl2.nano.bean.BeanUtil;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.exception.Message;
import de.tsl2.nano.core.http.EHttpClient;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.core.util.NetUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.historize.Volatile;
import de.tsl2.nano.specification.AbstractRunnable;

/**
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
@Default(value = DefaultType.FIELD, required = false)
public class WebClient<T> extends AbstractRunnable<T> {
    /** serialVersionUID */
    private static final long serialVersionUID = -5384389159596172090L;

    private static final Log LOG = LogFactory.getLog(WebClient.class);
    @SuppressWarnings({ "unchecked", "rawtypes" })
    transient Volatile response = new Volatile(ENV.get("cache.expire.milliseconds.webclient", 1000));

    /** REST method type */
    @Attribute(required = false)
    String method = "GET";
    /** content-type/media-type like application/xml etc. */
    @Attribute(required = false)
    String contentType;
    /**
     * (optional) key name of context entry to be used as content, if method is not GET. if null, the attributes type
     * name will be used.
     */
    @Element(required = false)
    String contextKey;
    /**
     * if false, the url query will be created like: http://.../search?city=Munich&code=80000. otherwise, the parameter
     * will be appended through '/'.
     */
    @Attribute(required = false)
    boolean urlRESTSeparators;
    /** if true, the values (without names and separators) will be appended directly to the url */
    @Attribute(required = false)
    boolean valuesOnly;
    /**
     * if true, the response-data will be embedded into the html (e.g. iframe-srcdoc) to be handled by a
     * dependency-listener etc. - if false, the response is handled as link (like iframe-src)
     */
    @Attribute(required = false)
    boolean handleResponse;
    /** constrain the read timeout of an http request */
    @Attribute(required = false)
    Integer readTimeout;

    /**
     * constructor
     */
    public WebClient() {
    }

    /**
     * sets the new Expression and evaluates new values for: {@link #method}, {@link #contentType} {@link #valuesOnly},
     * {@link #urlRESTSeparators}, {@link #handleResponse}.
     * 
     * @param expression new expression
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static WebClient create(String expression, Class contentClass) {
        WebClient ws = new WebClient();
        //optional: extract method and content-type, given at the end like: <<PUT:application/xml
        String methodContent = StringUtil.substring(expression, "<<", null, false, true);
        expression = StringUtil.substring(expression, null, "<<");
        if (methodContent != null) {
            ws.method = StringUtil.substring(methodContent, null, ":");
            ws.contentType = StringUtil.substring(methodContent, ":", null);
        }
        if (expression.matches(".*[{](\\w+)[}].*")) {
            StringBuilder e = new StringBuilder(expression);
            ws.parameter = (LinkedHashMap) ws.createSimpleParameters(StringUtil.extractAll(e, "[{](\\w+)[}]"));
        }
        if (expression.endsWith("="))
            ws.valuesOnly = true;
        if (expression.matches(".*[:]\\d+.*")) {//port --> special, not public service
            ws.urlRESTSeparators = true;
            ws.handleResponse = true;
        }
        if (expression.contains(String.valueOf(EHttpClient.SEPARATORS_QUERY)))
            ws.urlRESTSeparators = false;
        
        ws.operation = expression;
        if (contentClass != null) {
            ws.parameter = (LinkedHashMap) ws.createParameters(contentClass);
            ws.contextKey = StringUtil.toFirstLower(BeanClass.getName(contentClass));
        }
        ws.name = getName(expression);
        return ws;
    }

    static String[] createParameterNames(Class<?> declaringClass) {
        BeanDefinition<?> def = BeanDefinition.getBeanDefinition(declaringClass);
        return def.getAttributeNames(false);
    }

    @SuppressWarnings("unchecked")
    @Override
    public T run(Map<String, Object> context, Object... extArgs) {
        if (response.expired()) {
            if (!NetUtil.isOnline()) {
                Message.send("server is offline - " + getName() + " representing last rest response");
//            return (T) response;
            }
            Object data = context.size() == 1 ? context.values().iterator().next() : context.get(contextKey);
            String postStr = null;
            if (data != null) {
                if ("PUT".equals(method) || "POST".equals(method)) {
                    postStr = MapUtil.toJSon(BeanUtil.toValueMap(data, false, false, false, getName()));
                } else if (Util.isEmpty(extArgs)) {
                    Map<String, Object> valueMap =
                        BeanUtil.toValueMap(data, false, false, false, getName());
                    extArgs = valuesOnly ? valueMap.values().toArray() : MapUtil.asArray(valueMap);
                }
            } else if (Util.isEmpty(extArgs) && context.size() > 0) {
                extArgs = valuesOnly ? context.values().toArray() : MapUtil.asArray(context);
            } else {
                LOG.warn("the given context may not have desired informations for url " + operation + ": "
                    + context);
            }
            EHttpClient http = new EHttpClient(
                operation + (valuesOnly ? URLEncoder.encode(StringUtil.concat(new char[]{' '}, extArgs)) : ""),
                urlRESTSeparators);
            if (readTimeout != null)
                http.setReadTimeout(readTimeout);
            if (valuesOnly)
                response.set(http.getString());
            else
                response.set(
                    new EHttpClient(operation, urlRESTSeparators).rest("", method, contentType, postStr,
                        extArgs));
        }
        return (T) response.get();
    }

    public String getUrl() {
        return operation;
    }
    public static String getName(String url) {
        return FileUtil.getValidFileName(!Util.isEmpty(url) ? StringUtil.substring(url, "://", "/") : "[undefined]");
    }
    @Override
    public String getName() {
        return getName(operation);
    }
}
