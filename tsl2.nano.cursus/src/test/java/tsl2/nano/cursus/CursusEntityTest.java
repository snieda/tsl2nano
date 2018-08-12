package tsl2.nano.cursus;

import static org.junit.Assert.assertEquals;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import javax.persistence.Entity;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.bean.ValueHolder;
import de.tsl2.nano.bean.annotation.Attributes;
import de.tsl2.nano.bean.annotation.Presentable;
import de.tsl2.nano.bean.annotation.ValueExpression;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.MethodAction;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.cls.ClassFinder;
import de.tsl2.nano.core.cls.IAttribute;
import de.tsl2.nano.core.cls.UnboundAccessor;
import de.tsl2.nano.core.util.ConcurrentUtil;
import de.tsl2.nano.core.util.DateUtil;
import de.tsl2.nano.core.util.ENVTestPreparation;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.core.util.StringUtil;
import tsl2.nano.cursus.IConsilium.Priority;
import tsl2.nano.cursus.persistence.EConsilium;
import tsl2.nano.cursus.persistence.EExsecutio;
import tsl2.nano.cursus.persistence.EGrex;
import tsl2.nano.cursus.persistence.EMutatio;
import tsl2.nano.cursus.persistence.EObsidio;
import tsl2.nano.cursus.persistence.EProcess;
import tsl2.nano.cursus.persistence.ERes;
import tsl2.nano.cursus.persistence.ERuleEffectus;
import tsl2.nano.cursus.persistence.ETimer;

public class CursusEntityTest implements Serializable /* only for the inner-classes */ {

    @BeforeClass
    public static void setUp() {
    	ENVTestPreparation.setUp("cursus", false);
    }

    @AfterClass
    public static void tearDown() {
    	ENVTestPreparation.tearDown();
    }

	@Test
	public void testProcess() {
		BeanContainer.initEmtpyServiceActions();
		Contract contract = new Contract();
		contract.id="1";
		contract.setEnd(DateUtil.MAX_DATE);
		
		ValueHolder<EConsilium> hCons = new ValueHolder<>(null);
		ERes eRes = new ERes(Contract.class.getName(), "1", "end") {
			@Override
			public Collection<EConsilium> getConsilii() {
				return Arrays.asList(hCons.getValue());
			}
			@Override
			public Object resolve() {
				return contract;
			}
		};
		EMutatio eMutatio = new EMutatio("01/01/2019", eRes);
		EExsecutio eExsecutio = new EExsecutio("Änderung-ENDE-Datum", eMutatio, null);
		EConsilium eConsilium = new EConsilium("test", new ETimer(DateUtil.getYesterday(), DateUtil.getTomorrow()), Priority.NORMAL, eExsecutio);
		hCons.setValue(eConsilium);
		
		EProcess eProcess = new EProcess(DateUtil.getYesterday(), DateUtil.getTomorrow()) {
			@Override
			protected void checkAndSave() {
			}
		};
		EGrex grex = new EGrex(eRes) {
			@Override
			public ERes createResForId(Object objectId) {
				return eRes;
			}
		};
		eProcess.actionStart(grex);
		ConcurrentUtil.sleep(2000);
		assertEquals("01.01.2019"/*eMutatio.getNew()*/, DateFormat.getDateInstance().format(contract.end));
	}
	
	@Test
	public void testAnnotations() {
		Collection<Class> entities = ClassFinder.self().fuzzyFind(EConsilium.class.getPackage().getName()).values();
		entities.stream().filter(e -> e.isAnnotationPresent(Entity.class) && !e.equals(ERuleEffectus.class)).forEach(e -> checkEntity(e));
		
		//TODO: consilii is not visible -> attribute will not be found!
//		checkAnnotation(ERes.class, "transient", "consilii", true);
		
	}

	@Test
	public void testProcessActions() {
		checkAction(EProcess.class, "start", "Start", "icons/go.png", MapUtil.asMap("Grex", EGrex.class));
		checkAction(EProcess.class, "deactivate", "Deactivate", "icons/blocked.png", MapUtil.asMap("Res", ERes.class));
		checkAction(EProcess.class, "resetto", "Reset To", "icons/reload.png", MapUtil.asMap("LastActiveConsilium", EConsilium.class));
	}

