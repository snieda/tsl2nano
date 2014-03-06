/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: TS, Thomas Schneider
 * created on: Jan 11, 2010
 * 
 * Copyright: (c) Thomas Schneider 2010, all rights reserved
 */
package de.tsl2.nano.service.util;

import java.util.Arrays;

import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

import org.apache.commons.logging.Log;

import de.tsl2.nano.log.LogFactory;

/**
 * A EJB 3.x interceptor useful for logging.
 * 
 * @author EGU, Thomas Schneider
 * @version $Revision$
 */
public class LoggingInterceptor {
    private static final Log LOG = LogFactory.getLog(LoggingInterceptor.class);

    /**
     * "Around" interceptor method.
     * 
     * @param invocationContext the context.
     * @return whatever the invoked methods returned
     * @throws Exception only exceptions declared by the invoked method
     */
    @AroundInvoke
    public Object around(InvocationContext invocationContext) throws Exception {
        try {
            LOG.info("invoke method=" + invocationContext.getMethod()
                + " params="
                + Arrays.toString(invocationContext.getParameters()));
            return invocationContext.proceed();
        } catch (final Exception e) {
            LOG.error("catch and rethrow", e);
            throw e;
        }
    }

}
