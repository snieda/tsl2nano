/**
 * 
 */
package my.app;

import static de.tsl2.nano.bean.def.IPresentable.UNDEFINED;
import static de.tsl2.nano.h5.Html5Presentation.L_GRIDWIDTH;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_BORDER;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_SIZE;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_SPANCOL;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.anonymous.project.Times;
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
import de.tsl2.nano.bean.def.IValueDefinition;
import de.tsl2.nano.bean.def.PathExpression;
import de.tsl2.nano.bean.def.SecureAction;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.core.Environment;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.execution.ScriptUtil;
import de.tsl2.nano.h5.collector.Controller;
import de.tsl2.nano.h5.NanoH5;
import de.tsl2.nano.h5.collector.QueryResult;
import de.tsl2.nano.h5.SpecifiedAction;
import de.tsl2.nano.h5.expression.Query;
import de.tsl2.nano.h5.expression.QueryPool;
import de.tsl2.nano.h5.expression.RuleExpression;
import de.tsl2.nano.h5.expression.SQLExpression;
import de.tsl2.nano.h5.navigation.BeanAct;
import de.tsl2.nano.h5.navigation.Parameter;
import de.tsl2.nano.h5.navigation.Workflow;
import de.tsl2.nano.h5.websocket.WSEvent;
import de.tsl2.nano.h5.websocket.WebSocketDependencyListener;
import de.tsl2.nano.h5.websocket.WebSocketRuleDependencyListener;
import de.tsl2.nano.specification.ParType;
import de.tsl2.nano.specification.actions.Action;
import de.tsl2.nano.specification.actions.ActionPool;
import de.tsl2.nano.specification.rules.Rule;
import de.tsl2.nano.specification.rules.RulePool;

/**
 * Example implementations for Nano.h5
 * 
 * @author Tom
 * 
 */
public class MyApp extends NanoH5 {
    private static final Log LOG = LogFactory.getLog(MyApp.class);

    /**
     * @throws IOException
     */
    public MyApp() throws IOException {
    }

    /**
     * @param ipport ip + port
     * @param builder
     * @param navigation
     * @throws IOException
     */
    public MyApp(String ipport, IPageBuilder<?, String> builder) throws IOException {
        super(ipport, builder);
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked", "serial" })
    protected BeanDefinition<?> createBeanCollectors(List<Class> beanClasses) {

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
        Environment.persist(workflow);

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
        Environment.get(RulePool.class).add("test", testRule);

        //another rule to test sub-rule-imports
        Environment.get(RulePool.class).add("test-import",
            new Rule<BigDecimal>("test-import", "A ? 1 + §test : (x2 * 3)", par));

        BigDecimal result =
            (BigDecimal) Environment.get(RulePool.class).get("test-import")
                .run(MapUtil.asMap("A", true, "x1", new BigDecimal(1), "x2", new BigDecimal(2)));

        LOG.info("my test-import rule result:" + result);

        /*
         * define a query
         */
        String qstr = "select db_begin from times t where t.db_end = :dbEnd";

        HashMap<String, Serializable> par1 = new HashMap<>();
        Query<Object> query = new Query<>("times.begin", qstr, true, par1);
        QueryPool queryPool = Environment.get(QueryPool.class);
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
        a.addConstraint("arg1", new Constraint<String>(Environment.getConfigPath() + "antscripts.xml"));
        a.addConstraint("arg2", new Constraint<String>("help"));
        Environment.get(ActionPool.class).add("ant", a);

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
        Collection<Times> times = Environment.get(IBeanContainer.class).getBeans(Times.class, UNDEFINED, UNDEFINED);

        BeanCollector<Collection<Times>, Times> beanCollector =
            new BeanCollector<Collection<Times>, Times>(times, BeanCollector.MODE_ALL);

        AttributeDefinition space1 = beanCollector.getPresentationHelper().addSpaceValue();
        beanCollector.addAttribute("path-test", new PathExpression<>(Times.class, "id.dbBegin"), null, null);
        beanCollector.addAttribute("rule-test", new RuleExpression<>(Times.class, "§test-import"), null, null);
        beanCollector
            .addAttribute(
                "sql-test",
                new SQLExpression<>(
                    Times.class,
                    "?" + query.getName(), Object[].class),
                null, null);
        beanCollector.addAttribute("virtual-test", "I'm virtual", null, null, null);
        beanCollector.addAttribute("picture", new Attachment("picture", Environment.getConfigPath()
            + "/icons/attach.png"), null, null);
        beanCollector.setAttributeFilter("path-test", "creation", "dbEnd", "pause", space1.getName(), "project",
            "comment");
        //more fields on one line (one field has grid-width 3)
        beanCollector.getPresentable().setLayout((Serializable) MapUtil.asMap(L_GRIDWIDTH, 12));
        //let the field 'comment' grow to full width
        beanCollector.getAttribute("comment").getPresentation()
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

        /*
         * create a dependency listener
         */
        BeanDefinition b =
            BeanDefinition.getBeanDefinition(BeanClass.createBeanClass("org.anonymous.project.Person").getClazz());
        ((AttributeDefinition) b.getAttribute("organisation")).changeHandler().addListener(
            new WebSocketDependencyListener() {

                @Override
                protected Object evaluate(WSEvent evt) {
                    //new value of attribute 'organisation'
                    Object value = evt.newValue;
                    //here we set dynamically which attribute depends on changes
                    setAttribute(b.getAttribute("shortname"));
                    //the evt.source holds the changed bean value
                    IValueDefinition srcValue = (IValueDefinition)evt.getSource();
                    //here we get the old value of the dependent attribute 'shortname'
                    //through srcValue.getInstance() you could get all other values with Bean.getBean(srcValue)
                    Object lastAttributeValue = getAttribute().getValue(srcValue.getInstance());
                    //return the refreshed value for attribute 'shortname' 
                    return value + "/" + lastAttributeValue;
                }
            });

        /*
         * create a dependency listener with a rule
         */
        BeanDefinition b =
            BeanDefinition.getBeanDefinition(BeanClass.createBeanClass("org.anonymous.project.Person").getClazz());
        ((AttributeDefinition) b.getAttribute("name")).changeHandler().addListener(
            new WebSocketRuleDependencyListener<T>(b.getAttribute("shortname"), "shortname", "rule-shortname");

        /*
         * use file attachments
         */
        ((AttributeDefinition) b.getAttribute("comment")).getPresentation().setType(IPresentable.TYPE_ATTACHMENT);

        /*
         * define your own navigation stack
         */
        return beanCollector;
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        startApplication(MyApp.class, MapUtil.asMap(0, "service.url"), args);
    }
}
