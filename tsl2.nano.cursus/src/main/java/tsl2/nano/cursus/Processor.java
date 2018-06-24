package tsl2.nano.cursus;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.incubation.repeat.ICommand;
import de.tsl2.nano.incubation.repeat.impl.CommandManager;

/**
 * Change Process for Entities using Cursus. It's final to secure the use of IConsilium.refreshSeal(Processor)
 */
final class Processor {
	private static final Log LOG = LogFactory.getLog(Processor.class);
	final Id ID = new Id();
	class Id {
		final long timestamp = System.currentTimeMillis();
	}
	/**
	 * convenience to call {@link #run(Timer, IConsilium...)}
	 */
	void run(Date from, Date until, IConsilium...consiliums) {
		run(new Timer(from, until, 0, 0), consiliums);
	}

	/**
	 * @param timer holding from-until time period to do the process on
	 * @param consiliums items to be processed, if there timer has expired
	 */
	void run(Timer timer, IConsilium...consiliums) {
		log("------------------------------------------------------------------------------");
		log("processing " + consiliums.length + " consilii for period " + timer.from + " - " + timer.until);
		Set<IConsilium> cons = new TreeSet<>(Arrays.asList(consiliums));
		cons.addAll(evalTimedConsiliums(cons, timer.from, timer.until));
		for (IConsilium c : cons) {
			c.checkValidity(ID);
			if (c.getStatus().equals(Consilium.Status.INACTIVE) && timer.expired(c.getTimer().from)) {
				new CommandManager().doIt(c.getConsecutios().toArray(new ICommand[0]));
				c.refreshSeal(ID);
			}
		}
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

    static void log_(String msg) {
        System.out.print(msg);
    }

    static void log(String msg) {
        System.out.println(msg);
    }
}