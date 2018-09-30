package de.tsl2.nano.h5.annotation;

import java.io.Serializable;

import de.tsl2.nano.annotation.extension.AnnotationFactory;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.h5.configuration.BeanConfigurator;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class ControllerAnnotationFactory implements AnnotationFactory<BeanDefinition, Controller> {

    @Override
    public void build(BeanDefinition instance, Controller a) {
        Class<? extends Serializable> targetType = !isDefaultTargetType(a) ? a.targetType() : instance.getClazz();
        BeanConfigurator configurator = (BeanConfigurator) BeanConfigurator.create(instance.getClazz()).getInstance();
        configurator.createControllerBean(a.baseType().getName(), a.baseAttribute(), targetType.getName(), a.targetAttribute(), a.iconAttribute(),
            a.increaseAttribute(), a.increaseCount(), a.increaseStep());
    }

    private boolean isDefaultTargetType(Controller a) {
        return a.targetType().equals(Serializable.class);
    }

}
