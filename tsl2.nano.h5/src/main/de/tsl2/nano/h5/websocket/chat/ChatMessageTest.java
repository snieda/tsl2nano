package de.tsl2.nano.h5.websocket.chat;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.InetAddress;
import java.security.Principal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.Subject;

import org.junit.Test;

import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.exception.ExceptionHandler;
import de.tsl2.nano.core.util.NetUtil;
import de.tsl2.nano.h5.NanoH5;
import de.tsl2.nano.h5.NanoH5Session;
import de.tsl2.nano.service.util.IPersistable;
import de.tsl2.nano.serviceaccess.IAuthorization;

public class ChatMessageTest {
	
	@Test
	public void testChatMessages() throws IOException {
		Chatty c = new Chatty();
		c.setTo("TestReceiver");
		c.setMessage("TestMessage");
		Bean<Chatty> bean = Bean.getBean(c);

		//create the collection to use it inside the anonymous class
		Map<InetAddress, NanoH5Session> sessions = new HashMap<>();
		NanoH5 mainApp = new NanoH5() {
			@Override
			public NanoH5Session getSession(String userName) {
				getSessions().put(sessions.keySet().iterator().next(), sessions.values().iterator().next());
				return super.getSession(userName);
			}
		};
		IAuthorization auth = createAuth(c);
		ENV.setProperty("websocket.use", false); //to have access to the standard exception handler
		//		MySession session = new MySession(mainApp);
		NanoH5Session session = NanoH5Session.createSession(mainApp, NetUtil.getInetAddress(), null, Thread.currentThread().getContextClassLoader(), auth, new HashMap<>());
		sessions.put(session.getInetAddress(), session);
		
		if (ChatMessage.isChatMessage(bean)) {
			ChatMessage.createChatRequest(session, bean);
		}
		assertEquals(ChatMessage.formatMessage(auth.getUser().toString(), c.getMessage()), ((ExceptionHandler)session.getExceptionHandler()).getExceptions().iterator().next().getMessage());
	}

	private IAuthorization createAuth(Chatty c) {
		return new IAuthorization() {
			
			@Override
			public boolean hasRole(String roleName) {
				return false;
			}
			
			@Override
			public boolean hasPrincipal(Principal principal) {
				return false;
			}
			
			@Override
			public boolean hasAccess(String name, String action) {
				return false;
			}
			
			@Override
			public Object getUser() {
				return c.getTo();
			}
			
			@Override
			public Subject getSubject() {
				return null;
			}
		};
	}

}

@Chat(receiver="to", message="message", attachment="attachment")
class Chatty implements IPersistable<String> {
	private static final long serialVersionUID = 1L;
	private String id;
	private Date sent;
	private String from;
	private String to;
	private String message;
	private byte[] attachment;
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public Date getSent() {
		return sent;
	}
	public void setSent(Date sent) {
		this.sent = sent;
	}
	public String getFrom() {
		return from;
	}
	public void setFrom(String from) {
		this.from = from;
	}
	public String getTo() {
		return to;
	}
	public void setTo(String to) {
		this.to = to;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public byte[] getAttachment() {
		return attachment;
	}
	public void setAttachment(byte[] attachment) {
		this.attachment = attachment;
	}
	public static long getSerialversionuid() {
		return serialVersionUID;
	}
}