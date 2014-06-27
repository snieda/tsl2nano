/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Feb 2014
 * 
 * Copyright: (c) Thomas Schneider, all rights reserved
 */
package de.tsl2.nano.service.util;

import java.io.Serializable;
import java.util.Hashtable;
import java.util.LinkedList;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import de.tsl2.nano.bean.def.IStatus;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.execution.ICRunnable;
import de.tsl2.nano.core.execution.IRunnable;
import de.tsl2.nano.serviceaccess.ServiceFactory;
import de.tsl2.nano.serviceaccess.aas.ConsoleCallbackHandler;
import de.tsl2.nano.serviceaccess.aas.principal.UserPrincipal;

/**
 * Initializes the {@link ServiceFactory} , does a login and starts the given usecase.
 * <p/>
 * Usecases have to implement {@link IRunnable}. Optional arguments may be given additionally.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class RemoteServiceRunner<SimpleContext> {

    /**
     * mini service locator. overwrite this method, if you want to login to a special user, to get the roles, the user
     * entity and the mandator entity. another way would be to create the service inside your setUp method. to do a real
     * login, use the method {@linkplain ServiceFactory#login(String, ClassLoader, String, String)}.<p/>
     * NOT FINISHED YET!
     * 
     * @param <T> service type
     * @param serviceInterface service
     * @return service implementation
     */
    protected static <T> T getService(Class<T> serviceInterface) {
        if (!ServiceFactory.isInitialized()) {
            ServiceFactory.createInstance(BaseServiceTest.class.getClassLoader());
            ServiceFactory.instance().createSession(null,
                null,
                null,
                new LinkedList<String>(),
                new LinkedList<String>(),
                null);

            //init the server side
            ServiceFactory.getGenService().initServerSideFactories();
        }
        return ServiceFactory.instance().getService(serviceInterface);
    }

    /**
     * start usecase
     * 
     * @param useCaseInterface usecase interface.
     * @param args
     */
    protected void start(Class<ICRunnable<SimpleContext>> useCaseInterface, String... args) {
        Object user = null;
        if (!ServiceFactory.isInitialized()) {
            user = login();
        } else {
            user = ServiceFactory.instance().getUserObject();
        }
        final ICRunnable<SimpleContext> uc = getService(useCaseInterface);
        final SimpleContext context = new SimpleContext();
        context.user = user;
        try {
            log("starting usecase '" + useCaseInterface.getName()
                + "'\n  Parameter:\n"
                + context);
            uc.run(context, args);
            log("UseCase '" + useCaseInterface.getName()
                + "' finished successful\n  Result:\n"
                + context);
        } catch (final Exception e) {
            ManagedException.forward(e);
        }
    }

    /**
     * loginBatch
     * 
     * @return
     * @throws RuntimeException
     */
    protected Object login() {
        LoginContext lc;
        try {
            lc = new LoginContext("LoginJaas", new ConsoleCallbackHandler());
            lc.login();
            return lc.getSubject().getPrincipals(UserPrincipal.class).iterator().next();
        } catch (LoginException e) {
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * main method to start a usecase
     * 
     * @param args usecase interface.
     * @throws Exception on error...
     */
    public static final void main(String args[]) throws Exception {
        if (args.length == 0) {
            log("Please provide a classpath to the usecase-interface to be started!");
            return;
        }
        final Class<?> uc = Class.forName(args[0]);
        new RemoteServiceRunner().start((Class<ICRunnable<RemoteServiceRunner.SimpleContext>>) uc, args);
    }

    protected static final void log(String msg) {
        //perhaps no LOGGER availabe yet ==> system.out
        System.out.println(msg);
    }
    
    /**
     * 
     * @author Tom, Thomas Schneider
     * @version $Revision$ 
     */
    public class SimpleContext implements Serializable {
        private static final long serialVersionUID = 1L;
        String ucId;
        IStatus status;
        Object user;
        Hashtable<String, Object> parameter;
    }
}
