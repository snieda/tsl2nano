package de.tsl2.nano.h5;

import static de.tsl2.nano.h5.NanoH5Util.LOG;
import static de.tsl2.nano.h5.NanoH5Util.addListener;
import static de.tsl2.nano.h5.NanoH5Util.addVirtualAttribute;
import static de.tsl2.nano.h5.NanoH5Util.cover;
import static de.tsl2.nano.h5.NanoH5Util.createCompositor;
import static de.tsl2.nano.h5.NanoH5Util.createController;
import static de.tsl2.nano.h5.NanoH5Util.createQuery;
import static de.tsl2.nano.h5.NanoH5Util.createStatistic;
import static de.tsl2.nano.h5.NanoH5Util.createUser;
import static de.tsl2.nano.specification.SpecificationExchange.Change.addaction;
import static de.tsl2.nano.specification.SpecificationExchange.Change.addattribute;
import static de.tsl2.nano.specification.SpecificationExchange.Change.createcompositor;
import static de.tsl2.nano.specification.SpecificationExchange.Change.createcontroller;
import static de.tsl2.nano.specification.SpecificationExchange.Change.createquery;
import static de.tsl2.nano.specification.SpecificationExchange.Change.createsheet;
import static de.tsl2.nano.specification.SpecificationExchange.Change.createstatistics;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;

import de.tsl2.nano.bean.def.AttributeDefinition;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.IAttributeDefinition;
import de.tsl2.nano.bean.def.PathExpression;
import de.tsl2.nano.bean.def.ValueExpression;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.exception.Message;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.h5.collector.CSheet;
import de.tsl2.nano.specification.Pool;
import de.tsl2.nano.specification.SpecificationExchange;
import de.tsl2.nano.specification.rules.RuledEnabler;

public class SpecificationH5Exchange extends SpecificationExchange {
	
	private static final String DIV = "[:;,\\s]";

