/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 12.11.2015
 * 
 * Copyright: (c) Thomas Schneider 2015, all rights reserved
 */
package de.tsl2.nano.bean.def;

import org.simpleframework.xml.convert.Converter;
import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.OutputNode;

/**
 * TODO: implement default class evaluation, type/style conversions and default values for scale/prec
 * <p/>
 * enhance the readability of beandef xml files. to be used by annotation @Convert(XmlPresentableConverter).
 * 
 * @author Tom
 * @version $Revision$
 */
public class XmlPresentableConverter<T> implements Converter<T> {

    @Override
    public T read(InputNode node) throws Exception {
        InputNode clazz = node.getAttribute("class");
//        if (clazz == null)
//            clazz = new inputnod
        return null;
    }

    @Override
    public void write(OutputNode node, T instance) throws Exception {
//        node.get
    }

}
