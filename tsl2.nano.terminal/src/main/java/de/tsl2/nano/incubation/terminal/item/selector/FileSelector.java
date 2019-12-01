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
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;

import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.incubation.terminal.IItem;
import de.tsl2.nano.incubation.terminal.item.Option;

/**
 * creates a list of options for this container
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class FileSelector extends Selector<String> {
    @ElementList(inline = true, entry = "directory", type = String.class)
    List<String> roots;
    @Element
    String include;
    /** serialVersionUID */
    private static final long serialVersionUID = -8246582918469244440L;

    /**
     * constructor
     */
    public FileSelector() {
        if (roots == null) {
            roots = new LinkedList<String>();
            roots.add("${user.dir}");
        }
    }

    public FileSelector(String name, String value, String include, String... roots) {
        this(name, null, roots.length == 0 ? new ArrayList<String>(Arrays.asList(roots)) : Arrays.asList(roots),
            include);
    }

    /**
     * constructor
     * 
     * @param roots
     * @param filter
     */
    public FileSelector(String name, String description, List<String> roots, String include) {
        super(name, description);
        if (roots.size() == 0) {
            roots.add("${user.dir}");
        }
        this.roots = roots;
        this.include = include != null ? include : ".*";
    }

    @Override
    protected List<File> createItems(Map context) {
        List<File> files = new ArrayList<File>();
        include = StringUtil.insertProperties(include, context);
        for (String root : roots) {
            root = StringUtil.insertProperties(root, context);
            List<File> ff = FileUtil.getFileset(root, include);
            files.addAll(ff);
        }
        return files;
    }

    @Override
    protected void addOption(Object item) {
        File file = (File) item;
        final IItem<?> caller_ = this.getParent();
        if (file.isFile()) {
            nodes.add(new Option<String>(this, file.getName(), null, file.getPath(), FileUtil.getDetails(file)) {
                /** serialVersionUID */
                private static final long serialVersionUID = 1L;

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
        } else {//directory
            DirSelector dir = new DirSelector(file.getName(), file.getPath(), include, file.getPath());
            nodes.add(dir);
        }
    }
    
//    @Commit
//    protected void initDeserialization() {
//        super.initDeserialization();
//        if (roots == null) {
//            
//        }
//    }
}
