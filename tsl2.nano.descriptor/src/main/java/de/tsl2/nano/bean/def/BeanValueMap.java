package de.tsl2.nano.bean.def;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.cls.IAttribute;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.core.util.parser.Serial;
import de.tsl2.nano.core.util.parser.SerialClass;

/**
 * bean extension holding a map as istance to be de-/serializable on StructParser.
 * kind of dynamic bean (virtual) without type and its fixed attributes.
 * 
 * usable to transfer any data with its presentation
 */
@SuppressWarnings("rawtypes")
@SerialClass(attributeOrder = {"id", "name", "clazz", "instance", "presentation", "valueExpression", "attributeDefs"})
public class BeanValueMap extends Bean<Map> {
    private static final Log LOG = LogFactory.getLog(BeanValueMap.class);

    private static final List<String> COMMON_ATTRIBUTES = Arrays.asList("id", "name");

    public BeanValueMap(Map map) {
        super(map);
        if (map.containsKey("name"))
            name = map.get("name").toString();
        else if (getId() != null)
            name = getId().toString();
        createMapValueAttributes(map, this);            
    }

    public static Bean<Map> createMapValueAttributes(Map map, Bean<Map> bean) {
        bean.setMultiValue(false);
        Set keySet = map.keySet();
        Object v;
        if (map.keySet() != null) {//on a proxy instance, keySet() may return null!
            for (Object k : keySet) {
                v = map.get(k);
                if (isOwnBeanAttribute(k)) {
                    if (k.equals("instance") && v instanceof Map) {
                        bean.setInstance((Map) v);
                    } else if (k.equals("beanValues") && v instanceof List) {
                        addAllBeanValues(bean, (List)v);
                    } else {
                        setOwnBeanAttribute(bean, (String)k, v);
                    }
                } else {
                    addMapValue(bean, map, k, v);
                }
            }
            bean.allDefinitionsCached = true;
        }
        return bean;
    }

    @SuppressWarnings("unchecked")
    private static void addMapValue(Bean<Map> bean, Map map, Object k, Object v) {
        bean.addAttribute(
            new MBeanValue(k, bean.instance,
                new MapValue(k, (v != null ? BeanClass.getDefiningClass(v
                    .getClass()) : null), map)));
    }

    private static void addAllBeanValues(Bean<Map> bean, List mappedBeanValues) {
        // mappedBeanValues.forEach(bv -> addMapValue(bean, map, bv.););
    }

    @SuppressWarnings("unchecked")
    private static void setOwnBeanAttribute(Object bean, String name, Object value) {
        // BeanDefinition<BeanValueMap> def = BeanDefinition.getBeanDefinition(BeanValueMap.class);
        // def.getAttribute(name).setValue(bean, value);
    }

    private static boolean isOwnBeanAttribute(Object k) {
        if (!(k instanceof String) || COMMON_ATTRIBUTES.contains(k))
            return false;
        BeanDefinition<BeanValueMap> def = BeanDefinition.getBeanDefinition(BeanValueMap.class);
        return Arrays.asList(def.getAttributeNames()).contains(k);
    }

    @Override
    public String[] getAttributeNames() {
        // as allDefinitionsCached will be set to false on each addAttribut....
        return Util.isEmpty(attributeFilter) ?  super.getAttributeNames() : attributeFilter;
    }

    @Override
    @Serial(type = Map.class)
    public Class<Map> getClazz() {
        if (!Map.class.isAssignableFrom(clazz))
            clazz = Map.class;
        return super.getClazz();
    }

    @Override
    protected void setClazz(Class<Map> cls) {
        if (!Map.class.isAssignableFrom(clazz))
            throw new IllegalArgumentException("clazz must be of type map!");
        super.setClazz(cls);
    }

    @SuppressWarnings("unchecked")
    public List<MBeanValue> getAttributeDefs() {
        return (List<MBeanValue>)Util.untyped(super.getAttributes());
    }

    // @Override
    @SuppressWarnings("unchecked")
    public void setAttributeDefs(List<MBeanValue> attributes) {
        BeanDefinition<MBeanValue> beanDef = BeanDefinition.getBeanDefinition(MBeanValue.class);
        List<IAttribute> definedAttributes = beanDef.getAttributes();
        attributes.forEach(a -> {
            BeanValue bv_ = (BeanValue) getAttribute(a.getName(), false);
            boolean addIt = bv_ == null;
            final BeanValue bv = bv_ != null ? bv_ : a;

            if (!addIt) {
                definedAttributes.forEach(da -> {
                    // use the filled deep structure from given beanvalues (perhaps givne by structparser)
                    if (!Util.isSimpleType(da.getType()) && !da.getType().getPackage().getName().startsWith("java")) {
                        
                        Object deepValue = da.getValue(a);
                        if (deepValue != null)
                            da.setValue(bv, deepValue);
                    }
                });
            }
            if (bv.instance == null) {
                bv.instance = instance;
            }
            if (addIt)
                this.getAttributeDefinitions().put(bv.getName(), bv);
        });
        allDefinitionsCached = true;
    }

    // @Override
    @SuppressWarnings("unchecked")
    public <M extends Map> Bean<M> setInstance(M instance) {
        Objects.requireNonNull(instance);
        if (instance instanceof Map)
            super.setInstance((Map) instance);
        else
            LOG.warn("not an instanceof Map --> ignoring instance of type " + instance.getClass().getName());
        return (Bean<M>) this;    
    }

    @Override
    public Object getId() {
        return instance.get("id");
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setId(Object value) {
        instance.put("id", value);
    }
}
