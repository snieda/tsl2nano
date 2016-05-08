package tsl2.nano.restaccess;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.tsl2.nano.core.ManagedException;
import tsl2.nano.restaccess.RESTBeanContainer;

import static junit.framework.Assert.*;

/**
 * Unit test for RESTBeanContainer.
 */
public class RESTBeanContainerTest {
    private Client client;
    private WebTarget res;

    @Before
    public void setUp() {
//        client = ClientBuilder.newClient();
//        res = client.target("http://localhost:8080/beancontainer");
    }

    @Test
    public void testHeadAndOptions() throws Exception {
//        String options = options(MediaType.APPLICATION_JSON, String.class);
//        assertTrue(options.contains(RESTBeanContainer.class.getMethod("help", new Class[0]).getName()));
    }
    public <T> T options(String mediaType, Class<T> entityType) throws Exception {
        Response response = res.request(mediaType).options();
        T result = response.readEntity(entityType);
        response.close();
        return result;
    }

    /**
     * call
     * @param args
     */
    private Object call(String[] args) {
        Response response = res.request(MediaType.APPLICATION_JSON).build(args[0]).property(args[1], args[2]).invoke();
        return response.getEntity();
    }

    public static void main(String[] args) {
        RESTBeanContainerTest suite = new RESTBeanContainerTest();
        suite.setUp();
        
        try {
            Object result;
            if (args.length == 0)
                result = suite.options(MediaType.APPLICATION_JSON, String.class);
            else {
                result = suite.call(args);
            }
            System.out.println(result);
        } catch (Exception e) {
            ManagedException.forward(e);
        }
    }
}
