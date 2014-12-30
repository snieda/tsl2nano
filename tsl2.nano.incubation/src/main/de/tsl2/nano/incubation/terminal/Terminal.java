/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 13.12.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.incubation.terminal;

import static de.tsl2.nano.incubation.terminal.TextTerminal.BLOCK_BAR;
import static de.tsl2.nano.incubation.terminal.TextTerminal.SCREEN_HEIGHT;
import static de.tsl2.nano.incubation.terminal.TextTerminal.SCREEN_WIDTH;
import static de.tsl2.nano.incubation.terminal.TextTerminal.getTextFrame;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.core.Commit;

import de.tsl2.nano.core.Finished;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.Messages;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.ByteUtil;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.core.util.XmlUtil;

/**
 * Terminal showing a textual manual. The configuration can be read through an xml file (standard name:
 * {@link #DEFAULT_NAME}). For further informations, see {@link IItem}.
 * 
 * <pre>
 * Features:
 * - simple input and output on small screens
 * - input constraints check
 * - configuration over xml
 * - result will be written to a property map
 * - tree nodes can be selected through numbers or names
 * - batch mode possible
 * - simplified java method calls
 * - variable output sizes and styles
 * </pre>
 * 
 * @author Tom
 * @version $Revision$
 */
public class Terminal implements IItemHandler, Serializable {
    transient private static final Log LOG;
    static {
        LogFactory.setPrintToConsole(false);
        LOG = LogFactory.getLog(Terminal.class);
    }
    /** used as file name */
    @Attribute
    String name = DEFAULT_NAME;
    /** base item - should be a Selection */
    @Element
    IItem root;
    /** utility to read user input */
    transient Scanner input;
    transient InputStream in;
    transient PrintStream out;
    @Attribute
    int width = SCREEN_WIDTH;
    @Attribute
    int height = SCREEN_HEIGHT;
    @Attribute
    int style = BLOCK_BAR;
    transient Properties env;

    /** batch file name. the batch file contains input instructions (numbers or names) separated by '\n'. */
    String batch;

    static final String KEY_COMMAND = ":";
    static final String KEY_HELP = "help";
    static final String KEY_PROPERTIES = "properties";
    static final String KEY_SAVE = "save";
    static final String KEY_QUIT = "quit";

    public static final String DEFAULT_NAME = "terminal.xml";

    public Terminal() {
        initDeserialization();
    }

    @Commit
    protected void initDeserialization() {
        env = new Properties();
        in = System.in;
        out = System.out;
    }

    public Terminal(IItem root) {
        this(root, System.in, System.out);
    }

    public Terminal(IItem root, InputStream in, PrintStream out) {
        this(root, in, out, SCREEN_WIDTH, SCREEN_HEIGHT, BLOCK_BAR);
    }

    /**
     * constructor
     * 
     * @param root
     * @param input
     * @param in
     * @param out
     */
    public Terminal(IItem root, InputStream in, PrintStream out, int width, int height, int style) {
        super();
        this.root = root;
        this.in = in;
        this.out = out;
        this.width = width;
        this.height = height;
        this.style = style;
        env = new Properties();
    }

    public static Terminal create(String file) {
        Terminal t = XmlUtil.loadXml(file, Terminal.class);
        t.name = file;
        return t;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        try {
            LOG.info("starting terminal " + name);
            input = new Scanner(in);
            prepareEnvironment(env, root);
            serve(root, in, (PrintStream) out, env);
            shutdown();
        } catch (Finished ex) {
            shutdown();
        } catch (Exception ex) {
            ManagedException.forward(ex);
        } finally {
            if (input != null)
                input.close();
        }
    }

    protected void shutdown() {
        save();
        LOG.info("terminal " + name + " ended");
    }

