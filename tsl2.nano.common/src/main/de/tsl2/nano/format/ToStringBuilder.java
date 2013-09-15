/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: ts
 * created on: 13.09.2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.format;

import org.apache.commons.lang.builder.StandardToStringStyle;

/**
 * Encapsulates the ToStringBuilder of apache commons lang. The method ToStringBuilder.reflectionToString(obj) is an optional use through CompatibilityLayer.
 * @author ts
 * @version $Revision$ 
 */
public class ToStringBuilder {

    private static final StandardToStringStyle STYLE_TOSTRING = new StandardToStringStyle();
    static {
            STYLE_TOSTRING.setUseShortClassName(true);
            STYLE_TOSTRING.setUseIdentityHashCode(false);
            STYLE_TOSTRING.setArrayStart("[");
            STYLE_TOSTRING.setArraySeparator(", ");
            STYLE_TOSTRING.setArrayEnd("]");
            STYLE_TOSTRING.setNullText("%NULL%");
            STYLE_TOSTRING.setSizeStartText("%SIZE=");
            STYLE_TOSTRING.setSizeEndText("%");
            STYLE_TOSTRING.setSummaryObjectStartText("%");
            STYLE_TOSTRING.setSummaryObjectEndText("%");
            org.apache.commons.lang.builder.ToStringBuilder.setDefaultStyle(STYLE_TOSTRING);
        }

    public static String reflectionToString(Object obj) {
        return org.apache.commons.lang.builder.ToStringBuilder.reflectionToString(obj);
    }
}
