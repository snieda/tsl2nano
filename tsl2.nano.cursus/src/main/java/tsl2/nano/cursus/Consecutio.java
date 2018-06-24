package tsl2.nano.cursus;

import java.io.Serializable;
import java.util.function.Consumer;

import javax.persistence.Entity;

import de.tsl2.nano.bean.PReference;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.incubation.repeat.IChange;
import de.tsl2.nano.incubation.repeat.ICommand;

@Entity
class Consecutio<CONTEXT> implements ICommand<CONTEXT>, Serializable {
	private static final long serialVersionUID = 1L;
	Action action;
	Mutatio mutatio;
	String description;
	enum Action {
		CHANGE_END(c -> change(c)), 
		CHANGE_STATUS(c -> change(c)), 
		CHANGE_SALDO(c -> change(c));
		
		transient private Consumer<IChange> consumer;
		Action(Consumer<IChange> consumer) {
			this.consumer = consumer;
		}
		public void transform(IChange change) {
			consumer.accept(change);
		}
	}

	transient CONTEXT context;
	
	public Consecutio(Action action, Mutatio mutatio, String description) {
		this.action = action;
		this.mutatio = mutatio;
		this.description = description;
	}

	@Override
	public void run() {
		runWith(mutatio);
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
			//TODO: implement
			action.transform(changes[i]);
			Processor.log("  run: " + StringUtil.fixString(action, 16) + " on " + changes[i]);
		}
	}
	static void change(IChange iChange) {
		if (iChange.getItem() instanceof PReference) {
			PReference<Contract, Object> bref = (PReference<Contract, Object>) iChange.getItem();
			bref.getValueAccess().setValue(iChange.getNew());
		}
	}

	@Override
	public String toString() {
		return Util.toString(getClass(), action, mutatio);
	}
}