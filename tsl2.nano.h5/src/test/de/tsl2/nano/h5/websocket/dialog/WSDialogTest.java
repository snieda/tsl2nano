package de.tsl2.nano.h5.websocket.dialog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import org.junit.Before;
import org.junit.Test;

import de.tsl2.nano.bean.ValueHolder;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.BeanPresentationHelper;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ISession;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.exception.Message;
import de.tsl2.nano.core.util.AdapterProxy;
import de.tsl2.nano.core.util.ConcurrentUtil;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.h5.Html5Presentation;
import de.tsl2.nano.h5.websocket.NanoWebSocketServer;
import de.tsl2.nano.h5.websocket.WebSocketExceptionHandler;

public class WSDialogTest implements Serializable {
    private static final long serialVersionUID = 1L; // in cause of serializable inner classes

    @Before
    public void setUp() {
        ConcurrentUtil.reset();
    }

    @Test
    public void testDialogCreation() {
        String expected = "<dialog id=\"wsdialog.formDialog\"><form method=\"dialog\"><h3>title</h3><p>message</p><div>my Test Field<input id=\"wsdialog.input.myTestField.id\" name=\"myTestField\" type=\"textarea\" value=\"empty\"/></div><button id=\"wsdialog.button\" name=\"myTestButton\" value=\"myTestButton\">my Test Button</button></form></dialog>";
        WSDialog dlg = createTestDialog();
        String output = dlg.toHtmlDialog();
        System.out.println(expected);
        System.out.println(output);
        assertEquals(expected, output);
    }

    @Test
    public void testDialogResponse() {
        WSDialog dlg = createTestDialog();
        String expected = createThreadSimulatingAResponse("passt");
        String response = Message.sendAndWaitForResponse(dlg.toWSMessage(), String.class);
        // assertEquals(expected, response);
    }

    @Test
    public void testBeanDialogResponse() {
        ValueHolder instance = createExampleBean();

        String htmlDlg = WSDialog.createWSMessageFromBean("testTitle", instance);
        System.out.println(htmlDlg);
        Object expected = createThreadSimulatingAResponse(instance);
        Object response = Message.sendAndWaitForResponse(htmlDlg, ValueHolder.class);
        assertTrue(htmlDlg.startsWith(WSDialog.PREFIX_DIALOG + "<dialog id=\"wsdialog.formDialog\"><form method=\"dialog\">"));
        assertTrue(htmlDlg.contains(instance.getValue().toString()));
        // assertEquals(expected, response);
    }

    @Test
    public void testPrimitiveDialogResponse() {
        int defaultResponse = 99;

        String htmlDlg = WSDialog.createWSMessageFromBean("testTitle", defaultResponse);
        System.out.println(htmlDlg);
        Object expected = createThreadSimulatingAResponse(defaultResponse);
        Object response = Message.sendAndWaitForResponse(htmlDlg, Integer.class);
        // assertEquals(expected, response);
        assertTrue(htmlDlg.startsWith(WSDialog.PREFIX_DIALOG + "<dialog id=\"wsdialog.formDialog\"><form method=\"dialog\">"));
        assertTrue(htmlDlg.contains(String.valueOf(defaultResponse)));
        assertTrue(htmlDlg.contains("wsdialog.button"));
    }

    @Test
    public void testBooleanDialogResponse() {
        boolean defaultResponse = true;

        String htmlDlg = WSDialog.createWSMessageFromBean("testTitle", defaultResponse);
        System.out.println(htmlDlg);
        boolean expected = createThreadSimulatingAResponse(true);
        Boolean response = Message.sendAndWaitForResponse(htmlDlg, Boolean.class);
        // assertEquals(expected, response);
        assertTrue(htmlDlg.startsWith(WSDialog.PREFIX_DIALOG + "<dialog id=\"wsdialog.formDialog\"><form method=\"dialog\">"));
        assertTrue(htmlDlg.contains(String.valueOf(WSDialog.getYesNoButtons()[0].getName())));
        assertTrue(htmlDlg.contains("wsdialog.button"));
    }

    @Test
    public void testMessageFlow() throws UnknownHostException {
        Object instance = createExampleBean();
        ISession session = AdapterProxy.create(ISession.class);
        NanoWebSocketServer socket = new NanoWebSocketServer(session, InetSocketAddress.createUnresolved("localhost", 0));
        Thread.currentThread().setUncaughtExceptionHandler(new WebSocketExceptionHandler(socket));
        createThreadSimulatingAResponse(instance);
        Object response = Message.ask("My Question", instance);
        socket.onMessage(null, Message.hex(instance));
        System.out.println(response.toString());
    }

    private ValueHolder createExampleBean() {
        ValueHolder instance = new ValueHolder("meinTestWert") {
            public String actionMeineTestAction() {
                return "meinTestAction.run";
            }
        };
        //prepare the bean framework
        BeanDefinition bean = BeanDefinition.getBeanDefinition(ValueHolder.class);
        ENV.addService(BeanPresentationHelper.class, new Html5Presentation<>(bean));
        ENV.setProperty("websocket.use", false);
        bean.setPresentationHelper(new Html5Presentation<>(bean));
        return instance;
    }

    private <T> T createThreadSimulatingAResponse(T expectedResult) {
        try {
            // new NanoWebSocketServer().onMessage(null, NanoWebSocketServer.TARGET_DIALOG + ":" + value);
            ConcurrentUtil.setCurrent(Message.createResponse(expectedResult));
        } catch (Exception e) {
            ManagedException.forward(e);
        }
        return expectedResult;
    }

    private WSDialog createTestDialog() {
        return new WSDialog("title", "message", new WSButton("myTestButton")).addFields(new WSField("myTestField", "empty", MapUtil.asMap("type", "textarea")));
    }
}