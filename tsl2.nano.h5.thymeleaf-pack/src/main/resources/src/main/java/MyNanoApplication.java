
import java.io.Serializable;
import java.util.Map;
import java.util.SortedMap;

import org.w3c.dom.Document;

import de.tsl2.nano.action.IAction;
import de.tsl2.nano.bean.def.AttributeDefinition;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.IPageBuilder;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ISession;
import de.tsl2.nano.core.Main;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.h5.HtmlUtil;
import de.tsl2.nano.h5.NanoH5;
import de.tsl2.nano.h5.NanoH5Session;
import de.tsl2.nano.h5.NanoHTTPD.Method;
import de.tsl2.nano.h5.expression.URLExpression;
import de.tsl2.nano.h5.navigation.IBeanNavigator;
import de.tsl2.nano.h5.plugin.IDOMDecorator;
import de.tsl2.nano.h5.plugin.INanoPlugin;
import de.tsl2.nano.persistence.Persistence;
import de.tsl2.nano.serviceaccess.IAuthorization;

public class MyNanoApplication implements INanoPlugin, IDOMDecorator {

	public static void main(String[] args) {
		Main.startApplication(NanoH5.class, null, args);
	}
	
	public void onAuthentication(IAuthorization auth) {
		// TODO Auto-generated method stub
		
	}

	public void configuration(SortedMap<Object, Object> properties, Map<Class<?>, Object> services) {
		ENV.extractResource("location-on-googlemaps.html");
		ENV.extractResource("location-on-osm.html");
	}

	public <PAGE, OUTPUT, T extends IPageBuilder<PAGE, OUTPUT>> T definePresentationType(T pageBuilder) {
		return pageBuilder;
	}

	public void defineBeanDefinition(BeanDefinition<?> beanDef) {
		if (beanDef instanceof Bean && beanDef.getName().equals("Location")) {
			AttributeDefinition urlAttr = beanDef.addAttribute(new URLExpression("@http://web.de"));
			urlAttr.getPresentation().setLayout((Serializable)MapUtil.asMap(HtmlUtil.TAG_EMBED, HtmlUtil.TAG_EMBED));
		}
	}

	public void definePersistence(Persistence persistence) {
		// TODO Auto-generated method stub
		
	}

	public void actionBeforeHandler(IAction<?> action) {
		// TODO Auto-generated method stub
		
	}

	public void actionAfterHandler(IAction<?> action) {
		// TODO Auto-generated method stub
		
	}

	public void workflowHandler(IBeanNavigator workflow) {
		// TODO Auto-generated method stub
		
	}

	public void exceptionHandler(Exception ex) {
		// TODO Auto-generated method stub
		
	}

	public void requestHandler(String uri, Method m, Map<String, String> header, Map<String, String> parms,
			Map<String, String> files) {
		// TODO Auto-generated method stub
		
	}

	public void decorate(Document doc, ISession<?> session) {
		BeanDefinition<?> bean = (BeanDefinition<?>) session.getWorkingObject();
		if (bean instanceof Bean && bean.getName().equals("Coordinate")) {
//			Bean loc = (Bean) bean;
			DOMTLocationOnMap.decoratePanel(doc, session);
		}
	}

	@Override
	public void databaseGenerated(Persistence persistence) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beansGenerated(Persistence persistence) {
		// TODO Auto-generated method stub
		
	}

}
