package de.tsl2.nano.util;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.security.AccessControlException;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class MailTest {
	private boolean systemExitForbidden;

	public void forbidSystemExit() {
		systemExitForbidden = true;
		System.setSecurityManager(new SecurityManager() {
			@Override
			public void checkExit(int status) {
				if (systemExitForbidden)
					throw new IllegalStateException("systemexit:" + status);
			}
		});
	}
	
	// @Before
	public void setUp() {
		try {
			forbidSystemExit();
		} catch(Exception ex) {
			//Ok, nur der erste Aufruf funktioniert
		}
	}
	@Test
	@Ignore("Problems on maven fork with System.exit()")
	public void testSendMail() {
		try {
			Mail.main("mail.gmx.net MeinName@MeinProvider.de \"hello world\" x@y.z xyz S T".split("\\s"));
		} catch (Exception ex) {
			assertTrue(ex.getMessage().contains("systemexit:2"));
		} finally {
			systemExitForbidden = false;
		}
	}

	@Test
	@Ignore("Problems on maven fork with System.exit()")
	public void testSendLocalhost() throws IOException {
		ServerSocket serverSocket = null;
		try {
			// a port below 1024 requires root privileges
			serverSocket = new ServerSocket(1024, 0, InetAddress.getLocalHost());
//			serverSocket.accept();
			
			Mail.main("localhost MeinName@MeinProvider.de \"hello world\" x@y.z xyz S T".split("\\s"));
		} catch(Exception ex) {
			//TODO: how to create a real test on port 25
			assertTrue(ex.getMessage().contains("localhost:1024"));
		} finally {
			if (serverSocket != null)
				serverSocket.close();
			systemExitForbidden = false;
		}
	}

}
