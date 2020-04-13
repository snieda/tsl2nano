package de.tsl2.nano.h5.websocket.dialog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.net.UnknownHostException;

import org.junit.Ignore;
import org.junit.Test;

import de.tsl2.nano.bean.ValueHolder;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.BeanPresentationHelper;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.exception.Message;
import de.tsl2.nano.core.util.ConcurrentUtil;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.h5.Html5Presentation;
import de.tsl2.nano.h5.websocket.NanoWebSocketServer;
import de.tsl2.nano.h5.websocket.WebSocketExceptionHandler;

public class WSDialogTest implements Serializable {
    private static final long serialVersionUID = 1L; // in cause of serializable inner classes

    @Test
    public void testDialogCreation() {
        String expected = "<dialog id=\"formDialog\"><form method=\"dialog\"><h2>title</h2><p>message</p><input id=\"input.id\" name=\"myTestField\" type=\"textarea\" value=\"empty\"/><button id=\"button.id\" name=\"myTestButton\" value=\"myTestButton\"/></form></dialog>";
        WSDialog dlg = createTestDialog();
        String output = dlg.toHtmlDialog();
        System.out.println(output);
        assertEquals(expected, output);
    }

    @Test
    public void testDialogResponse() {
        WSDialog dlg = createTestDialog();
        WSResponse expected = createThreadSimulatingAResponse(new WSResponse("passt"));
        WSResponse response = Message.sendAndWaitForResponse(dlg.toWSMessage(), WSResponse.class);
        assertEquals(expected, response);
    }

    @Test
    @Ignore
    public void testBeanDialogResponse() {
        ValueHolder instance = createExampleBean();

        String htmlDlg = WSDialog.createWSMessageFromBean("testTitle", instance);
        System.out.println(htmlDlg);
        WSResponse expected = createThreadSimulatingAResponse(new WSResponse("passt"));
        WSResponse response = Message.sendAndWaitForResponse(htmlDlg, WSResponse.class);
        assertTrue(htmlDlg.startsWith(WSDialog.PREFIX_DIALOG + "<dialog id=\"formDialog\"><form method=\"dialog\">"));
        assertTrue(htmlDlg.contains(instance.getValue().toString()));
    }

    @Test
    public void testMessageFlow() throws UnknownHostException {
        Object instance = createExampleBean();
        NanoWebSocketServer socket = new NanoWebSocketServer();
        Thread.currentThread().setUncaughtExceptionHandler(new WebSocketExceptionHandler(socket));
        createThreadSimulatingAResponse(instance);
        // Object response = Message.ask(instance);
        // socket.onMessage(null, Message.hex(instance));
        // System.out.println(response.toString());
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
            ConcurrentUtil.setCurrent(expectedResult);
        } catch (Exception e) {
            ManagedException.forward(e);
        }
        return expectedResult;
    }

    private WSDialog createTestDialog() {
        return new WSDialog("title", "message", new WSButton("myTestButton")).addFields(new WSField("myTestField", "empty", MapUtil.asMap("type", "textarea")));
    }
}