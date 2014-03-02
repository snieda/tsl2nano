/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider, Thomas Schneider
 * created on: Feb 27, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.service.util;

import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import de.tsl2.nano.exception.ManagedException;
import de.tsl2.nano.execution.ICRunnable;
import de.tsl2.nano.execution.Runner;
import de.tsl2.nano.serviceaccess.ServiceFactory;
import de.tsl2.nano.util.StringUtil;

/**
 * ON IMPLEMENTATION...</p> provides starting services from shell.</p> call arguments:</br>
 * 
 * <pre>
 *   1. properties file-name
 *   2. login user
 *   3. login passwd
 *   
 *   the properties file should contain:
 *      service=[full remote service interface path]
 *      method=[remote service method]
 *      loginmodule=[login module name]
 * 
 * the context is a map with objects to be compatible to Properties.
 * 
 * preconditions:
 *   - the tsl2nano 'project.properties' should be found on root classpath (perhaps add your shared-plugin)
 *   - the file-pathes (like jndi-file) defined in project.properties should be found
 *   - the appservers client libs should be found in classpath
 *   - the service interfaces should be found in classpath
 *   - the given loginmodule must be given as java-start-argument:
 *     e.g. -Djava.security.auth.login.config=kion/config/jaas-login.config
 *     
 * test:
 *   a testable servicerunner.properties is available on project-root-path
 * </pre>
 * 
 * 
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
public class ServiceRunner extends BaseServiceTest implements ICRunnable<HashMap<Object, Object>> {
    Properties p;
    ArrayList<?> argList;

    /**
     * {@inheritDoc}
     */
    @Override
    public HashMap<Object, Object> run(HashMap<Object, Object> context, Object... extArgs) {
        argList = new ArrayList(Arrays.asList(extArgs));
        Object user = null;
        p = new Properties();
        try {
            p.load(new FileReader(new File((String) nextArg())));
            if (!ServiceFactory.isInitialized()) {
                final String loginmodule = p.getProperty("loginmodule");
                getService(IGenericService.class);
                log("starting login...");
                final String userName = (String) nextArg();
                ServiceFactory.instance();
                ServiceFactory.login(loginmodule, this.getClass().getClassLoader(), userName, (String) nextArg());
                user = ServiceFactory.instance().getUserObject();
                if (user == null) {
                    throw new RuntimeException("authentication of user " + userName + " failed!");
                }
            } else {
                user = ServiceFactory.instance().getUserObject();
            }
            context.putAll(p);
            context.put("user", user);
            final Object result = callService(context, argList.toArray());
            context.put("result", result);
            return context;
        } catch (final Exception e) {
            ManagedException.forward(e);
            return null;
        }
    }

    private Object nextArg() {
        return argList.remove(0);
    }

    /**
     * callService
     * 
     * @param context
     * @param extArgs
     * @return
     */
    protected Object callService(Map<Object, Object> context, Object... extArgs) {
        final String s = (String) context.get("service");
        Class<?> serviceInterface;
        try {
            serviceInterface = Class.forName(s);
            final Object service = getService(serviceInterface);

            final String m = (String) context.get("method");
            final Method method = serviceInterface.getMethod(m, Runner.methodArgs(extArgs));
            return method.invoke(service, extArgs);
        } catch (final Exception e) {
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * main methode zum starten eines anwendungsfalles
     * 
     * @param args schnittstelle vom type {@link IAnwendungsfall}.
     * @throws Exception bei fehler...
     */
    public static final void main(String args[]) throws Exception {
        if (args.length == 0) {
            log("Please provide the path to a properties file, the username and passwd");
            return;
        }
        final HashMap<Object, Object> result = new ServiceRunner().run(new HashMap<Object, Object>(), args);
        log(StringUtil.toFormattedString(result, -1, true));
    }

    protected static final void log(String msg) {
        // perhaps no LOGGER available ==> system.out
        System.out.println(msg);
    }
}
