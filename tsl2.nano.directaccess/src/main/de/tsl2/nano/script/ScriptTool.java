/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider, Thomas Schneider
 * created on: Sep 28, 2011
 * 
 * Copyright: (c) Thomas Schneider 2011, all rights reserved
 */
package de.tsl2.nano.script;

import java.io.File;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;

import de.tsl2.nano.action.CommonAction;
import de.tsl2.nano.action.IAction;
import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.bean.BeanUtil;
import de.tsl2.nano.bean.def.BeanValue;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.execution.ScriptUtil;
import de.tsl2.nano.persistence.Persistence;

/**
 * 
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings({ "rawtypes", "serial", "unchecked" })
public class ScriptTool implements Serializable {
    String sourceFile;
    IAction selectedAction;
    String text;
    transient Collection<?> result;
    transient Collection<IAction> availableActions;
    transient PrintStream systemout;
    transient IAction<Collection<?>> runner;

    /** ant script for several user defined targets. contains an sql target */
    final String ANTSCRIPTNAME = "antscripts.xml";
    final String ANTSCRIPTPROP = "antscripts.properties";
    static final String SERIALIZED_SCRIPTOOL = ENV.getTempPath() + ScriptTool.class.getSimpleName().toLowerCase();

    public static ScriptTool createInstance() {
        File file = new File(SERIALIZED_SCRIPTOOL);
        if (file.canRead()) {
            ScriptTool tool = (ScriptTool) FileUtil.load(SERIALIZED_SCRIPTOOL);
            tool.initAntScriptFile();
            tool.initActions();
            return tool;
        } else {
            return new ScriptTool();
        }
    }

    /**
     * constructor
     */
    protected ScriptTool() {
        initAntScriptFile();
        initActions();
    }

    private void initActions() {

        availableActions = new ArrayList<IAction>(8);
        IAction<?> a = new CommonAction<Object>("scripttool.sql.id", "Start Sql-Query", "Starts an sql statement") {
            @Override
            public Object action() throws Exception {
                return executeStatement(getText(), true);
            }

            @Override
            public String toString() {
                return getShortDescription();
            }
        };
        availableActions.add(a);

        a = new CommonAction<Object>("scripttool.ejbql.id", "Start EjbQl-Query", "Starts an Ejb-Ql statement") {
            @Override
            public Object action() throws Exception {
                return executeStatement(getText(), false);
            }

            @Override
            public String toString() {
                return getShortDescription();
            }
        };
        availableActions.add(a);

        a = new CommonAction<Object>("scripttool.sql-ant.id",
            "Start Sql-Script (Ant)",
            "Starts current text as ant sql script") {
            @Override
            public Object action() throws Exception {
                String queries = getText();
                if (queries != null) {
                    String sqlFileName = "sql-script-" + System.currentTimeMillis() + ".sql";
                    String sqlFilePath = ENV.getConfigPath() + sqlFileName;
                    FileUtil.writeBytes(queries.getBytes(), sqlFilePath, false);
                    Properties p = Persistence.current().getJdbcProperties();
                    p.put("dir", ENV.getConfigPath());
                    p.put("includes", sqlFileName);
                    p.put("classloader", ENV.get(ClassLoader.class));
                    String antFile = ENV.getConfigPath() + ANTSCRIPTNAME;
                    boolean result = ScriptUtil.ant(antFile, "sql", p);
                    new File(sqlFilePath).delete();
                    return result ? "successfull" : "errors occurred. please see console for details";
                } else {
                    return "no text found";
                }
            }

            @Override
            public String toString() {
                return getShortDescription();
            }
        };
        availableActions.add(a);

        a = new CommonAction<Object>("scripttool.script-ant.id",
            "Start Scripting",
            "Starts current text as script (see antscripts.xml and variable 'language'") {
            @Override
            public Object action() throws Exception {
                String queries = getText();
                if (queries != null) {
                    String scriptFileName = "script-" + System.currentTimeMillis() + ".script";
                    String scriptFilePath = ENV.getConfigPath() + scriptFileName;
                    FileUtil.writeBytes(queries.getBytes(), scriptFilePath, false);
                    Properties p = new Properties();
                    p.put("dir", ENV.getConfigPath());
                    p.put("includes", scriptFileName);
                    p.put("classloader", ENV.get(ClassLoader.class));
                    String antFile = ENV.getConfigPath() + ANTSCRIPTNAME;
                    boolean result = ScriptUtil.ant(antFile, "script", p);
                    new File(scriptFilePath).delete();
                    return result ? "successfull" : "errors occurred. please see console for details";
                } else {
                    return "no text found";
                }
            }

            @Override
            public String toString() {
                return getShortDescription();
            }
        };
        availableActions.add(a);

        a = new CommonAction<Object>("scripttool.shell-execute",
            "Call Executable File as Shell-Command",
            "Calls Executable File as Shell-Command") {
            @Override
            public Object action() throws Exception {
                try {
                    ScriptUtil.execute(getSourceFile());
                } catch (Exception e) {
                    return "execution of '" + getSourceFile() + "' failed with error:" + e.toString();
                }
                return "execution of '" + getSourceFile() + "' successful";
            }

            @Override
            public String toString() {
                return getShortDescription();
            }
        };
        availableActions.add(a);

        a = new CommonAction<Object>("scripttool.ant-start", "Call File as Ant-Script", "Calls File as Ant-Script") {
            @Override
            public Object action() throws Exception {
                boolean b = ScriptUtil.ant(getSourceFile(), null, new Properties());
                return "execution of ant-script '" + getSourceFile() + (b ? "' successful" : "' failed");
            }

            @Override
            public String toString() {
                return getShortDescription();
            }
        };
        availableActions.add(a);

        final ScriptTool scriptTool = this;
        a = new CommonAction<Object>("scripttool.load-file", "Load the selected file", "Load the selected file") {
            @Override
            public Object action() throws Exception {
                char[] fileData = FileUtil.getFileData(getSourceFile(), null);
                BeanValue.getBeanValue(scriptTool, "text").setValue(String.valueOf(fileData));
                return scriptTool;
            }

            @Override
            public String toString() {
                return getShortDescription();
            }
        };
        availableActions.add(a);

        a = new CommonAction<Object>("scripttool.save-file", "Save the current text", "Save the current text") {
            @Override
            public Object action() throws Exception {
                FileUtil.writeBytes(getText().getBytes(), getSourceFile(), false);
                return "file '" + sourceFile + "' saved";
            }

            @Override
            public String toString() {
                return getShortDescription();
            }
        };
        availableActions.add(a);
    }

    protected void initAntScriptFile() {
        /*
         * on first time, we copy the scripts like antscripts.xml
         * to the plugin-workspace. we do this to provide user changes on that files!
         */
        ENV.extractResource(ANTSCRIPTNAME);
        ENV.extractResource(ANTSCRIPTPROP);
    }

    protected Object executeStatement(String strStmt, boolean pureSQL) throws Exception {
        //jpa access
        if (StringUtil.findRegExp(strStmt.toLowerCase(), "^\\s*select", 0) != null) {
            Object result = BeanContainer.instance().getBeansByQuery(strStmt, pureSQL, new Object[0]);
            //if result is only a single value, return this single value
            if (result instanceof Collection) {
                Collection c = (Collection) result;
                if (c.size() == 1) {
                    Object singleObject = c.iterator().next();
                    if (BeanUtil.isStandardType(singleObject)) {
                        return singleObject;
                    }
                } else if (c.size() == 0) {
                    //don't return an empty collection to avoid an error message
                    return Arrays.asList(ENV.translate("tsl2nano.searchdialog.searchresultcount", false,
                        0));
                }
            }
            return result;
        } else {
//            if (pureSQL) {
//                //standard jdbc access
//                Connection con = null;
//                try {
//                    Persistence p = Persistence.getCurrent();
//                    BeanClass.load(p.getConnectionDriverClass());
//                    con = DriverManager.getConnection(p.getConnectionUrl(),
//                        p.getConnectionUserName(),
//                        p.getConnectionPassword());
//                    Statement stmt = con.createStatement();
//                    stmt.addBatch(strStmt);
//                    return "update-counts: " + StringUtil.toString(stmt.executeBatch(), 200);
//                } finally {
//                    if (con != null)
//                        con.close();
//                }
//            }
            return BeanContainer.instance().executeStmt(strStmt, pureSQL, new Object[0]);
        }
    }

    /**
     * @return Returns the sourceFile.
     */
    public String getSourceFile() {
        return sourceFile;
    }

    /**
     * @param sourceFile The sourceFile to set.
     */
    public void setSourceFile(String sourceFile) {
        this.sourceFile = sourceFile;
    }

    /**
     * @return Returns the text.
     */
    public String getText() {
        return text;
    }

    /**
     * @param text The text to set.
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * @return Returns the selectedAction.
     */
    public IAction getSelectedAction() {
        return selectedAction;
    }

    /**
     * @param selectedAction The selectedAction to set.
     */
    public void setSelectedAction(IAction selectedAction) {
        this.selectedAction = selectedAction;
    }

    /**
     * @return Returns the result.
     */
    public Collection<?> getResult() {
        return result;
    }

    /**
     * @param result The result to set.
     */
    public void setResult(Collection<?> result) {
        this.result = result;
    }

    public Collection<IAction> availableActions() {
        return availableActions;
    }

    public IAction<Collection<?>> runner() {
        if (runner == null) {
            String id = "scripttool.go";
            String lbl = ENV.translate(id, true);
            runner = new CommonAction(id, lbl, lbl) {
                @Override
                public Object action() throws Exception {
                    FileUtil.save(SERIALIZED_SCRIPTOOL, ScriptTool.this);
                    return selectedAction != null ? selectedAction.activate() : null;
                }
            };
        }
        return runner;
    }
}
