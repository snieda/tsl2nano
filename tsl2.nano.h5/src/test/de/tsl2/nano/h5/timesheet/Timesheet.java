/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 14.11.2015
 * 
 * Copyright: (c) Thomas Schneider 2015, all rights reserved
 */
package de.tsl2.nano.h5.timesheet;

import static de.tsl2.nano.bean.def.IPresentable.UNDEFINED;
import static de.tsl2.nano.h5.Html5Presentation.L_GRIDWIDTH;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_BORDER;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_SIZE;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_SPANCOL;
import static de.tsl2.nano.test.TypeBean.ATTR_BINARY;
import static de.tsl2.nano.test.TypeBean.ATTR_DATE;
import static de.tsl2.nano.test.TypeBean.ATTR_IMMUTABLEINTEGER;
import static de.tsl2.nano.test.TypeBean.ATTR_OBJECT;
import static de.tsl2.nano.test.TypeBean.ATTR_STRING;
import static de.tsl2.nano.test.TypeBean.ATTR_TIME;
import static de.tsl2.nano.test.TypeBean.ATTR_TIMESTAMP;
import static junit.framework.Assert.assertTrue;
import static org.anonymous.project.presenter.ChargeConst.ATTR_CHARGEITEM;
import static org.anonymous.project.presenter.ChargeConst.ATTR_COMMENT;
import static org.anonymous.project.presenter.ChargeConst.ATTR_FROMDATE;
import static org.anonymous.project.presenter.ChargeConst.ATTR_FROMTIME;
import static org.anonymous.project.presenter.ChargeConst.ATTR_PARTY;
import static org.anonymous.project.presenter.ChargeConst.ATTR_PAUSE;
import static org.anonymous.project.presenter.ChargeConst.ATTR_TOTIME;
import static org.anonymous.project.presenter.ChargeConst.ATTR_VALUE;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import my.app.Times;

import org.anonymous.project.Account;
import org.anonymous.project.Address;
import org.anonymous.project.Area;
import org.anonymous.project.Category;
import org.anonymous.project.Charge;
import org.anonymous.project.Chargeitem;
import org.anonymous.project.Coordinate;
import org.anonymous.project.Digital;
import org.anonymous.project.Discharge;
import org.anonymous.project.Item;
import org.anonymous.project.Location;
import org.anonymous.project.Organisation;
import org.anonymous.project.Party;
import org.anonymous.project.Property;
import org.anonymous.project.Type;
import org.anonymous.project.presenter.AccountConst;
import org.anonymous.project.presenter.AddressConst;
import org.anonymous.project.presenter.AreaConst;
import org.anonymous.project.presenter.CategoryConst;
import org.anonymous.project.presenter.ChargeConst;
import org.anonymous.project.presenter.ChargeitemConst;
import org.anonymous.project.presenter.CoordinateConst;
import org.anonymous.project.presenter.DigitalConst;
import org.anonymous.project.presenter.DischargeConst;
import org.anonymous.project.presenter.ItemConst;
import org.anonymous.project.presenter.LocationConst;
import org.anonymous.project.presenter.OrganisationConst;
import org.anonymous.project.presenter.PartyConst;
import org.anonymous.project.presenter.PropertyConst;
import org.anonymous.project.presenter.TypeConst;

