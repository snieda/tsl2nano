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

import static org.anonymous.project.presenter.ChargeConst.*;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.Time;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import my.app.MyApp;
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
import org.apache.commons.logging.Log;

import de.tsl2.nano.bean.IBeanContainer;
import de.tsl2.nano.bean.def.Attachment;
import de.tsl2.nano.bean.def.AttributeDefinition;
import de.tsl2.nano.bean.def.BeanCollector;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.Constraint;
import de.tsl2.nano.bean.def.IBeanCollector;
import de.tsl2.nano.bean.def.IPageBuilder;
import de.tsl2.nano.bean.def.IPresentable;
import de.tsl2.nano.bean.def.IPresentableColumn;
import de.tsl2.nano.bean.def.PathExpression;
import de.tsl2.nano.bean.def.SecureAction;
import de.tsl2.nano.bean.def.VAttribute;
import de.tsl2.nano.bean.def.ValueColumn;
import de.tsl2.nano.bean.def.ValueExpression;
import de.tsl2.nano.collection.MapUtil;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.DateUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.execution.ScriptUtil;
import de.tsl2.nano.h5.Controller;
import de.tsl2.nano.h5.NanoH5;
import de.tsl2.nano.h5.QueryResult;
import de.tsl2.nano.h5.RuleCover;
import de.tsl2.nano.h5.SpecifiedAction;
import de.tsl2.nano.h5.expression.Query;
import de.tsl2.nano.h5.expression.QueryPool;
import de.tsl2.nano.h5.expression.RuleExpression;
import de.tsl2.nano.h5.expression.SQLExpression;
import de.tsl2.nano.h5.navigation.BeanAct;
import de.tsl2.nano.h5.navigation.Parameter;
import de.tsl2.nano.h5.navigation.Workflow;
import de.tsl2.nano.h5.websocket.WSEvent;
import de.tsl2.nano.h5.websocket.WebSocketRuleDependencyListener;
import de.tsl2.nano.incubation.specification.ParType;
import de.tsl2.nano.incubation.specification.actions.Action;
import de.tsl2.nano.incubation.specification.actions.ActionPool;
import de.tsl2.nano.incubation.specification.rules.Rule;
import de.tsl2.nano.incubation.specification.rules.RulePool;
import de.tsl2.nano.incubation.specification.rules.RuleScript;
import de.tsl2.nano.messaging.ChangeEvent;
import de.tsl2.nano.messaging.IListener;
import de.tsl2.nano.util.codegen.ClassGenerator;

