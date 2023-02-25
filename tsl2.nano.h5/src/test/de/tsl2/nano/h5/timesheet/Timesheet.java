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

import static de.tsl2.nano.autotest.TypeBean.ATTR_BINARY;
import static de.tsl2.nano.autotest.TypeBean.ATTR_DATE;
import static de.tsl2.nano.autotest.TypeBean.ATTR_IMMUTABLEINTEGER;
import static de.tsl2.nano.autotest.TypeBean.ATTR_OBJECT;
import static de.tsl2.nano.autotest.TypeBean.ATTR_STRING;
import static de.tsl2.nano.autotest.TypeBean.ATTR_TIME;
import static de.tsl2.nano.autotest.TypeBean.ATTR_TIMESTAMP;
import static de.tsl2.nano.bean.def.IPresentable.UNDEFINED;
import static de.tsl2.nano.h5.Html5Presentation.L_GRIDWIDTH;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_BORDER;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_SIZE;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_SPANCOL;
import static de.tsl2.nano.h5.NanoH5Util.define;
import static de.tsl2.nano.h5.NanoH5Util.defineAction;
import static de.tsl2.nano.h5.NanoH5Util.icon;
import static de.tsl2.nano.specification.SpecificationExchange.*;
import static de.tsl2.nano.h5.NanoH5Util.*;
import static org.anonymous.project.presenter.ChargeConst.ATTR_CHARGEITEM;
import static org.anonymous.project.presenter.ChargeConst.ATTR_COMMENT;
import static org.anonymous.project.presenter.ChargeConst.ATTR_FROMDATE;
import static org.anonymous.project.presenter.ChargeConst.ATTR_FROMTIME;
import static org.anonymous.project.presenter.ChargeConst.ATTR_PARTY;
import static org.anonymous.project.presenter.ChargeConst.ATTR_PAUSE;
import static org.anonymous.project.presenter.ChargeConst.ATTR_TODATE;
import static org.anonymous.project.presenter.ChargeConst.ATTR_TOTIME;
import static org.anonymous.project.presenter.ChargeConst.ATTR_VALUE;
import static org.anonymous.project.presenter.ChargeitemConst.ATTR_CHARGE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;

