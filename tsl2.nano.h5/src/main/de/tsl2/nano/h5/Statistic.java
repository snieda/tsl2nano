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
import java.util.Map;

import org.apache.commons.logging.Log;
import org.simpleframework.xml.Transient;

import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.bean.BeanUtil;
import de.tsl2.nano.bean.def.ArrayValue;
import de.tsl2.nano.bean.def.Attachment;
import de.tsl2.nano.bean.def.AttributeDefinition;
import de.tsl2.nano.bean.def.BeanCollector;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.IAttributeDefinition;
import de.tsl2.nano.collection.CollectionUtil;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.cls.IAttribute;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.ListWrapper;
import de.tsl2.nano.core.util.NumberUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.incubation.graph.SVGChart;
import de.tsl2.nano.service.util.ServiceUtil;

/**
 * Does a statistic by doing a group by on all columns. Set {@link #from} and {@link #toString()} on the constructor
 * {@link #Statistic(Class, Object, Object)} to do a filtering. set the attribute filter (see
 * {@link BeanDefinition#setAttributeFilter(String...)} to select the attributes to do a statistic on (creating a 'group
 * by' select).
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings("unchecked")
public class Statistic<COLLECTIONTYPE extends Collection<T>, T> extends BeanCollector<COLLECTIONTYPE, T> {
    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    private static final Log LOG = LogFactory.getLog(Statistic.class);
    @Transient
    Class<T> beanType;

    /** columnNames of the statistic table */
    @Transient
    //this Transient annotation marks the attribute as extension member
    ListWrapper<String> columnNames;

    @Transient
    ListWrapper<String> statColumns;

    @Transient
    //this Transient annotation marks the attribute as extension member
    T from;
    @Transient
    //this Transient annotation marks the attribute as extension member
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
    public Statistic(Class<T> beanType, T from, T to) {
        super();
        setMode(0);
        columnNames = new ListWrapper<String>();
        this.from = from;
        this.to = to;
        this.beanType = beanType;
//        collection = (COLLECTIONTYPE) create(beanType, columnNames.getList(), from, to);
        isStaticCollection = true;
    }

    @SuppressWarnings({ "rawtypes" })
    @Override
    public List<IAttribute> getAttributes(boolean readAndWriteAccess) {
        if (Util.isEmpty(attributeDefinitions)) {
            attributeDefinitions = new LinkedHashMap<String, IAttributeDefinition<?>>();
            List<String> names = columnNames.getList();
            IAttribute<?> attribute;
            int i = 0;
            for (String name : names) {
                attribute = new ArrayValue(name, i++, i > 1 ? Number.class : null, null);
                attributeDefinitions.put(name, new AttributeDefinition(attribute));
            }
        }
        return new ArrayList<IAttribute>(attributeDefinitions.values());
    }

    private Collection<T> create(Class<T> beanType, List<String> attributeNames, T from, T to) {
        if (attributeNames.size() == 0) {
            attributeNames.add(ENV.translate("tsl2nano.name", true));
            attributeNames.add(ENV.translate("tsl2nano.count", true));
        }

        BeanDefinition<T> def = BeanDefinition.getBeanDefinition(beanType);
        setName(ENV.translate("tsl2nano.statistic", true) + " (" + def.getName() + ")");

        if (from != null && to != null) {
            Map<String, Object> fromMap = BeanUtil.toFormattedMap(from);
            Map<String, Object> toMap = BeanUtil.toFormattedMap(to);
            CollectionUtil.removeEmptyEntries(fromMap);
            CollectionUtil.removeEmptyEntries(toMap);
            String strFrom = fromMap.size() > 0 ? fromMap.toString() : "(" + ENV.translate("tsl2nano.all", false) + ")";
            String strTo = toMap.size() > 0 ? toMap.toString() : "(" + ENV.translate("tsl2nano.all", false) + ")";
            searchStatus =
                "<div>" +
                    ENV.translate("tsl2nano.summary", true) + ": " + ENV.translate("tsl2nano.range.from", true) +
                    strFrom + " - " + ENV.translate("tsl2nano.range.to", true) + strTo
                    + "</div>";
        }

        String[] names = def.getAttributeNames();
        List<String> valueColumns = new ArrayList<>(names.length);
        if (Util.isEmpty(statColumns)) {
            statColumns = new ListWrapper<String>(names.length);

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
        }
        
        /*
         * do all group by selects
         */
        Collection<T> collection = new ArrayList<>();
        for (String n : statColumns) {
            collection.addAll(createStatistics(def, n, valueColumns, from, to));
        }
        /*
         * create an svg chart file without summary
         */
        searchStatus += "<span>" + createGraph(getName(), columnNames.getList(), (Collection<Object[]>) collection) + "</span>";

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
        Collection<?> result = (Collection<? extends T>) getBeansByQuery(
            MessageFormat.format(qstr, summary, def, strValueColumns, where),
            parameter);
        return (Collection<? extends T>) (result != null ? result : new ArrayList<>());
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
        Collection<?> result = getBeansByQuery(
            MessageFormat.format(qstr, column, def, strValueColumns, columnname, where),
            parameter);
        return (Collection<? extends T>) (result != null ? result : new ArrayList<>());
    }

    @SuppressWarnings("rawtypes")
    private static <T> int groupByCount(BeanDefinition<T> def, String column, T from, T to) {
        Collection parameter = new LinkedList<>();
        String qstr =
            "select count(" + column + ") from " + def.getName() + " t " + whereConstraints(from, to, parameter)
                + " group by "
                + column;

        //JPA is not able to resolve the expression ..count(count(....)), so we have to use native sql

        Collection<Object> result = getBeansByQuery(qstr, parameter);
        //Woraround: returning direct size. count(count(.)) would have better performance
        return result != null ? result.size() : 0;
        //JPA-QL sometimes seems to return empty collections on my count() request
//        return result.size() > 0 ? ((Number)result.iterator().next()).intValue() : 0;
    }

    /**
     * getBeansByQuery
     * 
     * @param qstr
     * @param parameter
     * @return
     */
    protected static Collection<Object> getBeansByQuery(String qstr, Collection<?> parameter) {
        try {
            return BeanContainer.instance().getBeansByQuery(qstr, false, parameter.toArray());
        } catch (Exception e) {
            //ok, no group-by-count informations available, yet
        }
        return null;
    }

    @SuppressWarnings("rawtypes")
    private static <T> StringBuffer whereConstraints(T from, T to, Collection parameter) {
        return from == null || to == null ? new StringBuffer(" where 1=1 ") : ServiceUtil.addBetweenConditions(
            new StringBuffer(), from, to, parameter, true);
    }

    public void onActivation() {
        super.onActivation();
        collection = (COLLECTIONTYPE) create(beanType, columnNames.getList(), from, to);
    }

