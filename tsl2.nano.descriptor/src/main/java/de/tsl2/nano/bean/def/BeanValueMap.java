package de.tsl2.nano.bean.def;

import java.util.Map;
import java.util.Objects;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.parser.Serial;
import de.tsl2.nano.core.util.parser.SerialClass;

/**
 * bean extension holding a map as istance to be de-/serializable on StructParser.
 * kind of dynamic bean (virtual) without type and its fixed attributes.
 * 
 * usable to transfer any data with its presentation
 */
@SuppressWarnings("rawtypes")
@SerialClass(attributeOrder = {"id", "name", "clazz", "valueExpression", "attributeNames", "beanValues", "presentation"})
public class BeanValueMap extends Bean<Map> {
    private static final Log LOG = LogFactory.getLog(BeanValueMap.class);

    public BeanValueMap(Map map) {
        super(map);
        if (map.containsKey("name"))
            name = map.get("name").toString();
        else if (getId() != null)
            name = getId().toString();
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

    // @Override
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

    @Override
    public BeanClass<Map>.BeanMap map() {
        // TODO Auto-generated method stub
        return new BeanMap();
    }

    public class BeanMap extends BeanClass.BeanMap {
        @Override
        protected Object createInstanceFromValueMap(Map values) {
            return super.createInstanceFromValueMap(values);
        }
    }
}
