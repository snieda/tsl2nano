package de.tsl2.nano.incubation.specification;

import static junit.framework.Assert.assertEquals;

import java.math.BigDecimal;
import java.util.LinkedHashMap;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.tsl2.nano.bean.def.Constraint;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.util.ENVTestPreparation;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.incubation.specification.rules.Rule;
import de.tsl2.nano.incubation.specification.rules.RulePool;

public class RuleTest implements ENVTestPreparation {

    @BeforeClass
    public static void setUp() {
    	ENVTestPreparation.setUp("specification", false);
    }

    @AfterClass
    public static void tearDown() {
    	ENVTestPreparation.tearDown();
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
        //XmlUtil.saveXml("test.xml", rule);

        Pool pool = new RulePool();
        pool.add(rule);
        ENV.addService(pool);
        Rule<BigDecimal> ruleWithImport =
            new Rule<BigDecimal>("test-import", "A ? 1 + �test : (x2 * 3)",
                (LinkedHashMap<String, ParType>) MapUtil.asMap("A",
                    ParType.BOOLEAN,
                    "x1",
                    ParType.NUMBER,
                    "x2",
                    ParType.NUMBER));
        rule.addConstraint("result", new Constraint(BigDecimal.class, new BigDecimal(1), new BigDecimal(4)));
        BigDecimal r3 = ruleWithImport.run(MapUtil.asMap("A", true, "x1", new BigDecimal(1), "x2", new BigDecimal(2)));
        assertEquals(new BigDecimal(3), r3);
        //XmlUtil.saveXml("test-import.xml", ruleWithImport);
    }

}
