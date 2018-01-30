/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 24.03.2017
 * 
 * Copyright: (c) Thomas Schneider 2017, all rights reserved
 */
package de.tsl2.nano.bean.def;

import java.util.LinkedList;
import java.util.List;

import de.tsl2.nano.collection.CollectionUtil;
import de.tsl2.nano.core.ITransformer;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.util.FuzzyFinder;
import de.tsl2.nano.util.IFuzzyDescriptor;

/**
 * 
 * @author Tom
 * @version $Revision$ 
 */
public class AttributeFinder extends FuzzyFinder<String> {

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public AttributeFinder() {
        super(new IFuzzyDescriptor<String>() {
//            List<BeanValue> bvs = (List<BeanValue>) BeanClass.getStatic(BeanValue.class, "beanValueCache");
            List<BeanDefinition> bds = (List<BeanDefinition>) BeanClass.getStatic(BeanDefinition.class, "virtualBeanCache");
            
            @Override
            public Iterable<String> getAvailables() {
                return CollectionUtil.getTransforming(collectAttributes(bds), new ITransformer<AttributeDefinition, String>() {
                    @Override
                    public String transform(AttributeDefinition toTransform) {
                        return toTransform.getPath();
                    }
                });
            }

            @Override
            public double distance(String item, String expression) {
                return StringUtil.fuzzyMatch(item, expression);
            }
        });
    }
    @SuppressWarnings({ "rawtypes", "unchecked" })
    static List<AttributeDefinition> collectAttributes(List<BeanDefinition> bds) {
        List<AttributeDefinition> attrs = new LinkedList<AttributeDefinition>();
        for (BeanDefinition bd : bds) {
            attrs.addAll(bd.getAttributeDefinitions().values());
        }
        return attrs;
    }
}
