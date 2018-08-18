package de.tsl2.nano.h5.annotation;

import java.util.Collection;

import de.tsl2.nano.annotation.extension.AnnotationFactory;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.h5.QueryResult;

@SuppressWarnings({ "rawtypes" })
public class QueryAnnotationFactory implements AnnotationFactory<BeanDefinition, Query> {

    @Override
    public void build(BeanDefinition instance, Query a) {
        QueryResult<Collection<Object>, Object> qr = new QueryResult<>(a.name());
        qr.getPresentable().setIcon(a.icon());
        qr.saveDefinition();
    }

}
