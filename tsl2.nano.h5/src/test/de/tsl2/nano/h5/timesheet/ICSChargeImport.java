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

import java.util.Date;
import java.util.Map;

import org.anonymous.project.Charge;
import org.anonymous.project.Chargeitem;
import org.anonymous.project.Party;

import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.core.ICallback;
import de.tsl2.nano.core.util.DateUtil;
import de.tsl2.nano.h5.ICSCalendarImport;
import de.tsl2.nano.scanner.ICSCalendarReader;
import de.tsl2.nano.util.Period;

import static de.tsl2.nano.scanner.ICSCalendarReader.*;

/**
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$ 
 */
public class ICSChargeImport extends ICSCalendarImport implements ICallback {
    
    public static long doImportHolidays() {
        ICSChargeImport imp = new ICSChargeImport();
        return imp.importCalendar("deu", "Bavaria", new Period(DateUtil.getStartOfYear(null), DateUtil.getEndOfYear(null)), imp);
    }
    
    public static long doImportICS(String icsFile) {
        return ICSCalendarReader.forEach(icsFile, new ICSChargeImport());
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Object run(Map<Object, Object> entry) {
        BeanDefinition<Chargeitem> ciDef = BeanDefinition.getBeanDefinition(Chargeitem.class);
        BeanDefinition<Party> paDef = BeanDefinition.getBeanDefinition(Party.class);
        Charge c = new Charge();
        c.setFromdate((Date) entry.get(START));
        c.setTodate((Date) entry.get(END));
        c.setChargeitem(ciDef.getValueExpression().from((String) entry.get(CATEGORY)));
        c.setComment((String) entry.get(SUMMARY));
        c.setParty(paDef.getValueExpression().from(CLASS));
        BeanContainer.instance().save(c);
        return c;
    }

}
