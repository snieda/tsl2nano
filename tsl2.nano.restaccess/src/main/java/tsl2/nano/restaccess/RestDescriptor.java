/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 27.04.2016
 * 
 * Copyright: (c) Thomas Schneider 2016, all rights reserved
 */
package tsl2.nano.restaccess;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.ApplicationPath;

/**
 * Simple generic REST method descriptor. should be called by any rest-resource on its OPTION-operation.
 * 
 * @author Tom
 * @version $Revision$
 */
public class RestDescriptor {
    final static Package PRE_RS_ANNOTATION = ApplicationPath.class.getPackage();

    /**
     * describe
     * 
     * @param restBeanContainer
     * @return
     */
    public static String describe(Object rest) {
        StringBuilder txt = new StringBuilder();
        Method[] methods = rest.getClass().getMethods();
        for (int i = 0; i < methods.length; i++) {
            List<String> as = getJaxRSAnnotations(methods[i].getAnnotations());
            if (!as.isEmpty()) {
                txt.append(Arrays.deepToString(as.toArray()));
                txt.append("\n" + shorten(methods[i].toGenericString()) + "\n\n");
            }
        }
        return txt.toString();
    }

    /**
     * getJaxRSAnnotations
     * 
     * @param annotations
     * @return
     */
    private static List<String> getJaxRSAnnotations(Annotation[] annotations) {
        List<String> anns = new LinkedList<>();
        for (int i = 0; i < annotations.length; i++) {
            if (annotations[i].annotationType().getPackage().equals(PRE_RS_ANNOTATION))
                anns.add(shorten(annotations[i].toString()));
        }
        return anns;
    }

    /**
     * shorten
     * 
     * @param string
     * @return
     */
    private static String shorten(String decl) {
        return decl.replaceAll("((public |)?[\\w@]+\\.)", "");
    }

}
