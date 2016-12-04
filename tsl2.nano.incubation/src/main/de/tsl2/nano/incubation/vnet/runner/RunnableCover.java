package de.tsl2.nano.incubation.vnet.runner;

import java.io.Serializable;
import java.util.Map;

import de.tsl2.nano.execution.IPRunnable;
import de.tsl2.nano.incubation.vnet.ILocatable;
import de.tsl2.nano.incubation.vnet.Notification;
import de.tsl2.nano.messaging.IListener;
import de.tsl2.nano.structure.Cover;

/**
 * covers an {@link IPRunnable} (perhaps to run a script) to be used in vnet.
 * 
 * @param <T>
 * @param <D>
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
class RunnableCover<T extends IPRunnable<Object, Map<String, Object>> & Serializable & Comparable<? super T>, D extends Comparable<? super D>>
        extends
        Cover<T, D> implements ILocatable, IListener<Notification> {

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    public RunnableCover(T core, D descriptor) {
        super(core, descriptor);
    }

    @Override
    public void handleEvent(Notification event) {
        //do something like a calculation
        Object result = getContent().run(event.getResponse(), event.getNotification());
        event.addResponse(getPath(), result);
    }

    @Override
    public String getPath() {
        return toString();
    }

}