	private void checkAction(Class entity, String actionName, String actionLabel, String iconPath, Map<String, Class> argTypes) {
		String pre = EProcess.class.getSimpleName().toLowerCase();
		Bean<Object> bProcess = bean(new EProcess());
		
		MethodAction<?> action = (MethodAction<?>) bProcess.getAction(pre + "." + actionName);
		assertEquals(actionLabel, action.getShortDescription());
		assertEquals(iconPath, action.getPresentable().getIcon());
		Bean bean = (Bean) action.toBean(bProcess.getInstance());
		for (String argName : argTypes.keySet()) {
			assertEquals(argTypes.get(argName), bean.getAttribute(argName).getType());
		}
	}
	
	private void checkEntity(Class<?> entity) {
		System.out.println("checking entity " + entity + " ...");
		checkAnnotation(entity, "attributeNames", Attributes.class, "names");
		checkAnnotation(entity, "presentable.icon", Presentable.class, "icon");
		checkAnnotation(entity, "presentable.label", Presentable.class, "label");
		checkAnnotation(entity, "valueExpression.expression", ValueExpression.class, "expression");

		checkAnnotation(entity, "presentable.visible", "id", Presentable.class, "visible");
//		checkAnnotation(entity, "presentable.enabler.active", "exsecutios", Presentable.class, "enabled");
	}

    private void checkAnnotation(Class<?> entity, String beanAttribute, Class<? extends Annotation> annotationCls, String annAttribute) {
    	checkAnnotation(entity, beanAttribute, null, annotationCls, annAttribute);
    }
    private void checkAnnotation(Class<?> entity, String beanAttribute, String property, Class<? extends Annotation> annotationCls, String annAttribute) {
    	Annotation ann = entity.getAnnotation(annotationCls);
    	UnboundAccessor<Annotation> uaAnn = new UnboundAccessor<>(ann);
    	checkAnnotation(entity, beanAttribute, property, uaAnn.call(annAttribute, Object.class));
    }
    private void checkAnnotation(Class<?> entity, String beanAttribute, String property, Object expectedValue) {
		Bean<Object> bean = bean(BeanClass.createInstance(entity));
		if (property != null) {
			IAttribute attr = bean.getAttribute(property, false);
			if (attr == null) {
				if (beanAttribute.equals("presentable.visible"))
					return; //special case: on invisible, the attribute is not accessable...
				else
					throw ManagedException.illegalArgument(property, bean.getAttributeNames());
			}
			bean = bean(attr);
		} else
			bean = bean(bean);
		assertEquals(asString(expectedValue), asString(bean.getValue(beanAttribute.split("\\."))));
	}

	private String asString(Object s) {
		return StringUtil.toString(s, -1);
	}

	@Test
    public void testEntityAttributeAccessability() throws Exception {
    	accessAttributes(new EConsilium(), 10);
    	accessAttributes(new EExsecutio(), 7);
    	accessAttributes(new EObsidio(), 5);
    	accessAttributes(new EMutatio(), 5);
    	accessAttributes(new ERes(), 5);
    	accessAttributes(new ETimer(), 6);
    	accessAttributes(new EGrex(), 3);
//    	TODO: accessAttributes(new ERuleEffectus(), 5);
    }

    @Test
    public void testAttributeOrder() throws Exception {
    	BeanDefinition<ERes> bERes = BeanDefinition.getBeanDefinition(ERes.class);
    	//there are two methods getObjectid() in hierarchy - the overwritten returning a string has to win
    	assertEquals(String.class, bERes.getAttribute("objectid").getAccessMethod().getReturnType());
    }
    
	private void accessAttributes(Object instance, int attrCount) {
		Bean<?> bean = bean(instance);
		String[] attributesWithSetter = BeanClass.getBeanClass(instance).getAttributeNames(true);
    	assertEquals(StringUtil.toString(attributesWithSetter, 100), attrCount, attributesWithSetter.length);
		bean.getAttributes(true).stream().forEach(a -> a.setValue(instance , a.getValue(instance)));
	}

	private Bean<Object> bean(Object instance) {
		return Bean.getBean(instance);
	}
    
}
