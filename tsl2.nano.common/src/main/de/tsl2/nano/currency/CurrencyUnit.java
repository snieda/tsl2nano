/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: ts
 * created on: Apr 16, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.currency;

import java.util.Currency;
import java.util.Date;

/**
 * extends the java currency to use historized currencies and to define a base factor for conversions.
 * 
 * @author ts
 * @version $Revision$
 */
public class CurrencyUnit {
    /** ISO 3166 2-letter code */
    String countryCode;
    /** ISO 4217 currency code for this currency */
    String currencyCode;

    /** beginning of use of currency */
    Date validFrom;
    /** end of use of currency */
    Date validUntil;
    /** factor to a base unit */
    Double factor;

    public CurrencyUnit() {
        super();
    }

    public CurrencyUnit(String countryCode, String currencyCode, Date validFrom, Date validUntil, Double factor) {
        super();
        this.countryCode = countryCode;
        this.currencyCode = currencyCode;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
        this.factor = factor;
    }

    /**
     * see {@link #countryCode}
     * 
     * @return county code
     */
    public String getCountryCode() {
        return countryCode;
    }

    /**
     * see {@link #countryCode}
     * 
     * @param countryCode country code
     */
    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    /**
     * see {@link #currencyCode}
     * 
     * @return currency code
     */
    public String getCurrencyCode() {
        return currencyCode;
    }

    /**
     * see {@link #currencyCode}
     * 
     * @param currencyCode currency code
     */
    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    /**
     * see {@link #validFrom}
     * 
     * @return
     */
    public Date getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(Date validFrom) {
        this.validFrom = validFrom;
    }

    /**
     * see {@link #validUntil}
     * @return
     */
    public Date getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(Date validUntil) {
        this.validUntil = validUntil;
    }

    public Double getFactor() {
        return factor;
    }

    public void setFactor(Double factor) {
        this.factor = factor;
    }

    public Currency getCurrency() {
        return Currency.getInstance(currencyCode);
    }
}
