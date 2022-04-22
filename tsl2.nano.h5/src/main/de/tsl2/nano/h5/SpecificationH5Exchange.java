package de.tsl2.nano.h5;

import java.util.Properties;
import java.util.Scanner;
import java.util.Map.Entry;

import de.tsl2.nano.bean.def.AttributeDefinition;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.SpecificationExchange;
import de.tsl2.nano.bean.def.ValueExpression;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.incubation.specification.Pool;
import de.tsl2.nano.incubation.specification.rules.RuledEnabler;
import static de.tsl2.nano.h5.NanoH5Util.*;
import static de.tsl2.nano.bean.def.SpecificationExchange.Change.*;
public class SpecificationH5Exchange extends SpecificationExchange {
	
    public int enrichFromSpecificationProperties() {
    	String file = FILENAME_SPEC_PROPERTIES;
		Properties spec = ENV.getSortedProperties(file);
		if (spec == null)
			spec = fromCSV();
		int errors = 0;
    	if (spec != null) {
        	ENV.moveBackup(file);
    		Pool pool = ENV.get(Pool.class);
    		String k, v, object, property;
    		BeanDefinition bean;
    		for (Entry<Object, Object> e : spec.entrySet()) {
    			// as we get all entries by a map, we have to filter comments ourself
    			try {
    				k = e.getKey().toString();
        			// as we get all entries by a map, we have to filter comments ourself
    				if (k.trim().startsWith("#") /*|| k.trim().startsWith("\\#")*/)
    					continue;
    				
    				v = (String)e.getValue();
					object = StringUtil.substring(k, null, ".", true);
					property = StringUtil.substring(k, ".", null, true);
					if (k.equals(pool.getFullExpressionPattern())) { // rule
						pool.add(k, v);
					} else {
						if (!object.contains(".")) { // bean
							bean = BeanDefinition.getBeanDefinition(object);
							if (property.startsWith(addattribute.name())) {
								checkRule(pool, v);
								addVirtualAttribute(bean, v);
							} else if (property.startsWith(addaction.name())) {
								checkRule(pool, v);
								bean.addAction(new SpecifiedAction<>(v, null));
							} else {
								switch (Change.valueOf(property)) {
								case valueexpression: bean.setValueExpression(new ValueExpression<>(v)); break;
								case attributefilter: bean.setAttributeFilter(v.split("[;,\\s]")); break;
								case icon: bean.getPresentable().setIcon(ENV.getConfigPathRel() + v); break;
								default:
									throw new IllegalArgumentException(property);
								}
							}
						} else { // field
							AttributeDefinition<?> attr = (AttributeDefinition<?>) AttributeDefinition.getAttributeDefinitionFromPath(object);
							String rule;
							switch (Change.valueOf(property)) {
							case enabler: 
								checkRule(pool, v); attr.getPresentation().setEnabler(new RuledEnabler(attr, v)); 
								break;
							case listener: 
								rule = StringUtil.substring(v, null, ":");
								String[] observables = StringUtil.substring(v, ":", null).split("[,;\\s]");
								checkRule(pool, rule); 
								addListener(attr.getParentBean(), attr.getName(), rule, observables); 
								break;
							case rulecover: 
								String child = StringUtil.substring(v, ":", null);
								rule = StringUtil.substring(v, null, ":");
								checkRule(pool, rule); 
								cover(attr, child, rule); 
								break;
							default:
								throw new IllegalArgumentException(property);
							}
						}
					}
					LOG.info("specification imported: " + k + " -> " + v);
				} catch (Exception e1) {
					LOG.error(e1);
					spec.put(e.getKey(), "!!!" + e.getValue() + "!!! " + e1.getMessage());
					errors++;
				}
			}
    		FileUtil.saveProperties(ENV.getConfigPath() + file + (errors == 0 ? ".done" : ".errors"), spec);
    	}
    	return errors;
	}

	protected Properties fromCSV() {
		Scanner sc = new Scanner(ENV.getConfigPath() + FILENAME_SPEC_PROPERTIES);
		Properties p = new Properties();
		String l, k, v;
		while (sc.hasNextLine()) {
			l = sc.nextLine();
			k = StringUtil.substring(l, null, SEP);
			v = StringUtil.substring(l, SEP, null).replace(SEP, ":");
			p.put(k, v);
		}
		return p;
	}

	static void checkRule(Pool pool, String v) {
		pool.get(v);
	}
}
