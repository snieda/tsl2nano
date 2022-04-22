/*
 * Copyright © 2002-2009 Thomas Schneider
 * Alle Rechte vorbehalten.
 * Weiterverbreitung, Benutzung, Vervielfältigung oder Offenlegung,
 * auch auszugsweise, nur mit Genehmigung.
 * 
 * $Id$ 
 */
package de.tsl2.nano.bean;

import java.text.Format;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.logging.Log;

import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.BeanCollector;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.IAttributeDefinition;
import de.tsl2.nano.bean.def.NamedValue;
import de.tsl2.nano.collection.CollectionUtil;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.Messages;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.cls.IAttribute;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.DefaultFormat;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.ListSet;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.core.util.NumberUtil;
import de.tsl2.nano.core.util.ObjectUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.format.RegExpFormat;
import de.tsl2.nano.util.operation.IConverter;

/**
 * A Utility-Class for beans
 * 
 * @author ts 05.03.2009
 * @version $Revision$
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class BeanUtil extends ObjectUtil {
	private static final Log LOG = LogFactory.getLog(BeanUtil.class);

	/**
     * as BeanUtil.copyProperties(..) does only a shallow copy, this method does a deep recursive copy.
     * <p/>
     * uses lamdas of jdk 1.8. handles: iterable, enum, number - not handled yet: map, array
     */
