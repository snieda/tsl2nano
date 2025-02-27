/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 17.03.2015
 * 
 * Copyright: (c) Thomas Schneider 2015, all rights reserved
 */
package de.tsl2.nano.terminal.item.selector;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.simpleframework.xml.Attribute;

import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.terminal.item.Option;

/**
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class DirSelector extends TreeSelector<String> {
    /** serialVersionUID */
    private static final long serialVersionUID = 4171362760401843692L;

    @Attribute(required=false)
    boolean showFiles = true;
    /**
     * constructor
     */
    public DirSelector() {
        if (roots == null) {
            roots = new LinkedList<String>();
            roots.add("${user.dir}");
        }
    }

    public DirSelector(String name, String value, String include, String... roots) {
        this(name, null, roots.length == 0 ? new ArrayList<String>() : Arrays.asList(roots),
            include);
    }

    /**
     * constructor
     * 
     * @param roots
     * @param filter
     */
    public DirSelector(String name, String description, List<String> roots, String include) {
        super(name, description);
        if (roots.size() == 0) {
            roots.add("${user.dir}");
        }
        this.roots = roots;
        this.include = include;
    }

    @Override
    protected List<File> createItems(Map context) {
        List<File> files = new ArrayList<File>();
        if (roots == null) {
            roots = new ArrayList<String>();
            roots.add("${user.dir}");
        }
        if (include == null) {
            include = ".*";
        } else {
            include = StringUtil.insertProperties(include, context);
        }
        for (String root : roots) {
            root = StringUtil.insertProperties(root, context);
            File[] ff = FileUtil.getFiles(root, include);
            if (!Util.isEmpty(ff))
                files.addAll(Arrays.asList(ff));
        }
        return files;
    }

    @Override
    protected void addOption(Object item) {
        File file = (File) item;
        if (file.isFile() && !showFiles) {
            return;
        }
//        final IItem<?> parent_ = getParent();
        if (file.isFile()) {
            nodes.add(new Option<String>(this, file.getName(), null, file.getPath(), FileUtil.getDetails(file)) {
/** serialVersionUID */
                private static final long serialVersionUID = 1L;

                //                @Override
//                public IItem react(IItem caller, String input, InputStream in, PrintStream out, Properties env) {
//                    super.react(caller, input, in, out, env);
//                    return parent_;
//                }
//
                @Override
                protected String getName(int fixlength, char filler) {
                    //avoid translation of file names
                    String str = getPresentationPrefix() + name;
                    return fixlength != -1 ? StringUtil.fixString(str, fixlength, filler, true) : str;
                }
            });
        } else {//directory
            DirSelector dir = new DirSelector(file.getName(), file.getPath(), include, file.getPath());
            dir.setParent(this);
            nodes.add(dir);
        }
    }
}
