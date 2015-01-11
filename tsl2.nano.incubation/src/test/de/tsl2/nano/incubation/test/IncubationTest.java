/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider, Thomas Schneider
 * created on: Nov 11, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.incubation.test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import java.io.InputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Time;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.junit.Test;

import de.tsl2.nano.bean.BeanUtil;
import de.tsl2.nano.bean.def.Constraint;
import de.tsl2.nano.collection.MapUtil;
import de.tsl2.nano.collection.TableList;
import de.tsl2.nano.core.Environment;
import de.tsl2.nano.core.Finished;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.execution.IRunnable;
import de.tsl2.nano.core.execution.Profiler;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.ByteUtil;
import de.tsl2.nano.core.util.Crypt;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.NetUtil;
import de.tsl2.nano.core.util.Permutator;
import de.tsl2.nano.core.util.PrintUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.XmlUtil;
import de.tsl2.nano.incubation.network.JobServer;
import de.tsl2.nano.incubation.repeat.IChange;
import de.tsl2.nano.incubation.repeat.ICommand;
import de.tsl2.nano.incubation.repeat.impl.AChange;
import de.tsl2.nano.incubation.repeat.impl.ACommand;
import de.tsl2.nano.incubation.repeat.impl.CommandManager;
import de.tsl2.nano.incubation.specification.ParType;
import de.tsl2.nano.incubation.specification.Pool;
import de.tsl2.nano.incubation.specification.rules.Rule;
import de.tsl2.nano.incubation.specification.rules.RulePool;
import de.tsl2.nano.incubation.terminal.Action;
import de.tsl2.nano.incubation.terminal.IItem;
import de.tsl2.nano.incubation.terminal.Input;
import de.tsl2.nano.incubation.terminal.MainAction;
import de.tsl2.nano.incubation.terminal.Option;
import de.tsl2.nano.incubation.terminal.Terminal;
import de.tsl2.nano.incubation.terminal.TerminalAdmin;
import de.tsl2.nano.incubation.terminal.Tree;
import de.tsl2.nano.incubation.vnet.Connection;
import de.tsl2.nano.incubation.vnet.Cover;
import de.tsl2.nano.incubation.vnet.ILocatable;
import de.tsl2.nano.incubation.vnet.Net;
import de.tsl2.nano.incubation.vnet.Node;
import de.tsl2.nano.incubation.vnet.Notification;
import de.tsl2.nano.incubation.vnet.ThreadingEventController;
import de.tsl2.nano.incubation.vnet.neuron.VNeuron;
import de.tsl2.nano.incubation.vnet.routing.Location;
import de.tsl2.nano.incubation.vnet.routing.RoutingAStar;
import de.tsl2.nano.incubation.vnet.workflow.ComparableMap;
import de.tsl2.nano.incubation.vnet.workflow.VActivity;
import de.tsl2.nano.logictable.DefaultHeader;
import de.tsl2.nano.logictable.EquationSolver;
import de.tsl2.nano.logictable.LogicTable;
import de.tsl2.nano.messaging.EventController;
import de.tsl2.nano.messaging.IListener;
import de.tsl2.nano.test.TypeBean;
import de.tsl2.nano.util.operation.ConditionOperator;
import de.tsl2.nano.util.operation.Function;

