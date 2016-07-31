package tsl2.nano.restaccess;

import static junit.framework.Assert.assertTrue;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import de.idv.skiller.beans.Skill;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.BeanClass;

/**
 * Unit test for RESTBeanContainer.
 */
public class RESTBeanContainerTest {
    private Client client;
    private String base;

    @Before
    public void setUp() {
        base = "http://localhost:8080/beancontainer";
    }

    @Test
    @Ignore
    public void testHeadAndOptions() throws Exception {
        String options = options();
        assertTrue(options.contains(RESTBeanContainer.class.getMethod("help", new Class[0]).getName()));
    }

    @Test
    @Ignore
    public void testFindAll() throws Exception {
        Class type = Skill.class;
        Collection skills = send(base, "/findAll/{type}/{start}/{count}", "GET", MediaType.APPLICATION_JSON, null,
            Collection.class, type.getSimpleName(), 0, 100);
    }

    @Test
    @Ignore
    public void testFindById() throws Exception {
        Class type = Skill.class;
        Skill skill = send(base, "/findById/{type}/{id}", "GET", MediaType.APPLICATION_JSON, null,
            Skill.class, type.getSimpleName(), 3);
    }

    public String options() throws Exception {
        return send(base, null, "OPTIONS", MediaType.TEXT_PLAIN, null, String.class);
    }

    protected <T> T send(String urlTarget,
            String pathTemplate,
            String method,
            String mediatype,
            Object postData,
            Class<T> responseType,
            Object... pathValues) {
        //    Client client = null;
        Response response = null;
        try {
            client = ClientBuilder.newClient();
            WebTarget target = client.target(urlTarget);
            if (pathTemplate != null)
                target = client.target(target.path(pathTemplate).getUriBuilder().build(pathValues));
            System.out.println("sending " + method + " (" + mediatype + ") " + " request: " + target.getUri());
            response = target.request().build(method, Entity.entity(postData, mediatype)).invoke();
            if (response.getStatus() >= 400)
                throw new IllegalStateException(response.getStatus() + ": " + response.getStatusInfo() + " [" + response.getMetadata() + "]");
            System.out.println("  --> response: " + response.getMetadata());
            return (T) response.getEntity();
        } finally {
            if (client != null)
                client.close();
        }
    }

    public Map<String, Object> call(String... tests) {
        HashMap<String, Object> results = new HashMap<>();
        for (int i = 0; i < tests.length; i++) {
            results.put(tests[i], BeanClass.call(this, tests[i]));
        }
        return results;
    }
    
    public static void main(String[] args) {
        RESTBeanContainerTest suite = new RESTBeanContainerTest();
        suite.setUp();

        try {
            Object result;
            if (args.length == 0)
                result = suite.options();
            else {
                result = suite.call(args);
            }
            System.out.println(result);
        } catch (Exception e) {
            ManagedException.forward(e);
        }
    }
}
