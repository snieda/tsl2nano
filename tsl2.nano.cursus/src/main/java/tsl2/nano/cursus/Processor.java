package tsl2.nano.cursus;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.messaging.EventController;
import de.tsl2.nano.incubation.repeat.ICommand;
import de.tsl2.nano.incubation.repeat.impl.CommandManager;
import tsl2.nano.cursus.IConsilium.Status;
/**
 * Change Process for Entities using Cursus. It's final to secure the use of IConsilium.refreshSeal(Processor)
 */
public final class Processor {
	public static final String STOPPED = "STOPPED";
	public static final String FINISHED = "FINISHED";

	private static final Log LOG = LogFactory.getLog(Processor.class);
	
	final Id ID = new Id();
	class Id {
		final long timestamp = System.currentTimeMillis();
	}

	AtomicBoolean stop = new AtomicBoolean(false);
	
	EventController eventController = new EventController();
	
	/**
	 * convenience to call {@link #run(Timer, IConsilium...)}
	 */
	public void run(Date from, Date until, IConsilium...consiliums) {
		run(new Timer(from, until, 0, 0), consiliums);
	}

	/**
	 * @param timer holding from-until time period to do the process on
	 * @param consiliums items to be processed, if there timer has expired
	 */
	public void run(Timer timer, IConsilium...consiliums) {
		log("------------------------------------------------------------------------------");
		log("processing " + consiliums.length + " consilii for period " + timer.from + " - " + timer.until);
		Set<IConsilium> cons = new TreeSet<>(Arrays.asList(consiliums));
		cons.addAll(evalTimedConsiliums(cons, timer.from, timer.until));
		CommandManager cmdManager = new CommandManager();
		getEventController().fireEvent(cons.size());
		for (IConsilium c : cons) {
			if (stop.get()) {
				eventController.fireEvent(STOPPED);
				break;
			}
			c.checkValidity(ID);
			if ((c.getStatus() == null || c.getStatus().equals(Status.INACTIVE)) && timer.expired(c.getTimer().from)) {
				c.getExsecutios().stream().filter(e -> e instanceof Obsidio).forEach(e -> ((Obsidio)e).setContext(cons));
				
				try {
					eventController.fireEvent(c);
					cmdManager.doIt(c.getExsecutios().toArray(new ICommand[0]));
					c.setStatus(Status.ACTIVE);
					c.refreshSeal(ID);
					eventController.fireEvent(c);
				} catch (Exception ex) {
					LOG.error(ex);
					eventController.fireEvent(ex);
				}
			}
		}
		eventController.fireEvent(FINISHED);
		log("processing finished");
		log("------------------------------------------------------------------------------");
	}

	/**
	 *evaluates the automatic consilii through their generic timer. 
	 */
	private Set<Consilium> evalTimedConsiliums(Set<IConsilium> cons, Date from, Date until) {
		Set<Consilium> automated = new HashSet<>();
		for (Iterator<IConsilium> it = cons.iterator(); it.hasNext();) {
			Consilium c = (Consilium) it.next();
			if (c.getTimer().isGenerator()) {
				automated.addAll(c.createAutomated(from, until));
				it.remove();
			}
		}
		return automated;
	}

	public void stop() {
		stop.set(true);
	}
	
	public void resetTo(Set<? extends Consilium> consiliums, Consilium lastActiveConsilium) {
		deactivate(consiliums, lastActiveConsilium.getTimer().from, null);
	}
	public Set<? extends Consilium> deactivate(Set<? extends Consilium> consiliums, Date from, Date until) {
		return resetToStatus(consiliums, from, until, Status.REJECTED);
	}

	private Set<? extends Consilium> resetToStatus(Set<? extends Consilium> consiliums, Date from, Date until, Status status) {
		Stream<? extends Consilium> filter = consiliums.stream().
			filter(c -> !c.getTimer().isGenerator() && c.getTimer().isPartOf(from, until));
			filter.forEach(c -> c.setStatus(status));
		return filter.collect(Collectors.toSet());
	}
    static void log_(String msg) {
        System.out.print(msg);//LOG.info(msg);
    }

    static void log(String msg) {
        System.out.println(msg);//LOG.info(msg);
    }

	public EventController getEventController() {
		return eventController;
	}
}