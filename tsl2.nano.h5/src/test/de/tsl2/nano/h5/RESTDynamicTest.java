package de.tsl2.nano.h5;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.anonymous.project.Address;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.bean.BeanUtil;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.util.ByteUtil;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.h5.NanoHTTPD.Response;

public class RESTDynamicTest {

	@Before
	public void setUp() {
		NanoH5Test.createENV("restdynamic");
		ENV.setProperty("app.login.administration", false); // -> with authentication
        Bean<Address> beanAddress = Bean.getBean(new Address(1, "Berliner Str.1", "100000", "Buxdehude", "germany"));
		BeanContainer.initEmtpyServiceActions(beanAddress.getInstance());
	}
	
	@After
	public void tearDown() {
		ENV.reset();
		Bean.clearCache();
		BeanContainer.reset();
	}
	
	private Map<String, String> header(String url, String method) {
		String digest = new RESTDynamic().createDigest(url, method, "test2020-05-10");
		System.out.println("digest: " + digest);
		return MapUtil.asMap("authorization", "test 2020-05-10 " + digest, "user", "test", "password", "test");
	}
	
	@Test
	public void testHelp() {
		String url = RESTDynamic.BASE_PATH;
		assertTrue(RESTDynamic.canRest(url));
		Response response = new RESTDynamic().serve(url, "GET", null, null);
		String result = ByteUtil.toString(response.getData(), "UTF8");
		System.out.println(result);
		assertTrue(result.contains("RESTDynamic"));
	}
	
	@Test
	public void testDeactivated() {
		System.setProperty("app.rest.active", "false");
		String url = RESTDynamic.BASE_PATH;
		assertFalse(RESTDynamic.canRest(url));
	}
	
	@Test
	public void testWrongMethod() {
		String url = RESTDynamic.BASE_PATH + "/irgendwas";
		assertTrue(RESTDynamic.canRest(url));
		Response response = new RESTDynamic().serve(url, "XXX", header(url, "XXX"), null);
		String result = ByteUtil.toString(response.getData(), "UTF8");
		System.out.println(result);
		assertTrue(result.contains(RESTDynamic.METHODS));
	}
	
	@Test
	public void testWrongAuthenticationNoHeader() {
		String url = RESTDynamic.BASE_PATH + "/irgendwas";
		assertTrue(RESTDynamic.canRest(url));
		Response response = new RESTDynamic().serve(url, "XXX", null, null);
		assertEquals(Response.Status.FORBIDDEN, response.getStatus());
	}
	
	@Test
	public void testWrongAuthenticationWrongApiKey() {
		String url = RESTDynamic.BASE_PATH + "/irgendwas";
		Map<String, String> header = MapUtil.asMap("Authorization", "falscherapikey");
		assertTrue(RESTDynamic.canRest(url));
		Response response = new RESTDynamic().serve(url, "XXX", header, null);
		assertEquals(Response.Status.FORBIDDEN, response.getStatus());
	}
	
	@Test
	public void testWrongAuthenticationWrongUser() {
		String url = RESTDynamic.BASE_PATH + "/irgendwas";
		assertTrue(RESTDynamic.canRest(url));
		Map<String, String> header = header(url, "XXX");
		header.remove("user");
		Response response = new RESTDynamic().serve(url, "XXX", header, null);
		assertEquals(Response.Status.FORBIDDEN, response.getStatus());
	}
	
	@Ignore("TODO: under construction...")
	@Test
	public void testWrongAuthorization() {
		String url = RESTDynamic.BASE_PATH + "/address/id/1";
		assertTrue(RESTDynamic.canRest(url));
		Response response = new RESTDynamic().serve(url, "GET", header(url, "GET"), null);
		assertEquals(Response.Status.UNAUTHORIZED, response.getStatus());
	}
	
	@Test
	public void testWrongGET() {
		String url = RESTDynamic.BASE_PATH + "/irgendwas";
		assertTrue(RESTDynamic.canRest(url));
		Response response = new RESTDynamic().serve(url, "GET", header(url, "GET"), null);
		assertEquals(Response.Status.BAD_REQUEST, response.getStatus());
	}
	
