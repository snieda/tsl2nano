package de.tsl2.nano.cursus;

import java.io.Serializable;
import java.util.Set;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.DateUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.repeat.IChange;
import de.tsl2.nano.repeat.ICommand;

/**
 * Defines blocker to block a consilium in a special timespan
 * @author Tom
 * @param <CONTEXT>
 */
@SuppressWarnings("rawtypes")
public class Obsidio implements ICommand<Set<? extends IConsilium>>, Serializable {
	private static final long serialVersionUID = 1L;
	private static final Log LOG = LogFactory.getLog(Obsidio.class);
	protected Object consiliumID;
	protected Grex grex;
	protected Timer timer;
	private transient Set<? extends IConsilium> consilii;
	protected String name;

	public Obsidio() {
	}

	public Obsidio(String name, Object consiliumID, Timer timer, Grex grex) {
		this.name = name;
		this.consiliumID = consiliumID;
		this.timer = timer;
		this.grex = grex;
	}

	@Override
	public void run() {
		consilii.stream().filter(c -> isAffected(c)).forEach(c -> {
			Processor.log("block: " + StringUtil.fixString(name, 16) + " on " + c);
			c.setStatus(IConsilium.Status.REJECTED);
		});
	}

	private boolean isAffected(IConsilium c) {
		return c.getName().equals(consiliumID)
				&& DateUtil.contains(timer.from, timer.until, c.getTimer().from, c.getTimer().until)
				&& (grex.validObjectIDs == null || grex.validObjectIDs.stream().anyMatch(o -> c.affects(o)));
	}

	@Override
	public void undo() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void runWith(IChange... changes) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<? extends IConsilium> getContext() {
		return consilii;
	}

	@Override
	public void setContext(Set<? extends IConsilium> consilii) {
		this.consilii = consilii;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return Util.toString(getClass(), name, consiliumID, timer, grex);
	}
}
