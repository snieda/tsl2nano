package de.tsl2.nano.util.operation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Hashtable;
import java.util.Map;

import org.junit.Test;

import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.util.DateUtil;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.test.CurrencyUnit;
import de.tsl2.nano.util.test.TypeBean;

public class OperationTest {

    /**
     * tests all classes of package 'operation'
     * 
     * @throws Exception
     */
    @Test
    public void testOperation() throws Exception {
        /*
         * create two units: EURO, DM and convert them to each other
         */

        IConvertableUnit<TypeBean, Currency> euroUnit = new IConvertableUnit<TypeBean, Currency>() {
            @Override
            public TypeBean from(Number toValue) {
                TypeBean newBean = new TypeBean();
                newBean.setPrimitiveDouble((Double) toValue);
                return newBean;
            }

            @Override
            public Number to(TypeBean fromValue) {
                return fromValue.getPrimitiveDouble();
            }

            @Override
            public Currency getUnit() {
                return NumberFormat.getCurrencyInstance().getCurrency();
            }
        };
        long e100 = 100;
        TypeBean euro_100 = new TypeBean();
        euro_100.setPrimitiveDouble(e100);

        /*
         * first, we test basic calculation and presentation with unit
         */
        OperableUnit<TypeBean, Currency> euroValue = new OperableUnit<TypeBean, Currency>(euro_100, euroUnit) {
            @Override
            public String toString() {
                return getConversion() + " " + getUnit().getSymbol();
            }
        };
        TypeBean myBean10000 = euroValue.multiply(euro_100);
        assertTrue(myBean10000.getPrimitiveDouble() == 100d * 100d);
        assertTrue(euroValue.toString().equals("100.0 €"));

        /*
         * second, test the converting
         */
        IConvertableUnit<TypeBean, Currency> demUnit = new IConvertableUnit<TypeBean, Currency>() {
            CurrencyUnit currencyUnit = CurrencyUtil.getCurrency(DateUtil.getDate(2000, 1, 1));

            @Override
            public TypeBean from(Number toValue) {
                TypeBean newBean = new TypeBean();
                newBean.setPrimitiveDouble((Double) toValue);
                return newBean;
            }

            @Override
            public Number to(TypeBean fromValue) {
                return fromValue.getPrimitiveDouble() * currencyUnit.getFactor();
            }

            @Override
            public Currency getUnit() {
                return currencyUnit.getCurrency();
            }
        };

        OperableUnit<TypeBean, Currency> demValue = euroValue.convert(demUnit);
        //factor EUR --> DEM
        double c = 100d * 1.95583d;
        assertTrue(demValue.toString().equals(c + " DEM"));

        /*
         * test ranges
         */
        TypeBean min_10 = new TypeBean();
        min_10.setPrimitiveDouble(10d);
        TypeBean max_100 = new TypeBean();
        max_100.setPrimitiveDouble(100d);

        CRange<TypeBean> range = new CRange<TypeBean>(min_10, max_100, euroUnit);
        assertTrue(range.contains(euro_100));
        assertTrue(range.intersects(euro_100, euro_100));

        TypeBean max_99 = new TypeBean();
        max_99.setPrimitiveDouble(99d);
        CRange<TypeBean> rangeOutside = new CRange<TypeBean>(min_10, max_99, euroUnit);
        assertTrue(!rangeOutside.contains(euro_100));
        assertTrue(!rangeOutside.intersects(euro_100, euro_100));

    }

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