import de.tsl2.nano.bean.IBeanContainer;
import de.tsl2.nano.bean.def.Attachment;
import de.tsl2.nano.bean.def.AttributeDefinition;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.BeanCollector;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.Constraint;
import de.tsl2.nano.bean.def.IAttributeDefinition;
import de.tsl2.nano.bean.def.IBeanCollector;
import de.tsl2.nano.bean.def.IPageBuilder;
import de.tsl2.nano.bean.def.IPresentable;
import de.tsl2.nano.bean.def.IPresentableColumn;
import de.tsl2.nano.bean.def.PathExpression;
import de.tsl2.nano.bean.def.SecureAction;
import de.tsl2.nano.bean.def.ValueColumn;
import de.tsl2.nano.collection.TableList;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.util.DateUtil;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.execution.ScriptUtil;
import de.tsl2.nano.h5.Controller;
import de.tsl2.nano.h5.Html5Presentation;
import de.tsl2.nano.h5.QueryResult;
import de.tsl2.nano.h5.RuleCover;
import de.tsl2.nano.h5.SpecifiedAction;
import de.tsl2.nano.h5.Statistic;
import de.tsl2.nano.h5.expression.Query;
import de.tsl2.nano.h5.expression.QueryPool;
import de.tsl2.nano.h5.expression.RuleExpression;
import de.tsl2.nano.h5.expression.SQLExpression;
import de.tsl2.nano.h5.navigation.BeanAct;
import de.tsl2.nano.h5.navigation.Parameter;
import de.tsl2.nano.h5.navigation.Workflow;
import de.tsl2.nano.h5.test.NanoH5App;
import de.tsl2.nano.h5.websocket.WSEvent;
import de.tsl2.nano.h5.websocket.WebSocketRuleDependencyListener;
import de.tsl2.nano.incubation.specification.ParType;
import de.tsl2.nano.incubation.specification.actions.Action;
import de.tsl2.nano.incubation.specification.actions.ActionPool;
import de.tsl2.nano.incubation.specification.rules.Rule;
import de.tsl2.nano.incubation.specification.rules.RuleDecisionTable;
import de.tsl2.nano.incubation.specification.rules.RulePool;
import de.tsl2.nano.incubation.specification.rules.RuleScript;