	@Test
	public void testWrongPUT() {
		String url = RESTDynamic.BASE_PATH + "/entity/attribute/query/output";
		assertTrue(RESTDynamic.canRest(url));
		Response response = new RESTDynamic().serve(url, "PUT", header(url, "PUT"), null);
		assertEquals(Response.Status.BAD_REQUEST, response.getStatus());
	}
	
	@Test
	public void testWrongDELETE() {
		String url = RESTDynamic.BASE_PATH + "/entity/attribute/query/output";
		assertTrue(RESTDynamic.canRest(url));
		Response response = new RESTDynamic().serve(url, "DELETE", header(url, "DELETE"), null);
		assertEquals(Response.Status.BAD_REQUEST, response.getStatus());
	}
	
	@Test
	public void testWrongPOST() {
        Bean<Address> beanAddress = Bean.getBean(new Address(1, "Berliner Str.1", "100000", "Buxdehude", "germany"));
		String url = "rest/address/create";
		String method = "POST";
		
		Map<String, String> parms = MapUtil.asMap("xxxx", BeanUtil.toJSON(beanAddress));
		Response response = new RESTDynamic().serve(url, method, header(url, method), parms);
		assertEquals(Response.Status.BAD_REQUEST, response.getStatus());
	}

	@Test
	public void testPOST() {
        Bean<Address> beanAddress = Bean.getBean(new Address(1, "Berliner Str.1", "100000", "Buxdehude", "germany"));
		String url = "/rest/address/create";
		String method = "POST";
		
		assertTrue(RESTDynamic.canRest(url));
		Map<String, String> payload = MapUtil.asMap("postData", BeanUtil.toJSON(beanAddress.getInstance()));
		Response response = new RESTDynamic().serve(url, method, header(url, method), null, payload);
		assertEquals(NanoHTTPD.MIME_PLAINTEXT, response.getMimeType());
		assertEquals("CREATED", response.getStatus().toString());
		String result = ByteUtil.toString(response.getData(), "UTF8");
		assertTrue(result.contains(String.valueOf(beanAddress.getId())));
	}

	@Test
	public void testGET() {
		String url = RESTDynamic.BASE_PATH + "/address/id/1";
		assertTrue(RESTDynamic.canRest(url));
		Response response = new RESTDynamic().serve(url, "GET", header(url, "GET"), null);
		assertEquals(Response.Status.OK, response.getStatus());
		String result = ByteUtil.toString(response.getData(), "UTF8");
		assertTrue(result.contains("Buxdehude"));
	}
	
	@Test
	public void testGETWithOutputAttribute() {
		String url = RESTDynamic.BASE_PATH + "/address/city/Buxdehude/street";
		assertTrue(RESTDynamic.canRest(url));
		Response response = new RESTDynamic().serve(url, "GET", header(url, "GET"), null);
		assertEquals(Response.Status.OK, response.getStatus());
		String result = ByteUtil.toString(response.getData(), "UTF8");
		assertEquals("Berliner Str.1", result);
	}
	
	@Test
	public void testPUT() {
		String url = RESTDynamic.BASE_PATH + "/address/city/Buxdehude/city/Berlin";
		assertTrue(RESTDynamic.canRest(url));
		Response response = new RESTDynamic().serve(url, "PUT", header(url, "PUT"), null);
		assertEquals(Response.Status.OK, response.getStatus());
		String result = ByteUtil.toString(response.getData(), "UTF8");
		assertEquals("changed: Address", result);
	}
	
	@Test
	public void testDELETE() {
		String url = RESTDynamic.BASE_PATH + "/address/id/1";
		assertTrue(RESTDynamic.canRest(url));
		Response response = new RESTDynamic().serve(url, "DELETE", header(url, "DELETE"), null);
		assertEquals(Response.Status.OK, response.getStatus());
		String result = ByteUtil.toString(response.getData(), "UTF8");
		assertEquals("deleted: Address", result);
	}
	
}
