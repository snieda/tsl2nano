/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 13.03.2015
 * 
 * Copyright: (c) Thomas Schneider 2015, all rights reserved
 */
package de.tsl2.nano.incubation.terminal;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.simpleframework.xml.Element;

import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.util.StringUtil;

/**
 * creates a list of options for this container, evaluating the given class field types.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class FieldSelector extends Selector<String> {
    /** serialVersionUID */
    private static final long serialVersionUID = -8246582918469244440L;
    /** class holding the field */
    @Element
    String cls;
    /** type of static fields to collect */
    @Element
    String field;

    /**
     * constructor
     */
    public FieldSelector() {
        super();
    }

    /**
     * constructor
     * 
     * @param roots
     * @param filter
     */
    public FieldSelector(String name, String value, String description, Class<?> cls, Class<?> fieldType) {
        super(name, description);
        this.cls = cls.getName();
        this.field = fieldType.getName();
        this.value = value;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    protected List<String> createItems(Map context) {
        String clsName = StringUtil.insertProperties(cls, context);
        Class f = BeanClass.createBeanClass(field).getClazz();
        return Arrays.asList(BeanClass.createBeanClass(clsName).getFieldNames(f, true));
    }

}
