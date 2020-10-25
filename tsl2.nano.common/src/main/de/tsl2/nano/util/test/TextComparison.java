/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 26.01.2018
 * 
 * Copyright: (c) Thomas Schneider 2018, all rights reserved
 */
package de.tsl2.nano.util.test;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.junit.Assert;

import de.tsl2.nano.core.util.StringUtil;

/**
 * Utility to compare to texts, ignoring given regular expressions and printing the differences
 * @author Tom
 * @version $Revision$ 
 */
public class TextComparison {
    public static String REGEX_DATE_DE = "\\d\\d\\.\\d\\d\\.\\d\\d\\d\\d";
    public static String REGEX_DATE_US = "\\d\\d\\d\\d\\-\\d\\d\\-\\d\\d";
    public static String REGEX_TIME_DE = "\\d\\d\\:\\d\\d(\\:\\d\\d([.,]\\d(\\d(\\d)?)?)?)?";
    public static String XXX = "XXX";
    
    public static void assertEquals(String exptected, String result, boolean ignoreWhitespace, Map<String, String> replacements) {
        if (ignoreWhitespace) {
            exptected = exptected.replaceAll("\\s+", " ");
            result = result.replaceAll("\\s+", " ");
        }
        StringBuilder exp = new StringBuilder(exptected);
        StringBuilder res = new StringBuilder(result);
        prepareForComparison(exp, res, replacements);
        
        exptected = exp.toString();
        result = res.toString();
        Map<String, String> diffs = getDiffs(exptected, result);
        if (diffs.size() > 0) {
            System.out.println("====================================================");
            System.out.println("!!! DIFFERENCE BETWEEN EXPECTED AND RESULT:\n" + StringUtil.toFormattedString(diffs, -1, true));
            System.out.println("====================================================");
        }
        Assert.assertEquals(exp.toString(), res.toString());
    }
    
    public static void prepareForComparison(StringBuilder expected, StringBuilder result, Map<String, String> replacements) {
        Set<String> keys = replacements.keySet();
        int ignored = 0;
        for (String regex : keys) {
          StringUtil.replaceAll(expected, regex, replacements.get(regex));
          ignored += StringUtil.replaceAll(result, regex, replacements.get(regex));
      }
        System.out.println("preparedForComparison with " + replacements.size() + " replacements: ignored " + ignored + " positions");
    }
    
    public static Map<String, String> getDiffs(String text1, String text2) {
        Scanner sc1 = null;
        Scanner sc2 = null;
        Map<String, String> diffs = new HashMap<>();
        try {
            sc1 = new Scanner(text1);
            sc2 = new Scanner(text2);
            String s1 = null, s2 = null;
            while(sc1.hasNext() || sc2.hasNext()) {
                if (sc1.hasNext() )
                    s1 = sc1.next();
                if (sc2.hasNext() )
                    s2 = sc2.next();
                if (s1 != null && !s1.equals(s2))
                    diffs.put(s1,  s2);
            }
        } finally {
            if (sc1 != null)
                sc1.close();
            if (sc2 != null)
                sc2.close();
        }
        return diffs;
    }
}