/**
 * basic tests for algorithms to be refactored to the project tsl2nano.common in future.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings({ "unchecked", "rawtypes", "serial" })
public class IncubationTest {

    @Test
    public void testEventPerformance() {
        final IListener<Object> listener = new IListener<Object>() {
            @Override
            public void handleEvent(Object event) {
//                System.out.print('.');

//                int z = 0;
//                for (int i = 0; i < 10000000; i++) {
//                    z = i;
//                }
//              System.out.print(z);

                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    ManagedException.forward(e);
                }
                System.out.print('.');
            }
        };
        final EventController c = new EventController();
        final ThreadingEventController ct = Environment.get(ThreadingEventController.class);
        for (int i = 0; i < 100; i++) {
            c.addListener(listener);
            ct.addListener(listener);
        }
        final Object event = new Object();
        Profiler.si().compareTests("test simple and multi-threaded event-handling", 100, new Runnable() {
            public void run() {
                c.fireEvent(event);
            }
        }, new Runnable() {
            public void run() {
                ct.fireEvent(event);
            }
        });
    }

    @Test
    public void testVNetWithNeuralNet() {
        Net<VNeuron, Float> net = new Net<VNeuron, Float>();
//        net.setWorkParallel(false);
        Node<VNeuron, Float> nHunger = net.add(new VNeuron("hunger"));
        Node<VNeuron, Float> nHabe = net.addAndConnect(new VNeuron("habe"), nHunger.getCore(), -1f);
        net.addAndConnect(new VNeuron("ich"), nHabe.getCore(), -1f);

        //create a callback for responses
        IListener<Notification> responseHandler = new IListener<Notification>() {
            @Override
            public synchronized void handleEvent(Notification event) {
                System.out.println("RESPONSE: " + event.getNotification());
            }
        };

        //start input
        net.notify(new Notification("ich", 1f, null, responseHandler));
        net.notify(new Notification("hunger", 1f, null, responseHandler));

        //wait for all threads to complete
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            ManagedException.forward(e);
        }

        //should output: ich habe hunger
    }

    @Test
    public void testVNetWithWorkflow() {
        final ComparableMap<CharSequence, Object> state = new ComparableMap<CharSequence, Object>();
        state.put("A", true);
        state.put("B", true);

        class Act extends VActivity<String, String> {
            ConditionOperator<Object> op = new ConditionOperator<Object>(state);

            public Act(String name, String condition, String expression) {
                super(name, condition, expression);
            }

            @Override
            public String action() throws Exception {
                return (String) op.eval(expression);
            }

            @Override
            public boolean canActivate(Map parameter) {
                //sometimes we get a string out of eval, but sometimes a boolean. so we generalize it ;-)
                return (Boolean) Boolean.valueOf(StringUtil.toString(op.eval(condition)));
            }

        }
        Net<VActivity<String, String>, ComparableMap<CharSequence, Object>> net =
            new Net<VActivity<String, String>, ComparableMap<CharSequence, Object>>();
//        net.setWorkParallel(false);

        //works without linking the states to each other, too!!
        Node<VActivity<String, String>, ComparableMap<CharSequence, Object>> state0 = net.add(new Act("state0",
            "A&B",
            "A&B?C:D"));
        Node<VActivity<String, String>, ComparableMap<CharSequence, Object>> state1 =
            net.addAndConnect(new Act("state1",
                "C",
                "D"),
                state0.getCore(),
                state);
        Node<VActivity<String, String>, ComparableMap<CharSequence, Object>> state2 =
            net.addAndConnect(new Act("state2",
                "D",
                "E"),
                state1.getCore(),
                state);

        //create a callback for responses
        IListener<Notification> responseHandler = new IListener<Notification>() {
            @Override
            public synchronized void handleEvent(Notification event) {
                System.out.println("RESPONSE: " + event.getNotification());
                assertEquals("C", event.getNotification());
            }
        };

        //start input. don't set a query path, notification is always true, all items are linked
        net.notify(new Notification(null, state, null, responseHandler));
//        net.notify(new Notification(null, true, null, responseHandler));

        //wait for all threads to complete
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            ManagedException.forward(e);
        }
        //should output: C
    }

    @Test
    public void testVNetWithRouting() {
        /*
         * kuerzeste Verbindung zwischen Wuerzburg und Saarbruecken.
         * siehe Wikipedia Beispiel
         */
        Net<Location, Float> net = new Net<Location, Float>();
        Node<Location, Float> saarbruecken = net.add(new Location("Saarbrücken", 222, 0, 0));
        Node<Location, Float> kaiserslautern = net.add(new Location("Kaiserslautern", 158, 0, 0));
        Node<Location, Float> karlsruhe = net.add(new Location("Karlsruhe", 140, 0, 0));
        Node<Location, Float> ludwigshafen = net.add(new Location("Ludwigshafen", 108, 0, 0));
        Node<Location, Float> frankfurt = net.add(new Location("Frankfurt", 96, 0, 0));
        Node<Location, Float> heilbronn = net.add(new Location("Heilbronn", 87, 0, 0));
        Node<Location, Float> wuerzburg = net.add(new Location("Würzburg", 0, 0, 0));

        saarbruecken.connect(kaiserslautern, 70f);
        saarbruecken.connect(karlsruhe, 145f);

        kaiserslautern.connect(ludwigshafen, 53f);
        kaiserslautern.connect(frankfurt, 103f);
        karlsruhe.connect(heilbronn, 84f);

        heilbronn.connect(wuerzburg, 102f);
        frankfurt.connect(wuerzburg, 116f);
        ludwigshafen.connect(wuerzburg, 183f);

        RoutingAStar routing = new RoutingAStar();
        Connection<Location, Float> route = routing.route(saarbruecken, wuerzburg);
        if (route == null)
            fail("No route found for " + saarbruecken + " --> " + wuerzburg);
        else {
            Node<Location, Float>[] shortestWay = new Node[] { kaiserslautern, frankfurt, wuerzburg };
            Collection<Connection<Location, Float>> navigation = routing.navigate(saarbruecken, route, null);
            log("Navigation: " + navigation);
            int i = 0;
            if (navigation.size() < shortestWay.length)
                fail("navigation incomplete");
            for (Connection<Location, Float> connection : navigation) {
                if (!connection.getDestination().equals(shortestWay[i++]))
                    fail("Routing didn't find shortest Way!");
            }
        }
    }

    @Test
    public void testVNetWithKanbanFlow() {
        int testcount = 100;
        /*
         *  - create 8 nodes with a calculating runnable
         *  - call notifyIdle(..) on >8 notifications
         *  - check the results
         *  - check performance with profiler
         */

        final Net<TCover<TRunner, Float>, Float> net = new Net<TCover<TRunner, Float>, Float>();
        Node<TCover<TRunner, Float>, Float> n = null;
        for (int i = 0; i < 8; i++) {
            n = net.add(new TCover<TRunner, Float>(new TRunner(), Float.valueOf(i)));
        }

        /*
         * first: check monolithic performance
         */
        Profiler.si().stressTest("monolithic test", testcount, n.getCore().getContent());

        /*
         * second: test with own observing
         */
        final IListener<Notification> responseObserver = new IListener<Notification>() {
            @Override
            public void handleEvent(Notification event) {
                log("response: " + event.getResponse());
            }
        };
        log(net.dump());
        long start = System.currentTimeMillis();

        /*
         * start the test
         */
        Profiler.si().stressTest("kanbanflow on 8 nodes", testcount, new Runnable() {
            @Override
            public void run() {
                net.notifyFirstIdle(new Notification(null, "working-test"), responseObserver, 10 * 1000, 100);
            }
        });
        net.waitForIdle(60 * 1000);

        log("total test time: " + DateFormat.getTimeInstance().format(new Time(System.currentTimeMillis() - start)));
        log(net.dump());

        /*
         * do the whole test calling a net convenience method
         */
        net.resetStatistics();
        Collection<Notification> notifications = new ArrayList<Notification>();
        for (int i = 0; i < testcount; i++) {
            notifications.add(new Notification(null, "working-test-" + i));
        }
        Collection<Object> result = net.notifyIdlesAndCollect(notifications, Object.class);
        assertTrue(result.size() == testcount);
        log(StringUtil.toFormattedString(result, 10000, true));
        log(net.dump());
    }

    @Test
    public void testEquationSolver() {
        String f = "1+ ((x1 + x2)*3 + 4)+5";
//        char[] cs = f.toCharArray();
//        List<String> s = new ArrayList<String>(cs.length);
//        for (int i = 0; i < cs.length; i++) {
//            s.add(String.valueOf(cs[i]));
//        }
//        String term = StringUtil.extract(f, "\\([^)(]*\\)");
//        String op1 = StringUtil.extract(f, "[a-zA-Z0-9]+" + );
//        Structure<List<String>, String, String, String> structure = new Structure<List<String>, String, String, String>(s,
//            "(",
//            ")");
//        Collection<List<String>> items = structure.getTree().values();
//        for (List<String> list : items) {
//            log(items.toString());
//        }
        BigDecimal x1 = new BigDecimal(8);
        BigDecimal x2 = new BigDecimal(9);
        Map<String, Object> values = new Hashtable<String, Object>();
        values.put("x1", x1);
        values.put("x2", x2);
        assertEquals(new BigDecimal(61), new EquationSolver(null, values).eval(f));
    }

    @Test
    public void testFunction() {
        Function<Number> function = BeanClass.createInstance(Function.class);
        Number result = function.eval("min(pow(x1,x2), 3)", MapUtil.asMap("x1", 2d, "x2", 2d));
        log(result.toString());
    }

    @Test
    public void testLogicTable() {
        TableList<DefaultHeader, String> table = new LogicTable<DefaultHeader, String>(2).fill(String.class, 2);
        table.set(0, 0, new BigDecimal(10));
        table.set(1, 0, new BigDecimal(9));
        table.set(1, 1, "=A1 * A2");
        assertEquals(new BigDecimal(90), table.get(1, 1));
    }

    @Test
    public void testJobServer() throws Exception {
        JobServer jobServer = new JobServer();
        String result = jobServer.executeWait("test", new TestJob(), 5000);
        jobServer.close();
        assertNotNull(result);
    }

    @Test
    public void testRules() throws Exception {
        Rule<BigDecimal> rule =
            new Rule<BigDecimal>("test", "A ? (pow(x1, x2) + 1) : (x2 * 2)",
                (LinkedHashMap<String, ParType>) MapUtil.asMap("A",
                    ParType.BOOLEAN,
                    "x1",
                    ParType.NUMBER,
                    "x2",
                    ParType.NUMBER));
        BigDecimal r1 = rule.run(MapUtil.asMap("A", true, "x1", new BigDecimal(1), "x2", new BigDecimal(2)));
        assertEquals(new BigDecimal(2), r1);

        //use simplified parameter definition
        rule =
            new Rule<BigDecimal>("test", "A ? (x1 + 1) : (x2 * 2)", (LinkedHashMap<String, ParType>) MapUtil.asMap("A",
                ParType.BOOLEAN,
                "x1",
                ParType.NUMBER,
                "x2",
                ParType.NUMBER));
        rule.addConstraint("x1", new Constraint(BigDecimal.class, new BigDecimal(0), new BigDecimal(1)));
        BigDecimal r2 = rule.run(MapUtil.asMap("A", false, "x1", new BigDecimal(1), "x2", new BigDecimal(2)));
        assertEquals(new BigDecimal(4), r2);
        XmlUtil.saveXml("test.xml", rule);

        Pool pool = new RulePool();
        pool.add(rule.getName(), rule);
        Environment.addService(pool);
        Rule<BigDecimal> ruleWithImport =
            new Rule<BigDecimal>("test-import", "A ? 1 + §test : (x2 * 3)",
                (LinkedHashMap<String, ParType>) MapUtil.asMap("A",
                    ParType.BOOLEAN,
                    "x1",
                    ParType.NUMBER,
                    "x2",
                    ParType.NUMBER));
        rule.addConstraint("result", new Constraint(BigDecimal.class, new BigDecimal(1), new BigDecimal(4)));
        BigDecimal r3 = ruleWithImport.run(MapUtil.asMap("A", true, "x1", new BigDecimal(1), "x2", new BigDecimal(2)));
        assertEquals(new BigDecimal(3), r3);
        XmlUtil.saveXml("test-import.xml", ruleWithImport);
    }

    @Test
    public void testUndoRedo() throws Exception {
        /*
         * tests a simple regxp replacer
         * - item is a regexp
         * - old is the old text, found by regexp
         * - new is the new replaced text
         */
        StringBuilder text = new StringBuilder("A and B are not C");
        Command a = new Command(text, new AChange("(A[0-9]*)", "A", "A1"));
        Command b = new Command(text, new AChange("(B[0-9]*)", "B", "B1"));
        Command c = new Command(text, new AChange("(C[0-9]*)", "C", "C1"));
        undoRedo(a, b, c);
    }

    @Test
    public void testUndoRedoWithEntity() throws Exception {
        EBean tbean = new EBean();
        ECommand a = new ECommand(null, new AChange(null, null, tbean));
        ECommand b = new ECommand(tbean, new AChange("string", null, "astring"));
        ECommand c = new ECommand(tbean, new AChange("immutableInteger", null, 100));
        ECommand d = new ECommand(tbean, new AChange(null, tbean, null));
        undoRedo(b, c);

        //construction and deletion
        CommandManager commandManager = new CommandManager(10);
        assertEquals(a.getContext(), null);
        commandManager.doIt(a);
        assertEquals(a.getContext(), tbean);
        commandManager.doIt(d);
        assertEquals(a.getContext(), null);
        commandManager.undo();
        assertEquals(a.getContext(), tbean);
        commandManager.redo();
        assertEquals(a.getContext(), null);
    }

    public <CONTEXT> void undoRedo(ICommand<CONTEXT>... cmds) throws Exception {
        CommandManager cmdManager = new CommandManager();

        CONTEXT context = cmds[0].getContext();
        CONTEXT origin = BeanUtil.copy(context);
        log("origin : " + origin);

        cmdManager.doIt(cmds);

        CONTEXT changed = BeanUtil.copy(context);
        log("changed: " + changed);

        assertTrue(cmdManager.canUndo());
        assertFalse(cmdManager.canRedo());

        cmdManager.undo();
        cmdManager.undo();
        cmdManager.undo();

        assertFalse(cmdManager.canUndo());
        assertTrue(cmdManager.canRedo());

        log("text   : " + BeanUtil.copy(context));
        assertEquals(origin, context);

        cmdManager.redo();
        cmdManager.redo();
        cmdManager.redo();

        assertTrue(cmdManager.canUndo());
        assertFalse(cmdManager.canRedo());

        log("re-done: " + context);
        assertEquals(changed, context);
    }

    @Test
    public void testMacro() throws Exception {
        CommandManager cmdManager = new CommandManager();

        EBean tbean = new EBean();
        ECommand a = new ECommand(null, new AChange(null, null, tbean));
        ECommand b = new ECommand(tbean, new AChange("string", null, "astring"));
        ECommand c = new ECommand(tbean, new AChange("immutableInteger", null, 100));
        ECommand d = new ECommand(tbean, new AChange(null, tbean, null));

        cmdManager.getRecorder().record("test.record");
        cmdManager.doIt(a, b, c, d);
        cmdManager.getRecorder().stop();

        EBean mbean = new EBean();
        //now, redo that with on another context object --> macro replay!
        assertEquals(4, cmdManager.getRecorder().play("test.record", mbean));
        assertEquals(tbean, mbean);
    }

    @Test
    public void testInvader() throws Exception {
        final Map p = MapUtil.asMap("data", "Meier", "algorithm", Crypt.ALGO_PBEWithMD5AndDES);
        final String transformer =
            "transformer=\"de.tsl2.nano.core.util.Crypt encrypt ${data} ${password} ${algorithm}\"";
        final String backward = "backward=\"de.tsl2.nano.core.util.Crypt decrypt ${data} ${password} ${algorithm}\"";

        int len = 7;
        Permutator perm = new Permutator(len);
        InputStream in = perm.permute();
        try {
            ByteUtil.forEach(in, len, new IRunnable<Object, byte[]>() {
                @Override
                public Object run(byte[] context, Object... extArgs) {
                    p.put("password", new String(context));
                    String t = StringUtil.insertProperties(transformer, p);
                    String b = StringUtil.insertProperties(backward, p);

                    Permutator.main(new String[] { "source=deutsche-namen.txt", t, b });
                    return null;
                }
            });
        } catch (Finished f) {
            log(f.getMessage());
        }
    }

