package de.tsl2.nano.vnet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Time;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.execution.Profiler;
import de.tsl2.nano.core.messaging.EventController;
import de.tsl2.nano.core.messaging.IListener;
import de.tsl2.nano.core.util.ENVTestPreparation;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.structure.Cover;
import de.tsl2.nano.structure.IConnection;
import de.tsl2.nano.vnet.neuron.Layer;
import de.tsl2.nano.vnet.neuron.VNeuron;
import de.tsl2.nano.vnet.routing.Location;
import de.tsl2.nano.vnet.routing.RoutingAStar;
import de.tsl2.nano.vnet.workflow.Act;
import de.tsl2.nano.vnet.workflow.ComparableMap;
import de.tsl2.nano.vnet.workflow.VActivity;

public class VNetTest implements ENVTestPreparation {

    @BeforeClass
    public static void setUp() {
    	ENVTestPreparation.setUp("vnet", false);
    }

    @AfterClass
    public static void tearDown() {
    	ENVTestPreparation.tearDown();
    }
    
	@Test
	public void testVNetWithNeuralNet() {
		Net<VNeuron, Float> net = new Net<VNeuron, Float>();
		Layer layer = Layer.getDefault(net);
		// net.setWorkParallel(false);
		Node<VNeuron, Float> nHunger = net.add(new VNeuron("hunger", layer));
		Node<VNeuron, Float> nHabe = net.addAndConnect(new VNeuron("habe", layer), nHunger.getCore(), 1f);
		net.addAndConnect(new VNeuron("ich", layer), nHabe.getCore(), 1f);

		net.dump();
		
		List<Object> output = new LinkedList<>();
		// create a callback for responses
		IListener<Notification> responseHandler = new IListener<Notification>() {
			@Override
			public synchronized void handleEvent(Notification event) {
				Object n = event.getNotification();
				System.out.println("RESPONSE: " + n);
				output.add(n.toString());
			}
		};

		// start input
		net.notify(new Notification("ich", 1f, null, responseHandler));
		net.notify(new Notification("hunger", 1f, null, responseHandler));

		// wait for all threads to complete
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			ManagedException.forward(e);
		}

		assertTrue("should output at least: ich habe hunger", output.containsAll(Arrays.asList("ich", "habe", "hunger")));
	}

	@Test
	public void testVNetWithNeuralNetCommunicator() throws Exception {
		//don't throw an Exception
		System.setIn(StringUtil.toInputStream("\n"));
		NetCommunicator.main(new String[0]);
		
		// start and exit
		System.setIn(StringUtil.toInputStream(
				VNeuron.class.getName()+  "\nich bin tom\nwer bin ich\n\n"));
		
		NetCommunicator.main(new String[] {
				"implementation=" + VNeuron.class.getName()
		});
		
	}
	
	@Test
	public void testVNetWithWorkflow() {
		final ComparableMap<CharSequence, Object> state = new ComparableMap<CharSequence, Object>();
		state.put("A", true);
		state.put("B", true);

		Net<VActivity<String, String>, ComparableMap<CharSequence, Object>> net = new Net<VActivity<String, String>, ComparableMap<CharSequence, Object>>();
		// net.setWorkParallel(false);

		// works without linking the states to each other, too!!
		Node<VActivity<String, String>, ComparableMap<CharSequence, Object>> state0 = net
				.add(new Act("state0", "A&B", "A&B?C:D"));
		Node<VActivity<String, String>, ComparableMap<CharSequence, Object>> state1 = net
				.addAndConnect(new Act("state1", "C", "D"), state0.getCore(), state);
		net.addAndConnect(new Act("state2", "D", "E"), state1.getCore(), state);

		// create a callback for responses
		IListener<Notification> responseHandler = new IListener<Notification>() {
			@Override
			public synchronized void handleEvent(Notification event) {
				System.out.println("RESPONSE: " + event.getNotification());
				assertEquals("C", event.getNotification());
			}
		};

		// start input. don't set a query path, notification is always true, all items
		// are linked
		net.notify(new Notification(null, state, null, responseHandler));
		// net.notify(new Notification(null, true, null, responseHandler));

		// wait for all threads to complete
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			ManagedException.forward(e);
		}
		// should output: C
	}

	@Test
	public void testVNetWithRouting() {
		/*
		 * kuerzeste Verbindung zwischen Wuerzburg und Saarbruecken. siehe Wikipedia
		 * Beispiel
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
		
		net.dump();
		
		RoutingAStar routing = new RoutingAStar();
		IConnection<Location, Float> route = routing.route(saarbruecken, wuerzburg);
		if (route == null) {
			fail("No route found for " + saarbruecken + " --> " + wuerzburg);
		} else {
			Node<Location, Float>[] shortestWay = new Node[] { saarbruecken, kaiserslautern, frankfurt, wuerzburg };
			Collection<IConnection<Location, Float>> navigation = routing.navigate(saarbruecken, route, null);
			log("Navigation: " + navigation);
			int i = 0;
			if (navigation.size() < shortestWay.length) {
				fail("navigation incomplete");
			}
			for (IConnection<Location, Float> connection : navigation) {
				if (!connection.getDestination().equals(shortestWay[i++])) {
					fail("Routing didn't find shortest Way!");
				}
			}
		}
	}

	// @Test
	public void testVNetWithKanbanFlow() {
		int testcount = 100;
		/*
		 * - create 8 nodes with a calculating runnable - call notifyIdle(..) on >8
		 * notifications - check the results - check performance with profiler
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
				net.notifyFirstIdle(new Notification(null, "working-test"), responseObserver, 10 * 1000, 100);
			}
		});
		net.waitForIdle(60 * 1000);

		log("total test time: " + DateFormat.getTimeInstance().format(new Time(System.currentTimeMillis() - start)));
		log(net.dump());

		/*
		 * do the whole test calling a net convenience method
		 */
		net.resetStatistics();
		Collection<Notification> notifications = new ArrayList<Notification>();
		for (int i = 0; i < testcount; i++) {
			notifications.add(new Notification(null, "working-test-" + i));
		}
		Collection<Object> result = net.notifyIdlesAndCollect(notifications, Object.class);
		assertTrue(result.size() == testcount);
		log(StringUtil.toFormattedString(result, 10000, true));
		log(net.dump());
	}

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
                    ManagedException.forward(e);
                }
                System.out.print('.');
            }
        };
        final EventController c = new EventController();
        final ThreadingEventController ct = ENV.get(ThreadingEventController.class);
        for (int i = 0; i < 50; i++) {
            c.addListener(listener);
            ct.addListener(listener);
        }
        final Object event = new Object();
        Profiler.si().compareTests("test simple and multi-threaded event-handling", 50, new Runnable() {
            @Override
            public void run() {
                c.fireEvent(event);
            }
        }, new Runnable() {
            @Override
            public void run() {
                ct.fireEvent(event);
            }
        });
    }

	static void log_(String msg) {
		System.out.print(msg);
	}

	static void log(String msg) {
		System.out.println(msg);
	}
}

class TCover<T extends Runnable & Serializable & Comparable<? super T>, D extends Comparable<? super D>>
		extends Cover<T, D> implements ILocatable, IListener<Notification> {

	/** serialVersionUID */
	private static final long serialVersionUID = 1L;

	public TCover(T core, D descriptor) {
		super(core, descriptor);
	}

	@Override
	public void handleEvent(Notification event) {
		// do something like a calculation
		getContent().run();
		// return only the notification itself - the result doesn't matter
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
