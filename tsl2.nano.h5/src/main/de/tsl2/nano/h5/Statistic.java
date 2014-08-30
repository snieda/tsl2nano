/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 15.08.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.h5;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.bean.def.ArrayValue;
import de.tsl2.nano.bean.def.AttributeDefinition;
import de.tsl2.nano.bean.def.BeanCollector;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.IAttributeDefinition;
import de.tsl2.nano.core.Environment;
import de.tsl2.nano.core.cls.IAttribute;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.util.NumberUtil;

/**
 * ON CONSTRUCTION
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class Statistic<COLLECTIONTYPE extends Collection<T>, T> extends BeanCollector<COLLECTIONTYPE, T> {
    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    List<String> columnNames;

    public Statistic() {
    }

    /**
     * constructor
     * 
     * @param beanType
     * @param workingMode
     */
    @SuppressWarnings("unchecked")
    public Statistic(Class<T> beanType) {
        super();
        setMode(0);
        columnNames = new LinkedList<String>();
        collection = (COLLECTIONTYPE) create(beanType, columnNames);
        isStaticCollection = true;
    }

    @Override
    public List<IAttribute> getAttributes(boolean readAndWriteAccess) {
        if (Util.isEmpty(attributeDefinitions)) {
            attributeDefinitions = new LinkedHashMap<String, IAttributeDefinition<?>>();
            List<String> names = columnNames;
            IAttribute<?> attribute;
            int i = 0;
            for (String name : names) {
                attribute = new ArrayValue(name, i++, i > 1 ? Number.class : null, null);
                attributeDefinitions.put(name, new AttributeDefinition(attribute));
            }
        }
        return new ArrayList<IAttribute>((Collection) attributeDefinitions.values());
    }

    private <T> Collection<T> create(Class<T> beanType, List<String> attributeNames) {
        attributeNames.add(Environment.translate("tsl2nano.name", true));
        attributeNames.add(Environment.translate("tsl2nano.count", true));

        BeanDefinition<T> def = BeanDefinition.getBeanDefinition(beanType);
        setName(Environment.translate("tsl2nano.statistic", true) + " (" + def.getName() + ")");
        String[] names = def.getAttributeNames();
        List<String> statColumns = new ArrayList<>(names.length);
        List<String> valueColumns = new ArrayList<>(names.length);

        /*
         * check, which columns should be shown. if a column has more than 100 group by elements, its to big
         * evaluate the number columns
         */
        long maxcount = Environment.get("statistic.maxgroupcount", 500);
        String sum = Environment.translate("tsl2nano.sum", true) + "(";
        IAttributeDefinition<?> attrDef;
        for (int i = 0; i < names.length; i++) {
            attrDef = def.getAttribute(names[i]);
            if (attrDef.id() || attrDef.generatedValue() || attrDef.isMultiValue() || attrDef.unique()
                || attrDef.isVirtual())
                continue;
            else if (NumberUtil.isNumber(attrDef.getType())) {
                valueColumns.add(names[i]);
                attributeNames.add(sum + names[i] + ")");
            } else if (groupByCount(def, names[i]) <= maxcount)
                statColumns.add(names[i]);
        }

        /*
         * do all group by selects
         */
        Collection<T> collection = new ArrayList<>();
        for (String n : statColumns) {
            collection.addAll(createStatistics(def, n, valueColumns));
        }
        collection.addAll(createSummary(def, valueColumns));
        return collection;
    }

    private static <T> Collection<? extends T> createSummary(BeanDefinition<T> def, List<String> valueColumns) {
        String summary =
            Environment.translate("tsl2nano.all", true) + " " + Environment.translate("tsl2nano.elements", true);
        String qstr = "select ''{0}'', count(*) {2} from {1}";

        String strValueColumns = StringUtil.concatWrap(",sum({0})".toCharArray(), valueColumns.toArray());
        return BeanContainer.instance().getBeansByQuery(MessageFormat.format(qstr, summary, def, strValueColumns),
            false, (Object[]) null);
    }

    private static <T> Collection<? extends T> createStatistics(BeanDefinition<T> def,
            String column,
            List<String> valueColumns) {
        String qstr = "select ''{3}: '' || {0}, count({0}) {2} from {1} group by {0} order by 1";

        String strValueColumns = StringUtil.concatWrap(",sum({0})".toCharArray(), valueColumns.toArray());
        String columnname = Environment.translate(def.getAttribute(column).getId(), true);
        return BeanContainer.instance().getBeansByQuery(
            MessageFormat.format(qstr, column, def, strValueColumns, columnname),
            false, (Object[]) null);
    }

    private static <T> int groupByCount(BeanDefinition def, String column) {
        String qstr = "select count(" + column + ") from " + def.getName() + " group by " + column;

        //JPA is not able to resolve the expression ..count(count(....)), so we have to use native sql

        Collection<Object> result = BeanContainer.instance().getBeansByQuery(qstr, false, (Object[]) null);
        //Woraround: returning direct size. count(count(.)) would have better performance
        return result.size();
        //JPA-QL sometimes seems to return empty collections on my count() request
//        return result.size() > 0 ? ((Number)result.iterator().next()).intValue() : 0;
    }
}