/**
 * Creates a timesheet configuration on NanoH5 and anyway database
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class Timesheet extends NanoH5App {
    private static final String STAT_TYPES = "Types";
    private static final String STAT_PROJECTS = "Projects";
    private static final String STAT_TIMESHEET_STATISTICS = "Timesheet-Statistics";
    String redColorStyle = "color: red;";
    String greenColorStyle = "color: green;";

    /**
     * constructor
     * 
     * @throws IOException
     */
    protected Timesheet() throws IOException {
        super();
    }

    public Timesheet(String serviceURL, IPageBuilder<?, String> builder) throws IOException {
        super(serviceURL, builder);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    protected BeanDefinition<?> createBeanCollectors(List<Class> beanClasses) {
        //this call will initialize the complete entity structure
        BeanDefinition<Charge> charge = BeanDefinition.getBeanDefinition(Charge.class);

        /*
         * create some virtual attributes
         */
        final String ATTR_WEEKDAY = "weekday";
        LinkedHashMap<String, ParType> pt = new LinkedHashMap<String, ParType>();
        pt.put("charge", new ParType(Charge.class));
        pt.put("formatter", new ParType(new SimpleDateFormat("EE")));
        RuleScript<String> script =
            new RuleScript<String>(ATTR_WEEKDAY,
                "charge.getFromdate() != null ? formatter.format(charge.getFromdate()) : \"\";", pt);
        ENV.get(RulePool.class).add(script);
        charge.addAttribute(ATTR_WEEKDAY, new RuleExpression<>(Charge.class, RuleScript.PREFIX + ATTR_WEEKDAY), null,
            null);

        /*
         * define all beans
         */
        define(Type.class, icon("equipment"), TypeConst.ATTR_NAME);
        define(Category.class, null, CategoryConst.ATTR_NAME);
        define(Account.class, icon("euro"), AccountConst.ATTR_NAME);
        define(Property.class, icon("table"), PropertyConst.ATTR_AKEY);
        define(Organisation.class, icon("people"), OrganisationConst.ATTR_NAME);
        define(Party.class, icon("male"), PartyConst.ATTR_SHORTNAME);
        define(Address.class, icon("home"), ve(AddressConst.ATTR_CITY) + ", " + ve(AddressConst.ATTR_STREET));
        define(Location.class, icon("yellow_pin"), LocationConst.ATTR_NAME);
        define(Digital.class, icon("e-mail"), DigitalConst.ATTR_NAME);
        define(Coordinate.class, icon("blue_pin"), ve(CoordinateConst.ATTR_X) + "-" + ve(CoordinateConst.ATTR_Y) + "-"
            + ve(CoordinateConst.ATTR_X));
        define(Area.class, icon("boss"), AreaConst.ATTR_NAME);
        define(Item.class, null, ItemConst.ATTR_NAME, ItemConst.ATTR_ID, ItemConst.ATTR_NAME, ItemConst.ATTR_ORGANISATION,
            ItemConst.ATTR_CLASSIFICATION, ItemConst.ATTR_TYPE, ItemConst.ATTR_START, ItemConst.ATTR_END,
            ItemConst.ATTR_VALUE, ItemConst.ATTR_DESCRIPTION, ItemConst.ATTR_CHARGEITEMS, ItemConst.ATTR_PROPERTIES);
        define(Chargeitem.class, icon("buy"), ChargeitemConst.ATTR_ITEM);
        define(Discharge.class, icon("accounting"), ve(DischargeConst.ATTR_CHARGE) + " (" + ve(DischargeConst.ATTR_DATE) + ": "
            + ve(DischargeConst.ATTR_VALUE));

        /*
         * configure the main type: Charge (Zeiterfassung)
         */
        charge =
            define(Charge.class, icon("clock"), ve(ChargeConst.ATTR_CHARGEITEM) + " (" + ve(ChargeConst.ATTR_FROMDATE) + ": "
                + ve(ChargeConst.ATTR_VALUE) + ")"
                , ATTR_FROMDATE, ATTR_WEEKDAY, ATTR_FROMTIME, ATTR_TOTIME, ATTR_PAUSE, ATTR_PARTY, ATTR_CHARGEITEM,
                ATTR_VALUE, ATTR_COMMENT);
        IPresentableColumn column = charge.getAttribute(ATTR_VALUE).getColumnDefinition();
        if (column instanceof ValueColumn)
            ((ValueColumn) column).setStandardSummary(true);
        IAttributeDefinition attrWeekday = charge.getAttribute(ATTR_WEEKDAY);
        attrWeekday.getPresentation().setVisible(false);
        ((AttributeDefinition) attrWeekday).setColumnDefinition(UNDEFINED, UNDEFINED, false, UNDEFINED);

        ((ValueColumn) charge.getAttribute(ATTR_FROMDATE).getColumnDefinition()).setFormat(new SimpleDateFormat(
            "dd.MM EE"));

        /*
         * add dependency listeners on rules
         */
        RuleScript<BigDecimal> calcTime = new RuleScript<BigDecimal>("calcTime",
            "var from = fromtime != null ? fromtime.getTime() : 0;" +
                "var to = totime != null ? totime.getTime() : 0;" +
                "var p = pause != null ? pause.getTime() : 0;" +
                "Math.round(((to - from) - p) / (3600 * 10)) / 100;", null);
        /*
         * add some specifications (=tests) to be checked on loading a rule
         */
        Time t0800 = DateUtil.getTime(8, 0);
        Time t1700 = DateUtil.getTime(17, 0);
        Time t0000 = DateUtil.getTime(0, 0);
        Time t0030 = DateUtil.getTime(0, 30);
        calcTime.addSpecification("notime", "check for zero-times", 0d,
            MapUtil.asMap("fromtime", t0800, "totime", t0800, "pause", t0000));
        calcTime.addSpecification("standard", "standard work day", 8.5d,
            MapUtil.asMap("fromtime", t0800, "totime", t1700, "pause", t0030));

        charge.getAttribute(ATTR_VALUE).getConstraint().setScale(2);
        charge.getAttribute(ATTR_VALUE).getConstraint().setPrecision(4);

        // create dependency listeners for websocket and standard bean-changing
        ENV.get(RulePool.class).add(calcTime);
        Html5Presentation helper = charge.getPresentationHelper();
        helper.addRuleListener(ATTR_VALUE, RuleScript.PREFIX + calcTime.getName(), ATTR_FROMTIME, ATTR_TOTIME, ATTR_PAUSE);

        /*
         * add attribute presentation rules
         */
        RuleScript<String> presValueColor =
            new RuleScript<String>(
                "presValueColor", "var map = new java.util.HashMap(); map.put('style', value > 10 ? '" + redColorStyle
                    + "' : '" + greenColorStyle + "'); map;", null);
        ENV.get(RulePool.class).add(presValueColor);
        RuleCover.cover(Charge.class, ATTR_VALUE, "presentable.layoutConstraints", "%" + presValueColor.getName());
        RuleCover.cover(Charge.class, ATTR_VALUE, "columnDefinition.presentable.layoutConstraints", "%" + presValueColor.getName());
        //one rulecover on columndefs..presentable.layoutconstraints
//        ruleCover = new RuleCover("%" + presValueColor.getName(), MapUtil.asMap(
//                "columnDefinition.presentable.layoutConstraints", presValueColor.getName()));
//        ruleCover.connect(charge.getAttribute(ATTR_VALUE));

        //create a decision-table
        TableList tl = new TableList<>(2);
        tl.add("matrix", "<1>", "<2>", "<3>", "<4>", "<5>", "<6>", "<7>");
        tl.add("weekday", "Mo", "Di", "Mi", "Do", "Fr","Sa", "So");
        tl.add("result", greenColorStyle, greenColorStyle, greenColorStyle, greenColorStyle, greenColorStyle, redColorStyle, redColorStyle);
        String ruleDir = ENV.get(RulePool.class).getDirectory();
        FileUtil.save(ruleDir + "weekcolor.csv", tl.dump());
        
        RuleDecisionTable dtRule = RuleDecisionTable.fromCSV(ruleDir + "weekcolor.csv");
        ENV.get(RulePool.class).add(dtRule);
        RuleCover.cover(Charge.class, ATTR_FROMDATE, "presentable.layoutConstraints", "&" + dtRule.getName());
        RuleCover.cover(Charge.class, ATTR_FROMDATE, "columnDefinition.presentable.layoutConstraints", "&" + dtRule.getName());
        
        charge.saveDefinition();

        //TODO: Statistics executes queries immediately to evaluate group-by column names
        new Statistic<>(Charge.class).saveVirtualDefinition("statistics " + Charge.class.getSimpleName());

        /*
         * statistic queries
         */
        String stat = "\n-- get a statistic table from timesheet entries\n" +
              "-- user and time-period should be given...\n" +
              "select Month, sum(Workdays) as Workdays, sum(Hours) as Hours, sum(Ill) as Ill, sum(Holiday) as Holiday from (\n" +
              "select year(c.FROMDATE) || ' ' || monthname(c.FROMDATE) as Month, count(month(c.FROMDATE)) as Workdays, sum(value) as Hours, 0 as Ill, 0 as Holiday from Charge c\n" +
              "group by Month\n" +
              "union -- Illness\n" +
              "select year(c.FROMDATE) || ' ' || monthname(c.FROMDATE) as Month, 0 as Workdays, 0 as Hours, sum(value) as Ill, 0 as Holiday from Charge c join ChargeItem ci on c.CHARGEITEM = ci.ID join Item i on ci.ITEM = i.ID join Type t on i.TYPE = t.ID\n" +
              "where t.NAME = 'Krank'\n" +
              "group by Month\n" +
              "union -- Holidays\n" +
              "select year(c.FROMDATE) || ' ' || monthname(c.FROMDATE) as Month, 0 as Workdays, 0 as Hours, 0 as Ill, sum(value) as Holiday from Charge c join ChargeItem ci on c.CHARGEITEM = ci.ID join Item i on ci.ITEM = i.ID join Type t on i.TYPE = t.ID\n" +
              "where t.NAME = 'Urlaub'\n" +
              "group by Month\n" +
              ")\n" +
              "group by Month\n";
        Query<Object> query = new Query<>(STAT_TIMESHEET_STATISTICS, stat, true, null);
        QueryPool queryPool = ENV.get(QueryPool.class);
        queryPool.add(query);
        new QueryResult<>(query.getName()).saveVirtualDefinition(query.getName());

        stat = "\n-- get a statistic table over projects\n" +
                "select org.NAME || ' ' || i.NAME as Project, sum(value) as Hours from Charge c join CHARGEITEM ci on c.CHARGEITEM = ci.ID join ITEM i on ci.ITEM = i.ID join ORGANISATION org on i.ORGA = org.ID\n" + 
                "group by Project\n";
        queryPool.add(query = new Query<>(STAT_PROJECTS, stat, true, null));
        new QueryResult<>(query.getName()).saveVirtualDefinition(query.getName());

        stat = "\n-- get a statistic table over types\n" +
                "select t.NAME as Type, sum(value) as Hours from Charge c join CHARGEITEM ci on c.CHARGEITEM = ci.ID join ITEM i on ci.ITEM = i.ID join TYPE t on i.TYPE = t.ID\n" +
                "group by t.NAME\n";
        queryPool.add(query = new Query<>(STAT_TYPES, stat, true, null));
        new QueryResult<>(query.getName()).saveVirtualDefinition(query.getName());

        /*
         * Sample Workflow with three activities
         */
        LinkedList<BeanAct> acts = new LinkedList<BeanAct>();
        Parameter p = new Parameter();
        p.put("project", true);
        p.put("prjname", "test");
        acts.add(new BeanAct("timesByProject",
            "project&true",
            "select t from Times t where t.project.id = :prjname",
            p,
            "prjname"));
        p = new Parameter();
        p.put("prjname", "test");
        acts.add(new BeanAct("organisation",
            "!organisation.activated",
            "select p from Organisation p where ? is null",
            p,
            "prjname"));
        p = new Parameter();
        p.put("organisation", "test");
        acts.add(new BeanAct("person",
            "organisation.activated & (!person.activated)",
            "select p from Person p where p.organisation = ?",
            p,
            "organisation"));
        Workflow workflow = new Workflow("test.workflow", acts);
        ENV.persist(workflow);

        /*
         * use a rule with sub-rule
         */
        LinkedHashMap<String, ParType> par = new LinkedHashMap<String, ParType>();
        par.put("A", ParType.BOOLEAN);
        par.put("x1", ParType.NUMBER);
        par.put("x2", ParType.NUMBER);
        Rule<BigDecimal> testRule = new Rule<BigDecimal>("test", "A ? (x1 + 1) : (x2 * 2)", par);
        testRule.addConstraint("x1", new Constraint<BigDecimal>(BigDecimal.class, BigDecimal.ZERO, BigDecimal.TEN));
        testRule.addConstraint(Rule.KEY_RESULT, new Constraint<BigDecimal>(BigDecimal.class, BigDecimal.ZERO,
            BigDecimal.TEN));
        testRule.addSpecification("notA-1-2", null, new BigDecimal(4),
            MapUtil.asMap("x1", new BigDecimal(1), "x2", new BigDecimal(2)));
        testRule.addSpecification("A-2-1", null, new BigDecimal(2),
            MapUtil.asMap("x1", new BigDecimal(2), "x2", new BigDecimal(1)));
        ENV.get(RulePool.class).add(testRule);

        //another rule to test sub-rule-imports
        ENV.get(RulePool.class).add(new Rule<BigDecimal>("test-import", "A ? 1 + §test : (x2 * 3)", par));

        BigDecimal result =
            (BigDecimal) ENV.get(RulePool.class).get("test-import")
                .run(MapUtil.asMap("A", true, "x1", new BigDecimal(1), "x2", new BigDecimal(2)));

        LOG.info("my test-import rule result:" + result);

        /*
         * define an action
         */
        Method antCaller = null;
        try {
            antCaller = ScriptUtil.class.getMethod("ant", new Class[] { String.class, String.class, Properties.class });
        } catch (Exception e) {
            ManagedException.forward(e);
        }
        Action<Object> a = new Action<>(antCaller);
        a.addConstraint("arg1", new Constraint<String>(ENV.getConfigPath() + "antscripts.xml"));
        a.addConstraint("arg2", new Constraint<String>("help"));
        ENV.get(ActionPool.class).add(a);

        /*
         * define a Controller as Collector of Actions of a Bean
         */
        final BeanDefinition timeActionBean = new BeanDefinition(Times.class);
        timeActionBean.setName("time-actions");
        BeanDefinition.define(timeActionBean);
        final Controller controller = new Controller(timeActionBean, IBeanCollector.MODE_SEARCHABLE);
        timeActionBean.getActions().clear();
        timeActionBean.addAction(new SecureAction("times.actions.one.hour.add", "+1") {
            @Override
            public Object action() throws Exception {
//                this.shortDescription = 
                return controller;
            }
        });
        timeActionBean.addAction(new SecureAction("times.actions.one.hour.subtract", "-1") {
            @Override
            public Object action() throws Exception {
                return controller;
            }
        });
        timeActionBean.saveDefinition();
        controller.saveVirtualDefinition(timeActionBean.getName() + "-controller");

        /*
         * define own beans to present your entities another way
         */
        Collection<Times> times = ENV.get(IBeanContainer.class).getBeans(Times.class, UNDEFINED, UNDEFINED);

        BeanCollector<Collection<Times>, Times> beanCollector =
            new BeanCollector<Collection<Times>, Times>(Times.class, times, BeanCollector.MODE_ALL, null);

        AttributeDefinition space1 = beanCollector.getPresentationHelper().addSpaceValue();
        beanCollector.addAttribute("path-test", new PathExpression<>(Times.class, "relation.string"), null, null);
        beanCollector.addAttribute("rule-test", new RuleExpression<>(Times.class, "§test-import"), null, null);
        beanCollector
            .addAttribute(
                "sql-test",
                new SQLExpression<>(
                    Times.class,
                    "?" + query.getName(), Object[].class),
                null, null);
        beanCollector.addAttribute("virtual-test", "I'm virtual", null, null, null);
        beanCollector.addAttribute(ATTR_BINARY, new Attachment("picture", ENV.getConfigPath()
            + "/icons/attach.png"), null, null);
        beanCollector.setAttributeFilter("path-test", ATTR_TIMESTAMP, ATTR_TIME, ATTR_DATE, space1.getName(),
            ATTR_STRING,
            ATTR_OBJECT, ATTR_IMMUTABLEINTEGER);
        //more fields on one line (one field has grid-width 3)
        beanCollector.getPresentable().setLayout((Serializable) MapUtil.asMap(L_GRIDWIDTH, 12));
        //let the field 'comment' grow to full width
        beanCollector.getAttribute(ATTR_OBJECT).getPresentation()
            .setLayoutConstraints((Serializable) MapUtil.asMap(ATTR_SPANCOL, 11, ATTR_BORDER, 1, ATTR_SIZE, 150));

        /*
         * add a specified action
         */
        beanCollector.addAction(new SpecifiedAction<>("ant", null));

        /*
         * save it as beandef
         */
        BeanDefinition.define(beanCollector);
        beanCollector.saveDefinition();

        final BeanDefinition b =
            BeanDefinition.getBeanDefinition(Times.class);
        /*
         * create a dependency listener with a rule
         */
        ((AttributeDefinition) b.getAttribute(ATTR_STRING)).changeHandler().addListener(
            new WebSocketRuleDependencyListener((AttributeDefinition) b.getAttribute(ATTR_IMMUTABLEINTEGER),
                ATTR_IMMUTABLEINTEGER,
                "rule-string-integer"), WSEvent.class);

        /*
         * use file attachments
         */
        ((AttributeDefinition) b.getAttribute(ATTR_OBJECT)).getPresentation().setType(IPresentable.TYPE_ATTACHMENT);

        b.saveDefinition();
        /*
         * define your own navigation stack
         */
        return beanCollector;
    }

    /**
     * icon
     * @param name 
     * @return
     */
    StringBuilder icon(String name) {
        return new StringBuilder("icons/" + name + ".png");
    }

    /**
     * check assertions
     */
    @Override
    public void stop() {
        Charge c = new Charge();
        Bean bean = Bean.getBean(c);
        Date sunday = DateUtil.getDate("03.01.2016");
        c.setFromdate(sunday);
        c.setFromtime(DateUtil.getTime(8, 30));
        bean.getAttribute(ATTR_TOTIME).setValue(DateUtil.getTime(20, 0));
        assertTrue(new BigDecimal(11.5).equals(c.getValue()));

        Map lc = bean.getAttribute(ATTR_VALUE).getPresentation().getLayoutConstraints();
        assertTrue(redColorStyle.equals(lc.get("style")));

        String style = bean.getAttribute(ATTR_FROMDATE).getPresentation().getLayoutConstraints();
        assertTrue(redColorStyle.equals(style));

        //test it on value-column
        style = bean.getAttribute(ATTR_FROMDATE).getColumnDefinition().getPresentable().getLayoutConstraints();
        assertTrue(redColorStyle.equals(style));

        //test it on value-column in beancollector
        BeanCollector<Collection<Charge>,Charge> collector = BeanCollector.getBeanCollector(Arrays.asList(c), 0);
        collector.nextRow();
        style = collector.getAttribute(ATTR_FROMDATE).getColumnDefinition().getPresentable().getLayoutConstraints();
        assertTrue(redColorStyle.equals(style));

        //test the queries
//        QueryPool qpool = ENV.get(QueryPool.class);
//        assertTrue(qpool.get(STAT_TIMESHEET_STATISTICS).getColumnNames().equals(Arrays.asList("")));
//        assertTrue(qpool.get(STAT_PROJECTS).getColumnNames().equals(Arrays.asList("")));
//        assertTrue(qpool.get(STAT_TYPES).getColumnNames().equals(Arrays.asList("")));
        super.stop();
    }
}
