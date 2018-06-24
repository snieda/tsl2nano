/**
 * 
 */
package de.tsl2.nano.core.cls;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.messaging.EventController;
import de.tsl2.nano.core.util.CollectionUtil;
import de.tsl2.nano.core.util.FormatUtil;
import de.tsl2.nano.core.util.ListSet;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;

/**
 * Evaluates values through a bean path. See {@link #getValueAccess(Object, String...)}
 * @author Tom
 *
 */
@SuppressWarnings("rawtypes")
public class ValuePath {
    private static final Log LOG = LogFactory.getLog(ValuePath.class);

    /**
     * evaluate the value of the given bean attribute path. if a relation is an iterable or map, it can be specified
     * through a parameter.
     * <p/>
     * Example:
     * 
     * <pre>
     * 1. customer.address[first].city
     * 2. customer.address[0].city                        <- the first address
     * 3. customer.address.city                           <- the first address
     * 4. customer.address[last].street                   <- the last address
     * 5. customer.address[-1].code                       <- the last address
     * 6. customer.address[street=berlinerstrasse].city
     * 7. customer.address[new].city                      <- create a new address
     * 8. customer.address[?street=berlinerstrasse].city  <- searches for given address, if not found, create a new one
     * 9. customer.address[?2].city                       <- find the third address, or if not existing, create a new one 
     * 10 customer.address[?].city                        <- find the first address, or if not existing, create a new one 
     * </pre>
     * 
     * @param bean starting instance
     * @param path full relation path
     * @return attribute value or null
     */
    //TODO: refactore to use recursion and the operation-package instead of if-clauses
	public static IValueAccess getValueAccess(Object bean, String... path) {
        if (bean == null || path == null)
            throw new IllegalArgumentException("bean and path must not be null!");
        Object value = bean;
        IValue result = null;
        for (int i = 0; i < path.length; i++) {
            try {
                //eval the parameters of the last relation
                String par = i > 0 ? StringUtil.substring(path[i - 1], "[", "]", false, true) : null;
                boolean orNew = false;
                boolean isNew = false;
                if (par != null && par.startsWith("?")) {
                	par = par.substring(1);
                	orNew = true;
                }
                if (value == null) { //-> last value was null
                    if ("new".equalsIgnoreCase(par) || orNew) {
                    	value = newValue(result);
                    	isNew = true;
                    } else {
                        LOG.info("attribute '" + path[i] + "' of full path '" + StringUtil.toString(path, 1000) + "' is null");
                    	return null;
                    }
                }
                value = handleIterableValues(result, value, par, orNew, isNew);
                value = handleMapValues(result, value, par, orNew, isNew);
                String name = StringUtil.substring(path[i], null, "[");
                //don't use the performance enhanced BeanValue.getBeanValue in cause dependency-cycles
               	result = createValueAccess(value, name);
                value = getBeanValue(value, name);
            } catch (final Exception ex) {
                throw new ManagedException("Error on attribute path '" + StringUtil.toString(path, 1000)
                    + "'! Attribute '"
                    + path[i]
                    + "' not available!", ex);
            }
        }
        return result;
	}

	private static Object handleMapValues(IValue result, Object value, String par, boolean orNew, boolean isNew) {
    	if (value instanceof Map) {
            Map map = (Map) value;
            if (map.size() == 0) {
                return null;
            }
            value = map.get(par);
            if (value == null && orNew)
            	value = map.values().iterator().next();
        }
		return value;
	}

	private static Object handleIterableValues(IValue result, Object value, String par, boolean orNew, boolean isNew) {
		if (value.getClass().isArray()) { //TODO what's about primitive arrays?
			value = Arrays.asList(value);
		}
		if (value instanceof Iterable) {
			Iterable iter = (Iterable) value;
			int p;
			if (!iter.iterator().hasNext()) {
				return null;
			} else if ("first".equalsIgnoreCase(par)) {
				p = 0;
			} else if ("last".equalsIgnoreCase(par)) {
				p = -1;
			} else if (par != null && par.contains("=")) {
				String att = StringUtil.substring(par, null, "=");
				String val = StringUtil.substring(par, "=", null);
				p = -2;
				if (isNew) {
					int ii = 0;
					for (Object item : iter) {
						if (getBeanValue(item, att) == null) {
							setParsedBeanValue(item, att, val);
							p = ii;
							break;
						}
						ii++;
					}
				} else {
					int ii = 0;
					for (Object item : iter) {
						if (val.equals(Util.asString(getBeanValue(item, att)))) {
							p = ii;
							break;
						}
						ii++;
					}
					if (p == -2 && orNew) {
						Object newValue = createEntry(result);
						setParsedBeanValue(newValue, att, val);
						((Collection) iter).add(newValue);
						p = ((Collection) iter).size() - 1;
					}
				}
			} else if (par != null) {
				p = Integer.valueOf(par);
			} else {
				p = 0;
			}
			if (p >= 0)
				value = CollectionUtil.get(iter, p);
			else if (orNew)
				value = iter.iterator().next();
			else
				throw new IllegalArgumentException(value + par);
		}
		return value;
	}

	private static Object newValue(IValue va) {
		Class type = va.getType();
		if (type.isInterface()) {
			if (Set.class.isAssignableFrom(type) || List.class.isAssignableFrom(type))
				va.setValue(new ListSet(createEntry(va)));
			else if (Map.class.isAssignableFrom(type))
				va.setValue(new HashMap());
			else //TODO: what's about proxies?
				throw new IllegalArgumentException("path error: " + va.getType() + " must not be null!");
		} else {
			va.setValue(BeanClass.createInstance(type));
		}
		return va.getValue();
	}

    private static Object createEntry(IValue va) {
		return BeanClass.createInstance(va.getBeanAttribute().getGenericType());
	}

	protected static Object getBeanValue(Object instance, String attributeName) {
        final BeanAttribute attribute = BeanAttribute.getBeanAttribute(instance.getClass(), attributeName);
        return attribute.getValue(instance);
    }

	protected static void setParsedBeanValue(Object instance, String attributeName, String value) {
        final BeanAttribute attribute = BeanAttribute.getBeanAttribute(instance.getClass(), attributeName);
        try {
			attribute.setValue(instance, FormatUtil.getDefaultFormat(attribute.getType(), true).parseObject(value));
		} catch (ParseException e) {
			ManagedException.forward(e);
		}
	}
	protected static void setBeanValue(Object instance, String attributeName, Object value) {
        final BeanAttribute attribute = BeanAttribute.getBeanAttribute(instance.getClass(), attributeName);
        attribute.setValue(instance, value);
    }

    protected static IValue createValueAccess(Object instance, String attributeName) {
        final BeanAttribute attribute = BeanAttribute.getBeanAttribute(instance.getClass(), attributeName);
        return new IValue() {
			@Override
			public Object getValue() {
				return attribute.getValue(instance);
			}
			@Override
			public void setValue(Object newValue) {
				attribute.setValue(instance, newValue);
			}
			@Override
			public Class<Object> getType() {
				return attribute.getType();
			}
			@Override
			public EventController changeHandler() {
				throw new UnsupportedOperationException();
			}
			@Override
			public String toString() {
				return Util.toString(IValueAccess.class, getType(), attribute.name);
			}
			@Override
			public BeanAttribute getBeanAttribute() {
				return attribute;
			}
		};
    }

}
interface IValue extends IValueAccess {
	BeanAttribute getBeanAttribute();
}