//    @Test
//    public void testTerminal() throws Exception {
//        Tree root = new Tree("selection1", null, new ArrayList<IItem>(), null);
//        root.add(new Option("option1", null, false, "Option 1"));
//        root.add(new Option("option2", null, false, "Option 2"));
//        root.add(new Option("option3", null, false, "Option 3"));
//        root.add(new Input<Object>("input1", null, "Input 1", null));
//        root.add(new Action("action1", null, new SRunnable(), "Action 1"));
//
//        InputStream in = ByteUtil.getInputStream("1\n2\n3\n4\n5\n\n".getBytes());
//        new Terminal(root, in, System.out, 79, 10, 1).run();
//
//        Terminal.main(new String[] { Terminal.DEFAULT_NAME });
//
//        //admin console
//        Terminal.main(new String[] { Terminal.DEFAULT_NAME, TerminalAdmin.ADMIN });
//    }
//
    @Test
    public void testTerminalTools() throws Exception {
        Tree root = new Tree("Toolbox", "Helpful Utilities");

        Tree printing = new Tree("Printing", null);
        printing.add(new Input("source", "printer-info", "file to print - or only a printer info"));
        printing.add(new Input("printer", "PDFCreator", "printer to use"));
        printing.add(new Input("jobname", "tsl2nano", "print job name"));
        printing.add(new Input("mimetype", "MIME_PCL", "mime type"));
        printing.add(new Input("papersize", "ISO_A4", "paper size"));
        printing.add(new Input("quality", "NORMAL", "print quality"));
        printing.add(new Input("priority", "1", "print priority (1-100)"));
        printing.add(new Input("xsltfile", "test.xsl", "xsl-fo transformation file to do a apache fop"));
        printing.add(new Input("username", null, "user name to be used by the printer"));
        printing
            .add(new MainAction("print", PrintUtil.class, "source", "printer", "papersize", "quality", "priority", "xsltfile", "mimetype", "jobname", "username"));
        root.add(printing);

        Tree crypt = new Tree("Crypt", null);
        crypt.add(new Input("password", null, "password for encryption - if needed by algorithm"));
        crypt.add(new Input("algorithm", "PBEWithMD5AndDES", "encryption algorithm"));
        crypt.add(new Input("text", null, "text to be encrypted. if it starts with 'file:' the file will be read"));
        crypt.add(new Input("base64", true, "whether base64 encoding should be used"));
        crypt.add(new Input("include", ".*", "regular expression to constrain text parts to be encrypted"));
        crypt
            .add(new MainAction(Crypt.class, "password", "algorithm", "text", "base64", "include"));
        root.add(crypt);

        Tree perm = new Tree("Permutator", null);
        perm.add(new Input("source", null, "source collection"));
        perm.add(new Input("transformer", null, "transforming action"));
        perm.add(new Input("swap", null, "whether to swap key and values in destination-map"));
        perm.add(new Input("backward", null, "action to do a back-transformation for each keys value"));
        perm
            .add(new MainAction(Permutator.class, "source", "transformer", "swap", "backward"));
        root.add(perm);

        Tree xml = new Tree("Xml", null);
        xml.add(new Input("source", null, "source file"));
        xml.add(new Input("expression", null, "xpath expression"));
        xml.add(new Action(XmlUtil.class, "transform", "source", Action.KEY_ENV));
        xml.add(new Action(XmlUtil.class, "xpath", "expression", "source"));
        root.add(xml);

//        Tree getjar = new Tree("getJar", null);
//        getjar.add(new Input("name", null, "name, jar-file or class package to load with dependencies from web"));
//        getjar.add(new MainAction(BeanClass.createBeanClass("de.tsl2.nano.jarresolver.JarResolver", null).getClazz(), "name"));
//        root.add(getjar);
//
        Tree net = new Tree("Net", null);
        Tree scan = new Tree("Scan", null);
        net.add(scan);
        scan.add(new Input("ip", NetUtil.getMyIP(), "internet address to be scanned"));
        scan.add(new Input("lowest-port", 0, "lowest port to be scanned"));
        scan.add(new Input("highest-port", 100, "highest port to be scanned"));
        scan.add(new Action(NetUtil.class, "scans", "lowest-port", "highest-port", "ip"));
        Tree wcopy = new Tree("WCopy", null);
        net.add(wcopy);
        wcopy.add(new Input("url", null, "url to get files from"));
        wcopy.add(new Input("dir", null, "local directory to save the downloaded files"));
        wcopy.add(new Input("include", null, "regular expression for files to download"));
        wcopy.add(new Input("exclude", null, "regular exression for files to be filtered"));
        wcopy.add(new Action(NetUtil.class, "wcopy", "url", "dir", "include", "exclude"));

        net.add(new Action(NetUtil.class, "getNetInfo"));
        root.add(net);

        InputStream in = Terminal.createBatchStream("Printing", "jobname", "test", "print", ":quit");
        new Terminal(root, in, System.out, 79, 22, 1).run();

        Terminal.main(new String[] { Terminal.DEFAULT_NAME });

        FileUtil.copy(Terminal.DEFAULT_NAME, "src/resources/" + Terminal.DEFAULT_NAME);
        
        //admin console
        Terminal.main(new String[] { Terminal.DEFAULT_NAME, TerminalAdmin.ADMIN });
    }

    @Test
    public void testTerminalAdmin() throws Exception {
        //admin console
        Terminal.main(new String[] { Terminal.DEFAULT_NAME, TerminalAdmin.ADMIN });
    }

    static void log_(String msg) {
        System.out.print(msg);
    }

    static void log(String msg) {
        System.out.println(msg);
    }
}

