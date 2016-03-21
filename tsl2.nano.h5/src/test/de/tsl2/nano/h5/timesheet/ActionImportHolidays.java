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
public class ActionImportHolidays extends SecureAction {
    /** serialVersionUID */
    private static final long serialVersionUID = -2850207237378238545L;

    /**
     * constructor
     */
    public ActionImportHolidays() {
        super("ics.import.holidays");
    }
    
    @Override
    public Object action() throws Exception {
        return ICSChargeImport.doImportHolidays();
    }
    
    @Override
    public String getImagePath() {
        return "icons/calendar.png";
    }
}
