package de.tsl2.nano.h5;

import static org.junit.Assert.*;

import java.io.File;

import javax.ws.rs.Path;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.sun.jersey.api.container.httpserver.HttpServerFactory;
import com.sun.net.httpserver.HttpServer;

import de.tsl2.nano.core.execution.Profiler;
import de.tsl2.nano.core.util.ENVTestPreparation;
import de.tsl2.nano.core.util.NetUtil;

@Ignore("since changes of 2.4.3-SNAPSHOT in october, there is perhaps a collision with NanoH5Test")
public class NanoH5NetUtilTest implements ENVTestPreparation {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() {
		ENVTestPreparation.super.setUp("h5");
	}
	
	@Test
	@Path("/")
	public void testNetUtilRestful() throws Exception {
		String url = "http://localhost:9999/rest";
		Class<?> responseType = String.class;
//        Event event =
//            BeanProxy.createBeanImplementation(Event.class, MapUtil.asMap("type", "mouseclick", "target", null), null,
//                null);
		// TODO: how to provide parameter of any object type?
//        Point p = new Point(5,5);
		Object args[] = new Object[] { "event", "x", 5, "y", 5 };

		// create the server (see service class RestfulService, must be public!)
		HttpServer server = HttpServerFactory.create(url);
		server.start();

		// request..
		String response = NetUtil.getRest(url/* , responseType */, args);
		server.stop(0);

		assertTrue(response != null && responseType.isAssignableFrom(response.getClass()));
		assertTrue(response.equals("5, 5"));
	}

//  @Ignore("don't do that automatically") 
	@Test
	public void testNetUtilDownload() throws Exception {
		// the test checks the current download path of sourceforge...
		if (NetUtil.isOnline()) {
			Profiler.si().stressTest("downloader", 1, new Runnable() {
				@Override
				public void run() {
					String url;
					// https://sourceforge.net/projects/tsl2nano/files/latest/download?source=navbar
					// http://downloads.sourceforge.net/project/tsl2nano/1.1.0/tsl2.nano.h5.1.1.0.jar
					// http://netcologne.dl.sourceforge.net/project/tsl2nano/1.1.0/tsl2.nano.h5.1.1.0.jar
					// https://iweb.dl.sourceforge.net/project/tsl2nano/1.1.0/tsl2.nano.h5.1.1.0.jar
					File download = NetUtil.download(
							url = "https://sourceforge.net/projects/tsl2nano/files/latest/download", "target/test/",
							true, true);
					NetUtil.check(url, download, 3 * 1024 * 1024);
				}
			});
		}
	}

}
