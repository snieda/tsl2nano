package de.tsl2.nano.h5.annotation;

import de.tsl2.nano.annotation.extension.AnnotationFactory;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.h5.configuration.BeanConfigurator;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class CompositorAnnotationFactory implements AnnotationFactory<BeanDefinition, Compositor> {

    @Override
    public void build(BeanDefinition instance, Compositor a) {
        BeanConfigurator configurator = (BeanConfigurator) BeanConfigurator.create(instance.getClazz()).getInstance();
        configurator.createCompositor(a.baseType().getName(), a.baseAttribute(), a.targetAttribute(), a.iconAttribute());
    }

}
