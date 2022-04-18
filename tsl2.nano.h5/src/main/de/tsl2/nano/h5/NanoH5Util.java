package de.tsl2.nano.h5;

import java.lang.reflect.Method;
import java.util.Properties;

import org.apache.commons.logging.Log;

import java.util.Map.Entry;

import de.tsl2.nano.bean.def.AttributeDefinition;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.Constraint;
import de.tsl2.nano.bean.def.IAttributeDefinition;
import de.tsl2.nano.bean.def.ValueExpression;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.execution.ScriptUtil;
import de.tsl2.nano.h5.expression.RuleExpression;
import de.tsl2.nano.incubation.specification.Pool;
import de.tsl2.nano.incubation.specification.actions.Action;
import de.tsl2.nano.incubation.specification.rules.RuledEnabler;

import static de.tsl2.nano.bean.BeanUtil.*;

public interface NanoH5Util {
	static final Log LOG = LogFactory.getLog(NanoH5Util.class);
	
	static String ve(String expression) {
        return "{" + expression + "}";
    }

    /**
     * define
     * 
     * @param valueExpression
     * @param type
     */
    static <T> BeanDefinition<T> define(Class<T> type, StringBuilder icon, String valueExpression, String... attributeFilter) {
        BeanDefinition<T> bean = BeanDefinition.getBeanDefinition(type);
        assureHtml5Presentation(bean);
        String ve = valueExpression.contains("{") ? valueExpression : "{" + valueExpression + "}";
        bean.setValueExpression(new ValueExpression<T>(ve, type));
        if (!Util.isEmpty(attributeFilter))
            bean.setAttributeFilter(attributeFilter);
        if (icon != null)
            bean.getPresentable().setIcon(ENV.getConfigPathRel() + icon.toString());
        bean.saveDefinition();
        return bean;
    }

	static <T> void assureHtml5Presentation(BeanDefinition<T> bean) {
		if (!(bean.getPresentationHelper() instanceof Html5Presentation))
        		bean.setPresentationHelper(new Html5Presentation<>(bean));
	}

    /**
     * icon
     * @param name 
     * @return
     */
    static StringBuilder icon(String name) {
        return new StringBuilder("icons/" + name + ".png");
    }

    /**
     * @param cls class holding the source method
     * @param methodName method name 
     * @param methodArgTypes methods argument types (classes)
     * @param pars optional action argument constraints
     * @return new created Action that is hold and provided by the {@link Pool}.
     */
    static Action<Object> defineAction(Class cls, String methodName, Class[] methodArgTypes, Constraint...pars) {
        java.lang.reflect.Method antCaller = null;
        try {
            antCaller = cls.getMethod(methodName, methodArgTypes);
        } catch (Exception e) {
            ManagedException.forward(e);
        }
        Action<Object> a = new Action<>(antCaller);
        if (pars != null) {
        	for (int i = 0; i < pars.length; i++) {
                a.addConstraint("arg" + i, pars[i]);
			}
        }
        ENV.get(Pool.class).add(a);
        return a;
    }
    
    public static void addListener(BeanDefinition<?> bean, String observer, String rule, String... observables) {
    	assureHtml5Presentation(bean);
    	((Html5Presentation)bean.getPresentationHelper()).addRuleListener(observer, rule, 2, observables);
    }
    
    public static RuleCover cover(IAttributeDefinition<?> attr, String child, String rule) {
    	return RuleCover.cover(attr, child, rule);
    }
    
	public static AttributeDefinition addVirtualAttribute(BeanDefinition bean, String prefixedRuleName) {
		return bean.addAttribute(new RuleExpression<>(bean.getClazz(), prefixedRuleName));
	}

    public static int enrichFromSpecificationProperties() {
    	String file = FILENAME_SPEC_PROPERTIES;
		Properties spec = ENV.getSortedProperties(file);
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
							if (property.startsWith(PREFIX_ATTRIBUTE)) {
								checkRule(pool, v);
								addVirtualAttribute(bean, v);
							} else if (property.startsWith(PREFIX_ACTION)) {
								checkRule(pool, v);
								bean.addAction(new SpecifiedAction<>(v, null));
							} else {
								switch (property) {
								case PROP_VALUEEXPRESSION: bean.setValueExpression(new ValueExpression<>(v)); break;
								case PROP_ATTRIBUTEFILTER: bean.setAttributeFilter(v.split("[;,\\s]")); break;
								case PROP_ICON: bean.getPresentable().setIcon(ENV.getConfigPathRel() + v); break;
								default:
									throw new IllegalArgumentException(property);
								}
							}
						} else { // field
							AttributeDefinition<?> attr = (AttributeDefinition<?>) AttributeDefinition.getAttributeDefinitionFromPath(object);
							String rule;
							switch (property) {
							case PROP_ENABLER: 
								checkRule(pool, v); attr.getPresentation().setEnabler(new RuledEnabler(attr, v)); 
								break;
							case PROP_LISTENER: 
								rule = StringUtil.substring(v, null, ":");
								String[] observables = StringUtil.substring(v, ":", null).split("[,;\\s]");
								checkRule(pool, rule); 
								addListener(attr.getParentBean(), attr.getName(), rule, observables); 
								break;
							case PROP_RULECOVER: 
								String child = StringUtil.substring(v, null, ":");
								rule = StringUtil.substring(v, ":", null);
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

	static void checkRule(Pool pool, String v) {
		pool.get(v);
	}

}
