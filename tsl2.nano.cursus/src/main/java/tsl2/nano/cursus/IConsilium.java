package tsl2.nano.cursus;

import java.util.Date;
import java.util.Set;

import de.tsl2.nano.incubation.repeat.ICommand;
import tsl2.nano.cursus.Processor.Id;

/**
 * main change object to be processed by an authorized {@link Processor}.
 * @author Tom
 */
public interface IConsilium {
	/** commands to run if activated */
	Set<? extends ICommand<?>> getExsecutios();
	/** should check against a seal to avoid invalid data changes */
	void checkValidity(Id iD);
	/** only the authorized processor should refresh the seal! */
	void refreshSeal(Id iD);
	/** creates automated consilii, if timer is a generator */
	Set<? extends IConsilium> createAutomated(Date from, Date until);
	/** the authorized processor can change from inactive to active */ 
	Status getStatus();
	/** defines, when the consilium should be activated. if the timer is a generator, new consilii will be created */
	Timer getTimer();
	/** returns true, if any stored content is assigned */
	boolean hasFixedContent();
	
	enum Status {INACTIVE, ACTIVE, REJECTED}
	enum Priority {HIGHEST(1), HIGH(2), NORMAL(10), LOW(1000), LOWEST(Integer.MAX_VALUE);
		Integer index;
		Priority(Integer index) {this.index = index;}
	}
}