package de.tsl2.nano.util.autotest.creator;

import static de.tsl2.nano.autotest.creator.AutoTest.FILTER;
import static de.tsl2.nano.autotest.creator.AutoTest.FILTER_EXCLUDE;
import static de.tsl2.nano.autotest.creator.InitAllAutoTests.matchPackage;
import static de.tsl2.nano.autotest.creator.InitAllAutoTests.methods;
import static de.tsl2.nano.autotest.creator.InitAllAutoTests.set;

import java.lang.reflect.Proxy;
import java.util.Arrays;

import javax.persistence.EntityManager;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import de.tsl2.nano.autotest.creator.AutoFunctionTest;
import de.tsl2.nano.autotest.creator.CurrentStatePreservationTest;
import de.tsl2.nano.autotest.creator.InitAllAutoTests;
import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.bean.BeanProxy;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.util.ConcurrentUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.persistence.DatabaseTool;
import de.tsl2.nano.persistence.GenericLocalBeanContainer;
import de.tsl2.nano.persistence.Persistence;
import de.tsl2.nano.persistence.provider.NanoEntityManagerFactory;
import de.tsl2.nano.persistence.replication.Replication;
import de.tsl2.nano.script.ScriptTool;
import de.tsl2.nano.service.util.BeanContainerUtil;
import de.tsl2.nano.service.util.FileServiceBean;
import de.tsl2.nano.service.util.IGenericService;
import de.tsl2.nano.service.util.RemoteServiceRunner;
import de.tsl2.nano.serviceaccess.Authorization;
import de.tsl2.nano.serviceaccess.IAuthorization;
import de.tsl2.nano.serviceaccess.ServiceFactory;
import de.tsl2.nano.serviceaccess.ServiceProxy;

@RunWith(Suite.class)
@SuiteClasses({InitAllAutoTests.class, AutoFunctionTest.class, CurrentStatePreservationTest.class})
public class AllAutoTests {
	public static void init() {
		// setSequentialForDebugging();
		ConfigBeanContainer.initAuthAndLocalBeanContainer();
				set(FILTER, matchPackage(Persistence.class, ScriptTool.class,
				NanoEntityManagerFactory.class, FileServiceBean.class));
		set(FILTER_EXCLUDE, StringUtil.matchingOneOf(
			methods(RemoteServiceRunner.class, "start|getService"),
			methods(DatabaseTool.class, "shutdown"),
			methods(GenericLocalBeanContainer.class, "initLocalContainer"),
			methods(Replication.class, "save")));
	}
}

//copy of ConfigBeanContainer in directacccess
class ConfigBeanContainer {

	public static EntityManager initAuthAndLocalBeanContainer() {
		BeanContainerUtil.initEmptyProxyServices();
		BeanContainerUtil.initProxyServiceFactory();
		ServiceFactory.instance().setSubject(initUserAuth().getSubject());
		ConcurrentUtil.setCurrent(BeanContainer.instance());
		return ENV.addService(EntityManager.class, BeanProxy.createBeanImplementation(EntityManager.class));
	}

	public static Authorization initUserAuth() {
		Authorization auth = Authorization.create("SA", false);
		ENV.addService(IAuthorization.class, auth);
		ConcurrentUtil.setCurrent(auth);
		return auth;
	}

	/**
	 * use that only, if you called initProxyBeanContainer() before!
	 */
	public static IGenericService getGenServiceProxy() {
		return (IGenericService) ((ServiceProxy) Proxy.getInvocationHandler(ServiceFactory.getGenService()))
				.delegationProxy();
	}

	public static void simpleReturnExampleItself() {
		BeanProxy.doReturnWhen(getGenServiceProxy(), (m, a) -> Arrays.asList(a[0]), "findByExample");
		BeanProxy.doReturnWhen(getGenServiceProxy(), (m, a) -> Arrays.asList(a[0]), "findByExampleLike");
		BeanProxy.doReturnWhen(getGenServiceProxy(), (m, a) -> Arrays.asList(a[0]), "findBetween");
	}
}
