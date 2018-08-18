package de.tsl2.nano.h5.annotation;

import de.tsl2.nano.annotation.extension.AnnotationFactory;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.h5.configuration.BeanConfigurator;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class VirtualAttributeAnnotationFactory implements AnnotationFactory<BeanDefinition, VirtualAttribute> {

    @Override
    public void build(BeanDefinition instance, VirtualAttribute a) {
        BeanConfigurator configurator = (BeanConfigurator) BeanConfigurator.create(instance.getClazz()).getInstance();
        configurator.actionAddAttribute(SpecificationAnnotationFactory.typePrefix(a.specificationType()), a.expression());
    }

}
