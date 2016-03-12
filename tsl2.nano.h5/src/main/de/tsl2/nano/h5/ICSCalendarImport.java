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

import java.util.Map;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ICallback;
import de.tsl2.nano.core.util.NetUtil;
import de.tsl2.nano.scanner.ICSCalendarReader;
import de.tsl2.nano.util.Period;

/**
 * imports an holiday ics calendar
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class ICSCalendarImport {
    /**
     * importCalendar
     * 
     * @param country
     * @param region
     * @param period
     * @return count of entries
     */
    public long importCalendar(String country, String region, Period period) {
        String holServiceURL = ENV.get("application.holiday.service.url",
            "http://www.kayaposoft.com/enrico/ics/v1.0?country=deu&fromDate=01-01-2016&toDate=31-12-2016&region=Bavaria");
        NetUtil.download(holServiceURL, ENV.getTempPathRel());
        String file = getDownloadFile(country, region, period);
        long count = ICSCalendarReader.forEach(file, new ICallback() {
            @Override
            public Object run(Map<Object, Object> passInfo) {
                // TODO Auto-generated method stub
                return null;
            }
        });
        return count;
    }

    private String getDownloadFile(String country, String region, Period period) {
        return ENV.getTempPathRel() + country + " (" + region + ").ics";
    }
}
