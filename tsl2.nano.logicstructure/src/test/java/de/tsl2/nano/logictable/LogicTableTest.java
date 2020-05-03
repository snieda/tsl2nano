package de.tsl2.nano.logictable;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.tsl2.nano.collection.TableList;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.util.ENVTestPreparation;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.incubation.specification.Pool;
import de.tsl2.nano.incubation.specification.rules.Rule;

public class LogicTableTest implements ENVTestPreparation {

    @BeforeClass
    public static void setUp() {
    	ENVTestPreparation.setUp();
    }

    @AfterClass
    public static void tearDown() {
    	ENVTestPreparation.tearDown();
    }
    
    
    @Test
    public void testEquationSolver() {
        String f = "1+ ((x1 + x2)*3 + 4)+5";
       char[] cs = f.toCharArray();
       List<String> s = new ArrayList<String>(cs.length);
       for (int i = 0; i < cs.length; i++) {
           s.add(String.valueOf(cs[i]));
       }
       String term = StringUtil.extract(f, "\\([^)(]*\\)");
       String op1 = StringUtil.extract(f, "[a-zA-Z0-9]+");
       Structure<List<String>, String, String, String> structure = new Structure<List<String>, String, String, String>(s,
           "(",
           ")");
       Collection<List<String>> items = structure.getTree().values();
       for (List<String> list : items) {
           System.out.println(list.toString());
       }

       //TODO: check assertions
       
        BigDecimal x1 = new BigDecimal(8);
        BigDecimal x2 = new BigDecimal(9);
        Map<String, Object> values = new Hashtable<String, Object>();
        values.put("x1", x1);
        values.put("x2", x2);
        assertEquals(new BigDecimal(61), new EquationSolver(null, values).eval(f));
    }

    @Test
    public void testLogicTable() {
        TableList<DefaultHeader, String> table = new LogicTable<DefaultHeader, String>("test", 2).fill(String.class, 2);
        table.set(0, 0, new BigDecimal(10));
        table.set(1, 0, new BigDecimal(9));
        table.set(1, 1, "=A1 * A2");
        System.out.println(table.dump());
        assertEquals(new BigDecimal(90), table.get(1, 1));
        
        table.save("test/");
        TableList loadedTableList = table.load(ENVTestPreparation.testpath("test/test.csv"), LogicTable.class);
        System.out.println(loadedTableList.dump());
        assertEquals(new BigDecimal(90), loadedTableList.get(1, 1));
    }

    @Test
    public void testLogicTableFuncions() {
        Rule<Object> testRule = new Rule("mul", "x*y", Rule.parameters("x", "y"));
        ENV.get(Pool.class).add(testRule);
        
        TableList<DefaultHeader, String> table = new LogicTable<DefaultHeader, String>("test", 2).fill(String.class, 2);
        table.set(0, 0, new BigDecimal(10));
        table.set(1, 0, new BigDecimal(9));
        table.set(1, 1, "=mul(A1, A2)");
        System.out.println(table.dump());
        assertEquals("90", table.get(1, 1));
        
        table.save("test/");
        TableList loadedTableList = table.load(ENVTestPreparation.testpath("test/test.csv"), LogicTable.class);
        System.out.println(loadedTableList.dump());
        assertEquals("90", loadedTableList.get(1, 1));
    }

}