class SRunnable implements IRunnable<String, Properties>, Serializable {
    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    @Override
    public String run(Properties context, Object... extArgs) {
        return context.toString();
    }
}

class TCover<T extends Runnable & Serializable & Comparable<? super T>, D extends Comparable<? super D>> extends
        Cover<T, D> implements ILocatable, IListener<Notification> {

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    public TCover(T core, D descriptor) {
        super(core, descriptor);
    }

    @Override
    public void handleEvent(Notification event) {
        //do something like a calculation
        getContent().run();
        //return only the notification itself - the result doesn't matter
        event.addResponse(getPath(), event.getNotification());
    }

    @Override
    public String getPath() {
        return toString();
    }

}

class TRunner implements Runnable, Serializable, Comparable<TRunner> {
    BigDecimal result;
    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    @Override
    public int compareTo(TRunner o) {
        return 0;
    }

    @Override
    public void run() {
        Profiler.si().workLoop(1000000000);
    }

    @Override
    public String toString() {
        return "worker";
    }
}

class TestJob implements Callable<String>, Serializable {
    /** serialVersionUID */
    private static final long serialVersionUID = 7470280926655017926L;

    @Override
    public String call() throws Exception {
        Log log = LogFactory.getLog(this.getClass());
        for (int i = 0; i < 10; i++) {
            Thread.sleep(100);
            log.info(".");
        }
        log.info("test-work done!");
        return "my-test-job";
    }

}

