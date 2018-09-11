package de.tsl2.nano.h5.annotation;

import de.tsl2.nano.annotation.extension.AnnotationFactory;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.h5.configuration.BeanConfigurator;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class ControllerAnnotationFactory implements AnnotationFactory<BeanDefinition, Controller> {

    @Override
    public void build(BeanDefinition instance, Controller a) {
        BeanConfigurator configurator = (BeanConfigurator) BeanConfigurator.create(instance.getClazz()).getInstance();
        configurator.actionCreateController(a.increaseAttribute(), a.increaseCount(), a.increaseStep(),
            a.baseType().getName(), a.baseAttribute(), a.targetAttribute(), a.iconAttribute());
    }

}
