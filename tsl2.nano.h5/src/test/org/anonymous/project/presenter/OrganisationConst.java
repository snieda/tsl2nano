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
organisation.name=Name
organisation.name.tooltip=Name tooltip...
organisation.id=Id
organisation.id.tooltip=Id tooltip...
organisation.icon=Icon
organisation.icon.tooltip=Icon tooltip...
organisation.description=Description
organisation.description.tooltip=Description tooltip...
 * BLOCKEND */

/**
 * Attribute and message label key definition for Bean class.
 * <p>
 * <b>Generated</b> do not modify!!!
 *
 * @author Generated through velocity template (beanconstant.vm)
 * @version $Revision$ 
 */
public interface OrganisationConst {

    /** key name of name in resource bundle */
    public static final String  KEY_NAME = "organisation.name";
    public static final String  KEY_TOOLTIP_NAME = "organisation.name.tooltip";
    /** key name of id in resource bundle */
    public static final String  KEY_ID = "organisation.id";
    public static final String  KEY_TOOLTIP_ID = "organisation.id.tooltip";
    /** key name of icon in resource bundle */
    public static final String  KEY_ICON = "organisation.icon";
    public static final String  KEY_TOOLTIP_ICON = "organisation.icon.tooltip";
    /** key name of description in resource bundle */
    public static final String  KEY_DESCRIPTION = "organisation.description";
    public static final String  KEY_TOOLTIP_DESCRIPTION = "organisation.description.tooltip";

    /** bean attribute name */
    public static final String  ATTR_NAME = "name";
    /** bean attribute id */
    public static final String  ATTR_ID = "id";
    /** bean attribute icon */
    public static final String  ATTR_ICON = "icon";
    /** bean attribute description */
    public static final String  ATTR_DESCRIPTION = "description";
}
