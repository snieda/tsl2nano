/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 26.01.2018
 * 
 * Copyright: (c) Thomas Schneider 2018, all rights reserved
 */
package de.tsl2.nano.autotest;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.junit.Assert;

import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;

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
    
    public static void assertEquals(String name, String expected, String result, boolean ignoreWhitespace, Map<String, String> replacements) {
        if (ignoreWhitespace) {
            expected = expected.replaceAll("[\\s\\p{Z}]+", " ");
            result = result.replaceAll("[\\s\\p{Z}]+", " ");
        }
        StringBuilder exp = new StringBuilder(expected);
        StringBuilder res = new StringBuilder(result);
        prepareForComparison(exp, res, replacements);
        
        expected = exp.toString();
        result = res.toString();
        Map<String, String> diffs = getDiffs(expected, result);
        String formDiff = StringUtil.toFormattedString(diffs, -1, true);
        if (diffs.size() > 0) {
            String file1 = FileUtil.userDirFile(name + "-expected-file.txt").getAbsolutePath();
            String file2 = FileUtil.userDirFile(name + "-result-file.txt").getAbsolutePath();
            final String fexpected = expected, fresult = result;
            Util.trY( () -> Files.write(Paths.get(file1), fexpected.getBytes()));
            Util.trY( () -> Files.write(Paths.get(file2), fresult.getBytes()));
            formDiff = "\n====================================================\n"
				+ "!!! DIFFERENCE BETWEEN EXPECTED AND RESULT:\n" + StringUtil.toFormattedString(diffs, -1, true)
                + "\n ==> see files: " + file1 + " <=> " + file2
            	+ "\n====================================================\n";
        }
        Assert.assertEquals(formDiff, expected, result);
    }
    
    public static void prepareForComparison(StringBuilder expected, StringBuilder result,
            Map<String, String> replacements) {
        Set<String> keys = replacements.keySet();
        int ignored = 0;
        for (String regex : keys) {
            StringUtil.replaceAll(expected, regex, replacements.get(regex));
            ignored += StringUtil.replaceAll_(result, regex, replacements.get(regex));
        }
        System.out.println("preparedForComparison with " + replacements.size() + " replacements: ignored " + ignored
                + " positions");
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