/**
 * Creates a timesheet configuration on NanoH5 and anyway database
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class Timesheet extends NanoH5 {
    private static final Log LOG = LogFactory.getLog(MyApp.class);

    /**
     * constructor
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
        /*
         * define all beans
         */
        define(Type.class, TypeConst.ATTR_NAME);
        define(Category.class, CategoryConst.ATTR_NAME);
        define(Account.class, AccountConst.ATTR_NAME);
        define(Property.class, PropertyConst.ATTR_AKEY);
        define(Organisation.class, OrganisationConst.ATTR_NAME);
        define(Party.class, PartyConst.ATTR_NAME);
        define(Address.class, ve(AddressConst.ATTR_CITY) + ", " + ve(AddressConst.ATTR_STREET));
        define(Location.class, LocationConst.ATTR_NAME);
        define(Digital.class, DigitalConst.ATTR_NAME);
        define(Coordinate.class, ve(CoordinateConst.ATTR_X) + "-" + ve(CoordinateConst.ATTR_Y) + "-" + ve(CoordinateConst.ATTR_X));
        define(Area.class, AreaConst.ATTR_NAME);
        define(Item.class, ItemConst.ATTR_NAME);
        define(Chargeitem.class, ChargeitemConst.ATTR_ITEM);
        define(Discharge.class, ve(DischargeConst.ATTR_CHARGE) + " (" + ve(DischargeConst.ATTR_DATE) + ": " + ve(DischargeConst.ATTR_VALUE));
        
        /*
         * configure the main type: Charge (Zeiterfassung)
         */
        BeanDefinition<Charge> charge = define(Charge.class, ve(ChargeConst.ATTR_CHARGEITEM) + " (" + ve(ChargeConst.ATTR_FROMDATE) + ": " + ve(ChargeConst.ATTR_VALUE) + ")"
            , ATTR_FROMDATE, ATTR_FROMTIME, ATTR_TOTIME, ATTR_PAUSE, ATTR_PARTY, ATTR_CHARGEITEM, ATTR_VALUE);
        IPresentableColumn column = charge.getAttribute(ATTR_VALUE).getColumnDefinition();
        if (column instanceof ValueColumn)
            ((ValueColumn)column).setStandardSummary(true);
        
        /*
         * add dependency listeners on rules
         */
        RuleScript<BigDecimal> calcTime = new RuleScript<BigDecimal>("calcTime", "(toTime - fromTime) - pause", null);
        Time t0800 = DateUtil.getTime(8, 0);
        Time t1700 = DateUtil.getTime(17, 0);
        Time t0000 = DateUtil.getTime(0, 0);
        Time t0030 = DateUtil.getTime(0, 30);
        calcTime.addSpecification("notime", "check for zero-times", BigDecimal.ZERO, MapUtil.asMap("fromTime", t0800, "toTime", t0800, "pause", t0000));
        calcTime.addSpecification("standard", "standard work day", new BigDecimal(8.5), MapUtil.asMap("fromTime", t0800, t1700, t0030));
        ENV.get(RulePool.class).add(calcTime.getName(), calcTime);
        IListener<WSEvent> listener = new WebSocketRuleDependencyListener(charge.getAttribute(ATTR_VALUE), ATTR_VALUE, calcTime.getName());
        charge.getAttribute(ATTR_FROMTIME).changeHandler().addListener(listener);
        charge.getAttribute(ATTR_TOTIME).changeHandler().addListener(listener);
        
        /*
         * add attribute presentation rules
         */
        RuleScript<String> presValueColor = new RuleScript<String>("presValueColor", "value > 10 ? red : black", null);
        RuleCover ruleCover = new RuleCover(MapUtil.asMap("presentation.background", presValueColor.getName()));
        ruleCover.connect(charge.getAttribute(ATTR_VALUE));
        
        charge.saveDefinition();

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
        testRule.addConstraint("x1", new Constraint<BigDecimal>(BigDecimal.class, BigDecimal.ZERO, BigDecimal.ONE));
        testRule.addConstraint(Rule.KEY_RESULT, new Constraint<BigDecimal>(BigDecimal.class, BigDecimal.ZERO,
            BigDecimal.TEN));
        testRule.addSpecification("notA-1-2", null, 4, MapUtil.asMap("x1", 1, "x2", 2));
        testRule.addSpecification("A-2-1", null, 2, MapUtil.asMap("x1", 2, "x2", 1));
        ENV.get(RulePool.class).add("test", testRule);

        //another rule to test sub-rule-imports
        ENV.get(RulePool.class).add("test-import",
            new Rule<BigDecimal>("test-import", "A ? 1 + §test : (x2 * 3)", par));

        BigDecimal result =
            (BigDecimal) ENV.get(RulePool.class).get("test-import")
                .run(MapUtil.asMap("A", true, "x1", new BigDecimal(1), "x2", new BigDecimal(2)));

        LOG.info("my test-import rule result:" + result);

        /*
         * define a query
         */
        String qstr = "select db_begin from times t where t.db_end = :dbEnd";

        HashMap<String, Serializable> par1 = new HashMap<>();
        Query<Object> query = new Query<>("times.begin", qstr, true, par1);
        QueryPool queryPool = ENV.get(QueryPool.class);
        queryPool.add(query.getName(), query);

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
        ENV.get(ActionPool.class).add("ant", a);

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
         * define a specific bean-collector presenting a query (SQL or JPA-QL)
         */
        qstr = "\nselect t.day as Day, p.name as Project t.dbbegin as Begin, t.dbend as End, t.pause as Pause\n"
            + "from times t join project p on p.id = times.projid\n"
            + "where 1 = 1\n";

        query = new Query<>("times-overview", qstr, true, null);
        queryPool.add(query.getName(), query);

        QueryResult qr = new QueryResult<>(query.getName());
        qr.saveVirtualDefinition(query.getName());

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
                "rule-string-integer"));

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

    private String ve(String expression) {
        return "{" + expression + "}";
    }

    /**
     * define
     * @param valueExpression 
     * @param type 
     */
    protected <T> BeanDefinition<T> define(Class<T> type, String valueExpression, String...attributeFilter) {
        BeanDefinition<T> bean = BeanDefinition.getBeanDefinition(type);
        String ve = valueExpression.contains("{") ? valueExpression : "{" + valueExpression + "}";
        bean.setValueExpression(new ValueExpression<T>(ve, type));
        if (!Util.isEmpty(attributeFilter))
            bean.setAttributeFilter(attributeFilter);
        bean.saveDefinition();
        return bean;
    }
    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        startApplication(MyApp.class, MapUtil.asMap(0, "service.url"), args);
    }

}
