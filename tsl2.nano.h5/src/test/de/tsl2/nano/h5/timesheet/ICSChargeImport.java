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

import static de.tsl2.nano.scanner.ICSCalendarReader.CATEGORY;
import static de.tsl2.nano.scanner.ICSCalendarReader.CLASS;
import static de.tsl2.nano.scanner.ICSCalendarReader.END;
import static de.tsl2.nano.scanner.ICSCalendarReader.START;
import static de.tsl2.nano.scanner.ICSCalendarReader.SUMMARY;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

import org.anonymous.project.Charge;
import org.anonymous.project.Chargeitem;
import org.anonymous.project.Item;
import org.anonymous.project.Party;

import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.core.ICallback;
import de.tsl2.nano.core.exception.Message;
import de.tsl2.nano.core.util.DateUtil;
import de.tsl2.nano.h5.ICSCalendarImport;
import de.tsl2.nano.scanner.ICSCalendarReader;
import de.tsl2.nano.util.Period;

/**
 * import any ICS calendar from file. the CATEGRORIES must exist as project with same name, the CLASS must exist as
 * party with same name.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class ICSChargeImport extends ICSCalendarImport implements ICallback {

    public static long doImportHolidays() {
        ICSChargeImport imp = new ICSChargeImport();
        return imp.importCalendar("deu", "Bavaria",
            new Period(DateUtil.getStartOfYear(null), DateUtil.getEndOfYear(null)), imp);
    }

    public static long doImportICS(String icsFile) {
        return ICSCalendarReader.forEach(icsFile, new ICSChargeImport());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object run(Map<Object, Object> entry) {
        BeanDefinition<Item> itemDef = BeanDefinition.getBeanDefinition(Item.class);
        BeanDefinition<Party> paDef = BeanDefinition.getBeanDefinition(Party.class);
        Charge c = new Charge();
        BeanContainer.createId(c);
        c.setFromdate((Date) entry.get(START));
        c.setFromtime(c.getFromdate());
        c.setTodate((Date) entry.get(END));
        c.setTotime(c.getTodate());
        c.setValue(new BigDecimal(0/*DateUtil.diffHours(c.getFromtime(), c.getTotime())*/));
        Item item = itemDef.getValueExpression().from((String) entry.get(CATEGORY));
        Chargeitem chargeitem = new Chargeitem();
        chargeitem.setItem(item);
        chargeitem = BeanContainer.instance().save(chargeitem);
        c.setChargeitem(chargeitem);
        c.setComment((String) entry.get(SUMMARY));
        c.setParty(paDef.getValueExpression().from((String) entry.get(CLASS)));
        BeanContainer.instance().save(c);
        Bean<Charge> cBean = Bean.getBean(c);
        Message.send("new calendar entry imported: " + cBean);
        return c;
    }

}
