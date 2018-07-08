package tsl2.nano.cursus;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import de.tsl2.nano.bean.PReference;
import de.tsl2.nano.core.IPredicate;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.incubation.repeat.IChange;
import de.tsl2.nano.incubation.repeat.ICommand;
import de.tsl2.nano.incubation.tree.STree;
import de.tsl2.nano.incubation.tree.Tree;
import tsl2.nano.cursus.effectus.Effectree;
import tsl2.nano.cursus.effectus.Effectree.Entry;
import tsl2.nano.cursus.effectus.Effectus;

/**
 * The command that do changes on an instance path. This class should be overwritten to implement specific change commands.
 * @author Tom
 * @param <CONTEXT>
 */
public class Exsecutio<CONTEXT> implements ICommand<CONTEXT>, Serializable {
	private static final long serialVersionUID = 1L;
	protected String name;
	protected Mutatio mutatio;
	protected List<? extends Effectus> effectus;
	protected String description;

	transient CONTEXT context;
	
	public Exsecutio() {
	}

	public Exsecutio(String name, Mutatio mutatio, String description) {
		this.name = name;
		this.mutatio = mutatio;
		this.description = description;
	}

	@Override
	public void run() {
		LinkedList<IChange> muteffs = new LinkedList<>();
		muteffs.add(mutatio);
		muteffs.addAll(getEffectus());
		runWith(muteffs.toArray(new IChange[0]));
	}

	@Override
	public CONTEXT getContext() {
		return context;
	}

	@Override
	public void setContext(CONTEXT context) {
		this.context = context;
	}

	@Override
	public void undo() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void runWith(IChange... changes) {
		for (int i = 0; i < changes.length; i++) {
			if (!canRunOn(changes[i])) {
				Processor.log("ignoring run " + StringUtil.fixString(name, 16) + " on " + changes[i]);
				continue;
			}
			preCheck(changes[i]);
			changeAttribute(changes[i]);
			Processor.log("  run: " + StringUtil.fixString(name, 16) + " on " + changes[i]);
			postCheck(changes[i]);
		}
	}
	/**
	 * Called before run to perhaps ignore the run...returns true...to be overwritten...
	 * @param iChange
	 * @return true
	 */
	protected boolean canRunOn(IChange iChange) {
		return true;
	}

	/**
	 * Called before run...does nothing...to be overwritten...
	 * @param iChange
	 */
	protected void preCheck(IChange iChange) {
	}

	/**
	 * Called after run...does nothing...to be overwritten...
	 * @param iChange
	 */
	protected void postCheck(IChange iChange) {
	}

	static void changeAttribute(IChange iChange) {
		if (iChange.getItem() instanceof PReference) {
			PReference<?, Object> bref = (PReference<?, Object>) iChange.getItem();
			bref.getValueAccess().setValue(iChange.getNew());
		}
	}

	public boolean hasFixedContent() {
		return getEffectus().stream().anyMatch(e -> e.isFixed() && e.getNew() != null);
	}

	public Mutatio getMutatio() {
		return mutatio;
	}

	public List<? extends Effectus> getEffectus() {
		if (effectus == null && mutatio != null) {
			effectus = Effectree.generateEffects(mutatio.res);
		}
		return effectus;
	}

	@Override
	public String toString() {
		return Util.toString(getClass(), name, mutatio);
	}
}