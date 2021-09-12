package de.tsl2.nano.historize;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.execution.Profiler;
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
    public void testVolatileHard() throws Exception {
        Volatile<String> v = new Volatile<>(1000, "test", true);
        assertTrue(!v.expired());
        assertTrue(v.get().equals("test"));

        ConcurrentUtil.sleep(1010);
        assertTrue(v.expired());
        assertEquals(null, v.get());

        v.set("test1");
        assertTrue(!v.expired());
        assertEquals("test1", v.get());
    }

    @Test
    public void testVolatileSoft() throws Exception {
        Volatile<String> v = new Volatile<>(1000, "test");
        assertTrue(!v.expired());
        assertTrue(v.get().equals("test"));

        ConcurrentUtil.sleep(1010);
        assertTrue(v.expired());
        assertEquals("test", v.get());

        v.set("test1");
        assertTrue(!v.expired());
        assertEquals("test1", v.get());
    }

    @Test
    public void testVolatileWithSupplier() throws Exception {
        Volatile<String> v = new Volatile<>(1000, "test");
        assertTrue(!v.expired());
        assertTrue(v.get().equals("test"));

        ConcurrentUtil.sleep(1010);
        assertTrue(v.expired());
        assertEquals("testx", v.get(() -> "testx"));

        v.set("test1");
        assertTrue(!v.expired());
        assertEquals("test1", v.get( () -> "testy"));
    }

    @Test
    public void testVolatileWithSVolatile() throws Exception {
        SVolatile<String> v = new SVolatile<>(1000, () -> "test");
        assertTrue(v.expired());
        assertTrue(v.get().equals("test"));
        
        try {
			v.get( () -> "xxx");
			fail("don't set another supplier!");
		} catch (Throwable e) {
			assertTrue(e instanceof UnsupportedOperationException);
			assertEquals("please call the get() method without parameter", e.getMessage());
		}
        
        //no value set - so not expiring!
        ConcurrentUtil.sleep(1010);
        assertTrue(v.expired());
        assertEquals("test", v.get());

        v.set("test1");
        assertTrue(!v.expired());
        assertEquals("test1", v.get());

        ConcurrentUtil.sleep(1010);
        assertTrue(v.expired());
        assertEquals("test", v.get());
    }

    @Test
    public void testVolatileCachePerformance() throws Exception {
    	Volatile<Long> v = new Volatile<>(100, 100l);
    	long count = 100000;
    	List<Long> durations = Profiler.si().compareTests(BASE_DIR, true, count,
    			() -> { if (v.expired()) v.set(evalTestKleinerGauss(count)); else v.get();} ,
    			() -> evalTestKleinerGauss(count));
    	//check is done in compareTests!
//    	assertTrue("using Volatile cache should be faster than direct calls!", durations.get(0) > durations.get(1) );
    }
    
    @Test
    public void testSuppliedVolatileCachePerformance() throws Exception {
    	long count = 1000000;
    	Volatile<Long> v = new Volatile<>(100, 100l);
    	SVolatile<Long> vs = new SVolatile<>(100, () -> evalTestKleinerGauss(count));
    	List<Long> durations = Profiler.si().compareTests(BASE_DIR + "-supplied", true, count, 
    			() -> vs.get() ,
    			() -> { if (v.expired()) v.set(evalTestKleinerGauss(count)); else v.get();});
    	
    	//check is done in compareTests!
//    	assertTrue("using supplied Volatile cache should be to slow!", durations.get(0) >= durations.get(1) );
    }
    
    static long evalTestKleinerGauss(long count) {
    	long c = 0;
    	for (int i = 0; i < count; i++) {
			c += i;
		}
    	return c;
    }
}