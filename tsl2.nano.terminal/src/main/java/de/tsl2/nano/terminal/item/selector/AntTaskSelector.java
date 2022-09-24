package de.tsl2.nano.terminal.item.selector;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Selector for most ant tasks
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class AntTaskSelector extends Selector {

    /** serialVersionUID */
    private static final long serialVersionUID = -9130139461843255236L;

    /**
     * constructor
     */
    public AntTaskSelector() {
        super();
    }

    /**
     * constructor
     * 
     * @param name
     * @param value
     * @param description
     */
    public AntTaskSelector(String name, Object value, String description) {
        super(name, value, description);
    }

    /**
     * constructor
     * 
     * @param name
     * @param description
     */
    public AntTaskSelector(String name, String description) {
        super(name, description);
    }

    @Override
    protected List createItems(Map props) {
        String tasks =
            "Ant AntCall ANTLR AntStructure AntVersion Apply ExecOn Apt Attrib Augment Available Basename Bindtargets BuildNumber"
                + " BUnzip2 BZip2 Cab Continuus Synergy Tasks CvsChangeLog Checksum Chgrp Chmod Chown Clearcase Tasks Componentdef Concat"
                + " Condition Supported conditions Copy Copydir Copyfile Cvs CVSPass CvsTagDiff CvsVersion Defaultexcludes Delete Deltree"
                + " Depend Dependset Diagnostics Dirname Ear Echo Echoproperties EchoXML EJB Tasks Exec Fail Filter FixCRLF FTP GenKey"
                + " Get GUnzip GZip Hostinfo Image Import Include Input Jar Jarlib-available Jarlib-display Jarlib-manifest"
                + " Jarlib-resolve Java Javac JavaCC Javadoc Javadoc2 Javah JDepend JJDoc JJTree Jlink JspC JUnit JUnitReport Length"
                + " LoadFile LoadProperties LoadResource Local MacroDef Mail MakeURL Manifest ManifestClassPath MimeMail Mkdir Move"
                + " Native2Ascii NetRexxC Nice Parallel Patch PathConvert Perforce Tasks PreSetDef ProjectHelper Property PropertyFile"
                + " PropertyHelper Pvcs Record Rename RenameExtensions Replace ReplaceRegExp ResourceCount Retry RExec Rmic Rpm"
                + " SchemaValidate Scp Script Scriptdef Sequential ServerDeploy Setproxy SignJar Sleep SourceOffSite Sound Splash Sql"
                + " Sshexec Sshsession Subant Symlink Sync Tar Taskdef Telnet Tempfile Touch Translate Truncate TStamp Typedef Unjar"
                + " Untar Unwar Unzip Uptodate Microsoft Visual SourceSafe Tasks Waitfor War WhichResource Weblogic JSP Compiler"
                + " XmlProperty XmlValidate XSLT Style Zip";
        return Arrays.asList(tasks.split(" "));
    }
}