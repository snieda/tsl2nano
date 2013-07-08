/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider, Thomas Schneider
 * created on: Nov 11, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.incubation.test;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Time;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;

import de.tsl2.nano.Environment;
import de.tsl2.nano.collection.TableList;
import de.tsl2.nano.exception.ForwardedException;
import de.tsl2.nano.execution.Profiler;
import de.tsl2.nano.format.DefaultFormat;
import de.tsl2.nano.incubation.vnet.Connection;
import de.tsl2.nano.incubation.vnet.Cover;
import de.tsl2.nano.incubation.vnet.ILocatable;
import de.tsl2.nano.incubation.vnet.Net;
import de.tsl2.nano.incubation.vnet.Node;
import de.tsl2.nano.incubation.vnet.Notification;
import de.tsl2.nano.incubation.vnet.ThreadingEventController;
import de.tsl2.nano.incubation.vnet.neuron.VNeuron;
import de.tsl2.nano.incubation.vnet.routing.Location;
import de.tsl2.nano.incubation.vnet.routing.RoutingAStar;
import de.tsl2.nano.logictable.DefaultHeader;
import de.tsl2.nano.logictable.EquationSolver;
import de.tsl2.nano.logictable.LogicTable;
import de.tsl2.nano.logictable.Structure;
import de.tsl2.nano.messaging.EventController;
import de.tsl2.nano.messaging.IListener;
import de.tsl2.nano.util.StringUtil;

