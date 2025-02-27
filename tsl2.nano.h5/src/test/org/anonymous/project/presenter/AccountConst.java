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
account.name=Name
account.name.tooltip=Name tooltip...
account.id=Id
account.id.tooltip=Id tooltip...
account.discharges=Discharges
account.discharges.tooltip=Discharges tooltip...
account.bic=Bic
account.bic.tooltip=Bic tooltip...
account.iban=Iban
account.iban.tooltip=Iban tooltip...
 * BLOCKEND */

/**
 * Attribute and message label key definition for Bean class.
 * <p>
 * <b>Generated</b> do not modify!!!
 *
 * @author Generated through velocity template (beanconstant.vm)
 * @version $Revision$ 
 */
public interface AccountConst {

    /** key name of name in resource bundle */
    public static final String  KEY_NAME = "account.name";
    public static final String  KEY_TOOLTIP_NAME = "account.name.tooltip";
    /** key name of id in resource bundle */
    public static final String  KEY_ID = "account.id";
    public static final String  KEY_TOOLTIP_ID = "account.id.tooltip";
    /** key name of discharges in resource bundle */
    public static final String  KEY_DISCHARGES = "account.discharges";
    public static final String  KEY_TOOLTIP_DISCHARGES = "account.discharges.tooltip";
    /** key name of bic in resource bundle */
    public static final String  KEY_BIC = "account.bic";
    public static final String  KEY_TOOLTIP_BIC = "account.bic.tooltip";
    /** key name of iban in resource bundle */
    public static final String  KEY_IBAN = "account.iban";
    public static final String  KEY_TOOLTIP_IBAN = "account.iban.tooltip";

    /** bean attribute name */
    public static final String  ATTR_NAME = "name";
    /** bean attribute id */
    public static final String  ATTR_ID = "id";
    /** bean attribute discharges */
    public static final String  ATTR_DISCHARGES = "discharges";
    /** bean attribute bic */
    public static final String  ATTR_BIC = "bic";
    /** bean attribute iban */
    public static final String  ATTR_IBAN = "iban";
}
