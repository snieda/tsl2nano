/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 13.03.2015
 * 
 * Copyright: (c) Thomas Schneider 2015, all rights reserved
 */
package de.tsl2.nano.incubation.terminal.item.selector;

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
    public CSVSelector(String name, String description, String csvName, String pattern) {
        super(name, description);
        this.csv = csvName;
        this.pattern = pattern;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    protected List<String> createItems(Map context) {
            String csvFileName = StringUtil.insertProperties(csv, context);
            Scanner scanner = null;
            List<String> items = new LinkedList<String>();
            try {
                scanner = new Scanner(new File(csvFileName));
                while (scanner.hasNext(pattern)) {
                    items.add(scanner.next(pattern));
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
