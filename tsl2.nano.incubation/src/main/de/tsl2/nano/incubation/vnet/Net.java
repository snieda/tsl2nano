/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: ts
 * created on: 09.11.2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.incubation.vnet;

import java.io.Serializable;
import java.sql.Time;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

import de.tsl2.nano.collection.ListSet;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.messaging.EventController;
import de.tsl2.nano.messaging.IListener;
import de.tsl2.nano.structure.IConnection;

/**
 * provides a virtual net with {@link Node}s having {@link Connection}s to several other {@link Node}s. Each
 * {@link Node} must be {@link ILocatable} and contains a core object - the element you define by yourself. If you want
 * the net to work, you {@link #notify(Notification)} some nodes, where the {@link Notification} is a message with a
 * path-expression to address the handlers and a response map to store each response. All connected nodes will be
 * informed through the messaging system {@link ThreadingEventController}. All connected nodes must implement the
 * {@link IListener} interface to react on notifications and changes.<br/>
 * the goal is a fast parallel working net without using javas thread synchronization.
 * <p/>
 * the elements/nodes of that net are hold in a {@link TreeMap} - always having the same order.
 * <p/>
 * it is possible to handle each response by adding your implementation to the {@link Notification} object that you give
 * to {@link #notify(Notification)}.
 * <p/>
 * the goal of this vnet package is to encapsulate networking and parallelism method techniques from your special
 * implementation.
 * <p/>
 * to enable to work on numbers, each node implements the {@link Comparable} interface, delegating to its core (your
 * implementation). each connection has a {@link Connection#length()} as number representation (e.g. weight) of itself.
 * the default implementation uses the hashCode() of this descriptor object.
 * 
 * @param <T> type of {@link Node} content (=core)
 * @param <D> connection descriptor. on simple weighted connections, it would be Float. on complex connections you may
 *            need connection-properties for both ends.
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
public class Net<T extends IListener<Notification> & ILocatable & Serializable & Comparable<? super T>, D extends Comparable<? super D>> {
    /** all net nodes */
    Map<String, Node<T, D>> elements;
    /** using {@link #notifyIdles(Collection, IListener, long, long)}, all waiting sleep times are added */
    long waitingCycles = 0;
    /** normally true, but if you want to work without threading, you can set it to false */
    static boolean workParallel = true;

    /**
     * constructor
     */
    public Net() {
        elements = new TreeMap<String, Node<T, D>>();
        ENV.removeService(NotificationController.class);
    }

    /**
     * addAll
     * 
     * @param cores node cores to add
     */
    public void addAll(Collection<T> cores) {
        for (T c : cores) {
            add(c);
        }
    }

    /**
     * add new node
     * 
     * @param newNodeCore new node core to be wrapped into node instance to be added to the net.
     * @return new created node
     */
    public Node<T, D> add(T newNodeCore) {
        Node<T, D> node = new Node<T, D>(newNodeCore, null);
        elements.put(node.getPath(), node);
        return node;
    }

    /**
     * add the given node and add it to the given source node
     * 
     * @param newNodeCore
     * @param connection source node to be connected to
     * @param connectionDescriptor connection description
     * @return new created node
     */
    @SuppressWarnings("unchecked")
    public Node<T, D> addAndConnect(T newNodeCore, T connection, D connectionDescriptor) {
        Node<T, D> node = new Node<T, D>(newNodeCore,
            new ListSet<IConnection<T, D>>(new Connection<T, D>(getNode(connection),
                connectionDescriptor)));
        elements.put(node.getPath(), node);
        return node;
    }

    /**
     * remove
     * 
     * @param nodeCore node to be removed
     * @return removed node or null
     */
    public Node<T, D> remove(T nodeCore) {
        return elements.remove(getNode(nodeCore).getPath());
    }

    /**
     * notify all nodes that fulfill the path of the given notification.
     * 
     * @param notification notification to be sent to its defined path.
     */
    public void notify(Notification notification) {
        Collection<Node<T, D>> nodes = elements.values();
        for (Node<T, D> n : nodes) {
            //check, if node n has right path to be notified
            if (notification.notifiy(n)) {
                n.notify(notification);
            }
        }
    }

    /**
     * does the whole job. sends all notifications, waits until all nodes are ready, collects all results of all
     * notifications and returns a result list.
     * 
     * @param <R> result type
     * @param notifications notifications to send
     * @param resultType type of notification results
     * @return notification results
     */
    public <R> Collection<R> notifyAndCollect(Notification notification,
            Class<R> resultType
            ) {
        log("==> starting notify and collect on " + notification);
        long start = System.currentTimeMillis();
        notify(notification);
        waitForIdle(-1);
        log("<== notifications ended in " + (System.currentTimeMillis() - start)
            + " msecs (waiting cycles: "
            + waitingCycles
            + " msecs)");
        return (Collection<R>) (notification.getResponse() != null ? notification.getResponse().values()
            : new LinkedList<R>());
    }

    /**
     * notifies only nodes that are idle. this method blocks the current thread until an idle node was found. the given
     * responseObserver will be informed, if the working node puts its result to the notification object.
     * <p/>
     * this method should provide the kanban flow pattern.
     * 
     * @param notification notification to send
     * @param responseObserver response observer to be informed on any response
     * @param timeout timeout for blocking the current thread - searching for the next idle node
     * @param waitTime thread sleeping time between iteration
     */
    public void notifyFirstIdle(Notification notification,
            IListener<Notification> responseObserver,
            long timeout,
            long waitTime) {
        Collection<Node<T, D>> nodes = elements.values();
        long start = System.currentTimeMillis();
        int count = 0;
        while (timeout != -1 || System.currentTimeMillis() - start > timeout) {
            log_("\ndelegating notification " + notification + " to idle nodes (count: " + count++ + ")...");
            for (Node<T, D> n : nodes) {
                if (notification.notifiy(n) && n.isIdle()) {
                    n.notify(notification);
                    return;
                }
            }
            try {
                waitingCycles += waitTime;
                Thread.sleep(waitTime);
            } catch (InterruptedException e) {
                ManagedException.forward(e);
            }
        }
        log(this.getClass(), "timeout of " + timeout + " exceeded");
    }

    /**
     * notifyIdles
     * 
     * @param notifications
     * @param responseObserver
     * @param timeout
     * @param waitTime
     */
    public void notifyIdles(Collection<Notification> notifications,
            IListener<Notification> responseObserver,
            long timeout,
            long waitTime) {
        for (Notification notification : notifications) {
            notifyFirstIdle(notification, responseObserver, timeout, waitTime);
        }
    }

    /*
     * Utility block
     */

    /**
     * waits until all nodes are idle
     * 
     * @param timeout timeout for blocking the current thread - searching for the next idle node
     */
    public void waitForIdle(long timeout) {
        Collection<Node<T, D>> nodes = elements.values();
        long start = System.currentTimeMillis();
        log_("waiting");
        boolean wait;
        while (timeout == -1 || System.currentTimeMillis() - start < timeout) {
            log_(".");
            wait = false;
            for (Node<T, D> n : nodes) {
                if (!n.isIdle()) {
                    wait = true;
                    break;
                }
            }
            if (!wait) {
                log(this.getClass(), "no work left - all nodes are idle");
                return;
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                ManagedException.forward(e);
            }
        }
        log(this.getClass(), "wait timeout of " + timeout + " msecs exceeded");
    }

    /**
     * does the whole job. sends all notifications, waits until all nodes are ready, collects all results of all
     * notifications and returns a result list.
     * 
     * @param <R> result type
     * @param notifications notifications to send
     * @param resultType type of notification results
     * @return notification results
     */
    public <R> Collection<R> notifyIdlesAndCollect(Collection<Notification> notifications,
            Class<R> resultType
            ) {
        log("==> starting notify and collect on " + notifications.size() + " notifications");
        long start = System.currentTimeMillis();
        Collection<R> results = new ArrayList<R>();
        notifyIdles(notifications, null, -1, 500);
        waitForIdle(-1);
        log("<== notifications ended in " + (System.currentTimeMillis() - start)
            + " msecs (waiting cycles: "
            + waitingCycles
            + " msecs)");
        log_("collecting results...");
        for (Notification n : notifications) {
            if (n.getResponse() != null) {
                results.addAll((Collection<? extends R>) n.getResponse().values());
            }
        }
        log_("done\n");
        return results;
    }

    /**
     * setWorkParallel
     * 
     * @param workParallel {@link #workParallel}
     */
    public void setWorkParallel(boolean workParallel) {
        this.workParallel = workParallel;
    }

    /**
     * resetStatistics
     */
    public void resetStatistics() {
        waitingCycles = 0;
        for (Node<T, D> n : elements.values()) {
            n.statistics.reset();
        }
    }

    /**
     * log
     * 
     * @param logger logger
     * @param text message
     */
    static final void log(Class<?> logger, String text) {
        log(logger.getSimpleName() + ": " + text);
    }

    static void log_(String msg) {
        System.out.print(msg);
    }

    static void log(String msg) {
        log_(msg + "\n");
    }

    /**
     * searches for the given node inside this net
     * 
     * @param nodeCore nodes core to be searched
     * @return found node or null
     */
    public Node<T, D> getNode(T nodeCore) {
        return elements.get(nodeCore.getPath());
    }

    /**
     * factory to create event controller. all items of this net have to use this factory method.
     * 
     * @return new event controller
     */
    public static EventController createEventController() {
        if (workParallel) {
            return new NotificationController();
        } else {
            return new EventController();
        }
    }

    /**
     * dumps the properties and working times of all nodes - usable for statistics.
     * 
     * @return net and node descriptions
     */
    public String dump() {
        StringBuilder buf = new StringBuilder(30 + 30 * elements.size());
        buf.append(toString() + "\n");
        Collection<Node<T, D>> nodes = elements.values();
        long totalWorkingTime = 0;
        int notifications = 0;
        for (Node<T, D> node : nodes) {
            buf.append(node.dump() + "\n");
            totalWorkingTime += node.getStatistics().workingTime;
            notifications += node.getStatistics().notifications;
        }
        buf.append("total working time: " + DateFormat.getTimeInstance().format(new Time(totalWorkingTime))
            + " on "
            + notifications
            + " notifications\n");
        return buf.toString();
    }

    @Override
    public String toString() {
        return /*new BeanClass(this.getClass()).getGenericType() + */" Net with " + elements.size() + " elements";
    }
}
