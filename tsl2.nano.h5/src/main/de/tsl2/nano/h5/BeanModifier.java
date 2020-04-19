package de.tsl2.nano.h5;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;

import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.BeanValue;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.DateUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.format.RegExpFormat;

public class BeanModifier {
    private static final Log LOG = LogFactory.getLog(BeanModifier.class);

    /**
     * converts the standard date format yyyy-MM-dd to the locale specific date format - to be parseable
     * 
     * @param parms
     */
    void convertDates(Map<String, String> parms) {
        LOG.info("converting dates");
        String v;
        for (String p : parms.keySet()) {
            v = parms.get(p);
            if (v != null && v.matches(RegExpFormat.FORMAT_DATE_SQL)) {
                parms.put(p, DateUtil.getFormattedDate(DateUtil.getDateSQL(v)));
            }
        }
    }

    public void refreshValues(BeanDefinition beandef, Map<String, String> parms) {
        checkSecurity(beandef, parms);
        convertDates(parms);
        LOG.info("refreshing current bean values");
        if (beandef instanceof Bean) {
            Collection<Exception> exceptions = new LinkedList<Exception>();
            Bean vmodel = (Bean) beandef;
            for (String p : parms.keySet()) {
                if (vmodel.hasAttribute(p)) {
                    try {
                        /*
                         * check, if input was changed - so, don't lose instances if unchanged
                         * the oldString was sent to html-page - the newString returns from request
                         */
                        BeanValue bv = (BeanValue) vmodel.getAttribute(p);
                        Class<?> type = bv.getType();
                        /*
                         * if the type is object, the bean doesn't know exactly it's real type, so
                         * we assume it should be serializable...
                         */
                        if (!type.isPrimitive() && !Serializable.class.isAssignableFrom(type)
                            && !Object.class.isAssignableFrom(type)) {
                            LOG.debug("ignoring not-serializable attribute " + vmodel.getAttribute(p));
                            continue;
                        }
                        String oldString = bv.getValueText();
                        String newString = parms.get(p);
                        // checkboxes will send 'on' as value
                        if (boolean.class.isAssignableFrom(type) || Boolean.class.isAssignableFrom(type))
                            if (newString.equals("on"))
                                newString = "true";
                        if (Date.class.isAssignableFrom(type))
                            if (newString.matches("\\d{2,2}[:]\\d{2,2}"))
                                newString += ":00"; //append 0 seconds to respect format HH:mm:ss
                        
                        if (oldString == null || !oldString.equals(newString)) {
                            vmodel.setParsedValue(p, newString);
                        } else {
                            LOG.debug("ignoring unchanged attribute " + vmodel.getAttribute(p));
                        }
                    } catch (Exception e) {
                        exceptions.add(e);
                    }
                }
            }
            /*
             * create one exception, holding all messages of thrown sub-exceptions
             */
            if (exceptions.size() > 0) {
                StringBuffer buf = new StringBuffer();
                for (Exception ex : exceptions) {
                    buf.append(ex.getMessage() + "\n");
                }
                throw new ManagedException(buf.toString(), exceptions.iterator().next());
            }
        }
    }

    static void checkSecurity(Map<String, String> parms) {
        checkSecurity(null, parms);
    }

    static void checkSecurity(BeanDefinition<?> beanDefinition, Map<String, String> parms) {
        String beanName = beanDefinition != null ? beanDefinition.getName() + "." : "";
        String strBlackList = ENV.get("app.input.blacklist", "</,/>");
        String allowedFields = ENV.get("app.input.blacklist.fieldnames.allowed.regex", "ENV.properties");
        List<String> blacklist = Arrays.asList(strBlackList.split(","));
        String v;
        for (String k : parms.keySet()) {
            v = parms.get(k);
            if (!Util.isEmpty(v)) {
                for (String bad : blacklist) {
                    if (v.contains(bad) && !(beanName + k).matches(allowedFields)) {
                        throw new IllegalArgumentException("content of field " + k
                                + " is part of a defined blacklist. to allow such content on that field"
                                + ", add field name in 'app.input.blacklist.fieldnames.allowed.regex'");
                    }
                }
            }
        }
    }

}
