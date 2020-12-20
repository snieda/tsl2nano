package de.tsl2.nano.historize;

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.util.ConcurrentUtil;
import de.tsl2.nano.core.util.ENVTestPreparation;

public class HistorizeTest implements ENVTestPreparation {

	@Before
	public void setUp() {
		ENVTestPreparation.super.setUp("datastructure");
	}
    @Test
    public void testHistorize() throws Exception {
        HistorizedInputFactory.setPath(ENV.getConfigPath());
        HistorizedInputFactory.create("test", 5, String.class);

        HistorizedInputFactory.instance("test").addAndSave("1");
        HistorizedInputFactory.instance("test").addAndSave("2");
        HistorizedInputFactory.instance("test").addAndSave("3");
        HistorizedInputFactory.instance("test").addAndSave("4");
        HistorizedInputFactory.instance("test").addAndSave("5");

        assertTrue(HistorizedInputFactory.instance("test").containsValue("1"));

        //only 5 items are allowed, so the first one was deleted!
        HistorizedInputFactory.instance("test").addAndSave("6");
        assertTrue(HistorizedInputFactory.instance("test").containsValue("6"));
        assertTrue(!HistorizedInputFactory.instance("test").containsValue("1"));

        assertTrue(HistorizedInputFactory.deleteAll());

    }

    @Test
    public void testVolatile() throws Exception {
        Volatile v = new Volatile(1000, "test");
        assertTrue(!v.expired());
        assertTrue(v.get().equals("test"));

        ConcurrentUtil.sleep(1100);
        assertTrue(v.expired());
        assertTrue(v.get() == null);

        v.set("test1");
        assertTrue(!v.expired());
        assertTrue(v.get().equals("test1"));
    }

}