    protected void save() {
        XmlUtil.saveXml(name, this);
        Set<Object> keys = env.keySet();
        //replace objects through their toString()
        Properties envCopy = new Properties();
        for (Object k : keys) {
            Object v = env.get(k);
            envCopy.put(k, v instanceof String ? v : StringUtil.toString(v, -1));
        }
        FileUtil.saveProperties(name + ".properties", envCopy);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void prepareEnvironment(Properties env, IItem root) {
        Object value = root.getValue();
        if (value != null)
            env.put(root.getName(), value);
        if (root.getType().equals(Type.Tree)) {
            List<IItem> childs = ((ITree) root).getNodes();
            for (IItem c : childs) {
                prepareEnvironment(env, c);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getUserInterface() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void printScreen(IItem item, PrintStream out) {
        out.print(TextTerminal.getTextFrame(
            item.getName() + " (value: "
                + StringUtil.toString(item.getValue(), width - (item.getName().length() + 12)).replace('\n', ' ')
                + ")", style, width, true));
        String question = item.ask();
        printScreen(
            item.toString() + "\n " + Messages.getStringOpt("tsl2nano.description") + ":\n " + item.getDescription(),
            out, question);
    }

    /**
     * {@inheritDoc}
     */
    public void printScreen(String screen, PrintStream out, String question) {
        //split screens to max height
        String s = screen;
        int lines = 0, page = 0, i = 0, l = -1;
        while ((i = s.indexOf("\n", l + 1)) < s.length() && i != -1) {
            if (i - l > width - 2)
                i = l + width - 2;
            if (++lines >= height) {
                out.print(getTextFrame(s.substring(page, i), style, width, false));
                out.print("Please hit enter for the next page:");
                page = i + 1;
                lines = 0;
                if (!isInBatchMode())
                    nextLine(in);
                else
                    out.println();
            }
            l = i;
        }
        //print the rest
        if (lines > 1 && lines < height) {
            screen = s.substring(page, s.length());
            screen += StringUtil.fixString(height - lines, ' ').replace(" ", " \n");
            out.print(getTextFrame(screen, style, width, false));
        }
        out.print(question + ": ");
    }

    public static String translate(String name) {
        return StringUtil.toFirstUpper(Messages.getStringOpt(name));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void serve(IItem item, InputStream in, PrintStream out, Properties env) {
        try {
            printScreen(item, out);
            String input = nextLine(in);
            //to see the input in batch mode
            if (!Util.isEmpty(input)) {
                if (input.startsWith(KEY_COMMAND)) {
                    if (isCommand(input, KEY_HELP)) {
                        printScreen(getHelp(), out, "");
                    } else if (isCommand(input, KEY_PROPERTIES)) {
                        System.getProperties().list(out);
                    } else if (isCommand(input, KEY_SAVE)) {
                        save();
                    } else if (isCommand(input, KEY_QUIT)) {
                        throw new Finished("terminal stopped");
                    }
                    nextLine(in);
                    serve(item, in, out, env);
                } else {
//                    put(item);
                    IItem next = item.react(item, input, env);
                    //let us see the result
                    if (item.getType().equals(Type.Action)) {
                        nextLine(in);
                    }

                    if (next != null) {
                        serve(next, in, out, env);
                    }
                }
            }
        } catch (Finished ex) {
            //terminal will be stopped
            throw ex;
        } catch (Exception ex) {
            //print only - no problem
            ex.printStackTrace(out);
            //do it again
            nextLine(in);
            serve(item, in, out, env);
        }
    }

//    private void put(IItem item) {
//        if (item.getValue() != null)
//            env.put(item.getName(), item.getValue());
//        else
//            env.remove(item.getName());
//    }

    private boolean isInBatchMode() {
        return in != System.in;
    }

    /**
     * can be used to use a file as input (no user input possible). call this before you call {@link #run()}. this file
     * should have newline-separated entries that match the terminal structure.
     * 
     * @param batch The batch to set.
     */
    public void setBatch(File batch) {
        this.batch = batch.getPath();
        in = FileUtil.getFile(this.batch);
    }

    public void setBatch(String... cmds) {
        in = createBatchStream(cmds);
    }

    public static InputStream createBatchStream(String... cmds) {
        String concat = StringUtil.concat(new char[] { '\n' }, cmds);
        return ByteUtil.getInputStream(concat.getBytes());
    }

    private boolean isCommand(String input, String cmd) {
        return cmd.startsWith(input.substring(1).toLowerCase());
    }

    /**
     * nextLine
     * 
     * @param in
     * @return
     */
    private String nextLine(InputStream in) {
        String text = input.hasNextLine() ? input.nextLine() : null;
        if (isInBatchMode())
            out.println(text);
        return text;
    }

    public static final String getHelp() {
        return "The Terminal is configured through xml files with only four types of items.\n"
            + " (+) Tree    : holds childs of all types, but normally Options\n"
            + " (*) Input   : user input, has to be terminated with ENTER\n"
            + " (!) Action  : starts a command --> if terminated, the user has to hit ENTER\n"
            + " ( ) Option  : is a simple child of a Tree\n\n"
            + "The items can have the following properties:\n"
            + " x: changed or visited\n"
            + " §: duty (has to be visited)\n\n"
            + "You can leave an item with key ENTER, you can show this help typing ':help',\n"
            + "If you leave the entire menu with ENTER, a property file with the new values\n"
            + "will be written, if you hit Strg+c, the entire menu will be aborted. If you\n"
            + "type ':properties' you will see a list of all property values. :quit will stop\n"
            + " the terminal save the property file.";
    }

    public static void main(String[] args) {
//        if (args.length == 0) {
//            System.out.println("Please provide a file name as base for this terminal");
//            return;
//        }
        try {
            String name = args.length > 0 ? args[0] : DEFAULT_NAME;
            boolean admin = args.length > 1 && args[1].equals(TerminalAdmin.ADMIN) ? true : false;
            if (admin || !new File(name).exists())
                TerminalAdmin.create(name).run();
            else
                create(name).run();
        } catch (Exception e) {
            ManagedException.forward(e);
        }
    }
}
