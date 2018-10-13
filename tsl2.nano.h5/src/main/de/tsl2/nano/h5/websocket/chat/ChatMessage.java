package de.tsl2.nano.h5.websocket.chat;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;

import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ISession;
import de.tsl2.nano.core.Main;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.exception.Message;
import de.tsl2.nano.core.messaging.EMessage;
import de.tsl2.nano.core.messaging.IListener;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.h5.NanoH5;
import de.tsl2.nano.h5.NanoH5Session;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class ChatMessage extends EMessage<Object> {
    private static final long serialVersionUID = 1L;
    byte[] attachment;
    private Collection<InetAddress> userAddresses;
    
    public <S extends ISession<?> & IListener<?>> ChatMessage(S session, Bean chatBean) {
        super(session, chatBean, "*");
        init(chatBean);
    }

    public static <S extends ISession<?> & IListener<?>> void createChatRequest(S session, Bean bean) {
        getApplication().getEventController().fireEvent(new ChatMessage(session, bean));
    }

    void init(Bean bean) {
        Chat a = (Chat) bean.getClazz().getAnnotation(Chat.class);
        destPath = Util.asString(bean.getValue(a.receiver()));
        msg = Util.isEmpty(a.message()) ? bean.getValueExpression().to(bean.getInstance()) : String.valueOf(bean.getValue(a.message()));
        String attachmentField = Util.isEmpty(a.attachment()) ? bean.getPresentable().getIconFromField() : a.attachment();
        if (attachmentField != null) {
            Object bytes = bean.getValue(attachmentField);
            if (bytes instanceof byte[])
                attachment = (byte[]) bytes;
        }
        userAddresses = findSessionAddressesWithUser(userNames());
        ManagedException.assertion(userAddresses.size() > 0, "no user found with name(s) '" + destPath + "'");
    }
    
    public void handleChatRequest(ISession<?> session) {
        if (userAddresses.contains(session.getInetAddress())) {
            if (msg != null || attachment != null) {
                if (msg != null)
                    Message.send(session.getExceptionHandler(), formatMessage(getUserName(), msg));
                //TODO: implement handling on client side...
                if (attachment != null)
                    Message.send(ByteBuffer.wrap(attachment));
            }
        }
    }

    static String formatMessage(String userName, Object msg) {
        return PRE_BROADCAST + userName + ": " + msg;
    }

    private String getUserName() {
        return ((NanoH5Session)getSource()).getUserAuthorization().getUser().toString();
    }

    private String[] userNames() {
        return destPath.split(",");
    }
    private Collection<InetAddress> findSessionAddressesWithUser(String... userNames) {
        ArrayList<InetAddress> addresses = new ArrayList<>(userNames.length);
        for (int i = 0; i < userNames.length; i++) {
            NanoH5Session session = getApplication().getSession(userNames[i]);
            if (session != null)
                addresses.add(session.getInetAddress());
        }
        return addresses;
    }

    private static NanoH5 getApplication() {
        return (NanoH5)ENV.get(Main.class);
    }

    public static boolean isChatMessage(BeanDefinition<?> bean) {
        return bean.isAnnotationPresent(Chat.class);
    }
}
