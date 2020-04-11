package de.tsl2.nano.h5.websocket.dialog;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.exception.Message;
import de.tsl2.nano.core.util.ConcurrentUtil;
import de.tsl2.nano.core.util.MapUtil;

public class WSDialogTest {

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
        WSResponse expected = createThreadSimulatingAResponse();
        WSResponse response = Message.sendAndWaitForResponse(dlg.toWSMessage(), WSResponse.class);
        assertEquals(expected, response);
    }

    private WSResponse createThreadSimulatingAResponse() {
        final String value = "passt";
        final WSResponse result = new WSResponse("passt");
        try {
            // new NanoWebSocketServer().onMessage(null, NanoWebSocketServer.TARGET_DIALOG + ":" + value);
            ConcurrentUtil.setCurrent(result);
        } catch (Exception e) {
            ManagedException.forward(e);
        }
        return result;
    }

    private WSDialog createTestDialog() {
        return new WSDialog("title", "message", new WSButton("myTestButton")).addFields(new WSField("myTestField", "empty", MapUtil.asMap("type", "textarea")));
    }
}