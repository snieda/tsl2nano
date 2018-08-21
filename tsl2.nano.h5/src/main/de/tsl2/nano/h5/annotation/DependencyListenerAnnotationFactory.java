package de.tsl2.nano.h5.annotation;

import de.tsl2.nano.annotation.extension.AnnotationFactory;
import de.tsl2.nano.bean.def.AttributeDefinition;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.BeanPresentationHelper;
import de.tsl2.nano.h5.Html5Presentation;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class DependencyListenerAnnotationFactory implements AnnotationFactory<AttributeDefinition, DependencyListener> {

    @Override
    public void build(AttributeDefinition attr, DependencyListener a) {
        BeanDefinition beanDef = BeanDefinition.getBeanDefinition(attr.getDeclaringClass());
        BeanPresentationHelper helper = beanDef.getPresentationHelper();
        if (!(helper instanceof Html5Presentation))
            helper = new Html5Presentation<>(beanDef);
        ((Html5Presentation)helper).addRuleListener(attr, a.rule(), a.listenerType().ordinal(), a.observables());
    }

}
