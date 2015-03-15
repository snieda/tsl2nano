/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 13.03.2015
 * 
 * Copyright: (c) Thomas Schneider 2015, all rights reserved
 */
package de.tsl2.nano.incubation.terminal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.core.Persist;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.util.StringUtil;

/**
 * creates a list of options for this container, reading regular expression patterns from a text file.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class CSVSelector extends Container<String> {
    /** csv file name */
    @Element
    String csv;
    /** regular expression pattern to extract the child nodes. */
    @Element
    String pattern;
    /** serialVersionUID */
    private static final long serialVersionUID = -8246582918469244440L;

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
        initDeserialization();
    }

    @Override
    public List<AItem<String>> getNodes(Map context) {
        if (nodes == null || nodes.size() == 0) {
            final IItem caller_ = this.getParent();
            Properties props = new Properties();
            props.putAll(context);
            props.putAll(System.getProperties());
            String csvFileName = StringUtil.insertProperties(csv, props);
            Scanner scanner = null;
            try {
                scanner = new Scanner(new File(csvFileName));
                nodes = new LinkedList<AItem<String>>();
                while (scanner.hasNext(pattern)) {
                    String item = scanner.next(pattern);
                    nodes.add(new Option<String>(item, null, item, item) {
                        @Override
                        public IItem react(IItem caller, String input, InputStream in, PrintStream out, Properties env) {
                            super.react(caller, input, in, out, env);
                            return caller_;
                        }
                    });
                }
            } catch (FileNotFoundException e) {
                ManagedException.forward(e);
            } finally {
                if (scanner != null)
                scanner.close();
            }
        }
        return super.getNodes(context);
    }

    @Persist
    protected void initSerialization() {
        if (nodes != null)
            nodes.clear();
    }
}
