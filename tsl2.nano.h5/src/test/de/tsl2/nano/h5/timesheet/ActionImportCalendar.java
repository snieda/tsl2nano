/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 18.03.2016
 * 
 * Copyright: (c) Thomas Schneider 2016, all rights reserved
 */
package de.tsl2.nano.h5.timesheet;

import de.tsl2.nano.bean.def.SecureAction;

/**
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$ 
 */
public class ActionImportCalendar extends SecureAction {
    /**
     * constructor
     */
    public ActionImportCalendar() {
        super("ics.import.calendar");
    }
    
    @Override
    public Object action() throws Exception {
        return ICSChargeImport.doImportICS((String) getParameter(0));
    }
    @Override
    public Class[] getArgumentTypes() {
        return new Class[]{String.class};
    }
}
