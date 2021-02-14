package de.tsl2.nano.h5;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.tsl2.nano.core.http.HttpClient;
import de.tsl2.nano.core.util.StringUtil;

public class NanoHTTPDTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void testFileServerSmoke() {
		System.setIn(new ByteArrayInputStream( "\n".getBytes() ));
		NanoHTTPD.main(new String[0]);
		HttpClient httpClient = new HttpClient("http://localhost:8080");
		InputStream response = httpClient.send("GET", "text/html", null);
		System.out.println(StringUtil.fromInputStream(response));
	}

}
