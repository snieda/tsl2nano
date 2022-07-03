package de.tsl2.nano.h5;

import static de.tsl2.nano.h5.NanoH5Util.LOG;
import static de.tsl2.nano.h5.NanoH5Util.addListener;
import static de.tsl2.nano.h5.NanoH5Util.addVirtualAttribute;
import static de.tsl2.nano.h5.NanoH5Util.cover;
import static de.tsl2.nano.incubation.specification.SpecificationExchange.Change.addaction;
import static de.tsl2.nano.incubation.specification.SpecificationExchange.Change.addattribute;

import java.util.Map.Entry;
import java.util.Properties;
import java.util.Scanner;

import de.tsl2.nano.bean.def.AttributeDefinition;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.IAttributeDefinition;
import de.tsl2.nano.bean.def.ValueExpression;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.incubation.specification.Pool;
import de.tsl2.nano.incubation.specification.SpecificationExchange;
import de.tsl2.nano.incubation.specification.rules.RuledEnabler;
import de.tsl2.nano.util.FilePath;

public class SpecificationH5Exchange extends SpecificationExchange {
	
	public int enrichFromSpecificationProperties() {
    	String file = FILENAME_SPEC_PROPERTIES;
		Properties spec = ENV.getSortedProperties(file);
		if (Util.isEmpty(spec)) {
			LOG.info("file " + file + " emtpy or not existing. try to load from csv file...");
			spec = fromCSV();
			file += EXT_CSV;
			if (Util.isEmpty(spec))
				LOG.info("file " + file + " emtpy or not existing. try to load from markdown file...");
				spec = fromMarkdown();
				file += EXT_MARKDOWN;
				if (Util.isEmpty(spec))
					LOG.warn(file + " is empty!");
		}
		int errors = 0, rules = 0, attributes = 0, actions = 0, beanchanges = 0, attrchanges = 0;
    	if (spec != null) {
        	ENV.moveBackup(file);
        	LOG.info("=> importing " + spec.size() + " entries from " + file + " ...");
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
					if (k.matches(pool.getFullExpressionPattern())) { // rule
						pool.add(k, v);
					} else if (k.endsWith(PATH_POSTIFX)){ // set any attribute property
						String type = StringUtil.substring(object, null, ".");
						bean = BeanDefinition.getBeanDefinition(type);
						IAttributeDefinition attr = bean.getAttribute(StringUtil.substring(object, type + ".", "."));
						AttributeDefinition.getAttributePropertyFromPath(k.substring(0, k.length() - 1)).setValue(v);
						attrchanges++;
					} else if (k.startsWith(">")) { //workflow
						String mdfile = ENV.getConfigPath() + "/" + k.substring(1) + SpecificationExchange.EXT_MARKDOWN;
						FilePath.write(mdfile, v.getBytes());
						// TODO implement Workflow
					} else {
						if (!object.contains(".")) { // bean
							bean = BeanDefinition.getBeanDefinition(object);
							if (property.startsWith(addattribute.name())) {
								checkRule(pool, v);
								addVirtualAttribute(bean, v);
								attributes++;
							} else if (property.startsWith(addaction.name())) {
								checkRule(pool, v);
								bean.addAction(new SpecifiedAction<>(v, null));
								actions++;
							} else {
								switch (Change.valueOf(property)) {
								case valueexpression: bean.setValueExpression(new ValueExpression<>(v)); break;
								case attributefilter: bean.setAttributeFilter(v.split("[;,\\s]")); break;
								case icon: bean.getPresentable().setIcon(ENV.getConfigPathRel() + v); break;
								default:
									throw new IllegalArgumentException(property);
								}
								beanchanges++;
							}
						} else { // field
							AttributeDefinition<?> attr = (AttributeDefinition<?>) AttributeDefinition.getAttributeDefinitionFromIDPath(object);
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
							attrchanges++;
						}
					}
					LOG.info("specification imported: " + k + " -> " + v);
				} catch (Exception e1) {
					LOG.error(e1);
					spec.put(e.getKey(), "!!!" + e.getValue() + "!!! " + e1.getMessage());
					errors++;
				}
			}
    		if (errors > 0)
    			LOG.error(errors + " errors on import from file " + file);
    		else
    			LOG.info("<= import of " + (rules + attributes + actions + beanchanges + attrchanges) + " specification entries finished successfull"
    					+ "\n\trules        : " + rules
    					+ "\n\tattributes   : " + attributes
    					+ "\n\tactions      : " + actions
    					+ "\n\tbean changes : " + beanchanges
    					+ "\n\tattr changes : " + attrchanges
    					+ "\n\n");
    		FileUtil.saveProperties(ENV.getConfigPath() + file + (errors == 0 ? ".done" : ".errors"), spec);
    	}
    	return errors;
	}

	protected Properties fromCSV() {
		Scanner sc = Util.trY(() -> new Scanner(FileUtil.userDirFile(ENV.getConfigPath() + FILENAME_SPEC_PROPERTIES + EXT_CSV)), false);
		if (sc == null)
			return null;
		Properties p = MapUtil.createSortedProperties();
		String l, k, v;
		while (sc.hasNextLine()) {
			l = sc.nextLine();
			k = StringUtil.substring(l, null, SEP);
			v = StringUtil.substring(l, SEP, null).replace(SEP, ":");
			if (v.endsWith(":"))
				v = v.substring(0, v.length() - 1);
			p.put(k, v);
		}
		return p;
	}

	protected Properties fromMarkdown() {
		// TODO: implement
		Scanner sc = Util.trY(() -> new Scanner(FileUtil.userDirFile(ENV.getConfigPath() + FILENAME_SPEC_PROPERTIES + EXT_MARKDOWN)), false);
		if (sc == null)
			return null;
		Properties p = MapUtil.createSortedProperties();
		String l, k, v;
		while (sc.hasNextLine()) {
			l = sc.nextLine();
			k = StringUtil.substring(l, null, SEP);
			v = StringUtil.substring(l, SEP, null).replace(SEP, ":");
			if (v.endsWith(":"))
				v = v.substring(0, v.length() - 1);
			p.put(k, v);
		}
		return p;
	}

	static void checkRule(Pool pool, String v) {
		pool.get(v);
	}
}
