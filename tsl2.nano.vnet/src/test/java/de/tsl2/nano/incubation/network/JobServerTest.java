package de.tsl2.nano.incubation.network;

import static junit.framework.Assert.assertNotNull;

import java.io.Serializable;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.ENVTestPreparation;

public class JobServerTest {

    @BeforeClass
    public static void setUp() {
    	ENVTestPreparation.setUp("core", false);
    }

    @AfterClass
    public static void tearDown() {
    	ENVTestPreparation.tearDown();
    }
    
   @Test
    public void testJobServer() throws Exception {
        JobServer jobServer = new JobServer();
        String result = jobServer.executeWait("test", new TestJob(), 5000);
        jobServer.close();
        assertNotNull(result);
    }

}

class TestJob implements Callable<String>, Serializable {
    /** serialVersionUID */
    private static final long serialVersionUID = 7470280926655017926L;

    @Override
    public String call() throws Exception {
        Log log = LogFactory.getLog(this.getClass());
        for (int i = 0; i < 10; i++) {
            Thread.sleep(100);
            log.info(".");
        }
        log.info("test-work done!");
        return "my-test-job";
    }
}
