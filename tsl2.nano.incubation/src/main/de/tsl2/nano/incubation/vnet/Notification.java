/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: ts
 * created on: 11.11.2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.incubation.vnet;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.tsl2.nano.messaging.EventController;
import de.tsl2.nano.messaging.IListener;

/**
 * a notification will be sent to all recipients fulfilling the given path of {@link ILocatable#getPath()}. the creator
 * of that notification may register itself as listener to the response of the notification. each network node, becoming
 * this notification may add a response to this notification.
 * 
 * @author ts
 * @version $Revision$
 */
public class Notification implements ILocatable {
    /** path expression to constrain the network nodes to be notified */
    String path;
    /** notification content */
    Object notification;
    /** map of all nodes responses */
    Map<String, Object> response;
    /** registered listeners to node responses */
    EventController responseController;

    /**
     * constructor
     * @param path for nodes to be notified. if null, all nodes will be notified.
     * @param notification the notification content
     */
    public Notification(String path, Object notification) {
        this(path, notification, null, null);
    }

    /**
     * constructor
     * @param path for nodes to be notified
     * @param notification the notification content
     * @param origin notification to be copied.
     */
    public Notification(String path, Object notification, Notification origin) {
        this(path, notification, origin.response, null);
        responseController = origin.responseController;
    }

    /**
     * constructor
     * @param path for nodes to be notified
     * @param notification the notification content
     * @param response any node response
     * @param responseListener listeners to node responses
     */
    public Notification(String path,
            Object notification,
            Map<String, Object> response,
            IListener<Notification> responseListener) {
        super();
        this.notification = notification;
        this.path = path;
        this.response = response;
        if (responseListener != null)
            getResponseController().addListener(responseListener);
    }

    /**
     * @return Returns the notification.
     */
    public Object getNotification() {
        return notification;
    }

    /**
     * @param notification The notification to set.
     */
    public void setNotification(Object notification) {
        this.notification = notification;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPath() {
        return path;
    }

    /**
     * evaluates , if the given object should be notified with current notification
     * 
     * @param obj object to evaluate
     * @return true, if given object should be notified
     */
    public boolean notifiy(ILocatable obj) {
        return path == null || obj.getPath().startsWith(path);
    }

    /**
     * getResponse
     * @return all responses at this time
     */
    public Map<String, Object> getResponse() {
        return response;
    }

    /**
     * addResponse
     * @param srcPath path of responding node
     * @param aresponse response of a node
     */
    public void addResponse(String srcPath, Object aresponse) {
        if (response == null)
            response = new ConcurrentHashMap<String, Object>();
        //concurrenthashmap is not able to store null values!
        if (aresponse != null)
            response.put(srcPath, aresponse);
        if (responseController != null) {
            responseController.fireEvent(new Notification(srcPath, aresponse));
        }
    }

    /**
     * use this method to add your instance as listener to any notification response
     * @return event controller of this notification
     */
    public EventController getResponseController() {
        if (responseController == null)
            responseController = new EventController();
        return responseController;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Notification[ Path: " + path + " ==> " + notification + "]";
    }
}
