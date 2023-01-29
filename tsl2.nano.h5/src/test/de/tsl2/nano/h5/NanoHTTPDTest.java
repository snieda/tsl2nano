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
		String port = "8069";
		NanoHTTPD.main(new String[] {"-p", port});
		HttpClient httpClient = new HttpClient("http://localhost:" + port);
		InputStream response = httpClient.send("GET", "text/html", null);
		System.out.println(StringUtil.fromInputStream(response));
	}

}
