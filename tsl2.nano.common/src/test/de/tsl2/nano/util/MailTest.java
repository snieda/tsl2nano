package de.tsl2.nano.util;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

import org.junit.Ignore;
import org.junit.Test;

public class MailTest {

	public void forbidSystemExit() {
		System.setSecurityManager(new SecurityManager() {
			@Override
			public void checkExit(int status) {
				throw new IllegalStateException("systemexit");
			}
		});
	}
	@Test
	@Ignore("Problems on maven fork")
	public void testSendMail() {
		forbidSystemExit();
		try {
			Mail.main("mail.gmx.net MeinName@MeinProvider.de \"hello world\" x@y.z xyz S T".split("\\s"));
		} catch (Exception ex) {
			assertTrue(ex.getMessage().contains("systemexit"));
		}
	}

	@Test
	@Ignore("Problems on maven fork")
	public void testSendLocalhost() throws IOException {
		ServerSocket serverSocket = null;
		try {
			// a port below 1024 requires root privileges
			serverSocket = new ServerSocket(1024, 0, InetAddress.getLocalHost());
//			serverSocket.accept();
			
			forbidSystemExit();
			Mail.main("localhost MeinName@MeinProvider.de \"hello world\" x@y.z xyz S T".split("\\s"));
		} catch(Exception ex) {
			//TODO: how to create a real test on port 25
			assertTrue(ex.getMessage().contains("systemexit"));
		} finally {
			if (serverSocket != null)
				serverSocket.close();
		}
	}

}
