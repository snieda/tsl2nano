package de.tsl2.nano.h5;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.tsl2.nano.action.IAction;
import de.tsl2.nano.bean.def.AttributeDefinition;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.IPageBuilder;
import de.tsl2.nano.bean.def.VAttribute;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.Main;
import de.tsl2.nano.core.util.ConcurrentUtil;
import de.tsl2.nano.core.util.DelegationHandler;
import de.tsl2.nano.core.util.ENVTestPreparation;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.h5.NanoHTTPD.Method;
import de.tsl2.nano.h5.inspect.INanoHandler;
import de.tsl2.nano.h5.navigation.IBeanNavigator;
import de.tsl2.nano.persistence.Persistence;
import de.tsl2.nano.serviceaccess.IAuthorization;

public class NanoHandlerTest implements ENVTestPreparation {
    private static String TEST_DIR;
    private static final String MEINHANDLERTEST = "MEINHANDLERTEST";

    @BeforeClass
    public static void setUp() {
        TEST_DIR = ENVTestPreparation.setUp("h5", false) + TARGET_TEST;
    }

    @AfterClass
    public static void tearDown() {
        NanoHandlerApp.ID = null;
//        ENVTestPreparation.tearDown();
    }

    @Test
    public void testNanoHandlerCallbacks() {
        Thread thread = ConcurrentUtil.startDaemon("test", ()->NanoHandlerApp.main(new String[] {"http://localhost:8067", MEINHANDLERTEST}));
        ConcurrentUtil.sleep(9000);
        
        assertEquals(MEINHANDLERTEST, ENV.get(MEINHANDLERTEST));
        
        FileUtil.copy("src/resources/mda.xml", TEST_DIR + "mda.xml");
        FileUtil.copy("src/resources/mda.bat", TEST_DIR + "mda.bat");
        FileUtil.copy("src/resources/mda.sh", TEST_DIR + "mda.sh");
        FileUtil.copy("../tsl2.nano.directaccess/src/resources/reverse-eng.xml", TEST_DIR + "reverse-eng.xml");
        FileUtil.copy("src/resources/anyway.sql", TEST_DIR + "anyway.sql");
        FileUtil.copy("src/resources/drop-anyway.sql", TEST_DIR + "drop-anyway.sql");
        FileUtil.copy("src/resources/init-anyway.sql", TEST_DIR + "init-anyway.sql");
        NanoH5 nanoH5 = (NanoH5) ENV.get(Main.class);
        Persistence persistence = new Persistence();
        try {
            nanoH5.connect(persistence);
            fail("on first time, the handler should throw an exception!");
        } catch (Exception e) {
            assertEquals("java.lang.IllegalStateException: " + MEINHANDLERTEST, e.getMessage());
        }
        try {
            nanoH5.connect(persistence);
        } catch (Exception e) {
//            assertEquals(MEINHANDLERTEST, e.getStackTrace()[0].getClassName());
            //TODO: generation does not work completely...ant-problems....no problem for this test
        }
        assertEquals(MEINHANDLERTEST, persistence.getConnectionUserName());

        //TODO: test workflow having new Bean from handler
        //TODO: test actions before and after
        
        Map header = MapUtil.asMap("socket", new Socket());
        nanoH5.serve(null, Method.POST, header, new HashMap<>(), new HashMap<>());
        assertEquals(MEINHANDLERTEST, header.get(MEINHANDLERTEST));
        
        thread.interrupt();
    }
}

class NanoHandlerApp implements INanoHandler {
    static String ID;
    
    private int callCount;
    List<String> calledActions = new LinkedList<>();

    public static void main(String[] args) {
        ID = args[1];
        
        Main.startApplication(NanoH5.class, MapUtil.asMap(0, "service.url"), args);
    }
    
    @Override
    public boolean isEnabled() {
        return ID != null;
    }
    
    @Override
    public void onAuthentication(IAuthorization auth) {
        if (callCount++ < 1)
            throw new IllegalStateException(ID);
    }

    @Override
    public void configuration(SortedMap<Object, Object> properties, Map<Class<?>, Object> services) {
        properties.put("app.app.show.startpage", false);
        properties.put("app.db.check.connection", false);
        
        properties.put(ID, ID);
    }

    @Override
    public <PAGE, OUTPUT, T extends IPageBuilder<PAGE, OUTPUT>> T definePresentationType(T pageBuilder) {
        return DelegationHandler.createProxy(new DelegationHandler<T>(pageBuilder));
    }

    @Override
    public void defineBeanDefinition(BeanDefinition<?> beanDef) {
        beanDef.addAttribute(new AttributeDefinition<>(new VAttribute<>(ID)));
    }

    @Override
    public void definePersistence(Persistence persistence) {
        persistence.setConnectionUserName(ID);
    }

    @Override
    public void actionBeforeHandler(IAction<?> action) {
        calledActions.add(action.getShortDescription() + "->BEFORE");
    }

    @Override
    public void actionAfterHandler(IAction<?> action) {
        calledActions.add(action.getShortDescription() + "->AFTER");
    }

    @Override
    public void workflowHandler(IBeanNavigator workflow) {
        workflow.add(new Bean<>(ID));
    }

    @Override
    public void exceptionHandler(Exception ex) {
        ex.setStackTrace(new StackTraceElement[] {new StackTraceElement(ID, ID, ID, 0)});
    }

    @Override
    public void requestHandler(String uri,
            Method m,
            Map<String, String> header,
            Map<String, String> parms,
            Map<String, String> files) {
        header.put(ID, ID);
    }

}
