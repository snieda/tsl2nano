package de.tsl2.nano.h5;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.anonymous.project.Address;
import org.junit.Before;
import org.junit.Test;

import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.bean.BeanProxy;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.BeanPresentationHelper;
import de.tsl2.nano.bean.def.IPageBuilder;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ISession;
import de.tsl2.nano.h5.ARESTDynamic.Status;

public class RestUITest {
    
    @Before
    public void setUp() {
        ENV.addService(IPageBuilder.class, new Html5Presentation<>());
        ENV.addService(BeanPresentationHelper.class, new Html5Presentation<>());
		ENV.setProperty("app.login.administration", false); // -> with authentication

        Bean<Address> beanAddress = Bean.getBean(new Address(1, "Berliner Str.1", "100000", "Buxdehude", "germany"));
		BeanContainer.initEmtpyServiceActions(beanAddress.getInstance());
    }
    @Test
    public void testRestUI() {
        String url = "http://localhost:8099/restui";
        String method = "GET";
        Map<String, String> parms = new HashMap<>();
        
        // TODO: implement test with read expected
        String expected = "<html><body background=icons/spe.jpg><div style=\"border: 2px solid; float: middle; text-align: center; color: red; font-weight: bold;\">Fehler</div><pre>[#document: null]<p/>java.lang.NullPointerException: Cannot invoke \"de.tsl2.nano.bean.def.BeanDefinition.getName()\" because \"this.bean\" is null</pre></body></html>";

        String response = new MyRestUI().serve(BeanProxy.createBeanImplementation(ISession.class), url, method, new HashMap<>(), parms, null);
        assertEquals(expected, response);
    }
    @Test
    public void testEntities() {
        String url = "http://localhost:8099/restui/entities";
        String method = "GET";
        Map<String, String> parms = new HashMap<>();
        
        // TODO: implement test with read expected
        String expected = "<html><body background=icons/spe.jpg><div style=\"border: 2px solid; float: middle; text-align: center; color: red; font-weight: bold;\">Fehler</div><pre>[#document: null]<p/>java.lang.NullPointerException: Cannot invoke \"de.tsl2.nano.bean.def.BeanDefinition.getName()\" because \"this.bean\" is null</pre></body></html>";

        String response = new MyRestUI().serve(BeanProxy.createBeanImplementation(ISession.class), url, method, new HashMap<>(), parms, null);
        // assertEquals(expected, response);
    }

    @Test
    public void testBean() {
    }
    @Test
    public void testCREATE() {
        String url = "http://localhost:8099/restui/address/create";
        String method = "GET";
        Map<String, String> parms = new HashMap<>();
        
        // TODO: implement test with read expected
        String expected = "<html><body background=icons/spe.jpg><div style=\"border: 2px solid; float: middle; text-align: center; color: red; font-weight: bold;\">Fehler</div><pre>[#document: null]<p/>java.lang.NullPointerException: Cannot invoke \"de.tsl2.nano.bean.def.BeanDefinition.getName()\" because \"this.bean\" is null</pre></body></html>";

        String response = new MyRestUI().serve(BeanProxy.createBeanImplementation(ISession.class), url, method, new HashMap<>(), parms, null);
        // assertEquals(expected, response);
    }
    @Test
    public void testCHANGE() {
        String url = "http://localhost:8099/restui/address/change";
        String method = "GET";
        Map<String, String> parms = new HashMap<>();
        
        // TODO: implement test with read expected
        String expected = "<html><body background=icons/spe.jpg><div style=\"border: 2px solid; float: middle; text-align: center; color: red; font-weight: bold;\">Fehler</div><pre>[#document: null]<p/>java.lang.NullPointerException: Cannot invoke \"de.tsl2.nano.bean.def.BeanDefinition.getName()\" because \"this.bean\" is null</pre></body></html>";

        String response = new MyRestUI().serve(BeanProxy.createBeanImplementation(ISession.class), url, method, new HashMap<>(), parms, null);
        // assertEquals(expected, response);
    }

    @Test
    public void testDELETE() {
        String url = "http://localhost:8099/restui/address/delete";
        String method = "GET";
        Map<String, String> parms = new HashMap<>();
        
        // TODO: implement test with read expected
        String expected = "<html><body background=icons/spe.jpg><div style=\"border: 2px solid; float: middle; text-align: center; color: red; font-weight: bold;\">Fehler</div><pre>[#document: null]<p/>java.lang.NullPointerException: Cannot invoke \"de.tsl2.nano.bean.def.BeanDefinition.getName()\" because \"this.bean\" is null</pre></body></html>";

        String response = new MyRestUI().serve(BeanProxy.createBeanImplementation(ISession.class), url, method, new HashMap<>(), parms, null);
        // assertEquals(expected, response);
    }
}

class MyRestUI extends ARestUI<String> {

    @Override
    protected String createResponse(Status status, String mimeType, String html) {
        return html;
    }

    @Override
    protected Status getStatus(String restResponse) {
        return Status.OK;
    }

    @Override
    protected String getData(String restResponse) {
        return restResponse;
    }

    @Override
    protected String callRestService(String url, String method, Map<String, String> header, Map<String, String> parms,
            Map<String, String> payload) {
        return new MyRESTDynamic().serve(url, method, header, parms, payload);
    }
}

class MyRESTDynamic extends ARESTDynamic<String> {

    @Override
    void checkAuthorization(String beanName, String actionOrAttribute, Map<String, String> header)
            throws IllegalAccessException {
    }

    @Override
    String createResponse(Status status, String message) {
        return message;
    }

}