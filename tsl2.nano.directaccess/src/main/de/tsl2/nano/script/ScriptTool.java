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
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

import org.apache.commons.io.FileUtils;

import de.tsl2.nano.Environment;
import de.tsl2.nano.action.CommonAction;
import de.tsl2.nano.action.IAction;
import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.bean.def.BeanValue;
import de.tsl2.nano.exception.ForwardedException;
import de.tsl2.nano.execution.ScriptUtil;
import de.tsl2.nano.persistence.Persistence;
import de.tsl2.nano.util.FileUtil;
import de.tsl2.nano.util.StringUtil;

/**
 * 
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings({ "rawtypes", "serial" , "unchecked"})
public class ScriptTool implements Serializable {
    String sourceFile;
    IAction selectedAction;
    String text;
    Collection<?> result;
    transient Collection<IAction> availableActions;
    transient PrintStream systemout;
    transient IAction<Collection<?>> runner;
    
    /** ant script for several user defined targets. contains an sql target */
    final String ANTSCRIPTNAME = "antscripts.xml";
    final String ANTSCRIPTPROP = "antscripts.properties";

    /**
     * constructor
     */
    public ScriptTool() {
        initAntScriptFile();

        availableActions = new ArrayList<IAction>(1);
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
                    String sqlFilePath = Environment.getConfigPath() + sqlFileName;
                    FileUtil.writeBytes(queries.getBytes(), sqlFilePath, false);
                    Properties p = Persistence.current().getJdbcProperties();
                    p.put("dir", Environment.getConfigPath());
                    p.put("includes", sqlFileName);
                    p.put("classloader", Environment.get(ClassLoader.class));
                    String antFile = Environment.getConfigPath() + ANTSCRIPTNAME;
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
                    String scriptFilePath = Environment.getConfigPath() + scriptFileName;
                    FileUtil.writeBytes(queries.getBytes(), scriptFilePath, false);
                    Properties p = new Properties();
                    p.put("dir", Environment.getConfigPath());
                    p.put("includes", scriptFileName);
                    p.put("classloader", Environment.get(ClassLoader.class));
                    String antFile = Environment.getConfigPath() + ANTSCRIPTNAME;
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
                return "file '" + sourceFile + "' loaded";
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
        String basedir = Environment.getConfigPath();
        File antscriptFile = new File(basedir + ANTSCRIPTNAME);
        if (!antscriptFile.exists()) {
            URL antscriptOriginUrl = Environment.get(ClassLoader.class).getResource(ANTSCRIPTNAME);
            try {
                FileUtils.copyURLToFile(antscriptOriginUrl, antscriptFile);
            } catch (Exception e) {
                ForwardedException.forward(e);
            }
        }
        File antscriptProp = new File(basedir + ANTSCRIPTPROP);
        if (!antscriptProp.exists()) {
            URL antscriptOriginUrl = this.getClass().getClassLoader().getResource(ANTSCRIPTPROP);
            try {
                FileUtils.copyURLToFile(antscriptOriginUrl, antscriptProp);
            } catch (Exception e) {
                ForwardedException.forward(e);
            }
        }
    }

    protected Object executeStatement(String strStmt, boolean pureSQL) throws Exception {
        //jpa access
        if (StringUtil.findRegExp(strStmt.toLowerCase(), "(insert|update|delete|create)\\s.*", 0) == null) {
            return BeanContainer.instance().getBeansByQuery(strStmt, pureSQL, new Object[0]);
        } else {
//            if (pureSQL) {
//                //standard jdbc access
//                Connection con = null;
//                try {
//                    Persistence p = Persistence.getCurrent();
//                    Class.forName(p.getConnectionDriverClass());
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
            String id = "scipttool.go";
            runner = new CommonAction(id, id, id) {
                @Override
                public Object action() throws Exception {
                    return selectedAction != null ? selectedAction.activate() : null;
                }
            };
        }
        return runner;
    }
}
