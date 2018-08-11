package de.tsl2.nano.h5;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.anonymous.project.Address;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.tsl2.nano.action.IAction;
import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.MethodAction;
import de.tsl2.nano.bean.def.ValueGroup;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.h5.configuration.AttributeConfigurator;
import de.tsl2.nano.h5.configuration.BeanConfigurator;
import de.tsl2.nano.incubation.specification.actions.Action;
import de.tsl2.nano.incubation.specification.actions.ActionPool;

public class BeanConfiguratorTest {

    @BeforeClass
    public static void setUp() {
    }

    @AfterClass
    public static void tearDown() {
    }


    @SuppressWarnings("rawtypes")
    @Test
    //TODO: check the results!
    public void testConfigurators() throws Exception {
        NanoH5Test.createENV("beanconf");
        new NanoH5();
        BeanContainer.initEmtpyServiceActions();
        ENV.get(ActionPool.class).add(new Action("testaction", Address.class, "getCity", null));
        BeanConfigurator<Address> bconf = BeanConfigurator.create(Address.class).getInstance();
        bconf.actionAddAction("testaction");
        bconf.actionAddAttribute("date", "expression");
        bconf.actionCreateCompositor(Address.class.getName(), "city", "code", "wrench.png");
        bconf.actionCreateController("code", 1, 1, Address.class.getName(), "city", "code", "wrench.png");
        bconf.actionCreateRuleOrAction("testRule", "§", "1+1");
        bconf.actionCreateRuleOrAction("testRuleScript", "%", "1+1");
        bconf.actionCreateRuleOrAction("testaction", "!", Address.class.getName() + ".getCity");
        bconf.actionCreateRuleOrAction("testREST", "@", "http://web.de");
        bconf.actionCreateRuleOrAction("testaction", "?", "select 1=1");
        bconf.actionCreateSheet("sheet", 2, 2);
        bconf.setValueGroups(Arrays.asList(new ValueGroup("test", false, "city", "code")));
        
        List<AttributeConfigurator> attributes = bconf.getAttributes();
        for (AttributeConfigurator aconf : attributes) {
            aconf.actionAddListener(aconf.getName(), "city", "test");
            assertTrue(aconf.getListener() != null);

            aconf.actionAddRuleCover("presentable", "test");
            aconf.actionCreateRuleOrAction("testaction", "!", Address.class.getName() + ".getCity");
            aconf.actionRemoveRuleCover("presentable");
            if (aconf.getColumnDefinition() != null) //may be null on virtual attributes
            assertEquals(aconf.getPresentable().getLabel(), aconf.getColumnDefinition().getPresentable().getLabel());
        }
        
        checkMethodActions(bconf, 9, 8);
    }

    private void checkMethodActions(BeanConfigurator<Address> bconf, int actionCount, int methodActionCount) {
        Bean bean = new Bean(bconf);
        Collection<IAction<?>> actions = bean.getActions();
        assertEquals(StringUtil.toFormattedString(actions, -1, true), actionCount, actions.size());
        int count = 0;
        for (IAction<?> a : actions) {
            if (a instanceof MethodAction) {
                Collection<IAction> mas = ((MethodAction)a).toBean(bconf).getActions();
                IAction ma = mas.iterator().next();
                Class[] argumentTypes = ma.getArgumentTypes();
                Object[] args = new Object[argumentTypes.length+1];
                args[0] = bconf;
                for (int i = 1; i < args.length; i++) {
                    args[i] = BeanClass.createInstance(argumentTypes[i-1]);
                }
                ma.setParameter(args);
                count++;
                try {
                    ma.activate();
                } catch (ManagedException e) {
                    //OK, generic parameters not working - the test checkes not the action content!
                    if (e.getMessage().contains("FileNotFoundException") || e.getMessage().contains("attribute Expression")
                        || e.getMessage().contains("ClassNotFoundException")  || e.getMessage().contains("TableList"))
                        continue;
                    else
                        ManagedException.forward(e);
                }
            }
        }
        assertEquals(StringUtil.toFormattedString(actions, -1, true), methodActionCount, count);
    }

}
