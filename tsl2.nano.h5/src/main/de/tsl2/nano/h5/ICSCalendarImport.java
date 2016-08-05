/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 12.03.2016
 * 
 * Copyright: (c) Thomas Schneider 2016, all rights reserved
 */
package de.tsl2.nano.h5;

import java.text.SimpleDateFormat;
import java.util.Collection;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ICallback;
import de.tsl2.nano.core.util.NetUtil;
import de.tsl2.nano.scanner.ICSCalendarReader;
import de.tsl2.nano.util.Period;

/**
 * imports a holiday ics calendar from "http://www.kayaposoft.com/enrico/ics/v1.0?country=deu&fromDate=01-01-2016&toDate=31-12-2016&region=Bavaria".
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class ICSCalendarImport {
    private static final SimpleDateFormat SDF = new SimpleDateFormat("dd-MM-yyyy");
    
    /**
     * importCalendar into Charge
     * 
     * @param country
     * @param region
     * @param period
     * @return count of entries
     */
    protected <T> Collection<T> importCalendar(String country, String region, Period period, ICallback<T> callback) {
        String holServiceURL = ENV.get("app.holiday.service.url",
            "http://www.kayaposoft.com/enrico/ics/v1.0") 
                + "?country=" + country + "&fromDate=" + SDF.format(period.getStart()) + "&toDate=" + SDF.format(period.getEnd()) + "&region=" + region;
        NetUtil.download(holServiceURL, ENV.getTempPathRel());
        String file = getDownloadFile(country, region, period);
        return (Collection<T>) ICSCalendarReader.forEach(file, callback);
    }

    protected String getDownloadFile(String country, String region, Period period) {
        return ENV.getTempPathRel() + country + " (" + region + ").ics";
    }
}
