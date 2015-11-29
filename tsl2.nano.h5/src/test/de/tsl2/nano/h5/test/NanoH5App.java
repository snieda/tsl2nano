package de.tsl2.nano.h5.test;

import java.io.IOException;

import org.apache.commons.logging.Log;

import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.IPageBuilder;
import de.tsl2.nano.bean.def.ValueExpression;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.h5.NanoH5;

public class NanoH5App extends NanoH5 {
    protected static final Log LOG = LogFactory.getLog(NanoH5App.class);

    public NanoH5App() throws IOException {
        super();
    }

    public NanoH5App(String serviceURL, IPageBuilder<?, String> builder) throws IOException {
        super(serviceURL, builder);
    }

    protected String ve(String expression) {
        return "{" + expression + "}";
    }

    /**
     * define
     * @param valueExpression 
     * @param type 
     */
    protected <T> BeanDefinition<T> define(Class<T> type, String valueExpression, String...attributeFilter) {
        BeanDefinition<T> bean = BeanDefinition.getBeanDefinition(type);
        String ve = valueExpression.contains("{") ? valueExpression : "{" + valueExpression + "}";
        bean.setValueExpression(new ValueExpression<T>(ve, type));
        if (!Util.isEmpty(attributeFilter))
            bean.setAttributeFilter(attributeFilter);
        bean.saveDefinition();
        return bean;
    }
    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        startApplication(NanoH5App.class, MapUtil.asMap(0, "service.url"), args);
    }
}