//    @Override
//    @Commit
//    protected void initDeserialization() {
//        beanFinder = new BeanFinder() {
//            @Override
//            public Collection superGetData(Object fromFilter, Object toFilter) {
//                Map<String, Object> context = new HashMap<String, Object>();
//                if (fromFilter != null && toFilter != null) {
//                    String[] names = query.getParameter().keySet().toArray(new String[0]);
//                    context.putAll(BeanUtil.toValueMap(fromFilter, "from", false, names));
//                    context.putAll(BeanUtil.toValueMap(toFilter, "to", false, names));
//                }
//                return query.run(context);
//            }
//        };
//        init(null, beanFinder, 0, null);
//        isStaticCollection = false;
//    }

    /**
     * creates a simple xy-chart and exports it to an svg-file
     * 
     * @param data
     */
    @SuppressWarnings({ "rawtypes" })
    static String createGraph(String name, List<String> columnNames, Collection<Object[]> data) {
        if (data.size() == 0 || !data.iterator().next().getClass().isArray() /* the compiler checks for array, but on runtime it may be a simple object!*/)
            return "";
        int columnCount = data.iterator().next().length;
        List<Object> x = new ArrayList<Object>(data.size());
        //workaround: on adding a list item, we can't cast to <? extends Number>
        List yx[] = new ArrayList[columnCount - 1];
        for (int i = 0; i < yx.length; i++) {
            yx[i] = new ArrayList<>(columnCount);
        }
        for (Object[] a : data) {
            if (a[0] == null)
                a[0] = "---";//avoid nullpointer - perhaps the value for null makes sense...
            x.add(a[0]);
            for (int i = 1; i < a.length; i++) {
                if (!(a[i] instanceof Number)) {
                    LOG.debug("no graph created while y-data is non-number-values");
                    return "";
                }
                yx[i - 1].add(a[i]);
            }
        }
        int width = ENV.get("statistic.graph.width", 1920);
        int height = ENV.get("statistic.graph.height", 1080);
        String svgFileName =
            SVGChart.createGraph(SVGChart.Type.BAR, name, "", "", width, height, false, x,
                SVGChart.series(columnNames, yx))
                + ".svg";
        String svgContent = new String(FileUtil.getFileBytes(svgFileName, null));
        //workaround: remove fix-size in mm
        svgContent = svgContent.replaceAll("\\w+[=]\"\\d+mm\"", "");
        return svgContent.substring(svgContent.indexOf("<svg"));
    }
}
