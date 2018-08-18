package de.tsl2.nano.h5.annotation;

import de.tsl2.nano.annotation.extension.AnnotationFactory;
import de.tsl2.nano.bean.def.AttributeDefinition;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.h5.Html5Presentation;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class DependencyListenerAnnotationFactory implements AnnotationFactory<AttributeDefinition, DependencyListener> {

    @Override
    public void build(AttributeDefinition attr, DependencyListener a) {
        Html5Presentation helper = (Html5Presentation)BeanDefinition.getBeanDefinition(attr.getDeclaringClass()).getPresentationHelper();
        helper.addRuleListener(attr.getName(), a.rule(), a.listenerType().ordinal(), a.observables());
    }

}
