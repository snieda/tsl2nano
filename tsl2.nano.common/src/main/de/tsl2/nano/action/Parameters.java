/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 07.04.2017
 * 
 * Copyright: (c) Thomas Schneider 2017, all rights reserved
 */
package de.tsl2.nano.action;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.simpleframework.xml.Attribute;

import de.tsl2.nano.bean.def.IConstraint;

/**
 * Parameter handler for IAction
 * 
 * @author Tom
 * @version $Revision$
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class Parameters extends ArrayList<Parameter> {
    /** serialVersionUID */
    private static final long serialVersionUID = -508314601327290933L;

    /** compatibility mode to old Object[] */
    @Attribute
    boolean onlyAvailables = true;
    /**
     * constructor
     */
    public Parameters() {
    }

    /**
     * constructor
     * 
     * @param initialCapacity
     */
    public Parameters(int initialCapacity) {
        super(initialCapacity);
    }

    /**
     * constructor
     * 
     * @param c
     */
    public Parameters(Collection<? extends Parameter> c) {
        super(c);
    }

    /**
     * getNames
     * 
     * @return
     */
    public String[] getNames() {
        List arrayList = new ArrayList();
        for (Parameter p : this) {
            arrayList.add(p.getName());
        }
        return arrayList.size() > 0 ? (String[]) arrayList.toArray(new String[0]) : null;
    }

    /**
     * getTypes
     * 
     * @return
     */
    public Class[] getTypes() {
        List arrayList = new ArrayList();
        IConstraint<?> c;
        for (Parameter p : this) {
            c = p.getConstraint();
            if (!onlyAvailables || c != null)
                arrayList.add(c != null ? p.getConstraint().getType() : Object.class);
        }
        return arrayList.size() > 0 ? (Class[]) arrayList.toArray(new Class[0]) : null;
    }

    /**
     * getValues
     * 
     * @return
     */
    public Object[] getValues() {
        List arrayList = new ArrayList();
        for (Parameter p : this) {
            Object v = p.getValue();
            if (!onlyAvailables || v != null) //this is compatible to old code, but may create errors on the new structure...
                arrayList.add(v);
        }
        return arrayList.size() > 0 ? arrayList.toArray(new Object[0]) : null;
    }

    public Object getValue(int i) {
        return i < size() ? get(i).getValue() : null;
    }

    public Object getValue(String name) {
        for (Parameter p : this) {
            if (name.equals(p.getName()))
                return p.getValue();
        }
        return null;
    }

    /**
     * setParameter
     * 
     * @param args
     */
    public void setValues(Object... args) {
        int i = 0;
        for (Parameter p : this) {
            p.setValue(args[i++]);
        }
        for (int j = i; j < args.length; j++) {
            add(new Parameter("arg" + j, null, args[j]));
        }
    }
}
