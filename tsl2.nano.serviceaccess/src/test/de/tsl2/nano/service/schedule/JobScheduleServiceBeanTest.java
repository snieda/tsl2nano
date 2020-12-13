package de.tsl2.nano.service.schedule;

import static org.junit.Assert.*;

import java.io.Serializable;
import java.util.Arrays;

import javax.ejb.Timer;
import javax.ejb.TimerService;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.tsl2.nano.bean.BeanProxy;
import de.tsl2.nano.core.cls.PrivateAccessor;
import de.tsl2.nano.core.execution.ICRunnable;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.serviceaccess.ServiceFactory;

public class JobScheduleServiceBeanTest {
	static boolean ran = false;
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
        System.setProperty(ServiceFactory.NO_JNDI, Boolean.toString(true));
		ServiceFactory.create(Thread.currentThread().getContextClassLoader());
		ServiceFactory.instance().setInitialServices(MapUtil.asMap(TestRunner.class.getName(), BeanProxy.createBeanImplementation(TestRunner.class)));
        System.setProperty("java.security.manager", "");
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void testJobScheduler() {
		JobScheduleServiceBean scheduler = new JobScheduleServiceBean();
		
		PrivateAccessor<JobScheduleServiceBean> acc = new PrivateAccessor<JobScheduleServiceBean>(scheduler);
		Timer timer = BeanProxy.createBeanImplementation(Timer.class, MapUtil.asMap("info", "test"));
		acc.set("timerService", BeanProxy.createBeanImplementation(TimerService.class, 
				MapUtil.asMap("timers", Arrays.asList(timer), "createTimer", timer)));
		scheduler.createJob("test", System.currentTimeMillis(), false, TestRunner.class);
		acc.call("initializePersistedJobs", null);
		Serializable context = null;
		//TODO: einkommentieren
//		scheduler.run(TestRunner.class , context);
//		assertTrue(ran);
	}

}

interface TestRunner extends ICRunnable<Serializable> {
	@Override
	public default Serializable run(Serializable context, Object... extArgs) {
		JobScheduleServiceBeanTest.ran = true;
		return null;
	}
	
}