	@Override
	public int enrichFromSpecificationProperties() {
    	String file = FILENAME_SPEC_PROPERTIES;
		Properties spec = ENV.getSortedProperties(file);
		if (Util.isEmpty(spec)) {
			LOG.info("file " + file + " emtpy or not existing. try to load from csv file...");
			spec = fromFlatFile((file += EXT_CSV), SEP);
		}
		exists = spec != null;
		int errors = 0, rules = 0, attributes = 0, actions = 0, beanchanges = 0, attrchanges = 0;
    	if (spec != null && Message.ask("Run Specification Exchange on file " + file + "?", true)) {
        	LOG.info("=> importing " + spec.size() + " entries from " + file + " ...");
    		Pool pool = ENV.get(Pool.class);
    		String k, v, object, property;
    		Set<BeanDefinition> changedBeans = new LinkedHashSet<>();
    		BeanDefinition bean = null;
    		for (Entry<Object, Object> e : spec.entrySet()) {
    			// as we get all entries by a map, we have to filter comments ourself
    			try {
    				k = e.getKey().toString().trim();
        			// as we get all entries by a map, we have to filter comments ourself
    				if (k.trim().startsWith("#") /*|| k.trim().startsWith("\\#")*/)
    					continue;
    				
    				v = (String)e.getValue().toString().trim();
					object = StringUtil.substring(k, null, ".", true);
					property = StringUtil.substring(k, ".", null, true);
					if (k.matches(pool.getFullExpressionPattern())) { // rule
						pool.add(k, v);
					} else if (k.matches("createuser")) { // user
						String[] args = getArgs(v);
						createUser(args[0], args[1], args[2], args[3], Boolean.valueOf(args[4]));
					} else if (k.endsWith(PATH_POSTIFX)){ // set any attribute property
						String type = StringUtil.substring(object, null, ".");
						bean = BeanDefinition.getBeanDefinition(type);
						IAttributeDefinition attr = bean.getAttribute(StringUtil.substring(object, type + ".", "."));
						AttributeDefinition.getAttributePropertyFromPath(k.substring(0, k.length() - 1)).setValue(v);
						attrchanges++;
					} else {
						if (!object.contains(".")) { // bean
							bean = BeanDefinition.getBeanDefinition(object);
							if (property.startsWith(addattribute.name())) {
								if (!PathExpression.isPath(v))
									checkRule(pool, v);
								addVirtualAttribute(bean, v);
								attributes++;
							} else if (property.startsWith(addaction.name())) {
								checkRule(pool, v);
								bean.addAction(new SpecifiedAction<>(v, null));
								actions++;
							} else if (property.startsWith(createstatistics.name())) {
								createStatistic(bean.getDeclaringClass());
							} else if (property.startsWith(createquery.name())) {
								createQuery(k, v);
							} else if (property.startsWith(createcompositor.name())) {
								String[] args = getArgs(v);
								BeanDefinition<?> baseType = BeanDefinition.getBeanDefinition(args[0]);
								String baseAttribute = args[1];
								createCompositor(baseType.getDeclaringClass(), baseAttribute, bean.getDeclaringClass(), args[2], args[3]);
							} else if (property.startsWith(createcontroller.name())) {
								String[] args = getArgs(v);
								BeanDefinition<?> baseType = BeanDefinition.getBeanDefinition(args[0]);
								String baseAttribute = args[1];
								createController(baseType.getDeclaringClass(), baseAttribute, bean.getDeclaringClass(), args[2], args[3], args[4]);
							} else if (property.startsWith(createsheet.name())) {
								String[] args = getArgs(v);
								new CSheet(args[0], Integer.valueOf(args[1]), Integer.valueOf(args[2])).save();
							} else {
								switch (Change.valueOf(property)) {
								case valueexpression: bean.setValueExpression(new ValueExpression<>(v)); break;
								case attributefilter: bean.setAttributeFilter(getArgs(v)); break;
								case icon: bean.getPresentable().setIcon(v); break;
								default:
									throw new IllegalArgumentException(property);
								}
								beanchanges++;
							}
						} else { // field
							AttributeDefinition<?> attr = (AttributeDefinition<?>) AttributeDefinition.getAttributeDefinitionFromIDPath(object);
							bean = attr.getParentBean();
							String rule;
							switch (Change.valueOf(property)) {
							case enabler: 
								checkRule(pool, v); attr.getPresentation().setEnabler(new RuledEnabler(attr, v)); 
								break;
							case listener: 
								rule = StringUtil.subRegex(v, null, DIV, 0);
								String[] observables = StringUtil.subRegex(v, DIV, null, 0).split(DIV);
								checkRule(pool, rule); 
								addListener(attr.getParentBean(), attr.getName(), rule, observables); 
								break;
							case rulecover: 
								String child = StringUtil.subRegex(v, DIV, null, 0);
								rule = getArgs(v)[0];
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
					if (bean != null)
						changedBeans.add(bean);
				} catch (Exception e1) {
					LOG.error(e1);
					spec.put(e.getKey(), "!!!" + e.getValue() + "!!! " + e1.getMessage());
					errors++;
				}
			}
    		if (errors > 0)
    			Message.send(errors + " errors on import from file " + file);
            ENV.moveBackup(file);
			LOG.info("<= import of " + (rules + attributes + actions + beanchanges + attrchanges) + " specification entries finished successfull"
					+ "\n\trules        : " + rules
					+ "\n\tattributes   : " + attributes
					+ "\n\tactions      : " + actions
					+ "\n\tbean changes : " + beanchanges
					+ "\n\tattr changes : " + attrchanges
					+ "\n\n");
    		changedBeans.stream().forEach(b -> b.saveDefinition());
    		FileUtil.saveProperties(Pool.getSpecificationRootDir() + file + (errors == 0 ? ".done" : ".errors"), spec);
    	}
    	return errors;
	}

	private String[] getArgs(String v) {
		return v.split(DIV);
	}

	protected Properties fromFlatFile(String filename, String sep) {
		File file = FileUtil.userDirFile(ENV.getConfigPath() + filename);
		if (!file.exists())
			return null;
		Scanner sc = Util.trY(() -> new Scanner(file), false);
		if (sc == null)
			return null;
		Properties p = MapUtil.createSortedProperties();
		String l, l0 = "", k, keq, v, s;
		while (sc.hasNextLine()) {
			l = sc.nextLine();
			if (l.trim().length() == 0 || l.trim().startsWith("#"))
				continue;
			l = l0 + l;
			if (l.endsWith( "\\")) {
				l0 = l.substring(0, l.length() - 1) + "\n";
				continue;
			} else
				l0 = "";
			k = StringUtil.substring(l, null, sep);
			keq = StringUtil.substring(l, null, "=");
			if (keq.length() < k.length() && keq.substring(1).trim().matches("[*\\w.]+")) {
				k = keq;
				s = "=";
			} else
				s = sep;
			v = StringUtil.substring(l, s, null);//.replace(s, ":");
//			if (v.endsWith(":"))
//				v = v.substring(0, v.length() - 1);
			p.put(k, v);
		}
		return p;
	}

	static void checkRule(Pool pool, String v) {
		pool.get(v);
	}
}