/**
 * basic tests for algorithms to be refactored to the project swartifex.common in future.
 * 
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
public class IncubationTest {

    @Test
    public void testEventPerformance() {
        final IListener<Object> listener = new IListener<Object>() {
            @Override
            public void handleEvent(Object event) {
//                System.out.print('.');

//                int z = 0;
//                for (int i = 0; i < 10000000; i++) {
//                    z = i;
//                }
//              System.out.print(z);

                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    ForwardedException.forward(e);
                }
                System.out.print('.');
            }
        };
        final EventController c = new EventController();
        final ThreadingEventController ct = Environment.get(ThreadingEventController.class);
        for (int i = 0; i < 100; i++) {
            c.addListener(listener);
            ct.addListener(listener);
        }
        final Object event = new Object();
        Profiler.si().compareTests("test simple and multi-threaded event-handling", 100, new Runnable() {
            public void run() {
                c.fireEvent(event);
            }
        }, new Runnable() {
            public void run() {
                ct.fireEvent(event);
            }
        });
    }

    @Test
    public void testVNetWithNeuralNet() {
        Net<VNeuron, Float> net = new Net<VNeuron, Float>();
//        net.setWorkParallel(false);
        Node<VNeuron, Float> nHunger = net.add(new VNeuron("hunger"));
        Node<VNeuron, Float> nHabe = net.addAndConnect(new VNeuron("habe"), nHunger.getCore(), -1f);
        net.addAndConnect(new VNeuron("ich"), nHabe.getCore(), -1f);

        //create a callback for responses
        IListener<Notification> responseHandler = new IListener<Notification>() {
            @Override
            public synchronized void handleEvent(Notification event) {
                System.out.println("RESPONSE: " + event.getNotification());
            }
        };

        //start input
        net.notify(new Notification("ich", 1f, null, responseHandler));
        net.notify(new Notification("hunger", 1f, null, responseHandler));

        //should output: ich habe hunger
    }

    @Test
    public void testVNetWithRouting() {
        /*
         * kuerzeste Verbindung zwischen Wuerzburg und Saarbruecken.
         * siehe Wikipedia Beispiel
         */
        Net<Location, Float> net = new Net<Location, Float>();
        Node<Location, Float> saarbruecken = net.add(new Location("Saarbrücken", 222, 0, 0));
        Node<Location, Float> kaiserslautern = net.add(new Location("Kaiserslautern", 158, 0, 0));
        Node<Location, Float> karlsruhe = net.add(new Location("Karlsruhe", 140, 0, 0));
        Node<Location, Float> ludwigshafen = net.add(new Location("Ludwigshafen", 108, 0, 0));
        Node<Location, Float> frankfurt = net.add(new Location("Frankfurt", 96, 0, 0));
        Node<Location, Float> heilbronn = net.add(new Location("Heilbronn", 87, 0, 0));
        Node<Location, Float> wuerzburg = net.add(new Location("Würzburg", 0, 0, 0));

        saarbruecken.connect(kaiserslautern, 70f);
        saarbruecken.connect(karlsruhe, 145f);

        kaiserslautern.connect(ludwigshafen, 53f);
        kaiserslautern.connect(frankfurt, 103f);
        karlsruhe.connect(heilbronn, 84f);

        heilbronn.connect(wuerzburg, 102f);
        frankfurt.connect(wuerzburg, 116f);
        ludwigshafen.connect(wuerzburg, 183f);

        RoutingAStar routing = new RoutingAStar();
        Connection<Location, Float> route = routing.route(saarbruecken, wuerzburg);
        if (route == null)
            Assert.fail("No route found for " + saarbruecken + " --> " + wuerzburg);
        else {
            Node<Location, Float>[] shortestWay = new Node[] { kaiserslautern, frankfurt, wuerzburg };
            Collection<Connection<Location, Float>> navigation = routing.navigate(saarbruecken, route, null);
            log("Navigation: " + navigation);
            int i = 0;
            if (navigation.size() < shortestWay.length)
                Assert.fail("navigation incomplete");
            for (Connection<Location, Float> connection : navigation) {
                if (!connection.getDestination().equals(shortestWay[i++]))
                    Assert.fail("Routing didn't find shortest Way!");
            }
        }
    }

    @Test
    public void testVNetWithKanbanFlow() {
        int testcount = 100;
        /*
         *  - create 8 nodes with a calculating runnable
         *  - call notifyIdle(..) on >8 notifications
         *  - check the results
         *  - check performance with profiler
         */
        
        final Net<TCover<TRunner, Float>, Float> net = new Net<TCover<TRunner, Float>, Float>();
        Node<TCover<TRunner, Float>, Float> n = null;
        for (int i = 0; i < 8; i++) {
            n = net.add(new TCover<TRunner, Float>(new TRunner(), Float.valueOf(i)));
        }
        
        /*
         * first: check monolithic performance
         */
        Profiler.si().stressTest("monolithic test", testcount, n.getCore().getContent());
        
        /*
         * second: test with own observing
         */
        final IListener<Notification> responseObserver = new IListener<Notification>() {
            @Override
            public void handleEvent(Notification event) {
                log("response: " + event.getResponse());
            }
        };
        log(net.dump());
        long start = System.currentTimeMillis();

        /*
         * start the test
         */
        Profiler.si().stressTest("kanbanflow on 8 nodes", testcount, new Runnable() {
            @Override
            public void run() {
                net.notifyIdles(new Notification(null, "working-test"), responseObserver, 10*1000, 100);
            }
        });
        net.waitForIdle(60*1000);
        
        log("total test time: " + DateFormat.getTimeInstance().format(new Time(System.currentTimeMillis() - start)));
        log(net.dump());
        
        /*
         * do the whole test calling a net convenience method
         */
        net.resetStatistics();
        Collection<Notification> notifications = new ArrayList<Notification>();
        for (int i = 0; i<testcount; i++) {
            notifications.add(new Notification(null, "working-test-" + i));
        }
        Collection<Object> result = net.notifyAndCollect(notifications, Object.class);
        Assert.assertTrue(result.size() == testcount);
        log(StringUtil.toFormattedString(result, 10000, true));
        log(net.dump());
    }

    @Test
    public void testEquationSolver() {
        String f = "1+ ((x1 + x2)*3 + 4)+5";
//        char[] cs = f.toCharArray();
//        List<String> s = new ArrayList<String>(cs.length);
//        for (int i = 0; i < cs.length; i++) {
//            s.add(String.valueOf(cs[i]));
//        }
//        String term = StringUtil.extract(f, "\\([^)(]*\\)");
//        String op1 = StringUtil.extract(f, "[a-zA-Z0-9]+" + );
//        Structure<List<String>, String, String, String> structure = new Structure<List<String>, String, String, String>(s,
//            "(",
//            ")");
//        Collection<List<String>> items = structure.getTree().values();
//        for (List<String> list : items) {
//            log(items.toString());
//        }
        BigDecimal x1 = new BigDecimal(8);
        BigDecimal x2 = new BigDecimal(9);
        Map<String, Object> values = new Hashtable<String, Object>();
        values.put("x1", x1);
        values.put("x2", x2);
        Assert.assertEquals(new BigDecimal(61), new EquationSolver(null, values).eval(f));
    }

    @Test
    public void testLogicTable() {
        TableList<DefaultHeader, String> table = new LogicTable<DefaultHeader, String>(2).fill(String.class, 2);
        table.set(0, 0, new BigDecimal(10));
        table.set(1, 0, new BigDecimal(9));
        table.set(1, 1, "=A1 * A2");
        Assert.assertEquals(new BigDecimal(90), table.get(1, 1));
    }

    static void log_(String msg) {
        System.out.print(msg);
    }

    static void log(String msg) {
        System.out.println(msg);
    }
}

class TCover<T extends Runnable & Serializable & Comparable<? super T>, D extends Comparable<? super D>> extends Cover<T, D> implements
        ILocatable,
        IListener<Notification> {

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    public TCover(T core, D descriptor) {
        super(core, descriptor);
    }

    @Override
    public void handleEvent(Notification event) {
        //do something like a calculation
        getContent().run();
        //return only the notification itself - the result doesn't matter
        event.addResponse(getPath(), event.getNotification());
    }

    @Override
    public String getPath() {
        return toString();
    }

}

class TRunner implements Runnable, Serializable, Comparable<TRunner> {
    BigDecimal result;
    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    @Override
    public int compareTo(TRunner o) {
        return 0;
    }

    @Override
    public void run() {
        Profiler.si().workLoop(1000000000);
    }

    @Override
    public String toString() {
        return "worker";
    }
}