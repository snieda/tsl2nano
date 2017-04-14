/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 31.03.2017
 * 
 * Copyright: (c) Thomas Schneider 2017, all rights reserved
 */
package de.tsl2.nano.bean.annotation;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import de.tsl2.nano.bean.def.AttributeFinder;
import de.tsl2.nano.collection.CollectionUtil;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ITransformer;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.util.ClassFinder;

/**
 * provides some value sets as allowed values for a Constraint Annotation
 * 
 * @author Tom
 * @version $Revision$
 */
public class ConstraintValueSet {
    // pre-defined allowed value sets (providing a dynamic postfix after ':')
    /** provides all classes of current classloader */
    public static final String ALLOWED_CLASSES = "ALLOWED_CLASSES:";
    /** provides all methods of current classloader */
    public static final String ALLOWED_METHODS = "ALLOWED_METHODS:";
    /** provides all fields of current classloader */
    public static final String ALLOWED_FIELDS = "ALLOWED_FIELDS:";
    /** provides all generated application classes */
    public static final String ALLOWED_APPCLASSES = "ALLOWED_APPCLASSES";
    /** provides all loaded beans */
    public static final String ALLOWED_BEANS = "ALLOWED_BEANS:";
    /** provides all loaded bean-attributes */
    public static final String ALLOWED_BEANATTRS = "ALLOWED_BEANATTRS:";
    /** provides all loaded bean-attributes (without bean prefix) */
    public static final String ALLOWED_BEANATTR_NAMES = "ALLOWED_BEANATTR_NAMES:";
    /** provides all generated application bean-attributes */
    public static final String ALLOWED_APPBEANATTRS = "ALLOWED_APPBEANATTRS";
    /** provides all files inside current ENV-path, matching given reg-exp postfix */
    public static final String ALLOWED_ENVFILES = "ALLOWED_ENVFILES:";
    /** reads from given fileName as postfix, and splits the data through '\n' */
    public static final String ALLOWED_FROMFILE = "ALLOWED_FROMFILE:";

    /**
     * fill pre-defined value sets, given by Constraint annotation
     * 
     * @param allowed Constraint annotation
     * @return value set
     */
    public static String[] preDefined(String[] allowed) {
        LinkedList<String> preAllowed = new LinkedList<String>();
        for (int i = 0; i < allowed.length; i++) {
            if (allowed[i].equals(ALLOWED_CLASSES)) {
                String exp = StringUtil.substring(allowed[i], ":", null);
                preAllowed.addAll((Collection<? extends String>) CollectionUtil.getList(CollectionUtil.getTransforming(
                    new ClassFinder().fuzzyFind(exp).values(), new ITransformer<Class, String>() {

                        @Override
                        public String transform(Class toTransform) {
                            return toTransform.getName();
                        }
                    }).iterator()));
            } else if (allowed[i].equals(ALLOWED_APPCLASSES)) {
                String exp = ENV.get("bean.generation.packagename", "");
                preAllowed.addAll((Collection<? extends String>) CollectionUtil.getList(CollectionUtil.getTransforming(
                    new ClassFinder().fuzzyFind(exp).values(), new ITransformer<Class, String>() {
                        @Override
                        public String transform(Class toTransform) {
                            return toTransform.getName();
                        }
                    }).iterator()));
            } else if (allowed[i].equals(ALLOWED_METHODS)) {
                String exp = StringUtil.substring(allowed[i], ":", null);
                preAllowed.addAll(CollectionUtil
                    .toStringTransformed(new ClassFinder().fuzzyFind(exp, Method.class, -1, null).values()));
            } else if (allowed[i].equals(ALLOWED_FIELDS)) {
                String exp = StringUtil.substring(allowed[i], ":", null);
                preAllowed.addAll(CollectionUtil
                    .toStringTransformed(new ClassFinder().fuzzyFind(exp, Field.class, -1, null).values()));
            } else if (allowed[i].equals(ALLOWED_BEANS)) {
                String exp = ENV.get("bean.generation.packagename", "");
                preAllowed.addAll(CollectionUtil.toStringTransformed(new AttributeFinder().fuzzyFind(exp).values()));
            } else if (allowed[i].equals(ALLOWED_BEANATTRS)) {
                String exp = StringUtil.substring(allowed[i], ":", null);
                preAllowed.addAll(CollectionUtil.toStringTransformed(new AttributeFinder().fuzzyFind(exp).values()));
            } else if (allowed[i].equals(ALLOWED_APPBEANATTRS)) {
                String exp = ENV.get("bean.generation.packagename", "");
                preAllowed.addAll(CollectionUtil.toStringTransformed(new AttributeFinder().fuzzyFind(exp).values()));
            } else if (allowed[i].equals(ALLOWED_BEANATTR_NAMES)) {
                String exp = StringUtil.substring(allowed[i], ":", null);
                preAllowed.addAll((Collection<? extends String>) CollectionUtil.getList(CollectionUtil.getTransforming(
                    new AttributeFinder().fuzzyFind(exp).values(),
                    new ITransformer<String, String>() {

                        @Override
                        public String transform(String toTransform) {
                            return StringUtil.substring(toTransform, ".", null, true);
                        }
                    }).iterator()));
            } else if (allowed[i].startsWith(ALLOWED_ENVFILES)) {
                String regExp = StringUtil.substring(allowed[i], ":", null);
                List<File> files = FileUtil.getTreeFiles(ENV.getConfigPath(), regExp, false);
                preAllowed.addAll(CollectionUtil.toStringTransformed(files));
            } else if (allowed[i].startsWith(ALLOWED_FROMFILE)) {
                String fileName = StringUtil.substring(allowed[i], ":", null);
                String data = String.valueOf(FileUtil.getFileData(fileName, null));
                preAllowed.addAll(Arrays.asList(data.split("\n")));
            } else {
                preAllowed.add(allowed[i]);
            }
        }
        return preAllowed.toArray(new String[0]);
    }

}