/**
 * test command doing a simple text replacing
 */
class Command extends ACommand<StringBuilder> {

    public Command(StringBuilder context, IChange... changes) {
        super(context, changes);
    }

    @Override
    public void runWith(IChange... changes) {
        for (int i = 0; i < changes.length; i++) {
            String item = (String) changes[i].getItem();
            String old = StringUtil.toString((String) changes[i].getOld());
            String neW = StringUtil.toString((String) changes[i].getNew());
            StringUtil.extract(getContext(), item != null ? item : old, neW);
        }
    }

}

/**
 * test command doing a simple text replacing
 */
@SuppressWarnings({ "unchecked" })
class ECommand extends ACommand<Serializable> {

    public ECommand(Serializable context, IChange... changes) {
        super(context, changes);
    }

    @Override
    public void runWith(IChange... changes) {
        for (int i = 0; i < changes.length; i++) {
            if (changes[i].getItem() != null) {
                //while the context can change on item=null, we have to do it inside the loop
                Class<?> type = getContext().getClass();
                BeanClass<Serializable> b = (BeanClass<Serializable>) BeanClass.getBeanClass(type);
                b.setValue(getContext(), (String) changes[i].getItem(), changes[i].getNew());
            } else {
                setContext((Serializable) changes[i].getNew());
            }
        }
    }

}

class EBean extends TypeBean {
    /** serialVersionUID */
    private static final long serialVersionUID = 2662724418006121099L;

    public boolean equals(Object obj) {
        return BeanUtil.equals(BeanUtil.serialize(this), BeanUtil.serialize(obj));
    }

    @Override
    public String toString() {
        return getString() + getImmutableInteger();//new String(BeanUtil.serialize(this));
    }
}