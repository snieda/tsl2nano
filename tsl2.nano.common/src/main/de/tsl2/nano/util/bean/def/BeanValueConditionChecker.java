/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Oct 15, 2010
 * 
 * Copyright: (c) Thomas Schneider 2010, all rights reserved
 */
package de.tsl2.nano.util.bean.def;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import de.tsl2.nano.exception.FormattedException;

/**
 * condition checker.
 * is able to check given 'must have' and 'must not have' constraints.
 * </p>
 * Example:<pre>
 * 
 * first, create the constraints:
                    final BeanValueConditionChecker checker = new BeanValueConditionChecker();
                    final BeanValue name1Value = BeanValue.getBeanValue(searchBean, AktenSuchParameterConst.ATTR_NAME1);
                    final BeanValue name2Value = BeanValue.getBeanValue(searchBean, AktenSuchParameterConst.ATTR_NAME2);
                    checker.add(name2Value, Arrays.asList(name1Value), null);
 * means: if the attribute nam2 is filled, the attribute name1 must be filled, too.
 * 
 * now, we check that, perhaps in a save method:
 *                  checker.check();
 * if the check fails, a readable exception will be thrown.
 * </pre>
 * @author Thomas Schneider
 * @version $Revision$
 */
public class BeanValueConditionChecker implements Serializable {
    /** serialVersionUID */
    private static final long serialVersionUID = -5150482606347127502L;
    
    Map<BeanValue, Collection<BeanValue>> mustHave = new LinkedHashMap<BeanValue, Collection<BeanValue>>();
    Map<BeanValue, Collection<BeanValue>> mustNotHave = new LinkedHashMap<BeanValue, Collection<BeanValue>>();

    /**
     * constructor to be serializable
     */
    public BeanValueConditionChecker() {
        super();
    }

    /**
     * add constraint
     * 
     * @param attribute attribute to constrain
     * @param mustHave must have (not null) constraints
     * @param mustNotHave must not have (null) constraints
     */
    public void add(BeanValue attribute, Collection<BeanValue> mustHave, Collection<BeanValue> mustNotHave) {
        this.mustHave.put(attribute, mustHave);
        this.mustNotHave.put(attribute, mustNotHave);
    }

    /**
     * checks all given conditions and throws exception on constraint failure.
     */
    public void check() {
        //Must Have
        final Collection<BeanValue> mustHaveKeys = mustHave.keySet();
        for (final BeanValue beanValue : mustHaveKeys) {
            if (beanValue.getValue() != null) {
                final Collection<BeanValue> mustHaveValues = mustHave.get(beanValue);
                if (mustHaveValues != null) {
                    for (final BeanValue mhValue : mustHaveValues) {
                        if (mhValue.getValue() == null) {
                            throw new FormattedException("tsl2nano.musthavefailure",
                                new Object[] { beanValue.getName(), mhValue.getName() });
                        }
                    }
                }
            }
        }
        //Must Not Have
        final Collection<BeanValue> mustNotHaveKeys = mustNotHave.keySet();
        for (final BeanValue beanValue : mustNotHaveKeys) {
            if (beanValue.getValue() != null) {
                final Collection<BeanValue> mustNotHaveValues = mustNotHave.get(beanValue);
                if (mustNotHaveValues != null) {
                    for (final BeanValue mnhValue : mustNotHaveValues) {
                        if (mnhValue.getValue() != null) {
                            throw new FormattedException("tsl2nano.mustnothavefailure",
                                new Object[] { beanValue.getName(), mnhValue.getName() });
                        }
                    }
                }
            }
        }
    }
}
