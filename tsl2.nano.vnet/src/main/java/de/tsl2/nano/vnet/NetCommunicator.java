/**
 * 
 */
package de.tsl2.nano.vnet;

import java.util.Map;
import java.util.Scanner;

import de.tsl2.nano.core.Argumentator;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.cls.ClassFinder;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.messaging.IListener;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.vnet.neuron.Layer;

/**
 * provides a terminal communnication with a {@link Net}
 * 
 * @author Tom
 */
public class NetCommunicator implements Runnable {
	/** implementation of the cover class */
	Class<?> implementation;
	/** handles command line arguments */
	private Argumentator art;

	public NetCommunicator(Argumentator art) {
		this.art = art;
	}

	public static void main(String[] args) throws Exception {
		LogFactory.setPrintToConsole(false);
		Argumentator art = new Argumentator("VNet", createManual(), args);
		new NetCommunicator(art).run();
	}

	@SuppressWarnings("unchecked")
	private static Map<String, String> createManual() {
		return MapUtil.asMap(
				"description", "unknown content will be learned, otherwise the net will be notifed, ENTER will exit", 
				"file", "serialized vnet xml file to create the net from", 
				"implementation", "full class name to a node cover implementation"
				);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void run() {
		if (art.check(System.out)) {
			Net net;
			String fileArg = art.get("file");
			if (!Util.isEmpty(fileArg)) {
				net = Net.create(fileArg);
				implementation = net.elements.keySet().iterator().next().getClass();
			} else {
				String implArg = art.get("implementation");
				if (implArg == null) {
					log(art);
					log("Creating new Net. Please give a Node implementation - currently found on classpath:");
					net = setImplementationByUserInput();
					if (implementation == null)
						return;
				} else {
					implementation = BeanClass.load(implArg);
					net = new Net(implementation.getSimpleName());
				}
			}
			// create a callback for responses
			StringBuilder output = new StringBuilder();
			IListener<Notification> responseHandler = new IListener<Notification>() {
				@Override
				public synchronized void handleEvent(Notification event) {
					Object n = event.getNotification();
					log("RESPONSE: " + n);
					output.append(n.toString() + " ");
				}
			};
			
			log("Net created: " + net.toString());
			log("cover implementation: " + implementation);
			log("core implementation: " + implementation.getSuperclass());
			Scanner scr = new Scanner(System.in);
			try {
				System.out.print(": ");
				while (scr.hasNextLine()) {
					String input = scr.nextLine();
					if (input.isEmpty()) {
						log("Realy want to exit vNet? (ENTER: yes, otherwise: no)");
						if (!scr.hasNextLine() || scr.nextLine().isEmpty())
							break;
					}
					String[] words = input.split(" ");
					Node node = null;
					for (int i = 0; i < words.length; i++) {
						IListener coreCover = (IListener) BeanClass.createInstance(implementation,
								new Object[] { words[i], Layer.getDefault(net) });
						if (net.getNode(coreCover) == null) {
							if (node != null)
								node = net.addAndConnect(coreCover, (IListener) node.getCore(), 1f);
							else
								node = net.add(coreCover);
						} else {
							if (node != null)
								net.getNode(coreCover).connect(node, 1f);
							net.notify(new Notification(words[i], words[i], null, responseHandler));
						}
					}
					log(net.dump());
					log("\n\n" + output);
					System.out.print(": ");
				}
			} finally {
				scr.close();
				net.save();
			}
		}
	}

	private Net setImplementationByUserInput() {
		log(ClassFinder.self().findClass(IListener.class));
		Scanner input = new Scanner(System.in);
		Net net = new Net();
		String line;
		do {
			try {
				System.out.print("please input the implementation classname: ");
				 line = input.nextLine();
				 if (line.length() > 0)
					 implementation = BeanClass.load(line);
				break;
			} catch (Exception e) {
				log("ERROR: " + e.toString());
				//->continue in while-loop
			}
		} while((input.hasNext()));
		//input.close(); //don't close it - we need System.in later...
		log("implementation class loaded: " + implementation);
		return net;
	}

	void log(Object obj) {
		System.out.println(obj);
	}
}
