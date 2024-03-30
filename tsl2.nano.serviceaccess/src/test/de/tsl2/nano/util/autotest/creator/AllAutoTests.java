package de.tsl2.nano.util.autotest.creator;

import static de.tsl2.nano.autotest.creator.InitAllAutoTests.matchPackage;

import java.lang.reflect.Proxy;
import java.util.Arrays;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import de.tsl2.nano.autotest.ValueRandomizer;
import de.tsl2.nano.autotest.creator.ADefaultAutoTester;
import de.tsl2.nano.autotest.creator.AutoFunctionTest;
import de.tsl2.nano.autotest.creator.CurrentStatePreservationTest;
import de.tsl2.nano.autotest.creator.InitAllAutoTests;
import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.bean.BeanProxy;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.util.ConcurrentUtil;
import de.tsl2.nano.core.util.DependencyInjector;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.resource.fs.FsConnection;
import de.tsl2.nano.service.util.BeanContainerUtil;
import de.tsl2.nano.service.util.GenericServiceBean;
import de.tsl2.nano.service.util.IGenericService;
import de.tsl2.nano.service.util.ServiceUtil;
import de.tsl2.nano.serviceaccess.Authorization;
import de.tsl2.nano.serviceaccess.IAuthorization;
import de.tsl2.nano.serviceaccess.ServiceFactory;
import de.tsl2.nano.serviceaccess.ServiceProxy;

@RunWith(Suite.class)
@SuiteClasses({InitAllAutoTests.class, AutoFunctionTest.class, CurrentStatePreservationTest.class})
public class AllAutoTests {

	public static void init() {
		ConfigBeanContainer.initAuthAndLocalBeanContainer();

		System.setProperty("tsl2.functiontest.filter",
				matchPackage(FsConnection.class, ServiceUtil.class, ServiceFactory.class, GenericServiceBean.class));
		System.setProperty("tsl2.functiontest.filter.exclude", ".*(FsManagedConnection.setLogWriter|FsManagedConnectionFactory.setLogWriter|PrintWriter|DefaultService.getSubject).*");
		System.setProperty(ADefaultAutoTester.KEY_AUTOTEST_INITMOCKITO, "false");
		System.setProperty(DependencyInjector.KEY_INJECTORANNOTATIONS,
				StringUtil.trim(Arrays.toString(ADefaultAutoTester.DEFAULT_MOCK_CLASSNAMES), "{}[]"));

		ValueRandomizer.setDependencyInjector(new DependencyInjector(
				Arrays.asList(Resource.class, PersistenceContext.class, jakarta.persistence.PersistenceContext.class),
				Arrays.asList(DependencyInjector.Producer.class), null));
	}
}

//copy of ConfigBeanContainer in directacccess
class ConfigBeanContainer {
	public static EntityManager initAuthAndLocalBeanContainer() {
		return initProxyBeanContainer(initUserAuth());
	}

	public static EntityManager initProxyBeanContainer(Authorization auth) {
		BeanContainerUtil.initEmptyProxyServices();
		BeanContainerUtil.initProxyServiceFactory();
		ServiceFactory.instance().setSubject(auth.getSubject());
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
