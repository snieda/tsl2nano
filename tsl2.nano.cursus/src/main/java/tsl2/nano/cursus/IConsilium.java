package tsl2.nano.cursus;

import java.util.Date;
import java.util.List;
import java.util.Set;

import de.tsl2.nano.incubation.repeat.ICommand;
import tsl2.nano.cursus.Processor.Id;

/**
 * main change object to be processed by {@link Processor}.
 * @author Tom
 */
interface IConsilium {
	List<? extends ICommand> getConsecutios();
	void checkValidity(Id iD);
	void refreshSeal(Id iD);
	Set<? extends IConsilium> createAutomated(Date from, Date until);
	Status getStatus();
	Timer getTimer();
	enum Status {INACTIVE, ACTIVE, REJECTED}
	enum Priority {HIGHEST(1), HIGH(2), NORMAL(10), LOW(1000), LOWEST(Integer.MAX_VALUE);
		Integer index;
		Priority(Integer index) {this.index = index;}
	}
}