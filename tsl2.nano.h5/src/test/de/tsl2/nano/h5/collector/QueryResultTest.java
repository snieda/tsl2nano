package de.tsl2.nano.h5.collector;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;

import de.tsl2.nano.action.IAction;
import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.bean.def.AttributeDefinition;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.SecureAction;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.util.ENVTestPreparation;
import de.tsl2.nano.h5.expression.Query;
import de.tsl2.nano.specification.Pool;

@net.jcip.annotations.NotThreadSafe
public class QueryResultTest implements ENVTestPreparation {

	@Before
	public void setUpBefore() throws Exception {
		Locale.setDefault(Locale.GERMANY);
		setUp("h5");
	}

	@Test
	public void testSimpleQuery() {
		Pool.registerTypes(Query.class);
		String title = "mytestquery1";
		QueryResult.createQueryResult(title, "select * from Charge c where c.value > ${testmaxvalue}");
		
		BeanDefinition.clearCache();
		ENV.get(Pool.class).reset();
		ENV.reload();
		
		Collection<BeanDefinition<?>> virtualDefs = BeanDefinition.loadVirtualDefinitions();
		QueryResult queryResult = (QueryResult) virtualDefs.iterator().next();
		
		Object testResult = new Object[] {"id007", "1", };
		BeanContainer.initEmtpyServiceActions(testResult);
		queryResult.onActivation(new HashMap());
		assertEquals(title, queryResult.queryName);
		assertEquals("Es wurden 1 Elemente gefunden.", queryResult.getSummary());
		
		List<AttributeDefinition<?>> attributes = queryResult.getAttributes();
		assertEquals(1, attributes.size());
		assertEquals("*", attributes.get(0).getColumnDefinition().getName());
		IAction search = queryResult.getAction(title + ".search");
		assertTrue(search instanceof SecureAction);
		assertFalse(search.isEnabled());
		
		((SecureAction)search).setAllPermission(true);
		assertTrue(search.isEnabled());
		
		assertArrayEquals((Object[])testResult, (Object[])((List)search.activate()).get(0));
		
		Query query = (Query) ENV.get(Pool.class).get(title);
		assertEquals("testmaxvalue", query.getParameter().keySet().iterator().next());
		assertEquals("select * from Charge c where c.value > :testmaxvalue", query.getQuery());
		assertEquals("*", query.getColumnNames().get(0));
	}

	@Test
	public void testQueryWithColumnNames() {
		Pool.registerTypes(Query.class);
		String title = "mytestquery2";
		QueryResult.createQueryResult(title, "select c.id, c.value\n from Charge c\n where c.value > ${testmaxvalue}");
		
		BeanDefinition.clearCache();
		ENV.get(Pool.class).reset();
		ENV.reload();
		
		Collection<BeanDefinition<?>> virtualDefs = BeanDefinition.loadVirtualDefinitions();
		QueryResult queryResult = (QueryResult) virtualDefs.iterator().next();

		Object testResult = new Object[] {"id007", "1", };
		BeanContainer.initEmtpyServiceActions(testResult);
		queryResult.onActivation(new HashMap());
		assertEquals(title, queryResult.queryName);
		// on columns.size > 1 a graph will be added (here empty!)
		assertEquals("Es wurden 1 Elemente gefunden.<span></span>", queryResult.getSummary());
		
		List<AttributeDefinition<?>> attributes = queryResult.getAttributes();
		assertEquals(2, attributes.size());
		assertEquals("id", attributes.get(0).getName());
		assertEquals("value", attributes.get(1).getName());

		IAction search = queryResult.getAction(title + ".search");
		assertTrue(search instanceof SecureAction);
		((SecureAction)search).setAllPermission(true);
		assertTrue(search.isEnabled());
		
		assertArrayEquals((Object[])testResult, (Object[])((List)search.activate()).get(0));

		Query query = (Query) ENV.get(Pool.class).get(title);
		assertEquals("testmaxvalue", query.getParameter().keySet().iterator().next());
		assertEquals("select c.id, c.value\n from Charge c\n where c.value > :testmaxvalue", query.getQuery());
		assertEquals("id", query.getColumnNames().get(0));
		assertEquals("value", query.getColumnNames().get(1));
	}

	@Test
	public void testQueryWithColumnAs() {
		Pool.registerTypes(Query.class);
		String title = "mytestquery3";
		QueryResult.createQueryResult(title, "select c.id as id1,\n c.value as value1 \nfrom Charge c \nwhere c.value > ${testmaxvalue}");
		
		BeanDefinition.clearCache();
		ENV.get(Pool.class).reset();
		ENV.reload();
		
		Collection<BeanDefinition<?>> virtualDefs = BeanDefinition.loadVirtualDefinitions();
		QueryResult queryResult = (QueryResult) virtualDefs.iterator().next();
		List<AttributeDefinition<?>> attributes = queryResult.getAttributes();
		assertEquals(title, queryResult.queryName);
		assertEquals(2, attributes.size());
		assertEquals("id1", attributes.get(0).getName());
		assertEquals("value1", attributes.get(1).getName());

		IAction search = queryResult.getAction(title + ".search");
		assertTrue(search instanceof SecureAction);
		((SecureAction)search).setAllPermission(true);
		assertTrue(search.isEnabled());
		
		Object testResult = new Object[] {"id007", "1", };
		BeanContainer.initEmtpyServiceActions(testResult);
		assertArrayEquals((Object[])testResult, (Object[])((List)search.activate()).get(0));
	}

}
