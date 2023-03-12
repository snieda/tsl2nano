package de.tsl2.nano.h5;

import java.io.Serializable;
import java.util.Collection;

import org.apache.commons.logging.Log;

import de.tsl2.nano.bean.def.AttributeDefinition;
import de.tsl2.nano.bean.def.BeanCollector;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.Constraint;
import de.tsl2.nano.bean.def.IAttributeDefinition;
import de.tsl2.nano.bean.def.ValueExpression;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.h5.collector.CSheet;
import de.tsl2.nano.h5.collector.Controller;
import de.tsl2.nano.h5.collector.QueryResult;
import de.tsl2.nano.h5.collector.Statistic;
import de.tsl2.nano.h5.configuration.BeanConfigurator;
import de.tsl2.nano.specification.Pool;
import de.tsl2.nano.specification.SpecificationExchange;
import de.tsl2.nano.specification.actions.Action;

/**
 * convenience class to create and configure bean types and their attributes.
 * 
 * @author ts
 */
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

    static void define(String attributePropertyPath, Object value) {
    	AttributeDefinition.getAttributePropertyFromPath(attributePropertyPath).setValue(value);
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
//		IPRunnable rule = ENV.get(Pool.class).get(prefixedRuleName);
		return bc(bean.getDeclaringClass()).addAttribute(null, prefixedRuleName);
//		return bean.addAttribute(new RuleExpression<>(bean.getClazz(), prefixedRuleName));
	}

	public static <T> Statistic<Collection<T>, T> createStatistics(Class<T> type) {
        return createStatistics(type, "icons/barchart.png");
	}

    public static <T> Statistic<Collection<T>, T> createStatistics(Class<T> type, String iconPath) {
        Statistic<Collection<T>, T> statistic = new Statistic<>(type);
        statistic.getPresentable().setIcon(iconPath);
        statistic.onActivation(null).saveDefinition();
        return statistic;
	}
	
	public static QueryResult createQuery(String name, String sqlQuery) {
		return QueryResult.createQueryResult(name, sqlQuery);
	}
	
	public static CSheet createSheet(String title, int rows, int cols) {
		CSheet sheet = new CSheet(title, cols, rows);
		sheet.save();
		return sheet;
	}

	public static <T extends Serializable> BeanCollector<Collection<T>, T> createCompositor(Class<?> baseType, String baseAttribute, Class<T> targetType, String targetAttribute, 
			String iconAttribute) {
		return bc(targetType).createCompositor(baseType.getName(), baseAttribute, targetAttribute, iconAttribute);
	}

    /**
     * define a Controller as Collector of Actions of a Bean
     */
	public static <T extends Serializable> Controller<Collection<T>, T> createController(
			Class<?> baseType, String baseAttribute, Class<T> targetType, String targetAttribute, 
			String iconAttribute, String increaseAttribute) {
		return bc(targetType).createControllerBean(baseType.getName(), baseAttribute, targetType.getName(), targetAttribute, 
				iconAttribute, increaseAttribute, 1, 1, false, true, false);
	}

	static <T extends Serializable> BeanConfigurator<T> bc(Class<T> type) {
		return BeanConfigurator.create(type).getInstance();
	}
	
	public static User createUser(String name, String passwd, String dbUser, String dbPasswd, boolean admin) {
		return Users.load().auth(name, passwd, dbUser, dbPasswd, admin);
	}
	
    public static int enrichFromSpecificationProperties() {
    	return ENV.get(SpecificationExchange.class).enrichFromSpecificationProperties();
	}
}
