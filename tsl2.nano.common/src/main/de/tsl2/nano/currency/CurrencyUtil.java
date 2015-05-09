/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Apr 16, 2012
 * 
 * Copyright: (c) Thomas Schneider, all rights reserved
 */
package de.tsl2.nano.currency;

import java.io.File;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Currency;
import java.util.Date;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.logging.Log;

import de.tsl2.nano.bean.BeanUtil;
import de.tsl2.nano.collection.CollectionUtil;
import de.tsl2.nano.collection.MapUtil;
import de.tsl2.nano.core.IPredicate;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.util.Period;

/**
 * The jdk implementation (until 1.6) {@link Currency} does not provide needed features like giving historized currency
 * codes. This is done by ICU - International Components for Unicode - Project. But, no factor for calculating the new
 * currency value is given, the implementation is heavy (7MB) and has to initialize a lot of overhead (several seconds).
 * <p/>
 * This class encapsulates and simplifies the use of historic currencies through {@link CurrencyUnit}s. The
 * {@link CurrencyUnit} wraps the standard {@link Currency} extending historizing and calculating aspects. The file
 * 'historical-currencies.csv' should define all historic currencies - the list is not complete! For a complete list, see
 * http://de.wikipedia.org/wiki/ISO_4217.
 * 
 * @author ts
 * @version $Revision$
 */
public class CurrencyUtil {
    private static final String HISTORICAL_CURRENCIES_FILE_NAME = "historical-currencies.csv";
    /** historic Currency-Units read from csv file */
    static final Collection<CurrencyUnit> historicCurrencyUnits = new ArrayList<CurrencyUnit>();
    /** cache of already loaded currencies and their units */
    static final Map<Currency, CurrencyUnit> currencyUnits = new Hashtable<Currency, CurrencyUnit>();

    private static final Log LOG = LogFactory.getLog(CurrencyUtil.class);

//  public static final Currency getCurrency(Locale loc, Date date) {
//  ULocale uLocale = new ULocale(loc.getISO3Country(), loc.getISO3Language());
//  String[] currencyCodes = com.ibm.icu.util.Currency.getAvailableCurrencyCodes(uLocale, date);
//  LOG.debug("Currencies for locale " + loc + " and date " + date + ": " + StringUtil.toString(currencyCodes, 80)); 
//  return Currency.getInstance(currencyCodes[0]);
//}

    /**
     * getCurrency
     * 
     * @return default currency unit
     */
    public static final CurrencyUnit getCurrency() {
        return getCurrency(Locale.getDefault());
    }

    /**
     * getCurrency
     * 
     * @param loc locale to get the currency for
     * @return currency unit for given locale
     */
    public static final CurrencyUnit getCurrency(Locale loc) {
        Currency c = Currency.getInstance(loc);
        CurrencyUnit unit = currencyUnits.get(c);
        if (unit == null) {
            unit = new CurrencyUnit(loc.getCountry(), c.getCurrencyCode(), null, null, 1.0);
            currencyUnits.put(c, unit);
        }
        return unit;
    }

    /**
     * getCurrency
     * 
     * @param date validation date for the default currency
     * @return default currency, valid for the given date
     */
    public static final CurrencyUnit getCurrency(Date date) {
        return getCurrency(Locale.getDefault(), date);
    }

    public static final CurrencyUnit getCurrencyUnit(final Currency c) {
        if (c == null) {
            return null;
        }
        if (historicCurrencyUnits.isEmpty()) {
            initializeCurrencyUnits();
        }
        Collection<CurrencyUnit> currencies = CollectionUtil.getFilteredCollection(historicCurrencyUnits,
            new IPredicate<CurrencyUnit>() {
                @Override
                public boolean eval(CurrencyUnit arg0) {
                    CurrencyUnit cu = arg0;
                    return cu.getCurrencyCode().equals(c.getCurrencyCode());
                }
            });
        return currencies.iterator().next();
    }

    /**
     * getCurrency
     * 
     * @param loc locale to get the currency for
     * @param date validation date for the default currency
     * @return default currency, valid for the given date
     */
    public static final CurrencyUnit getCurrency(final Locale loc, final Date date) {
        if (historicCurrencyUnits.isEmpty()) {
            initializeCurrencyUnits();
        }
        Collection<CurrencyUnit> currencies = CollectionUtil.getFilteredCollection(historicCurrencyUnits,
            new IPredicate<CurrencyUnit>() {
                @Override
                public boolean eval(CurrencyUnit arg0) {
                    CurrencyUnit cu = arg0;
                    return cu.getCountryCode().equals(loc.getCountry()) && new Period(cu.getValidFrom(),
                        cu.getValidUntil()).contains(new Period(date, date));
                }
            });
        if (currencies.size() == 0) {//no historical entry found - use standard currency
            LOG.warn("no historical entry found for " + date + " -> using standard currency!");
            return getCurrency(loc);
        } else if (currencies.size() != 1) {
            LOG.warn("not exactly one currency found for " + loc + ", " + date);
        }
        return currencies.iterator().next();
    }

    /**
     * convenience to get the value for the actual default currency (depends on current locale).
     * 
     * @param value historic value
     * @param currencyDate date of value and its currency
     * @return calculated and rounded value for the actual currency
     */
    public static BigDecimal getActualValue(float value, Date currencyDate) {
        return getActualValue(value, getCurrency(currencyDate));
    }

