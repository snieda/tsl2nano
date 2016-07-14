/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 25.02.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.h5.expression;

import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Default;
import org.simpleframework.xml.DefaultType;
import org.simpleframework.xml.ElementArray;

import de.tsl2.nano.bean.BeanUtil;
import de.tsl2.nano.core.exception.Message;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.EHttpClient;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.core.util.NetUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.execution.IPRunnable;
import de.tsl2.nano.historize.Volatile;

/**
 * Usable as attribute getting it's value through a given restful service. the expression is here an url query path. The
 * default method is GET. If method is PUT or POST, the parent bean (this expression is part of an attribute!) will be
 * transfered as JSON object.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
@Default(value = DefaultType.FIELD, required = false)
public class RestfulExpression<T extends Serializable> extends RunnableExpression<T> {
    /** serialVersionUID */
    private static final long serialVersionUID = -107140100937166501L;

    private static final Log LOG = LogFactory.getLog(RestfulExpression.class);
    @SuppressWarnings({ "unchecked", "rawtypes" })
    transient Volatile response = new Volatile(300, null);
    /** REST method type */
    @Attribute(required = false)
    String method = "GET";
    @Attribute(required = false)
    String contentType;
    @ElementArray(required = false)
    String[] parameterNames;
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

    static {
        registerExpression(RestfulExpression.class);
    }

    public RestfulExpression() {
    }

    public RestfulExpression(Class<?> declaringClass, String query) {
        this(declaringClass, query, null);
    }

    /**
     * constructor
     */
    public RestfulExpression(Class<?> declaringClass, String query, Class<T> type) {
        super(declaringClass, query, type);
    }

    @Override
    public String getExpressionPattern() {
        return "http[s]?:[/]{2,2}.*(\\:\\d{1,8}/)?.*";
    }

    @Override
    protected IPRunnable<T, Map<String, Object>> createRunnable() {
        final RestfulExpression<T> this_ = this;
        return new IPRunnable<T, Map<String, Object>>() {
            transient Map<String, ? extends Serializable> parameters;

            @SuppressWarnings("unchecked")
            @Override
            public T run(Map<String, Object> context, Object... extArgs) {
                if (response.expired()) {
                    if (!NetUtil.isOnline()) {
                        Message.send("server is offline - " + getName() + " representing last rest response");
//                    return (T) response;
                    }
                    Object data = context.size() == 1 ? context.values().iterator().next() : context.get(getName());
                    String postStr = null;
                    if (data != null) {
                        if ("PUT".equals(method) || "POST".equals(method)) {
                            postStr = MapUtil.toJSON(BeanUtil.toValueMap(data, false, false, false, this_.getName()));
                        } else if (Util.isEmpty(extArgs)) {
                            Map<String, Object> valueMap =
                                BeanUtil.toValueMap(data, false, false, false, this_.getName());
                            extArgs = valuesOnly ? valueMap.values().toArray() : MapUtil.asArray(valueMap);
                        }
                    } else {
                        LOG.warn("the given context may not have desired informations for url " + expression + ": "
                            + context);
                    }
                    EHttpClient http = new EHttpClient(
                        expression + (valuesOnly ? StringUtil.concat(new char[0], extArgs) : ""), urlRESTSeparators);
                    if (valuesOnly)
                        response.set(http.get());
                    else
                        response.set(
                            new EHttpClient(expression, urlRESTSeparators).rest("", method, contentType, postStr,
                                extArgs));
                }
                return (T) response.get();
            }

            @Override
            public String getName() {
                return expression;
            }

            @Override
            public Map<String, ? extends Serializable> getParameter() {
                if (parameters == null) {
//                    parameters = new LinkedHashMap<>();
//                    if (parameterNames != null) {
//                        for (int i = 0; i < parameterNames.length; i++) {
//                            parameters.put(parameterNames[i], String.class);
//                        }
//                    }
                }
                return parameters;
            }

            @Override
            public Map<String, Object> checkedArguments(Map<String, Object> args, boolean strict) {
                return parameterNames != null
                    ? MapUtil.retainAll(new LinkedHashMap<>(args), Arrays.asList(parameterNames))
                    : args;
            }
        };
    }

    @Override
    public String getName() {
        return !Util.isEmpty(expression) ? StringUtil.substring(expression, "://", "/") : "[undefined]";
    }
}
