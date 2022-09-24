/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 13.03.2015
 * 
 * Copyright: (c) Thomas Schneider 2015, all rights reserved
 */
package de.tsl2.nano.terminal.item.selector;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.simpleframework.xml.Element;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.util.StringUtil;

/**
 * creates a list of options for this container, reading regular expression patterns from a text file.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class CSVSelector extends Selector<String> {
    /** serialVersionUID */
    private static final long serialVersionUID = -8246582918469244440L;
    /** csv file name */
    @Element
    String csv;
    /** regular expression pattern to extract the child nodes. */
    @Element
    String pattern;
    /** whether to use pattern as delimiter */
    @Element(required = false)
    boolean usePatternAsDelimiter;

    /**
     * constructor
     */
    public CSVSelector() {
        super();
    }

    /**
     * constructor
     * 
     * @param roots
     * @param filter
     */
    public CSVSelector(String name, String description, String csvName, String pattern, boolean usePatternAsDelimiter) {
        super(name, description);
        this.csv = csvName;
        this.pattern = pattern;
        this.usePatternAsDelimiter = usePatternAsDelimiter;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    protected List<String> createItems(Map context) {
            String csvFileName = StringUtil.insertProperties(csv, context);
            Scanner scanner = null;
            List<String> items = new LinkedList<String>();
            try {
                scanner = new Scanner(new File(csvFileName));
                if (usePatternAsDelimiter)
                    scanner.useDelimiter(pattern);
                while (scanner.hasNext()) {
                    if (usePatternAsDelimiter)
                        items.add(StringUtil.insertProperties(scanner.next(), context));
                    else
                        items.add(StringUtil.insertProperties(scanner.next(pattern), context));
                }
            } catch (FileNotFoundException e) {
                ManagedException.forward(e);
            } finally {
                if (scanner != null) {
                    scanner.close();
                }
            }
        return items;
    }

}
