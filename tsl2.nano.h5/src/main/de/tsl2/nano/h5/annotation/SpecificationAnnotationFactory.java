package de.tsl2.nano.h5.annotation;

import de.tsl2.nano.annotation.extension.AnnotationFactory;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.h5.annotation.Specification.SpecificationType;
import de.tsl2.nano.h5.configuration.BeanConfigurator;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class SpecificationAnnotationFactory implements AnnotationFactory<BeanDefinition, Specification> {
    static final String TYPE_MARKER = "§%!?@";
    @Override
    public void build(BeanDefinition instance, Specification a) {
        BeanConfigurator configurator = (BeanConfigurator) BeanConfigurator.create(instance.getClazz()).getInstance();
        configurator.actionCreateRuleOrAction(a.name(), typePrefix(a.specificationType()), a.expression());
    }
    public static String typePrefix(SpecificationType specificationType) {
        return String.valueOf(TYPE_MARKER.charAt(specificationType.ordinal()));
    }

}