import org.anonymous.project.Account;
import org.anonymous.project.Address;
import org.anonymous.project.Area;
import org.anonymous.project.Category;
import org.anonymous.project.Charge;
import org.anonymous.project.Chargeitem;
import org.anonymous.project.Chargestatus;
import org.anonymous.project.Classification;
import org.anonymous.project.Coordinate;
import org.anonymous.project.Digital;
import org.anonymous.project.Discharge;
import org.anonymous.project.Item;
import org.anonymous.project.Location;
import org.anonymous.project.Mission;
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
import org.anonymous.project.presenter.ClassificationConst;
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
import de.tsl2.nano.bean.def.IPageBuilder;
import de.tsl2.nano.bean.def.IPresentable;
import de.tsl2.nano.bean.def.IPresentableColumn;
import de.tsl2.nano.bean.def.PathExpression;
import de.tsl2.nano.bean.def.SecureAction;
import de.tsl2.nano.bean.def.ValueColumn;
import de.tsl2.nano.collection.TableList;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.util.ConcurrentUtil;
import de.tsl2.nano.core.util.DateUtil;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.execution.ScriptUtil;
import de.tsl2.nano.h5.Html5Presentation;
import de.tsl2.nano.h5.NanoH5App;
import de.tsl2.nano.h5.RuleCover;
import de.tsl2.nano.h5.SpecifiedAction;
import de.tsl2.nano.h5.collector.CSheet;
import de.tsl2.nano.h5.collector.Compositor;
import de.tsl2.nano.h5.collector.Controller;
import de.tsl2.nano.h5.collector.Increaser;
import de.tsl2.nano.h5.collector.QueryResult;
import de.tsl2.nano.h5.collector.Statistic;
import de.tsl2.nano.h5.configuration.BeanConfigurator;
import de.tsl2.nano.h5.expression.Query;
import de.tsl2.nano.h5.expression.RuleExpression;
import de.tsl2.nano.h5.expression.SQLExpression;
import de.tsl2.nano.h5.navigation.BeanAct;
import de.tsl2.nano.h5.navigation.Parameter;
import de.tsl2.nano.h5.navigation.Workflow;
import de.tsl2.nano.h5.websocket.WSEvent;
import de.tsl2.nano.h5.websocket.WebSocketRuleDependencyListener;
import de.tsl2.nano.specification.ParType;
import de.tsl2.nano.specification.Pool;
import de.tsl2.nano.specification.actions.Action;
import de.tsl2.nano.specification.rules.RuleDecisionTable;
import de.tsl2.nano.specification.rules.RuleDependencyListener;
import de.tsl2.nano.specification.rules.RuleScript;
import de.tsl2.nano.util.PrintUtil;
import my.app.Times;

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
        ENV.get(Pool.class).add(script);
        addVirtualAttribute(charge, RuleScript.PREFIX + ATTR_WEEKDAY);

        /*
         * define all beans
         */
        define(Type.class, icon("equipment"), ve(TypeConst.ATTR_NAME));
        define(Category.class, icon("equipment"), ve(CategoryConst.ATTR_NAME));
        define(Account.class, icon("euro"), ve(AccountConst.ATTR_NAME));
        define(Property.class, icon("table"), ve(PropertyConst.ATTR_AKEY), PropertyConst.ATTR_AKEY, PropertyConst.ATTR_AVALUE, PropertyConst.ATTR_ORGANISATION, PropertyConst.ATTR_PARTY, PropertyConst.ATTR_ITEM);
        define(Organisation.class, icon("people"), ve(OrganisationConst.ATTR_NAME));
        define(Party.class, icon("male"), ve(PartyConst.ATTR_SHORTNAME));
        define(Address.class, icon("home"), ve(AddressConst.ATTR_CITY) + ", " + ve(AddressConst.ATTR_STREET));
        define(Location.class, icon("yellow_pin"), ve(LocationConst.ATTR_NAME));
        define(Digital.class, icon("e-mail"), ve(DigitalConst.ATTR_NAME));
        define(Coordinate.class, icon("blue_pin"), ve(CoordinateConst.ATTR_X) + "-" + ve(CoordinateConst.ATTR_Y) + "-"
            + ve(CoordinateConst.ATTR_X));
        define(Area.class, icon("boss"), ve(AreaConst.ATTR_NAME));
        define(Classification.class, icon("widget"), ve(ClassificationConst.ATTR_NAME));
        define(Item.class, icon("equipment"), ve(ItemConst.ATTR_NAME), ItemConst.ATTR_ID, ItemConst.ATTR_NAME, ItemConst.ATTR_ORGANISATION,
            ItemConst.ATTR_CLASSIFICATION, ItemConst.ATTR_TYPE, ItemConst.ATTR_START, ItemConst.ATTR_END,
            ItemConst.ATTR_VALUE, ItemConst.ATTR_ICON, ItemConst.ATTR_DESCRIPTION, ItemConst.ATTR_CHARGEITEMS, ItemConst.ATTR_PROPERTIES);
        define(Discharge.class, icon("accounting"), ve(DischargeConst.ATTR_CHARGE) + " (" + ve(DischargeConst.ATTR_DATE) + ": "
            + ve(DischargeConst.ATTR_VALUE));
        define(Chargestatus.class, icon("yellow_pin"), ve("name"));
        define(Mission.class, icon("yellow_pin"), ve("name"));

        BeanDefinition<Chargeitem> chargeItem = define(Chargeitem.class, icon("buy"), ve(ChargeitemConst.ATTR_ITEM));
        //this test does not use a real beancontainer reading jpa annotations
        chargeItem.getAttribute(ATTR_CHARGE).getConstraint().setNullable(false);
        chargeItem.saveDefinition();
        
        /*
         * configure the main type: Charge (Zeiterfassung)
         */
        charge =
            define(Charge.class, icon("clock"), ve(ChargeConst.ATTR_CHARGEITEM) + " (" + ve(ChargeConst.ATTR_FROMDATE) + ": "
                + ve(ChargeConst.ATTR_VALUE) + ")"
                , ATTR_FROMDATE, ATTR_WEEKDAY, ATTR_FROMTIME, ATTR_TODATE, ATTR_TOTIME, ATTR_PAUSE, ATTR_PARTY, ATTR_CHARGEITEM,
                ATTR_VALUE, ATTR_COMMENT, "chargestatus");
        //this test does not use a real beancontainer, but jpa annotations will be read
        charge.getAttribute(ATTR_FROMTIME).getPresentation().setType(IPresentable.TYPE_TIME);
        charge.getAttribute(ATTR_TOTIME).getPresentation().setType(IPresentable.TYPE_TIME);
        charge.getAttribute(ATTR_PAUSE).getPresentation().setType(IPresentable.TYPE_TIME);
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
        long dayTimeFraction = DateUtil.T_DAY;
        RuleScript<BigDecimal> calcTime = new RuleScript<BigDecimal>("calcTime",
            "var from = fromtime != null ? fromtime.getTime() " + "% " + dayTimeFraction + " : 0;" +
                "var to = totime != null ? totime.getTime() " + "% " + dayTimeFraction + " : 0;" +
                "var p = pause != null ? (pause.getTime() - new Date(pause.toGMTString()).getTimezoneOffset()*60000) " + "% " + dayTimeFraction + " : 0;" +
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
        calcTime.addSpecification("notime1", "check for zero-times", 0d,
            MapUtil.asMap("fromtime", t0800, "totime", t0800, "pause", null));
        calcTime.addSpecification("standard", "standard work day", 8.5d,
            MapUtil.asMap("fromtime", t0800, "totime", t1700, "pause", t0030));

        charge.getAttribute(ATTR_VALUE).getConstraint().setScale(2);
        charge.getAttribute(ATTR_VALUE).getConstraint().setPrecision(4);

        // create dependency listeners for websocket and NOT standard bean-changing
        ENV.get(Pool.class).add(calcTime);
        Html5Presentation helper = charge.getPresentationHelper();
        helper.addRuleListener(ATTR_VALUE, RuleScript.PREFIX + calcTime.getName(), 2, ATTR_FROMTIME, ATTR_TOTIME, ATTR_PAUSE);

        /*
         * add attribute presentation rules
         */
        RuleScript<String> presValueColor =
            new RuleScript<String>(
                "presValueColor", "var map = new java.util.HashMap(); map.put('style', (typeof value != 'undefined' ? value : 0) > 10 ? '" + redColorStyle
                    + "' : '" + greenColorStyle + "'); map;", null);
        ENV.get(Pool.class).add(presValueColor);
        RuleCover.cover(Charge.class, ATTR_VALUE, PATH_LAYOUTCONSTRAINTS, "%" + presValueColor.getName());
        RuleCover.cover(Charge.class, ATTR_VALUE, PATH_COLDEF_LAYOUTCONSTRAINTS, "%" + presValueColor.getName());
        //one rulecover on columndefs..presentable.layoutconstraints
//        ruleCover = new RuleCover("%" + presValueColor.getName(), MapUtil.asMap(
//                "columnDefinition.presentable.layoutConstraints", presValueColor.getName()));
//        ruleCover.connect(charge.getAttribute(ATTR_VALUE));

        //create a decision-table
        TableList tl = new TableList<>("weekcolor", 2);
        tl.add("matrix", "<1>", "<2>", "<3>", "<4>", "<5>", "<6>", "<7>");
        tl.add("weekday", "Mo.", "Di.", "Mi.", "Do.", "Fr.","Sa.", "So.");
        tl.add("result", greenColorStyle, greenColorStyle, greenColorStyle, greenColorStyle, greenColorStyle, redColorStyle, redColorStyle);
        String ruleDir = ENV.get(Pool.class).getDirectory(RuleDecisionTable.class);
        FileUtil.save(ruleDir + "weekcolor", tl.dump());
        tl.save(ruleDir);
        RuleDecisionTable dtRule = RuleDecisionTable.fromCSV(ruleDir + "weekcolor.csv");
        ENV.get(Pool.class).add(dtRule);
        RuleCover.cover(Charge.class, ATTR_FROMDATE, PATH_LAYOUTCONSTRAINTS, "&" + dtRule.getName());
        RuleCover.cover(Charge.class, ATTR_FROMDATE, PATH_COLDEF_LAYOUTCONSTRAINTS, "&" + dtRule.getName());
        
        //copy fromdate to todate
        charge.getAttribute(ATTR_TODATE).getPresentation().setVisible(false);
        RuleScript<String> id = new RuleScript<String>("id", "value", null);
        ENV.get(Pool.class).add(id);
        charge.getAttribute(ATTR_FROMDATE).changeHandler().addListener(new RuleDependencyListener<>(charge.getAttribute(ATTR_TODATE),ATTR_TODATE, "id"));

        charge.saveDefinition();

        //create a compositor
        //WORAROUND: switch-off real BeanContainer not having any bean-type from anyway.jar
        //don't use a real persistence.xml while no anyway.jar file exists!
        // BeanContainer.initEmtpyServiceActions();
        
        Bean<BeanConfigurator<Charge>> bconf = BeanConfigurator.create(Charge.class);
        bconf.getInstance().actionCreateCompositor(Item.class.getName(), "chargeitems", "chargeitem", "icon");
//        GenericLocalBeanContainer.initLocalContainer(Thread.currentThread().getContextClassLoader(), false);

        //TODO: Statistics executes queries immediately to evaluate group-by column names
        createStatistics(Charge.class, "icons/barchart.png");
        
        /*
         * statistic queries
         */
        String stat = "\n-- get a statistic table from timesheet entries\n" +
              "-- user and time-period should be given...\n" +
              "select Month, sum(Workdays) as Workdays, sum(Hours) as Hours, sum(Dayhours) as Dayhours, sum(Ill) as Ill, sum(Holiday) as Holiday from (\n" +
              "select year(c.FROMDATE) || '(' || lpad(month(c.FROMDATE), 2, '0') || ') ' || monthname(c.FROMDATE) as Month, count(distinct(c.FROMDATE)) as Workdays, sum(value) as Hours, sum(value) / count(distinct(c.FROMDATE)) as Dayhours, 0 as Ill, 0 as Holiday from Charge c\n" +
              "group by Month\n" +
              "union -- Illness\n" +
              "select year(c.FROMDATE) || '(' || lpad(month(c.FROMDATE), 2, '0') || ') ' || monthname(c.FROMDATE) as Month, 0 as Workdays, 0 as Hours, 0 as Dayhours, sum(value) as Ill, 0 as Holiday from Charge c join ChargeItem ci on c.CHARGEITEM = ci.ID join Item i on ci.ITEM = i.ID join Type t on i.TYPE = t.ID\n" +
              "where t.NAME = 'Krank'\n" +
              "group by Month\n" +
              "union -- Holidays\n" +
              "select year(c.FROMDATE) || '(' || lpad(month(c.FROMDATE), 2, '0') || ') ' || monthname(c.FROMDATE) as Month, 0 as Workdays, 0 as Hours, 0 as Dayhours, 0 as Ill, sum(value) as Holiday from Charge c join ChargeItem ci on c.CHARGEITEM = ci.ID join Item i on ci.ITEM = i.ID join Type t on i.TYPE = t.ID\n" +
              "where t.NAME = 'Urlaub'\n" +
              "group by Month\n" +
              ")\n" +
              "group by Month\n" +
              "order by Month\n";
        QueryResult.createQueryResult(STAT_TIMESHEET_STATISTICS, stat);

        stat = "\n-- get a statistic table over projects\n" +
                "select org.NAME || ' ' || i.NAME as Project, sum(c.value) as Hours from Charge c join CHARGEITEM ci on c.CHARGEITEM = ci.ID join ITEM i on ci.ITEM = i.ID join ORGANISATION org on i.ORGA = org.ID\n" + 
                "group by Project\n";
        QueryResult.createQueryResult(STAT_PROJECTS, stat);

        stat = "\n-- get a statistic table over types\n" +
                "select t.NAME as Type, sum(c.value) as Hours from Charge c join CHARGEITEM ci on c.CHARGEITEM = ci.ID join ITEM i on ci.ITEM = i.ID join TYPE t on i.TYPE = t.ID\n" +
                "group by t.NAME\n";
        QueryResult queryResult = QueryResult.createQueryResult(STAT_TYPES, stat);

        /*
         * Sample Workflow with three activities
         */
        LinkedList<BeanAct> acts = new LinkedList<BeanAct>();
        Parameter p = new Parameter();
        p.put("project", true);
        p.put("type", "Urlaub");
        acts.add(new BeanAct("holidays",
            "project&true",
            "select t from Charge t where t.chargeitem.item.type.name = :type",
            p,
            "type"));
        p = new Parameter();
        p.put("prjname", "timesheet");
        acts.add(new BeanAct("organisation",
            "!organisation.activated",
            "select p from Organisation p where ? is null",
            p,
            "prjname"));
        p = new Parameter();
        p.put("organisation", "timesheet-system");
        acts.add(new BeanAct("person",
            "organisation.activated & (!person.activated)",
            "select p from Party p where p.organisation.name = ?",
            p,
            "organisation"));
        Workflow workflow = new Workflow("test.workflow", acts);
        ENV.persist(workflow);

        /*
         * define an action
         */
        defineAction(ScriptUtil.class, "ant", new Class[] { String.class, String.class, Properties.class }, 
        		new Constraint<String>(ENV.getConfigPathRel() + "antscripts.xml"),
        		new Constraint<String>("help"));
        defineAction(ICSChargeImport.class, "doImportHolidays", null);
        defineAction(ICSChargeImport.class, "doImportICS", new Class[] {String.class});
        Action<Object> mdImport = defineAction(FBRImport.class, FBRImport.MTD_DOIMPORTHUMANREADABLE, new Class[] {String.class});
        defineAction(PrintUtil.class, "print", new Class[] {String.class});

        BeanDefinition<Charge> chargeDef = BeanDefinition.getBeanDefinition(Charge.class);
        chargeDef.addAction(new ActionImportHolidays());
        chargeDef.addAction(new ActionImportCalendar());
        chargeDef.addAction(new SpecifiedAction<>(mdImport.getName(), null));
        chargeDef.saveDefinition();
        
        /*
         * define a Controller as Collector of Actions of a Bean
         */
        final BeanDefinition timeActionBean = new BeanDefinition(Charge.class);
        timeActionBean.setName("time-actions");
        BeanDefinition.define(timeActionBean);
        final Controller controller = new Controller(timeActionBean.getName());
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
        controller.getPresentable().setIcon("icons/forward.png");
        controller.saveDefinition();

        /*
         * define a Controller as Collector of Actions of a Bean
         */
        final BeanDefinition reservationBean = new BeanDefinition(Charge.class);
        reservationBean.setName("reservation");
        BeanDefinition.define(reservationBean);
        final Controller reservation = new Controller(reservationBean.getName());
        reservationBean.getActions().clear();
        reservationBean.addAction(new SecureAction("reservate", "reservate") {
            @Override
            public Object action() throws Exception {
                return reservation;
            }
        });
        timeActionBean.addAction(new SecureAction("cancel", "cancel") {
            @Override
            public Object action() throws Exception {
                return reservation;
            }
        });
        reservation.setItemProvider(new Increaser("fromTime", 12, 3600 * 1000));
        reservationBean.saveDefinition();
        reservation.getPresentable().setIcon("icons/forward.png");
        reservation.saveDefinition();

        /*
         * define own beans to present your entities another way
         */
        Collection<Times> times = ENV.get(IBeanContainer.class).getBeans(Times.class, UNDEFINED, UNDEFINED);

        BeanCollector<Collection<Times>, Times> beanCollector =
            new BeanCollector<Collection<Times>, Times>(Times.class, times, BeanCollector.MODE_ALL, null);

        AttributeDefinition space1 = beanCollector.getPresentationHelper().addSpaceValue();
        beanCollector.addAttribute("pathTest", new PathExpression<>(Times.class, "relation.pathTest"), null, null);
        beanCollector.addAttribute("rule-test", new RuleExpression<>(Times.class, "§test-import"), null, null);
        beanCollector
            .addAttribute(
                "sql-test",
                new SQLExpression<>(
                    Times.class,
                    "?" + queryResult.getQueryName(), Object[].class),
                null, null);
        beanCollector.addAttribute("virtual-test", "I'm virtual", null, null, null);
        beanCollector.addAttribute(ATTR_BINARY, new Attachment("picture", ENV.getConfigPath()
            + "/icons/attach.png"), null, null);
        beanCollector.setAttributeFilter("pathTest", ATTR_TIMESTAMP, ATTR_TIME, ATTR_DATE, space1.getName(),
            ATTR_STRING,
            ATTR_OBJECT, ATTR_IMMUTABLEINTEGER);
        //more fields on one line (one field has grid-width 3)
        beanCollector.getPresentable().setLayout((Serializable) MapUtil.asMap(L_GRIDWIDTH, 12));
        //let the field 'comment' grow to full width
        beanCollector.getAttribute(ATTR_OBJECT).getPresentation()
            .setLayoutConstraints((Serializable) MapUtil.asMap(ATTR_SPANCOL, 11, ATTR_BORDER, 1, ATTR_SIZE, 150));

        /*
         * test csheet as logic table like excel
         */
        CSheet cSheet = new CSheet("csheet", 3, 3);
        cSheet.saveDefinition();

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
        
        createUser("MUSTER", "meinmuster", "SA", "", true);
        
        /*
         * define your own navigation stack
         */
        return beanCollector;
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
        assertEquals(new BigDecimal(11.5), c.getValue());

        //checking rulecovers with volatile caches (the iterating is an workaround as on debugging it works always)
        Map lc = iterateRuleCoverWithVolatile(() -> bean.getAttribute(ATTR_VALUE).getPresentation().getLayoutConstraints());
        assertEquals(redColorStyle, lc.get("style"));

        String style = iterateRuleCoverWithVolatile(() -> bean.getAttribute(ATTR_FROMDATE).getPresentation().getLayoutConstraints());
        assertEquals(redColorStyle, style);

        //test it on value-column
        style = iterateRuleCoverWithVolatile(() -> bean.getAttribute(ATTR_FROMDATE).getColumnDefinition().getPresentable().getLayoutConstraints());
        assertEquals(redColorStyle, style);

        //test it on value-column in beancollector -> here it must be null, because its a definition, not a bean value!
        BeanCollector<Collection<Charge>,Charge> collector = BeanCollector.getBeanCollector(Arrays.asList(c), 0);
        style = collector.getAttribute(ATTR_FROMDATE).getColumnDefinition().getPresentable().getLayoutConstraints();
        assertEquals(null, style);

        assertEquals(IPresentable.TYPE_TIME, bean.getAttribute(ATTR_FROMTIME).getPresentation().getType());
        assertFalse(BeanDefinition.getBeanDefinition(Chargeitem.class).getAttribute(ATTR_CHARGE).getConstraint().isNullable());
        
        assertEquals(c, collector.nextRow());

        //test the queries
        Pool qpool = ENV.get(Pool.class);
        assertEquals(qpool.get(STAT_TIMESHEET_STATISTICS, Query.class).getColumnNames(), Arrays.asList("Month", "Workdays", "Hours", "Dayhours", "Ill", "Holiday"));
        assertEquals(qpool.get(STAT_PROJECTS, Query.class).getColumnNames(), Arrays.asList("Project", "Hours"));
        assertEquals(qpool.get(STAT_TYPES, Query.class).getColumnNames(), Arrays.asList("Type", "Hours"));
        
        //test virtual beans
        Collection<BeanDefinition<?>> virtualDefinitions = BeanDefinition.loadVirtualDefinitions();
        int count = 0;
        for (BeanDefinition<?> beandef : virtualDefinitions) {
            if (beandef instanceof Statistic)
                count++;
            else if (beandef instanceof Compositor)
                count++;
            else if (beandef.getName().equals(new QueryResult(STAT_TIMESHEET_STATISTICS).getName()))
                count++;
            else if (beandef.getName().equals(new QueryResult(STAT_PROJECTS).getName()))
                count++;
            else if (beandef.getName().equals(new QueryResult(STAT_TYPES).getName()))
                count++;
        }
        assertEquals(6, count);
        super.stop();
    }

    /** on parallel testing the volatile may not work 
     * @param attrValue */
	private <T> T iterateRuleCoverWithVolatile(Supplier<T> supplier) {
		int i = 0;
		T result = null;
		do {
        	result = supplier.get();
        	if (result != null)
        		return result;
        	System.out.println("iteration " + i + " to evaluate rulecover value");
        	ConcurrentUtil.sleep(100);
        	i++;
        } while (i < 10);
		return result;
	}
}
