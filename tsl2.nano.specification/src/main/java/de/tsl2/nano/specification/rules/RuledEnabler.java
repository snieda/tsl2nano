package de.tsl2.nano.specification.rules;

import org.simpleframework.xml.Attribute;

import de.tsl2.nano.action.IActivable;
import de.tsl2.nano.bean.def.BeanValue;
import de.tsl2.nano.bean.def.IAttributeDefinition;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.specification.Pool;

/**
 * NOT FINISHED YET! The injection of beanvalue after deserialization must be implemented!
 * 
 * @author ts
 */
public class RuledEnabler implements IActivable {
	transient BeanValue<?> context;
	@Attribute
	String ruleName;

	RuledEnabler() {
	}

	public RuledEnabler(IAttributeDefinition attr, String ruleName) {
		this.ruleName = ruleName;
	}

	@Override
	public boolean isActive() {
		return Util.isTrue(ENV.get(Pool.class).get(ruleName).run(context));
	}

}