    /**
     * calculates the value for the given (perhaps historic) unit to actual default unit (depends on current locale).
     * The result is round to the currencies default fraction digits. Please see hints of
     * {@link BigDecimal#BigDecimal(double)} to see problems on construction with a double value.
     * 
     * @param value historic value
     * @param unit currency unit
     * @return actual rounded value
     */
    public static BigDecimal getActualValue(float value, CurrencyUnit unit) {
        BigDecimal bd = BigDecimal.valueOf(value / unit.getFactor());
        return bd.setScale(unit.getCurrency().getDefaultFractionDigits(), BigDecimal.ROUND_HALF_UP);
    }

    /**
     * convenience to get the factor from historic currency to actual currency (depends on current locale).
     * 
     * @param currencyDate date of value and its currency
     * @return historic factor or null
     */
    public static final Double getFactor(Date historicCurrencyDate) {
        return getFactor(Locale.getDefault(), historicCurrencyDate);
    }

    /**
     * convenience to get the factor from historic currency to actual currency.
     * 
     * @param loc locale of currency
     * @param currencyDate date of value and its currency
     * @return historic factor or null
     */
    public static final Double getFactor(Locale loc, Date historicCurrencyDate) {
        return getCurrency(loc, historicCurrencyDate).factor;
    }

    /**
     * evaluates the currency symbol of the given date and formats the value plus symbol. see
     * {@link #getFormattedValue(Number, String)}.
     */
    public static String getFormattedValue(Number value, Date dateOfCurrency) {
        return getFormattedValue(value, getCurrency(dateOfCurrency).getCurrency().getSymbol());
    }

    /**
     * see {@link #getFormattedValue(Number, String)}.
     */
    public static String getFormattedValue(Number value, CurrencyUnit unit) {
        return getFormattedValue(value, unit.getCurrency().getSymbol());
    }

    /**
     * formats the given value to fulfill the current locale - appending the given symbol
     * 
     * @param value value to format
     * @param currencySymbol symbol to append
     * @return string representation
     */
    public static String getFormattedValue(Number value, String currencySymbol) {
        return value != null ? MessageFormat.format("{0} {1}", value, currencySymbol) : "";
    }


    /**
     * creates a NumberFormat for BigDecimals with currency code and fractionDigits
     * 
     * @param currencyCode (optional) currency code (see {@link Currency#getInstance(String)} and {@link http
     *            ://de.wikipedia.org/wiki/ISO_4217} - or null.
     * @param fractionDigits number of fraction digits (precision)
     * @return new numberformat instance
     */
    public static final NumberFormat getFormat(String currencyCode, int fractionDigits) {
        final DecimalFormat numberFormat = (DecimalFormat) (currencyCode != null ? NumberFormat.getCurrencyInstance()
            : NumberFormat.getInstance());
        if (currencyCode != null) {
            numberFormat.setCurrency(Currency.getInstance(currencyCode));
        }
        numberFormat.setMinimumFractionDigits(fractionDigits);
        numberFormat.setMaximumFractionDigits(fractionDigits);
        numberFormat.setGroupingUsed(true);
        numberFormat.setParseBigDecimal(true);
        return numberFormat;
    }

    /**
     * delegates to {@link #getFormat(Locale, Date)} with default locale
     */
    public static NumberFormat getFormat(Date historicCurrencyDate) {
        return getFormat(Locale.getDefault(), historicCurrencyDate);
    }
    
    /**
     * formatter for given perhaps historic currency
     * @param loc locale
     * @param historicCurrencyDate date of currency
     * @return formatter
     */
    public static NumberFormat getFormat(Locale loc, Date historicCurrencyDate) {
        CurrencyUnit currency = getCurrency(loc, historicCurrencyDate);
        return getFormat(currency.getCurrencyCode(), currency.getCurrency().getDefaultFractionDigits());
    }
    
    /**
     * initializeCurrencyUnits
     */
    private static void initializeCurrencyUnits() {
        String filePath = null;
        InputStreamReader reader = null;

        final File file = new File(System.getProperty("user.dir") + File.separator + HISTORICAL_CURRENCIES_FILE_NAME);
        if (file.exists() && file.canRead()) {
            filePath = file.getPath();
            reader = new InputStreamReader(FileUtil.getFile(filePath));
        }

        if (filePath == null) {
            //loading from resource in a jar needs a path separator '/'
            final String ppath = CurrencyUnit.class.getPackage().getName().replace('.', '/');
            filePath = ppath + "/" + HISTORICAL_CURRENCIES_FILE_NAME;
            reader = new InputStreamReader(FileUtil.getResource(filePath));
        }

        Format df = new SimpleDateFormat("yyyy-MM-dd");
        Map<String, Format> formats = MapUtil.asMap("validFrom",
            df,
            "validUntil",
            df,
            "factor",
            NumberFormat.getInstance(Locale.US));
        Collection<CurrencyUnit> c = BeanUtil.fromFlatFile(reader,
            "\t",
            CurrencyUnit.class,
            formats,
            "countryCode",
            "currencyCode",
            "validFrom",
            "validUntil",
            "factor");
        for (CurrencyUnit cu : c) {
            historicCurrencyUnits.add(cu);
        }
    }

}
