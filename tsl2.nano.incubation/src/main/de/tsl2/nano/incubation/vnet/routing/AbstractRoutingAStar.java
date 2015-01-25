/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider, Thomas Schneider
 * created on: Nov 25, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.incubation.vnet.routing;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import de.tsl2.nano.incubation.vnet.Connection;
import de.tsl2.nano.incubation.vnet.ILocatable;
import de.tsl2.nano.incubation.vnet.Link;
import de.tsl2.nano.incubation.vnet.Node;
import de.tsl2.nano.incubation.vnet.Notification;
import de.tsl2.nano.messaging.IListener;
import de.tsl2.nano.structure.ANode;
import de.tsl2.nano.structure.Cover;
import de.tsl2.nano.structure.IConnection;
import de.tsl2.nano.structure.INode;

/**
 * Implementation of A*-Algorithm like http://de.wikipedia.org/wiki/A*-Algorithmus.
 * <p/>
 * Using the vnet with its {@link Node}s and {@link Connection}s, a new created route will have new created nodes with
 * back-connections (weight is negative).
 * <p/>
 * to change the 'weight' behavior (e.g. to categorize specific roads) overwrite the {@link #g(Connection)} method.
 * <p/>
 * TODO: use ThreadingEventController to do parallel networking.
 * 
 * @param <T> type of {@link Node} content (=core)
 * @param <D> connection descriptor (on simple weighted connections, it would be Float)
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
public abstract class AbstractRoutingAStar<T extends IListener<Notification> & ILocatable & Serializable & Comparable<T>, D extends Comparable<D>> {
    /**
     * this priority queue holds all connections to be inspected. the priority is defined by the calculated minimal
     * distance to the destination. we use a weighted connection - a connection of a real node-connection to use the
     * float-number as calculated distance.
     */
    PriorityQueue<Cover<Connection<T, D>, Float>> openlist = new PriorityQueue<Cover<Connection<T, D>, Float>>();
    /** closedlist - see description of {@link #openlist} */
    Set<Connection<T, D>> closedlist = new HashSet<Connection<T, D>>();

    /**
     * route
     * 
     * @param start
     * @param destination
     * @return
     */
    public IConnection<T, D> route(Node<T, D> start, Node<T, D> destination) {
        Connection<T, D> currentConnection, route;
        // Initialisierung der Open List, die Closed List ist noch leer
        // (die Priorit�t bzw. der f Wert des Startknotens ist unerheblich)
        log("starting at: " + start);
        route = new Connection<T, D>(start, null);
        openlist.add(new Link<Connection<T, D>, Float>(route, 0f));
        // diese Schleife wird durchlaufen bis entweder
        // - die optimale L�sung gefunden wurde oder
        // - feststeht, dass keine L�sung existiert
        do {
            // Knoten mit dem geringsten f Wert aus der Open List entfernen
            currentConnection = openlist.poll().getContent();
            // Wurde das Ziel gefunden?
            if (currentConnection.getDestination().equals(destination)) {
                log("route finished: " + start + currentConnection);
                return route.getDestination().getConnections().iterator().next();
            }
            // Wenn das Ziel noch nicht gefunden wurde: Nachfolgeknoten
            // des aktuellen Knotens auf die Open List setzen
            log("checking connection " + currentConnection);
            expandNode(currentConnection, destination);
            // der aktuelle Knoten ist nun abschlie�end untersucht
            closedlist.add(currentConnection);
        } while (!openlist.isEmpty());
        // die Open List ist leer, es existiert kein Pfad zum Ziel
        return null;
    }

    /**
     * <pre>
     * �berpr�ft alle Nachfolgeknoten und f�gt sie der Open List hinzu, wenn entweder
     * - expandNode 
     * - der Nachfolgeknoten zum ersten Mal gefunden wird oder 
     * - ein besserer Weg zu diesem Knoten gefunden wird
     * </pre>
     * 
     * @param currentNode node to evaluate
     * @param destination
     */
    void expandNode(Connection<T, D> currentConnection, Node<T, D> destination) {
        // distance f
        float f, g, tentative_g;
        List<IConnection<T, D>> nextConnections = currentConnection.getDestination().getConnections();
        Node<T, D> successor;
        for (IConnection<T, D> con : nextConnections) {
            successor = (Node<T, D>) con.getDestination();
            log_("\t--> " + successor);
            // wenn der Nachfolgeknoten bereits auf der Closed List ist - tue nichts
            if (closedlist.contains(con)) {
                continue;
            }
            // g Wert f�r den neuen Weg berechnen: g Wert des Vorg�ngers plus
            // die Kosten der gerade benutzten Kante
            g = g((Connection<T, D>) con);
            tentative_g = g(currentConnection) + g;
            log_("\t g + c = " + tentative_g);
            // wenn der Nachfolgeknoten bereits auf der Open List ist,
            // aber der neue Weg nicht besser ist als der alte - tue nichts
            if (openlist.contains(new Link((Serializable) con, g)) && tentative_g >= g) {
                continue;
            }
            // Vorg�ngerzeiger setzen und g Wert merken
            // f Wert des Knotens in der Open List aktualisieren
            // bzw. Knoten mit f Wert in die Open List einf�gen
            f = tentative_g + h(successor, destination);
            log("\t f = " + f);
//         if (openlist.contains(successor)) {
//             openlist.decreaseKey(successor, f);
//         } else {
//             openlist.add(successor, f);
//         }
            Connection<T, D> next =
                (Connection<T, D>) connect(currentConnection.getDestination(), (Node<T, D>) con.getDestination(), g);
            Link<Connection<T, D>, Float> weightedConnection = new Link<Connection<T, D>, Float>(next, f);
            openlist.remove(weightedConnection);
            openlist.add(weightedConnection);
        }
    }

    public Collection<IConnection<T, D>> navigate(INode<T, D> start,
            IConnection<T, D> route,
            Collection<IConnection<T, D>> currentTrack) {
        if (currentTrack == null)
            currentTrack = new LinkedList<IConnection<T, D>>();
        IConnection<T, D> realConnection = ((ANode<T, D>) start).getConnection(route.getDestination());
        currentTrack.add(realConnection);
        List<IConnection<T, D>> connections = route.getDestination().getConnections();
        if (connections.size() == 0) {
            return currentTrack;
        } else if (connections.size() >= 1) {
            IConnection<T, D> c = connections.iterator().next();
            return navigate(realConnection.getDestination(), c, currentTrack);
        } else {
//            return currentTrack;
            throw new IllegalArgumentException("navigation nodes must have one or none connection");
        }
    }

    /**
     * successor.connectTo(currentNode, -tentative_g);
     * 
     * @param successor
     * @param currentNode
     * @param f
     */
    protected abstract IConnection<T, D> connect(Node<T, D> successor, Node<T, D> currentNode, float f);

    /**
     * direct distance to destination
     * 
     * @param successor current evaluation node
     * @param destination destination
     * @return distance
     */
    protected float h(Node<T, D> successor, Node<T, D> destination) {
        return successor.compareTo(destination);
    }

    /**
     * edge length (weight) of given connection
     * 
     * @param con connection
     * @return connection length
     */
    protected float g(Connection<T, D> con) {
        return con.length();
    }

    protected void log_(String msg) {
        System.out.print(msg);
    }

    protected void log(String msg) {
        System.out.println(msg);
    }
}
