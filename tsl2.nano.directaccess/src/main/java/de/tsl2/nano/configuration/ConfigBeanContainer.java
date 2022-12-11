package de.tsl2.nano.configuration;

import java.lang.reflect.Proxy;
import java.util.Arrays;

import javax.persistence.EntityManager;

import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.bean.BeanProxy;
import de.tsl2.nano.bean.IBeanContainer;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.util.ConcurrentUtil;
import de.tsl2.nano.persistence.GenericLocalBeanContainer;
import de.tsl2.nano.persistence.Persistence;
import de.tsl2.nano.service.util.BeanContainerUtil;
import de.tsl2.nano.service.util.IGenericService;
import de.tsl2.nano.serviceaccess.Authorization;
import de.tsl2.nano.serviceaccess.IAuthorization;
import de.tsl2.nano.serviceaccess.ServiceFactory;
import de.tsl2.nano.serviceaccess.ServiceProxy;

/**
 * create a proxied or local beancontainer - for test purposes only
 */

public class ConfigBeanContainer {
    public static EntityManager initAuthAndLocalBeanContainer() {
        return initProxyBeanContainer(initUserAuth());
    }
	public static EntityManager initProxyBeanContainer(Authorization auth) {
		BeanContainerUtil.initProxyServiceFactory();
        GenericLocalBeanContainer.initLocalContainer();
        ServiceFactory.instance().setSubject(auth.getSubject());
        ENV.addService(IBeanContainer.class, BeanContainer.instance());
        ConcurrentUtil.setCurrent(BeanContainer.instance());
        return ENV.addService(EntityManager.class, BeanProxy.createBeanImplementation(EntityManager.class));
	}
	public static Authorization initUserAuth() {
		String userName = Persistence.current().getConnectionUserName();
        Authorization auth = Authorization.create(userName, false);
        ENV.addService(IAuthorization.class, auth);
        ConcurrentUtil.setCurrent(auth);
		return auth;
	}

    /**
     * use that only, if you called initProxyBeanContainer() before!
     */
    public static IGenericService getGenServiceProxy() {
        return (IGenericService) ((ServiceProxy)Proxy.getInvocationHandler(ServiceFactory.getGenService())).delegationProxy();
    }

    public static void simpleReturnExampleItself() {
        BeanProxy.doReturnWhen(getGenServiceProxy(), (m, a) -> Arrays.asList(a[0]), "findByExample");
        BeanProxy.doReturnWhen(getGenServiceProxy(), (m, a) -> Arrays.asList(a[0]), "findByExampleLike");
        BeanProxy.doReturnWhen(getGenServiceProxy(), (m, a) -> Arrays.asList(a[0]), "findBetween");
    }
}
