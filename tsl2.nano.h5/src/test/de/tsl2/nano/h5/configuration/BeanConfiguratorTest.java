package de.tsl2.nano.h5.configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.anonymous.project.Address;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.tsl2.nano.action.IAction;
import de.tsl2.nano.action.IConstraint;
import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.bean.annotation.ConstraintValueSet;
import de.tsl2.nano.bean.def.AttributeDefinition;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.BeanValue;
import de.tsl2.nano.bean.def.Constraint;
import de.tsl2.nano.bean.def.IAttributeDefinition;
import de.tsl2.nano.bean.def.IValueDefinition;
import de.tsl2.nano.bean.def.MethodAction;
import de.tsl2.nano.bean.def.Presentable;
import de.tsl2.nano.bean.def.ValueColumn;
import de.tsl2.nano.bean.def.ValueGroup;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.util.ConcurrentUtil;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.h5.NanoH5;
import de.tsl2.nano.h5.NanoH5Test;

public class BeanConfiguratorTest {

    @BeforeClass
    public static void setUp() {
    }

    @AfterClass
    public static void tearDown() {
    }


    @SuppressWarnings("rawtypes")
    @Test
    public void testConfigurators() throws Throwable {
        NanoH5Test.createENV("beanconf");
        NanoH5 nanoH5 = new NanoH5();
        BeanContainer.initEmtpyServiceActions();
//        ENV.get(Pool.class).add(new Action("testaction", Address.class, "getCity", new LinkedHashMap<>()));
        BeanConfigurator<Address> bconf = BeanConfigurator.create(Address.class).getInstance();
        bconf.actionCreateCompositor(Address.class.getName(), "city", "code", "wrench.png");
        bconf.actionCreateController(Address.class.getName(), "city", null, "code", "wrench.png", "code", 1, 1);
        bconf.actionCreateRuleOrAction("testRule", "§", "1+1");
        bconf.actionCreateRuleOrAction("testRuleScript", "%", "1+1");
        bconf.actionCreateRuleOrAction("testaction", "!", Address.class.getName() + ".getCity");
        bconf.actionCreateRuleOrAction("testREST", "@", "http://web.de");
        bconf.actionCreateRuleOrAction("testquery", "?", "select 1=1");
        bconf.actionAddAttribute("§", "testRule");
        bconf.actionAddAction("testaction");
        bconf.actionCreateSheet("sheet", 2, 2);
        bconf.setValueGroups(Arrays.asList(new ValueGroup("test", false, "city", "code")));
        
        for (AttributeConfigurator aconf : bconf.getAttributes()) {
        	if (!aconf.attr.isVirtual()) {
	            aconf.setColumnDefinition(changeProperty((((ValueColumn)aconf.getColumnDefinition())), "width", 99));
	            aconf.setConstraint(changeProperty((((Constraint)aconf.getConstraint())), "length", 99));
	            aconf.setPresentable(changeProperty((((Presentable)aconf.getPresentable())), "type", 99));
//	            changeProperty(aconf.getValueExpression().getClass(), "expression", "id");
//	            changeProperty(aconf.getFormat().getClass(), "???");
        	}
            ConcurrentUtil.setCurrent(bconf);
            aconf.actionAddListener(aconf.getName(), "city", "§testRule");
            aconf.actionAddRuleCover("presentable", "§testRule");
            aconf.actionCreateRuleOrAction("testaction", "!", Address.class.getName() + ".getCity");
        }
        bconf.actionSave();
        nanoH5.reset();
        
        bconf = BeanConfigurator.create(Address.class).getInstance();
        for (AttributeConfigurator aconf : bconf.getAttributes()) {
            assertTrue(aconf.getListener() != null);
            if (aconf.getColumnDefinition() != null) //may be null on virtual attributes
                assertEquals(aconf.getPresentable().getLabel(), aconf.getColumnDefinition().getPresentable().getLabel());
            aconf.actionRemoveRuleCover("presentable");
        }
        Bean.getBean(new Address(1, "Berliner Str.1", "100000", "Buxdehude", "germany")).getAction("testaction").run();
        checkMethodActions(bconf, 9, 8);
        
        assertEquals("[]", FileUtil.getFileset("./", "**/*beanconf/**/*.failed").toString());
        assertEquals("[]", FileUtil.getFileset("./", "**/*beanconf/**/*.stacktrace").toString());
    }

    private <T extends Serializable> T changeProperty(T attrProperty, String propName, Object value) throws Throwable {
    	BeanConfigurator bc = BeanConfigurator.create(attrProperty.getClass()).getInstance();
    	bc.getAttributes().stream()
    		.filter( a -> ((AttributeConfigurator)a).attr.getName().equals(propName))
    		.peek(a -> ((AttributeConfigurator)a).attr.setValue(attrProperty, value))
    		.findFirst()
    		.orElseThrow(() -> new IllegalArgumentException(propName + " not found"));
    	return attrProperty;
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
                    args[i] = ConstraintValueSet.transformEmptyToVoid(ma, i-1, args[i]);
                }
                ma.setParameter(args);
                count++;
                try {
                    ma.activate();
                } catch (Exception e) {
                    //OK, generic parameters not working - the test checks not the action content!
                    String m = e.getMessage();
                    if (m.contains("FileNotFoundException") || m.contains("attribute Expression") || m.contains("Void")
                        || m.contains("ClassNotFoundException")  || m.contains("TableList"))
                        continue;
                    else
                        ManagedException.forward(e);
                }
            }
        }
        List<File> fileset = FileUtil.getFileset("./", "**/java.xml.stacktrace");
        assertEquals(1, fileset.size());
        assertTrue(fileset.get(0).delete());
        assertEquals(StringUtil.toFormattedString(actions, -1, true), methodActionCount, count);
    }

    @Test
	public void testConfigureConstraint() throws Exception {
		NanoH5Test.createENV("beanconf");
		NanoH5 nanoH5 = new NanoH5();
		BeanContainer.initEmtpyServiceActions();
		Bean<Address> address = Bean.getBean(new Address());
		BeanConfigurator<Address> bconfAddress = BeanConfigurator.create(Address.class).getInstance();
		for (AttributeConfigurator attrAddressConf : bconfAddress.getAttributes()) {
			if (attrAddressConf.getName().equals("city")) {
				BeanConfigurator<? extends AttributeDefinition> bconf = BeanConfigurator.create(attrAddressConf.attr.getClass()).getInstance();
				for (AttributeConfigurator ac : bconf.getAttributes()) {
					if (ac.attr.getName().equals("constraint")) {
						BeanConfigurator<? extends IConstraint> bc = BeanConfigurator
								.create(((Constraint) ac.attr.getConstraint()).getClass()).getInstance();
						System.out.println(bc.def.getValueExpression());
						for (AttributeConfigurator aci : bc.getAttributes()) {
							if (aci.getName().contentEquals("length")) {
								aci.attr.setValue(ac.attr.getConstraint(), 99);
								ac.setConstraint(ac.getConstraint());
								bc.setAttributes(bc.getAttributes());
								bconf.setAttributes(bconf.getAttributes());
								bconfAddress.actionSave();
								break;
							}
						}
						break;
					}
				}
			}
		}
		nanoH5.reset();

		address = Bean.getBean(new Address());
		assertEquals(99, address.getAttribute("city").getConstraint().getLength());
	}
}
