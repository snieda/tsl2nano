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
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.core.Persist;

import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.StringUtil;

/**
 * creates a list of options for this container
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class FileSelector extends Container<String> {
    @ElementList(inline=true, entry="directory", type=String.class)
    List<String> roots;
    @Element
    String include;
    /** serialVersionUID */
    private static final long serialVersionUID = -8246582918469244440L;

    /**
     * constructor
     */
    public FileSelector() {
        super();
    }

    public FileSelector(String name, String include, String...roots) {
        this(name, null, Arrays.asList(roots), include);
    }
    
    /**
     * constructor
     * 
     * @param roots
     * @param filter
     */
    public FileSelector(String name, String description, List<String> roots, String include) {
        super(name, description);
        this.roots = roots;
        this.include = include;
        initDeserialization();
    }

    @Override
    public List<AItem<String>> getNodes(Map context) {
        if (nodes == null || nodes.size() == 0) {
            final IItem caller_ = this.getParent();
            Properties props = new Properties();
            props.putAll(context);
            props.putAll(System.getProperties());
            nodes = new LinkedList<AItem<String>>();
            for (String root : roots) {
                root = StringUtil.insertProperties(root, props);
                List<File> files = FileUtil.getFileset(root, include);
                for (File file : files) {
                    nodes.add(new Option<String>(file.getName(), null, file.getPath(), FileUtil.getDetails(file)) {
                        @Override
                        public IItem react(IItem caller, String input, InputStream in, PrintStream out, Properties env) {
                            super.react(caller, input, in, out, env);
                            return caller_;
                        }
                        @Override
                        protected String getName(int fixlength, char filler) {
                            //avoid translation of file names
                            String str = getPresentationPrefix() + name;
                            return fixlength != -1 ? StringUtil.fixString(str, fixlength, filler, true) : str;
                        }
                    });
                }
            }
        }
        return nodes;
    }
    
    @Persist
    protected void initSerialization() {
        if (nodes != null)
            nodes.clear();
    }
}
