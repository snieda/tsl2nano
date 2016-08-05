/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 03.08.2016
 * 
 * Copyright: (c) Thomas Schneider 2016, all rights reserved
 */
package de.tsl2.nano.bean.def;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import de.tsl2.nano.core.util.ByteUtil;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.StringUtil;

/**
 * reads value-expressions from file and creates corresponding objects. writes objects through there value-expressions.
 * 
 * @author Tom
 * @version $Revision$
 */
public class ValueStream {

    /**
     * convenience delegating to {@link #read(InputStream, ValueExpression)} reading the file content
     */
    public static <T> Collection<T> read(String file, final ValueExpression<T> ve) {
        return read(FileUtil.getFile(file), ve);
    }

    /**
     * reads lines of stream and creates objects through the given value-expression. comments start with '#'.
     * 
     * @param in stream to read the lines from
     * @param ve defines the object format
     * @return list of objects, defined by the given value-expression
     */
    public static <T> Collection<T> read(InputStream in, final ValueExpression<T> ve) {
        List<T> objects = new LinkedList<T>();
        Scanner sc = new Scanner(in);
        try {
            String line;
            T obj;
            while (sc.hasNextLine()) {
                line = sc.nextLine();
                if (line.trim().startsWith("#"))
                    continue;
                line = StringUtil.substring(line, null, "#");
                obj = ve.from(line);
                if (obj == null)
                    obj = ve.createExampleBean(line);//new transient object
                objects.add(obj);
            }
        } finally {
            sc.close();
        }
        return objects;
    }

    /**
     * convenience delegating to {@link #write(OutputStream, Collection)} writing to the given file
     */
    public static <T> void write(String file, Collection<T> objects) {
        write(FileUtil.getFileOutput(file), objects);
    }

    /**
     * write
     * 
     * @param out
     * @param objects
     */
    public static <T> void write(OutputStream out, Collection<T> objects) {
        StringBuilder str = new StringBuilder();
        for (T obj : objects) {
            //WARING: this is more generic than the read method - different types with different ve can be written, but not read! 
            ValueExpression<T> ve = Bean.getBean(obj).getValueExpression();
            str.append(ve.to(obj) + "\n");
        }
        ByteUtil.addToByteStream(out, str.toString(), true);
    }
}
