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
            List<BeanValue> bvs = (List<BeanValue>) BeanClass.getStatic(BeanValue.class, "beanValueCache");
            @Override
            public Iterable<String> getAvailables() {
                return CollectionUtil.getTransforming(bvs, new ITransformer<BeanValue, String>() {
                    @Override
                    public String transform(BeanValue toTransform) {
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

}
