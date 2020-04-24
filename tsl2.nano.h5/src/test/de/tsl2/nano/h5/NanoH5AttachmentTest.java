package de.tsl2.nano.h5;

import static de.tsl2.nano.h5.HtmlUtil.TAG_HTML;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Map;

import javax.sql.rowset.serial.SerialBlob;
import javax.xml.parsers.DocumentBuilderFactory;

import org.anonymous.project.Party;
import org.java_websocket.WebSocket;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import de.tsl2.nano.bean.BeanProxy;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.BeanValue;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.NetUtil;
import de.tsl2.nano.h5.navigation.IBeanNavigator;
import de.tsl2.nano.h5.websocket.NanoWebSocketServer;
import de.tsl2.nano.serviceaccess.IAuthorization;

public class NanoH5AttachmentTest {

    @Test
//  @Ignore
  public void testAttachmentsTransferInH5() throws Exception {
    String filename = new File(NanoH5Test.projectPath() + "doc/beanconfigator.png").getAbsolutePath();
    NanoH5Test.createENV("attachments");
      //first: create the entity with a byte[]
      Party party = new Party();
      party.setId(1);
      byte[] b = FileUtil.getFileBytes(filename, null);
      party.setIcon(new SerialBlob(b));
      
      //simulate the socket-send from client-browser to socket-server 
      Bean<Party> bean = Bean.getBean(party);
      NanoWebSocketServer socketServer = new NanoWebSocketServer(new NanoH5Session(null, null, null, null, null, null) {
          @Override
          protected void init(NanoH5 server,
                  InetAddress inetAddress,
                  IBeanNavigator navigator,
                  ClassLoader appstartClassloader,
                  IAuthorization authorization,
                  Map context) {
              //do nothing
          }
          @Override
          public BeanDefinition getWorkingObject() {
              return bean;
          }
      }, new InetSocketAddress("localhost", NetUtil.getFreePort()));
      WebSocket webSocket = BeanProxy.createBeanImplementation(WebSocket.class, null);
      socketServer.onMessage(webSocket, createMessage("attachment", bean.getAttribute("icon").getId(), new File(filename).getAbsoluteFile().getPath(), -1, -1));
      socketServer.onMessage(webSocket, ByteBuffer.wrap(b));
      
      //check, if the file will be found on showing the bean
      Html5Presentation<Object> pres = new Html5Presentation<>();
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      Document doc = factory.newDocumentBuilder().newDocument();
      Element html = doc.createElement(TAG_HTML);
      doc.appendChild(html);
      Element field = pres.createField(html, (BeanValue<?>) bean.getAttribute("icon"), true);
      assertEquals("party.icon", field.getAttribute("id"));
      /*TODO: wieder einkommentieren!!!*/ assertEquals("file", field.getAttribute("type"));
      assertEquals("beanfieldinput", field.getAttribute("class"));
      assertEquals("this.select();", field.getAttribute("onfocus"));
      assertEquals("inputassist(event)", field.getAttribute("onkeypress"));
      assertEquals("transferattachment(this)", field.getAttribute("onchange"));

      Node img = field.getParentNode().getFirstChild();
      assertEquals("beanfielddata", img.getAttributes().getNamedItem("class").getNodeValue());
      assertEquals("party.icon.data", img.getAttributes().getNamedItem("id").getNodeValue());
      String imgSrc = img.getAttributes().getNamedItem("src").getNodeValue();
      assertTrue(new File(imgSrc).exists());
      pres.reset();
  }

    private String createMessage(String target, String id, String value, int clientX, int clientY) {
        return '/' + target + '@' + id + '?' + clientX + ',' + clientY + ':'
                + value;
    }
    
}
