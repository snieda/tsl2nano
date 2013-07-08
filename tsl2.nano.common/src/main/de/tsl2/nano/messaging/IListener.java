/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Feb 15, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.messaging;

/**
 * bean value change listener. can be registered on an {@link EventController}. The type should only be used, if you
 * have exactly one event type handling to be implemented. if you have more than one, you should use {@link Object},
 * using instanceof to fork into the desired event-type-handler.
 * 
 * <pre>
 * Example:
 * 
 * public class MyClass implements IListener&lt;Object&gt; {
 *     &#064;Override
 *     public Object handleEvent(Object event) {
 *         if (event instanceof ChangeEvent) {
 *             return handleEvent((ChangeEvent) event);
 *         } else
 *             return handleEvent((Notification) event);
 *     }
 * 
 *     public Object handleEvent(ChangeEvent event) {
 *         feedSignal(FIRE, true);
 *         return null;
 *     }
 * 
 *     public Object handleEvent(Notification event) {
 *         return fire(true);
 *     }
 * }
 * </pre>
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public interface IListener<T> {
    /**
     * handleChange
     * 
     * @param event current event
     */
    void handleEvent(T event);
}
