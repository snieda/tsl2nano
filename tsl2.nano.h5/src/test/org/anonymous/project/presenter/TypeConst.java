/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: velocity template (codegen/beanconstant.vm)
 * created on: ----/--/-- (not filled with property 'time' to simplify version-diffs)
 * 
 * Copyright (c) 2002-2015 Thomas Schneider
 */
package org.anonymous.project.presenter;

/*
 * Resource bundle preparation: copy this block to your resource bundle!
 * 
 * BLOCKSTART
type.area=Area
type.area.tooltip=Area tooltip...
type.name=Name
type.name.tooltip=Name tooltip...
type.id=Id
type.id.tooltip=Id tooltip...
type.icon=Icon
type.icon.tooltip=Icon tooltip...
 * BLOCKEND */

/**
 * Attribute and message label key definition for Bean class.
 * <p>
 * <b>Generated</b> do not modify!!!
 *
 * @author Generated through velocity template (beanconstant.vm)
 * @version $Revision$ 
 */
public interface TypeConst {

    /** key name of area in resource bundle */
    public static final String  KEY_AREA = "type.area";
    public static final String  KEY_TOOLTIP_AREA = "type.area.tooltip";
    /** key name of name in resource bundle */
    public static final String  KEY_NAME = "type.name";
    public static final String  KEY_TOOLTIP_NAME = "type.name.tooltip";
    /** key name of id in resource bundle */
    public static final String  KEY_ID = "type.id";
    public static final String  KEY_TOOLTIP_ID = "type.id.tooltip";
    /** key name of icon in resource bundle */
    public static final String  KEY_ICON = "type.icon";
    public static final String  KEY_TOOLTIP_ICON = "type.icon.tooltip";

    /** bean attribute area */
    public static final String  ATTR_AREA = "area";
    /** bean attribute name */
    public static final String  ATTR_NAME = "name";
    /** bean attribute id */
    public static final String  ATTR_ID = "id";
    /** bean attribute icon */
    public static final String  ATTR_ICON = "icon";
}
