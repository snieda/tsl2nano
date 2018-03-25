package de.tsl2.nano.h5;

import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.ValueExpression;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.util.Util;

public interface NanoH5Util {

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
        bean.setPresentationHelper(new Html5Presentation<>(bean));
        String ve = valueExpression.contains("{") ? valueExpression : "{" + valueExpression + "}";
        bean.setValueExpression(new ValueExpression<T>(ve, type));
        if (!Util.isEmpty(attributeFilter))
            bean.setAttributeFilter(attributeFilter);
        if (icon != null)
            bean.getPresentable().setIcon(ENV.getConfigPathRel() + icon.toString());
        bean.saveDefinition();
        return bean;
    }

    /**
     * icon
     * @param name 
     * @return
     */
    static StringBuilder icon(String name) {
        return new StringBuilder("icons/" + name + ".png");
    }


}
