package de.my.logbook;

import java.util.Map;
import java.util.SortedMap;

import org.w3c.dom.Document;

import de.tsl2.nano.action.IAction;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.IPageBuilder;
import de.tsl2.nano.core.ISession;
import de.tsl2.nano.core.Main;
import de.tsl2.nano.h5.NanoH5;
import de.tsl2.nano.h5.NanoHTTPD.Method;
import de.tsl2.nano.h5.Users;
import de.tsl2.nano.h5.collector.QueryResult;
import de.tsl2.nano.h5.navigation.IBeanNavigator;
import de.tsl2.nano.h5.plugin.IDOMDecorator;
import de.tsl2.nano.h5.plugin.INanoPlugin;
import de.tsl2.nano.persistence.Persistence;
import de.tsl2.nano.serviceaccess.IAuthorization;

public class LogbookApplication implements INanoPlugin, IDOMDecorator {

	public static void main(String[] args) {
		Main.startApplication(NanoH5.class, null, args);
	}
	
	public void onAuthentication(IAuthorization auth) {
		// TODO Auto-generated method stub
		
	}

	public void configuration(SortedMap<Object, Object> properties, Map<Class<?>, Object> services) {
		properties.put("app.login.administration", false);
		properties.put("websocket.use", true);
		properties.put("app.ssl.activate", true);
		properties.put("tsl2nano.offline", true);
		properties.put("app.show.startpage", true);
		properties.put("session.navigation.gimmick.onemptycollector.create.newitem", true);
		properties.put("session.navigation.gimmick.ononeitemincollector.select.first", true);
		properties.put("session.navigation.start.beandefinitions", "virtual.Controller (ValueType-Entry), Entry");
		properties.put("app.page.style", "background-image: url(icons/fenz.gif); color: white; -webkit-animation: fade 2s; -webkit-animation-fill-mode: both; -moz-animation: fade 2s; -moz-animation-fill-mode: both; -o-animation: fade 2s; -o-animation-fill-mode: both; animation: fade 2s; animation-fill-mode: both;");
        
		Users users = Users.load();
		users.auth("TOM", "tomtom", "SA", "", true);
		users.auth("MONE", "monemone", "SA", "", true);
		properties.put("app.login.secure", true);
		
		createStatistics();
	}

	public <PAGE, OUTPUT, T extends IPageBuilder<PAGE, OUTPUT>> T definePresentationType(T pageBuilder) {
		return pageBuilder;
	}

	public void defineBeanDefinition(BeanDefinition<?> beanDef) {
	}

	public void definePersistence(Persistence persistence) {
		persistence.setAutoddl("update");
		persistence.setDatabase("logbook");
		persistence.setJarFile(System.getProperty("user.dir") + "/tsl2.app.logbook" + "-2.5.1-SNAPSHOT.jar");
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
	}

	@Override
	public void databaseGenerated(Persistence persistence) {
	}

	@Override
	public void beansGenerated(Persistence persistence) {
	}

	private void createStatistics() {
		/*
         * statistic queries
         */
        String stmt = """
			
			-- get a statistic table from logbook entries
			-- user and log-category should be given...
			select lg.Name as LogCategory, vt.Name as ValueType, MIN(e.value) as Minimum, MAX(e.value - e1.value) as MaxDiff, AVG(e.value) as Average, MAX(e.value) as Maximum, SUM(e.value) as Sum\r
			from Entry e \r
			  join Entry e1 on e1.TYPE_ID= e.TYPE_ID\r
			  join LogCategory lg on lg.ID = e.CATEGORY_ID\r
			  join ValueType vt on vt.ID = e.TYPE_ID\r
			where e1.date < e.date or (e1.date = e.date and e1.time < e.time)\r
			group by 1, 2\r
			order by 1, 2""";
        QueryResult.createQueryResult("Logbook-Statistics", stmt);
        
        stmt = """
			
			-- get a statistic table from logbook entries
			-- user and log-category should be given...
			select lg.Name as LogCategory, vt.Name as ValueType, e.date as Date, e.time as Time, e.value as Value\r
			from Entry e \r
			  join LogCategory lg on lg.ID = e.CATEGORY_ID\r
			  join ValueType vt on vt.ID = e.TYPE_ID\r
			order by 1, 2, 3, 4""";
        		
        QueryResult.createQueryResult("Logbook-Course", stmt);
	}

}
