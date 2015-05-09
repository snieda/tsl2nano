/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 30.12.2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.h5.navigation;

import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.util.StringUtil;

/**
 * map of objects to be handled as beans. means, you are able to search for bean-paths inside this map.<p/>
 * Example:<pre>
 * class Organization {
 *  String name;
 * }
 * class Person {
 *  Organization orga;
 * }
 * 
 * map is holding: key=person, value=instance of Person
 * call of get('person.orga.name') will return the persons organization name.
 * 
 * </pre>
 * @author Tom, Thomas Schneider
 * @version $Revision$ 
 */
public class BeanParameter extends Parameter {
    /** serialVersionUID */
    private static final long serialVersionUID = 1L;
    
    @Override
    public Object get(Object key) {
            Object p = super.get(key);
            String name = key.toString();
            if (p != null || name.indexOf('.') == -1) {
                return p;
            }
            //ok, we look for a bean path
            String refName = StringUtil.substring(name, null, ".");
            Object refObject = super.get(refName);
            if (refObject == null) {
                return null;
            }
            return BeanClass.getValue(refObject, StringUtil.substring(name, ".", null));
        }
}
