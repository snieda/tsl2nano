package de.tsl2.nano.core.util;

import java.util.function.Consumer;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.exception.Message;
import de.tsl2.nano.core.log.LogFactory;

/** provides a wait for response mechanism - on the old synchronized blocks with wait() and notifiy() */
public class SuppliedWait<T> {
    private static final Log LOG = LogFactory.getLog(SuppliedWait.class);
    Object waitObject;
    T response;

    public T waitOn(Object waitObject, long timeout, Consumer<T> doOnResponse) {
        try {
            if (waitObject != null) {
                LOG.info("==> " + waitObject + " waiting for response...");
                synchronized (waitObject) {
                    waitObject.wait(timeout);
                }
                LOG.info("<== " + waitObject + " notified --> continue with response: " + response);
                doOnResponse.accept(response);
            }
        } catch (Exception e) {
            Message.send(e);
        }
        return response;
    }

	public void setResponseAndNotify(T response) {
        this.response = response;
        if (waitObject != null) {
            synchronized (waitObject) {
                LOG.info("response message arrived. notifying session " + waitObject + " with response: " + response);
                waitObject.notifyAll();
            }
        }
    }
}