package de.tsl2.nano.network;

import static org.junit.Assert.assertEquals;

import java.io.Serializable;
import java.net.Socket;
import java.net.URI;
import java.util.concurrent.Callable;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.tsl2.nano.core.util.ConcurrentUtil;
import de.tsl2.nano.core.util.ENVTestPreparation;
import de.tsl2.nano.core.util.NetUtil;
import de.tsl2.nano.network.JobContext;
import de.tsl2.nano.network.JobServer;
import de.tsl2.nano.network.Request;

public class JobServerTest {

    @BeforeClass
    public static void setUp() {
    	ENVTestPreparation.setUp("vnet", false);
    }

    @AfterClass
    public static void tearDown() {
    	ENVTestPreparation.tearDown();
    }
    
   @Test
    public void testJobServerDirectly() throws Exception {
        JobServer jobServer = new JobServer(NetUtil.getFreePort());
        String result = jobServer.executeWait("test", new TestJob(), 5000);
        jobServer.close();
        assertEquals("my-test-job", result);
    }

    @Test
    public void testJobServerThroughSocket() throws Exception {
        JobServer jobServer = new JobServer();
        ConcurrentUtil.sleep(500);

        Socket socket = new Socket("127.0.0.1", 9876);
        ConcurrentUtil.sleep(500);
        JobContext<String> jobContext = new JobContext<>("test", new TestJob(), null, URI.create("file://./").toURL());

        NetUtil.send(socket, jobContext);
        ConcurrentUtil.sleep(500);

        Request result = NetUtil.request(socket, Request.class, new Request(Request.RESULT));
        result.setProgress(Request.PROGRESS_SENT);
        jobServer.close();
        socket.close();

        assertEquals("my-test-job", result.getResponse());
    }

}

class TestJob implements Callable<String>, Serializable {
    /** serialVersionUID */
    private static final long serialVersionUID = 7470280926655017926L;

    @Override
    public String call() throws Exception {
        for (int i = 0; i < 10; i++) {
            Thread.sleep(100);
            System.out.print(".");
        }
        System.out.println("\ntest-work done!");
        return "my-test-job";
    }
}

