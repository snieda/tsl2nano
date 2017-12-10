package de.tsl2.nano.util.operation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.Hashtable;
import java.util.Map;

import org.junit.Test;

import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.util.MapUtil;

public class OperationTest {


    @Test
    public void testNumericOperator() {
        String f = "1+ ((x1 + x2)*3 + 4)+5";
        BigDecimal x1 = new BigDecimal(8);
        BigDecimal x2 = new BigDecimal(9);
        Map<CharSequence, BigDecimal> values = new Hashtable<CharSequence, BigDecimal>();
        values.put("x1", x1);
        values.put("x2", x2);
        assertEquals(new BigDecimal(61), new NumericOperator(values).eval(f));

        //TODO: implement this case
//        f ="-1 + (-x1 + x2)";
//        assertEquals(BigDecimal.ZERO, new NumericOperator(values).eval(f));
    }

    @Test
    public void testConditionOperator() {
        String f = "(A&B ) | C ? D : E";

        Map<CharSequence, Object> values = new Hashtable<CharSequence, Object>();
        values.put("A", true);
        values.put("C", true);
//        /*
//         * two condition value-possibilities: action or expression
//         */
//        values.put("D", new CommonAction<Object>() {
//            @Override
//            public Object action() throws Exception {
//                return "DD";
//            }
//        });
        assertEquals("D", new ConditionOperator(values).eval(f));

        values.remove(Operator.KEY_RESULT);
        values.put("A", true);
        values.put("C", false);
        values.put("E", "E");
        assertEquals("E", new ConditionOperator(values).eval(f));

        f = " A = B";
        values.put("B", true);
        assertTrue((Boolean) new ConditionOperator(values).eval(f));
        values.put("B", false);
        assertFalse((Boolean) new ConditionOperator(values).eval(f));
    }

    @Test
    public void testFunction() {
        Function<Number> function = BeanClass.createInstance(Function.class);
        Number result = function.eval("min(pow(x1,x2), 3)", MapUtil.asMap("x1", 2d, "x2", 2d));
//        log(result.toString());
    }

}
