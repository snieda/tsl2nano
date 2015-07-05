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

import org.simpleframework.xml.Element;

import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.bean.BeanUtil;
import de.tsl2.nano.bean.def.ArrayValue;
import de.tsl2.nano.bean.def.Attachment;
import de.tsl2.nano.bean.def.AttributeDefinition;
import de.tsl2.nano.bean.def.BeanCollector;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.IAttributeDefinition;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.cls.IAttribute;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.service.util.ServiceUtil;
import de.tsl2.nano.util.NumberUtil;

/**
 * Does a statistic by doing a group by on all columns. Set {@link #from} and {@link #toString()} on the constructor
 * {@link #Statistic(Class, Object, Object)} to do a filtering. set the attribute filter (see
 * {@link BeanDefinition#setAttributeFilter(String...)} to select the attributes to do a statistic on (creating a 'group
 * by' select).
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class Statistic<COLLECTIONTYPE extends Collection<T>, T> extends BeanCollector<COLLECTIONTYPE, T> {
    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    /** columnNames of the statistic table */
    List<String> columnNames;

    @Element(required = false)
    T from;
    @Element(required = false)
    T to;

    public Statistic() {
    }

    public Statistic(Class<T> beanType) {
        this(beanType, null, null);
    }

    /**
     * constructor
     * 
     * @param beanType
     * @param workingMode
     */
    @SuppressWarnings("unchecked")
    public Statistic(Class<T> beanType, T from, T to) {
        super();
        setMode(0);
        columnNames = new LinkedList<String>();
        this.from = from;
        this.to = to;
        collection = (COLLECTIONTYPE) create(beanType, columnNames, from, to);
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
        return new ArrayList<IAttribute>(attributeDefinitions.values());
    }

    private <T> Collection<T> create(Class<T> beanType, List<String> attributeNames, T from, T to) {
        attributeNames.add(ENV.translate("tsl2nano.name", true));
        attributeNames.add(ENV.translate("tsl2nano.count", true));

        BeanDefinition<T> def = BeanDefinition.getBeanDefinition(beanType);
        setName(ENV.translate("tsl2nano.statistic", true) + " (" + def.getName() + ")");
        String[] names = def.getAttributeNames();
        List<String> statColumns = new ArrayList<>(names.length);
        List<String> valueColumns = new ArrayList<>(names.length);

        searchStatus =
            ENV.translate("tsl2nano.summary", true) + ": " + ENV.translate("tsl2nano.from", true)
                + BeanUtil.toFormattedMap(from) + ENV.translate("tsl2nano.to", true) + BeanUtil.toFormattedMap(to);
        /*
         * check, which columns should be shown. if a column has more than 500 group by elements, its to big
         * evaluate the number columns
         */
        long maxcount = ENV.get("statistic.maxgroupcount", 500);
        String sum = ENV.translate("tsl2nano.sum", true) + "(";
        IAttributeDefinition<?> attrDef;
        for (int i = 0; i < names.length; i++) {
            attrDef = def.getAttribute(names[i]);
            if (attrDef.id() || attrDef.generatedValue() || attrDef.isMultiValue() || attrDef.unique()
                || attrDef.isVirtual() || Attachment.isData(attrDef)) {
                continue;
            } else if (NumberUtil.isNumber(attrDef.getType())) {
                valueColumns.add(names[i]);
                attributeNames.add(sum + names[i] + ")");
            } else if (groupByCount(def, names[i], from, to) <= maxcount) {
                statColumns.add(names[i]);
            }
        }

        /*
         * do all group by selects
         */
        Collection<T> collection = new ArrayList<>();
        for (String n : statColumns) {
            collection.addAll(createStatistics(def, n, valueColumns, from, to));
        }
        collection.addAll(createSummary(def, valueColumns, from, to));
        return collection;
    }

    private static <T> Collection<? extends T> createSummary(BeanDefinition<T> def,
            List<String> valueColumns,
            T from,
            T to) {
        String summary =
            ENV.translate("tsl2nano.all", true) + " " + ENV.translate("tsl2nano.elements", true);
        String qstr = "select ''{0}'', count(*) {2} from {1} t {3}";

        String strValueColumns = StringUtil.concatWrap(",sum({0})".toCharArray(), valueColumns.toArray());
        Collection<?> parameter = new LinkedList<>();
        String where = whereConstraints(from, to, parameter).toString();
        return BeanContainer.instance().getBeansByQuery(
            MessageFormat.format(qstr, summary, def, strValueColumns, where),
            false, parameter.toArray());
    }

    private static <T> Collection<? extends T> createStatistics(BeanDefinition<T> def,
            String column,
            List<String> valueColumns,
            T from,
            T to) {
        String qstr = "select ''{3}: '' || {0}, count({0}) {2} from {1} t {4} group by {0} order by 1";

        String strValueColumns = StringUtil.concatWrap(",sum({0})".toCharArray(), valueColumns.toArray());
        String columnname = ENV.translate(def.getAttribute(column).getId(), true);
        Collection<?> parameter = new LinkedList<>();
        String where = whereConstraints(from, to, parameter).toString();
        return BeanContainer.instance().getBeansByQuery(
            MessageFormat.format(qstr, column, def, strValueColumns, columnname, where),
            false, parameter.toArray());
    }

    private static <T> int groupByCount(BeanDefinition<T> def, String column, T from, T to) {
        Collection parameter = new LinkedList<>();
        String qstr =
            "select count(" + column + ") from " + def.getName() + " t " + whereConstraints(from, to, parameter)
                + " group by "
                + column;

        //JPA is not able to resolve the expression ..count(count(....)), so we have to use native sql

        Collection<Object> result = BeanContainer.instance().getBeansByQuery(qstr, false, parameter.toArray());
        //Woraround: returning direct size. count(count(.)) would have better performance
        return result.size();
        //JPA-QL sometimes seems to return empty collections on my count() request
//        return result.size() > 0 ? ((Number)result.iterator().next()).intValue() : 0;
    }

    private static <T> StringBuffer whereConstraints(T from, T to, Collection parameter) {
        return from == null || to == null ? new StringBuffer(" 1=1 ") : ServiceUtil.addBetweenConditions(
            new StringBuffer(), from, to, parameter, true);
    }
}
