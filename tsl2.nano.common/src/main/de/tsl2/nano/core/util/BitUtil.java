/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Apr 13, 2010
 * 
 * Copyright: (c) Thomas Schneider 2010, all rights reserved
 */
package de.tsl2.nano.core.util;

import java.util.List;

/**
 * Some utils for bits and comparables
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class BitUtil extends CUtil {

    protected BitUtil() {
        super();
    }

    /**
     * delegates to {@link Integer#highestOneBit(int)}
     * 
     * @param decimal decimal number containing bits
     * @return highest bit in decimal number
     */
    public static final int highestOneBit(int decimal) {
        return Integer.highestOneBit(decimal);
    }

    /**
     * highestBitPosition
     * 
     * @param decimal number
     * @return number of trailing zeros or -1 if zero.
     */
    public static final int highestBitPosition(int decimal) {
        if (decimal == 0) {
            return 0;
        }
        return Integer.numberOfTrailingZeros(highestOneBit(decimal)) + 1;
    }

    /**
     * bitToDecimal. to do the other direction (e.g. decimalToBit), call {@link #highestOneBit(int)}.
     * 
     * @param bit bit field to be converted into a decimal number
     * @return decimal representation of given bit
     */
    public static final int bitToDecimal(int bit) {
        return 1 << bit;//Math.pow(2, bit);//Float.floatToIntBits(bit);
    }

    /**
     * toggles (removes or adds) the given bitsToFilter from/to the given bit-field
     * 
     * @param field bit field
     * @param bitsToFilter bits to remove or add
     * @return toggled bit field
     */
    public static final int toggleBits(int field, int... bitsToFilter) {
        for (int i = 0; i < bitsToFilter.length; i++) {
            field = hasBit(field, bitsToFilter[i]) ? field - bitsToFilter[i] : field | bitsToFilter[i];
        }
        return field;
    }

    /**
     * filters (removes) the given bitsToFilter from the given bit-field
     * 
     * @param field bit field
     * @param bitsToFilter bits to remove from bit field
     * @return filtered bit field
     */
    public static final int filterBits(int field, int... bitsToFilter) {
        for (int i = 0; i < bitsToFilter.length; i++) {
            field = hasBit(field, bitsToFilter[i]) ? field - bitsToFilter[i] : field;
        }
        return field;
    }

    /**
     * removes all bits between lowest bit and highest bit from given bit field.
     * 
     * @param field bit field
     * @param low lowest bit to filter (eliminate). please provide only the bit position: e.g. 10 instead of 1 << 10.
     * @param high highest bit to filter (eliminate). please provide only the bit position: e.g. 10 instead of 1 << 10.
     * @return filtered bit field
     */
    public static final int filterBitRange(int field, int low, int high) {
        for (int i = low; i <= high; i++) {
            int b = 1 << i;
            if (field < b) {
                break;
            }
            field = hasBit(field, b) ? field - b : field;
        }
        return field;
    }

    /**
     * retains all bits between lowest bit and highest bit from given bit field.
     * 
     * @param field bit field
     * @param low lowest bit to filter (retain). please provide only the bit position: e.g. 10 instead of 1 << 10.
     * @param high highest bit to filter (retain). please provide only the bit position: e.g. 10 instead of 1 << 10.
     * @return filtered bit field
     */
    public static final int retainBitRange(int field, int low, int high) {
        int bits = 0;
        for (int i = low; i <= high; i++) {
            int b = 1 << i;
            if (field < b) {
                break;
            }
            bits |= hasBit(field, b) ? b : 0;
        }
        return bits;
    }

    /**
     * filters (retaines) the given bitsToFilter from the given bit-field. bitsToFilter should be bit values - not bit
     * positions!
     * 
     * @param field bit field
     * @param bitsToFilter bit values (no bit positions) to remove from bit field
     * @return filtered bit field
     */
    public static final int retainBits(int field, int... bitsToFilter) {
        int f = 0;
        for (int i = 0; i < bitsToFilter.length; i++) {
            f += hasBit(field, bitsToFilter[i]) ? bitsToFilter[i] : 0;
        }
        return f;
    }

    /**
     * delegates to {@link #hasBit(int, int, boolean)} using oneOfThem=true.
     */
    public static final boolean hasBit(int bit, int... availableBits) {
        int mask = 0;
        for (int i = 0; i < availableBits.length; i++) {
            mask |= availableBits[i];
        }
        return hasBit(mask, bit, true);
    }

    /**
     * delegates to {@link #hasBit(int, int, boolean)} using oneOfThem=true.
     */
    public static final boolean hasBit(int mask, int bit) {
        return hasBit(mask, bit, true);
    }

    /**
     * evaluates whether the given number contains the bits of the given mask.
     * 
     * @param number number to evaluate
     * @param mask - an OR combination of bits
     * @param oneOfThem if true, only one of the given combination must be contained in the event to return true
     * @return true, if number contains the given bits (mask).
     */
    public static final boolean hasBit(int mask, int number, boolean oneOfThem) {
        return (number & mask) >= (oneOfThem ? 1/*=any*/: number);
    }

    /**
     * reads all bits of given number and returns a bit field with length equal to the highest set bit. each bit that is
     * contained is represented by 1 - all others with 0.
     * 
     * @param number number representing a bitfield
     * @return array containing bits of number (values are 1 or 0)
     */
    public static final int[] bits(int number) {
        int highestBit = highestBitPosition(number);
        int[] bits = new int[highestBit];
        for (int i = 0; i < highestBit; i++) {
            bits[i] = hasBit(number, bitToDecimal(i)) ? 1 : 0;
        }
        return bits;
    }

    /**
     * bit
     * @param bool
     * @return 1 or 0
     */
    public static final int bit(boolean bool) {
        return bool ? 1 : 0;
    }

    /**
     * transforms an int value to a boolean. all values > 0 are true - all others false;
     * 
     * @param value number
     * @return true or false
     */
    public static final boolean bool(int value) {
        return value > 0 ? true : false;
    }

    /**
     * inverse function of {@link #description(int, List)}.
     * 
     * @param description comma-separated list of bit-names to be found on bitNames
     * @param bitNames bit-positioned bit-names
     * @return bit-field representing the given description
     */
    public static final int bits(String description, List<String> bitNames) {
        String[] names = description.split(",[\\s]*");
        int bits = 0;
        for (int i = 0; i < names.length; i++) {
            bits += bitToDecimal(bitNames.indexOf(names[i]));
        }
        return bits;
    }

    /**
     * inverse function of {@link #bits(String, List)}. generates a bit-field description for the given number through
     * the given bitNames.
     * 
     * @param number bit-field
     * @param bitNames bit-positioned bit-names
     * @return bit-field description
     */
    public static final String description(int number, List<String> bitNames) {
        int[] bits = bits(number);
        StringBuilder description = new StringBuilder();
        for (int i = 0; i < bits.length; i++) {
            if (bool(bits[i])) {
                description.append(", " + bitNames.get(i));
            }
        }
        return description.length() > 2 ? description.substring(2) : null;
    }
}