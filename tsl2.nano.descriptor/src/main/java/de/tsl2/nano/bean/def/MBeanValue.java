package de.tsl2.nano.bean.def;

import java.util.Map;

import de.tsl2.nano.core.util.parser.SerialClass;

@SerialClass(attributeOrder = {"attribute", "name", "description", "constraint", "presentation", "secure", "selector"})
public class MBeanValue<T> extends BeanValue<T> {
    String name;
    
    protected MBeanValue() {
        aquireAttribute();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void aquireAttribute() {
        if (attribute == null)
            attribute = new MapValue("onconstruction-" + System.currentTimeMillis(), String.class, null);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public MBeanValue(Object name, Map instance, MapValue mapValue) {
        super(instance, mapValue);
        if (name != null)
        setName(name.toString());
    }

    @Override
    public void setName(String name) {
        this.name = name;
        aquireAttribute();        
        super.setName(name);
    }
}
