package de.tsl2.nano.format;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.text.Format;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;

import de.tsl2.nano.autotest.TypeBean;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.util.DateUtil;
import de.tsl2.nano.currency.CurrencyUnit;
import de.tsl2.nano.currency.CurrencyUtil;
import de.tsl2.nano.util.operation.CRange;
import de.tsl2.nano.util.operation.IConvertableUnit;
import de.tsl2.nano.util.operation.OperableUnit;

public class FormatTest {

	@Before
	public void setUp() {
    	Locale.setDefault(Locale.GERMANY);
	}
	@Test
	public void testSimpleDateFormat() throws Exception {
		Format df = RegExpFormat.createDateRegExp();
		String valid[] = { "0", "1", "2", "3", "01", "10", "11", "21", "30", "31", "01.", "01.0", "31.", "31.0", "31.1",
				"31.01", "31.12", "31.12.", "31.12.1", "31.12.2", "22.11.20", "23.09.200", "13.10.2000" };
		Date d;
		for (int i = 0; i < valid.length; i++) {
			d = (Date) df.parseObject(valid[i]);
			System.out.println("parsing '" + valid[i] + "' ==> " + d);
			if (d == null) {
				fail("parsing '" + valid[i] + "' should not fail!");
			}
		}

		// invalids
		String invalid[] = { "4", "9", "32", "01.2", "30.02", "31.02", "31.11", "31.11.", "31.11.2000", "30.13.2000" };
		for (int i = 0; i < invalid.length; i++) {
			try {
				System.out.println("parsing invalid value '" + invalid[i] + "'");
				d = (Date) df.parseObject(invalid[i]);
				if (d != null) {
					fail("parsing '" + invalid[i] + "' should fail!");
				}
			} catch (ManagedException ex) {
				// ok, parsing should fail
			}
		}
	}

	@Test
	public void testCurrencyUnit() throws Exception {
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
		// factor EUR --> DEM
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
}