/*    @SuppressWarnings({ "rawtypes", "unchecked" })
private static Object deepCopy(Object src, Object dest) throws Exception {
  BeanUtils.copyProperties(src, dest);
  PropertyDescriptor[] pdSrc = BeanUtils.getPropertyDescriptors(src.getClass());
  PropertyDescriptor pdDest;
  String name;
  Object vold, vnew;
  Class type;
  for (int i = 0; i < pdSrc.length; i++) {
    name = pdSrc[i].getName();
    vold = null;
    vnew = null;
    pdDest = BeanUtils.getPropertyDescriptor(dest.getClass(), name);
    if (pdDest != null && pdSrc[i].getReadMethod() != null && (vold = pdSrc[i].getReadMethod().invoke(src)) != null
        && (pdDest.getReadMethod() == null || (vnew = pdDest.getReadMethod().invoke(dest)) == null)) {
      type = pdDest.getReadMethod().getReturnType();
      //create instance of enum, number or entity
      vnew = type.isEnum() ? Enum.valueOf((Class) type, ((Enum) vold).name())
          : Number.class.isAssignableFrom(type) ? type.getConstructor(String.class).newInstance(vold.toString()) : type.newInstance();
      //set the new value and go into recursion
      pdDest.getWriteMethod().invoke(dest, deepCopy(vold, vnew));
    } else if (vold != null && Iterable.class.isAssignableFrom(type = pdDest.getReadMethod().getReturnType())) {
      //handle collections with recursion
      Collection it = (Collection) vold;
      Collection cnew = (Collection) vnew;
      Object genType = pdDest.getReadMethod().getGenericReturnType();
      if (genType instanceof ParameterizedType) {
        genType = ((ParameterizedType) genType).getActualTypeArguments()[0];
      }
      if (genType instanceof Class) {
        Class gtype = (Class) genType;
        boolean javaType = gtype.getPackage().getName().startsWith("java");
        it.forEach((Object o) -> {
          try {
            cnew.add(javaType ? o : deepCopy(o, gtype.newInstance()));
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
      }
    }
  }
  return dest;
}
*/
    /**
     * creates new objects, cloned from defaultClone and with increasing attribute value - evaluated by first+count+step.
     * @param defaultClone
     * @param attributeName
     * @param first first attribute value - defaultClone can contain the value or this must not be null
     * @param count number of objects to create
     * @param step step of increase
     * @return list of new created objects
     */
    public static <S>  List<S> create(S defaultClone, String attributeName, Object first, int count, double step) {
        Object clone, value;
        ArrayList result = new ArrayList();
        if (first == null) {
            first = Bean.getBean(defaultClone).getAttribute(attributeName).getValue();
        }
        long start = NumberUtil.toNumber(first);
        long end = (long) (start + count * step);
        for (long i = start; i < end; i+=step) {
            clone = clone(defaultClone);
            value = NumberUtil.fromNumber(i, first.getClass());
            Bean.getBean(clone).getAttribute(attributeName).setValue(value);
            result.add(clone);
        }
        return result;
    }
    
    /**
     * wraps all attributes having a collection as value into a new {@link ListSet} instance to unbind a
     * {@link #clone()} instance.
     * 
     * @param src instance to wrap the attribute values for
     * @return the instance itself
     */
    public static <S> S createOwnCollectionInstances(S src) {
        BeanClass<S> bc = (BeanClass<S>) BeanClass.getBeanClass(src.getClass());
        List<IAttribute> attributes = bc.getAttributes();
        for (IAttribute a : attributes) {
            if (Collection.class.isAssignableFrom(a.getType())) {
                Collection v = (Collection) a.getValue(src);
                if (v != null) {
                    LOG.debug("creating own collection instance for " + a.getName() + " with" + v.size() + " elements");
                    a.setValue(src, new ListSet(v));
                }
            }
        }
        return src;
    }

    /**
     * calls a method through reflection
     * 
     * @param clazz class to load
     * @param method method to call
     * @param args optional arguments
     * @return result of method calling
     */
    public static Object call(String clazz, String method, Object... args) {
        return BeanClass.createBeanClass(clazz).callMethod(null, method, null, args);
    }

    /**
     * calls {@link #call(String, String, Object...)} to start any java method
     * 
     * @param args
     */
    public static final void main(String args[]) {
        System.out.println("calling BeanUtil.main");
        String clazz = args[0];
        String method = args[1];
        Object[] objs = CollectionUtil.copyOfRange(args, 2, args.length, Object[].class);
        call(clazz, method, objs);
    }

    /**
     * fills a map with all bean-attribute-names and their values
     * 
     * @param o bean
     * @return see {@link Bean#toValueMap()}
     */
    public static Map<String, Object> toValueMap(Object o) {
        return toValueMap(o, false, false, false);
    }

    /**
     * fills a map with all bean-attribute-names and their values
     * 
     * @param o bean
     * @param useClassPrefix if true, the class-name will be used as prefix for the key
     * @param onlySingleValues if true, collections will be ignored
     * @param onlyFilterAttributes if true, all other than filterAttributes will be ignored
     * @param filterAttributes attributes to be filtered (ignored, if onlyFilterAttributes)
     * @return see {@link Bean#toValueMap()}
     */
    public static Map<String, Object> toValueMap(Object o,
            boolean useClassPrefix,
            boolean onlySingleValues,
            boolean onlyFilterAttributes,
            String... filterAttributes) {
        return Bean.getBean(o).toValueMap(o, useClassPrefix, onlySingleValues, onlyFilterAttributes, filterAttributes);
    }

    /**
     * delegates to {@link #toValueMap(Object, String, boolean, boolean, String...)} with onlyFilteredAttributes = true.
     */
    public static Map<String, Object> toValueMap(Object o,
            String keyPrefix,
            boolean onlySingleValues,
            String... filterAttributes) {
        return toValueMap(o, keyPrefix, onlySingleValues, true, filterAttributes);
    }

    /**
     * fills a map with all bean-attribute-names and their values
     * 
     * @param o java instance
     * @param keyPrefix key prefix to be used for each attribute name. must not be null - use an empty string instead!
     * @param onlySingleValues if true, collections will be ignored
     * @param onlyFilteredAttributes if true, only the filterAttributes will be filled - otherwise, all others will be
     *            filled.
     * @param filterAttributes attributes to be filtered
     * @return see {@link Bean#toValueMap()}
     */
    public static Map<String, Object> toValueMap(Object o,
            String keyPrefix,
            boolean onlySingleValues,
            boolean onlyFilteredAttributes,
            String... filterAttributes) {
        BeanDefinition beandef = BeanDefinition.getBeanDefinition(BeanClass.getDefiningClass(o.getClass()));
//        if (onlyFilteredAttributes && filterAttributes.length > 0) {
//            //attributes will be changed - so we have to use an own instance
//            beandef = new BeanDefinition(BeanClass.getDefiningClass(o.getClass()));
//            beandef.setAttributeFilter(filterAttributes);
//        } else {
//            beandef = BeanDefinition.getBeanDefinition(BeanClass.getDefiningClass(o.getClass()));
//        }
        return beandef.toValueMap(o, keyPrefix, onlySingleValues, onlyFilteredAttributes, filterAttributes);
    }

    public static <T> T fromValueMap(Class<T> type, Map<String, Object> values) {
        return BeanDefinition.getBeanDefinition(type).fromValueMap(values);
    }

    public static String toJSON(Object... instances) {
    	StringBuilder buf = new StringBuilder();
    	for (int i = 0; i < instances.length; i++) {
            buf.append(MapUtil.toJSon(toValueMap(instances[i])) + ",");
		}
    	return instances.length > 0 ? buf.deleteCharAt(buf.length() - 1).toString(): buf.toString();
    }

    /** only flat object, no recursion yet! */
    public static <T> T fromJSON(Class<T> type, String json) {
        return (T) fromValueMap(type, MapUtil.fromJSon(json));
    }
    /**
     * delegates to {@link #toFormattedMap(Object, String, boolean)}.
     */
    public static Map<String, Object> toFormattedMap(Object bean) {
        return toFormattedMap(bean, null, true, new DefaultFormat());
    }

    /**
     * provides a map containing all formatted single value attributes of the given bean
     * 
     * @param bean bean to evaluate
     * @param keyPrefix (optional) key name prefix (normally ending with a dot)
     * @param translateKeys if true, all keys will be translated
     * @param filterAttributes attributes to be filtered (ignored)
     * @param format formatter (use {@link DefaultFormat} if unknown)
     * @return map containing formatted values
     */
    public static Map<String, Object> toFormattedMap(Object bean,
            String keyPrefix,
            boolean translateKeys,
            Format format,
            String... filterAttributes) {
        final Map<String, Object> valueMap = keyPrefix != null ? BeanUtil.toValueMap(bean,
            keyPrefix,
            true,
            filterAttributes) : BeanUtil.toValueMap(bean, true, true, false, filterAttributes);
        final Set<String> keySet = valueMap.keySet();
        final Map<String, Object> formattedMap = new LinkedHashMap<String, Object>();
        for (final String k : keySet) {
            final String key = translateKeys ? Messages.getString(k) : k;
            formattedMap.put(key, format.format(valueMap.get(k)));
        }
        return formattedMap;
    }

    /**
     * returns the value itself - or if null the defaultIfNull
     * 
     * @param <T>
     * @param value value or null
     * @param defaultIfNull default value if value is null
     * @return value of defaultIfNull
     */
    public static final <T> T valueOf(T value, T defaultIfNull) {
        return value != null ? value : defaultIfNull;
    }

    /**
     * simple delegation to {@link #valueOf(Object, Object)}.
     */
    public static final <T> T defaultValue(T value, T defaultIfNull) {
        return valueOf(value, defaultIfNull);
    }

    /**
     * createUUID (see {@link UUID#randomUUID()}
     * 
     * @return random uuid string (128-bit value)
     */
    public static final String createUUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * creates a hashcode through all single-value attibutes of given bean instance
     * 
     * @param bean instance to evaluate the hashcode for
     * @param attributes (optional) attributes to be used for hashcode
     * @return new hashcode for given bean instance
     */
    public static int hashCodeReflect(Object bean, String... attributes) {
        final int prime = 31;
        int result = 1;
        BeanClass bc = BeanClass.getBeanClass(bean.getClass());
        if (attributes == null) {
            attributes = bc.getAttributeNames();
        }
        Object v;
        for (int i = 0; i < attributes.length; i++) {
            v = BeanClass.getValue(bean, attributes[i]);
            result = prime * result + (v == null ? 0 : v.hashCode());
        }
        return result;
    }

    /**
     * if not already a bean or beancollector, the given object will be wrapped into a bean or beancollector. if it is not
     * serializable, a map of values will be packed into a beancollector.
     * 
     * @param obj
     * @return
     */
    public static BeanDefinition<?> getBean(Object obj) {
    	return (BeanDefinition<?>) (obj instanceof BeanDefinition<?> 
    		? obj
    			: Bean.canWrap(obj)
    				? Bean.getBean(obj)
    					: Util.isContainer(obj)
    						? BeanCollector.getBeanCollector(CollectionUtil.getContainer(obj), 0)
    							: BeanCollector.getBeanCollector(CollectionUtil.getContainer(BeanUtil.toValueMap(obj)), 0));
    }

    public static <T> Collection<NamedValue> asNamedCollection(Map<?, T> m) {
        LinkedList<NamedValue> list = new LinkedList<NamedValue>();
        NamedValue.putAll(m, list);
        return list;
    }

    /**
     * delegates to {@link #getParser(Class, String, String, String, IConverter, boolean)} using @id attribute and
     * cache.
     */
    public static <TYPE> RegExpFormat getParser(final Class<TYPE> type,
            String pattern,
            final IConverter<String, Object> converter) {
        //workaround to have a simple instance for calling getIdAttribute(). poor performance - but works
        TYPE instance = BeanClass.createInstance(type);
        return RegExpFormat.getParser(type, BeanContainer.getIdAttribute(instance).getName(), pattern, null, converter, true);
    }

}
